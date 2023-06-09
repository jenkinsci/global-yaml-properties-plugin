# Global Properties Plugin

## Introduction

The GlobalProperties Plugin is a universal extension for Jenkins that enhances the global configuration page by adding a custom configurations field.
This field allows users to define global properties in YAML format, which is then parsed into a HashMap and can be accessed throughout specific build step.

The plugin simplifies the management of global properties and configurations by centralizing them within Jenkins. You can define key-value pairs, nested structures, and arrays using YAML, making it easier to express complex relationships.

Once the properties are saved, they are automatically parsed into a HashMap, enabling easy access throughout your Jenkins pipelines or jobs. This promotes dynamic and customizable workflows.

With the GlobalProperties Plugin, you can efficiently manage environment-specific variables, deployment settings, and other configurable parameters. Say goodbye to scattered configuration files and embrace streamlined configuration management within Jenkins.

### Main Goals

1. Backward compatibility for shared libraries: The GlobalProperties Plugin aims to ensure backward compatibility for private shared libraries that contain environment-specific information such as IP addresses, ports, and other configuration details. 
By centralizing and managing these properties within the plugin, it allows for easier maintenance and updates, ensuring smooth integration with existing libraries.
Example: You have self-hosted shared library that obtains toolchain from FTP to workspace and builds your product by running some commands.
Build commands and FTP address can be changed over time. Now imagine situation when you need to build old version of your product.
You have to build product using old version of shared library, where FTP address is deprecated.
Global Properties Plugin allows you to define FTP address in one place and update it easily. In shred library you can just obtain value from global configuration and it will be always correct across all versions of shared library.
2. Unify handling of actual configuration: The plugin provides a unified and centralized location to handle actual configurations used across various places within Jenkins. As configurations may change or become deprecated over time, the GlobalProperties Plugin offers a streamlined approach to manage and update these configurations effectively. By consolidating them in one place, it promotes consistency and simplifies the configuration management process.
## Getting started
### Recommendations:
1. Think twice about configurations and format you want to put into global properties.
   If the keys and format changes frequently, this eliminates the benefit of backwards compatibility.

2. DO NOT put there sensitive data like passwords or tokens.
   If you need to put there some sensitive data, you can use [Credentials Plugin](https://plugins.jenkins.io/credentials/) and [Credentials Binding Plugin](https://plugins.jenkins.io/credentials-binding/) to access them in your pipeline.
   In this case global properties can be nice feature for handling credential IDs.

### Configuration
At first, define your YAML configuration in the global configuration page of Jenkins.

 Manage Jenkins -> Configure System -> Global Properties Configuration

![Global Properties Configuration](docs/images/configuration.png)

Then, you can access the configuration in your pipeline or freestyle job.

### Usage

Step `getGlobalYAMLProperties` returns HashMap object which is parsed from YAML configuration.
Here is pipeline example which explains to access the configuration:
```groovy
def myProperties = getGlobalYAMLProperties()
String message = "Message for:\n${myProperties['devopsTeam'].join('\n')}\n"
message += "Guys, you don't need to update FTP server into each pipeline separately,\n"
message += "I've put FTP address to Global Properties and updated it easily in one place.\n"
message += "Let's go drink beer."
echo message
String ftpCommand = "curl -O ftp://${myProperties.ftpServerAddress}/path/to/file"
println "Command to download file from internal ftp:"
println ftpCommand
```
Pipeline output:
![Global Properties Configuration](docs/images/output_example.png)

## Issues

Report issues and enhancements in the [Jenkins issue tracker](https://issues.jenkins-ci.org/).

## Contributing

Refer to our [contribution guidelines](CONTRIBUTING.md)

## LICENSE

Licensed under MIT, see [LICENSE](LICENSE.md)

