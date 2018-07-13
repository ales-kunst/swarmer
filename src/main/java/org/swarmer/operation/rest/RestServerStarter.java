package org.swarmer.operation.rest;

import com.fizzed.rocker.Rocker;
import com.fizzed.rocker.RockerOutput;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.rapidoid.http.MediaType;
import org.rapidoid.http.Req;
import org.rapidoid.http.Resp;
import org.rapidoid.setup.App;
import org.rapidoid.setup.AppBootstrap;
import org.rapidoid.setup.My;
import org.rapidoid.setup.On;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swarmer.context.SwarmerCtx;
import org.swarmer.operation.DefaultOperation;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

public class RestServerStarter extends DefaultOperation<SwarmerCtx, Boolean> {
   public static final  String   OP_NAME = "REST Server";
   private static final Logger   LOG     = LoggerFactory.getLogger(RestServerStarter.class);
   private              String[] cliArguments;

   public RestServerStarter(String name, SwarmerCtx context, String[] cliArguments) {
      super(name, context);
      this.cliArguments = cliArguments;
   }

   @Override
   public void cleanUp() {
      On.setup().shutdown();
   }

   @Override
   protected Boolean executionBock() {
      AppBootstrap bootstrap = App.run(cliArguments); // instead of App.bootstrap(args), which might start the server
      // customizing the server address and port - before the server is bootstrapped
      int port = getContext().getPort();
      On.address("0.0.0.0").port(port);
      My.errorHandler(this::handleError);
      On.get("/version").json(this::getVersionInfo);
      bootstrap.beans();
      return true;
   }

   @Override
   protected void handleError(Exception exception) {
      LOG.error("Exception from executionBlock:\n{}", ExceptionUtils.getStackTrace(exception));
      On.setup().shutdown();
   }

   private String getVersionInfo() {
      String      resultText = "";
      InputStream inStream   = RestServerStarter.class.getResourceAsStream("/version.txt");
      try {
         StringWriter writer = new StringWriter();
         IOUtils.copy(inStream, writer, StandardCharsets.UTF_8);
         resultText = writer.toString();
      } catch (IOException e) {
         // Nothing to do here
      }
      return resultText.trim();
   }

   private Object handleError(Req req, Resp resp, Throwable error) {
      RockerOutput errContent = Rocker.template("org/swarmer/views/err_content.rocker.html")
                                      .bind("status", "Error")
                                      .bind("uri", req.uri())
                                      .bind("query", req.query())
                                      .bind("error", ExceptionUtils.getStackTrace(error))
                                      .render();

      return resp.contentType(MediaType.HTML_UTF_8).result(errContent.toString()).code(500);
   }
}
