package io.jenkins.plugins.globalyamlproperties;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.verb.POST;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


@SuppressWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
public class PipelineYAMLJobProperty extends JobProperty<AbstractProject<?, ?>> implements Serializable {

    private final String yamlConfiguration;
    private transient HashMap<String, Object> parsedConfig;

    @DataBoundConstructor
    public PipelineYAMLJobProperty(String yamlConfiguration) {
        this.yamlConfiguration = yamlConfiguration;
        Yaml parser = new Yaml();
        parsedConfig = parser.load(yamlConfiguration);
        parser = null;
    }

    public String getYamlConfiguration() {
        return yamlConfiguration;
    }
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        Yaml parser = new Yaml();
        this.parsedConfig = parser.load(yamlConfiguration);
        parser = null;
    }

    public Map<String, Object> getParsedConfig() {
        if (parsedConfig == null) {
            Yaml parser = new Yaml();
            parsedConfig = parser.load(yamlConfiguration);
        }
        return parsedConfig;
    }


    @Extension
    public static class DescriptorImpl extends JobPropertyDescriptor {

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            // handle the submitted form data
            return super.configure(req, json);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean isApplicable(Class<? extends Job> jobType) {
            return true;
        }

        @Override
        public JobProperty<?> newInstance(StaplerRequest req, JSONObject formData) {
            String yamlConfiguration = formData.getString("yamlConfiguration");
            return new PipelineYAMLJobProperty(yamlConfiguration);
        }

        @POST
        @SuppressWarnings("unused")
        public FormValidation doCheckYamlConfiguration(@QueryParameter String value) {
            return ConfigValidator.validateYamlConfig(value);
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Custom YAML Configuration";
        }
    }

    private static final long serialVersionUID = 1L;
}
