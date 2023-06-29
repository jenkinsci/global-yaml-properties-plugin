package io.jenkins.plugins.globalyamlproperties;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.verb.POST;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Example of Jenkins global configuration.
 */
@Extension
public class GlobalYAMLPropertiesConfiguration extends GlobalConfiguration {
    private static final Logger LOGGER = Logger.getLogger(GlobalYAMLPropertiesConfiguration.class.getName());
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
            throw new FormException("Configs must be an array", "configs");
        }

        Config.DescriptorImpl configDescriptor = Jenkins.get().getDescriptorByType(Config.DescriptorImpl.class);

        net.sf.json.JSONArray configs = json.getJSONArray("configs");

        for (Object obj : configs) {
            if (!(obj instanceof JSONObject)) continue;
            JSONObject configJson = (JSONObject) obj;


            if (!configJson.has("name") || !(configJson.get("name") instanceof String)) {
                throw new FormException("Name must be a string", "name");
            }

            // If the "yamlConfig" key does not exist or is not a string, validation fails
            if (!configJson.has("yamlConfig") || !(configJson.get("yamlConfig") instanceof String)) {
                throw new FormException("YAML config must be a string", "yamlConfig");
            }

            // Perform the validation
            FormValidation nameValidation = configDescriptor.doCheckName(configJson.getString("name"));
            if (nameValidation.kind != FormValidation.Kind.OK) {
                throw new FormException(nameValidation.getMessage(), "name");
            }

            FormValidation yamlConfigValidation = configDescriptor.doCheckYamlConfig(configJson.getString("yamlConfig"));
            if (yamlConfigValidation.kind != FormValidation.Kind.OK) {
                throw new FormException(yamlConfigValidation.getMessage(), "yamlConfig");
            }
        }

        // If no exceptions were thrown during validation, bind the JSON to this instance
        req.bindJSON(this, json);

        return true;
    }
}
