package org.swarmer.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.SwarmerInputParams;
import org.swarmer.exception.ExceptionThrower;
import org.swarmer.exception.ValidationException;
import org.swarmer.json.SwarmerCfg;

import java.io.File;
import java.io.IOException;

public class SwarmerCtxManager {
   private static final Object            CTX_LOCK      = new Object();
   private static final SwarmerCtxManager CTX_MANAGER   = new SwarmerCtxManager();
   private static final Logger            LOG           = LogManager.getLogger(SwarmerCtxManager.class);
   private static final ObjectMapper      OBJECT_MAPPER = new ObjectMapper();
   private              SwarmerCtx        ctx;

   public static SwarmerCtxManager instance() {
      return CTX_MANAGER;
   }

   public static SwarmerCtxManager on(String jsonPathname) throws IOException, ValidationException {
      if (CTX_MANAGER.getCtx() != null) {
         ExceptionThrower.throwValidationException("Context is already set. Try to use reset method!");
      }
      SwarmerCtx ctx = CTX_MANAGER.retrieve(jsonPathname);
      CTX_MANAGER.setCtx(ctx);
      return CTX_MANAGER;
   }

   public SwarmerCtx getCtx() {
      synchronized (CTX_LOCK) {
         return ctx;
      }
   }

   private void setCtx(SwarmerCtx ctx) {
      synchronized (CTX_LOCK) {
         this.ctx = ctx;
      }
   }

   private SwarmerCtx retrieve(String jsonPathname) throws IOException, ValidationException {
      File jsonFile = new File(jsonPathname);

      if (!jsonFile.exists()) {
         String errorMsg = String.format("Json file [%s] does not exist.", jsonPathname);
         LOG.error(errorMsg);
         throw new IOException(errorMsg);
      }
      LOG.info("Reading configuration from {} file.", jsonPathname);
      SwarmerCfg swarmerCfg = OBJECT_MAPPER.readerFor(SwarmerCfg.class).readValue(jsonFile);
      return SwarmerCtx.newBuilder(swarmerCfg).build();
   }

   public static SwarmerCtxManager reset(String jsonPathname) throws IOException, ValidationException {
      SwarmerCtx ctx = CTX_MANAGER.retrieve(jsonPathname);
      CTX_MANAGER.setCtx(ctx);
      SwarmerInputParams.resetJsonAbsolutePath(jsonPathname);
      return CTX_MANAGER;
   }

   /**
    * Default constructor
    */
   private SwarmerCtxManager() {
      ctx = null;
   }

   public SwarmerCfg getCtxCfg() throws CloneNotSupportedException {
      SwarmerCfg resultCtxCfg = null;
      synchronized (CTX_LOCK) {
         if (ctx != null) {
            resultCtxCfg = ctx.getSwarmerCfg();
         }
      }
      return resultCtxCfg;
   }
}
