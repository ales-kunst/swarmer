package org.swarmer.watcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.context.SwarmConfig;
import org.swarmer.context.SwarmerContext;
import org.swarmer.util.FileUtil;

public class FolderChangesWatcher {

   private static final Logger LOG = LogManager.getLogger(FolderChangesWatcher.class);

   private Map<WatchKey, SwarmConfig> keySwarmConfigMap = new HashMap<WatchKey, SwarmConfig>();

   public FolderChangesWatcher() {
   }

   public void start() throws IOException, InterruptedException {
      WatchService watchService = createDefaultWatchService();
      try {
         watchLoop(watchService);
      } finally {
         watchService.close();
      }
   }

   private void watchLoop(WatchService watchService) throws IOException, InterruptedException {
      registerFolders(watchService, SwarmerContext.instance().getSwarmConfigs());
      LOG.info("Folder changes watcher started.");
      while (true) {
         WatchKey queuedKey = watchService.take();

         processWatchEvents(queuedKey);

         if (!queuedKey.reset()) {
            LOG.info("Removed WatchKey {}.", queuedKey.toString());
            keySwarmConfigMap.remove(queuedKey);
         }
         if (keySwarmConfigMap.isEmpty()) {
            LOG.info("Folder changes map is empty. Going out of loop for folder changes watching.", queuedKey.toString());
            break;
         }
      }
      LOG.warn("Folder changes watcher ended.");
   }

   private void processWatchEvents(WatchKey queuedKey) throws IOException {
      for (WatchEvent<?> watchEvent : queuedKey.pollEvents()) {
         Path srcFullPath = getFullPath(queuedKey, watchEvent);

         LOG.trace("Event... kind={}, count={}, context={} path={}", watchEvent.kind(), watchEvent.count(), watchEvent.context(),
                   srcFullPath.toFile().getAbsolutePath());

         boolean isFile = srcFullPath.toFile().isFile();
         if ((watchEvent.kind() == StandardWatchEventKinds.ENTRY_MODIFY) && isFile) {
            String srcFilename = srcFullPath.toFile().getName();
            boolean fileMatchesPattern = keySwarmConfigMap.get(queuedKey).matchesFilePattern(srcFilename);

            if (!FileUtil.isFileLocked(srcFullPath) && fileMatchesPattern) {
               Path destPath = getDestPath(queuedKey, watchEvent);
               if (shouldCopyFile(srcFullPath, destPath)) {
                  LOG.trace("File [{}] ready for copying [size: {}]", srcFullPath.toString(), srcFullPath.toFile().length());
                  FileUtil.nioBufferCopy(srcFullPath.toFile(), destPath.toFile());
               }
            }
         }
      }
   }

   private boolean shouldCopyFile(Path srcPath, Path destPath) {
      boolean shouldCopy = true;
      final File destFile = destPath.toFile();

      if (destFile.exists()) {
         final BasicFileAttributes srcBfa = FileUtil.getFileAttributes(srcPath);
         final BasicFileAttributes destBfa = FileUtil.getFileAttributes(destPath);

         // If source file is older destination file then we do not need to copy file,
         // because
         // file has already been copied.
         if (srcBfa.lastModifiedTime().toMillis() < destBfa.lastModifiedTime().toMillis()) {
            LOG.debug("File [{}] will not be copied to [{}] because destination file has newer timestamp.", srcPath.toString(), destPath.toString());
            shouldCopy = false;
         }
      }
      return shouldCopy;
   }

   private Path getFullPath(WatchKey queuedKey, WatchEvent<?> watchEvent) {
      Path watchEventFile = (Path) watchEvent.context();
      Path watchEventFolder = (Path) queuedKey.watchable();

      return watchEventFolder.resolve(watchEventFile);
   }

   private Path getDestPath(WatchKey queuedKey, WatchEvent<?> watchEvent) {
      // this is not a complete path
      Path file = (Path) watchEvent.context();
      // need to get parent path
      SwarmConfig swarmConfig = keySwarmConfigMap.get(queuedKey);
      Path parentPath = swarmConfig.getTargetPath().toPath();

      return parentPath.resolve(file);
   }

   private WatchService createDefaultWatchService() throws IOException {
      return FileSystems.getDefault().newWatchService();

   }

   private void registerFolders(WatchService watchService, SwarmConfig[] swarmConfigs) throws IOException {
      for (SwarmConfig swarmConfig : swarmConfigs) {
         File srcFolder = swarmConfig.getSourcePath();
         if (srcFolder.exists() && srcFolder.isDirectory()) {
            Path path = srcFolder.toPath();
            WatchKey key = path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY,
                                         StandardWatchEventKinds.ENTRY_DELETE);
            keySwarmConfigMap.put(key, swarmConfig);
         } else {
            throw new IOException("Folder [" + srcFolder.getAbsolutePath() + "] does not exist!");
         }
      }
   }
}
