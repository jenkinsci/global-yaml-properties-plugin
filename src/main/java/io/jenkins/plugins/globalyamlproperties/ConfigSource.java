package io.jenkins.plugins.globalyamlproperties;

import hudson.model.Describable;

public interface ConfigSource extends Describable<ConfigSource> {

    String getYamlConfig();
}
