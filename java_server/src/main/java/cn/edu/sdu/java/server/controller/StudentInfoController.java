package cn.edu.sdu.java.server.controller;

import cn.edu.sdu.java.server.entity.StudentInfo;
import cn.edu.sdu.java.server.entity.DormRoom;
import cn.edu.sdu.java.server.repository.StudentInfoRepository;
import cn.edu.sdu.java.server.repository.DormRoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 学生信息管理 Controller
 */
@RestController
@RequestMapping("/api/students")
public class StudentInfoController {

    @Autowired
    private StudentInfoRepository studentInfoRepository;

    @Autowired
    private DormRoomRepository dormRoomRepository;

    /** 查询学生列表，支持关键字搜索 */
    @GetMapping
    public List<StudentInfo> getStudents(@RequestParam(required = false) String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return studentInfoRepository.search(keyword);
        }
        return studentInfoRepository.findAll();
    }

    /** 获取单个学生 */
    @GetMapping("/{id}")
    public ResponseEntity<StudentInfo> getStudent(@PathVariable Long id) {
        return studentInfoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** 通过 userId 获取学生信息（前端登录后调用） */
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<StudentInfo> getStudentByUser(@PathVariable Long userId) {
        return studentInfoRepository.findByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** 新增学生 */
    @PostMapping
    public ResponseEntity<?> createStudent(@RequestBody StudentInfo student) {
        if (student.getStudentNo() != null &&
            studentInfoRepository.findByStudentNo(student.getStudentNo()).isPresent()) {
            return ResponseEntity.badRequest().body("学号已存在！");
        }
        if (student.getCheckInDate() == null) {
            student.setCheckInDate(LocalDateTime.now());
        }
        // 同步更新宿舍人数
        if (student.getRoomNumber() != null) {
            dormRoomRepository.findByRoomNumber(student.getRoomNumber()).ifPresent(room -> {
                room.setCurrentCount(room.getCurrentCount() + 1);
                dormRoomRepository.save(room);
            });
        }
        return ResponseEntity.ok(studentInfoRepository.save(student));
    }

    /** 修改学生信息 */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateStudent(@PathVariable Long id, @RequestBody StudentInfo updated) {
        return studentInfoRepository.findById(id).map(student -> {
            // 如果宿舍号发生变更，同步更新宿舍人数
            if (updated.getRoomNumber() != null &&
                !updated.getRoomNumber().equals(student.getRoomNumber())) {
                // 旧宿舍-1
                if (student.getRoomNumber() != null) {
                    dormRoomRepository.findByRoomNumber(student.getRoomNumber()).ifPresent(room -> {
                        room.setCurrentCount(Math.max(0, room.getCurrentCount() - 1));
                        dormRoomRepository.save(room);
                    });
                }
                // 新宿舍+1
                dormRoomRepository.findByRoomNumber(updated.getRoomNumber()).ifPresent(room -> {
                    room.setCurrentCount(room.getCurrentCount() + 1);
                    dormRoomRepository.save(room);
                });
            }
            student.setStudentNo(updated.getStudentNo());
            student.setRealName(updated.getRealName());
            student.setGender(updated.getGender());
            student.setPhone(updated.getPhone());
            student.setDepartment(updated.getDepartment());
            student.setMajor(updated.getMajor());
            student.setClassName(updated.getClassName());
            student.setGrade(updated.getGrade());
            student.setRoomNumber(updated.getRoomNumber());
            student.setBedNumber(updated.getBedNumber());
            student.setStatus(updated.getStatus());
            return ResponseEntity.ok(studentInfoRepository.save(student));
        }).orElse(ResponseEntity.notFound().build());
    }

    /** 删除学生 */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStudent(@PathVariable Long id) {
        return studentInfoRepository.findById(id).map(student -> {
            // 同步宿舍人数
            if (student.getRoomNumber() != null) {
                dormRoomRepository.findByRoomNumber(student.getRoomNumber()).ifPresent(room -> {
                    room.setCurrentCount(Math.max(0, room.getCurrentCount() - 1));
                    dormRoomRepository.save(room);
                });
            }
            studentInfoRepository.deleteById(id);
            return ResponseEntity.ok("删除成功");
        }).orElse(ResponseEntity.notFound().build());
    }

    /** 按宿舍查询学生 */
    @GetMapping("/by-room/{roomNumber}")
    public List<StudentInfo> getStudentsByRoom(@PathVariable String roomNumber) {
        return studentInfoRepository.findByRoomNumber(roomNumber);
    }

    /** 统计各院系人数 */
    @GetMapping("/stats/department")
    public List<Map<String, Object>> statsByDepartment() {
        List<Object[]> raw = studentInfoRepository.countByDepartment();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : raw) {
            Map<String, Object> item = new HashMap<>();
            item.put("department", row[0]);
            item.put("count", row[1]);
            result.add(item);
        }
        return result;
    }

    /** 初始化示例学生数据 */
    @GetMapping("/init")
    public String initDemoStudents() {
        if (studentInfoRepository.count() > 0) return "学生数据已存在";
        String[] departments = {"计算机学院", "软件学院", "数学学院"};
        String[] majors = {"计算机科学", "软件工程", "数学"};
        String[] rooms = {"一101", "一102", "一103", "二101", "二102"};
        for (int i = 1; i <= 20; i++) {
            StudentInfo s = new StudentInfo();
            s.setStudentNo(String.format("2025%04d", i));
            s.setRealName("学生" + i);
            s.setGender(i % 2 == 0 ? "男" : "女");
            s.setPhone("1380000" + String.format("%04d", i));
            int dIdx = (i - 1) % 3;
            s.setDepartment(departments[dIdx]);
            s.setMajor(majors[dIdx]);
            s.setClassName("智能科技25-0" + ((i - 1) / 5 + 1) + "班");
            s.setGrade(2025);
            s.setRoomNumber(rooms[(i - 1) % 5]);
            s.setBedNumber((i - 1) % 4 + 1);
            s.setCheckInDate(LocalDateTime.now().minusDays(i * 3L));
            s.setStatus("在住");
            studentInfoRepository.save(s);
        }
        return "已初始化 " + studentInfoRepository.count() + " 名学生";
    }
}
