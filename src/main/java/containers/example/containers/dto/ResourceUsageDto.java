package containers.example.containers.dto;

public class ResourceUsageDto {
    private int  availableCpu;  // Available CPU cores (e.g., "0-3")

    private int leftCpu;       // Used CPU cores (e.g., "1,2")

    private Long availableMemory; // Available memory in bytes
    private Long leftMemory;      // Used memory in bytes


    public int getAvailableCpu() {
        return availableCpu;
    }

    public void setAvailableCpu(int availableCpu) {
        this.availableCpu = availableCpu;
    }

    public int getLeftCpu() {
        return leftCpu;
    }

    public void setLeftCpu(int leftCpu) {
        this.leftCpu = leftCpu;
    }

    public Long getAvailableMemory() {
        return availableMemory;
    }

    public void setAvailableMemory(Long availableMemory) {
        this.availableMemory = availableMemory;
    }

    public Long getLeftMemory() {
        return leftMemory;
    }

    public void setLeftMemory(Long leftMemory) {
        this.leftMemory = leftMemory;
    }

    // Constructors
    public ResourceUsageDto() {
    }

    public ResourceUsageDto(int availableCpu, int leftCpu, Long availableMemory, Long leftMemory) {
        this.availableCpu = availableCpu;
        this.leftCpu = leftCpu;
        this.availableMemory = availableMemory;
        this.leftMemory = leftMemory;
    }

    // Getters and setters

}
