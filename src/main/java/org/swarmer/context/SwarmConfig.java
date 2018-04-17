package org.swarmer.context;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ini4j.Ini;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SwarmConfig {

   public static final String BLUE_JAVA_PARAMS          = "blue.jvm.params";
   public static final String CONSUL_SERVICE_HEALTH_URL = "consul.service.health.url";
   public static final String FILE_PATTERN              = "file.pattern";
   public static final String GREEN_JAVA_PARAMS         = "green.jvm.params";
   public static final String SECTION_NAME              = "section.name";
   public static final String SOURCE_PATH               = "src.folder";
   public static final String TARGET_PATH               = "dest.folder";

   private static final Logger     LOG = LogManager.getLogger(SwarmConfig.class);
   private final        File       destPath;
   private final        Properties properties;
   private final        File       srcPath;

   private SwarmConfig(Builder builder) {
      this.properties = builder.properties;
      this.srcPath = new File(getSrcPathFromIni());
      this.destPath = new File(getDestPathFromIni());
   }

   private String getSrcPathFromIni() {
      return properties.getProperty(SOURCE_PATH);
   }

   private String getDestPathFromIni() {
      return properties.getProperty(TARGET_PATH);
   }

   public String getBlueJvmParams() { return properties.getProperty(BLUE_JAVA_PARAMS); }

   public String getConsulHealthServiceUrl() {
      return properties.getProperty(CONSUL_SERVICE_HEALTH_URL);
   }

   public String getFilenamePattern() {
      return properties.getProperty(FILE_PATTERN);
   }

   public String getGreenJvmParams() {
      return properties.getProperty(GREEN_JAVA_PARAMS);
   }

   public File getSourcePath() {
      return srcPath;
   }

   public File getTargetPath() {
      return destPath;
   }

   public boolean matchesFilePattern(String fileName) {
      String  pattern = getFilenamePattern();
      boolean matches = fileName.matches(pattern);
      LOG.trace("Using pattern {} on filename {} [match: {}].", pattern, fileName, matches);
      return matches;
   }

   public String getName() { return properties.getProperty(SECTION_NAME); }

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

   public static class Builder {
      Properties  properties;
      Ini.Section section;

      public Builder(Ini.Section section) {
         this();
         this.section = section;
      }

      public Builder() {
         properties = new Properties();
         section = null;
      }

      public SwarmConfig build() throws IOException {
         properties.load(new StringReader(""));
         if ((section != null) && (section.getName() != null)) {
            setSectionName(section.getName());
         }
         setValue(SwarmConfig.SOURCE_PATH);
         setValue(SwarmConfig.TARGET_PATH);
         setValue(SwarmConfig.FILE_PATTERN);
         setValue(SwarmConfig.BLUE_JAVA_PARAMS);
         setValue(SwarmConfig.GREEN_JAVA_PARAMS);
         setValue(SwarmConfig.CONSUL_SERVICE_HEALTH_URL);
         return new SwarmConfig(this);
      }

      public Builder setSectionName(String value) {
         properties.setProperty(SwarmConfig.SECTION_NAME, value);
         return this;
      }

      private void setValue(String key) {
         boolean sectionValid = (section != null) && section.containsKey(key);
         if (sectionValid && (properties.getProperty(key) == null)) {
            properties.setProperty(key, section.get(key));
         }
      }

      public Builder setBlueJvmParams(String value) {
         properties.setProperty(SwarmConfig.BLUE_JAVA_PARAMS, value);
         return this;
      }

      public Builder setConsulServiceHeatlhUrl(String value) {
         properties.setProperty(SwarmConfig.CONSUL_SERVICE_HEALTH_URL, value);
         return this;
      }

      public Builder setFilePattern(String value) {
         properties.setProperty(SwarmConfig.FILE_PATTERN, value);
         return this;
      }

      public Builder setGreenJvmParams(String value) {
         properties.setProperty(SwarmConfig.GREEN_JAVA_PARAMS, value);
         return this;
      }

      public Builder setSourcePath(String value) {
         properties.setProperty(SwarmConfig.SOURCE_PATH, value);
         return this;
      }

      public Builder setTargetPath(String value) {
         properties.setProperty(SwarmConfig.TARGET_PATH, value);
         return this;
      }
   }
}
