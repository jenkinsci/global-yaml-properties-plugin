package io.jenkins.plugins.globalproperties;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import java.util.HashMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * Example of Jenkins global configuration.
 */
@Extension
public class PropertiesConfiguration extends GlobalConfiguration {

    /** @return the singleton instance */
    public static PropertiesConfiguration get() {
        return ExtensionList.lookupSingleton(PropertiesConfiguration.class);
    }

    private String yamlConfig = "";
    private HashMap<String, Object> configMap = new HashMap<>();

    public PropertiesConfiguration() {
        // When Jenkins is restarted, load any saved configuration from disk.
        load();
    }

    /** @return YAML String with current yamlConfig */
    public String getYamlConfig() {
        return yamlConfig;
    }

    /**
     * Parse YAML from {@param yamlConfig} and save it to global configuration
     * @param yamlConfig the new value of this field
     */
    @DataBoundSetter
    public void setYamlConfig(String yamlConfig) {
        if (StringUtils.isEmpty(yamlConfig)) {
            this.yamlConfig = "";
            this.configMap = new HashMap<>();
            return;
        }
        this.yamlConfig = yamlConfig;
        Yaml parser = new Yaml();
        this.configMap = parser.load(yamlConfig);
        save();
    }

    /** @return YAML properties parsed to Map */
    public Map<String, Object> getConfigMap() {
        return this.configMap;
    }

    public FormValidation doCheckYamlConfig(@QueryParameter String value) {
        if (StringUtils.isEmpty(value)) {
            return FormValidation.warning("Config is empty.");
        }
        Yaml parser = new Yaml();
        // Exception will be thrown also when YAML is actually valid but can not be cast to Map.
        try {
            Object parsedYAML = parser.load(value);
            if (!(parsedYAML instanceof Map)) throw new GlobalPropertiesConfigurationException("Provided config is not a Map");
        } catch (YAMLException e) {
            return FormValidation.error("Config is not a valid YAML file.");
        } catch (GlobalPropertiesConfigurationException e) {
            return FormValidation.error("Specified YAML is valid, but can not be parsed to Map. Please, use key-value format.");
        }

        return FormValidation.ok();
    }

}
