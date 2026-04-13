package cn.edu.sdu.java.server.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "repair_order")
public class RepairOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_name", nullable = false, length = 50)
    private String studentName;

    @Column(name = "room_number", nullable = false, length = 20)
    private String roomNumber;

    /** 宿舍楼 */
    @Column(name = "building", length = 50)
    private String building;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    /** 优先级：Low / Normal / High / Urgent */
    @Column(length = 10)
    private String priority = "Normal";

    /** 报修类型：水电/家具/网络/门窗/其他 */
    @Column(name = "repair_type", length = 20)
    private String repairType = "其他";

    /** 状态：Pending / Processing / Resolved / Rejected */
    @Column(length = 20)
    private String status = "Pending";

    /** 管理员备注 / 处理意见 */
    @Column(name = "admin_remark", columnDefinition = "TEXT")
    private String adminRemark;

    /** 学生催单次数 */
    @Column(name = "urge_count")
    private Integer urgeCount = 0;

    /** 学生评价分（1-5，null 表示未评价） */
    @Column(name = "rating")
    private Integer rating;

    /** 学生评价内容 */
    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ===== Getters & Setters =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getRepairType() { return repairType; }
    public void setRepairType(String repairType) { this.repairType = repairType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAdminRemark() { return adminRemark; }
    public void setAdminRemark(String adminRemark) { this.adminRemark = adminRemark; }
    public Integer getUrgeCount() { return urgeCount; }
    public void setUrgeCount(Integer urgeCount) { this.urgeCount = urgeCount; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
