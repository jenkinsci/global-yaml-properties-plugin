package io.jenkins.plugins.globalyamlproperties;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

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

    public List<Config> getConfigsByCategory(String category) throws GlobalYAMLPropertiesConfigurationException {
        List<Config> collectedConfigs = new ArrayList<>();
        for (Config config : configs) {
            if (config.getCategory().equals(category)) {
                collectedConfigs.add(config);
            }
        }
        return collectedConfigs;
    }

    public List<String> getConfigNamesByCategory(String category) throws GlobalYAMLPropertiesConfigurationException {
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

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        if (!json.has("configs")) {
            return true;
        }
        String configNameField = "name";
        String yamlConfigField = "yamlConfig";

        Config.DescriptorImpl configDescriptor = Jenkins.get().getDescriptorByType(Config.DescriptorImpl.class);

        Object configs = json.get("configs");
        List<JSONObject> configList = new ArrayList<>();

        if(configs instanceof net.sf.json.JSONArray) {
            net.sf.json.JSONArray configsArray = json.getJSONArray("configs");
            for (Object config : configsArray) {
                configList.add((JSONObject) config);
            }
        } else if(configs instanceof JSONObject){
            configList.add((JSONObject)configs);
        }


        for (JSONObject obj : configList) {
            if (obj == null) continue;

            if (!obj.has(configNameField) || !(obj.get(configNameField) instanceof String)) {
                throw new FormException("Global YAML Configuration is not valid: Config name must be a string", configNameField);
            }

            // If the "yamlConfig" key does not exist or is not a string, validation fails
            if (!obj.has(yamlConfigField) || !(obj.get(yamlConfigField) instanceof String)) {
                throw new FormException("YAML config must be a string", yamlConfigField);
            }

            // Perform the validation
            FormValidation nameValidation = configDescriptor.doCheckName(obj.getString(configNameField));
            if (nameValidation.kind != FormValidation.Kind.OK) {
                throw new FormException("Global YAML Configuration is not valid: " + nameValidation.getMessage(), configNameField);
            }

            FormValidation yamlConfigValidation = configDescriptor.doCheckYamlConfig(obj.getString(yamlConfigField));
            if (yamlConfigValidation.kind == FormValidation.Kind.ERROR) {
                throw new FormException("Global YAML Configuration [" + obj.get(configNameField) + "] is not valid: " + yamlConfigValidation.getMessage(), yamlConfigField);
            }
        }

        // If no exceptions were thrown during validation, bind the JSON to this instance
        req.bindJSON(this, json);

        return true;
    }
}
