package org.swarmer.context;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ini4j.Ini;
import org.ini4j.Profile.Section;

// Check: http://www.logicbig.com/tutorials/core-java-tutorial/java-nio/java-watch-service/
public class SwarmerContextRetriever {
   
   private static final Logger LOG = LogManager.getLogger(SwarmConfig.class);
   
   public static final String DEFAULT_SECTION_NAME = "default";

   /**
    * @param iniFilePathname
    * @throws IOException
    */
   public static SwarmerContext retrieve(String iniFilePathname) throws IOException {
      File iniFile = new File(iniFilePathname);
      
      if (!iniFile.exists()) {
         String errorMsg = String.format("Ini file [%s] does not exist.", iniFilePathname);
         LOG.error(errorMsg);
         throw new IOException(errorMsg);
      }
      
      LOG.info("Reading configuration from {} file.", iniFilePathname);
      Ini ini = new Ini(iniFile);
      SwarmerContext.Builder swarmerBuilder = SwarmerContext.newBuilder();
      for (Map.Entry<String, Section> entry : ini.entrySet()) {
         Ini.Section section = ini.get(entry.getKey());
         // In case we encounter [default] section we fill the special field in SwarmerContext
         if (DEFAULT_SECTION_NAME.equalsIgnoreCase(section.getName())) {
            swarmerBuilder.setDefaultSection(section);
         } else {
            SwarmInstanceData swarmInstance = new SwarmInstanceData(new SwarmConfig(section)); 
            swarmerBuilder.addSwarmInstanceData(swarmInstance);
         } 
      }
      SwarmerContext.reset(swarmerBuilder.build());
      return SwarmerContext.instance();
   }

   private SwarmerContextRetriever() {
   }
}
