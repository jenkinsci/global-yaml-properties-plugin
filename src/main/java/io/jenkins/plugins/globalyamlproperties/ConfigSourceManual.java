package io.jenkins.plugins.globalyamlproperties;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import java.io.Serializable;

@SuppressWarnings("unused")
public class ConfigSourceManual implements ConfigSource, Serializable {

    private String yamlConfig;

    @DataBoundConstructor
    public ConfigSourceManual(String yamlConfig) {
        this.yamlConfig = yamlConfig;
    }

    public String getYamlConfig() {
        return yamlConfig;
    }

    @DataBoundSetter
    public void setYamlConfig(String yamlConfig) {
        this.yamlConfig = yamlConfig;
    }

    @Override
    public ConfigSourceManual.DescriptorImpl getDescriptor() {
        return Jenkins.get().getDescriptorByType(ConfigSourceManual.DescriptorImpl.class);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ConfigSource> {

        @POST
        public FormValidation doCheckYamlConfig(@QueryParameter String value) {
            if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                return FormValidation.error("Only administrators can update global configuration");
            }
            return ConfigValidator.validateYamlConfig(value);
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Manually defined config";
        }
    }
}
