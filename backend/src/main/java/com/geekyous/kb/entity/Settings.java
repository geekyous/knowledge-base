package com.geekyous.kb.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 系统设置实体，映射 kb_settings 表。用于存储系统级键值对配置。
 *
 * @author Geekyous Guo
 */
@Entity
@Table(name = "kb_settings")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Settings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 配置键，全局唯一 */
    @Column(name = "setting_key", nullable = false, unique = true, length = 100)
    private String settingKey;

    /** 配置值，使用 TEXT 类型支持长文本 */
    @Column(name = "setting_value", nullable = false, columnDefinition = "TEXT")
    private String settingValue;

    /** 配置分类，用于按组管理设置项 */
    @Column(nullable = false, length = 50)
    private String category;

    /** 配置项的描述说明 */
    @Column(length = 255)
    private String description;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
