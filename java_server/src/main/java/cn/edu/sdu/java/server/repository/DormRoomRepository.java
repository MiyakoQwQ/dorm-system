package cn.edu.sdu.java.server.repository;

import cn.edu.sdu.java.server.entity.DormRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DormRoomRepository extends JpaRepository<DormRoom, Long> {
    Optional<DormRoom> findByRoomNumber(String roomNumber);
    List<DormRoom> findByBuildingNameContaining(String buildingName);
    List<DormRoom> findByStatus(String status);
    List<DormRoom> findByBuildingName(String buildingName);
}
