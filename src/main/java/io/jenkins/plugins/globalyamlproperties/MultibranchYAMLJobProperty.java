package io.jenkins.plugins.globalyamlproperties;

import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty;
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.util.FormValidation;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.verb.POST;
import org.yaml.snakeyaml.Yaml;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class MultibranchYAMLJobProperty extends AbstractFolderProperty<WorkflowMultiBranchProject> {

    private String yamlConfiguration;
    private Map<String, Object> parsedConfig;

    @DataBoundConstructor
    public MultibranchYAMLJobProperty(String yamlConfiguration) {
        this.yamlConfiguration = yamlConfiguration;
        if (yamlConfiguration.isEmpty()) {
            this.parsedConfig = new HashMap<>();
        } else {
            parseYamlConfig();
        }
    }

    private void parseYamlConfig() {
        Yaml parser = new Yaml();
        parsedConfig = parser.load(yamlConfiguration);
        parser = null;
    }

    public Map<String, Object> getParsedConfig() {
        if (parsedConfig == null) {
            parseYamlConfig();
        }
        return parsedConfig;
    }

    @DataBoundSetter
    void setYamlConfiguration(String yamlConfiguration) {
        this.yamlConfiguration = yamlConfiguration;
        parseYamlConfig();
    }

    public String getYamlConfiguration() {
        return yamlConfiguration;
    }

    @Extension
    public static class DescriptorImpl extends AbstractFolderPropertyDescriptor {
        @NonNull
        @Override
        public String getDisplayName() {
            return "YAML Configuration";
        }

        @SuppressWarnings("unused")
        @POST
        public FormValidation doCheckYamlConfiguration(@QueryParameter String value) {
            return ConfigValidator.validateYamlConfig(value);
        }
    }
}
