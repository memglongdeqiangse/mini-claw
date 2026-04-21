package com.miniclaw.skill;

import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.skill.repository.FileSystemSkillRepository;
import io.agentscope.core.tool.*;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description: skill管理器
 * @author: lei
 * @date: 2026/3/23
 */
@Slf4j
public class SkillManager {

    public static Map<String, List<AgentSkill>> SKILLMAP = new HashMap<>();

    /**
     * 加载单个skill，返回SkillBox
     *
     * @param path
     * @return SkillBox
     */
    public static SkillBox registerSkills(String path) {
        SkillBox skillBox = new SkillBox(new Toolkit());
        List<AgentSkill> agentSkills = loadSkillsByPath(path);
        if (agentSkills != null && !agentSkills.isEmpty()) {
            for (AgentSkill agentSkill : agentSkills) {
                try {
                    skillBox.registerSkill(agentSkill);
                } catch (Exception e) {
                    log.warn("加载skill {} 失败", path, e);
                }
            }

        } else {
            log.warn("没有找到skill {}", path);
            return null;
        }
        return skillBox;
    }

    /**
     * 加载多个skill，返回SkillBox
     *
     * @param paths
     * @return
     */
    public static SkillBox registerSkills(List<String> paths) {
        SkillBox skillBox = new SkillBox(new Toolkit());
        for (String path : paths) {
            List<AgentSkill> agentSkills = loadSkillsByPath(path);
            if (agentSkills != null && !agentSkills.isEmpty()) {
                for (AgentSkill agentSkill : agentSkills) {
                    try {
                        skillBox.registerSkill(agentSkill);
                    } catch (Exception e) {
                        log.warn("加载skill {} 失败", path, e);
                    }
                }
            } else {
                log.warn("没有找到skill {}", path);
                return null;
            }
        }
        return skillBox;
    }

    /**
     * 加载指定路径下的技能
     *
     * @param path
     * @return
     */
    public static List<AgentSkill> loadSkillsByPath(String path) {
        //处理path，如果以～开头，则替换
        if (path.startsWith("~")) {
            path = path.replaceFirst("~", System.getProperty("user.home"));
        }
        //加载指定路径下的skill
        try {
            Path baseDir = Path.of(path);
            //先判断是否加载过
            if (SKILLMAP.containsKey(baseDir.toAbsolutePath().toString())) {
                log.warn("skill {} 已经加载过", path);
                return SKILLMAP.get(baseDir.toAbsolutePath().toString());
            }
            FileSystemSkillRepository skillRepository = new FileSystemSkillRepository(baseDir);
            List<AgentSkill> skill = skillRepository.getAllSkills();
            SKILLMAP.put(baseDir.toAbsolutePath().toString(), skill);
            return skill;
        } catch (Exception e) {
            log.info("加载{}下的skill失败", path, e);
        }
        return null;
    }

}

