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

   public Exception getException() {
      return exception;
   }

   public State getState() {
      return state;
   }

   public FileUtil.CopyProgress getCopyProgress() {
      return copyProgress;
   }

   public void setState(State state) {
      setState(state, null);
   }

   public boolean isError() {
      return getException() != null;
   }

   public boolean isSuccess() {
      return getException() == null;
   }

   public void setState(State state, Exception exception) {
      this.state = state;
      this.exception = exception;
   }

   public boolean isValid(File file) {
      return (file == null) ? false : swarmFile.getAbsolutePath().equals(file.getAbsolutePath());
   }

   public enum State {
      COPYING, ERROR_COPYING, COPIED, STARTING_SWARM, ERROR_STARTING_SWARM, SWARM_STARZED, SHUTDOWN_SWARM;
   }
}
