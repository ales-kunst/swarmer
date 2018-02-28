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
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.zip.CRC32;

public class FolderChangesWatcher {

   private static final Logger LOG = LogManager.getLogger(FolderChangesWatcher.class);

   private FolderChangesContext ctx;

   public FolderChangesWatcher() {
      ctx = new FolderChangesContext();
   }

   private void copyFileOnModify(Path srcPath, Path destPath) throws IOException {
      boolean canSrcFileBeLocked = FileUtil.canObtainExclusiveLock(srcPath);
      // Dest file should not be present because it should be deleted when source file is created
      boolean destFileNotExistent    = !destPath.toFile().exists();
      boolean fileAccessConditionsOk = canSrcFileBeLocked && destFileNotExistent;

      LOG.trace("Entering copyFileOnModify");

      if (fileAccessConditionsOk && shouldCopyFile(srcPath, destPath)) {
         LOG.info("File [{}] ready for copying [size: {}]", srcPath.toString(),
                  srcPath.toFile().length());
         // Copy file
         FileUtil.nioBufferCopy(srcPath.toFile(), destPath.toFile());
      } else {
         LOG.info("File not copied because one of following happened: " +
                  "Dest. file exists, Src. file can not be locked, Dest. file newer then src. file.");
      }
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
      Path    srcPath      = getFullPath(queuedKey, watchEvent);
      Path    destPath     = getDestPath(queuedKey, watchEvent);
      boolean fileCrcsSame = true;

      boolean destFileExists = destPath.toFile().exists();

      if (destFileExists) {
         LOG.trace("Calculating Crc32 for [{}]", srcPath);
         CRC32 srcFileCrc = FileUtil.calculateCrc32(srcPath);
         LOG.trace("Calculating Crc32 for [{}]", destPath);
         CRC32 destFileCrc = FileUtil.calculateCrc32(destPath);
         fileCrcsSame = (srcFileCrc.getValue() == destFileCrc.getValue());

         if (!fileCrcsSame) {
            LOG.warn("Files crc are not the same - [Src: [{}] Crc: [{}]] -> [Dest: [{}] Crc: [{}]]. Retry copy.",
                     srcPath, srcFileCrc.getValue(), destPath, destFileCrc.getValue());
            if (FileUtil.removeFile(destPath)) {
               copyFileOnModify(srcPath, destPath);
            } else {
               LOG.trace("File [{}] can not be deleted.", destPath);
               // TODO Implement error signal not copied file is not in consistent state.
            }
         } else {
            LOG.trace("Files crc are the same for now - [Src: [{}] Crc: [{}]] -> [Dest: [{}] Crc: [{}]].",
                      srcPath, srcFileCrc.getValue(), destPath, destFileCrc.getValue());
         }
      } else {
         copyFileOnModify(srcPath, destPath);
         // TODO Implement thread sleep because it can happen (copying over network) that the file is not in consistent state
      }
   }

   private void processWatchEvents(WatchKey queuedKey) throws IOException {
      for (WatchEvent<?> watchEvent : queuedKey.pollEvents()) {
         Path srcPath = getFullPath(queuedKey, watchEvent);

         LOG.trace("Event... kind={}, count={}, context={} path={}", watchEvent.kind(), watchEvent.count(),
                   watchEvent.context(),
                   srcPath.toFile().getAbsolutePath());

         String  srcFilename        = srcPath.toFile().getName();
         boolean isFile             = srcPath.toFile().isFile();
         boolean fileMatchesPattern = ctx.getSwarmConfig(queuedKey).matchesFilePattern(srcFilename);
         boolean isValidFile        = isFile && fileMatchesPattern;
         boolean isValidCreateEvent = (watchEvent.kind() == StandardWatchEventKinds.ENTRY_CREATE) && isValidFile;
         boolean isValidModifyEvent = (watchEvent.kind() == StandardWatchEventKinds.ENTRY_MODIFY) && isValidFile;

         if (isValidCreateEvent) {
            Path destPath = getDestPath(queuedKey, watchEvent);
            // We have to remove target file here because we know that we have to copy source file into target file.
            // In case the timestamp is preserved on source file (when copying) then it can happen that we would in
            // modify event have always file with older timestamp as the one in target folder
            if (FileUtil.canObtainExclusiveLock(destPath)) {
               FileUtil.removeFile(destPath);
            } else {
               LOG.error("Destination file [{}] is locked by another process.", destPath);
            }
         } else if (isValidModifyEvent) {
            processModifyEvent(queuedKey, watchEvent);
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

   private boolean shouldCopyFile(Path srcPath, Path destPath) {
      boolean    shouldCopy = true;
      final File destFile   = destPath.toFile();

      if (destFile.exists()) {
         final BasicFileAttributes srcBfa      = FileUtil.getFileAttributes(srcPath);
         final BasicFileAttributes destBfa     = FileUtil.getFileAttributes(destPath);
         final FileTime            srcTimeCre  = srcBfa.creationTime();
         final FileTime            srcTimeMod  = srcBfa.lastModifiedTime();
         final FileTime            srcTimeAcc  = srcBfa.lastAccessTime();
         final FileTime            destTimeCre = destBfa.creationTime();
         final FileTime            destTimeMod = destBfa.lastModifiedTime();
         final FileTime            destTimeAcc = destBfa.lastAccessTime();

         StringBuffer sb = new StringBuffer();
         sb.append(String.format("\nSrc CT: %s - Dest CT: %s\n", srcBfa.creationTime(), destBfa.creationTime()))
           .append(String.format("Src LMT: %s - Dest LMT: %s\n", srcBfa.lastModifiedTime(), destBfa.lastModifiedTime()))
           .append(String.format("Src LAT: %s - Dest LAT: %s\n", srcBfa.lastAccessTime(), destBfa.lastAccessTime()));

         LOG.trace(sb.toString());

         // If source file is older destination file then we do not need to copy file,
         // because
         // file has already been copied.
         if (srcBfa.lastAccessTime().toMillis() < destBfa.lastAccessTime().toMillis()) {
            LOG.debug("File [{}] will not be copied to [{}] because destination file has newer access time.",
                      srcPath.toString(), destPath.toString());
            shouldCopy = false;
         }
      }
      return shouldCopy;
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
