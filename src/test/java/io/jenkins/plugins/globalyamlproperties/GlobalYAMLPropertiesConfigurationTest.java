package io.jenkins.plugins.globalyamlproperties;

import hudson.model.Label;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Test;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class GlobalYAMLPropertiesConfigurationTest {


    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    public String yamlConfig = "version: 1.0";
    public String name = "test";
    public String emptyYamlConfig = "";


    GlobalYAMLPropertiesConfiguration createTestInstance() {
        GlobalYAMLPropertiesConfiguration globalConfiguration = GlobalYAMLPropertiesConfiguration.get();
        List<Config> config = new ArrayList<>();
        config.add(new Config(name, emptyYamlConfig));
        config.get(0).setYamlConfig(yamlConfig);
        globalConfiguration.setConfigs(config);
        return globalConfiguration;
    }

    @Test
    public void testConfigApi() throws GlobalYAMLPropertiesConfigurationException {
        GlobalYAMLPropertiesConfiguration globalConfiguration = createTestInstance();
        assert globalConfiguration.getConfigByName(name).getYamlConfig().equals(yamlConfig);
        assert globalConfiguration.getConfigs().get(0).getYamlConfig().equals(yamlConfig);
        assert globalConfiguration.getConfigs().get(0).getName().equals(name);
        assert globalConfiguration.getConfigs().get(0).getConfigMap().containsKey("version");
    }

    @Test
    public void testSerialization() throws IOException {
        GlobalYAMLPropertiesConfiguration globalConfiguration = createTestInstance();

        // Serialize the object to a byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeObject(globalConfiguration);
        objectOutputStream.close();

        // Deserialize the object from the byte array
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        try {
            Object deserializedObject = objectInputStream.readObject();
            // The deserialization should be successful
            assert deserializedObject instanceof GlobalYAMLPropertiesConfiguration;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            objectInputStream.close();
        }
    }

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
