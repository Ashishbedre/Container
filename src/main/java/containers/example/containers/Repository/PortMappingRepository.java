package containers.example.containers.Repository;

import containers.example.containers.Entity.PortMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PortMappingRepository extends JpaRepository<PortMapping, Long> {
}