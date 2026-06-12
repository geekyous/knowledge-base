package com.geekyous.kb.service;

import com.geekyous.kb.dto.request.CreateTagRequest;
import com.geekyous.kb.dto.request.UpdateTagRequest;
import com.geekyous.kb.dto.response.TagResponse;
import com.geekyous.kb.entity.Tag;
import com.geekyous.kb.exception.BusinessException;
import com.geekyous.kb.repository.TagRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 标签服务 — 处理标签的增删改查业务逻辑。
 * 标签的 usageCount 为反范式化字段，由文档关联操作维护。
 *
 * @author Geekyous Guo
 */
@Slf4j
@Service
public class TagService {

    private static final String DEFAULT_TAG_COLOR = "#2563eb";

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    /**
     * 获取所有标签列表 — 按使用次数降序排列。
     *
     * @return 标签响应列表
     */
    public List<TagResponse> listTags() {
        return tagRepository.findAllOrderByUsageCountDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 创建新标签 — 校验名称唯一性，颜色默认 "#2563eb"。
     *
     * @param req 创建标签请求
     * @return 创建后的标签响应
     */
    @Transactional
    public TagResponse createTag(CreateTagRequest req) {
        if (tagRepository.existsByName(req.getName())) {
            throw new BusinessException(409, "标签名称已存在");
        }

        Tag tag = Tag.builder()
                .name(req.getName())
                .color(req.getColor() != null ? req.getColor() : DEFAULT_TAG_COLOR)
                .build();

        Tag saved = tagRepository.save(tag);
        log.info("标签创建: name={}", saved.getName());
        return toResponse(saved);
    }

    /**
     * 更新标签 — 只更新请求中非 null 的字段。
     *
     * @param id  标签 ID
     * @param req 更新标签请求
     * @return 更新后的标签响应
     */
    @Transactional
    public TagResponse updateTag(Integer id, UpdateTagRequest req) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "标签不存在"));

        if (req.getName() != null) {
            tag.setName(req.getName());
        }
        if (req.getColor() != null) {
            tag.setColor(req.getColor());
        }

        Tag saved = tagRepository.save(tag);
        log.info("标签更新: id={}", id);
        return toResponse(saved);
    }

    /**
     * 删除标签。
     *
     * @param id 标签 ID
     */
    @Transactional
    public void deleteTag(Integer id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "标签不存在"));

        tagRepository.delete(tag);
        log.info("标签删除: id={}, name={}", id, tag.getName());
    }

    /**
     * 实体转响应 DTO — usageCount 映射为 docCount。
     */
    private TagResponse toResponse(Tag tag) {
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .color(tag.getColor())
                .docCount(tag.getUsageCount())
                .createdAt(tag.getCreatedAt())
                .build();
    }
}
