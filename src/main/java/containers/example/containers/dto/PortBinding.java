package containers.example.containers.dto;

public class PortBinding {
    private String hostPort;

    public PortBinding(String hostPort) {
        this.hostPort = hostPort;
    }

    public String getHostPort() {
        return hostPort;
    }

    public void setHostPort(String hostPort) {
        this.hostPort = hostPort;
    }
}
