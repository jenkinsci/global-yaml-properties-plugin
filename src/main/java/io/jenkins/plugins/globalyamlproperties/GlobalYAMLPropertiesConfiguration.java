package io.jenkins.plugins.globalyamlproperties;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import java.util.HashMap;
import java.util.Map;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.verb.POST;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * Example of Jenkins global configuration.
 */
@Extension
public class GlobalYAMLPropertiesConfiguration extends GlobalConfiguration {

    /** @return the singleton instance */
    public static GlobalYAMLPropertiesConfiguration get() {
        return ExtensionList.lookupSingleton(GlobalYAMLPropertiesConfiguration.class);
    }

    private String yamlConfig = "";
    private HashMap<String, Object> configMap = new HashMap<>();

    public GlobalYAMLPropertiesConfiguration() {
        // When Jenkins is restarted, load any saved configuration from disk.
        load();
    }

    /** @return YAML String with current yamlConfig */
    public String getYamlConfig() {
        return yamlConfig;
    }

    /**
     * Parse YAML from yamlConfig parameter and save it to global configuration
     * Please note that this method is called when user saves configuration in Jenkins UI.
     * Call it manually from Java code only when you understand what you are doing.
     * @param yamlConfig
     *     the new value of this field
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


    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {

        // Retrieve the form values
        String yamlConfiguration = json.optString("yamlConfig", "");

        // Perform the validation
        FormValidation validation = doCheckYamlConfig(yamlConfiguration);
        if (validation.kind != FormValidation.Kind.OK) {
            throw new FormException(validation.getMessage() + ". Config will be restored to previous version.", "yamlConfig");
        }
        req.bindJSON(this, json);
        return true;
    }

    /** @return YAML properties parsed to Map
     *  Can be called from Java code manually to get current configuration without calling getGlobalYAMLProperties() step
     */
    public Map<String, Object> getConfigMap() {
        return this.configMap;
    }

    @POST
    public FormValidation doCheckYamlConfig(@QueryParameter String value) {
        if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
            return FormValidation.error("Only administrators can update global configuration");
        }
        if (StringUtils.isEmpty(value)) {
            return FormValidation.warning("Config is empty");
        }
        Yaml parser = new Yaml();
        // Exception will be thrown also when YAML is actually valid but can not be cast to Map.
        try {
            Object parsedYAML = parser.load(value);
            if (!(parsedYAML instanceof Map)) throw new GlobalYAMLPropertiesConfigurationException("Provided config's root element is not a Map");
        } catch (YAMLException e) {
            return FormValidation.error("Config is not a valid YAML file");
        } catch (GlobalYAMLPropertiesConfigurationException e) {
            return FormValidation.error("Specified YAML is valid, but root element is not a Map. Please, use key-value format for root element");
        }

        return FormValidation.ok("YAML config is valid");
    }

}
