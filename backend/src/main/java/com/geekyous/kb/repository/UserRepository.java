package com.geekyous.kb.repository;

import com.geekyous.kb.entity.User;
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

    /**
     * 根据用户名查找用户（登录认证）
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱哈希查找用户。
     * email 字段已加密，无法直接查询，调用方需先计算 SHA-256 哈希再传入。
     *
     * @param emailHash 邮箱的 SHA-256 哈希值
     * @see com.geekyous.kb.utils.FieldEncryptor#hash(String)
     */
    Optional<User> findByEmailHash(String emailHash);

    /**
     * 检查用户名是否已存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否已注册。
     * email 字段已加密，调用方需先计算 SHA-256 哈希再传入。
     *
     * @param emailHash 邮箱的 SHA-256 哈希值
     * @see com.geekyous.kb.utils.FieldEncryptor#hash(String)
     */
    boolean existsByEmailHash(String emailHash);
}
