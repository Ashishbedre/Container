package containers.example.containers.dto;

import java.util.List;
import java.util.Map;

public class DockerCreateRequest {
    private String image;
    private List<String> env;
    private List<String> cmd;
    private String name;
    private HostConfig hostConfig;

    // Getters and setters...


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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HostConfig getHostConfig() {
        return hostConfig;
    }

    public void setHostConfig(HostConfig hostConfig) {
        this.hostConfig = hostConfig;
    }

    public static class HostConfig {
        private Map<String, List<PortBinding>> PortBindings;

        // Getters and setters...

        public Map<String, List<PortBinding>> getPortBindings() {
            return PortBindings;
        }

        public void setPortBindings(Map<String, List<PortBinding>> portBindings) {
            PortBindings = portBindings;
        }
    }

    public static class PortBinding {
        private String HostPort;

        public String getHostPort() {
            return HostPort;
        }

        public void setHostPort(String hostPort) {
            HostPort = hostPort;
        }
    }
}