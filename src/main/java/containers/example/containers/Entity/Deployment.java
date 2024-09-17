package containers.example.containers.Entity;

import jakarta.persistence.*;


@Entity
public class Deployment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String deploymentId;

    private String containerName;

    @OneToOne(cascade = CascadeType.ALL)
    private ContainerConfig containerConfig;



    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public ContainerConfig getContainerConfig() {
        return containerConfig;
    }

    public void setContainerConfig(ContainerConfig containerConfig) {
        this.containerConfig = containerConfig;
    }
    // ...


}