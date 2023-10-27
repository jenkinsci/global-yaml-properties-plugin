package io.jenkins.plugins.globalyamlproperties;

import hudson.model.Label;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowBranchProjectFactory;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.junit.Test;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class GlobalYAMLPropertiesConfigurationTest {


    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    public String yamlConfig = "version: 1.0";
    public String name = "test";
    public String category = "example";
    public String emptyYamlConfig = "";


    GlobalYAMLPropertiesConfiguration createTestInstance() {
        GlobalYAMLPropertiesConfiguration globalConfiguration = GlobalYAMLPropertiesConfiguration.get();
        List<Config> config = new ArrayList<>();
        config.add(new Config(name, emptyYamlConfig));
        config.get(0).setYamlConfig(yamlConfig);
        config.get(0).setCategory(category);
        globalConfiguration.setConfigs(config);
        return globalConfiguration;
    }

    GlobalYAMLPropertiesConfiguration createMultipleTestInstances() {
        GlobalYAMLPropertiesConfiguration globalConfiguration = GlobalYAMLPropertiesConfiguration.get();
        List<Config> config = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            config.add(new Config(name + i, emptyYamlConfig));
            config.get(i).setYamlConfig(yamlConfig);
            config.get(i).setCategory(category);
        }
        globalConfiguration.setConfigs(config);
        return globalConfiguration;
    }

    GlobalYAMLPropertiesConfiguration createMultiCategorizedTestInstances() {
        GlobalYAMLPropertiesConfiguration globalConfiguration = GlobalYAMLPropertiesConfiguration.get();
        List<Config> config = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            config.add(new Config(name + i, emptyYamlConfig));
            config.get(i).setYamlConfig(yamlConfig);
            config.get(i).setCategory(category + i);
        }
        globalConfiguration.setConfigs(config);
        return globalConfiguration;
    }

    @Test
    public void testGetAllCategories() throws GlobalYAMLPropertiesConfigurationException {
        GlobalYAMLPropertiesConfiguration globalConfiguration = createTestInstance();
        assert globalConfiguration.getCategories().contains(category);
    }

    @Test
    public void testConfigsFromCategory() throws GlobalYAMLPropertiesConfigurationException {
        GlobalYAMLPropertiesConfiguration globalConfiguration = createMultipleTestInstances();
        assert globalConfiguration.getConfigsByCategory(category).size() == 3;

        // Create assertion that each config's category is equal to the category
        assert globalConfiguration.getConfigsByCategory(category).stream().allMatch(config -> config.getCategory().equals(category));

    }

    @Test
    public void testConfigApi() throws GlobalYAMLPropertiesConfigurationException {
        GlobalYAMLPropertiesConfiguration globalConfiguration = createTestInstance();
        assert globalConfiguration.getConfigByName(name).getYamlConfig().equals(yamlConfig);
        assert globalConfiguration.getConfigs().get(0).getYamlConfig().equals(yamlConfig);
        assert globalConfiguration.getConfigs().get(0).getName().equals(name);
        assert globalConfiguration.getConfigs().get(0).getConfigMap().containsKey("version");
        assert globalConfiguration.getConfigs().get(0).getCategory().equals(category);
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
    public void testScriptedPipelineEmptyConfig() throws Exception {
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
    public void testScriptedPipelineDefaultConfig() throws Exception {
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

    @Test
    public void testScriptedPipelineCategorized() throws Exception {
        String agentLabel = "my-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        GlobalYAMLPropertiesConfiguration globalConfiguration = createMultipleTestInstances();
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript
                = "node {\n"
                + "  println getGlobalYAMLCategories()\n"
                + "}";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        jenkins.assertLogContains(category, completedBuild);
    }

    @Test
    public void testScriptedPipelineGetConfigNamesByCategory() throws Exception {
        String agentLabel = "my-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        GlobalYAMLPropertiesConfiguration globalConfiguration = createMultipleTestInstances();
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript
                = "node {\n"
                + "  println getGlobalYAMLConfigNamesByCategory('" + category + "')\n"
                + "}";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        jenkins.assertLogContains(name + "0", completedBuild);
        jenkins.assertLogContains(name + "1", completedBuild);
        jenkins.assertLogContains(name + "2", completedBuild);
    }

    @Test
    public void testMultibranchJobConfiguration() throws Exception {
        // Create a MultiBranch Pipeline
        WorkflowMultiBranchProject mp = jenkins.jenkins.createProject(WorkflowMultiBranchProject.class, "my-multi-branch-project");

        // Setup the branch project factory (how Jenkins should create pipeline jobs for branches)
        mp.setProjectFactory(new WorkflowBranchProjectFactory());

        // Adding a property to the multibranch pipeline
        mp.addProperty(new MultibranchYAMLJobProperty(yamlConfig)); // Replace with your actual property and value

        MultibranchYAMLJobProperty property = mp.getProperties().get(MultibranchYAMLJobProperty.class);
        assertNotNull(property);
    }

    @Test
    public void testMultibranchJobConfigurationParsing() throws Exception {
        // Create a MultiBranch Pipeline
        WorkflowMultiBranchProject mp = jenkins.jenkins.createProject(WorkflowMultiBranchProject.class, "my-multi-branch-project");

        // Setup the branch project factory (how Jenkins should create pipeline jobs for branches)
        mp.setProjectFactory(new WorkflowBranchProjectFactory());

        // Adding a property to the multibranch pipeline
        mp.addProperty(new MultibranchYAMLJobProperty(yamlConfig)); // Replace with your actual property and value

        MultibranchYAMLJobProperty property = mp.getProperties().get(MultibranchYAMLJobProperty.class);
        assertEquals(1.0, property.getParsedConfig().get("version"));
    }

    @Test
    public void testScriptedPipelineGetMultipleCategories() throws Exception {
        String agentLabel = "my-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        GlobalYAMLPropertiesConfiguration globalConfiguration = createMultiCategorizedTestInstances();
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript
                = "node {\n"
                + "  println getGlobalYAMLCategories()\n"
                + "}";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        jenkins.assertLogContains(category + "0", completedBuild);
        jenkins.assertLogContains(category + "1", completedBuild);
        jenkins.assertLogContains(category + "2", completedBuild);
    }

}
