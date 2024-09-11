package containers.example.containers.Entity;


import jakarta.persistence.*;


@Entity
public class PortMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String protocol;
    private String exposedPort;
    private String hostPort;

    @ManyToOne
//    @JoinColumn(name = "container_config_id")
    private ContainerConfig containerConfig;

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public ContainerConfig getContainerConfig() {
        return containerConfig;
    }

    public void setContainerConfig(ContainerConfig containerConfig) {
        this.containerConfig = containerConfig;
    }
    // ...


}

