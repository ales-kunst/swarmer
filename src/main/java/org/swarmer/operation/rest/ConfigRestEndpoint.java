package org.swarmer.operation.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.rapidoid.annotation.Controller;
import org.rapidoid.annotation.GET;
import org.rapidoid.annotation.Param;
import org.swarmer.MainExecutor;
import org.swarmer.SwarmerInputParams;
import org.swarmer.context.CfgCreator;
import org.swarmer.context.CtxElementVisitor;
import org.swarmer.context.SwarmerCtxManager;
import org.swarmer.exception.ExceptionThrower;
import org.swarmer.json.SwarmerCfg;

import java.io.File;
import java.nio.charset.StandardCharsets;

@Controller("/config")
public class ConfigRestEndpoint {
   private static final ObjectMapper JSON_MAPPER        = new ObjectMapper();
   private static final Object       RELOAD_CONFIG_LOCK = new Object();

   @GET("/dump")
   public String dumpConfigToFile(@Param("file") String file) throws Exception {
      SwarmerCfg cfgObj   = SwarmerCtxManager.instance().getCtxCfg();
      String     filename = file != null ? file : "config_dump_" + System.currentTimeMillis() + ".json";
      File       cfgFile  = getConfigFile(filename);

      String json = JSON_MAPPER.writeValueAsString(cfgObj);

      FileUtils.write(cfgFile, json, StandardCharsets.UTF_8.name());

      return String.format("OK [%s]", cfgFile.getAbsolutePath());
   }

   private File getConfigFile(String file) {
      File cfgDir = SwarmerInputParams.getJsonFileFolder();
      return new File(cfgDir, file);
   }

   @GET("/path")
   public String getConfigFilePath() {
      return SwarmerInputParams.jsonAbsoluteFilePath();
   }

   @GET("/")
   public SwarmerCfg getCtxConfig() throws Exception {
      return SwarmerCtxManager.instance().getCtxCfg();
   }

   @GET("/reload")
   public String reloadConfig(@Param("file") String file) throws Exception {
      synchronized (RELOAD_CONFIG_LOCK) {
         File cfgFile = getConfigFile(file);
         if (!cfgFile.exists()) {
            String msg = String.format("ERROR: File [%s] does not exist!" + cfgFile.getAbsolutePath());
            ExceptionThrower.throwIllegalArgumentException(msg);
         }
         SwarmerCtxManager.reset(cfgFile.getAbsolutePath());
         MainExecutor.instance().restartOperations(SwarmerCtxManager.instance().getCtx());

         return "OK";
      }
   }
}
