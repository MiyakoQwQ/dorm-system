package cn.edu.sdu.java.server.repository;

import cn.edu.sdu.java.server.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findByStatusOrderByCreatedAtDesc(String status);
    List<Announcement> findByTypeOrderByCreatedAtDesc(String type);
    List<Announcement> findAllByOrderByCreatedAtDesc();
}
