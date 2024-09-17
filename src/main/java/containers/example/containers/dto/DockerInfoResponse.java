package containers.example.containers.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DockerInfoResponse {

    @JsonProperty("NCPU")
    private int ncpu;

    @JsonProperty("MemTotal")
    private Long memTotal;

    // Getters and Setters

    public int getNcpu() {
        return ncpu;
    }

    public void setNcpu(int ncpu) {
        this.ncpu = ncpu;
    }

    public Long getMemTotal() {
        return memTotal;
    }

    public void setMemTotal(Long memTotal) {
        this.memTotal = memTotal;
    }
}