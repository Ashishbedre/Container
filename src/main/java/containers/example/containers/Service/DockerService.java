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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

    public Mono<String> createContainer(ContainerConfigdto config)  {
        Mono<String> containerId = createDockerContainer(config);
//        startDockerContainer(containerId);

//        Deployment deployment = new Deployment(config.getName() + "-deployment", config);
//        config.setDeployment(deployment);

//        containerConfigRepository.save(config);
//        return deploymentRepository.save(deployment);
        return containerId;
    }

    public void stopContainer(String containerName) {
        String containerId = findContainerIdByName(containerName);
        stopDockerContainer(containerId);
    }

    public Mono<String> createDockerContainer(ContainerConfigdto config){
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
                .bodyToMono(String.class);

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


