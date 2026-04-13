package cn.edu.sdu.java.server.repository;

import cn.edu.sdu.java.server.entity.RepairOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface RepairOrderRepository extends JpaRepository<RepairOrder, Long> {
    List<RepairOrder> findByRoomNumberContaining(String keyword);
    List<RepairOrder> findByStatus(String status);
    List<RepairOrder> findByStudentNameContainingOrRoomNumberContaining(String name, String room);
    List<RepairOrder> findByStudentName(String studentName);
    List<RepairOrder> findByStudentNameContaining(String studentName);

    @Query("SELECT r.status, COUNT(r) FROM RepairOrder r GROUP BY r.status")
    List<Object[]> countByStatus();

    @Query("SELECT r.repairType, COUNT(r) FROM RepairOrder r GROUP BY r.repairType")
    List<Object[]> countByType();

    @Query("SELECT r.priority, COUNT(r) FROM RepairOrder r GROUP BY r.priority")
    List<Object[]> countByPriority();

    /** 按报修月份统计数量（用于趋势图） */
    @Query(value = "SELECT DATE_FORMAT(created_at, '%Y-%m') as month, COUNT(*) FROM repair_order " +
                   "WHERE created_at >= DATE_SUB(NOW(), INTERVAL 6 MONTH) " +
                   "GROUP BY month ORDER BY month", nativeQuery = true)
    List<Object[]> countByMonth();
}
