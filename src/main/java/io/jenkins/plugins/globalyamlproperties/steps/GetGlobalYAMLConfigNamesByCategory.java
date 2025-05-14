package io.jenkins.plugins.globalyamlproperties.steps;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.TaskListener;
import io.jenkins.plugins.globalyamlproperties.GlobalYAMLPropertiesConfiguration;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.Serial;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class GetGlobalYAMLConfigNamesByCategory extends Step {

    private String category = "";

    @DataBoundConstructor
    public GetGlobalYAMLConfigNamesByCategory(String category) {
        this.category = category;
    }

    @DataBoundSetter
    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategory() {
        return category;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new GlobalPropertiesStepExecution(context, category);
    }

    private static class GlobalPropertiesStepExecution extends SynchronousStepExecution<List<String>> {
        private final String category;

        @Serial
        private static final long serialVersionUID = 1L;

        protected GlobalPropertiesStepExecution(StepContext context, String category) {
            super(context);
            this.category = category;
        }

        @Override
        protected List<String> run() throws Exception {
            GlobalYAMLPropertiesConfiguration globalPropertiesConfig = GlobalYAMLPropertiesConfiguration.get();
            return globalPropertiesConfig.getConfigNamesByCategory(category);
        }
    }

    @Symbol("getGlobalYAMLConfigNamesByCategory")
    @Extension
    public static final class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() { return "getGlobalYAMLConfigNamesByCategory"; }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Get list of configs by category";
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
