package io.jenkins.plugins.globalyamlproperties;

import hudson.model.Label;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowBranchProjectFactory;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WithJenkins
class GlobalYAMLPropertiesConfigurationTest {

    private static final String YAML_CONFIG = "version: 1.0";
    private static final String NAME = "test";
    private static final String CATEGORY = "example";
    private static final String EMPTY_YAML_CONFIG = "";
    private static final String PARSED_YAML_CONFIG = "{version=1.0}";

    private JenkinsRule jenkins;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        jenkins = rule;
    }

    private static GlobalYAMLPropertiesConfiguration createTestInstance() {
        GlobalYAMLPropertiesConfiguration globalConfiguration = GlobalYAMLPropertiesConfiguration.get();
        List<Config> config = new ArrayList<>();
        config.add(new Config(NAME, CATEGORY, new ConfigSourceManual(YAML_CONFIG)));
        globalConfiguration.setConfigs(config);
        return globalConfiguration;
    }

    private static GlobalYAMLPropertiesConfiguration createMultipleTestInstances() {
        GlobalYAMLPropertiesConfiguration globalConfiguration = GlobalYAMLPropertiesConfiguration.get();
        List<Config> config = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            config.add(new Config(NAME + i, CATEGORY, new ConfigSourceManual(YAML_CONFIG)));
        }
        globalConfiguration.setConfigs(config);
        return globalConfiguration;
    }

    private static GlobalYAMLPropertiesConfiguration createMultiCategorizedTestInstances() {
        GlobalYAMLPropertiesConfiguration globalConfiguration = GlobalYAMLPropertiesConfiguration.get();
        List<Config> config = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            config.add(new Config(NAME + i, CATEGORY + i, new ConfigSourceManual(YAML_CONFIG)));
        }
        globalConfiguration.setConfigs(config);
        return globalConfiguration;
    }

    @Test
    void testGetAllCategories() {
        GlobalYAMLPropertiesConfiguration globalConfiguration = createTestInstance();
        assertTrue(globalConfiguration.getCategories().contains(CATEGORY));
    }

    @Test
    void testConfigsFromCategory() {
        GlobalYAMLPropertiesConfiguration globalConfiguration = createMultipleTestInstances();
        assertEquals(3, globalConfiguration.getConfigsByCategory(CATEGORY).size());

        // Create assertion that each config's category is equal to the category
        assertTrue(globalConfiguration.getConfigsByCategory(CATEGORY).stream().allMatch(config -> config.getCategory().equals(CATEGORY)));
    }

    @Test
    void testConfigApi() throws Exception {
        GlobalYAMLPropertiesConfiguration globalConfiguration = createTestInstance();
        assertEquals(YAML_CONFIG, globalConfiguration.getConfigByName(NAME).getYamlConfig());
        assertEquals(YAML_CONFIG, globalConfiguration.getConfigs().get(0).getYamlConfig());
        assertEquals(NAME, globalConfiguration.getConfigs().get(0).getName());
        assertTrue(globalConfiguration.getConfigs().get(0).getConfigMap().containsKey("version"));
        assertEquals(CATEGORY, globalConfiguration.getConfigs().get(0).getCategory());
    }

    @Test
    void testSerialization() throws Exception {
        GlobalYAMLPropertiesConfiguration globalConfiguration = createTestInstance();
        List<Config> configs = globalConfiguration.getConfigs();
        configs.add(new Config("test2", CATEGORY, new ConfigSourceSCM("repo", "owner", "master", "testCreds", "path")));
        globalConfiguration.setConfigs(configs);
        // Serialize the object to a byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeObject(globalConfiguration);
        objectOutputStream.close();

        // Deserialize the object from the byte array
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        try (ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
            Object deserializedObject = objectInputStream.readObject();
            // The deserialization should be successful
            assertInstanceOf(GlobalYAMLPropertiesConfiguration.class, deserializedObject);
        }
    }

    @Test
    void testScriptedPipeline() throws Exception {
        String agentLabel = "my-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        GlobalYAMLPropertiesConfiguration globalConfiguration = GlobalYAMLPropertiesConfiguration.get();
        List<Config> config = new ArrayList<>();
        config.add(new Config(NAME, YAML_CONFIG, new ConfigSourceManual(YAML_CONFIG)));
        globalConfiguration.setConfigs(config);
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript
                = """
                node {
                  println getGlobalYAMLProperties("test").version
                }""";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        jenkins.assertLogContains("1.0", completedBuild);
    }

    @Test
    void testScriptedPipelineEmptyConfig() throws Exception {
        String agentLabel = "my-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        GlobalYAMLPropertiesConfiguration globalConfiguration = GlobalYAMLPropertiesConfiguration.get();
        List<Config> config = new ArrayList<>();
        config.add(new Config(NAME, "", new ConfigSourceManual(EMPTY_YAML_CONFIG)));
        globalConfiguration.setConfigs(config);
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript
                = """
                node {
                  println getGlobalYAMLProperties().version
                }""";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        jenkins.assertLogContains("Warning: Configuration is empty", completedBuild);
    }

    @Test
    void testScriptedPipelineDefaultConfig() throws Exception {
        String agentLabel = "my-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        GlobalYAMLPropertiesConfiguration globalConfiguration = GlobalYAMLPropertiesConfiguration.get();
        List<Config> config = new ArrayList<>();
        config.add(new Config(NAME, YAML_CONFIG, new ConfigSourceManual(YAML_CONFIG)));
        globalConfiguration.setConfigs(config);
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript
                = """
                node {
                  println getGlobalYAMLProperties().version
                }""";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        jenkins.assertLogContains("Obtaining default configuration", completedBuild);
        jenkins.assertLogContains("1.0", completedBuild);
    }

    @Test
    void testScriptedPipelineCategorized() throws Exception {
        String agentLabel = "my-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        GlobalYAMLPropertiesConfiguration globalConfiguration = createMultipleTestInstances();
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript
                = """
                node {
                  println getGlobalYAMLCategories()
                }""";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        jenkins.assertLogContains(CATEGORY, completedBuild);
    }

    @Test
    void testScriptedPipelineGetConfigNamesByCategory() throws Exception {
        String agentLabel = "my-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        GlobalYAMLPropertiesConfiguration globalConfiguration = createMultipleTestInstances();
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript
                = "node {\n"
                + "  println getGlobalYAMLConfigNamesByCategory('" + CATEGORY + "')\n"
                + "}";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        jenkins.assertLogContains(NAME + "0", completedBuild);
        jenkins.assertLogContains(NAME + "1", completedBuild);
        jenkins.assertLogContains(NAME + "2", completedBuild);
    }

    @Test
    void testMultibranchJobConfiguration() throws Exception {
        // Create a MultiBranch Pipeline
        WorkflowMultiBranchProject mp = jenkins.jenkins.createProject(WorkflowMultiBranchProject.class, "my-multi-branch-project");

        // Setup the branch project factory (how Jenkins should create pipeline jobs for branches)
        mp.setProjectFactory(new WorkflowBranchProjectFactory());

        // Adding a property to the multibranch pipeline
        mp.addProperty(new MultibranchYAMLJobProperty(YAML_CONFIG)); // Replace with your actual property and value

        MultibranchYAMLJobProperty property = mp.getProperties().get(MultibranchYAMLJobProperty.class);
        assertNotNull(property);
    }

    @Test
    void testMultibranchJobConfigurationParsing() throws Exception {
        // Create a MultiBranch Pipeline
        WorkflowMultiBranchProject mp = jenkins.jenkins.createProject(WorkflowMultiBranchProject.class, "my-multi-branch-project");

        // Setup the branch project factory (how Jenkins should create pipeline jobs for branches)
        mp.setProjectFactory(new WorkflowBranchProjectFactory());
        // Adding a property to the multibranch pipeline
        mp.addProperty(new MultibranchYAMLJobProperty(YAML_CONFIG)); // Replace with your actual property and value

        MultibranchYAMLJobProperty property = mp.getProperties().get(MultibranchYAMLJobProperty.class);
        assertEquals(1.0, property.getParsedConfig().get("version"));
    }

    @Test
    void testScriptedPipelineLocalConfiguration() throws Exception {
        String agentLabel = "my-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        job.addProperty(new PipelineYAMLJobProperty(YAML_CONFIG));
        String pipelineScript
                = """
                node {
                  println getLocalYAMLProperties()
                }""";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        jenkins.assertLogContains(PARSED_YAML_CONFIG, completedBuild);
    }

    @Test
    void testScriptedPipelineWithoutLocalConfigurationProperty() throws Exception {
        String agentLabel = "my-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript
                = """
                node {
                  println getLocalYAMLProperties()
                }""";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
    }

    @Test
    void testScriptedPipelineGetMultipleCategories() throws Exception {
        String agentLabel = "my-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        GlobalYAMLPropertiesConfiguration globalConfiguration = createMultiCategorizedTestInstances();
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript
                = """
                node {
                  println getGlobalYAMLCategories()
                }""";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        jenkins.assertLogContains(CATEGORY + "0", completedBuild);
        jenkins.assertLogContains(CATEGORY + "1", completedBuild);
        jenkins.assertLogContains(CATEGORY + "2", completedBuild);
    }

}
