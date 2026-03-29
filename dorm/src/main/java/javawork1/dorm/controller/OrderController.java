package javawork1.dorm.controller;

import javawork1.dorm.entity.RepairOrder;
import javawork1.dorm.repository.RepairOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private RepairOrderRepository repairOrderRepository;

    // 1. 处理 GET 请求：负责把数据库里的所有单子挖出来，发给前端
// 升级版 GET 请求：接收可选的搜索关键字参数 (keyword)
    @GetMapping
    public List<RepairOrder> getOrders(@org.springframework.web.bind.annotation.RequestParam(required = false) String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            System.out.println("【后端】执行模糊搜索，关键字: " + keyword);
            return repairOrderRepository.findByRoomNumberContaining(keyword);
        }
        System.out.println("【后端】执行全量查询...");
        return repairOrderRepository.findAll();
    }

    // 2. 处理 POST 请求：负责接收前端发来的 JSON 数据，物理写入数据库
    @PostMapping
    public RepairOrder createOrder(@RequestBody RepairOrder newOrder) {
        System.out.println("【后端】收到前端提交的新报修单，报修人: " + newOrder.getStudentName());
        return repairOrderRepository.save(newOrder);
    }
    // ================= 前面的 GET 和 POST 方法保留不动 =================

    // @PutMapping 专门用于处理“修改/更新”的网络请求
    // {id} 是一个路径变量。比如前端请求 /api/orders/1/resolve，这个 1 就会被抓取下来赋给 id 参数
    @PutMapping("/{id}/resolve")
    public RepairOrder resolveOrder(@PathVariable Long id) {
        System.out.println("【后端】收到管理员指令，准备处理报修单，单号: " + id);

        // 1. 物理探查：先让矿工去数据库里把这张单子找出来。如果找不到，直接抛出异常引爆。
        RepairOrder order = repairOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("【致命错误】找不到该单号在物理硬盘上的记录！"));

        // 2. 状态重写：把它的状态强行改为 已解决 (Resolved)
        order.setStatus("Resolved");

        // 3. 落盘保存：再次调用 save 方法。
        // 神奇的 JPA 框架发现这个单子有 ID，它在底层就不会执行 INSERT，而是自动执行 UPDATE repair_order SET status = 'Resolved' WHERE id = ?
        return repairOrderRepository.save(order);
    }
}