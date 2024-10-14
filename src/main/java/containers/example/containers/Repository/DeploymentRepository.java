package containers.example.containers.Repository;

import containers.example.containers.Entity.Deployment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface DeploymentRepository extends JpaRepository<Deployment, Long> {

    @Query("SELECT SUM(c.memory) FROM Deployment d JOIN d.containerConfig c WHERE c.status = 'running'")
    Long getTotalMemory();

    @Query("SELECT SUM(CAST(c.cpu AS int)) FROM Deployment d JOIN d.containerConfig c WHERE c.status = 'running'")
    Integer getTotalCpu();

    Optional<Deployment> findByContainerName(String containerName);

    Optional<Deployment> findByDeploymentId(String deploymentId);

    // Custom delete method based on deploymentId
    void deleteByDeploymentId(String deploymentId);


}