package cn.edu.sdu.java.server.repository;

import cn.edu.sdu.java.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 赋予矿工根据账号精准挖人的能力
    Optional<User> findByUsername(String username);
}