package org.swarmer;

import org.swarmer.context.SwarmerContext;
import org.swarmer.operation.DefaultOperation;
import org.swarmer.operation.InfiniteLoopOperation;
import org.swarmer.operation.SwarmerOperation;
import org.swarmer.operation.executor.SwarmDeployer;
import org.swarmer.operation.swarm.RestServerStarter;
import org.swarmer.operation.watcher.FolderChangesWatcher;

import java.util.HashMap;
import java.util.Map;

public class MainExecutor {

   private static MainExecutor                       instance;
   private        String[]                           cliArgs;
   private        Map<String, InfiniteLoopOperation> infiniteLoopOperations;
   private        DefaultOperation                   restServer;

   public static MainExecutor cliArgs(String[] cliArgs) {
      return instance(cliArgs);
   }

   private static MainExecutor instance(String[] cliArgs) {
      if (instance == null) {
         instance = new MainExecutor(cliArgs);
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

   public void restartOperations(SwarmerContext swarmerContext) throws InterruptedException {
      gracefullyStopOperations();
      infiniteLoopOperations.clear();
      startOperations(swarmerContext);
   }

   private void gracefullyStopOperations() throws InterruptedException {
      for (InfiniteLoopOperation operation : infiniteLoopOperations.values()) {
         operation.gracefulStop();
         operation.cleanUp();
      }
   }

   public MainExecutor startOperations(SwarmerContext swarmerContext) {
      addOperation(folderWatcher(swarmerContext));
      addOperation(swarmDeployer(swarmerContext));

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

   private InfiniteLoopOperation folderWatcher(SwarmerContext swarmerContext) {
      return new FolderChangesWatcher(FolderChangesWatcher.OP_NAME, swarmerContext);
   }

   private InfiniteLoopOperation swarmDeployer(SwarmerContext swarmerContext) {
      return new SwarmDeployer(SwarmDeployer.OP_NAME, swarmerContext);
   }

   public MainExecutor startRestServer(SwarmerContext swarmerContext) {
      if (restServer == null) {
         restServer = restServer(swarmerContext);
         restServer.execute();
      }

      return this;
   }

   private DefaultOperation restServer(SwarmerContext swarmerContext) {
      return new RestServerStarter(RestServerStarter.OP_NAME, swarmerContext, cliArgs);
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
         restServer.cleanUp();
      }
   }
}
