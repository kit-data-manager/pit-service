package edu.kit.datamanager.pit.cli;

import java.io.IOException;
import edu.kit.datamanager.pit.common.InvalidConfigException;

public interface ICliTask {
    public void process() throws IOException, InvalidConfigException;
}
