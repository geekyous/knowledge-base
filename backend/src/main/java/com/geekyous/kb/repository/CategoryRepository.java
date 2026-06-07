package com.geekyous.kb.repository;

import com.geekyous.kb.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 分类数据访问层，支持两级树形结构查询
 *
 * @author Geekyous Guo
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * 查询所有顶层分类（根节点），用于构建分类树的入口
     */
    List<Category> findByParentIdIsNull();

    /**
     * 查询指定父分类下的直接子分类（不递归）
     */
    List<Category> findByParentId(Long parentId);

    /**
     * 根据 slug（URL 标识）查找分类
     */
    Optional<Category> findBySlug(String slug);
}
