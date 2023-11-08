package io.jenkins.plugins.globalyamlproperties;

import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.*;
import org.kohsuke.stapler.verb.GET;
import org.kohsuke.stapler.verb.POST;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;


public class Config extends AbstractDescribableImpl<Config> implements Serializable {

    private String name;
    private String yamlConfig;
    private String category;
    private HashMap<String, Object> configMap = new HashMap<>();
    private ConfigSource configSource;

    @DataBoundConstructor
    public Config(String name, String category, ConfigSource configSource) {
        this.name = name;
        this.configSource = configSource;
        this.category = category;
        this.yamlConfig = this.configSource.getYamlConfig();
        parseConfiguration();
    }

    @DataBoundSetter
    public void setCategory(String category) {
        this.category = category;
    }

    @DataBoundSetter
    public void setConfigSource(ConfigSource configSource) {
        this.configSource = configSource;
    }

    @DataBoundSetter
    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public String getYamlConfig() {
        return yamlConfig;
    }

    public ConfigSource getConfigSource() {
        return configSource;
    }

    public Map<String, Object> getConfigMap() {
        return this.configMap;
    }

    public void refreshConfiguration() throws IOException {
        Logger logger = Logger.getLogger(Config.class.getName());
        logger.info("Refreshing configuration for " + this.name);
        if (getConfigSource() instanceof ConfigSourceSCM) {
            ConfigSourceSCM configSourceSCM = (ConfigSourceSCM) getConfigSource();
            configSourceSCM.fetchConfiguration();
        }
        parseConfiguration();
    }

    public void parseConfiguration() {
        Yaml parser = new Yaml();
        this.yamlConfig = configSource.getYamlConfig();
        if (this.yamlConfig == null || this.yamlConfig.isEmpty()) {
            this.configMap = new HashMap<>();
        } else {
            this.configMap = parser.load(yamlConfig);
        }
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<Config> {

        @POST
        public FormValidation doCheckName(@QueryParameter String value) {
            return ConfigValidator.validateName(value);
        }

        @POST
        @GET
        public DescriptorExtensionList<ConfigSource, Descriptor<ConfigSource>> getApplicableConfigSources() {
            return Jenkins.get().getDescriptorList(ConfigSource.class);
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
