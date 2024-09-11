package containers.example.containers.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class DockerContainerResponse {
    @JsonProperty("Id")
    private String id;

    @JsonProperty("Warnings")
    private List<String> warnings;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    @Override
    public String toString() {
        return "DockerContainerResponse{id='" + id + "', warnings=" + warnings + "}";
    }
}


