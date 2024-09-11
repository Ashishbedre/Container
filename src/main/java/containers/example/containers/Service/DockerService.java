package containers.example.containers.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
//import containers.example.containers.Entity.ContainerConfig;
//import containers.example.containers.Entity.Deployment;
import com.fasterxml.jackson.databind.ObjectMapper;
import containers.example.containers.Entity.ContainerConfig;
import containers.example.containers.Entity.Deployment;
import containers.example.containers.Repository.ContainerConfigRepository;
import containers.example.containers.Repository.DeploymentRepository;
import containers.example.containers.dto.ContainerConfigdto;
import containers.example.containers.dto.DockerContainerResponse;
import containers.example.containers.dto.DockerCreateRequest;
import io.netty.handler.ssl.SslContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import reactor.netty.http.client.HttpClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.io.File;
import java.io.IOException;

import java.nio.charset.StandardCharsets;
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

    public Mono<DockerContainerResponse> createContainer(ContainerConfigdto config)  {

        pullDockerImage(config.getImageName(),config.getImageTag());
        Mono<DockerContainerResponse> containerId = createDockerContainer(config);
//        DockerContainerResponse containerId1 = createDockerContainer(config).block();
//        Mono<String> startContainer = startContainer("containerId1.getId()");

//        Deployment deployment = new Deployment(config.getName() + "-deployment", config);
//        config.setDeployment(deployment);

//        containerConfigRepository.save(config);
//        return deploymentRepository.save(deployment);
//        DockerContainerResponse containerId = new DockerContainerResponse();
//        return Mono.just(containerId);
        return containerId;
    }


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

    public Mono<Void> startContainer(String containerId) {

        return webClient.post()
                .uri("https://172.16.0.3:2376/v1.46/containers/"+containerId+"/start")
//                .header("Host", "172.16.0.3:2376")  // Correct usage of Host header
                .contentLength(0)
                .retrieve()
                .bodyToMono(Void.class);
    }


    public void stopContainer(String containerName) {
        String containerId = findContainerIdByName(containerName);
        stopDockerContainer(containerId);
    }

    public Mono<DockerContainerResponse> createDockerContainer(ContainerConfigdto config){
        DockerCreateRequest request = new DockerCreateRequest();
        request.setImage(config.getImageName()+ ":"+ config.getImageTag());
        request.setEnv(config.getEnv());
        request.setCmd(config.getCmd());

// Set HostConfig with port bindings
        DockerCreateRequest.HostConfig hostConfig = new DockerCreateRequest.HostConfig();
        Map<String, List<DockerCreateRequest.PortBinding>> portBindings = new HashMap<>();
        DockerCreateRequest.PortBinding portBinding = new DockerCreateRequest.PortBinding();
        portBinding.setHostPort(config.getHostPort());
        portBindings.put(config.getExposedPort()+"/tcp", Collections.singletonList(portBinding));  // Map 8080 on the host to 8080 on the container
        hostConfig.setPortBindings(portBindings);

        request.setHostConfig(hostConfig);
        request.setName(config.getName());


        return webClient.post()
                .uri("https://172.16.0.3:2376/v1.46/containers/create")
                .bodyValue(request)
                .header("Host", "172.16.0.3:2376")  // Include port in the Host header
                .header("Content-Type", "application/json")
//                .header("Content-Length", String.valueOf(contentLength))  // Set Content-Length
                .retrieve()
                .bodyToMono(DockerContainerResponse.class);
//                .doOnNext(body -> {
//            // Print or log the raw response body
//            System.out.println("Raw response body: " + body);
//        })
//                .flatMap(body -> {
//                    // Parse the raw response body to DockerContainerResponse
//                    try {
//                        DockerContainerResponse response = new ObjectMapper().readValue(body, DockerContainerResponse.class);
//                        return Mono.just(response);
//                    } catch (IOException e) {
//                        return Mono.error(e);
//                    }
//                });

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


