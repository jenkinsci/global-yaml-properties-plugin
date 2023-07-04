package io.jenkins.plugins.globalyamlproperties;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.AbstractDescribableImpl;
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Config extends AbstractDescribableImpl<Config> implements Serializable {

    private String name;
    private String yamlConfig;
    private HashMap<String, Object> configMap = new HashMap<>();

    @DataBoundConstructor
    public Config(String name, String yamlConfig) {
        this.name = name;
        this.yamlConfig = yamlConfig;
        if (yamlConfig.isEmpty()) {
            this.configMap = new HashMap<>();
        } else {
            parseYamlConfig();
        }
    }

    public void setYamlConfig(String yamlConfig) {
        this.yamlConfig = yamlConfig;
        parseYamlConfig();
    }

    private void parseYamlConfig() {
        Yaml parser = new Yaml();
        this.configMap = parser.load(yamlConfig);
    }

    public String getName() {
        return name;
    }

    public String getYamlConfig() {
        return yamlConfig;
    }

    public Map<String, Object> getConfigMap() {
        return this.configMap;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<Config> {

        @POST
        public FormValidation doCheckName(@QueryParameter String value) {
            if (StringUtils.isEmpty(value)) {
                return FormValidation.warning("Name is empty");
            }
            return FormValidation.ok();
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
                if (!(parsedYAML instanceof Map))
                    throw new GlobalYAMLPropertiesConfigurationException("Provided config's root element is not a Map");
            } catch (YAMLException e) {
                return FormValidation.error("Config is not a valid YAML file");
            } catch (GlobalYAMLPropertiesConfigurationException e) {
                return FormValidation.error(
                        "Specified YAML is valid, but root element is not a Map. Please, use key-value format for root element");
            }

            return FormValidation.ok("YAML config is valid");
        }

        @Override
        public String getDisplayName() {
            return "Config";
        }
    }
}
