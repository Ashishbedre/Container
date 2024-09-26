package containers.example.containers.Service.Imp;


import containers.example.containers.Entity.ContainerConfig;
import containers.example.containers.Entity.Deployment;
import containers.example.containers.Entity.PortMapping;
import containers.example.containers.Repository.ContainerConfigRepository;
import containers.example.containers.Repository.DeploymentRepository;
import containers.example.containers.Service.DockerUpdateService;
import containers.example.containers.dto.ContainerConfigDto;
import containers.example.containers.dto.UpdateContainerRequest;
import io.netty.handler.ssl.SslContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.io.File;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class DockerUpdateServiceImp implements DockerUpdateService {

    private final WebClient webClient;

    @Value("${docker.api.url}")
    private String dockerApiUrl;

    @Value("${docker.api.scheme}")
    private String scheme;

    @Value("${docker.api.host}")
    private String host;

    @Value("${docker.api.port}")
    private int port;

    @Autowired
    private ContainerConfigRepository containerConfigRepository;

    @Autowired
    private DeploymentRepository deploymentRepository;

    @Autowired
    private DockerServiceImp dockerServiceImp;

    private static final Logger logger = LoggerFactory.getLogger(DockerServiceImp.class);

    public DockerUpdateServiceImp(WebClient webClient) {
        this.webClient = webClient;
    }


    public Mono<String> updateContainerResourcesByName(String containerName, ContainerConfigDto requestBody){
        Deployment deployment = deploymentRepository.findByContainerName(containerName)
                .orElseThrow(() -> new NoSuchElementException("Deployment with container name '" + containerName + "' not found."));

        // Check if it's only a memory or CPU update
        if (isOnlyMemoryOrCpuUpdate(containerName,requestBody)) {
            return updateContainerResources(deployment.getDeploymentId(), requestBody);
        } else {
            return recreateContainer(containerName, requestBody);
        }

    }

    public Deployment inspectContainer(String containerName) {

        Deployment deployment = deploymentRepository.findByContainerName(containerName)
                .orElseThrow(() -> new NoSuchElementException("Deployment with container name '" + containerName + "' not found."));

        return  deployment;
    }


    private Mono<String>  recreateContainer(String containerName, ContainerConfigDto requestBody){
        Deployment deployment = deploymentRepository.findByContainerName(containerName)
                .orElseThrow(() -> new NoSuchElementException("Deployment with container name '" + containerName + "' not found."));
        ContainerConfigDto recreateBody = updateDeploymentFromDto(deployment,requestBody);
        try {
            dockerServiceImp.deleteByContainerName(containerName);
            deploymentRepository.deleteById(deployment.getId());
            dockerServiceImp.createContainer(recreateBody);

        }catch (Exception e){
            System.out.println(e.getMessage());
        }
        return Mono.just("Container recreated successfully");
    }
    public ContainerConfigDto updateDeploymentFromDto(Deployment deployment,ContainerConfigDto requestBody) {

        // Update or replace the fields
        if (requestBody.getImageName() == null) {
            requestBody.setImageName(deployment.getContainerConfig().getImageName());
        }
        if (requestBody.getImageTag() == null) {
            requestBody.setImageTag(deployment.getContainerConfig().getImageTag());
        }
        if (requestBody.getEnv() == null) {
            requestBody.setEnv(deployment.getContainerConfig().getEnv());
        }
        if (requestBody.getCmd() == null) {
            requestBody.setCmd(deployment.getContainerConfig().getCmd());
        }
        if (requestBody.getName() == null) {
            requestBody.setName(deployment.getContainerConfig().getName());
        }
        if (requestBody.getMemory() == null) {
            requestBody.setMemory(deployment.getContainerConfig().getMemory());
        }
        if (requestBody.getCpusetCpus() == null) {
            requestBody.setCpusetCpus(deployment.getContainerConfig().getCpu());
        }
        if (requestBody.getPortMappings() == null) {
            requestBody.setPortMappings(deployment.getContainerConfig().getPortMappings());
        }
        return requestBody;
    }

    private boolean isOnlyMemoryOrCpuUpdate(String containerName, ContainerConfigDto request) {
        return (((request.getMemory() != null || request.getCpusetCpus() != null)) ||
                ((request.getMemory() == null && request.getCpusetCpus() == null))) &&
                !changesAffectRecreation(containerName,request);
    }

    private boolean changesAffectRecreation(String containerName,ContainerConfigDto request) {
        Deployment deployment = deploymentRepository.findByContainerName(containerName)
                .orElseThrow(() -> new NoSuchElementException("Deployment with container name '" + containerName + "' not found."));
        return (request.getImageName() != null && !request.getImageName().equals(deployment.getContainerConfig().getImageName()))
                || (request.getImageTag() != null && !request.getImageTag().equals(deployment.getContainerConfig().getImageTag()))
                || (request.getEnv() != null && !request.getEnv().isEmpty() && !request.getEnv().equals(deployment.getContainerConfig().getEnv()))
                || (request.getCmd() != null && !request.getCmd().isEmpty() && !request.getCmd().equals(deployment.getContainerConfig().getCmd()))
                || (request.getPortMappings() != null && !request.getPortMappings().isEmpty() && !comparePortMappings(request.getPortMappings(), deployment.getContainerConfig().getPortMappings()));


    }
    private boolean comparePortMappings(List<PortMapping> requestPortMappings, List<PortMapping> existingPortMappings) {
        if (requestPortMappings.size() != existingPortMappings.size()) {
            return false;
        }

        // Compare the fields in PortMapping manually
        for (int i = 0; i < requestPortMappings.size(); i++) {
            PortMapping reqMapping = requestPortMappings.get(i);
            PortMapping existingMapping = existingPortMappings.get(i);

            if (reqMapping.getExposedPort() != null && !reqMapping.getExposedPort().equals(existingMapping.getExposedPort())) {
                return false;
            }
            if (reqMapping.getHostPort() != null && !reqMapping.getHostPort().equals(existingMapping.getHostPort())) {
                return false;
            }
            if (reqMapping.getProtocol() != null && !reqMapping.getProtocol().equals(existingMapping.getProtocol())) {
                return false;
            }
        }

        return true;
    }


    //     Update memory and cpusetCpus
    public Mono<String> updateContainerResources(String containerId,ContainerConfigDto requestBody) {

        UpdateContainerRequest request = new UpdateContainerRequest(requestBody.getMemory(),requestBody.getMemory(), requestBody.getCpusetCpus());

        return webClient.post()
                .uri(dockerApiUrl + "/containers/" + containerId + "/update")
                .body(Mono.just(request), UpdateContainerRequest.class)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response -> {
                    Deployment deployment = deploymentRepository.findByDeploymentId(containerId)
                            .orElseThrow(() -> new NoSuchElementException("Deployment with deploymentId '" + containerId + "' not found."));
                    ContainerConfig containerConfig = deployment.getContainerConfig();
//                    containerConfig.setMemory(requestBody.getMemory());
//                    containerConfig.setCpu(requestBody.getCpusetCpus());
                    containerConfig.setCpu(requestBody.getCpusetCpus() != null ? requestBody.getCpusetCpus() : "0");
                    containerConfig.setMemory(requestBody.getMemory() != null ? requestBody.getMemory() : 0L);
                    deploymentRepository.save(deployment);
                    return Mono.just(response); // Return the response from Docker API
                })
                .onErrorResume(e -> Mono.just("Error updating container: " + e.getMessage()));
    }

}
