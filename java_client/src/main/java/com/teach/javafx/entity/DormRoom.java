package com.teach.javafx.entity;

import jakarta.persistence.*;

/**
 * 宿舍房间信息
 */
@Entity
@Table(name = "dorm_room")
public class DormRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 宿舍楼名称，如 "一号楼" */
    @Column(name = "building_name", nullable = false, length = 50)
    private String buildingName;

    /** 房间号，如 "101" */
    @Column(name = "room_number", nullable = false, length = 20, unique = true)
    private String roomNumber;

    /** 楼层 */
    @Column(name = "floor")
    private Integer floor;

    /** 房间类型：4人间/6人间/8人间 */
    @Column(name = "room_type", length = 20)
    private String roomType = "4人间";

    /** 额定人数 */
    @Column(name = "capacity")
    private Integer capacity = 4;

    /** 当前入住人数（冗余字段，提升查询性能） */
    @Column(name = "current_count")
    private Integer currentCount = 0;

    /** 状态：Normal / Maintenance / Closed */
    @Column(length = 20)
    private String status = "Normal";

    /** 备注 */
    @Column(columnDefinition = "TEXT")
    private String remark;

    // ===== Getters & Setters =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBuildingName() { return buildingName; }
    public void setBuildingName(String buildingName) { this.buildingName = buildingName; }
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public Integer getFloor() { return floor; }
    public void setFloor(Integer floor) { this.floor = floor; }
    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public Integer getCurrentCount() { return currentCount; }
    public void setCurrentCount(Integer currentCount) { this.currentCount = currentCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
