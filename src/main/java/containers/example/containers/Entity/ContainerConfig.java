package containers.example.containers.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    private String status;

    private boolean State;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> env;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> cmd;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PortMapping> portMappings;

    @OneToOne(mappedBy = "containerConfig", cascade = CascadeType.ALL)
    @JsonIgnore
    private Deployment deployment;

    // New fields for authentication
    private String username;
    private String password;
    private String email;
    private String serverAddress;


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


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public boolean isState() {
        return State;
    }

    public void setState(boolean state) {
        State = state;
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