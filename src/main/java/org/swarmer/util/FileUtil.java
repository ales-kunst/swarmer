package org.swarmer.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.watcher.FolderChangesContext;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Random;

public class FileUtil {

   private static final Logger LOG = LogManager.getLogger(FileUtil.class);

   public static boolean canObtainExclusiveLock(Path source) {
      boolean canObtainExclusiveLock = false;
      File    sourceFile             = source != null ? source.toFile() : null;

      if (source == null) {
         LOG.error("Can not obtain lock on null source in canObtainExclusiveLock.");
      } else {
         try {
            Random randomGenerator = new Random();
            int    randomInt       = randomGenerator.nextInt(100000);
            String tmpFileName     = String.format("%d.tmp_%d", System.currentTimeMillis(), randomInt);
            File   tmpFile         = new File(sourceFile.getParent() + File.separator + tmpFileName);
            canObtainExclusiveLock = sourceFile.renameTo(tmpFile) && tmpFile.renameTo(sourceFile);

            if (canObtainExclusiveLock) {
               LOG.info("File [{}] EXCLUSIVELY LOCKED.", sourceFile.getAbsolutePath());
            } else {
               LOG.warn("File [{}] NOT EXCLUSIVELY LOCKED.", sourceFile.getAbsolutePath());
            }
         } catch (Exception e) {
            LOG.error("Error when canObtainExclusiveLock: {}", e);
         }
      }


      return canObtainExclusiveLock;
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

   public static boolean nioBufferCopy(File source, File target) {
      boolean          isCopySuccess = false;
      FileInputStream  inStream      = null;
      FileOutputStream outStream     = null;
      FileChannel      inChannel     = null;
      FileChannel      outChannel    = null;

      if (source == null || target == null) {
         LOG.warn("Can not copy file if source or target is null!");
         return isCopySuccess;
      }

      try {
         LOG.info("*** Started copying file [{} -> {}] ***", source.getAbsolutePath(), target.getAbsolutePath());
         inStream = new FileInputStream(source);
         outStream = new FileOutputStream(target);
         inChannel = inStream.getChannel();
         outChannel = outStream.getChannel();

         ByteBuffer buffer = ByteBuffer.allocateDirect(8192);
         while (inChannel.read(buffer) != -1) {
            buffer.flip();
            while (buffer.hasRemaining()) {
               outChannel.write(buffer);
            }
            buffer.clear();
         }
         LOG.info("*** Ended copying file [{} -> {}] ***", source.getAbsolutePath(), target.getAbsolutePath());
         isCopySuccess = true;
      } catch (IOException e) {
         LOG.error("Error at nioBufferCopy when copying file [{} -> {}]: ", source.getAbsolutePath(),
                   target.getAbsolutePath(), e);
      } finally {
         close(inStream);
         close(outStream);
         close(inChannel);
         close(outChannel);
      }

      return isCopySuccess;
   }

   public static boolean removeFile(Path source) {
      boolean success = false;
      try {
         success = Files.deleteIfExists(source);
         LOG.debug("Successfully removed path [{}].", source.toString());
      } catch (NoSuchFileException e) {
         LOG.error("No such file in removeFile for path [{}]: {}", source.toString(), e);
      } catch (DirectoryNotEmptyException e) {
         LOG.error("Folder not empty in removeFile for path [{}]: {}", source.toString(), e);
      } catch (IOException e) {
         LOG.error("Error in removeFile for path [{}]: {}", source.toString(), e);
      }

      return success;
   }

}
