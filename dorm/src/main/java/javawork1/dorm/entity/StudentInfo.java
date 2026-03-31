package javawork1.dorm.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 学生信息（扩展用户，与 User 通过 studentId 关联）
 */
@Entity
@Table(name = "student_info")
public class StudentInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联的用户 ID */
    @Column(name = "user_id", unique = true)
    private Long userId;

    /** 学号 */
    @Column(name = "student_no", length = 20, unique = true)
    private String studentNo;

    /** 真实姓名 */
    @Column(name = "real_name", length = 50)
    private String realName;

    /** 性别：男/女 */
    @Column(length = 5)
    private String gender;

    /** 联系电话 */
    @Column(name = "phone", length = 20)
    private String phone;

    /** 院系 */
    @Column(name = "department", length = 100)
    private String department;

    /** 专业 */
    @Column(name = "major", length = 100)
    private String major;

    /** 班级 */
    @Column(name = "class_name", length = 50)
    private String className;

    /** 年级，如 2025 */
    @Column(name = "grade")
    private Integer grade;

    /** 分配的宿舍号（冗余，加速查询） */
    @Column(name = "room_number", length = 20)
    private String roomNumber;

    /** 床位号 1-8 */
    @Column(name = "bed_number")
    private Integer bedNumber;

    /** 入住时间 */
    @Column(name = "check_in_date")
    private LocalDateTime checkInDate;

    /** 状态：在住/离校/毕业 */
    @Column(length = 10)
    private String status = "在住";

    // ===== Getters & Setters =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getStudentNo() { return studentNo; }
    public void setStudentNo(String studentNo) { this.studentNo = studentNo; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public Integer getBedNumber() { return bedNumber; }
    public void setBedNumber(Integer bedNumber) { this.bedNumber = bedNumber; }
    public LocalDateTime getCheckInDate() { return checkInDate; }
    public void setCheckInDate(LocalDateTime checkInDate) { this.checkInDate = checkInDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
