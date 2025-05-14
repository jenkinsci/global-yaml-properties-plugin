package io.jenkins.plugins.globalyamlproperties;

import hudson.Extension;
import hudson.ExtensionList;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest2;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * Example of Jenkins global configuration.
 */
@Extension
public class GlobalYAMLPropertiesConfiguration extends GlobalConfiguration implements Serializable {

    /** @return the singleton instance */
    public static GlobalYAMLPropertiesConfiguration get() {
        return ExtensionList.lookupSingleton(GlobalYAMLPropertiesConfiguration.class);
    }

    private List<Config> configs = new ArrayList<>();
    private int refreshInterval = 60;

    public GlobalYAMLPropertiesConfiguration() {
        // When Jenkins is restarted, load any saved configuration from disk.
        load();
    }

    public List<Config> getConfigs() {
        return configs;
    }

    public Config getConfigByName(String name) throws GlobalYAMLPropertiesConfigurationException {
        for (Config config : configs) {
            if (config.getName().equals(name)) {
                return config;
            }
        }
        throw new GlobalYAMLPropertiesConfigurationException("Config with name " + name + " not found");
    }

    public List<String> getConfigNames() {
        List<String> configNames = new ArrayList<>();
        for (Config config : configs) {
            configNames.add(config.getName());
        }
        return configNames;
    }

    void refreshConfiguration() throws IOException {
        for (Config config : configs) {
            config.refreshConfiguration();
        }
    }

    public List<Config> getConfigsByCategory(String category) {
        List<Config> collectedConfigs = new ArrayList<>();
        for (Config config : configs) {
            if (config.getCategory().equals(category)) {
                collectedConfigs.add(config);
            }
        }
        return collectedConfigs;
    }

    public List<String> getConfigNamesByCategory(String category) {
        List<String> collectedConfigNames = new ArrayList<>();
        for (Config config : configs) {
            if (config.getCategory().equals(category)) {
                collectedConfigNames.add(config.getName());
            }
        }
        return collectedConfigNames;
    }

    public List<String> getCategories() {
        Set<String> categories = new HashSet<>();
        for(final Config config: getConfigs()) {
            if (config.getCategory() == null || config.getCategory().isEmpty()) {
                continue;
            }
            categories.add(config.getCategory());
        }
        return new ArrayList<>(categories);
    }

    public Config getDefaultConfig() {
        return configs.get(0);
    }

    @DataBoundSetter
    public void setConfigs(List<Config> configs) {
        this.configs = configs;
        save();
    }

    @DataBoundSetter
    public void setRefreshInterval(int refreshInterval) {
        this.refreshInterval = refreshInterval;
        save();
    }

    public int getRefreshInterval() {
        return this.refreshInterval;
    }

    @Override
    public boolean configure(StaplerRequest2 req, JSONObject json) throws FormException {
        String configsField = "configs";

        if (!json.has(configsField)) {
            return true;
        }

        // If no exceptions were thrown during validation, bind the JSON to this instance
        req.bindJSON(this, json);
        save();

        return true;
    }
}
