package org.swarmer.watcher;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.InfiniteThreadOperation;
import org.swarmer.context.DeploymentContainer;
import org.swarmer.context.SwarmFile;
import org.swarmer.context.SwarmerContext;
import org.swarmer.util.FileUtil;
import org.swarmer.util.SwarmExecutor;

import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FolderChangesWatcher extends InfiniteThreadOperation<SwarmerContext> {

   private static final Logger  LOG = LogManager.getLogger(FolderChangesWatcher.class);
   private              boolean folderChangesStarted;

   public FolderChangesWatcher(SwarmerContext context) {
      super(context);
   }

   @Override
   protected String threadName() {
      return "Folder Watcher Thread";
   }

   @Override
   protected void operationInitialize() {
      folderChangesStarted = false;
   }

   @Override
   protected boolean shouldStop() {
      return false;
   }

   @Override
   protected void loopBlock() throws Exception {
      if (!folderChangesStarted) {
         LOG.info("Folder changes watcher started.");
         folderChangesStarted = true;
      }

      WatchKey queuedKey = getContext().getWatchService().poll(1000, TimeUnit.MILLISECONDS);

      if ((queuedKey != null) && queuedKey.isValid()) {
         try {
            processWatchEvents(queuedKey);
         } catch (Exception e) {
            LOG.error("Exception from processWatchEvents: [{}]. Continue with watch.", e.getMessage());
         }
         if (!queuedKey.reset()) {
            LOG.warn("WatchKey is closed [{}].", queuedKey.toString());
         }
      }
   }

   private void processWatchEvents(WatchKey queuedKey) {
      // If we do it in this way then we poll current events and remove them from list. Must be done in such a way
      // because every call to this function gets fresh list from event list. Do NOT put queuedKey.pollEvents() in the
      // for loop (e.g. for (WatchEvent<?> watchEvent : queuedKey.pollEvents()) {})
      List<WatchEvent<?>> pollEvents = queuedKey.pollEvents();
      LOG.info("Started polling watch events [size: {}]: {}", pollEvents.size(),
               getWatchEventsInfo(queuedKey, pollEvents));
      for (WatchEvent<?> watchEvent : pollEvents) {
         Path srcPath = getFullPath(queuedKey, watchEvent);

         String  srcFilename             = srcPath.toFile().getName();
         boolean isFile                  = srcPath.toFile().isFile();
         String  filePattern             = getContext().getFilePattern(queuedKey);
         boolean fileMatchesPattern      = FileUtil.matchesFilePattern(srcFilename, filePattern);
         boolean isValidFile             = isFile && fileMatchesPattern;
         boolean isValidModifyEvent      = (watchEvent.kind() == StandardWatchEventKinds.ENTRY_MODIFY) && isValidFile;
         boolean isFileCheckedForLocking = getContext().isFileSuccessfullyLocked(queuedKey);

         if (isValidModifyEvent) {
            LOG.debug("Event... kind={}, count={}, context={} path={}", watchEvent.kind(), watchEvent.count(),
                      watchEvent.context(),
                      srcPath.toFile().getAbsolutePath());
         }

         if (isValidModifyEvent && !isFileCheckedForLocking) {
            processModifyEvent(queuedKey, watchEvent);
         } else if (isValidModifyEvent && isFileCheckedForLocking) {
            getContext().clearFileSuccessfullyLocked(queuedKey);
         }
      }
      LOG.info("Ended polling watch events [size: {}]\n", pollEvents.size());
   }

   private String getWatchEventsInfo(WatchKey queuedKey, List<WatchEvent<?>> pollEvents) {
      StringBuilder sb = new StringBuilder();
      for (WatchEvent<?> watchEvent : pollEvents) {
         Path srcPath = getFullPath(queuedKey, watchEvent);
         sb.append(String.format("| %s : %s ", watchEvent.kind(), srcPath));
      }
      sb.append("|");

      return sb.toString();
   }

   private Path getFullPath(WatchKey queuedKey, WatchEvent<?> watchEvent) {
      Path watchEventFile   = (Path) watchEvent.context();
      Path watchEventFolder = (Path) queuedKey.watchable();

      return watchEventFolder.resolve(watchEventFile);
   }

   private void processModifyEvent(WatchKey queuedKey, WatchEvent<?> watchEvent) {
      Path srcPath  = getFullPath(queuedKey, watchEvent);
      Path destPath = getDestPath(queuedKey, watchEvent);

      boolean canSrcFileBeLocked = FileUtil.canObtainExclusiveLock(srcPath);

      if (!canSrcFileBeLocked) {
         LOG.trace("Sleep for {} ms", SwarmerContext.instance().getLockWaitTimeout());
         SwarmExecutor.waitFor(SwarmerContext.instance().getLockWaitTimeout());
         canSrcFileBeLocked = FileUtil.canObtainExclusiveLock(srcPath);
      }
      // Remove Dest file only if it exists and src file can be locked
      boolean destFileExists =
              (destPath.toFile().exists() && canSrcFileBeLocked) && !FileUtil.removeFile(destPath);
      boolean fileAccessConditionsOk = canSrcFileBeLocked && !destFileExists;

      if (!canSrcFileBeLocked) {
         LOG.warn("File not copied because source file can not be locked.");
      } else if (destFileExists) {
         // We have to add that the lock has happened for next two events should be ignored
         getContext().setFileSuccessfullyLocked(queuedKey);
         LOG.error("Destination file [{}] is locked by another process.", destPath);
      } else if (fileAccessConditionsOk) {
         getContext().setFileSuccessfullyLocked(queuedKey);
         LOG.info("File [{}] ready for copying [size: {}]", srcPath.toString(),
                  srcPath.toFile().length());
         DeploymentContainer deploymentContainer = getContext().getDeploymentContainer(queuedKey);
         long                fileSize            = srcPath.toFile().length();
         SwarmFile swarmFile = deploymentContainer.addSwarmFile(destPath.toFile(),
                                                                SwarmFile.State.COPYING, fileSize);
         try {
            FileUtil.nioBufferCopy(srcPath.toFile(), destPath.toFile(), swarmFile.getCopyProgress());
         } catch (Exception e) {
            swarmFile.setState(SwarmFile.State.ERROR_COPYING, e);
            throw e;
         }
         swarmFile.setState(SwarmFile.State.COPIED);
      }
   }

   private Path getDestPath(WatchKey queuedKey, WatchEvent<?> watchEvent) {
      // this is not a complete path
      Path file = (Path) watchEvent.context();
      // need to get parent path
      Path parentPath = getContext().getDestFolder(queuedKey).toPath();

      return parentPath.resolve(file);
   }

   @Override
   protected void handleError(Exception exception) {
      LOG.error("Exception from processWatchEvents. Continue with watch. Error stacktrace: \n {}",
                ExceptionUtils.getStackTrace(exception));
   }

   @Override
   protected void operationFinalize() {}
}
