package org.swarmer.util;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileUtil {

   private static final Logger LOG = LogManager.getLogger(FileUtil.class);

   public static boolean isFileLocked(Path path) throws IOException {
      boolean isLocked = true;
      if (path != null && path.toFile().exists()) {
         RandomAccessFile rf = null;
         FileChannel fileChannel = null;
         FileLock lock = null;
         try {
            rf = new RandomAccessFile(path.toFile(), "rw");
            fileChannel = rf.getChannel();
            LOG.warn("Acquring lock on the file [{}]!", path.toFile().getAbsolutePath());
            lock = fileChannel.tryLock();
            if (lock != null && lock.isValid()) {
               isLocked = false;
            }
         } catch (IOException e) {
            LOG.error("Error in isFileLocked for file [{}]: {}", path.toFile().getAbsolutePath(), e);
         } finally {
            if (lock != null) {
               lock.release();
            }
            if (fileChannel != null) {
               close(fileChannel);
            }
            if (rf != null) {
               close(rf);
            }
         }
      } else {
         LOG.warn("When acquring lock the file [{}] was not found!", path.toFile().getAbsolutePath());
         isLocked = false;
      }

      return isLocked;
   }

   @SuppressWarnings("resource")
   public static boolean nioBufferCopy(File source, File target) {
      boolean isCopySuccess = false;
      FileChannel in = null;
      FileChannel out = null;
      
      if (source == null || target == null) {
         LOG.warn("Can not copy file if source or target is null!");
         return isCopySuccess;
      }
      
      try {
         LOG.debug("Started copying file [{} -> {}]", source.getAbsolutePath(), target.getAbsolutePath());
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
         LOG.debug("Ended copying file [{} -> {}]", source.getAbsolutePath(), target.getAbsolutePath());
         isCopySuccess = true;
      } catch (IOException e) {
         LOG.error("Error at nioBufferCopy when copying file [{} -> {}]: ", source.getAbsolutePath(), target.getAbsolutePath(), e);
      } finally {
         close(in);
         close(out);
      }
      
      return isCopySuccess;
   }

   /**
    * Closes closeable object.
    * 
    * @param closable
    * @return Returns true if closing was successful, otherwise false.
    */
   public static boolean close(Closeable closable) {
      boolean isSuccessfull = false;
      if (closable != null) {
         try {
            closable.close();
            isSuccessfull = true;
         } catch (IOException e) {
            LOG.error("Error when closing [{}]: {}", closable.getClass().getName(), e);
         }
      }
      return isSuccessfull;
   }

}
