package org.swarmer.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.exception.ValidationException;
import org.swarmer.json.SwarmerCfg;

import java.io.File;
import java.io.IOException;

// Check: http://www.logicbig.com/tutorials/core-java-tutorial/java-nio/java-watch-service/
public class SwarmerContextRetriever {
   private static final Logger       LOG                  = LogManager.getLogger(SwarmerContextRetriever.class);
   private static final ObjectMapper OBJECT_MAPPER        = new ObjectMapper();

   public static SwarmerContext retrieve(String jsonPathname) throws IOException, ValidationException {
      File jsonFile = new File(jsonPathname);

      if (!jsonFile.exists()) {
         String errorMsg = String.format("Json file [%s] does not exist.", jsonPathname);
         LOG.error(errorMsg);
         throw new IOException(errorMsg);
      }
      LOG.info("Reading configuration from {} file.", jsonPathname);
      SwarmerCfg swarmerCfg = OBJECT_MAPPER.readerFor(SwarmerCfg.class).readValue(jsonFile);
      return SwarmerContext.newBuilder(swarmerCfg).buildFromCfg();
   }

   /**
    * Default constructor
    */
   private SwarmerContextRetriever() {
   }
}
