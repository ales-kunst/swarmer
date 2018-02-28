package org.swarmer.watcher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.context.SwarmConfig;
import org.swarmer.context.SwarmInstanceData;
import org.swarmer.context.SwarmerContext;
import org.swarmer.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

public class FolderChangesWatcher {

   private static final Logger LOG = LogManager.getLogger(FolderChangesWatcher.class);

   private FolderChangesContext ctx;

   public FolderChangesWatcher() {
      ctx = new FolderChangesContext();
   }

   private WatchService createDefaultWatchService() throws IOException {
      return FileSystems.getDefault().newWatchService();
   }

   private Path getDestPath(WatchKey queuedKey, WatchEvent<?> watchEvent) {
      // this is not a complete path
      Path file = (Path) watchEvent.context();
      // need to get parent path
      SwarmConfig swarmConfig = ctx.getSwarmConfig(queuedKey);
      Path        parentPath  = swarmConfig.getTargetPath().toPath();

      return parentPath.resolve(file);
   }

   private Path getFullPath(WatchKey queuedKey, WatchEvent<?> watchEvent) {
      Path watchEventFile   = (Path) watchEvent.context();
      Path watchEventFolder = (Path) queuedKey.watchable();

      return watchEventFolder.resolve(watchEventFile);
   }

   private void processModifyEvent(WatchKey queuedKey, WatchEvent<?> watchEvent) throws IOException {
      Path srcPath  = getFullPath(queuedKey, watchEvent);
      Path destPath = getDestPath(queuedKey, watchEvent);

      boolean canSrcFileBeLocked = FileUtil.canObtainExclusiveLock(srcPath, ctx);
      // Dest file should not be present because it should be deleted when source file is created
      boolean destFileExists         = !FileUtil.removeFile(destPath);
      boolean fileAccessConditionsOk = canSrcFileBeLocked && !destFileExists;

      if (!canSrcFileBeLocked) {
         LOG.error("File not copied because source file can not be locked.");
      } else if (destFileExists) {
         LOG.error("Destination file [{}] is locked by another process.", destPath);
      } else if (fileAccessConditionsOk) {
         LOG.info("File [{}] ready for copying [size: {}]", srcPath.toString(),
                  srcPath.toFile().length());
         FileUtil.nioBufferCopy(srcPath.toFile(), destPath.toFile());
      }
   }

   private void processWatchEvents(WatchKey queuedKey) throws IOException {
      for (WatchEvent<?> watchEvent : queuedKey.pollEvents()) {
         Path srcPath = getFullPath(queuedKey, watchEvent);

         LOG.trace("Event... kind={}, count={}, context={} path={}", watchEvent.kind(), watchEvent.count(),
                   watchEvent.context(),
                   srcPath.toFile().getAbsolutePath());

         String  srcFilename             = srcPath.toFile().getName();
         boolean isFile                  = srcPath.toFile().isFile();
         boolean fileMatchesPattern      = ctx.getSwarmConfig(queuedKey).matchesFilePattern(srcFilename);
         boolean isValidFile             = isFile && fileMatchesPattern;
         boolean isValidCreateEvent      = (watchEvent.kind() == StandardWatchEventKinds.ENTRY_CREATE) && isValidFile;
         boolean isValidModifyEvent      = (watchEvent.kind() == StandardWatchEventKinds.ENTRY_MODIFY) && isValidFile;
         boolean isFileCheckedForLocking = ctx.hasCheckedFileForLocking(srcPath.toFile());

         if (isValidModifyEvent && !isFileCheckedForLocking) {
            processModifyEvent(queuedKey, watchEvent);
         } else if (isValidModifyEvent && isFileCheckedForLocking) {
            ctx.removeCheckedFileForLocking(srcPath.toFile());
         }
      }
   }

   private void registerFolders(WatchService watchService) throws IOException {
      SwarmInstanceData[] swarmInstances = SwarmerContext.instance().getSwarmInstances();
      for (SwarmInstanceData swarmInstance : swarmInstances) {
         File srcFolder = swarmInstance.getSourcePath();
         if (srcFolder.exists() && srcFolder.isDirectory()) {
            Path path = srcFolder.toPath();
            WatchKey key = path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                                         StandardWatchEventKinds.ENTRY_MODIFY,
                                         StandardWatchEventKinds.ENTRY_DELETE);
            ctx.addSwarmInstance(key, swarmInstance);
         } else {
            String msg = String.format("Folder [%s] does not exist!", srcFolder.getAbsolutePath());
            LOG.error(msg);
            throw new IOException(msg);
         }
      }
   }

   public void start() throws IOException, InterruptedException {
      WatchService watchService = createDefaultWatchService();
      try {
         watchLoop(watchService);
      } finally {
         watchService.close();
         ctx.reset();
      }
   }

   private void watchLoop(WatchService watchService) throws IOException, InterruptedException {
      registerFolders(watchService);
      LOG.info("Folder changes watcher started.");
      while (true) {
         WatchKey queuedKey = watchService.take();

         processWatchEvents(queuedKey);

         if (!queuedKey.reset()) {
            LOG.info("Removed WatchKey {}.", queuedKey.toString());
            ctx.remove(queuedKey);
         }
         if (ctx.isEmpty()) {
            LOG.info("Folder changes map is empty. Going out of loop for folder changes watching.",
                     queuedKey.toString());
            break;
         }
      }
      LOG.warn("Folder changes watcher ended.");
   }
}
