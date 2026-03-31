package javawork1.dorm.controller;

import javawork1.dorm.entity.RepairOrder;
import javawork1.dorm.repository.RepairOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 报修工单管理 Controller（升级版：含审批流、催单、评价、统计）
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private RepairOrderRepository repairOrderRepository;

    // ===== 1. 查询 =====

    /** 获取报修单列表，支持关键字搜索 + 状态过滤 */
    @GetMapping
    public List<RepairOrder> getOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        List<RepairOrder> result;
        if (keyword != null && !keyword.trim().isEmpty()) {
            result = repairOrderRepository.findByStudentNameContainingOrRoomNumberContaining(keyword, keyword);
        } else if (status != null && !status.trim().isEmpty()) {
            result = repairOrderRepository.findByStatus(status);
        } else {
            result = repairOrderRepository.findAll();
        }
        // 按创建时间倒序
        result.sort((a, b) -> {
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });
        return result;
    }

    /** 获取单个工单详情 */
    @GetMapping("/{id}")
    public ResponseEntity<RepairOrder> getOrder(@PathVariable Long id) {
        return repairOrderRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ===== 2. 提交报修 =====

    /** 学生提交新报修单 */
    @PostMapping
    public RepairOrder createOrder(@RequestBody RepairOrder newOrder) {
        newOrder.setStatus("Pending");
        newOrder.setUrgeCount(0);
        System.out.println("【报修】收到新工单，报修人: " + newOrder.getStudentName()
                + "，宿舍: " + newOrder.getRoomNumber()
                + "，类型: " + newOrder.getRepairType()
                + "，优先级: " + newOrder.getPriority());
        return repairOrderRepository.save(newOrder);
    }

    // ===== 3. 管理员操作（审批流） =====

    /** 管理员受理工单（Pending → Processing） */
    @PutMapping("/{id}/accept")
    public ResponseEntity<?> acceptOrder(@PathVariable Long id,
                                         @RequestParam(required = false) String remark) {
        return repairOrderRepository.findById(id).map(order -> {
            if (!"Pending".equals(order.getStatus())) {
                return ResponseEntity.badRequest().body("只有【待处理】状态的工单才能受理");
            }
            order.setStatus("Processing");
            if (remark != null) order.setAdminRemark(remark);
            System.out.println("【管理员受理】工单 #" + id);
            return ResponseEntity.<Object>ok(repairOrderRepository.save(order));
        }).orElse(ResponseEntity.notFound().build());
    }

    /** 管理员完结工单（Processing → Resolved） */
    @PutMapping("/{id}/resolve")
    public ResponseEntity<?> resolveOrder(@PathVariable Long id,
                                           @RequestParam(required = false) String remark) {
        return repairOrderRepository.findById(id).map(order -> {
            if (!"Processing".equals(order.getStatus()) && !"Pending".equals(order.getStatus())) {
                return ResponseEntity.badRequest().body("只有【处理中】或【待处理】的工单才能完结");
            }
            order.setStatus("Resolved");
            order.setResolvedAt(LocalDateTime.now());
            if (remark != null) order.setAdminRemark(remark);
            System.out.println("【管理员完结】工单 #" + id);
            return ResponseEntity.<Object>ok(repairOrderRepository.save(order));
        }).orElse(ResponseEntity.notFound().build());
    }

    /** 管理员驳回工单（Pending/Processing → Rejected） */
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectOrder(@PathVariable Long id,
                                          @RequestParam(required = false) String remark) {
        return repairOrderRepository.findById(id).map(order -> {
            if ("Resolved".equals(order.getStatus()) || "Rejected".equals(order.getStatus())) {
                return ResponseEntity.badRequest().body("已完结或已驳回的工单不能再操作");
            }
            order.setStatus("Rejected");
            if (remark != null) order.setAdminRemark(remark);
            System.out.println("【管理员驳回】工单 #" + id + "，原因: " + remark);
            return ResponseEntity.<Object>ok(repairOrderRepository.save(order));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ===== 4. 学生操作 =====

    /** 学生催单（Pending/Processing 状态可催） */
    @PutMapping("/{id}/urge")
    public ResponseEntity<?> urgeOrder(@PathVariable Long id) {
        return repairOrderRepository.findById(id).map(order -> {
            if (!"Pending".equals(order.getStatus()) && !"Processing".equals(order.getStatus())) {
                return ResponseEntity.badRequest().body("当前状态不允许催单");
            }
            int count = order.getUrgeCount() == null ? 0 : order.getUrgeCount();
            if (count >= 3) {
                return ResponseEntity.badRequest().body("每个工单最多催单3次");
            }
            order.setUrgeCount(count + 1);
            System.out.println("【催单】工单 #" + id + " 被催第 " + (count + 1) + " 次");
            return ResponseEntity.<Object>ok(repairOrderRepository.save(order));
        }).orElse(ResponseEntity.notFound().build());
    }

    /** 学生评价（Resolved 状态可评价，且只能评一次） */
    @PutMapping("/{id}/review")
    public ResponseEntity<?> reviewOrder(@PathVariable Long id,
                                          @RequestParam Integer rating,
                                          @RequestParam(required = false) String comment) {
        if (rating < 1 || rating > 5) {
            return ResponseEntity.badRequest().body("评分必须在 1-5 分之间");
        }
        return repairOrderRepository.findById(id).map(order -> {
            if (!"Resolved".equals(order.getStatus())) {
                return ResponseEntity.badRequest().body("只有【已解决】的工单才能评价");
            }
            if (order.getRating() != null) {
                return ResponseEntity.badRequest().body("已评价，不能重复评价");
            }
            order.setRating(rating);
            order.setReviewComment(comment);
            System.out.println("【评价】工单 #" + id + " 获得 " + rating + " 星评价");
            return ResponseEntity.<Object>ok(repairOrderRepository.save(order));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ===== 5. 统计 =====

    /** 报修统计数据（管理员仪表盘用） */
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        // 各状态数量
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Object[] row : repairOrderRepository.countByStatus()) {
            byStatus.put((String) row[0], (Long) row[1]);
        }
        stats.put("byStatus", byStatus);

        // 各类型数量
        Map<String, Long> byType = new LinkedHashMap<>();
        for (Object[] row : repairOrderRepository.countByType()) {
            byType.put((String) row[0], (Long) row[1]);
        }
        stats.put("byType", byType);

        // 各优先级数量
        Map<String, Long> byPriority = new LinkedHashMap<>();
        for (Object[] row : repairOrderRepository.countByPriority()) {
            byPriority.put((String) row[0], (Long) row[1]);
        }
        stats.put("byPriority", byPriority);

        // 总计
        stats.put("total", repairOrderRepository.count());
        stats.put("pending", byStatus.getOrDefault("Pending", 0L));
        stats.put("processing", byStatus.getOrDefault("Processing", 0L));
        stats.put("resolved", byStatus.getOrDefault("Resolved", 0L));

        return stats;
    }
}
