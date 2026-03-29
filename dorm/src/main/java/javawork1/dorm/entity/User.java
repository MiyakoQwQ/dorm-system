package javawork1.dorm.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "system_user") // 不能用 user 作为表名，这是数据库的系统保留字，会引发底层崩溃
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username; // 登录账号

    // 【致命修复】：将 length 从 50 改为 255，给 BCrypt 密文腾出足够的物理空间！
    @Column(nullable = false, length = 255)
    private String password; // 登录密码 (真实环境必须加密，目前课设暂时用明文)

    @Column(nullable = false, length = 20)
    private String role; // 角色：STUDENT (学生) 或 ADMIN (宿管)

    // 必须的 Getter 和 Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}