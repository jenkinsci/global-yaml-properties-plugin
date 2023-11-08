package io.jenkins.plugins.globalyamlproperties;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github_branch_source.Connector;
import org.kohsuke.github.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;
import org.jenkinsci.plugins.github_branch_source.GitHubAppCredentials;

import java.io.*;
import java.util.List;


public class ConfigSourceSCM implements ConfigSource, Serializable {

    private String yamlConfig;

    private String repositoryName;
    private String repositoryOwner;

    private String ref;
    private String credentialsId;
    private String path;

    @DataBoundConstructor
    public ConfigSourceSCM(String repositoryName, String repositoryOwner, String ref, String credentialsId, String path) {
        this.ref = ref;
        this.credentialsId = credentialsId;
        this.path = path;
        this.repositoryOwner = repositoryOwner;
        this.repositoryName = repositoryName;
    }

    @Override
    public String getYamlConfig() {
        return yamlConfig;
    }

    @DataBoundSetter
    void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @DataBoundSetter
    void setRef(String ref) {
        this.ref = ref;
    }

    @DataBoundSetter
    void setPath(String path) {
        this.path = path;
    }

    @DataBoundSetter
    void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    @DataBoundSetter
    void setRepositoryOwner(String repositoryOwner) {
        this.repositoryOwner = repositoryOwner;
    }

    public String getRepositoryOwner() {
        return this.repositoryOwner;
    }

    public String getRepositoryName() {
        return this.repositoryName;
    }

    public String getRef() {
        return this.ref;
    }

    public String getPath() {
        return this.path;
    }

    public String getCredentialsId() {
        return this.credentialsId;
    }

    public void fetchConfiguration() throws IOException {
        GitHub connect = Connector.connect("https://api.github.com", (GitHubAppCredentials) Utils.getCredentialsById(this.credentialsId));
        try {
            GHRepository repository = connect.getRepository(repositoryOwner + "/" + repositoryName);
            GHContent content = repository.getFileContent(path, ref);
            this.yamlConfig = Utils.readInputStream(content.read());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Connector.release(connect);
        }
    }

    @Override
    public ConfigSourceSCM.DescriptorImpl getDescriptor() {
        return Jenkins.get().getDescriptorByType(ConfigSourceSCM.DescriptorImpl.class);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ConfigSource> {

        @SuppressWarnings("unused")
        @POST
        public FormValidation doCheckName(@QueryParameter String value) {
            return ConfigValidator.validateName(value);
        }

        @SuppressWarnings("unused")
        @POST
        public FormValidation doCheckYamlConfig(@QueryParameter String value) {
            if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                return FormValidation.error("Only administrators can update global configuration");
            }
            return ConfigValidator.validateYamlConfig(value);
        }

        @POST
        @SuppressWarnings("unused")
        public FormValidation doValidate(
                @QueryParameter("repositoryName") String repositoryName,
                @QueryParameter("repositoryOwner") String repositoryOwner,
                @QueryParameter("ref") String ref,
                @QueryParameter("credentialsId") String credentialsId,
                @QueryParameter("path") String path
        ) {
            GitHubAppCredentials requestedCredentials = (GitHubAppCredentials) Utils.getCredentialsById(credentialsId);
            try {
                GitHub connect = Connector.connect("https://api.github.com", requestedCredentials);
                try {
                    GHRepository repository = connect.getRepository(repositoryOwner + "/" + repositoryName);
                    GHContent content = repository.getFileContent(path, ref);
                    String yamlConfig = Utils.readInputStream(content.read());
                    return FormValidation.ok("Success, Remaining rate limit: "
                            + connect.getRateLimit().getRemaining() + "\n" + yamlConfig);
                } finally {
                    Connector.release(connect);
                }
            } catch (Exception e) {
                return FormValidation.error(e.toString());
            }
        }

        @SuppressWarnings("unused")
        @POST
        public ListBoxModel doFillCredentialsIdItems() {
            ListBoxModel items = new ListBoxModel();
            List<GitHubAppCredentials> credentials = CredentialsProvider.lookupCredentials(
                    GitHubAppCredentials.class
            );

            for (GitHubAppCredentials c : credentials) {
                items.add(c.getId());
            }

            return items;
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Config from SCM";
        }
    }
}
