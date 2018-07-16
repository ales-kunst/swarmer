package org.swarmer.context;

import java.nio.file.Path;

public class ObservableFile {
   private final String containerName;
   private final Path   destPath;
   private final Path   srcPath;
   private final long   startObservingTime;

   public ObservableFile(Path srcPath, Path destPath, String containerName, long startObservingTime) {
      this.srcPath = srcPath;
      this.destPath = destPath;
      this.containerName = containerName;
      this.startObservingTime = startObservingTime;
   }

   public String getContainerName() {
      return containerName;
   }

   public Path getDestPath() {
      return destPath;
   }

   public long getStartObservingTime() {
      return startObservingTime;
   }

   public long getTimeObserving() {
      return System.currentTimeMillis() - startObservingTime;
   }

   public boolean isEqual(ObservableFile otherObservableFile) {
      String otherFilepath = otherObservableFile.getSrcPath().toFile().getAbsolutePath();
      return srcPath.toFile().getAbsolutePath().equalsIgnoreCase(otherFilepath);
   }

   public Path getSrcPath() {
      return srcPath;
   }

   @Override
   public String toString() {
      return "ObservableFile [" +
             "srcPath=" + srcPath +
             ", destPath=" + destPath +
             ", containerName='" + containerName + '\'' +
             ", startObservingTime=" + startObservingTime +
             ']';
   }
}
