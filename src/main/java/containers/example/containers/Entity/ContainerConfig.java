package containers.example.containers.Entity;

import jakarta.persistence.*;

@Entity
public class ContainerConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String imageName;

    private String imageTag;

    private String name;

    @ElementCollection
    private String[] env;

    @ElementCollection
    private String[] cmd;

    private Integer exposedPort;

    private Integer hostPort;

//     One-to-one relationship with Deployment
    @OneToOne(mappedBy = "containerConfig", cascade = CascadeType.ALL)
    private Deployment deployment;

    // Getters and setters


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getEnv() {
        return env;
    }

    public void setEnv(String[] env) {
        this.env = env;
    }

    public String[] getCmd() {
        return cmd;
    }

    public void setCmd(String[] cmd) {
        this.cmd = cmd;
    }

    public Integer getExposedPort() {
        return exposedPort;
    }

    public void setExposedPort(Integer exposedPort) {
        this.exposedPort = exposedPort;
    }

    public Integer getHostPort() {
        return hostPort;
    }

    public void setHostPort(Integer hostPort) {
        this.hostPort = hostPort;
    }

    public Deployment getDeployment() {
        return deployment;
    }

    public void setDeployment(Deployment deployment) {
        this.deployment = deployment;
    }
}