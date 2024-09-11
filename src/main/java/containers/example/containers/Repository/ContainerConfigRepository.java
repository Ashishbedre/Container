package containers.example.containers.Repository;

import containers.example.containers.Entity.ContainerConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import containers.example.containers.Entity.ContainerConfig;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface ContainerConfigRepository extends JpaRepository<ContainerConfig, Long> {
}