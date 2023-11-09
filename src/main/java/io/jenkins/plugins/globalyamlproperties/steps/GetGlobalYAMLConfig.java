package io.jenkins.plugins.globalyamlproperties.steps;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.TaskListener;
import io.jenkins.plugins.globalyamlproperties.GlobalYAMLPropertiesConfiguration;
import io.jenkins.plugins.globalyamlproperties.Utils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.*;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class GetGlobalYAMLConfig extends Step {
    private String configName = "";

    @DataBoundConstructor public GetGlobalYAMLConfig(String configName) {
        if (configName != null) {
            this.configName = configName;
        }
    }

    @DataBoundSetter
    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public String getConfigName() {
        return configName;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new GlobalPropertiesStepExecution(context, configName);
    }

    private static class GlobalPropertiesStepExecution extends SynchronousStepExecution<Map<String, Object>> {

        private final String configName;
        private static final long serialVersionUID = 1L;

        protected GlobalPropertiesStepExecution(StepContext context, String configName) {
            super(context);
            this.configName = configName;
        }

        @Override
        protected Map<String, Object> run() throws Exception {
            StepContext context = getContext();
            PrintStream logger = context.get(TaskListener.class).getLogger();
            GlobalYAMLPropertiesConfiguration globalPropertiesConfig = GlobalYAMLPropertiesConfiguration.get();
            Map<String, Object> globalPropertiesConfigMap;
            if (configName != null && !configName.isEmpty()) {
                logger.println("[GetGlobalProperties] Obtaining configuration for " + configName);
                globalPropertiesConfigMap = globalPropertiesConfig.getConfigByName(configName).getConfigMap();
            } else {
                logger.println("[GetGlobalProperties] Obtaining default configuration (" + globalPropertiesConfig.getDefaultConfig().getName() + ")");
                globalPropertiesConfigMap = globalPropertiesConfig.getDefaultConfig().getConfigMap();
            }

            if (globalPropertiesConfigMap.isEmpty()) {
                logger.println("[GetGlobalProperties] Warning: Configuration is empty");
            }
            return Utils.deepCopyMap(globalPropertiesConfigMap);
        }
    }

    @Symbol("getGlobalYAMLProperties")
    @Extension
    public static final class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "getGlobalYAMLProperties";
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Get Global YAML Properties in HashMap format";
        }

        @Override
        public boolean takesImplicitBlockArgument() {
            return false;
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(TaskListener.class);
        }
    }
}
