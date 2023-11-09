package io.jenkins.plugins.globalyamlproperties;

import hudson.Extension;
import hudson.model.*;
import hudson.security.Permission;
import hudson.security.PermissionGroup;
import hudson.security.PermissionScope;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.*;
import org.kohsuke.stapler.verb.POST;
import org.springframework.lang.NonNull;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Extension
public class ConfigurationManagementLink extends ManagementLink {

    public static final PermissionGroup PERMISSIONS = new PermissionGroup(ConfigurationManagementLink.class, Messages._permission_component());

    public static final Permission VIEW_CONFIG = new Permission(PERMISSIONS, "View", Messages._permission_view(), Jenkins.ADMINISTER, PermissionScope.JENKINS);
    public static final Permission UPDATE_CONFIG = new Permission(PERMISSIONS, "Edit", Messages._permission_edit(), Jenkins.ADMINISTER, PermissionScope.JENKINS);

    private List<Config> configs = new ArrayList<>();

    @Override
    public String getIconFileName() {
        return "symbol-document-text-outline plugin-ionicons-api";
    }

    public GlobalYAMLPropertiesConfiguration getConfiguration() {
        if (!Jenkins.get().hasPermission(VIEW_CONFIG)) {
            return null; // hide it
        }
        return GlobalYAMLPropertiesConfiguration.get();
    }

    @POST
    @SuppressWarnings("unused")
    public HttpResponse doConfigure(StaplerRequest req, StaplerResponse rsp) throws ServletException {

        if (!Jenkins.get().hasPermission(UPDATE_CONFIG)) {
            return HttpResponses.errorWithoutStack(403, "You have no permissions to update global configuration");
        }

        // Get logger for ConfigurationManagementLink
        GlobalYAMLPropertiesConfiguration globalConfig = getConfiguration();
        String configNameField = "name";
        String yamlConfigField = "yamlConfig";
        String configSourceField = "configSource";

        JSONObject json = req.getSubmittedForm();
        Object configs = json.get("configs");
        List<JSONObject> configList = new ArrayList<>();
        if(configs instanceof net.sf.json.JSONArray) {
            net.sf.json.JSONArray configsArray = json.getJSONArray("configs");
            for (Object config : configsArray) {
                configList.add((JSONObject) config);
            }
        } else if(configs instanceof JSONObject) {
            configList.add((JSONObject)configs);
        }

        for (JSONObject obj : configList) {
            HttpResponses.HttpResponseException validationError = validateConfig(obj, configNameField, configSourceField, yamlConfigField);
            if (validationError != null) {
                return validationError;
            }
        }

        // If no exceptions were thrown during validation, bind the JSON to this instance
        req.bindJSON(globalConfig, json);
        try {
            globalConfig.refreshConfiguration();
        } catch (IOException e) {
            e.printStackTrace();
        }
        globalConfig.save();
        return HttpResponses.redirectTo(".");
    }

    HttpResponses.HttpResponseException validateConfig(JSONObject config, String nameField, String configSourceField, String yamlConfigField) {
        if (config == null) return null;
        if (!config.has(nameField) || !(config.get(nameField) instanceof String)) {
            return HttpResponses.errorWithoutStack(400, "Global YAML Configuration is not valid: Config name must be a string");
        }
        if (!config.has(configSourceField)) {
            return HttpResponses.errorWithoutStack(400, "Global YAML Configuration is not valid: Config has no configuration source");
        }
        JSONObject configSource = config.getJSONObject(configSourceField);
        if (configSource.get("stapler-class").equals(ConfigSourceManual.class.getName())) {
            FormValidation yamlConfigValidation = ConfigValidator.validateYamlConfig(configSource.getString(yamlConfigField));
            if (yamlConfigValidation.kind == FormValidation.Kind.ERROR) {
                return HttpResponses.errorWithoutStack(400, "Global YAML Configuration [" + config.get(nameField) + "] is not valid: " + yamlConfigValidation.getMessage());
            }
        }
        return null;
    }

    @POST
    public FormValidation doCheckName(@QueryParameter String value) {
        if (!Jenkins.get().hasPermission(UPDATE_CONFIG)) {
            return FormValidation.error("You have no permissions to update global configuration");
        }

        return ConfigValidator.validateName(value);
    }

    public List<Descriptor<ConfigSource>> getApplicableConfigSources() {
        Logger logger = Logger.getLogger(ConfigurationManagementLink.class.getName());
        logger.info("getApplicableConfigSources");
        List<Descriptor<ConfigSource>> applicableConfigSources = new ArrayList<>();
        applicableConfigSources.add(Jenkins.get().getDescriptor(ConfigSourceSCM.class));
        applicableConfigSources.add(Jenkins.get().getDescriptor(ConfigSourceManual.class));
        return applicableConfigSources;
    }

    @POST
    public FormValidation doCheckYamlConfig(@QueryParameter String value) {
        if (!Jenkins.get().hasPermission(UPDATE_CONFIG)) {
            return FormValidation.error("You have no permissions to update global configuration");
        }

        return ConfigValidator.validateYamlConfig(value);
    }

    @Override
    public String getUrlName() {
        return "globalyamlconfiguration"; // replace with your plugin name
    }

    @Override
    public String getDisplayName() {
        return "Global YAML Configuration"; // replace with your display name
    }

    @Override
    public String getDescription() {
        return "Manage Global YAML Configurations"; // replace with your description
    }

    @Override
    @NonNull
    public Category getCategory() {
        return Category.CONFIGURATION;
    }
}
