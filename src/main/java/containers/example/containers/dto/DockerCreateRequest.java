package containers.example.containers.dto;

import java.util.List;
import java.util.Map;


public class DockerCreateRequest {
    private String image;
    private List<String> env;
    private List<String> cmd;
    private HostConfig hostConfig;
//    private String name;
    private Map<String, Object> exposedPorts;

    // Getters and setters


    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
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

    public HostConfig getHostConfig() {
        return hostConfig;
    }

    public void setHostConfig(HostConfig hostConfig) {
        this.hostConfig = hostConfig;
    }

//    public String getName() {
//        return name;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }

    public Map<String, Object> getExposedPorts() {
        return exposedPorts;
    }

    public void setExposedPorts(Map<String, Object> exposedPorts) {
        this.exposedPorts = exposedPorts;
    }

    public static class HostConfig {
        private Map<String, List<PortBinding>> portBindings;

        private Long Memory;  // Dedicated memory in bytes
        private String CpusetCpus;  // Dedicated CPU cores (e.g., "0,1")

        // Getter and setter


        public Map<String, List<PortBinding>> getPortBindings() {
            return portBindings;
        }

        public void setPortBindings(Map<String, List<PortBinding>> portBindings) {
            this.portBindings = portBindings;
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
    }

    public static class PortBinding {
        private String hostPort;

        // Getter and setter


        public String getHostPort() {
            return hostPort;
        }

        public void setHostPort(String hostPort) {
            this.hostPort = hostPort;
        }
    }
}
