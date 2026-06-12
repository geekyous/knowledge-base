package com.geekyous.kb.repository;

import com.geekyous.kb.entity.User;
import com.geekyous.kb.entity.User.Role;
import com.geekyous.kb.entity.User.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户数据访问层
 *
 * @author Geekyous Guo
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmailHash(String emailHash);

    boolean existsByUsername(String username);

    boolean existsByEmailHash(String emailHash);

    /** 按角色分页查询（管理员后台） */
    Page<User> findByRole(Role role, Pageable pageable);

    /** 按状态分页查询（管理员后台） */
    Page<User> findByStatus(UserStatus status, Pageable pageable);

    /** 按用户名或邮箱模糊搜索（管理员后台） */
    Page<User> findByUsernameContainingOrEmailContaining(String username, String email, Pageable pageable);
}
