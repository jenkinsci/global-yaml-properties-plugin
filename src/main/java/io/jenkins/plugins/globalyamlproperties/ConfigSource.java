package io.jenkins.plugins.globalyamlproperties;

import hudson.model.Describable;
import hudson.model.Descriptor;

import java.io.IOException;

public interface ConfigSource extends Describable<ConfigSource> {

    public String getYamlConfig();
}
