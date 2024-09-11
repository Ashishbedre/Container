package containers.example.containers.Service;

//import containers.example.containers.Entity.ContainerConfig;
//import containers.example.containers.Entity.Deployment;
import containers.example.containers.Entity.ContainerConfig;
import containers.example.containers.Entity.Deployment;
import containers.example.containers.Repository.ContainerConfigRepository;
import containers.example.containers.Repository.DeploymentRepository;
import containers.example.containers.dto.ContainerConfigDto;
import containers.example.containers.dto.DockerContainerResponse;
import containers.example.containers.dto.DockerCreateRequest;
import containers.example.containers.Entity.PortMapping;
import io.netty.handler.ssl.SslContextBuilder;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
        import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import reactor.netty.http.client.HttpClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.io.File;
import java.io.IOException;

        import java.util.*;

@Service
public class DockerService {

    private final WebClient webClient;

    @Value("${cert.path}")
    private Resource certPath; // Path to client-cert.pem

    @Value("${key.path}")
    private Resource keyPath; // Path to client-key.pem

    @Autowired
    private ContainerConfigRepository containerConfigRepository;

    @Autowired
    private DeploymentRepository deploymentRepository;

    private static final Logger logger = LoggerFactory.getLogger(DockerService.class);


    public DockerService() {
        // Load certificates from resources folder
        Resource certResource = new ClassPathResource("client-cert.pem");
        Resource keyResource = new ClassPathResource("client-key.pem");
        Resource caCertResource = new ClassPathResource("ca.pem");

        File certFile;
        File keyFile;
        File caCertFile;
        try {
            certFile = certResource.getFile();
            keyFile = keyResource.getFile();
            caCertFile = caCertResource.getFile();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load SSL certificate files from resources", e);
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
                .uri("https://172.16.0.3:2376/containers/json?all="+flag)
                .header("Host", "172.16.0.3:2376")  // Include port in the Host header
                .retrieve()
                .bodyToMono(String.class);
    }



    public Mono<DockerContainerResponse> createContainer(ContainerConfigDto config)  {
        pullDockerImage(config.getImageName(),config.getImageTag());

        ContainerConfig containerConfig = new ContainerConfig();
        containerConfig.setImageName(config.getImageName());
        containerConfig.setImageTag(config.getImageTag());
        containerConfig.setEnv(config.getEnv());
        containerConfig.setCmd(config.getCmd());
        containerConfig.setName(config.getName());
        containerConfig.setPortMappings(config.getPortMappings());

        Deployment deployment = new Deployment();
        deployment.setDeploymentId("deploymentId");
        deployment.setContainerConfig(containerConfig);
        deploymentRepository.save(deployment);

        return createDockerContainer(config)
                .doOnNext(response -> logger.info("Container created and saved with ID: {}", response.getId()))
                .doOnError(error -> logger.error("Error in createAndSaveContainer", error));
//        System.out.println(containerId.block());
//        DockerContainerResponse containerId1 = createDockerContainer(config).block();
//        Void startContainer = startContainer(containerId);

//        Deployment deployment = new Deployment(config.getName() + "-deployment", config);
//        config.setDeployment(deployment);

//        containerConfigRepository.save(config);
//        return deploymentRepository.save(deployment);
//        DockerContainerResponse container = new DockerContainerResponse();
//        return Mono.just(container);
//        return container;
    }

//    private Mono<DockerContainerResponse> saveContainerDetails(ContainerConfigDto config, DockerContainerResponse response) {
//
//        ContainerConfig containerConfig = new ContainerConfig();
//        containerConfig.setImageName(config.getImageName());
//        containerConfig.setImageTag(config.getImageTag());
//        containerConfig.setEnv(config.getEnv().toArray(new String[config.getEnv().size()]));
//        containerConfig.setCmd(config.getCmd().toArray(new String[config.getCmd().size()]));
//        containerConfig.setName(config.getName());
//        containerConfig.setPortMappings(config.getPortMappings());
//
//        Deployment deployment = new Deployment();
//        deployment.setDeploymentName(response.getId());
//        deployment.setContainerConfig(containerConfig);
//
//        return Mono.fromCallable(() -> deploymentRepository.save(deployment))
//                .thenReturn(response)
//                .doOnSuccess(savedEntity -> logger.info("Container details saved to database"))
//                .doOnError(error -> logger.error("Error saving container details to database", error));
//    }


//    public Mono<String> startContainer(String containerId) {
//        return webClient.post()
//                .uri("https://172.16.0.3:2376/v1.46/containers/{containerId}/start", containerId)
//                .contentLength(0)  // Ensure no body is sent
//                .exchangeToMono(response -> {
//                    if (response.statusCode() == HttpStatus.NO_CONTENT) {
//                        return Mono.just("Container " + containerId + " started successfully");
//                    } else {
//                        return response.bodyToMono(String.class)
//                                .map(body -> "Failed to start container: " + body);
//                    }
//                })
//                .onErrorResume(e -> Mono.just("Error starting container: " + e.getMessage()));
//    }

    public Void startContainer(String containerId) {
        System.out.println(containerId);

        return webClient.post()
                .uri("https://172.16.0.3:2376/v1.46/containers/"+containerId+"/start")
//                .header("Host", "172.16.0.3:2376")  // Correct usage of Host header
                .contentLength(0)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }


    public void stopContainer(String containerName) {
        String containerId = findContainerIdByName(containerName);
        stopDockerContainer(containerId);
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
        request.setHostConfig(hostConfig);
        request.setName(config.getName());

        Map<String, Object> exposedPorts = new HashMap<>();
        for (PortMapping portMapping : config.getPortMappings()) {
            String key = portMapping.getExposedPort() + "/" + portMapping.getProtocol();
            exposedPorts.put(key, new HashMap<>());
        }
        request.setExposedPorts(exposedPorts);



        return webClient.post()
                .uri("https://172.16.0.3:2376/v1.46/containers/create")
                .bodyValue(request)
                .header("Host", "172.16.0.3:2376")  // Include port in the Host header
                .header("Content-Type", "application/json")
//                .header("Content-Length", String.valueOf(contentLength))  // Set Content-Length
                .retrieve()
                .bodyToMono(DockerContainerResponse.class)
                .doOnSubscribe(subscription -> logger.info("Creating Docker container with config: {}", config))
                .doOnNext(response -> logger.info("Docker API response received: {}", response))
                .doOnError(error -> logger.error("Error in createDockerContainer", error));

    }

    public void pullDockerImage(String image, String tag) {
        webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("172.16.0.3")
                        .port(2376)
                        .path("/images/create")
                        .queryParam("fromImage", image)
                        .queryParam("tag", tag)
                        .build())
                .header("Host", "172.16.0.3:2376")  // Include port in the Host header
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToFlux(String.class)  // Handle a stream of responses
                .doOnNext(response -> {
                    System.out.println("Docker image pull response: " + response);
                })
                .blockLast();  // Wait for the last response to ensure the entire process completes
    }




    private void stopDockerContainer(String containerId) {
        webClient.post()
                .uri("https://172.16.0.3:2376" + "/containers/{containerId}/stop", containerId)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    private String findContainerIdByName(String containerName) {

        Map containerList =  webClient.get()
                .uri("https://172.16.0.3:2376"  + "/containers/json")
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        for (Map<String, Object> container : (Iterable<Map<String, Object>>) containerList.get("Containers")) {
            if (container.get("Names").equals("/" + containerName)) {
                return (String) container.get("Id");
            }
        }

        throw new RuntimeException("Container not found: " + containerName);
    }





}


