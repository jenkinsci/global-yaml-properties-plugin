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
    private String category;
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

    public String getCategory() {
        return category;
    }

    @DataBoundSetter
    public void setCategory(String category) {
        this.category = category;
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
            return ConfigValidator.validateName(value);
        }

        @POST
        public FormValidation doCheckYamlConfig(@QueryParameter String value) {
            if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                return FormValidation.error("Only administrators can update global configuration");
            }
            return ConfigValidator.validateYamlConfig(value);
        }

        @Override
        public String getDisplayName() {
            return "Config";
        }
    }
}
