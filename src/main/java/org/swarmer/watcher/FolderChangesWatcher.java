package org.swarmer.watcher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.context.SwarmConfig;
import org.swarmer.context.SwarmDeployment;
import org.swarmer.context.SwarmFile;
import org.swarmer.context.SwarmerContext;
import org.swarmer.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

public class FolderChangesWatcher {

   private static final Logger LOG = LogManager.getLogger(FolderChangesWatcher.class);

   private FolderChangesContext folderChangesCtx;
   private WatchService         watchService;

   public FolderChangesWatcher() {
      folderChangesCtx = null;
      watchService = null;
   }

   private WatchService createDefaultWatchService() throws IOException {
      return FileSystems.getDefault().newWatchService();
   }

   private Path getDestPath(WatchKey queuedKey, WatchEvent<?> watchEvent) {
      // this is not a complete path
      Path file = (Path) watchEvent.context();
      // need to get parent path
      SwarmConfig swarmConfig = folderChangesCtx.getSwarmConfig(queuedKey);
      Path        parentPath  = swarmConfig.getTargetPath().toPath();

      return parentPath.resolve(file);
   }

   private Path getFullPath(WatchKey queuedKey, WatchEvent<?> watchEvent) {
      Path watchEventFile   = (Path) watchEvent.context();
      Path watchEventFolder = (Path) queuedKey.watchable();

      return watchEventFolder.resolve(watchEventFile);
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

   private void initializeWatcher() throws IOException {
      folderChangesCtx = new FolderChangesContext();
      watchService = createDefaultWatchService();
   }

   private void processModifyEvent(WatchKey queuedKey, WatchEvent<?> watchEvent) throws IOException {
      Path srcPath  = getFullPath(queuedKey, watchEvent);
      Path destPath = getDestPath(queuedKey, watchEvent);

      boolean canSrcFileBeLocked = FileUtil.canObtainExclusiveLock(srcPath);

      if (!canSrcFileBeLocked) {
         try {
            LOG.trace("Sleep for {} ms", SwarmerContext.instance().getLockWaitTimeout());
            Thread.sleep(SwarmerContext.instance().getLockWaitTimeout());
         } catch (InterruptedException e) {}
         canSrcFileBeLocked = FileUtil.canObtainExclusiveLock(srcPath);
      }
      // Remove Dest file only if it exists and src file can be locked
      boolean destFileExists =
              (destPath.toFile().exists() && canSrcFileBeLocked) ? !FileUtil.removeFile(destPath) : false;
      boolean fileAccessConditionsOk = canSrcFileBeLocked && !destFileExists;

      if (!canSrcFileBeLocked) {
         LOG.warn("File not copied because source file can not be locked.");
      } else if (destFileExists) {
         LOG.error("Destination file [{}] is locked by another process.", destPath);
      } else if (fileAccessConditionsOk) {
         folderChangesCtx.addCheckedFileForLocking(srcPath.toFile());
         LOG.info("File [{}] ready for copying [size: {}]", srcPath.toString(),
                  srcPath.toFile().length());
         SwarmDeployment swarmDeployment = folderChangesCtx.getSwarmDeployment(queuedKey);
         long            fileSize        = srcPath.toFile().length();
         SwarmFile swarmFile = swarmDeployment
                 .addSwarmFile(destPath.toFile(), SwarmFile.State.COPYING, fileSize);
         try {
            FileUtil.nioBufferCopy(srcPath.toFile(), destPath.toFile(), swarmFile.getCopyProgress());
         } catch (Exception e) {
            swarmFile.addState(SwarmFile.State.ERROR_COPYING, e);
            throw e;
         }
         swarmFile.addState(SwarmFile.State.COPIED);
      }
   }

   private void processWatchEvents(WatchKey queuedKey) throws IOException {
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
         boolean fileMatchesPattern      = folderChangesCtx.getSwarmConfig(queuedKey).matchesFilePattern(srcFilename);
         boolean isValidFile             = isFile && fileMatchesPattern;
         boolean isValidModifyEvent      = (watchEvent.kind() == StandardWatchEventKinds.ENTRY_MODIFY) && isValidFile;
         boolean isFileCheckedForLocking = folderChangesCtx.hasCheckedFileForLocking(srcPath.toFile());

         if (isValidModifyEvent) {
            LOG.debug("Event... kind={}, count={}, context={} path={}", watchEvent.kind(), watchEvent.count(),
                      watchEvent.context(),
                      srcPath.toFile().getAbsolutePath());
         }

         if (isValidModifyEvent && !isFileCheckedForLocking) {
            processModifyEvent(queuedKey, watchEvent);
         } else if (isValidModifyEvent && isFileCheckedForLocking) {
            folderChangesCtx.removeCheckedFileForLocking(srcPath.toFile());
         }
      }
      LOG.info("Ended polling watch events [size: {}]\n", pollEvents.size());
   }

   private void registerFolders() throws IOException {
      SwarmDeployment[] swarmInstances = SwarmerContext.instance().getSwarmDeployments();
      for (SwarmDeployment swarmInstance : swarmInstances) {
         File srcFolder = swarmInstance.getSourcePath();
         if (srcFolder.exists() && srcFolder.isDirectory()) {
            Path     srcPath = srcFolder.toPath();
            WatchKey key     = srcPath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            folderChangesCtx.addSwarmDeployment(key, swarmInstance);
            LOG.info("Registered folder [{}] for watch.", srcFolder);
         } else {
            String msg = String.format("Folder [%s] does not exist!", srcFolder.getAbsolutePath());
            LOG.error(msg);
            throw new IOException(msg);
         }
      }
   }

   public void watch() throws IOException, InterruptedException {
      initializeWatcher();
      registerFolders();
      try {
         watchLoop(watchService);
      } finally {
         watchService.close();
         folderChangesCtx.reset();
      }
   }

   private void watchLoop(WatchService watchService) throws InterruptedException {
      boolean folderChangesStarted = false;
      while (true) {
         if (!folderChangesStarted) {
            LOG.info("Folder changes watcher started.");
            folderChangesStarted = true;
         }

         WatchKey queuedKey = watchService.take();

         try {
            processWatchEvents(queuedKey);
         } catch (Exception e) {
            LOG.error("Exception from processWatchEvents: [{}]. Continue with watch.", e.getMessage());
         }

         if (!queuedKey.reset()) {
            LOG.info("Removed WatchKey {}.", queuedKey.toString());
            folderChangesCtx.remove(queuedKey);
         }
         if (folderChangesCtx.isEmpty()) {
            LOG.info("Folder changes map is empty. Going out of loop for folder changes watching.",
                     queuedKey.toString());
            break;
         }
      }
      LOG.warn("Folder changes watcher ended.");
   }
}
