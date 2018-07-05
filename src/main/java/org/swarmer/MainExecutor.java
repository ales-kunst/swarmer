package org.swarmer;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swarmer.context.SwarmerCtx;
import org.swarmer.context.SwarmerCtxManager;
import org.swarmer.operation.DefaultOperation;
import org.swarmer.operation.InfiniteLoopOperation;
import org.swarmer.operation.SwarmerOperation;
import org.swarmer.operation.executor.SwarmJobExecutor;
import org.swarmer.operation.rest.RestServerStarter;
import org.swarmer.operation.watcher.FolderChangesWatcher;
import org.swarmer.util.FileUtil;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MainExecutor {
   private static final Logger                             LOG = LoggerFactory.getLogger(MainExecutor.class);
   private static       SwarmerCtx                         ctx;
   private static       MainExecutor                       instance;
   private              String[]                           cliArgs;
   private              Map<String, InfiniteLoopOperation> infiniteLoopOperations;
   private              DefaultOperation                   restServer;

   public static Initializer cliArgs(String[] cliArgs) {
      return new Initializer(instance(cliArgs));
   }

   private static MainExecutor instance(String[] cliArgs) {
      if (instance == null) {
         instance = new MainExecutor(cliArgs);
         ctx = SwarmerCtxManager.instance().getCtx();
      }
      return instance;
   }

   public static boolean finishedWithErrors() {
      return SwarmerOperation.finishedWithError();
   }

   public static MainExecutor instance() {
      return instance(new String[0]);
   }

   private MainExecutor(String[] cliArgs) {
      infiniteLoopOperations = new HashMap<>();
      this.cliArgs = cliArgs;
   }

   public void restartOperations(SwarmerCtx newSwarmerCtx) throws InterruptedException {
      gracefullyStopOperations();
      infiniteLoopOperations.clear();
      try {
         ctx.destroy();
      } catch (Exception e) {
         LOG.error("Error at closing old swarmer context! Continuing with restarting of processes!\n{}",
                   ExceptionUtils.getStackTrace(e));
      }
      ctx = newSwarmerCtx;
      startOperations();
   }

   private void gracefullyStopOperations() throws InterruptedException {
      for (InfiniteLoopOperation operation : infiniteLoopOperations.values()) {
         operation.gracefulStop();
         operation.cleanUp();
      }
   }

   public MainExecutor startOperations() {
      addOperation(folderWatcher());
      addOperation(swarmDeployer());

      for (SwarmerOperation swarmerOperation : infiniteLoopOperations.values()) {
         swarmerOperation.execute();
      }

      return instance;
   }

   private void addOperation(InfiniteLoopOperation operation) {
      boolean operationExists = infiniteLoopOperations.get(operation.name()) != null;
      if (!operationExists) {
         infiniteLoopOperations.put(operation.name(), operation);
      }
   }

   private InfiniteLoopOperation folderWatcher() {
      return new FolderChangesWatcher(FolderChangesWatcher.OP_NAME, ctx);
   }

   private InfiniteLoopOperation swarmDeployer() {
      return new SwarmJobExecutor(SwarmJobExecutor.OP_NAME, ctx);
   }

   public MainExecutor startRestServer() {
      if (restServer == null) {
         restServer = restServer();
         restServer.execute();
      }

      return this;
   }

   private DefaultOperation restServer() {
      return new RestServerStarter(RestServerStarter.OP_NAME, ctx, cliArgs);
   }

   public void waitToEnd() throws InterruptedException {
      synchronized (SwarmerOperation.operationsStates) {
         while (!SwarmerOperation.finishedWithError()) {
            SwarmerOperation.operationsStates.wait();
         }
      }
      gracefullyStopRestServer();
      gracefullyStopOperations();
      infiniteLoopOperations.clear();
      restServer = null;
   }

   private void gracefullyStopRestServer() {
      if (restServer != null) {
         try {
            restServer.cleanUp();
         } catch (Exception e) {
            LOG.error("Error at cleaning up of rest server!\n{}", e);
         }
      }
   }

   public static class Initializer {
      private final MainExecutor executor;

      private Initializer(MainExecutor executor) {
         this.executor = executor;
      }

      public MainExecutor initialize() {
         new File(FileUtil.KILL_APP_PATH).delete();
         new File(FileUtil.WIN_TEE_APP_PATH).delete();
         FileUtil.copyWindowsKillAppToTmp();
         FileUtil.copyWinTeeAppToTmp();
         return executor;
      }
   }
}
