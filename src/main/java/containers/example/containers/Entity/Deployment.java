package containers.example.containers.Entity;

import jakarta.persistence.*;

@Entity
public class Deployment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String deploymentName;

    public Deployment(String deploymentName, ContainerConfig containerConfig) {
        this.deploymentName = deploymentName;
        this.containerConfig = containerConfig;
    }

    // One-to-one relationship with ContainerConfig
    @OneToOne
    @JoinColumn(name = "container_config_id")
    private ContainerConfig containerConfig;

    // Getters and setters


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public void setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
    }

    public ContainerConfig getContainerConfig() {
        return containerConfig;
    }

    public void setContainerConfig(ContainerConfig containerConfig) {
        this.containerConfig = containerConfig;
    }
}