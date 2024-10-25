package containers.example.containers.Service;

import containers.example.containers.Entity.Deployment;
import containers.example.containers.dto.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface DockerService {

    public Flux<Map<String, Object>> getDockerInfo(boolean flag);

    public ResourceUsageDto getAvaiableCpuAndMemory();

    public DockerContainerResponse createContainer(ContainerConfigDto config);

//    public Deployment startContainerByApi(String containerName);
//    public void stopContainer(String containerName);

//    public Mono<String> updateContainerResourcesByName(String containerName, UpdateContainerRequest requestBody);

    public void stopDockerContainer(String containerId);

    public Deployment startContainer(String containerId);

    public boolean deleteByContainerId(String containerName);

    public void pullDockerImage(String image, String tag, String username,String password,String email,String serverAddress);



}
