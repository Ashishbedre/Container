package containers.example.containers.Service.Imp;


import containers.example.containers.Entity.ContainerConfig;
import containers.example.containers.Entity.Deployment;
import containers.example.containers.Entity.PortMapping;
import containers.example.containers.Repository.ContainerConfigRepository;
import containers.example.containers.Repository.DeploymentRepository;
import containers.example.containers.Service.DockerUpdateService;
import containers.example.containers.dto.ContainerConfigDto;
import containers.example.containers.dto.DeploymentDto;
import containers.example.containers.dto.PortMappingdto;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

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

    public DeploymentDto inspectContainer(String containerName) {

        Deployment deployment = deploymentRepository.findByContainerName(containerName)
                .orElseThrow(() -> new NoSuchElementException("Deployment with container name '" + containerName + "' not found."));
        DeploymentDto deploymentdto = getContainerDetailsById(deployment.getDeploymentId()).block();
        ContainerConfig config = deployment.getContainerConfig();
        config.setName(containerName);
        // Mapping PortMapping to PortMappingdto
        List<PortMappingdto> portMappingDtos = config.getPortMappings().stream()
                .map(portMapping -> {
                    PortMappingdto dto = new PortMappingdto();
                    dto.setProtocol(portMapping.getProtocol());
                    dto.setExposedPort(portMapping.getExposedPort());
                    dto.setHostPort(portMapping.getHostPort());
                    return dto;
                }).collect(Collectors.toList());
        deploymentdto.setPortMappings(portMappingDtos);

        deploymentdto.setEnv(config.getEnv());
        deploymentdto.setUsername(config.getUsername());
        deploymentdto.setEmail(config.getEmail());
        deploymentdto.setServerAddress(config.getServerAddress());
        return deploymentdto;

    }

    public Mono<DeploymentDto> getContainerDetailsById(String containerId) {
        return webClient.get()
                .uri(dockerApiUrl +"/containers/{id}/json", containerId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(this::convertToDeploymentDto);  // Mapping JSON response to DeploymentDto
    }

    private DeploymentDto convertToDeploymentDto(Object containerJson) {
        Map<String, Object> containerMap = (Map<String, Object>) containerJson;

        // Map containerMap fields to DeploymentDto fields
        DeploymentDto deploymentDto = new DeploymentDto();
        deploymentDto.setDeploymentId((String) containerMap.get("Id"));
//        deploymentDto.setContainerName((String) containerMap.get("Name"));



        Map<String, Object> config = (Map<String, Object>) containerMap.get("Config");

        // Extracting image name and tag from the full image string
        String fullImage = (String) config.get("Image");
        if (fullImage != null) {
            String[] imageParts = fullImage.split(":");
            deploymentDto.setImageName(imageParts[0]);
            if (imageParts.length > 1) {
                deploymentDto.setImageTag(imageParts[1]);
            } else {
                deploymentDto.setImageTag("");
            }
        }

        // Check for Entrypoint and set cmd accordingly
        List<String> entrypoint = (List<String>) config.get("Entrypoint");
        if (entrypoint == null ) {
            deploymentDto.setCmd(entrypoint);
        } else {
            deploymentDto.setCmd(new ArrayList<>());
        }

        // Set the State status
        Map<String, Object> state = (Map<String, Object>) containerMap.get("State");
        deploymentDto.setStatus((String) state.get("Status"));

        // Assuming CPU and Memory mapping (you may need to adjust based on actual Docker API response)
        Map<String, Object> hostConfig = (Map<String, Object>) containerMap.get("HostConfig");
        deploymentDto.setCpu(String.valueOf(hostConfig.get("CpusetCpus")));
        deploymentDto.setMemory(((Number) hostConfig.get("Memory")).longValue());

//        // Map Ports (including protocol)
//        Map<String, Object> networkSettings = (Map<String, Object>) containerMap.get("NetworkSettings");
//        Map<String, Object> ports = (Map<String, Object>) networkSettings.get("Ports");
//        deploymentDto.setPortMappings(mapPorts(ports));

        return deploymentDto;
    }

    // Helper method to map ports including protocol
    private List<PortMappingdto> mapPorts(Map<String, Object> ports) {
        return ports.entrySet().stream()
                .filter(entry -> entry.getValue() != null) // Filter out null entries
                .flatMap(entry -> {
                    // entry.getKey() is the port (e.g., "80/tcp")
                    String portWithProtocol = entry.getKey();
                    String[] parts = portWithProtocol.split("/"); // Split into port and protocol
                    String containerPort = parts[0];
                    String protocol = parts.length > 1 ? parts[1] : ""; // Get protocol, default to empty if not present

                    // Each entry can have multiple host ports
                    List<String> hostPorts = ((List<Map<String, Object>>) entry.getValue()).stream()
                            .map(port -> port.get("HostPort").toString()) // Extract host port
                            .collect(Collectors.toList());

                    // Create PortMappingDto for each host port
                    return hostPorts.stream().map(hostPort -> {
                        PortMappingdto portMappingDto = new PortMappingdto();
                        portMappingDto.setExposedPort(containerPort);
                        portMappingDto.setHostPort(hostPort);
                        portMappingDto.setProtocol(protocol); // Set the protocol
                        return portMappingDto;
                    });
                })
                .collect(Collectors.toList());
    }


//    public Deployment inspectContainer(String containerName) {
//
//        Deployment deployment = deploymentRepository.findByContainerName(containerName)
//                .orElseThrow(() -> new NoSuchElementException("Deployment with container name '" + containerName + "' not found."));
//
//        return  deployment;
//    }

//      Ashish : this will be done in future
//    public DeploymentDto inspectContainer(String containerName) {
//
//        Deployment deployment = deploymentRepository.findByContainerName(containerName)
//                .orElseThrow(() -> new NoSuchElementException("Deployment with container name '" + containerName + "' not found."));
//        DeploymentDto deploymentdto = new DeploymentDto();
////        deploymentdto.setId(deployment.getId());
//        deploymentdto.setDeploymentId(deployment.getDeploymentId());
//        deploymentdto.setContainerName(deployment.getContainerName());
//
//        ContainerConfig config = deployment.getContainerConfig();
//        deploymentdto.setImageName(config.getImageName());
//        deploymentdto.setImageTag(config.getImageTag());
////        deploymentdto.setName(config.getName());
//        deploymentdto.setCpu(config.getCpu());
//        deploymentdto.setMemory(config.getMemory());
////        deploymentdto.setStatus(config.isStatus());
//        deploymentdto.setEnv(config.getEnv());
//        deploymentdto.setCmd(config.getCmd());
//        // Convert List<PortMapping> to List<PortMappingdto>
//        List<PortMappingdto> portMappingDtos = config.getPortMappings().stream()
//                .map(this::convertToDto) // Call conversion method for each PortMapping
//                .toList(); // Collect results into a list
//        deploymentdto.setPortMappings(portMappingDtos);
//
//        deploymentdto.setPortMappings(portMappingDtos);
//        deploymentdto.setUsername(config.getUsername());
//        deploymentdto.setEmail(config.getEmail());
//        deploymentdto.setServerAddress(config.getServerAddress());
//
//
//        return  deploymentdto;
//    }

    // Method to convert PortMapping to PortMappingdto
    private PortMappingdto convertToDto(PortMapping portMapping) {
        PortMappingdto dto = new PortMappingdto();
        dto.setProtocol(portMapping.getProtocol());
        dto.setExposedPort(portMapping.getExposedPort());
        dto.setHostPort(portMapping.getHostPort());
        return dto;
    }


    private Mono<String>  recreateContainer(String containerName, ContainerConfigDto requestBody){
        Deployment deployment = deploymentRepository.findByContainerName(containerName)
                .orElseThrow(() -> new NoSuchElementException("Deployment with container name '" + containerName + "' not found."));
//        ContainerConfigDto recreateBody = updateDeploymentFromDto(deployment,requestBody);
        if(requestBody.getName()==null){
            requestBody.setName(containerName);
        }
        if(requestBody.getUsername()==null || requestBody.getUsername().isBlank() ||
                requestBody.getPassword()==null || requestBody.getPassword().isBlank()){
                requestBody.setUsername(deployment.getContainerConfig().getUsername());
                requestBody.setPassword(deployment.getContainerConfig().getPassword());
        }
        try {
            dockerServiceImp.deleteByContainerName(containerName);
            deploymentRepository.deleteById(deployment.getId());
            dockerServiceImp.createContainer(requestBody);

        }catch (Exception e){
            System.out.println(e.getMessage());
        }
        return Mono.just("Container recreated successfully");
    }


    private boolean isOnlyMemoryOrCpuUpdate(String containerName, ContainerConfigDto request) {
        return (((request.getMemory() != null || request.getCpusetCpus() != null)) ||
                ((request.getMemory() == null && request.getCpusetCpus() == null))) &&
                !changesAffectRecreation(containerName,request);
    }


    private boolean changesAffectRecreation(String containerName,ContainerConfigDto request) {
        Deployment deployment = deploymentRepository.findByContainerName(containerName)
                .orElseThrow(() -> new NoSuchElementException("Deployment with container name '" + containerName + "' not found."));

        // Compare each field, including handling null values in the request
        return (request.getImageName() != null && !request.getImageName().equals(deployment.getContainerConfig().getImageName()))
                || (request.getImageName() == null && deployment.getContainerConfig().getImageName() != null)  // Check if DB has value but request is null
                || (request.getImageTag() != null && !request.getImageTag().equals(deployment.getContainerConfig().getImageTag()))
                || (request.getImageTag() == null && deployment.getContainerConfig().getImageTag() != null)  // Same check for imageTag
                || (request.getEnv() != null && !request.getEnv().isEmpty() && !request.getEnv().equals(deployment.getContainerConfig().getEnv()))
                || (request.getEnv() != null && !(request.getEnv().isEmpty() && deployment.getContainerConfig().getEnv().isEmpty()))  // Check if DB has environment but request is null
                || (request.getCmd() != null && !request.getCmd().isEmpty() && !request.getCmd().equals(deployment.getContainerConfig().getCmd()))
                || (request.getCmd() != null && !(request.getCmd().isEmpty() && deployment.getContainerConfig().getCmd().isEmpty()))  // Check for command differences
                || (request.getPortMappings() != null && !request.getPortMappings().isEmpty() && !comparePortMappings(request.getPortMappings(), deployment.getContainerConfig().getPortMappings()))
                || (request.getPortMappings() != null && !(request.getPortMappings().isEmpty() && deployment.getContainerConfig().getPortMappings().isEmpty()))
                || (request.getUsername()!=null && !request.getUsername().isBlank() && request.getUsername().equals(deployment.getContainerConfig().getUsername()))
                ||  (request.getPassword()!=null && !request.getPassword().isBlank() && request.getPassword().equals(deployment.getContainerConfig().getPassword()))
                ||  (request.getEmail()!=null && !request.getEmail().isBlank() && request.getEmail().equals(deployment.getContainerConfig().getEmail()))
                ||  (request.getServerAddress()!=null && !request.getServerAddress().isBlank() && request.getServerAddress().equals(deployment.getContainerConfig().getServerAddress()));
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
