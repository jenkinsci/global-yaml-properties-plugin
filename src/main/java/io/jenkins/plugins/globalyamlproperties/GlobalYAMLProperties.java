package io.jenkins.plugins.globalyamlproperties;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.*;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class GlobalYAMLProperties extends Step {
    private String configName = "";

    @DataBoundConstructor public GlobalYAMLProperties(String configName) {
        if (configName != null) {
            this.configName = configName;
        }
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
                logger.println("[GetGlobalProperties] Obtaining default configuration");
                globalPropertiesConfigMap = globalPropertiesConfig.getDefaultConfig().getConfigMap();
            }

            if (globalPropertiesConfigMap.isEmpty()) {
                logger.println("[GetGlobalProperties] Warning: Configuration is empty");
            } else {
                logger.println("[GetGlobalProperties] Obtained configuration:");
                globalPropertiesConfigMap.forEach((k, v) -> logger.println(k + " : " + v));
            }
            return deepCopyMap(globalPropertiesConfigMap);
        }

        static Map<String, Object> deepCopyMap(Map<String, Object> orig) throws IOException, ClassNotFoundException {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(orig);
            oos.flush();
            ByteArrayInputStream bin = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bin);
            return (Map<String, Object>) ois.readObject();
        }
    }

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
