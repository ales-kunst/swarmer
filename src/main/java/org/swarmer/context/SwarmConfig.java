package org.swarmer.context;

import org.ini4j.Ini;

public class SwarmConfig {
   
   public static final String SOURCE_PATH = "src.folder";
   public static final String TARGET_PATH = "dest.folder";
   public static final String FILENAME = "filename";
   
   private final Ini.Section section;
   
   public SwarmConfig(Ini.Section section) {
      this.section = section;
   }
   
   public String getName() {
      return section.getName();
   }

   public String getSourcePath() {
      return section.get(SOURCE_PATH);
   }
   
   public String getTargetPath() {
      return section.get(TARGET_PATH);
   }
   
   public String getFilename() {
      return section.get(FILENAME);
   }
}
