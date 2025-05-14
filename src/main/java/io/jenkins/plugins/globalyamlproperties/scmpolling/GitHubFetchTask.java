package io.jenkins.plugins.globalyamlproperties.scmpolling;

import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;
import io.jenkins.plugins.globalyamlproperties.Config;
import io.jenkins.plugins.globalyamlproperties.ConfigSourceSCM;
import io.jenkins.plugins.globalyamlproperties.GlobalYAMLPropertiesConfiguration;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Extension
public class GitHubFetchTask extends AsyncPeriodicWork {

    private static final Logger LOGGER = Logger.getLogger(GitHubFetchTask.class.getName());
    private static final String LOG_PREFIX = "[GlobalYAMLPropertiesPeriodicTask] ";
    public GitHubFetchTask() {
        super("GitHubFetchTask");
    }

    @Override
    protected void execute(TaskListener listener) throws IOException {
        // Place your logic to fetch content using the Kohsuke GitHub API here
        LOGGER.info(LOG_PREFIX + "Refreshing GlobalYAMLProperties configuration...");

        for (Config config: GlobalYAMLPropertiesConfiguration.get().getConfigs()) {
            LOGGER.info(LOG_PREFIX + "Processing " + config.getName() + "...");
            try {
                if (config.getConfigSource() instanceof ConfigSourceSCM configSourceSCM) {
                    configSourceSCM.fetchConfiguration();
                }
                config.refreshConfiguration();
            } catch (Exception e) {
                LOGGER.severe(LOG_PREFIX + "Error refreshing " + config.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
            config.refreshConfiguration();
            LOGGER.info(LOG_PREFIX + "Refresh " + config.getName() + "...");
        }

        LOGGER.info(LOG_PREFIX + "Refresh completed. Next refresh after " + TimeUnit.MILLISECONDS.toMinutes(getRecurrencePeriod()) + " minutes.");
    }

    @Override
    public long getRecurrencePeriod() {
        return MIN * GlobalYAMLPropertiesConfiguration.get().getRefreshInterval();  // This will run the task every hour. Adjust as needed.
    }
}
