package io.jenkins.plugins.globalyamlproperties.steps;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.TaskListener;
import io.jenkins.plugins.globalyamlproperties.GlobalYAMLPropertiesConfiguration;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serial;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class GetGlobalYAMLCategories extends Step {

    @DataBoundConstructor
    public GetGlobalYAMLCategories() {
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new GlobalPropertiesStepExecution(context);
    }

    private static class GlobalPropertiesStepExecution extends SynchronousStepExecution<List<String>> {

        @Serial
        private static final long serialVersionUID = 1L;

        protected GlobalPropertiesStepExecution(StepContext context) {
            super(context);
        }

        @Override
        protected List<String> run() throws Exception {
            GlobalYAMLPropertiesConfiguration globalPropertiesConfig = GlobalYAMLPropertiesConfiguration.get();
            return globalPropertiesConfig.getCategories();
        }
    }

    @Symbol("getGlobalYAMLCategories")
    @Extension
    public static final class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "getGlobalYAMLCategories";
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Get all categories defined in Global YAML Properties";
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
