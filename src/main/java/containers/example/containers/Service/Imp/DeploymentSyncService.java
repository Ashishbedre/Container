package containers.example.containers.Service.Imp;

import com.fasterxml.jackson.databind.ObjectMapper;
import containers.example.containers.Entity.ContainerConfig;
import containers.example.containers.Entity.Deployment;
import containers.example.containers.Entity.PortMapping;
import containers.example.containers.Repository.DeploymentRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

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

        if (apiDeployments == null || apiDeployments.isEmpty()) {
            // If the API call fails or returns no data, log an error and exit the method
            System.err.println("Failed to fetch deployments from the Docker API or no deployments found.");
            return;
        }

        // Step 3: Iterate over API deployments and sync with the DB
        Set<String> apiDeploymentIds = new HashSet<>();
        apiDeployments.forEach(apiDeployment -> {
            String deploymentId = (String) apiDeployment.get("Id");
            apiDeploymentIds.add(deploymentId);

            Optional<Deployment> dbDeploymentOpt = deployments.stream()
                    .filter(dbDeployment -> dbDeployment.getDeploymentId().equals(deploymentId))
                    .findFirst();

            if (dbDeploymentOpt.isPresent()) {
                // Deployment exists, update its details
                Deployment dbDeployment = dbDeploymentOpt.get();
                updateDeploymentFromApi(dbDeployment, apiDeployment);
                deploymentRepository.save(dbDeployment);
            } else {
                // Deployment doesn't exist, create a new one
                Deployment newDeployment = createDeploymentFromApi(apiDeployment);
                deploymentRepository.save(newDeployment);
            }
        });
        // Step 4: Handle deployments that are in the database but not in the API
        deployments.forEach(dbDeployment -> {
            if (!apiDeploymentIds.contains(dbDeployment.getDeploymentId())) {
                // Deployment exists in DB but not in the API, delete it
                deploymentRepository.deleteByDeploymentId(dbDeployment.getDeploymentId());
            }
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
//        dbDeployment.setContainerName((String) apiDeployment.get("containerName"));

        // Update ContainerConfig
        ContainerConfig config = dbDeployment.getContainerConfig();
        if (config == null) {
            config = new ContainerConfig();
        }
        config.setStatus((String) apiDeployment.get("State"));
//        String state = (String) apiDeployment.get("State");
//        if ("running".equals(state)) {
//            config.setState("1");
//        }else {
//            config.setState("0");
//        }
        dbDeployment.setContainerConfig(config);
    }

    // Method to create a new Deployment from API data
    private Deployment createDeploymentFromApi(Map apiDeployment) {
        Deployment newDeployment = new Deployment();
        newDeployment.setDeploymentId((String) apiDeployment.get("Id"));

        // Create ContainerConfig
        ContainerConfig config = new ContainerConfig();
//        config.setImageName((String) apiDeployment.get("imageName"));
//        config.setImageTag((String) apiDeployment.get("imageTag"));

        // Extracting image name and tag from the full image string
        String fullImage = (String) apiDeployment.get("Image");
        if (fullImage != null) {
            String[] imageParts = fullImage.split(":");
            config.setImageName(imageParts[0]);
            if (imageParts.length > 1) {
                config.setImageTag(imageParts[1]);
            } else {
                config.setImageTag("");
            }
        }
        List<String> namesList = (List<String>) apiDeployment.get("Names");

        if (namesList != null && !namesList.isEmpty()) {
            String names = namesList.get(0);  // Accessing the first element of the list
            if (names != null && !names.isEmpty()) {
                newDeployment.setContainerName(names.substring(1));  // Remove first character (like "/")
                config.setName(names.substring(1));
            } else {
                System.out.println("Names element is null or empty");
            }
        } else {
            System.out.println("Names list is null or empty");
        }

        config.setStatus((String) apiDeployment.get("State"));



//        config.setName((String) apiDeployment.get("name"));
//        config.setCpu((String) apiDeployment.get("cpu"));
//        config.setMemory(((Number) apiDeployment.get("memory")).longValue());
//        config.setStatus((String) apiDeployment.get("status"));

        newDeployment.setContainerConfig(config);

        return newDeployment;
    }



}

