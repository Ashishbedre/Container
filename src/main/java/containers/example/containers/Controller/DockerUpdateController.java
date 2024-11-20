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

    @PostMapping("/containers/{containerId}/update")
    public Mono<ResponseEntity<String>> updateContainer(@PathVariable String containerId,
                                                        @RequestBody ContainerConfigDto requestBody) {
        return dockerUpdateService.updateContainerResourcesById(containerId,requestBody)
                .map(response -> ResponseEntity.ok(response))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body("Failed to update container")));
    }

    @GetMapping("/containers/{containerId}/inspect")
    public DeploymentDto inspectContainer(@PathVariable String containerId) {
        return dockerUpdateService.inspectContainer(containerId);
    }


}
