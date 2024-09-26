package containers.example.containers.dto;


public class UpdateContainerRequest {
    private long memory;          // Memory in bytes
    private long memorySwap;      // Memory swap in bytes
    private String cpusetCpus;    // Restrict CPU cores

    public UpdateContainerRequest() {
    }

    public UpdateContainerRequest(long memory, long memorySwap, String cpusetCpus) {
        this.memory = memory;
        this.memorySwap = memorySwap;
        this.cpusetCpus = cpusetCpus;
    }

    public long getMemory() {
        return memory;
    }

    public void setMemory(long memory) {
        this.memory = memory;
    }

    public long getMemorySwap() {
        return memorySwap;
    }

    public void setMemorySwap(long memorySwap) {
        this.memorySwap = memorySwap;
    }

    public String getCpusetCpus() {
        return cpusetCpus;
    }

    public void setCpusetCpus(String cpusetCpus) {
        this.cpusetCpus = cpusetCpus;
    }
}