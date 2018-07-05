package org.swarmer.util;

import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.Random;

public class FileUtil {
   public static final  String KILL_APP_PATH    = System.getProperty("java.io.tmpdir") + "\\windows-kill.exe";
   public static final  String WIN_TEE_APP_PATH = System.getProperty("java.io.tmpdir") + "\\wintee.exe";
   private static final Logger LOG              = LoggerFactory.getLogger(FileUtil.class);

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
               LOG.warn("File [{}] COULD NOT BE EXCLUSIVELY LOCKED.", sourceFile.getAbsolutePath());
            }
         } catch (Exception e) {
            LOG.error("Error when canObtainExclusiveLock: {}", e);
         }
      }

      return canObtainExclusiveLock;
   }

   public static boolean copyFile(File source, File target) {
      boolean isCopySuccess = false;
      /*
      boolean          isCopySuccess = false;
      FileInputStream  inStream      = null;
      FileOutputStream outStream     = null;
      FileChannel      inChannel     = null;
      FileChannel      outChannel    = null;
      */
      if (source == null || target == null) {
         LOG.warn("Can not copy file if source or target is null!");
         return isCopySuccess;
      }

      try {
         LOG.info("|---> Started copying file [{} -> {}]", source.getAbsolutePath(), target.getAbsolutePath());
         /*
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
         */
         FileUtils.copyFile(source, target);
         LOG.info("--->| Ended copying file [{} -> {}]", source.getAbsolutePath(), target.getAbsolutePath());
         isCopySuccess = true;
      } catch (IOException e) {
         LOG.warn("Error at copyFile when copying file [{} -> {}]: ", source.getAbsolutePath(),
                  target.getAbsolutePath(), e);
      } finally {
         /*
         CloseableUtil.close(inStream);
         CloseableUtil.close(outStream);
         CloseableUtil.close(inChannel);
         CloseableUtil.close(outChannel);
         */
      }

      return isCopySuccess;
   }

   public static boolean copyWinTeeAppToTmp() {
      return copyAppToTmp("/apps/wintee.exe", WIN_TEE_APP_PATH);
   }

   private static boolean copyAppToTmp(String resourcePath, String targetpath) {
      boolean      success   = true;
      InputStream  inStream  = null;
      OutputStream outStream = null;
      try {
         inStream = FileUtil.class.getResourceAsStream(resourcePath);
         outStream = new FileOutputStream(targetpath);
         byte[] buffer = new byte[1024];
         int    length;
         while ((length = inStream.read(buffer)) > 0) {
            outStream.write(buffer, 0, length);
         }
      } catch (Exception e) {
         LOG.debug("Error executing copyAppToTmp: {}", e);
         success = false;
      }
      CloseableUtil.close(inStream);
      CloseableUtil.close(outStream);
      return success;
   }

   public static boolean copyWindowsKillAppToTmp() {
      return copyAppToTmp("/apps/windows-kill.exe", KILL_APP_PATH);
   }

   public static boolean forceRemoveFile(Path source) {
      return forceRemoveFile(source.toFile());
   }

   public static boolean forceRemoveFile(File file) {
      boolean deleted = false;
      try {
         FileDeleteStrategy.FORCE.delete(file);
         LOG.debug("File [{}] could SUCCESSFULLY removed.", file.getAbsolutePath());
         deleted = true;
      } catch (IOException ioe) {
         LOG.warn("File [{}] could NOT be removed:\n{}", file.getAbsolutePath(), ExceptionUtils.getFullStackTrace(ioe));
      }

      return deleted;
   }

   public static boolean matchesFilePattern(String fileName, String pattern) {
      boolean matches = fileName.matches(pattern);
      LOG.debug("Using pattern {} on filename {} [match: {}].", pattern, fileName, matches);
      return matches;
   }

   public static boolean winTeeAppExists() {
      File winTeeFile = new File(WIN_TEE_APP_PATH);
      return winTeeFile.exists();
   }

   public static boolean windowsKillAppExists() {
      File windowsKillFile = new File(KILL_APP_PATH);
      return windowsKillFile.exists();
   }

}
