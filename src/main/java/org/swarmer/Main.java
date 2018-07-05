package org.swarmer;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.hsqldb.persist.HsqlProperties;
import org.hsqldb.server.Server;
import org.hsqldb.server.ServerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swarmer.context.SwarmerCtxManager;
import org.swarmer.util.SwarmerInputParams;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;


public class Main {
   private static final int    ERROR_STATUS = 8;
   private static final Logger LOG          = LoggerFactory.getLogger(Main.class);
   private static       Server hsqlDbServer = null;

   public static void main(String[] args) {
      boolean finishedWithError = false;
      try {
         System.out.println("\n" + getInfoTxt());

         startHsqldb();
         URL logConffile = Main.class.getResource("/logback-conf.xml");
         initLogConfiguration(logConffile);

         SwarmerCtxManager.on(SwarmerInputParams.jsonAbsoluteFilePath());
         MainExecutor.cliArgs(args)
                     .initialize()
                     .startOperations()
                     .startRestServer()
                     .waitToEnd();
         stopHsqlDbServer();
         if (MainExecutor.finishedWithErrors()) {
            finishedWithError = true;
         }
      } catch (Exception e) {
         LOG.error("Swarmer ended with error:\n {}", ExceptionUtils.getStackTrace(e));
         finishedWithError = true;
      } finally {
         stopHsqlDbServer();
      }

      if (finishedWithError) {
         System.exit(ERROR_STATUS);
      }
   }

   private static String getInfoTxt() {
      String      resultText = "";
      InputStream inStream   = Main.class.getResourceAsStream("/info.txt");
      try {
         StringWriter writer = new StringWriter();
         IOUtils.copy(inStream, writer, StandardCharsets.UTF_8);
         resultText = writer.toString();
      } catch (IOException e) {}
      return resultText;
   }

   public static void startHsqldb() {
      try {
         int            hsqldbPort  = 10081;
         HsqlProperties configProps = new HsqlProperties();
         configProps.setProperty("server.port", hsqldbPort);
         configProps.setProperty("server.database.0", "file:" + SwarmerInputParams.getHsqldbPath());
         configProps.setProperty("server.dbname.0", "logdb");
         configProps.setProperty("server.remote_open", true);
         configProps.setProperty("server.daemon", true);
         ServerConfiguration.translateDefaultDatabaseProperty(configProps);
         hsqlDbServer = new Server();
         hsqlDbServer.setPort(hsqldbPort);
         hsqlDbServer.setLogWriter(null);
         hsqlDbServer.setErrWriter(null);
         hsqlDbServer.setRestartOnShutdown(false);
         hsqlDbServer.setNoSystemExit(true);
         hsqlDbServer.setProperties(configProps);
         hsqlDbServer.start();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   private static void initLogConfiguration(URL confFile) {
      LoggerContext logCtx = (LoggerContext) LoggerFactory.getILoggerFactory();

      try {
         JoranConfigurator configurator = new JoranConfigurator();
         configurator.setContext(logCtx);
         logCtx.reset();
         configurator.doConfigure(confFile);
         StatusPrinter.print(logCtx);
      } catch (JoranException je) {
         StatusPrinter.printInCaseOfErrorsOrWarnings(logCtx);
      }

   }

   private static void stopHsqlDbServer() {
      hsqlDbServer.stop();
   }
}
