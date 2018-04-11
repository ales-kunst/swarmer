package org.swarmer.context;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.util.FileUtil;

import java.io.File;

public class SwarmFile {

   private static final Logger LOG = LogManager.getLogger(SwarmFile.class);

   private FileUtil.CopyProgress copyProgress;
   private Exception             exception;
   private State                 state;
   private File                  swarmFile;

   public SwarmFile(File swarmFile, State state, long fileSize) {
      this.swarmFile = swarmFile;
      this.state = state;
      exception = null;
      copyProgress = FileUtil.createCopyProgress(fileSize);
   }

   public String getAbsolutePath() {
      return swarmFile.getAbsolutePath();
   }

   public FileUtil.CopyProgress getCopyProgress() {
      return copyProgress;
   }

   public Exception getException() {
      return exception;
   }

   public File getFile() { return swarmFile; }

   public String getFilename() { return swarmFile.getName(); }

   public boolean isSuccess() {
      return getException() == null;
   }

   public State getState() {
      return state;
   }

   public boolean isError() {
      return getException() != null;
   }

   public void setState(State state, Exception exception) {
      this.state = state;
      this.exception = exception;
   }

   public void setState(State state) {
      setState(state, null);
   }

   public boolean isValid(File file) {
      return (file != null) && swarmFile.getAbsolutePath().equals(file.getAbsolutePath());
   }

   public enum State {
      COPYING, ERROR_COPYING, COPIED, SWARM_PORT_TAKEN, STARTING_SWARM, ERROR_STARTING_SWARM, SWARM_STARTED, SHUTDOWN_SWARM
   }
}
