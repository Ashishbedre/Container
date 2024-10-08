package containers.example.containers.Controller;


import containers.example.containers.Entity.Deployment;
import containers.example.containers.Service.DockerUpdateService;
import containers.example.containers.Service.Imp.DockerUpdateServiceImp;
import containers.example.containers.dto.ContainerConfigDto;
import containers.example.containers.dto.DeploymentDto;
import containers.example.containers.dto.UpdateContainerRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/docker")
@CrossOrigin("*")
public class DockerUpdateController {

    @Autowired
    DockerUpdateService dockerUpdateService;

    @PostMapping("/containers/{containerName}/update")
    public Mono<ResponseEntity<String>> updateContainer(@PathVariable String containerName,
                                                        @RequestBody ContainerConfigDto requestBody) {
        return dockerUpdateService.updateContainerResourcesByName(containerName,requestBody)
                .map(response -> ResponseEntity.ok(response))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body("Failed to update container")));
    }

    @GetMapping("/containers/{containerName}/inspect")
    public DeploymentDto inspectContainer(@PathVariable String containerName) {
        return dockerUpdateService.inspectContainer(containerName);
    }


}
