package containers.example.containers.Controller;

//import containers.example.containers.Service.DockerService;
import containers.example.containers.Service.DockerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/docker")
public class DockerController {

    private final DockerService dockerService;

    @Autowired
    public DockerController(DockerService dockerService) {
        this.dockerService = dockerService;
    }
    @GetMapping("/info")
    public Mono<String> getDockerInfo() {
        return dockerService.getDockerInfo();
    }



//    @GetMapping("/containers")
//    public Mono<String> listContainers() {
//        return dockerService.listContainers();
//    }
}
