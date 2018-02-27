package org.swarmer.context;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ini4j.Ini;

import java.io.File;

public class SwarmConfig {

   public static final  String SOURCE_PATH  = "src.folder";
   public static final  String TARGET_PATH  = "dest.folder";
   public static final  String FILE_PATTERN = "file.pattern";
   private static final Logger LOG          = LogManager.getLogger(SwarmConfig.class);

   private final Ini.Section section;
   private final File        srcPath;
   private final File        destPath;

   public SwarmConfig(Ini.Section section) {
      this.section = section;
      this.srcPath = new File(getSrcPathFromIni());
      this.destPath = new File(getDestPathFromIni());
   }

   public String getName() {
      return section.getName();
   }

   public final File getSourcePath() {
      return srcPath;
   }

   public final File getTargetPath() {
      return destPath;
   }

   public final boolean matchesFilePattern(String fileName) {
      String  pattern = getFilenamePatternFromIni();
      boolean matches = fileName.matches(pattern);
      LOG.debug("Using pattern {} on filename {} [match: {}].", pattern, fileName, matches);
      return matches;
   }

   private String getSrcPathFromIni() {
      return section.get(SOURCE_PATH);
   }

   private String getDestPathFromIni() {
      return section.get(TARGET_PATH);
   }

   private String getFilenamePatternFromIni() {
      return section.get(FILE_PATTERN);
   }
}
