package containers.example.containers.Entity;

import jakarta.persistence.*;

import java.util.List;

@Entity
public class ContainerConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String imageName;
    private String imageTag;
    private String name;

    private String cpu;

    private Long memory;

    @ElementCollection
    private List<String> env;

    @ElementCollection
    private List<String> cmd;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PortMapping> portMappings;

    @OneToOne(mappedBy = "containerConfig", cascade = CascadeType.ALL)
    private Deployment deployment;



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


    public String getCpu() {
        return cpu;
    }

    public void setCpu(String cpu) {
        this.cpu = cpu;
    }

    public Long getMemory() {
        return memory;
    }

    public void setMemory(Long memory) {
        this.memory = memory;
    }

    public void setName(String name) {
        this.name = name;
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

    public List<PortMapping> getPortMappings() {
        return portMappings;
    }

    public void setPortMappings(List<PortMapping> portMappings) {
        this.portMappings = portMappings;
    }

    public Deployment getDeployment() {
        return deployment;
    }

    public void setDeployment(Deployment deployment) {
        this.deployment = deployment;
    }
}