package org.swarmer.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Random;
import java.util.zip.CRC32;

public class FileUtil {

   private static final Logger LOG = LogManager.getLogger(FileUtil.class);

   private static boolean bufferedStreamsCopy(Path source, Path target) {
      boolean      isCopySuccess = false;
      InputStream  fileIn        = null;
      OutputStream fileOut       = null;

      if (source == null || target == null) {
         LOG.warn("Can not copy file if source or target is null!");
         return isCopySuccess;
      }

      File inFile  = source.toFile();
      File outFile = target.toFile();

      try {
         fileIn = new BufferedInputStream(new FileInputStream(inFile));
         fileOut = new BufferedOutputStream(new FileOutputStream(outFile));

         int    dataRead = 0;
         byte[] buffer   = new byte[1024 * 65];
         while ((dataRead = fileIn.read(buffer)) != -1) {
            fileOut.write(dataRead);
         }
      } catch (Exception e) {
         LOG.error("Error at bufferedStreamsCopy when copying file [{} -> {}]: ", inFile.getAbsolutePath(),
                   outFile.getAbsolutePath(), e);
      } finally {
         boolean closeFileIn  = close(fileIn);
         boolean closeFileOut = close(fileOut);
         isCopySuccess = closeFileIn && closeFileOut;
      }

      return isCopySuccess;
   }

   public static CRC32 calculateCrc32(Path sourceFile) {
      CRC32           resultCrc32   = new CRC32();
      FileInputStream inStream      = null;
      FileChannel     fileInChannel = null;
      try {
         if (sourceFile.toFile().exists()) {
            int    bytesRead = 0;
            byte[] buffer    = new byte[1024 * 1024];
            inStream = new FileInputStream(sourceFile.toFile());
            fileInChannel = inStream.getChannel();

            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
            while ((bytesRead = fileInChannel.read(byteBuffer)) != -1) {
               resultCrc32.update(buffer, 0, bytesRead);
               byteBuffer.clear();
            }
         }
      } catch (IOException ioe) {
         close(fileInChannel);
         close(inStream);
         LOG.error("Error when calculateCrc32: {}", ioe);
      }

      return resultCrc32;
   }

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
            File   tmpFile         = new File(sourceFile.getParent() + File.pathSeparator + tmpFileName);
            canObtainExclusiveLock = sourceFile.renameTo(tmpFile);
            canObtainExclusiveLock = canObtainExclusiveLock && tmpFile.renameTo(sourceFile);
            if (canObtainExclusiveLock) {
               LOG.debug("File [{}] can be exclusively locked.", sourceFile.getAbsolutePath());
            } else {
               LOG.debug("File [{}] can NOT be exclusively locked.", sourceFile.getAbsolutePath());
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

   public static BasicFileAttributes getFileAttributes(Path path) {
      BasicFileAttributes result_bfa = null;
      try {
         result_bfa = Files.readAttributes(path, BasicFileAttributes.class);
      } catch (IOException e) {
         LOG.error("Error when getFileAttributes for path [{}]: {}", path.toString(), e);
      }

      return result_bfa;
   }

   public static boolean isFileLocked(Path path) throws IOException {
      boolean isLocked = true;
      if (path != null && path.toFile().exists()) {
         RandomAccessFile rf          = null;
         FileChannel      fileChannel = null;
         FileLock         lock        = null;
         try {
            rf = new RandomAccessFile(path.toFile(), "rw");
            fileChannel = rf.getChannel();
            LOG.debug("Acquring lock for file [{}]!", path.toFile().getAbsolutePath());
            lock = fileChannel.tryLock();
            if (lock != null) {
               LOG.debug("Lock successful acquired for file [{}]!", path.toFile().getAbsolutePath());
               isLocked = false;
               lock.release();
            } else {
               LOG.debug("Lock unsuccessful for file [{}]!", path.toFile().getAbsolutePath());
            }
         } catch (IOException e) {
            LOG.error("Error in isFileLocked for file [{}]: {}", path.toFile().getAbsolutePath(), e);
         } finally {
            close(fileChannel);
            close(rf);

         }
      } else {
         LOG.warn("When acquring lock the file [{}] was not found!", path.toFile().getAbsolutePath());
         isLocked = false;
      }

      return isLocked;
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
         LOG.info("Started copying file [{} -> {}]", source.getAbsolutePath(), target.getAbsolutePath());
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
         LOG.info("Ended copying file [{} -> {}]", source.getAbsolutePath(), target.getAbsolutePath());
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
         LOG.debug("Successfully removed path [{}]: {}", source.toString());
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
