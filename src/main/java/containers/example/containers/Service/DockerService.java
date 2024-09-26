package containers.example.containers.Service;

import containers.example.containers.Entity.Deployment;
import containers.example.containers.dto.*;
import reactor.core.publisher.Mono;

public interface DockerService {

    public Mono<String> getDockerInfo(boolean flag);

    public ResourceUsageDto getAvaiableCpuAndMemory();

    public DockerContainerResponse createContainer(ContainerConfigDto config);

    public Deployment startContainerByApi(String containerName);
    public void stopContainer(String containerName);

//    public Mono<String> updateContainerResourcesByName(String containerName, UpdateContainerRequest requestBody);
    public boolean deleteByContainerName(String containerName);

    public void pullDockerImage(String image, String tag);



}
