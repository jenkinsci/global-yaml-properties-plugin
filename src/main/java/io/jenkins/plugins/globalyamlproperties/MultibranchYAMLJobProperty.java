package io.jenkins.plugins.globalyamlproperties;

import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty;
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.yaml.snakeyaml.Yaml;

import java.util.HashMap;
import java.util.Map;

public class MultibranchYAMLJobProperty extends AbstractFolderProperty<WorkflowMultiBranchProject> {

    private final String yamlConfiguration;
    private final HashMap<String, Object> parsedConfig;
    @DataBoundConstructor
    public MultibranchYAMLJobProperty(Boolean enableEdits, String yamlConfiguration) {
        this.yamlConfiguration = yamlConfiguration;
        Yaml parser = new Yaml();
        parsedConfig = parser.load(yamlConfiguration);
        parser = null;
    }

    public String getYamlConfiguration() {
        return yamlConfiguration;
    }

    public Map<String, Object> getParsedConfig() {
        return parsedConfig;
    }

    @Extension
    public static class DescriptorImpl extends AbstractFolderPropertyDescriptor {
        @NonNull
        @Override
        public String getDisplayName() {
            return "YAML Configuration";
        }
    }
}
