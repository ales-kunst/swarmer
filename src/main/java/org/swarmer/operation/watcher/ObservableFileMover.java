package org.swarmer.operation.watcher;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swarmer.context.ObservableFile;
import org.swarmer.context.SwarmJob;
import org.swarmer.context.SwarmerCtx;
import org.swarmer.operation.InfiniteLoopOperation;
import org.swarmer.util.FileUtil;
import org.swarmer.util.SwarmUtil;

import java.nio.file.Path;

public class ObservableFileMover extends InfiniteLoopOperation {
   public static final  String OP_NAME                 = "Observable File Mover";
   private static final Logger LOG                     = LoggerFactory.getLogger(ObservableFileMover.class);
   private static final long   OBSERVABLE_FILE_TIMEOUT = 600000;

   public ObservableFileMover(String name, SwarmerCtx context) {
      super(name, context);
   }

   @Override
   protected void operationInitialize() {

   }

   @Override
   protected void loopBlock() {
      ObservableFile observableFile = getContext().popObservableFile();

      // If list of observable files is empty or the observable file does not exist then do nothing
      if (observableFile != null && observableFile.getSrcPath().toFile().exists()) {
         Path    srcPath             = observableFile.getSrcPath();
         Path    destPath            = observableFile.getDestPath();
         boolean srcJarFileValid     = SwarmUtil.isJarFileValid(srcPath.toFile());
         boolean hasJarFileBeenMoved = srcJarFileValid && FileUtil.moveFile(srcPath, destPath);

         if (srcJarFileValid && hasJarFileBeenMoved) {
            LOG.info("Jar file [{}] valid.", srcPath.toFile().getAbsolutePath());
            getContext().addSwarmJob(SwarmJob.builder()
                                             .action(SwarmJob.Action.RUN_NEW)
                                             .containerName(observableFile.getContainerName())
                                             .swarmJarFile(destPath.toFile())
                                             .build());

         }

         boolean shouldReturnObservableFile = !(srcJarFileValid && hasJarFileBeenMoved);
         boolean shouldStopObservingFile =
                 (observableFile.getTimeObserving() > OBSERVABLE_FILE_TIMEOUT) || !srcPath.toFile().exists();

         if (shouldReturnObservableFile && !shouldStopObservingFile) {
            getContext().returnObservableFile(observableFile);
         } else if (shouldReturnObservableFile && shouldStopObservingFile) {
            LOG.warn("Jar file could not be MOVED [Valid: {}].", srcPath.toFile().getAbsolutePath(), srcJarFileValid);
            LOG.warn("Timeout for file observing exceeded [{}]! Removing observable file from list [{}].",
                     observableFile.getStartObservingTime(), observableFile);
         }
      }

      SwarmUtil.waitFor(1000);
   }

   @Override
   protected void handleError(Throwable exception) {
      LOG.warn("Exception from ObservableFileMover. Continue with watch. Error stacktrace: \n {}",
               ExceptionUtils.getStackTrace(exception));
   }

   @Override
   protected void operationFinalize() {
      getContext().clearObservableFiles();
   }
}
