package containers.example.containers.dto;


public class UpdateContainerRequest {
    private long memory;      // Memory in bytes
    private String cpusetCpus; // Restrict CPU cores

    public UpdateContainerRequest() {
    }

    public UpdateContainerRequest(long memory, String cpusetCpus) {
        this.memory = memory;
        this.cpusetCpus = cpusetCpus;
    }

    public long getMemory() {
        return memory;
    }

    public void setMemory(long memory) {
        this.memory = memory;
    }

    public String getCpusetCpus() {
        return cpusetCpus;
    }

    public void setCpusetCpus(String cpusetCpus) {
        this.cpusetCpus = cpusetCpus;
    }
}