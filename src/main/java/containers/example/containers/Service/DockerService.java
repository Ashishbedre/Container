package containers.example.containers.Service;

import containers.example.containers.Entity.Deployment;
import containers.example.containers.dto.ContainerConfigDto;
import containers.example.containers.dto.DockerContainerResponse;
import containers.example.containers.dto.DockerInfoResponse;
import containers.example.containers.dto.UpdateContainerRequest;
import reactor.core.publisher.Mono;

public interface DockerService {

    public Mono<String> getDockerInfo(boolean flag);

    public DockerInfoResponse getAvaiableCpuAndMemory();

    public DockerContainerResponse createContainer(ContainerConfigDto config);

    public Deployment startContainerByApi(String containerName);
    public void stopContainer(String containerName);

//    public Mono<String> updateContainerResourcesByName(String containerName, UpdateContainerRequest requestBody);
    public boolean deleteByContainerName(String containerName);

    public void pullDockerImage(String image, String tag);



}
