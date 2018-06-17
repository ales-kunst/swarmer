package org.swarmer.operation.watcher;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.context.SwarmJob;
import org.swarmer.context.SwarmerCtx;
import org.swarmer.json.DeploymentContainerCfg;
import org.swarmer.json.SwarmerCfg;
import org.swarmer.operation.InfiniteLoopOperation;
import org.swarmer.util.FileUtil;
import org.swarmer.util.SwarmUtil;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class FolderChangesWatcher extends InfiniteLoopOperation {
   public static final  String OP_NAME = "Folder Watcher";
   private static final Logger LOG     = LogManager.getLogger(FolderChangesWatcher.class);

   private SwarmerCfg cfg;
   private Set<Integer> succesfullyLocked;

   public FolderChangesWatcher(String name, SwarmerCtx context) {
      super(name, context);
      succesfullyLocked = new HashSet<>();
   }

   @Override
   protected void operationInitialize() {
      cfg = getContext().getCfg();
   }

   private SwarmerCfg getCfg() {
      if (cfg.getSwarmerCtxId() != getContext().getId()) {
         cfg = getContext().getCfg();
      }
      return cfg;
   }

   @Override
   protected void loopBlock() throws Exception {
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

         String  srcFilename            = srcPath.toFile().getName();
         boolean isFile                 = srcPath.toFile().isFile();
         String  filePattern            = getFilePattern(queuedKey);
         boolean fileMatchesPattern     = FileUtil.matchesFilePattern(srcFilename, filePattern);
         boolean isValidFile            = isFile && fileMatchesPattern;
         boolean isValidModifyEvent     = (watchEvent.kind() == StandardWatchEventKinds.ENTRY_MODIFY) && isValidFile;
         boolean fileSuccessfullyLocked = isFileSuccessfullyLocked(queuedKey);

         if (isValidModifyEvent) {
            LOG.debug("Event... kind={}, count={}, context={} path={}", watchEvent.kind(), watchEvent.count(),
                      watchEvent.context(),
                      srcPath.toFile().getAbsolutePath());
         }

         if (isValidModifyEvent && !fileSuccessfullyLocked) {
            processModifyEvent(queuedKey, watchEvent);
         } else if (isValidModifyEvent && fileSuccessfullyLocked) {
            clearFileSuccessfullyLocked(queuedKey);
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
         LOG.trace("Sleep for {} ms", getContext().getLockWaitTimeout());
         SwarmUtil.waitFor(getContext().getLockWaitTimeout());
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
         setFileSuccessfullyLocked(queuedKey);
         LOG.error("Destination file [{}] is locked by another process.", destPath);
      } else if (fileAccessConditionsOk) {
         setFileSuccessfullyLocked(queuedKey);
         LOG.info("File [{}] ready for copying [size: {}]", srcPath.toString(),
                  srcPath.toFile().length());
         FileUtil.nioBufferCopy(srcPath.toFile(), destPath.toFile());
         String containerName = getContainerName(queuedKey);
         getContext().addSwarmJob(SwarmJob.builder()
                                          .action(SwarmJob.Action.RUN_NEW)
                                          .containerName(containerName)
                                          .swarmJarFile(destPath.toFile())
                                          .build());
      }
   }

   private Path getDestPath(WatchKey queuedKey, WatchEvent<?> watchEvent) {
      // this is not a complete path
      Path file = (Path) watchEvent.context();
      // need to get parent path
      Path parentPath = getDestFolder(queuedKey).toPath();

      return parentPath.resolve(file);
   }

   private File getDestFolder(WatchKey watchKey) {
      return findDeploymentCfg(watchKey).getDestFolder();
   }

   private String getFilePattern(WatchKey watchKey) {
      return findDeploymentCfg(watchKey).getFilePattern();
   }

   private String getContainerName(WatchKey watchKey) {
      return findDeploymentCfg(watchKey).getName();
   }

   private void setFileSuccessfullyLocked(WatchKey watchKey) {
      succesfullyLocked.add(watchKey.hashCode());
   }

   private boolean isFileSuccessfullyLocked(WatchKey watchKey) {
      return succesfullyLocked.contains(watchKey.hashCode());
   }

   private void clearFileSuccessfullyLocked(WatchKey watchKey) {
      succesfullyLocked.remove(watchKey.hashCode());
   }

   private DeploymentContainerCfg findDeploymentCfg(WatchKey watchKey) {
      for (int index = 0; index < cfg.deploymentContainerCfgsSize(); index++) {
         DeploymentContainerCfg containerCfg = cfg.getDeploymentContainerCfg(index);
         String hashCode = Integer.toString(watchKey.hashCode());
         if (containerCfg.getWatchKeyHash().equals(hashCode)) {
            return containerCfg;
         }
      }
      return null;
   }

   @Override
   protected void handleError(Exception exception) {
      LOG.error("Exception from processWatchEvents. Continue with watch. Error stacktrace: \n {}",
                ExceptionUtils.getStackTrace(exception));
   }

   @Override
   protected void operationFinalize() {}
}
