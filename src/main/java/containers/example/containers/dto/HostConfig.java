package containers.example.containers.dto;


import java.util.List;
import java.util.Map;

public class HostConfig {
    private Map<String, List<PortBinding>> portBindings;

    public Map<String, List<PortBinding>> getPortBindings() {
        return portBindings;
    }

    public void setPortBindings(Map<String, List<PortBinding>> portBindings) {
        this.portBindings = portBindings;
    }
}
