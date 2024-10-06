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
    public ResponseEntity<Mono<String>> getDockerInfo(@PathVariable boolean flag) {
        return ResponseEntity.ok(dockerService.getDockerInfo(flag));
    }

    @GetMapping("/AvaiableCpuAndMemory")
    public ResponseEntity<ResourceUsageDto> getAvaiableCpuAndMemory() {
        return ResponseEntity.ok(dockerService.getAvaiableCpuAndMemory());
    }

    @PostMapping("/create")
    public ResponseEntity<DockerContainerResponse> createContainer(@RequestBody ContainerConfigDto config ) {
        return ResponseEntity.ok(dockerService.createContainer(config));
    }

    @PostMapping("/start/{containerName}")
    public ResponseEntity<Deployment> startContainer(@PathVariable String containerName) {
        Deployment deployment = dockerService.startContainerByApi(containerName);
        return ResponseEntity.ok(deployment);
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
    @PostMapping("/pull/{tag}")
    public ResponseEntity<Void> pull(@PathVariable String tag) {
        dockerService.pullDockerImage("mysql","latest","ashishbedre","123456789" ,null,null);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }


}
