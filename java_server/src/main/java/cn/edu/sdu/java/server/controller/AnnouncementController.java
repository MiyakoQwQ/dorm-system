package cn.edu.sdu.java.server.controller;

import cn.edu.sdu.java.server.entity.Announcement;
import cn.edu.sdu.java.server.repository.AnnouncementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公告管理 Controller
 */
@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    @Autowired
    private AnnouncementRepository announcementRepository;

    /** 获取已发布的公告列表（学生端） */
    @GetMapping
    public List<Announcement> getPublished() {
        return announcementRepository.findByStatusOrderByCreatedAtDesc("Published");
    }

    /** 获取所有公告（管理员端，含草稿/归档） */
    @GetMapping("/all")
    public List<Announcement> getAll() {
        return announcementRepository.findAllByOrderByCreatedAtDesc();
    }

    /** 新增公告 */
    @PostMapping
    public ResponseEntity<Announcement> create(@RequestBody Announcement announcement) {
        return ResponseEntity.ok(announcementRepository.save(announcement));
    }

    /** 修改公告 */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Announcement updated) {
        return announcementRepository.findById(id).map(ann -> {
            ann.setTitle(updated.getTitle());
            ann.setContent(updated.getContent());
            ann.setType(updated.getType());
            ann.setStatus(updated.getStatus());
            return ResponseEntity.ok(announcementRepository.save(ann));
        }).orElse(ResponseEntity.notFound().build());
    }

    /** 删除公告 */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!announcementRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        announcementRepository.deleteById(id);
        return ResponseEntity.ok("删除成功");
    }

    /** 归档公告 */
    @PutMapping("/{id}/archive")
    public ResponseEntity<?> archive(@PathVariable Long id) {
        return announcementRepository.findById(id).map(ann -> {
            ann.setStatus("Archived");
            return ResponseEntity.ok(announcementRepository.save(ann));
        }).orElse(ResponseEntity.notFound().build());
    }

    /** 初始化示例公告 */
    @GetMapping("/init")
    public String initDemoAnnouncements() {
        if (announcementRepository.count() > 0) return "公告数据已存在";
        String[][] demos = {
            {"紧急", "紧急通知：本周五全体宿舍大扫除", "请各寝室于本周五下午2点前完成宿舍卫生清扫工作，宿管将进行检查评分。"},
            {"通知", "宿舍用电安全提醒", "严禁在宿舍使用大功率电器（电磁炉、热得快等），违者将按校规处理。"},
            {"公告", "2025-2026学年宿舍分配公告", "新学年宿舍分配结果已公布，请同学们于9月1日前完成入住手续。"},
            {"通知", "网络维护通知", "本周六凌晨0:00-6:00进行网络设备维护，期间网络可能短暂中断，敬请谅解。"}
        };
        for (String[] d : demos) {
            Announcement ann = new Announcement();
            ann.setType(d[0]);
            ann.setTitle(d[1]);
            ann.setContent(d[2]);
            ann.setPublisher("admin");
            ann.setStatus("Published");
            announcementRepository.save(ann);
        }
        return "初始化公告成功";
    }
}
