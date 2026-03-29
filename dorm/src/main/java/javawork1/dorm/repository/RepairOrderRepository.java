package javawork1.dorm.repository;

import javawork1.dorm.entity.RepairOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; // 记得导入 List

public interface RepairOrderRepository extends JpaRepository<RepairOrder, Long> {
    // 新增这把高级铁镐：通过宿舍号包含的关键字进行模糊查找 (相当于 SQL 里的 LIKE '%keyword%')
    List<RepairOrder> findByRoomNumberContaining(String keyword);
}