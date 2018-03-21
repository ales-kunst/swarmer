package org.swarmer.context;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ini4j.Ini;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SwarmConfig {

   public static final String BLUE_JAVA_PARAMS      = "blue.java.params";
   public static final String FILE_PATTERN          = "file.pattern";
   public static final String GREEN_JAVA_PARAMS     = "green.java.params";
   public static final String SOURCE_PATH           = "src.folder";
   // public static final String SWARM_BIND_ADDRESS_REGEX = "-D(swarm\\.bind\\.address=([0-9\\.]+))";
   public static final String SWARM_HTTP_PORT_REGEX = "-D(swarm\\.http\\.port=([0-9]+))";
   public static final String TARGET_PATH           = "dest.folder";

   private static final Logger LOG = LogManager.getLogger(SwarmConfig.class);
   private final File        destPath;
   private final Ini.Section section;
   private final File        srcPath;

   protected SwarmConfig() {
      this.section = null;
      this.srcPath = null;
      this.destPath = null;
   }

   public SwarmConfig(Ini.Section section) {
      this.section = section;
      this.srcPath = new File(getSrcPathFromIni());
      this.destPath = new File(getDestPathFromIni());
   }

   public String getBlueJavaParams() {
      return section.get(BLUE_JAVA_PARAMS);
   }

   public int getBlueUrlPort() {
      String portString = getJavaParamValue(getBlueJavaParams(), SWARM_HTTP_PORT_REGEX);
      int    port       = (portString == null) ? 8080 : Integer.parseInt(portString);
      return port;
   }

   public String getGreenJavaParams() {
      return section.get(GREEN_JAVA_PARAMS);
   }

   public int getGreenUrlPort() {
      String portString = getJavaParamValue(getGreenJavaParams(), SWARM_HTTP_PORT_REGEX);
      int    port       = (portString == null) ? 8080 : Integer.parseInt(portString);
      return port;
   }


   public String getName() {
      return section.getName();
   }

   public File getSourcePath() {
      return srcPath;
   }

   public File getTargetPath() {
      return destPath;
   }

   public boolean matchesFilePattern(String fileName) {
      String  pattern = getFilenamePatternFromIni();
      boolean matches = fileName.matches(pattern);
      LOG.trace("Using pattern {} on filename {} [match: {}].", pattern, fileName, matches);
      return matches;
   }

   private String getDestPathFromIni() {
      return section.get(TARGET_PATH);
   }

   private String getFilenamePatternFromIni() {
      return section.get(FILE_PATTERN);
   }

   protected String getJavaParamValue(String javaParams, String regex) {
      final int VALUE_POS = 2;
      String    result    = null;

      if ((javaParams == null) || (regex == null)) {
         return result;
      }

      final Pattern pattern = Pattern.compile(regex);
      final Matcher matcher = pattern.matcher(javaParams);

      if (matcher.find()) {
         LOG.trace("Pattern {} match in params: [{}]", regex, javaParams);
         result = matcher.group(VALUE_POS);
      } else {
         LOG.trace("Pattern {} do not match in params: [{}]", regex, javaParams);
      }
      return result;
   }

   private String getSrcPathFromIni() {
      return section.get(SOURCE_PATH);
   }
}
