package javawork1.dorm.controller;

import javawork1.dorm.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 数据统计汇总 Controller（仪表盘用）
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private RepairOrderRepository repairOrderRepository;

    @Autowired
    private StudentInfoRepository studentInfoRepository;

    @Autowired
    private DormRoomRepository dormRoomRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private UserRepository userRepository;

    /** 管理员仪表盘全量统计数据 */
    @GetMapping
    public Map<String, Object> getDashboard() {
        Map<String, Object> data = new LinkedHashMap<>();

        // === 核心数字卡片 ===
        data.put("totalStudents", studentInfoRepository.count());
        data.put("totalRooms", dormRoomRepository.count());
        data.put("totalOrders", repairOrderRepository.count());
        data.put("totalUsers", userRepository.count());
        data.put("publishedAnnouncements", announcementRepository.findByStatusOrderByCreatedAtDesc("Published").size());

        // 待处理报修单数
        data.put("pendingOrders", repairOrderRepository.findByStatus("Pending").size());
        data.put("processingOrders", repairOrderRepository.findByStatus("Processing").size());
        data.put("resolvedOrders", repairOrderRepository.findByStatus("Resolved").size());

        // 宿舍使用率
        long normalRooms = dormRoomRepository.findByStatus("Normal").size();
        long maintenanceRooms = dormRoomRepository.findByStatus("Maintenance").size();
        data.put("normalRooms", normalRooms);
        data.put("maintenanceRooms", maintenanceRooms);

        // === 图表数据 ===

        // 报修类型饼图
        Map<String, Long> orderByType = new LinkedHashMap<>();
        for (Object[] row : repairOrderRepository.countByType()) {
            orderByType.put(String.valueOf(row[0]), (Long) row[1]);
        }
        data.put("orderByType", orderByType);

        // 报修状态饼图
        Map<String, Long> orderByStatus = new LinkedHashMap<>();
        for (Object[] row : repairOrderRepository.countByStatus()) {
            orderByStatus.put(String.valueOf(row[0]), (Long) row[1]);
        }
        data.put("orderByStatus", orderByStatus);

        // 各院系学生人数柱状图
        List<Map<String, Object>> studentByDept = new ArrayList<>();
        for (Object[] row : studentInfoRepository.countByDepartment()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", row[0]);
            item.put("value", row[1]);
            studentByDept.add(item);
        }
        data.put("studentByDepartment", studentByDept);

        return data;
    }
}
