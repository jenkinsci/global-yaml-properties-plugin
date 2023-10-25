package io.jenkins.plugins.globalyamlproperties.steps;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.globalyamlproperties.GlobalYAMLPropertiesConfiguration;
import io.jenkins.plugins.globalyamlproperties.MultibranchYAMLJobProperty;
import io.jenkins.plugins.globalyamlproperties.PipelineYAMLJobProperty;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import hudson.PluginWrapper;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.*;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class GetLocalYAMLConfig extends Step {

    @DataBoundConstructor public GetLocalYAMLConfig() {
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new GlobalPropertiesStepExecution(context);
    }

    private static class GlobalPropertiesStepExecution extends SynchronousStepExecution<Map<String, Object>> {
        private static final long serialVersionUID = 1L;

        protected GlobalPropertiesStepExecution(StepContext context) {
            super(context);
        }

        protected boolean isMultibranchPipeline(Object job) {
            PluginWrapper plugin = Jenkins.getInstanceOrNull().pluginManager.getPlugin("workflow-multibranch");
            if (plugin != null) {
                // The Multi-Branch Project Plugin is installed
                if (job instanceof org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject) {
                    return true;
                }
            }
            return false;
        }
        @Override
        protected Map<String, Object> run() throws Exception {
            StepContext context = getContext();
            PrintStream logger = context.get(TaskListener.class).getLogger();
            Object job = context.get(Run.class).getParent();

            if (isMultibranchPipeline(job)) {
                org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject multibranchProject = (org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject) job;
                MultibranchYAMLJobProperty localYamlConfiguration = multibranchProject.getProperties().get(MultibranchYAMLJobProperty.class);
                return localYamlConfiguration.getParsedConfig();
            } else {
                WorkflowJob pipelineJob = (WorkflowJob) job;
                PipelineYAMLJobProperty localYamlConfiguration = pipelineJob.getProperty(PipelineYAMLJobProperty.class);
                return deepCopyMap(localYamlConfiguration.getParsedConfig());
            }
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
