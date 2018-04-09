package org.swarmer.util;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class FileUtilTest {

   @Test
   public void testCopyWindowsKillAppToTmp() {
      Assert.assertTrue("Windows Kill App was not copied!", FileUtil.copyWindowsKillAppToTmp());
      Assert.assertTrue("Windows Kill App was not copied to tmp folder!", FileUtil.windowsKillAppExists());
      File windowsKillAppFile = new File(FileUtil.KILL_APP_PATH);
      Assert.assertTrue("Windows Kill App was not deleted!", windowsKillAppFile.delete());
   }

   @Test
   public void testCopyWinTeeAppToTmp() {
      Assert.assertTrue("Wintee App was not copied!", FileUtil.copyWinTeeAppToTmp());
      Assert.assertTrue("Wintee App was not copied to tmp folder!", FileUtil.winTeeAppExists());
      File winTeeAppFile = new File(FileUtil.WIN_TEE_APP_PATH);
      Assert.assertTrue("Wintee App was not deleted!", winTeeAppFile.delete());
   }

   @Test
   public void testWindowsKillAppExists() {
      File windowsKillAppFile = new File(FileUtil.KILL_APP_PATH);
      windowsKillAppFile.delete();
      Assert.assertFalse("Windows Kill App exists in tmp folder!", FileUtil.windowsKillAppExists());
   }

   @Test
   public void testWinTeeAppExists() {
      File winTeeAppFile = new File(FileUtil.WIN_TEE_APP_PATH);
      winTeeAppFile.delete();
      Assert.assertFalse("Wintee App exists in tmp folder!", FileUtil.winTeeAppExists());
   }
}