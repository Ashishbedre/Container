package containers.example.containers.Service.Imp;

import com.fasterxml.jackson.databind.ObjectMapper;
import containers.example.containers.Entity.ContainerConfig;
import containers.example.containers.Entity.Deployment;
import containers.example.containers.Entity.PortMapping;
import containers.example.containers.Repository.DeploymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@EnableScheduling
public class DeploymentSyncService {

    @Autowired
    private DeploymentRepository deploymentRepository;

    @Autowired
    private final WebClient webClient;

    @Autowired
    private ObjectMapper objectMapper;
    @Value("${docker.api.url}")
    private String dockerApiUrl;

    public DeploymentSyncService(WebClient webClient) {
        this.webClient = webClient;
    }

    // Scheduled to run every 10 minutes (600000 ms)
    @Scheduled(fixedRate = 600000)
    public void syncDeployments() {
        // Step 1: Fetch all deployments from the database
        List<Deployment> deployments = deploymentRepository.findAll();

        // Step 2: Fetch data from external Docker API
        List<Map> apiDeployments = fetchApiDeployments();

        // Step 3: Iterate over API deployments and sync with the DB
        apiDeployments.forEach(apiDeployment -> {
            String deploymentId = (String) apiDeployment.get("Id");

            Optional<Deployment> dbDeploymentOpt = deployments.stream()
                    .filter(dbDeployment -> dbDeployment.getDeploymentId().equals(deploymentId))
                    .findFirst();

            if (dbDeploymentOpt.isPresent()) {
                // Deployment exists, update its details
                Deployment dbDeployment = dbDeploymentOpt.get();
                updateDeploymentFromApi(dbDeployment, apiDeployment);
                deploymentRepository.save(dbDeployment);
                }
//            } else {
//                // Deployment doesn't exist, create a new one
//                Deployment newDeployment = createDeploymentFromApi(apiDeployment);
//                deploymentRepository.save(newDeployment);
//            }
        });
    }

    // Method to call the external API and return raw data
    private List<Map> fetchApiDeployments() {
        // Fetch raw data as List<Map>
        return webClient
                .get()
                .uri(dockerApiUrl +"/containers/json?all=true")
                .retrieve()
                .bodyToFlux(Map.class)
                .collectList()
                .block(); // Blocking to simulate sync for this example
    }

    // Method to update an existing Deployment from API data
    private void updateDeploymentFromApi(Deployment dbDeployment, Map apiDeployment) {
        dbDeployment.setContainerName((String) apiDeployment.get("containerName"));

        // Update ContainerConfig
        ContainerConfig config = dbDeployment.getContainerConfig();
        if (config == null) {
            config = new ContainerConfig();
        }
//        config.setImageName((String) apiDeployment.get("imageName"));
//        config.setImageTag((String) apiDeployment.get("imageTag"));
//        config.setName((String) apiDeployment.get("name"));
//        config.setCpu((String) apiDeployment.get("cpu"));
//        config.setMemory(((Number) apiDeployment.get("memory")).longValue());

        config.setStatus((String) apiDeployment.get("State"));
//        config.setEnv((List<String>) apiDeployment.get("env"));
//        config.setCmd((List<String>) apiDeployment.get("cmd"));

        // Set PortMappings
//        List<Map<String, String>> apiPortMappings = (List<Map<String, String>>) apiDeployment.get("portMappings");
//        if (apiPortMappings != null) {
//            List<PortMapping> portMappings = apiPortMappings.stream()
//                    .map(this::convertToPortMapping)
//                    .toList();
//            config.setPortMappings(portMappings);
//        }

        // Set ContainerConfig back to Deployment
        dbDeployment.setContainerConfig(config);
    }

//    // Method to create a new Deployment from API data
//    private Deployment createDeploymentFromApi(Map apiDeployment) {
//        Deployment newDeployment = new Deployment();
//        newDeployment.setDeploymentId((String) apiDeployment.get("deploymentId"));
//        newDeployment.setContainerName((String) apiDeployment.get("containerName"));
//
//        // Create ContainerConfig
//        ContainerConfig config = new ContainerConfig();
//        config.setImageName((String) apiDeployment.get("imageName"));
//        config.setImageTag((String) apiDeployment.get("imageTag"));
//        config.setName((String) apiDeployment.get("name"));
//        config.setCpu((String) apiDeployment.get("cpu"));
//        config.setMemory(((Number) apiDeployment.get("memory")).longValue());
//        config.setStatus((String) apiDeployment.get("status"));
//        config.setEnv((List<String>) apiDeployment.get("env"));
//        config.setCmd((List<String>) apiDeployment.get("cmd"));
//
//        // Set PortMappings
//        List<Map<String, String>> apiPortMappings = (List<Map<String, String>>) apiDeployment.get("portMappings");
//        if (apiPortMappings != null) {
//            List<PortMapping> portMappings = apiPortMappings.stream()
//                    .map(this::convertToPortMapping)
//                    .toList();
//            config.setPortMappings(portMappings);
//        }
//
//        newDeployment.setContainerConfig(config);
//
//        return newDeployment;
//    }


    // Convert a raw port mapping from API response to PortMapping entity
//    private PortMapping convertToPortMapping(Map<String, String> apiPortMapping) {
//        PortMapping portMapping = new PortMapping();
//        portMapping.setProtocol(apiPortMapping.get("protocol"));
//        portMapping.setExposedPort(apiPortMapping.get("exposedPort"));
//        portMapping.setHostPort(apiPortMapping.get("hostPort"));
//        return portMapping;
//    }
}

