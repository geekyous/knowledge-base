package com.geekyous.kb.repository;

import com.geekyous.kb.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 标签数据访问层，提供标签实体的基础 CRUD 及自定义查询。
 *
 * @author Geekyous Guo
 */
@Repository
public interface TagRepository extends JpaRepository<Tag, Integer> {

    /** 判断指定名称的标签是否已存在 */
    boolean existsByName(String name);

    /** 查询所有标签，按使用次数降序排列 */
    @Query("SELECT t FROM Tag t ORDER BY t.usageCount DESC")
    List<Tag> findAllOrderByUsageCountDesc();
}
