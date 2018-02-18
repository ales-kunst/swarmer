package com.swarmer.watcher;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;

import org.swarmer.context.SwarmConfig;
import org.swarmer.context.SwarmerContext;

public class FolderChangesWatcher {

   private enum CopyStatus {
                            FILE_NOT_FOUND, COPYING, FINISHED
   }

   private Map<WatchKey, Path> keyPathMap = new HashMap<WatchKey, Path>();

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
      while (true) {
         WatchKey queuedKey = watchService.take();

         for (WatchEvent<?> watchEvent : queuedKey.pollEvents()) {
            System.out.printf("Event... kind=%s, count=%d, context=%s Context type=%s%n", watchEvent.kind(), watchEvent.count(), watchEvent.context(),
                              ((Path) watchEvent.context()).getClass());

            if (watchEvent.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
               Path srcPath = getAbsolutePath(queuedKey, watchEvent);
               if (copyingFileStatus(srcPath) == CopyStatus.FINISHED) {
                  System.out.println("File [" + srcPath.toString() + "] ready for copying [size: " + srcPath.toFile().length() + "]!!!!");
                  nioBufferCopy(srcPath.toFile(), new File("D:\\Temp\\tmp\\" + watchEvent.context()));
                  System.out.println("Finished copying!!!");
               }
            }
         }
         if (!queuedKey.reset()) {
            keyPathMap.remove(queuedKey);
         }
         if (keyPathMap.isEmpty()) {
            break;
         }
      }
   }

   private static void nioBufferCopy(File source, File target) {
      FileChannel in = null;
      FileChannel out = null;

      try {
         in = new FileInputStream(source).getChannel();
         out = new FileOutputStream(target).getChannel();

         ByteBuffer buffer = ByteBuffer.allocateDirect(8192);
         while (in.read(buffer) != -1) {
            buffer.flip();

            while (buffer.hasRemaining()) {
               out.write(buffer);
            }

            buffer.clear();
         }
      } catch (IOException e) {
         e.printStackTrace();
      } finally {
         close(in);
         close(out);
      }
   }

   private static void close(Closeable closable) {
      if (closable != null) {
         try {
            closable.close();
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
   }

   private Path getAbsolutePath(WatchKey queuedKey, WatchEvent<?> watchEvent) {
      // this is not a complete path
      Path path = (Path) watchEvent.context();
      // need to get parent path
      Path parentPath = keyPathMap.get(queuedKey);
      // get complete path
      path = parentPath.resolve(path);

      return path;
   }

   private CopyStatus copyingFileStatus(Path path) {
      CopyStatus copyStatus = CopyStatus.COPYING;
      FileChannel channel = null;
      FileLock lock = null;

      if (!path.toFile().exists()) {
         return CopyStatus.FILE_NOT_FOUND;
      }

      try {
         channel = new RandomAccessFile(path.toFile(), "rw").getChannel();
         lock = channel.tryLock();
         if (lock != null && lock.isValid()) {
            copyStatus = CopyStatus.FINISHED;
         }
      } catch (IOException e) {
      } finally {
         if (lock != null) {
            try {
               lock.close();
            } catch (IOException e) {
            }
         }
         if (channel != null) {
            try {
               channel.close();
            } catch (IOException e) {
            }
         }
      }
      return copyStatus;
   }

   private WatchService createDefaultWatchService() throws IOException {
      return FileSystems.getDefault().newWatchService();

   }

   private void registerFolders(WatchService watchService, SwarmConfig[] swarmConfigs) throws IOException {
      for (SwarmConfig swarmConfig : swarmConfigs) {
         File folder = new File(swarmConfig.getSourcePath());
         if (folder.exists() && folder.isDirectory()) {
            Path path = folder.toPath();
            WatchKey key = path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY,
                                         StandardWatchEventKinds.ENTRY_DELETE);
            keyPathMap.put(key, path);
         } else {
            throw new IOException("Folder [" + folder.getAbsolutePath() + "] does not exist!");
         }
      }
   }
}
