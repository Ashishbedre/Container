package containers.example.containers.Controller;

//import containers.example.containers.Service.DockerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import containers.example.containers.Entity.ContainerConfig;
import containers.example.containers.Entity.Deployment;
import containers.example.containers.Service.DockerService;
import containers.example.containers.dto.ContainerConfigDto;
import containers.example.containers.dto.DockerContainerResponse;
import containers.example.containers.dto.DockerInfoResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/docker")
@CrossOrigin("*")
public class DockerController {

    private final DockerService dockerService;

    @Autowired
    public DockerController(DockerService dockerService) {
        this.dockerService = dockerService;
    }
    @GetMapping("/info/{flag}")
    public Mono<String> getDockerInfo(@PathVariable boolean flag) {
        return dockerService.getDockerInfo(flag);
    }

    @GetMapping("/AvaiableCpuAndMemory")
    public DockerInfoResponse getAvaiableCpuAndMemory() {
        return dockerService.getAvaiableCpuAndMemory();
    }

    @PostMapping("/create")
    public ResponseEntity<Mono<DockerContainerResponse>> createContainer(@RequestBody ContainerConfigDto config ) {
        return ResponseEntity.ok(dockerService.createContainer(config));
    }

    @PostMapping("/start/{containerName}")
    public Deployment startContainer(@PathVariable String containerName) {
        return dockerService.startContainerByApi(containerName);
    }

    @PostMapping("/stop/{containerName}")
    public ResponseEntity<Void> stopContainer(@PathVariable String containerName) {
        dockerService.stopContainer(containerName);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/deleteByContainerName/{containerName}")
    public ResponseEntity<String> deleteDeploymentByContainerName(@PathVariable String containerName) {
        boolean isDeleted = dockerService.deleteByContainerName(containerName);
        if (isDeleted) {
            return ResponseEntity.ok("Deployment deleted successfully.");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

}
