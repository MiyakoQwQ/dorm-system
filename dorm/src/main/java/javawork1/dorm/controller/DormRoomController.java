package javawork1.dorm.controller;

import javawork1.dorm.entity.DormRoom;
import javawork1.dorm.repository.DormRoomRepository;
import javawork1.dorm.repository.StudentInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 宿舍房间管理 Controller
 */
@RestController
@RequestMapping("/api/rooms")
public class DormRoomController {

    @Autowired
    private DormRoomRepository dormRoomRepository;

    @Autowired
    private StudentInfoRepository studentInfoRepository;

    /** 获取所有宿舍，支持楼栋名过滤 */
    @GetMapping
    public List<DormRoom> getRooms(@RequestParam(required = false) String building) {
        if (building != null && !building.trim().isEmpty()) {
            return dormRoomRepository.findByBuildingNameContaining(building);
        }
        return dormRoomRepository.findAll();
    }

    /** 获取单个宿舍详情 */
    @GetMapping("/{id}")
    public ResponseEntity<DormRoom> getRoom(@PathVariable Long id) {
        return dormRoomRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** 新增宿舍 */
    @PostMapping
    public ResponseEntity<?> createRoom(@RequestBody DormRoom room) {
        if (dormRoomRepository.findByRoomNumber(room.getRoomNumber()).isPresent()) {
            return ResponseEntity.badRequest().body("宿舍号已存在！");
        }
        DormRoom saved = dormRoomRepository.save(room);
        return ResponseEntity.ok(saved);
    }

    /** 修改宿舍信息 */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateRoom(@PathVariable Long id, @RequestBody DormRoom updated) {
        return dormRoomRepository.findById(id).map(room -> {
            room.setBuildingName(updated.getBuildingName());
            room.setRoomNumber(updated.getRoomNumber());
            room.setFloor(updated.getFloor());
            room.setRoomType(updated.getRoomType());
            room.setCapacity(updated.getCapacity());
            room.setStatus(updated.getStatus());
            room.setRemark(updated.getRemark());
            return ResponseEntity.ok(dormRoomRepository.save(room));
        }).orElse(ResponseEntity.notFound().build());
    }

    /** 删除宿舍 */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRoom(@PathVariable Long id) {
        if (!dormRoomRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        dormRoomRepository.deleteById(id);
        return ResponseEntity.ok("删除成功");
    }

    /** 获取各楼栋统计 */
    @GetMapping("/stats")
    public Map<String, Object> getRoomStats() {
        Map<String, Object> stats = new HashMap<>();
        long total = dormRoomRepository.count();
        long normalCount = dormRoomRepository.findByStatus("Normal").size();
        long maintenanceCount = dormRoomRepository.findByStatus("Maintenance").size();
        stats.put("total", total);
        stats.put("normal", normalCount);
        stats.put("maintenance", maintenanceCount);
        return stats;
    }

    /** 初始化示例宿舍数据 */
    @GetMapping("/init")
    public String initDemoRooms() {
        if (dormRoomRepository.count() > 0) return "宿舍数据已存在";
        String[] buildings = {"一号楼", "二号楼", "三号楼"};
        int[] floors = {1, 2, 3, 4};
        for (String building : buildings) {
            for (int floor : floors) {
                for (int room = 1; room <= 5; room++) {
                    DormRoom r = new DormRoom();
                    r.setBuildingName(building);
                    String roomNum = building.charAt(0) + String.valueOf(floor * 100 + room);
                    r.setRoomNumber(roomNum);
                    r.setFloor(floor);
                    r.setRoomType("4人间");
                    r.setCapacity(4);
                    r.setCurrentCount(0);
                    r.setStatus("Normal");
                    dormRoomRepository.save(r);
                }
            }
        }
        return "已初始化 " + dormRoomRepository.count() + " 间宿舍";
    }
}
