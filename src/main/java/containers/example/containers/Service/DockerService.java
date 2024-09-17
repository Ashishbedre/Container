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

//    @Value("${cert.path}")
//    private Resource certPath; // Path to client-cert.pem
//
//    @Value("${key.path}")
//    private Resource keyPath; // Path to client-key.pem

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

    private static final Logger logger = LoggerFactory.getLogger(DockerService.class);


    public DockerService(@Value("${docker.cert.path}") String certPath,
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
                .header("Host", "172.16.0.3:2376")  // Include port in the Host header
                .retrieve()
                .bodyToMono(String.class);
    }



    public Mono<DockerContainerResponse> createContainer(ContainerConfigDto config)  {
        pullDockerImage(config.getImageName(),config.getImageTag());



        DockerContainerResponse container = createDockerContainer(config).block();

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
        startContainer(container.getId());
        deploymentRepository.save(deployment);

        return Mono.just(container);
    }



    public Void startContainer(String containerId) {
        System.out.println(containerId);

        return webClient.post()
                .uri(dockerApiUrl + "/containers/"+containerId+"/start")
//                .header("Host", "172.16.0.3:2376")  // Correct usage of Host header
                .contentLength(0)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
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
                        .scheme(scheme)
                        .host(host)
                        .port(port)
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




//    private void stopDockerContainer(String containerId) {
//        webClient.post()
//                .uri(dockerApiUrl +  "/containers/{containerId}/stop", containerId)
//                .retrieve()
//                .bodyToMono(Void.class)
//                .block();
//    }

    //    public void stopContainer(String containerName) {
//        String containerId = findContainerIdByName(containerName);
//        stopDockerContainer(containerId);
//    }

//    private String findContainerIdByName(String containerName) {
//
//        Map containerList =  webClient.get()
//                .uri(dockerApiUrl + "/containers/json")
//                .retrieve()
//                .bodyToMono(Map.class)
//                .block();
//        for (Map<String, Object> container : (Iterable<Map<String, Object>>) containerList.get("Containers")) {
//            if (container.get("Names").equals("/" + containerName)) {
//                return (String) container.get("Id");
//            }
//        }
//
//        throw new RuntimeException("Container not found: " + containerName);
//    }





}


