package org.swarmer.operation.watcher;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swarmer.context.ObservableFile;
import org.swarmer.context.SwarmerCtx;
import org.swarmer.exception.ExceptionThrower;
import org.swarmer.exception.SwarmerException;
import org.swarmer.json.DeploymentContainerCfg;
import org.swarmer.json.SwarmerCfg;
import org.swarmer.operation.InfiniteLoopOperation;
import org.swarmer.util.FileUtil;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FolderChangesWatcher extends InfiniteLoopOperation {
   public static final  String OP_NAME = "Folder Watcher";
   private static final Logger LOG     = LoggerFactory.getLogger(FolderChangesWatcher.class);

   private SwarmerCfg cfg;

   public FolderChangesWatcher(String name, SwarmerCtx context) {
      super(name, context);
   }

   @Override
   protected void operationInitialize() {
      cfg = getContext().getCfg();
   }

   @Override
   protected void loopBlock() throws Exception {
      WatchKey queuedKey = getContext().getWatchService().poll(1000, TimeUnit.MILLISECONDS);

      if ((queuedKey != null) && queuedKey.isValid()) {
         processWatchEvents(queuedKey);
         if (!queuedKey.reset()) {
            LOG.warn("WatchKey is closed [{}].", queuedKey);
         }
      }
   }

   @Override
   protected void handleError(Throwable exception) {
      LOG.warn("Exception from processWatchEvents. Continue with watch. Error stacktrace: \n {}",
               ExceptionUtils.getStackTrace(exception));
   }

   private void processWatchEvents(WatchKey queuedKey) throws SwarmerException {
      // If we do it in this way then we poll current events and remove them from list. Must be done in such a way
      // because every call to this function gets fresh list from event list. Do NOT put queuedKey.pollEvents() in the
      List<WatchEvent<?>> pollEvents = queuedKey.pollEvents();
      LOG.debug("Started polling watch events [size: {}]: {}", pollEvents.size(),
                getWatchEventsInfo(queuedKey, pollEvents));
      for (WatchEvent<?> watchEvent : pollEvents) {
         Path srcPath = FileUtil.getFullPath(queuedKey, watchEvent);

         String  srcFilename        = srcPath.toFile().getName();
         boolean isFile             = srcPath.toFile().isFile();
         String  filePattern        = getFilePattern(queuedKey);
         boolean fileMatchesPattern = FileUtil.matchesFilePattern(srcFilename, filePattern);
         boolean isValidFile        = isFile && fileMatchesPattern;

         if (isValidFile) {
            LOG.info("Event... kind={}, count={}, context={} path={}", watchEvent.kind(), watchEvent.count(),
                     watchEvent.context(),
                     srcPath.toFile().getAbsolutePath());
            processModifyEvent(queuedKey, watchEvent);
         }
      }
      LOG.debug("Ended polling watch events [size: {}]\n", pollEvents.size());
   }

   private String getFilePattern(WatchKey watchKey) {
      return findDeploymentCfg(watchKey).getFilePattern();
   }

   private String getWatchEventsInfo(WatchKey queuedKey, List<WatchEvent<?>> pollEvents) {
      StringBuilder sb = new StringBuilder();
      for (WatchEvent<?> watchEvent : pollEvents) {
         Path srcPath = FileUtil.getFullPath(queuedKey, watchEvent);
         sb.append(String.format("| %s : %s ", watchEvent.kind(), srcPath));
      }
      sb.append("|");

      return sb.toString();
   }

   private void processModifyEvent(WatchKey queuedKey, WatchEvent<?> watchEvent) {
      Path           srcPath       = FileUtil.getFullPath(queuedKey, watchEvent);
      Path           destPath      = getDestPath(queuedKey, watchEvent);
      String         containerName = getContainerName(queuedKey);
      ObservableFile of            = new ObservableFile(srcPath, destPath, containerName, System.currentTimeMillis());
      getContext().addObservableFile(of);
   }

   private Path getDestPath(WatchKey queuedKey, WatchEvent<?> watchEvent) {
      // this is not a complete path
      Path file = (Path) watchEvent.context();
      // need to get parent path
      Path parentPath = getDestFolder(queuedKey).toPath();

      return parentPath.resolve(file);
   }

   private String getContainerName(WatchKey watchKey) {
      return findDeploymentCfg(watchKey).getName();
   }

   private File getDestFolder(WatchKey watchKey) {
      return findDeploymentCfg(watchKey).getDestFolder();
   }

   private DeploymentContainerCfg findDeploymentCfg(WatchKey watchKey) {
      for (int index = 0; index < cfg.deploymentContainerCfgsSize(); index++) {
         DeploymentContainerCfg containerCfg = cfg.getDeploymentContainerCfg(index);
         String                 hashCode     = Integer.toString(watchKey.hashCode());
         if (containerCfg.getWatchKeyHash().equals(hashCode)) {
            return containerCfg;
         }
      }
      throw ExceptionThrower.createIllegalArgumentException("DeploymentContainer not found [WatchKey: {}]!", watchKey);
   }

   @Override
   protected void operationFinalize() {
      // This method is empty because we do not need to do any finalization!!!!!
   }
}
