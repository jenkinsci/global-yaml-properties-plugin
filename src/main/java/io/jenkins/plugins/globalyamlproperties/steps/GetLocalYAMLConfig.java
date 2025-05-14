package io.jenkins.plugins.globalyamlproperties.steps;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.globalyamlproperties.MultibranchYAMLJobProperty;
import io.jenkins.plugins.globalyamlproperties.PipelineYAMLJobProperty;
import io.jenkins.plugins.globalyamlproperties.Utils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import hudson.PluginWrapper;
import jenkins.model.Jenkins;

import java.io.Serial;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GetLocalYAMLConfig extends Step {

    @DataBoundConstructor public GetLocalYAMLConfig() {
        // Empty because step requires no parameters
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new GlobalPropertiesStepExecution(context);
    }

    private static class GlobalPropertiesStepExecution extends SynchronousStepExecution<Map<String, Object>> {
        @Serial
        private static final long serialVersionUID = 1L;

        protected GlobalPropertiesStepExecution(StepContext context) {
            super(context);
        }

        protected boolean isMultibranchPipeline(Job job) {
            try {
                PluginWrapper plugin = Jenkins.get().pluginManager.getPlugin("workflow-multibranch");
                return plugin != null && (job.getParent() instanceof org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject);
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        protected Map<String, Object> run() throws Exception {
            StepContext context = getContext();
            Job job = context.get(Run.class).getParent();

            if (isMultibranchPipeline(job)) {
                org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject multibranchProject = (org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject) job.getParent();
                MultibranchYAMLJobProperty localYamlConfiguration = multibranchProject.getProperties().get(MultibranchYAMLJobProperty.class);
                if (localYamlConfiguration == null) {
                    return new HashMap<>();
                }
                return Utils.deepCopyMap(localYamlConfiguration.getParsedConfig());
            } else {
                WorkflowJob pipelineJob = (WorkflowJob) job;
                PipelineYAMLJobProperty localYamlConfiguration = pipelineJob.getProperty(PipelineYAMLJobProperty.class);
                if (localYamlConfiguration == null) {
                    return new HashMap<>();
                }
                return Utils.deepCopyMap(localYamlConfiguration.getParsedConfig());
            }
        }
    }

    @Symbol("getLocalYAMLProperties")
    @Extension
    public static final class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "getLocalYAMLProperties";
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Get Local YAML Properties in HashMap format";
        }

        @Override
        public boolean takesImplicitBlockArgument() {
            return false;
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Set.of(TaskListener.class, Run.class);
        }
    }
}
