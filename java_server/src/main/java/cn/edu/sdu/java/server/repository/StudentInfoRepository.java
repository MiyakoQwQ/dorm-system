package cn.edu.sdu.java.server.repository;

import cn.edu.sdu.java.server.entity.StudentInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface StudentInfoRepository extends JpaRepository<StudentInfo, Long> {
    Optional<StudentInfo> findByUserId(Long userId);
    Optional<StudentInfo> findByStudentNo(String studentNo);
    List<StudentInfo> findByRoomNumber(String roomNumber);
    List<StudentInfo> findByStatus(String status);

    @Query("SELECT s FROM StudentInfo s WHERE " +
           "(:keyword IS NULL OR s.realName LIKE %:keyword% OR s.studentNo LIKE %:keyword% OR s.roomNumber LIKE %:keyword%)")
    List<StudentInfo> search(@Param("keyword") String keyword);

    @Query("SELECT s.department, COUNT(s) FROM StudentInfo s WHERE s.status = '在住' GROUP BY s.department")
    List<Object[]> countByDepartment();
}
