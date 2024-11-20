package containers.example.containers.Service;

import containers.example.containers.Entity.Deployment;
import containers.example.containers.dto.ContainerConfigDto;
import containers.example.containers.dto.DeploymentDto;
import reactor.core.publisher.Mono;

public interface DockerUpdateService {

    public Mono<String> updateContainerResourcesById(String containerName, ContainerConfigDto requestBody);

//    public Deployment inspectContainer(String containerName);
    public DeploymentDto inspectContainer(String containerId);

//    public DeploymentDto inspectContainer1(String containerName);
}
