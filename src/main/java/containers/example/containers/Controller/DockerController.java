package containers.example.containers.Controller;

//import containers.example.containers.Service.DockerService;
import containers.example.containers.Entity.ContainerConfig;
import containers.example.containers.Entity.Deployment;
import containers.example.containers.Service.DockerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/docker")
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


    @PostMapping("/create")
    public ResponseEntity<Deployment> createContainer(@RequestBody ContainerConfig config) {
        Deployment deployment = dockerService.createContainer(config);
        return ResponseEntity.ok(deployment);
    }

    @PostMapping("/stop/{containerName}")
    public ResponseEntity<Void> stopContainer(@PathVariable String containerName) {
        dockerService.stopContainer(containerName);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }




//    @GetMapping("/containers")
//    public Mono<String> listContainers() {
//        return dockerService.listContainers();
//    }
}
