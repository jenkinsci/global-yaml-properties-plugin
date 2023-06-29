package io.jenkins.plugins.globalyamlproperties;

import hudson.model.Label;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Test;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.ArrayList;
import java.util.List;

public class GlobalYAMLPropertiesConfigurationTest {


    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    public String yamlConfig = "version: 1.0";
    public String name = "test";
    public String emptyYamlConfig = "";

    @Test
    public void testScriptedPipeline() throws Exception {
        String agentLabel = "my-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        GlobalYAMLPropertiesConfiguration globalConfiguration = GlobalYAMLPropertiesConfiguration.get();
        List<Config> config = new ArrayList<>();
        config.add(new Config(name, yamlConfig));
        globalConfiguration.setConfigs(config);
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript
                = "node {\n"
                + "  println getGlobalYAMLProperties(\"test\").version\n"
                + "}";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        jenkins.assertLogContains("1.0", completedBuild);
    }

    @Test
    public void testEmptyConfigScriptedPipeline() throws Exception {
        String agentLabel = "my-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        GlobalYAMLPropertiesConfiguration globalConfiguration = GlobalYAMLPropertiesConfiguration.get();
        List<Config> config = new ArrayList<>();
        config.add(new Config(name, emptyYamlConfig));
        globalConfiguration.setConfigs(config);
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript
                = "node {\n"
                + "  println getGlobalYAMLProperties().version\n"
                + "}";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        jenkins.assertLogContains("Warning: Configuration is empty", completedBuild);
    }

    @Test
    public void testDefaultConfigScriptedPipeline() throws Exception {
        String agentLabel = "my-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        GlobalYAMLPropertiesConfiguration globalConfiguration = GlobalYAMLPropertiesConfiguration.get();
        List<Config> config = new ArrayList<>();
        config.add(new Config(name, yamlConfig));
        globalConfiguration.setConfigs(config);
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript
                = "node {\n"
                + "  println getGlobalYAMLProperties().version\n"
                + "}";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        jenkins.assertLogContains("Obtaining default configuration", completedBuild);
        jenkins.assertLogContains("1.0", completedBuild);
    }

}
