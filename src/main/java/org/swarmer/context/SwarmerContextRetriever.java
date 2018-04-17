package org.swarmer.context;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ini4j.Ini;
import org.ini4j.Profile.Section;
import org.swarmer.exception.ValidationException;

import java.io.File;
import java.io.IOException;
import java.util.Map;

// Check: http://www.logicbig.com/tutorials/core-java-tutorial/java-nio/java-watch-service/
public class SwarmerContextRetriever {

   public static final String DEFAULT_SECTION_NAME = "default";

   private static final Logger LOG = LogManager.getLogger(SwarmerContextRetriever.class);

   public static SwarmerContext retrieve(String iniFilePathname) throws IOException, ValidationException {
      File iniFile = new File(iniFilePathname);

      if (!iniFile.exists()) {
         String errorMsg = String.format("Ini file [%s] does not exist.", iniFilePathname);
         LOG.error(errorMsg);
         throw new IOException(errorMsg);
      }

      LOG.info("Reading configuration from {} file.", iniFilePathname);
      Ini                    ini            = new Ini(iniFile);
      SwarmerContext.Builder swarmerBuilder = SwarmerContext.newBuilder();
      for (Map.Entry<String, Section> entry : ini.entrySet()) {
         Ini.Section section = ini.get(entry.getKey());
         // In case we encounter [default] section we fill the special field in SwarmerContext
         if (DEFAULT_SECTION_NAME.equalsIgnoreCase(section.getName())) {
            swarmerBuilder.setDefaultSection(section);
         } else {
            SwarmConfig         swarmConfig         = new SwarmConfig.Builder(section).build();
            DeploymentContainer deploymentContainer = new DeploymentContainer(swarmConfig);
            deploymentContainer.isValid();
            swarmerBuilder.addDeploymentContainer(deploymentContainer);
         }
      }
      SwarmerContext.reset(swarmerBuilder.build());
      return SwarmerContext.instance();
   }

   /**
    * Default constructor
    */
   private SwarmerContextRetriever() {
   }
}
