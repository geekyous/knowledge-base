package com.geekyous.kb.service;

import com.geekyous.kb.entity.Settings;
import com.geekyous.kb.repository.SettingsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统设置服务 — 处理系统配置项的查询和更新。
 * 设置按 category 分组，值自动进行类型推断（布尔/数值/字符串）。
 *
 * @author Geekyous Guo
 */
@Slf4j
@Service
public class SettingsService {

    private final SettingsRepository settingsRepository;

    public SettingsService(SettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    /**
     * 获取所有系统设置 — 按 category 分组，返回嵌套 Map 结构。
     * 示例: {"ai": {"enable_qa": true, ...}, "permission": {...}, "storage": {...}}
     *
     * 值类型自动推断："true"/"false" → Boolean，纯数字 → Number，其余 → String。
     *
     * @return 分类分组的设置 Map
     */
    public Map<String, Map<String, Object>> getAllSettings() {
        List<Settings> allSettings = settingsRepository.findAll();

        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (Settings setting : allSettings) {
            result.computeIfAbsent(setting.getCategory(), k -> new LinkedHashMap<>())
                    .put(setting.getSettingKey(), parseValue(setting.getSettingValue()));
        }

        return result;
    }

    /**
     * 批量更新设置项 — 遍历传入的 key-value 对，逐项查找并更新。
     *
     * @param settings 需要更新的设置 key-value 映射
     */
    @Transactional
    public void updateSettings(Map<String, String> settings) {
        if (settings == null || settings.isEmpty()) {
            return;
        }
        settings.forEach((key, value) -> {
            Settings setting = settingsRepository.findBySettingKey(key)
                    .orElseThrow(() -> new com.geekyous.kb.exception.BusinessException(404, "设置项不存在: " + key));
            setting.setSettingValue(value);
            settingsRepository.save(setting);
        });
        log.info("系统设置更新: count={}", settings.size());
    }

    /**
     * 解析设置值 — 自动推断类型。
     * "true"/"false" → Boolean，纯数字 → Long，其余保留 String。
     */
    private Object parseValue(String value) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return value;
        }
    }
}
