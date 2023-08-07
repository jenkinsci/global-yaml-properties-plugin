package io.jenkins.plugins.globalyamlproperties;

import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.Map;

public class ConfigValidator {
    public static FormValidation validateName(String name) {
        if (name == null || name.isEmpty()) {
            return FormValidation.error("Name cannot be empty");
        }
        return FormValidation.ok();
    }

    public static FormValidation validateYamlConfig(String value) {
        if (StringUtils.isEmpty(value)) {
            return FormValidation.warning("Config is empty");
        }
        Yaml parser = new Yaml();
        // Exception will be thrown also when YAML is actually valid but can not be cast to Map.
        try {
            Object parsedYAML = parser.load(value);
            if (!(parsedYAML instanceof Map))
                throw new GlobalYAMLPropertiesConfigurationException("Provided config's root element is not a Map");
        } catch (YAMLException e) {
            return FormValidation.error("Config is not a valid YAML file");
        } catch (GlobalYAMLPropertiesConfigurationException e) {
            return FormValidation.error(
                    "Specified YAML is valid, but root element is not a Map. Please, use key-value format for root element");
        }

        return FormValidation.ok("YAML config is valid");
    }
}
