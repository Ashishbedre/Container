package containers.example.containers.Controller;

//import containers.example.containers.Service.DockerService;
import containers.example.containers.Entity.Deployment;
import containers.example.containers.Service.DockerService;
import containers.example.containers.Service.Imp.DockerServiceImp;
import containers.example.containers.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;

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
    public ResponseEntity<Flux<Map<String, Object>>> getDockerInfo(@PathVariable boolean flag) {
        return ResponseEntity.ok(dockerService.getDockerInfo(flag));
    }

    @GetMapping("/AvaiableCpuAndMemory")
    public ResponseEntity<ResourceUsageDto> getAvaiableCpuAndMemory() {
        return ResponseEntity.ok(dockerService.getAvaiableCpuAndMemory());
    }

    @PostMapping("/create")
    public ResponseEntity<DockerContainerResponse> createContainer(@RequestBody ContainerConfigDto config ) {
        try {
            DockerContainerResponse response = dockerService.createContainer(config);
            return ResponseEntity.ok(response);
        } catch (DockerOperationException e) {
            DockerContainerResponse errorResponse = new DockerContainerResponse();
            errorResponse.setId(null);
            errorResponse.setWarnings(Collections.singletonList(e.getMessage()));
            return ResponseEntity.status(e.getStatusCode()).body(errorResponse);
        } catch (Exception e) {
            DockerContainerResponse errorResponse = new DockerContainerResponse();
            errorResponse.setId(null);
            errorResponse.setWarnings(Collections.singletonList("An unexpected error occurred: " + e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/start/{containerId}")
    public ResponseEntity<Deployment> startContainer(@PathVariable String containerId) {
        Deployment deployment = dockerService.startContainer(containerId);
        return ResponseEntity.ok(deployment);
    }

    @PostMapping("/stop/{containerId}")
    public ResponseEntity<Void> stopContainer(@PathVariable String containerId) {
        dockerService.stopDockerContainer(containerId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/deleteByContainerId/{containerId}")
    public ResponseEntity<String> deleteDeploymentByContainerId(@PathVariable String containerId) {
        boolean isDeleted = dockerService.deleteByContainerId(containerId);
        if (isDeleted) {
            return ResponseEntity.ok("Deployment deleted successfully.");
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    @PostMapping("/pull/{tag}")
    public ResponseEntity<Void> pull(@PathVariable String tag) {
        dockerService.pullDockerImage("mysql","latest","ashishbedre","123456789" ,null,null);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }


}
