package containers.example.containers.dto;


public class PortConfig {
    private String protocol;
    private String exposedPort;
    private String hostPort;

    // Getters and Setters
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getExposedPort() {
        return exposedPort;
    }

    public void setExposedPort(String exposedPort) {
        this.exposedPort = exposedPort;
    }

    public String getHostPort() {
        return hostPort;
    }

    public void setHostPort(String hostPort) {
        this.hostPort = hostPort;
    }
}

