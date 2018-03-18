package org.swarmer.context;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.swarmer.util.FileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SwarmFile {

   private static final Logger LOG = LogManager.getLogger(SwarmFile.class);

   private FileUtil.CopyProgress copyProgress;
   private List<Exception>       exceptions;
   private List<State>           states;
   private File                  swarmFile;

   public SwarmFile(File swarmFile, State state, long fileSize) {
      this.swarmFile = swarmFile;
      states = new ArrayList<>();
      exceptions = new ArrayList<>();
      copyProgress = FileUtil.createCopyProgress(fileSize);
   }

   public void addState(State state, Exception exception) {
      states.add(state);
      exceptions.add(exception);
   }

   public void addState(State state) {
      addState(state, null);
   }

   public FileUtil.CopyProgress getCopyProgress() {
      return copyProgress;
   }

   public Exception getException() {
      return exceptions.get(exceptions.size() - 1);
   }

   public State getState() {
      return states.get(states.size() - 1);
   }

   public boolean hasErrorHappened() {
      return getException() != null;
   }

   public boolean isValid(File file) {
      return (file == null) ? false : swarmFile.getAbsolutePath().equals(file.getAbsolutePath());
   }

   public enum State {
      COPYING, ERROR_COPYING, COPIED, STARTING_SWARM, ERROR_STARTING_SWARM, SWARM_STARZED, SHUTDOWN_SWARM;
   }
}
