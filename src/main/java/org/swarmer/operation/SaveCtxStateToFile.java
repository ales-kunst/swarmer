package org.swarmer.operation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.swarmer.context.SwarmerCtx;
import org.swarmer.context.SwarmerCtxManager;
import org.swarmer.exception.ExceptionThrower;
import org.swarmer.json.SwarmerCfg;
import org.swarmer.util.SwarmerInputParams;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class SaveCtxStateToFile extends DefaultOperation<SwarmerCtx, File> {
   public static final  String       OP_NAME     = "Save Ctx State To File";
   private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
   private final        String       filename;

   public SaveCtxStateToFile(String filename, SwarmerCtx context) {
      super(OP_NAME, context);
      this.filename = filename != null ? filename : "config_dump_" + System.currentTimeMillis() + ".json";
   }

   @Override
   public void cleanUp() {

   }

   @Override
   protected File executionBock() throws Exception {
      SwarmerCfg cfgObj  = SwarmerCtxManager.instance().getCtxCfg();
      File       cfgFile = SwarmerInputParams.getConfigFile(filename);

      String json = JSON_MAPPER.writeValueAsString(cfgObj);

      FileUtils.write(cfgFile, json, StandardCharsets.UTF_8.name());
      return cfgFile;
   }

   @Override
   protected void handleError(Exception e) {
      ExceptionThrower.throwRuntimeError(e);
   }
}