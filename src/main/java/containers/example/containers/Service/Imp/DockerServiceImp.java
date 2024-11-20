package containers.example.containers.Service.Imp;


import containers.example.containers.Entity.ContainerConfig;
import containers.example.containers.Entity.Deployment;
import containers.example.containers.Helper.Helper;
import containers.example.containers.Repository.ContainerConfigRepository;
import containers.example.containers.Repository.DeploymentRepository;
import containers.example.containers.Service.DockerService;
import containers.example.containers.dto.*;
import containers.example.containers.Entity.PortMapping;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

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

    @Autowired
    Helper helper;



    @Autowired
    public DockerServiceImp(WebClient webClient) {
        this.webClient = webClient;
    }

    private static final Logger logger = LoggerFactory.getLogger(DockerServiceImp.class);




    public Flux<Map<String, Object>> getDockerInfo(boolean flag) {

        // Fetch all deployments from the database and store them in a map using deploymentId
        List<Deployment> deployments = deploymentRepository.findAll();
        Map<String, Deployment> deploymentMap = new HashMap<>();
        for (Deployment deployment : deployments) {
            deploymentMap.put(deployment.getDeploymentId(), deployment); // Using deploymentId as the key
        }

        // Create a set to track the container deploymentIds returned by Docker
        Set<String> processedDeploymentIds = new HashSet<>();

        return webClient.get()
                .uri(dockerApiUrl + "/containers/json?all=" + flag)
                .header("Host", host + ":" + port)  // Include port in the Host header
                .retrieve() // Fetch the response
                .bodyToFlux(Map.class)
                .map(container -> {

                    String deploymentId = (String) container.get("Id"); // Assuming Docker container ID matches deploymentId

                    Deployment deployment = deploymentMap.get(deploymentId);

                    Map<String, Object> response = new HashMap<>(container);

                    if (deployment != null && deployment.getContainerConfig() != null) {
                        ContainerConfig config = deployment.getContainerConfig();

                        // Check both CPU and Memory values carefully
                        if ((config.getCpu() == null || config.getCpu().isBlank() || Integer.parseInt(config.getCpu()) == 0) &&
                                (config.getMemory() == null || config.getMemory().equals(0) || config.getMemory() == 0)) {
                            // Fallback values if no valid CPU or memory are found
                            response.put("cpu", null);
                            response.put("memory", null);
                        } else {
                            // Use the CPU and memory from the database if present
                            response.put("cpu", config.getCpu());
                            response.put("memory", config.getMemory());
                        }
                        response.put("log",config.getLog());
                        response.put("ui", config.isState());
                    } else {
                        // Fallback values if no deployment is found
                        response.put("cpu", null);
                        response.put("memory", null);
                        response.put("ui", 0);
                        response.put("log",null);
                    }

                    // Mark the deploymentId as processed
                    if (deploymentId != null) {
                        processedDeploymentIds.add(deploymentId);
                    }

                    return response;
                })
                .concatWith(Flux.defer(() -> {
                    // After processing all Docker containers, add the leftover deployments from the database
                    List<Map<String, Object>> leftoverResponses = new ArrayList<>();

                    for (Map.Entry<String, Deployment> entry : deploymentMap.entrySet()) {
                        String deploymentId = entry.getKey();
                        Deployment deployment = entry.getValue();

                        // Only include deployments with a "pending" status and that haven't been processed yet
                        if (!processedDeploymentIds.contains(deploymentId) &&
                                (deployment.getContainerConfig() != null &&
                                        "pending".equals(deployment.getContainerConfig().getStatus())) && flag==true) {


                            Map<String, Object> leftoverResponse = new HashMap<>();
                            ContainerConfig config = deployment.getContainerConfig();

                            leftoverResponse.put("Names", Collections.singletonList("/" + deployment.getContainerName())); // Add back the leading slash for consistency

                            // Include all relevant fields from the database
                            leftoverResponse.put("cpu", config.getCpu() != null && !config.getCpu().isBlank() ? config.getCpu() : null);
                            leftoverResponse.put("memory", config.getMemory() != null && config.getMemory() != 0 ? config.getMemory() : null);
                            leftoverResponse.put("ui", config.isState());
                            leftoverResponse.put("status", config.getStatus() != null && !config.getStatus().isBlank() ? config.getStatus() : "pending");
                            leftoverResponse.put("Id", deployment.getDeploymentId());
                            leftoverResponse.put("imageName", config.getImageName());
                            leftoverResponse.put("imageTag", config.getImageTag());
                            leftoverResponse.put("env", config.getEnv());
                            leftoverResponse.put("cmd", config.getCmd());
                            leftoverResponse.put("portMappings", config.getPortMappings());
                            leftoverResponse.put("username", config.getUsername());
                            leftoverResponse.put("email", config.getEmail());
                            leftoverResponse.put("serverAddress", config.getServerAddress());
                            leftoverResponse.put("log",config.getLog());

                            // Add the leftover response to the list
                            leftoverResponses.add(leftoverResponse);
                        }
                    }

                    return Flux.fromIterable(leftoverResponses); // Return the leftover responses as a Flux
                }));
    }




    public ResourceUsageDto getAvaiableCpuAndMemory() {
        DockerInfoResponse dockerInfoResponse = webClient
                .get()
                .uri(dockerApiUrl + "/info")
                .header("Host", host+":"+port)
                .retrieve()
                .bodyToMono(DockerInfoResponse.class).block();

        ResourceUsageDto resourceUsageDto = new ResourceUsageDto();
//         Perform some manipulation on the retrieved data
        if (dockerInfoResponse != null) {
            // Get total CPU and memory, defaulting to 0 if null
            Integer totalCpu = deploymentRepository.getTotalCpu() != null ? deploymentRepository.getTotalCpu() : 0;
            Long totalMemory = deploymentRepository.getTotalMemory() != null ? deploymentRepository.getTotalMemory() : 0L;

            resourceUsageDto.setAvailableCpu(dockerInfoResponse.getNcpu());
            resourceUsageDto.setLeftCpu(dockerInfoResponse.getNcpu() - totalCpu);
            resourceUsageDto.setAvailableMemory(dockerInfoResponse.getMemTotal());
            resourceUsageDto.setLeftMemory(dockerInfoResponse.getMemTotal()-totalMemory);

        }
        return resourceUsageDto;
    }


    public DockerContainerResponse createContainer(ContainerConfigDto config)  {
        try {
//            pullDockerImage(config.getImageName(),config.getImageTag(),config.getUsername(),config.getPassword(),config.getEmail(),config.getServerAddress());
            DockerContainerResponse container;
            Deployment savedDeployment;

            //Ashish add  Save or update deployment based on status and deployment ID
            if ((config.getDeploymentId() == null || config.getDeploymentId().isBlank()) &&
                    (config.getStatus() == null || config.getStatus().isBlank())) {

                savedDeployment = saveDeployment(config);
            } else if (config.getDeploymentId() != null && !config.getDeploymentId().isBlank() &&
                    "pending".equals(config.getStatus())) {

                savedDeployment = UpdateSaveDeployment(config);
            } else {
                throw new RuntimeException("Invalid deployment status or ID.");
            }

            try{
                pullDockerImage(config.getImageName(),config.getImageTag(),config.getUsername(),config.getPassword(),config.getEmail(),config.getServerAddress());
            }catch (Exception e){
                log(savedDeployment.getDeploymentId(), "Docker image pull failed: " + e.getMessage());
                throw new RuntimeException("Docker image pull failed");
            }

            try {
                container = createDockerContainer(config).block();
                if (container == null) {
                    throw new RuntimeException("Docker container creation returned null.");
                }
            }catch (Exception e){
                System.out.println(savedDeployment.getDeploymentId());
                log(savedDeployment.getDeploymentId(),  e.getMessage());
                throw new RuntimeException("Docker container creation failed.");
            }

            // Ashish add Fetch the existing deployment that was saved initially
            Deployment deployment = deploymentRepository.findByDeploymentId(savedDeployment.getDeploymentId())
                    .orElseThrow(() -> new RuntimeException("Failed to retrieve saved deployment."));

            // Update container config with new container details
            ContainerConfig containerConfig = deployment.getContainerConfig();
            updateContainerConfig(containerConfig, config);

    //        Ashish comment
    //        Deployment deployment = new Deployment();
            deployment.setDeploymentId(container.getId());
            deployment.setContainerName(config.getName());
            deployment.setContainerConfig(containerConfig);
            // Consider wrapping the save in a transaction
            try {
                deploymentRepository.save(deployment);
            } catch (Exception e) {
                log(savedDeployment.getDeploymentId(), "Failed to save deployment: " + e.getMessage());
                throw new RuntimeException("Failed to save deployment: " + e.getMessage());
            }
            startContainer(container.getId());
            log(savedDeployment.getDeploymentId(),"Docker container created and started successfully.");

            return container;
        } catch (DockerOperationException e) {
            throw e;
        } catch (Exception e) {
            throw new DockerOperationException("An unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }


    public boolean deleteByContainerId(String containerId) {

        Optional<Deployment> deploymentOpt = deploymentRepository.findByDeploymentId(containerId);

        if (deploymentOpt.isEmpty()) {
            return false;
        }

        Deployment deployment = deploymentOpt.get();

            // Check if the container status is not "pending"
            if (!"pending".equals(deployment.getContainerConfig().getStatus())) {
                // Construct the URL
//            String containerId = deployment.getDeploymentId();

                try {
                    // Make the DELETE request to the external API
                    webClient.delete()
                            .uri(dockerApiUrl + "/containers/" + containerId + "?force=true")
                            .header("Host", host + ":" + port)
                            .retrieve()
                            .bodyToMono(Void.class)
                            .block();

                    // If successful, delete the deployment from the database
                    deploymentRepository.delete(deployment);
//                    log(containerId, "Container successfully deleted.");
                    return true;

                } catch (WebClientResponseException e) {
                    // Log the error to the container's configuration
                    log(containerId, "Error deleting container: " + e.getResponseBodyAsString());
                    System.err.println("Error deleting container: " + e.getResponseBodyAsString());
                    return false;
                }catch (Exception e) {
                    // Log unexpected errors
                    log(containerId, "Unexpected error deleting container: " + e.getMessage());
                    System.err.println("Unexpected error deleting container: " + e.getMessage());
                    return false;
                }
            }else {
                // If the container is in "pending" status, directly delete from the database
                deploymentRepository.delete(deployment);
                return true;
            }

    }


    public void stopDockerContainer(String containerId) {
        // Retrieve Deployment by deploymentId
        Deployment deployment = deploymentRepository.findByDeploymentId(containerId)
                .orElseThrow(() -> {
                    String errorMessage = "Deployment with deploymentId '" + containerId + "' not found.";
                    return new NoSuchElementException(errorMessage);
                });

        ContainerConfig containerConfig = deployment.getContainerConfig();

        // Proceed only if the container is not in "pending" status
        if (!"pending".equals(containerConfig.getStatus())) {
            try {
                // Stop the Docker container via WebClient
                webClient.post()
                        .uri(dockerApiUrl + "/containers/{containerId}/stop", containerId)
                        .retrieve()
                        .bodyToMono(Void.class)
                        .block();

                containerConfig.setStatus("exited");
                deploymentRepository.save(deployment);
                // Log the successful stop action
                log(containerId, "Container stopped successfully.");

            } catch (WebClientResponseException e) {
                String errorMessage = "Error stopping container: " + e.getResponseBodyAsString();
                log(containerId, errorMessage);
                System.err.println(errorMessage);
                throw new RuntimeException("Failed to stop container: " + e.getMessage(), e);

            } catch (Exception e) {
                String errorMessage = "Unexpected error: " + e.getMessage();
                log(containerId, errorMessage);
                System.err.println(errorMessage);
                throw new RuntimeException("Unexpected error occurred while stopping the container", e);
            }
        } else {
            // Log if the container is in "pending" status and cannot be stopped
            String message = "Container is in 'pending' status and cannot be stopped.";
            log(containerId, message);
            System.out.println(message);
        }
    }



    public Deployment startContainer(String containerId) {

//        try {
            Deployment deployment = deploymentRepository.findByDeploymentId(containerId)
                    .orElseThrow(() -> {
                        String errorMessage = "Deployment with deploymentId '" + containerId + "' not found.";
                        return new NoSuchElementException(errorMessage);
                    });

            ContainerConfig containerConfig = deployment.getContainerConfig();

            // Proceed only if the container is not in "pending" status
            if (!"pending".equals(containerConfig.getStatus())) {
                try {
                    // Start the Docker container via WebClient
                    webClient.post()
                            .uri(dockerApiUrl + "/containers/" + containerId + "/start")
                            .contentLength(0)
                            .retrieve()
                            .bodyToMono(Void.class)
                            .block();

                    containerConfig.setStatus("running");
                    deploymentRepository.save(deployment);
                    log(containerId, "Container started successfully.");
                } catch (WebClientResponseException e) {
                    String errorMessage = "Error starting container: " + (e.getResponseBodyAsString() != null ? e.getResponseBodyAsString() : "No response body");
                    log(containerId, errorMessage);
                    System.err.println(errorMessage);
                    throw new RuntimeException("Failed to start container: " + e.getMessage(), e);

                } catch (Exception e) {
                    String errorMessage = "Unexpected error: " + e.getMessage();
                    log(containerId, errorMessage);
                    System.err.println(errorMessage);
                    throw new RuntimeException("Unexpected error occurred while starting the container", e);
                }
            } else {
                System.out.println("Container is in 'pending' status and cannot be started.");
            }
            return deployment;

//        } catch (Exception e) {
//            String errorMessage = "Unexpected error: " + e.getMessage();
//            log(containerId, errorMessage);
//            System.err.println(errorMessage);
//            throw new RuntimeException("Unexpected error occurred while starting the container", e);
//        }
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
                    String errorMessage = String.format("Failed to create Docker container: %d %s ",
                            e.getRawStatusCode(), e.getStatusText());
                    throw new DockerOperationException(errorMessage, e.getRawStatusCode());
                })
                .onErrorResume(Exception.class, e -> {
                    String errorMessage = "Unexpected error occurred: " + e.getMessage();
                    throw new DockerOperationException(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR.value());
                });

    }


    public void pullDockerImage(String image, String tag, String username,String password,String email,String serverAddress) {
        String authHeader = null;
        if (username != null && password != null && !username.isBlank() && !password.isBlank()) {
            authHeader = helper.createAuthHeader(username, password, email ,serverAddress);
        }

        WebClient.RequestHeadersSpec<?> requestSpec = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme(scheme)
                        .host(host)
                        .port(port)
                        .path("/images/create")
                        .queryParam("fromImage", image)
                        .queryParam("tag", tag)
                        .build())
                .header("Host", host+":"+port)  // Include port in the Host header
                .header("Content-Type", "application/json");

        // Conditionally add the authentication header only if authHeader is not null
        if (authHeader != null) {
            requestSpec = requestSpec.header("X-Registry-Auth", authHeader);
        }

        // Send the request and handle the response
        requestSpec.retrieve()
                .bodyToFlux(String.class)
                .doOnNext(response -> {
                    System.out.println("Docker image pull response: " + response);
                })
                .doOnError(error -> {
                    String errorMessage = "Error pulling Docker image: " + error.getMessage();
                    System.err.println(errorMessage);

                    // Rethrow the exception to indicate failure
                    throw new RuntimeException(errorMessage, error);
                })
                .blockLast();
    }

    public Deployment UpdateSaveDeployment(ContainerConfigDto containerConfigDto) {
        // Check if deployment exists by deploymentId
        Optional<Deployment> existingDeploymentOpt = deploymentRepository.findByDeploymentId(containerConfigDto.getDeploymentId());

        if (existingDeploymentOpt.isPresent()) {
            // If deployment exists, update it
            Deployment deployment = existingDeploymentOpt.get();
            ContainerConfig containerConfig = deployment.getContainerConfig();

            // Update only non-null fields from DTO
            if (containerConfigDto.getImageName() != null) {
                containerConfig.setImageName(containerConfigDto.getImageName());
            }
            if (containerConfigDto.getImageTag() != null) {
                containerConfig.setImageTag(containerConfigDto.getImageTag());
            }
            if (containerConfigDto.getName() != null) {
                containerConfig.setName(containerConfigDto.getName());
                deployment.setContainerName(containerConfigDto.getName()); // Also update deployment's container name
            }
            if (containerConfigDto.getMemory() != null) {
                containerConfig.setMemory(containerConfigDto.getMemory());
            }
            if (containerConfigDto.getCpusetCpus() != null) {
                containerConfig.setCpu(containerConfigDto.getCpusetCpus());
            }
            if (containerConfigDto.getEnv() != null) {
                containerConfig.setEnv(containerConfigDto.getEnv());
            }
            if (containerConfigDto.getCmd() != null) {
                containerConfig.setCmd(containerConfigDto.getCmd());
            }
            if (containerConfigDto.getUsername() != null) {
                containerConfig.setUsername(containerConfigDto.getUsername());
            }
            if (containerConfigDto.getPassword() != null) {
                containerConfig.setPassword(containerConfigDto.getPassword());
            }
            if (containerConfigDto.getEmail() != null) {
                containerConfig.setEmail(containerConfigDto.getEmail());
            }
            if (containerConfigDto.getServerAddress() != null) {
                containerConfig.setServerAddress(containerConfigDto.getServerAddress());
            }
            if (containerConfigDto.getStatus() != null) {
                containerConfig.setStatus(containerConfigDto.getStatus());
            } else {
                containerConfig.setStatus("pending"); // Default to "pending" if status is not provided
            }

            // Handle port mappings if provided
            if (containerConfigDto.getPortMappings() != null) {
                List<PortMapping> currentPortMappings = containerConfig.getPortMappings();

                // Iterate through the DTO and update or add new PortMappings
                for (PortMapping portMappingDto : containerConfigDto.getPortMappings()) {
                    boolean found = false;
                    for (PortMapping existingPortMapping : currentPortMappings) {
                        if (existingPortMapping.getId() != null &&
                                existingPortMapping.getExposedPort().equals(portMappingDto.getExposedPort())) {

                            // Update existing port mapping
                            existingPortMapping.setProtocol(portMappingDto.getProtocol());
                            existingPortMapping.setHostPort(portMappingDto.getHostPort());
                            found = true;
                            break;
                        }
                    }

                    // If not found, create a new PortMapping
                    if (!found) {
                        PortMapping newPortMapping = new PortMapping();
                        newPortMapping.setProtocol(portMappingDto.getProtocol());
                        newPortMapping.setExposedPort(portMappingDto.getExposedPort());
                        newPortMapping.setHostPort(portMappingDto.getHostPort());
                        containerConfig.getPortMappings().add(newPortMapping);
                    }
                }

                // Remove port mappings that are no longer present in the DTO
                currentPortMappings.removeIf(existingPortMapping -> containerConfigDto.getPortMappings().stream()
                        .noneMatch(portMappingDto -> portMappingDto.getExposedPort().equals(existingPortMapping.getExposedPort())));
            }

            // Save the updated deployment (and associated ContainerConfig)
            return deploymentRepository.save(deployment);
        } else {
            // If the deployment doesn't exist, you might want to handle this case appropriately
            throw new EntityNotFoundException("Deployment with ID " + containerConfigDto.getDeploymentId() + " not found.");
        }
    }


    public Deployment saveDeployment(ContainerConfigDto containerConfigDto) {
        // Create and populate ContainerConfig from DTO
        ContainerConfig containerConfig = new ContainerConfig();
        containerConfig.setImageName(containerConfigDto.getImageName());
        containerConfig.setImageTag(containerConfigDto.getImageTag());
        containerConfig.setName(containerConfigDto.getName());
        containerConfig.setMemory(containerConfigDto.getMemory());
        containerConfig.setCpu(containerConfigDto.getCpusetCpus());
        containerConfig.setEnv(containerConfigDto.getEnv());
        containerConfig.setCmd(containerConfigDto.getCmd());
        containerConfig.setUsername(containerConfigDto.getUsername());
        containerConfig.setPassword(containerConfigDto.getPassword());
        containerConfig.setEmail(containerConfigDto.getEmail());
        containerConfig.setServerAddress(containerConfigDto.getServerAddress());
        containerConfig.setStatus("pending");
        containerConfig.setState(true);

        // Set PortMappings
        List<PortMapping> portMappings = containerConfigDto.getPortMappings() != null
                ?containerConfigDto.getPortMappings()
                .stream()
                .map(this::mapToPortMapping)
                .collect(Collectors.toList()):null;
        containerConfig.setPortMappings(portMappings);

        // Create Deployment
        Deployment deployment = new Deployment();
        deployment.setDeploymentId(helper.generateDeploymentId());
        deployment.setContainerConfig(containerConfig);
        deployment.setContainerName(containerConfigDto.getName());

        // Save the deployment (which also saves the associated ContainerConfig)
        return deploymentRepository.save(deployment);
    }

    private PortMapping mapToPortMapping(PortMapping portMappingDto) {
        PortMapping portMapping = new PortMapping();
        portMapping.setProtocol(portMappingDto.getProtocol());
        portMapping.setExposedPort(portMappingDto.getExposedPort());
        portMapping.setHostPort(portMappingDto.getHostPort());
        return portMapping;
    }


    private void updateContainerConfig(ContainerConfig containerConfig, ContainerConfigDto dto) {
        if (dto.getImageName() != null) {
            containerConfig.setImageName(dto.getImageName());
        }
        if (dto.getImageTag() != null) {
            containerConfig.setImageTag(dto.getImageTag());
        }
        if (dto.getName() != null) {
            containerConfig.setName(dto.getName());
        }
        if (dto.getMemory() != null) {
            containerConfig.setMemory(dto.getMemory());
        }
        if (dto.getCpusetCpus() != null) {
            containerConfig.setCpu(dto.getCpusetCpus());
        }
        if (dto.getEnv() != null) {
            containerConfig.setEnv(dto.getEnv());
        }
        if (dto.getCmd() != null) {
            containerConfig.setCmd(dto.getCmd());
        }
        if (dto.getUsername() != null) {
            containerConfig.setUsername(dto.getUsername());
        }
        if (dto.getPassword() != null) {
            containerConfig.setPassword(dto.getPassword());
        }
        if (dto.getEmail() != null) {
            containerConfig.setEmail(dto.getEmail());
        }
        if (dto.getServerAddress() != null) {
            containerConfig.setServerAddress(dto.getServerAddress());
        }

        // Update status, defaulting to "pending" if not provided
        containerConfig.setStatus("created");

        // Handle port mappings if provided
        if (dto.getPortMappings() != null) {
            List<PortMapping> currentPortMappings = containerConfig.getPortMappings();

            // Iterate through the DTO and update or add new PortMappings
            for (PortMapping portMappingDto : dto.getPortMappings()) {
                boolean found = false;
                for (PortMapping existingPortMapping : currentPortMappings) {
                    if (existingPortMapping.getId() != null &&
                            existingPortMapping.getExposedPort().equals(portMappingDto.getExposedPort())) {

                        // Update existing port mapping
                        existingPortMapping.setProtocol(portMappingDto.getProtocol());
                        existingPortMapping.setHostPort(portMappingDto.getHostPort());
                        found = true;
                        break;
                    }
                }

                // If not found, create a new PortMapping
                if (!found) {
                    PortMapping newPortMapping = new PortMapping();
                    newPortMapping.setProtocol(portMappingDto.getProtocol());
                    newPortMapping.setExposedPort(portMappingDto.getExposedPort());
                    newPortMapping.setHostPort(portMappingDto.getHostPort());
                    containerConfig.getPortMappings().add(newPortMapping);
                }
            }

            // Remove port mappings that are no longer present in the DTO
            currentPortMappings.removeIf(existingPortMapping -> dto.getPortMappings().stream()
                    .noneMatch(portMappingDto -> portMappingDto.getExposedPort().equals(existingPortMapping.getExposedPort())));
        }
    }

    private void log(String containerId, String Message) {
        Deployment deployment = deploymentRepository.findByDeploymentId(containerId)
                .orElseThrow(() -> new NoSuchElementException("Deployment with deploymentId '" + containerId + "' not found."));
        ContainerConfig containerConfig = deployment.getContainerConfig();
        containerConfig.setLog(Message);
        deploymentRepository.save(deployment);
    }

}


