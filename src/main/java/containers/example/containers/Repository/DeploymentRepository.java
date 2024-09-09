package containers.example.containers.Repository;

import containers.example.containers.Entity.Deployment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface DeploymentRepository extends JpaRepository<Deployment, Long> {
}