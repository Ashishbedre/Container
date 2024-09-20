package containers.example.containers.Service.Imp;

//import containers.example.containers.Entity.ContainerConfig;
//import containers.example.containers.Entity.Deployment;
import containers.example.containers.Entity.ContainerConfig;
import containers.example.containers.Entity.Deployment;
import containers.example.containers.Repository.ContainerConfigRepository;
import containers.example.containers.Repository.DeploymentRepository;
import containers.example.containers.Service.DockerService;
import containers.example.containers.dto.*;
import containers.example.containers.Entity.PortMapping;
import io.netty.handler.ssl.SslContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.io.File;
import java.util.*;

@Service
public class DockerServiceImp implements DockerService {

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

    private static final Logger logger = LoggerFactory.getLogger(DockerServiceImp.class);


    public DockerServiceImp(@Value("${docker.cert.path}") String certPath,
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

    public Mono<String> getDockerInfo( boolean flag) {
        return webClient.get()
                .uri(dockerApiUrl + "/containers/json?all="+flag)
                .header("Host", host+":"+port)  // Include port in the Host header
                .retrieve()
                .bodyToMono(String.class);
    }



    public DockerInfoResponse getAvaiableCpuAndMemory() {
        DockerInfoResponse dockerInfoResponse = webClient
                .get()
                .uri(dockerApiUrl + "/info")
                .header("Host", host+":"+port)
                .retrieve()
                .bodyToMono(DockerInfoResponse.class).block();

//         Perform some manipulation on the retrieved data
        if (dockerInfoResponse != null) {
            // Get total CPU and memory, defaulting to 0 if null
            Integer totalCpu = deploymentRepository.getTotalCpu() != null ? deploymentRepository.getTotalCpu() : 0;
            Long totalMemory = deploymentRepository.getTotalMemory() != null ? deploymentRepository.getTotalMemory() : 0L;

            dockerInfoResponse.setNcpu(dockerInfoResponse.getNcpu() - totalCpu);
            dockerInfoResponse.setMemTotal(dockerInfoResponse.getMemTotal()-totalMemory);
        }
        return dockerInfoResponse;
    }


    public DockerContainerResponse createContainer(ContainerConfigDto config)  {

        pullDockerImage(config.getImageName(),config.getImageTag());
        DockerContainerResponse container = createDockerContainer(config).block();
        if (container == null) {
            throw new RuntimeException("Docker container creation failed.");
        }

        ContainerConfig containerConfig = new ContainerConfig();
        containerConfig.setImageName(config.getImageName());
        containerConfig.setImageTag(config.getImageTag());
        containerConfig.setEnv(config.getEnv());
        containerConfig.setCmd(config.getCmd());
        containerConfig.setName(config.getName());
        containerConfig.setCpu(config.getCpusetCpus());
        containerConfig.setMemory(config.getMemory());
        containerConfig.setPortMappings(config.getPortMappings());

        Deployment deployment = new Deployment();
        deployment.setDeploymentId(container.getId());
        deployment.setContainerName(config.getName());
        deployment.setContainerConfig(containerConfig);
        deploymentRepository.save(deployment);
        startContainer(container.getId());

        return container;
    }


    public Deployment startContainerByApi(String containerName) {
        Deployment deployment = deploymentRepository.findByContainerName(containerName)
                .orElseThrow(() -> new NoSuchElementException("Deployment with container name '" + containerName + "' not found."));

        startContainer(deployment.getDeploymentId());
        return deployment ;
    }

    public void stopContainer(String containerName) {
        Deployment deployment = deploymentRepository.findByContainerName(containerName)
                .orElseThrow(() -> new NoSuchElementException("Deployment with container name '" + containerName + "' not found."));

        stopDockerContainer(deployment.getDeploymentId());
    }


    public boolean deleteByContainerName(String containerName) {
        // Find the deployment by containerName
        Optional<Deployment> deploymentOpt = deploymentRepository.findByContainerName(containerName);
        if (deploymentOpt.isPresent()) {
            Deployment deployment = deploymentOpt.get();


            // Construct the URL
            String containerId = deployment.getDeploymentId();

            try {
                // Make the DELETE request to the external API
                webClient.delete()
                        .uri(dockerApiUrl+"/containers/" + containerId + "?force=true")
                        .header("Host", host+":"+port)
                        .retrieve()
                        .bodyToMono(Void.class)
                        .block();

                // If successful, delete the deployment from the database
                deploymentRepository.delete(deployment);
                return true;

            } catch (WebClientResponseException e) {
                // Handle the case when the external API call fails
                System.err.println("Error deleting container: " + e.getResponseBodyAsString());
                return false;
            }
        }
        return false;
    }


    private void stopDockerContainer(String containerId) {
        // Retrieve Deployment by deploymentId
        Deployment deployment = deploymentRepository.findByDeploymentId(containerId)
                .orElseThrow(() -> new NoSuchElementException("Deployment with deploymentId '" + containerId + "' not found."));
        ContainerConfig containerConfig = deployment.getContainerConfig();
        containerConfig.setStatus(false);
        deploymentRepository.save(deployment);

        webClient.post()
                .uri(dockerApiUrl +  "/containers/{containerId}/stop", containerId)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }


    private Void startContainer(String containerId) {

        // Make the API call to start the container
        try {

            webClient.post()
                    .uri(dockerApiUrl + "/containers/"+containerId+"/start")
                    .contentLength(0)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            // Retrieve Deployment by deploymentId
            Deployment deployment = deploymentRepository.findByDeploymentId(containerId)
                    .orElseThrow(() -> new NoSuchElementException("Deployment with deploymentId '" + containerId + "' not found."));
            ContainerConfig containerConfig = deployment.getContainerConfig();
            containerConfig.setStatus(true);
            deploymentRepository.save(deployment);
            return null; // Returning null as Void is expected

        } catch (WebClientResponseException e) {
            // Handle errors from the external API
            System.err.println("Error starting container: " + e.getResponseBodyAsString());
            throw new RuntimeException("Failed to start container: " + e.getMessage(), e);
        } catch (Exception e) {
            // Handle other possible exceptions
            System.err.println("Unexpected error: " + e.getMessage());
            throw new RuntimeException("Unexpected error occurred", e);
        }
    }


    private Mono<DockerContainerResponse> createDockerContainer(ContainerConfigDto config) {
        DockerCreateRequest request = new DockerCreateRequest();
        request.setImage(config.getImageName() + ":" + config.getImageTag());
        request.setEnv(config.getEnv());
        request.setCmd(config.getCmd());

        DockerCreateRequest.HostConfig hostConfig = new DockerCreateRequest.HostConfig();
        Map<String, List<DockerCreateRequest.PortBinding>> portBindings = new HashMap<>();

        for (PortMapping portMapping : config.getPortMappings()) {
            DockerCreateRequest.PortBinding portBinding = new DockerCreateRequest.PortBinding();
            portBinding.setHostPort(portMapping.getHostPort());
            String key = portMapping.getExposedPort() + "/" + portMapping.getProtocol();
            portBindings.computeIfAbsent(key, k -> new ArrayList<>()).add(portBinding);
        }

        hostConfig.setPortBindings(portBindings);
        hostConfig.setMemory(config.getMemory());  // 2 GB dedicated memory
        hostConfig.setCpusetCpus(config.getCpusetCpus());  // Dedicated CPU cores 0 and 1
        request.setHostConfig(hostConfig);
//        request.setName(config.getName());

        Map<String, Object> exposedPorts = new HashMap<>();
        for (PortMapping portMapping : config.getPortMappings()) {
            String key = portMapping.getExposedPort() + "/" + portMapping.getProtocol();
            exposedPorts.put(key, new HashMap<>());
        }
        request.setExposedPorts(exposedPorts);



        return webClient.post()
                .uri(dockerApiUrl + "/containers/create?name="+config.getName())
                .bodyValue(request)
                .header("Host", host+":"+port)  // Include port in the Host header
                .header("Content-Type", "application/json")
//                .header("Content-Length", String.valueOf(contentLength))  // Set Content-Length
                .retrieve()
                .bodyToMono(DockerContainerResponse.class)
                .doOnSubscribe(subscription -> logger.info("Creating Docker container with config: {}", config))
                .doOnNext(response -> logger.info("Docker API response received: {}", response))
                .onErrorResume(WebClientResponseException.class, e -> {
                    // Handle specific WebClient response errors
                    logger.error("WebClientResponseException: Status code: {}, Response body: {}", e.getStatusCode(), e.getResponseBodyAsString());
                    return Mono.error(new RuntimeException("Failed to create Docker container: " + e.getMessage(), e));
                })
                .onErrorResume(Exception.class, e -> {
                    // Handle other exceptions
                    logger.error("Unexpected error in createDockerContainer", e);
                    return Mono.error(new RuntimeException("Unexpected error occurred: " + e.getMessage(), e));
                });

    }

    private void pullDockerImage(String image, String tag) {
        webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme(scheme)
                        .host(host)
                        .port(port)
                        .path("/images/create")
                        .queryParam("fromImage", image)
                        .queryParam("tag", tag)
                        .build())
                .header("Host", host+":"+port)  // Include port in the Host header
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToFlux(String.class)  // Handle a stream of responses
                .doOnNext(response -> {
                    System.out.println("Docker image pull response: " + response);
                })
                .blockLast();  // Wait for the last response to ensure the entire process completes

    }


}


