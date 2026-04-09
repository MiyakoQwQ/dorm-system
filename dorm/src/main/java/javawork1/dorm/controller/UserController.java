package javawork1.dorm.controller;

import javawork1.dorm.entity.User;
import javawork1.dorm.entity.StudentInfo;
import javawork1.dorm.repository.UserRepository;
import javawork1.dorm.repository.StudentInfoRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentInfoRepository studentInfoRepository;

    // ================= 接口 1：登录核验（兼容前端调用） =================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String username, 
                                    @RequestParam String password,
                                    @RequestParam String role) {
        System.out.println("【安检中心】收到登录请求，账号: " + username + ", 角色: " + role);

        return userRepository.findByUsername(username)
                .map(user -> {
                    if (BCrypt.checkpw(password, user.getPassword())) {
                        // 验证角色是否匹配
                        if (user.getRole().equals(role)) {
                            System.out.println("【安检通过】身份: " + user.getRole());
                            return ResponseEntity.ok(user.getRole());
                        } else {
                            System.out.println("【安检拦截】角色不匹配");
                            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("角色不匹配");
                        }
                    } else {
                        System.out.println("【安检拦截】密码碰撞失败");
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("密码错误");
                    }
                })
                .orElseGet(() -> {
                    System.out.println("【安检拦截】账号不存在");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("账号不存在");
                });
    }

    // ================= 接口 2：登录核验（JSON请求体方式） =================
    @PostMapping("/users/login")
    public ResponseEntity<?> loginWithBody(@RequestBody User loginRequest) {
        System.out.println("【安检中心】收到登录请求，账号: " + loginRequest.getUsername());

        return userRepository.findByUsername(loginRequest.getUsername())
                .map(user -> {
                    if (BCrypt.checkpw(loginRequest.getPassword(), user.getPassword())) {
                        System.out.println("【安检通过】身份: " + user.getRole());
                        return ResponseEntity.ok(user);
                    } else {
                        System.out.println("【安检拦截】密码碰撞失败");
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("密码错误");
                    }
                })
                .orElseGet(() -> {
                    System.out.println("【安检拦截】账号不存在");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("账号不存在");
                });
    }

    // ================= 接口 3：安全注册 (新增) =================
    @PostMapping("/users/register")
    public ResponseEntity<?> register(@RequestBody User registerRequest) {
        System.out.println("【安检中心】收到注册申请，申请账号: " + registerRequest.getUsername());

        // 防线 1：物理查重拦截
        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            System.out.println("【安检拦截】账号已被占用！");
            return ResponseEntity.status(HttpStatus.CONFLICT).body("该账号已被占用，请更换账号！");
        }

        // 防线 2 & 3：字段隔离与强制加密
        User secureUser = new User();
        secureUser.setUsername(registerRequest.getUsername());
        secureUser.setPassword(BCrypt.hashpw(registerRequest.getPassword(), BCrypt.gensalt()));

        // 防线 4：越权切断，强制锁定为学生
        secureUser.setRole("STUDENT");

        // 保存用户
        User savedUser = userRepository.save(secureUser);
        
        // 自动创建关联的学生信息记录
        StudentInfo studentInfo = new StudentInfo();
        studentInfo.setUserId(savedUser.getId());
        studentInfo.setRealName(registerRequest.getUsername()); // 默认使用用户名作为真实姓名
        studentInfo.setStudentNo(""); // 学号留空，由管理员后续完善
        studentInfo.setStatus("在住");
        studentInfo.setCheckInDate(LocalDateTime.now());
        studentInfoRepository.save(studentInfo);
        
        System.out.println("【安检通过】新学生档案已建立！用户ID: " + savedUser.getId());
        return ResponseEntity.ok("注册成功！");
    }

    // ================= 接口 4：初始化测试数据 =================
    @GetMapping("/users/init")
    public String initTestUsers() {
        if(userRepository.count() == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(BCrypt.hashpw("123456", BCrypt.gensalt()));
            admin.setRole("ADMIN");
            userRepository.save(admin);

            User student = new User();
            student.setUsername("student");
            student.setPassword(BCrypt.hashpw("123456", BCrypt.gensalt()));
            student.setRole("STUDENT");
            userRepository.save(student);
            return "加密测试账号初始化成功！数据库中已不存在明文密码。";
        }
        return "账号已存在。如果旧密码是明文，请手动清空数据库后重新初始化！";
    }
}