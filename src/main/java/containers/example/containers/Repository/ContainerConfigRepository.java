package containers.example.containers.Repository;

import containers.example.containers.Entity.ContainerConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContainerConfigRepository extends JpaRepository<ContainerConfig, Long> {
}