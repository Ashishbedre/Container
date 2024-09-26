package containers.example.containers.Service;

import containers.example.containers.Entity.Deployment;
import containers.example.containers.dto.ContainerConfigDto;
import reactor.core.publisher.Mono;

public interface DockerUpdateService {

    public Mono<String> updateContainerResourcesByName(String containerName, ContainerConfigDto requestBody);

    public Deployment inspectContainer(String containerName);
}
