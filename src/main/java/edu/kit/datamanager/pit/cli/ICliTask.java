package edu.kit.datamanager.pit.cli;

import java.io.IOException;
import edu.kit.datamanager.pit.common.InvalidConfigException;

public interface ICliTask {
    /**
     * Processes this CLI task.
     * 
     * @return true, if application should shut down after this task.
     * @throws IOException            on IO issues
     * @throws InvalidConfigException on configuration issues
     */
    public boolean process() throws IOException, InvalidConfigException;
}
