package containers.example.containers.Controller;


import containers.example.containers.Entity.Deployment;
import containers.example.containers.Service.Imp.DockerUpdateServiceImp;
import containers.example.containers.dto.ContainerConfigDto;
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
    DockerUpdateServiceImp dockerUpdateServiceImp;

    @PostMapping("/containers/{containerName}/update")
    public Mono<ResponseEntity<String>> updateContainer(@PathVariable String containerName,
                                                        @RequestBody ContainerConfigDto requestBody) {
        return dockerUpdateServiceImp.updateContainerResourcesByName(containerName,requestBody)
                .map(response -> ResponseEntity.ok(response))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body("Failed to update container")));
    }

    @GetMapping("/containers/{containerName}/inspect")
    public Deployment inspectContainer(@PathVariable String containerName) {
        return dockerUpdateServiceImp.inspectContainer(containerName);
//                .map(response -> ResponseEntity.ok().body(response))
//                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(e.getMessage())));
    }


}