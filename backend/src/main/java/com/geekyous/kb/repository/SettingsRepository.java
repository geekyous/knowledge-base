package com.geekyous.kb.repository;

import com.geekyous.kb.entity.Settings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 系统设置数据访问层，提供设置实体的基础 CRUD 及自定义查询。
 *
 * @author Geekyous Guo
 */
@Repository
public interface SettingsRepository extends JpaRepository<Settings, Long> {

    /** 根据配置键查询设置项 */
    Optional<Settings> findBySettingKey(String key);

    /** 根据分类查询设置项列表 */
    List<Settings> findByCategory(String category);
}
