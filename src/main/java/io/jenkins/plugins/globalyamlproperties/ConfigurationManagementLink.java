package io.jenkins.plugins.globalyamlproperties;

import hudson.Extension;
import hudson.model.ManagementLink;
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
import java.util.ArrayList;
import java.util.List;

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
    public HttpResponse doConfigure(StaplerRequest req, StaplerResponse rsp) throws ServletException {
        if (!Jenkins.get().hasPermission(UPDATE_CONFIG)) {
            return HttpResponses.errorWithoutStack(403, "You have no permissions to update global configuration");
        }
        // Get logger for ConfigurationManagementLink
        GlobalYAMLPropertiesConfiguration globalConfig = getConfiguration();
        String configNameField = "name";
        String yamlConfigField = "yamlConfig";

        JSONObject json = req.getSubmittedForm();

        Object configs = json.get("config");
        List<JSONObject> configList = new ArrayList<>();

        if(configs instanceof net.sf.json.JSONArray) {
            net.sf.json.JSONArray configsArray = json.getJSONArray("config");
            for (Object config : configsArray) {
                configList.add((JSONObject) config);
            }
        } else if(configs instanceof JSONObject){
            configList.add((JSONObject)configs);
        }


        for (JSONObject obj : configList) {
            if (obj == null) continue;

            if (!obj.has(configNameField) || !(obj.get(configNameField) instanceof String)) {
                return HttpResponses.errorWithoutStack(400, "Global YAML Configuration is not valid: Config name must be a string");
            }

            // If the "yamlConfig" key does not exist or is not a string, validation fails
            if (!obj.has(yamlConfigField) || !(obj.get(yamlConfigField) instanceof String)) {
                return HttpResponses.errorWithoutStack(400, "YAML config must be a string");
            }

            // Perform the validation
            FormValidation nameValidation = ConfigValidator.validateName(obj.getString(configNameField));
            if (nameValidation.kind != FormValidation.Kind.OK) {
                return HttpResponses.errorWithoutStack(400, "Global YAML Configuration is not valid: " + nameValidation.getMessage());
            }

            FormValidation yamlConfigValidation = ConfigValidator.validateYamlConfig(obj.getString(yamlConfigField));
            if (yamlConfigValidation.kind == FormValidation.Kind.ERROR) {
                return HttpResponses.errorWithoutStack(400, "Global YAML Configuration [" + obj.get(configNameField) + "] is not valid: " + yamlConfigValidation.getMessage());}
        }

        // If no exceptions were thrown during validation, bind the JSON to this instance
        req.bindJSON(globalConfig, json);
        globalConfig.save();
        return HttpResponses.redirectTo(".");
    }

    @POST
    public FormValidation doCheckName(@QueryParameter String value) {
        if (!Jenkins.get().hasPermission(UPDATE_CONFIG)) {
            return FormValidation.error("You have no permissions to update global configuration");
        }

        return ConfigValidator.validateName(value);
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
