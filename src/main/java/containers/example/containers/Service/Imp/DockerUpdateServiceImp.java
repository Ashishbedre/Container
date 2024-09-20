package containers.example.containers.Service.Imp;


import containers.example.containers.Entity.ContainerConfig;
import containers.example.containers.Entity.Deployment;
import containers.example.containers.Repository.ContainerConfigRepository;
import containers.example.containers.Repository.DeploymentRepository;
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
import java.util.NoSuchElementException;

@Service
public class DockerUpdateServiceImp {

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


    public DockerUpdateServiceImp(@Value("${docker.cert.path}") String certPath,
                            @Value("${docker.key.path}") String keyPath,
                            @Value("${docker.ca-cert.path}") String caCertPath) {
        logger.info("Cert path: {}", certPath);
        logger.info("Key path: {}", keyPath);
        logger.info("CA cert path: {}", caCertPath);

        File certFile = new File(certPath);
        File keyFile = new File(keyPath);
        File caCertFile = new File(caCertPath);

        logger.info("Cert file exists: {}", certFile.exists());
        logger.info("Key file exists: {}", keyFile.exists());
        logger.info("CA cert file exists: {}", caCertFile.exists());

        if (!certFile.exists() || !keyFile.exists() || !caCertFile.exists()) {
            throw new RuntimeException("One or more SSL certificate files not found");
        }

        // Create an SSL context with custom key and trust managers
        SslContextBuilder sslContextBuilder = SslContextBuilder.forClient()
                .keyManager(certFile, keyFile)
                .trustManager(caCertFile);

        HttpClient httpClient = HttpClient.create()
                .secure(sslSpec -> sslSpec.sslContext(sslContextBuilder));

        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    public Mono<String> updateContainerResourcesByName(String containerName, ContainerConfigDto requestBody){
        Deployment deployment = deploymentRepository.findByContainerName(containerName)
                .orElseThrow(() -> new NoSuchElementException("Deployment with container name '" + containerName + "' not found."));

        // Check if it's only a memory or CPU update
        if (isOnlyMemoryOrCpuUpdate(requestBody)) {
            return updateContainerResources(deployment.getDeploymentId(), requestBody);
        } else {
            return recreateContainer(containerName, requestBody);
        }

    }

    public Deployment inspectContainer(String containerName) {

        Deployment deployment = deploymentRepository.findByContainerName(containerName)
                .orElseThrow(() -> new NoSuchElementException("Deployment with container name '" + containerName + "' not found."));

        return  deployment;
//        return webClient.get()
//                .uri(dockerApiUrl + "/containers/{containerName}/json", containerName)  // Docker API Inspect Container endpoint
//                .retrieve()
//                .bodyToMono(String.class);
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






    private boolean isOnlyMemoryOrCpuUpdate(ContainerConfigDto request) {
        return ((request.getMemory() != null || request.getCpusetCpus() != null)) &&
                !changesAffectRecreation(request);
    }

    private boolean changesAffectRecreation(ContainerConfigDto request) {
        return request.getImageName() != null ||
                request.getImageTag() != null ||
                (request.getEnv() != null && !request.getEnv().isEmpty()) || // Check if env is not null and not empty
                (request.getCmd() != null && !request.getCmd().isEmpty()) || // Check if cmd is not null and not empty
                (request.getPortMappings() != null && !request.getPortMappings().isEmpty()); // Check if portMappings is not null and not empty
    }

    //     Update memory and cpusetCpus
    public Mono<String> updateContainerResources(String containerId,ContainerConfigDto requestBody) {

        UpdateContainerRequest request = new UpdateContainerRequest(requestBody.getMemory(), requestBody.getCpusetCpus());

        return webClient.post()
                .uri(dockerApiUrl + "/containers/" + containerId + "/update")
                .body(Mono.just(request), UpdateContainerRequest.class)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response -> {
                    Deployment deployment = deploymentRepository.findByDeploymentId(containerId)
                            .orElseThrow(() -> new NoSuchElementException("Deployment with deploymentId '" + containerId + "' not found."));
                    ContainerConfig containerConfig = deployment.getContainerConfig();
                    containerConfig.setMemory(requestBody.getMemory());
                    containerConfig.setCpu(requestBody.getCpusetCpus());
                    deploymentRepository.save(deployment);
                    return Mono.just(response); // Return the response from Docker API
                })
                .onErrorResume(e -> Mono.just("Error updating container: " + e.getMessage()));
    }

}
