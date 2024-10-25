package containers.example.containers.dto;


import containers.example.containers.Entity.PortMapping;

import java.util.List;

public class ContainerConfigDto {

    private String deploymentId;

    private String status;
    private String imageName;
    private String imageTag;
    private List<String> env;
    private List<String> cmd;
    private String name;

    private Long Memory;  // Dedicated memory in bytes
    private String CpusetCpus;  // Dedicated CPU cores (e.g., "0,1")
    private List<PortMapping> portMappings;

    // New fields for authentication
    private String username;
    private String password;
    private String email;
    private String serverAddress;

    // Getters and setters


    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getImageTag() {
        return imageTag;
    }

    public void setImageTag(String imageTag) {
        this.imageTag = imageTag;
    }

    public List<String> getEnv() {
        return env;
    }

    public void setEnv(List<String> env) {
        this.env = env;
    }

    public List<String> getCmd() {
        return cmd;
    }

    public void setCmd(List<String> cmd) {
        this.cmd = cmd;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getMemory() {
        return Memory;
    }

    public void setMemory(Long memory) {
        Memory = memory;
    }

    public String getCpusetCpus() {
        return CpusetCpus;
    }

    public void setCpusetCpus(String cpusetCpus) {
        CpusetCpus = cpusetCpus;
    }

    public List<PortMapping> getPortMappings() {
        return portMappings;
    }

    public void setPortMappings(List<PortMapping> portMappings) {
        this.portMappings = portMappings;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }
}
