package org.swarmer.operation.rest;

import org.rapidoid.annotation.Controller;
import org.rapidoid.annotation.GET;
import org.rapidoid.annotation.Param;
import org.swarmer.MainExecutor;
import org.swarmer.context.SwarmerCtx;
import org.swarmer.context.SwarmerCtxManager;
import org.swarmer.exception.ExceptionThrower;
import org.swarmer.json.SwarmerCfg;
import org.swarmer.operation.SaveCtxStateToFile;
import org.swarmer.util.SwarmerInputParams;

import java.io.File;

@Controller("/config")
public class ConfigRestEndpoint {
   private static final Object RELOAD_CONFIG_LOCK = new Object();

   @GET("/dump")
   public String dumpConfigToFile(@Param("file") String file) {
      SwarmerCtx ctx     = SwarmerCtxManager.instance().getCtx();
      File       cfgFile = new SaveCtxStateToFile(file, ctx).execute();

      return String.format("OK [%s]", cfgFile.getAbsolutePath());
   }

   @GET("/path")
   public String getConfigFilePath() {
      return SwarmerInputParams.jsonAbsoluteFilePath();
   }

   @GET("/")
   public SwarmerCfg getCtxConfig() {
      return SwarmerCtxManager.instance().getCtxCfg();
   }

   @GET("/reload")
   public String reloadConfig(@Param("file") String file) throws Exception {
      // This must be synchronized because we can not call this method concurrently
      synchronized (RELOAD_CONFIG_LOCK) {
         File cfgFile = SwarmerInputParams.getConfigFile(file);
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
