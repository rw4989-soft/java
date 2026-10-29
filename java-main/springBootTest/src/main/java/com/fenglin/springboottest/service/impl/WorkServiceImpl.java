package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fenglin.springboottest.entity.WorkProfile;
import com.fenglin.springboottest.entity.WorkShift;
import com.fenglin.springboottest.mapper.WorkProfileMapper;
import com.fenglin.springboottest.mapper.WorkShiftMapper;
import com.fenglin.springboottest.service.WorkScript;
import com.fenglin.springboottest.service.WorkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 打工日常实现。
 *
 * 核心规则：
 * 1. 每个用户每个角色一份档案，两条线互不干扰；
 * 2. 每个自然日只能打一次卡（防刷，也让「连续打卡」有意义）；
 * 3. 结算数值全部由服务端按剧本重新计算，前端提交的只是「选了第几个」。
 */
@Service
public class WorkServiceImpl implements WorkService {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("MM-dd");

    @Autowired
    private WorkProfileMapper profileMapper;

    @Autowired
    private WorkShiftMapper shiftMapper;

    private final ObjectMapper json = new ObjectMapper();

    /** 职级阶梯：绩效达到 min 即为该级别 */
    private record Level(String name, int min) {
    }

    private static final List<Level> LEVELS = List.of(
            new Level("实习第一天", 0),
            new Level("正式员工", 50),
            new Level("熟练工", 150),
            new Level("蟹堡王骨干", 350),
            new Level("传奇员工", 700)
    );

    // ==================== 对外接口 ====================

    @Override
    public Map<String, Object> overview(Long userId) {
        LocalDate today = LocalDate.now();

        List<Map<String, Object>> roles = new ArrayList<>();
        int totalShifts = 0;

        for (WorkScript.Role role : WorkScript.ROLES) {
            WorkProfile p = ensureProfile(userId, role.code());
            totalShifts += p.getShiftCount();

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", role.code());
            item.put("name", role.name());
            item.put("job", role.job());
            item.put("emoji", role.emoji());
            item.put("workplace", role.workplace());
            item.put("tagline", role.tagline());
            item.put("accent", role.accent());

            item.put("coins", p.getCoins());
            item.put("mood", p.getMood());
            item.put("performance", p.getPerformance());
            item.put("shiftCount", p.getShiftCount());
            item.put("streak", p.getStreak());

            Level lv = levelOf(p.getPerformance());
            item.put("level", lv.name());
            item.put("levelIndex", levelIndex(p.getPerformance()));
            item.put("levelProgress", levelProgress(p.getPerformance()));

            item.put("todayDone", today.equals(p.getLastShiftDate()));
            item.put("moodLabel", moodLabel(p.getMood()));
            item.put("gradeCounts", gradeCounts(userId, role.code()));

            roles.add(item);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("today", today.toString());
        data.put("todayLabel", today.format(DAY_FMT));
        data.put("roles", roles);
        data.put("totalShifts", totalShifts);
        data.put("levels", LEVELS.stream()
                .map(l -> Map.of("name", l.name(), "min", l.min()))
                .toList());
        data.put("recent", recentShifts(userId, null, 8));
        return data;
    }

    @Override
    public Map<String, Object> draw(Long userId, String roleCode) {
        WorkScript.Role role = WorkScript.roleOf(roleCode);
        if (role == null) {
            throw new IllegalStateException("没有这个角色，你是从哪个海底冒出来的？");
        }

        WorkProfile p = ensureProfile(userId, roleCode);
        if (LocalDate.now().equals(p.getLastShiftDate())) {
            throw new IllegalStateException(role.name() + "今天已经打过卡了，明天再来吧");
        }

        List<WorkScript.Scene> all = new ArrayList<>(WorkScript.scenesOf(roleCode));
        Collections.shuffle(all, new Random());
        List<WorkScript.Scene> picked = new ArrayList<>(
                all.subList(0, Math.min(WorkScript.SCENES_PER_SHIFT, all.size())));

        // 抽完再按剧情时间排序：否则会出现「早 8 点开门」排在「23:30 下班」后面，
        // 整个班次读起来就不像一天，像一段被剪坏的录像。
        picked.sort(Comparator.comparing(WorkScript.Scene::time));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("roleCode", role.code());
        data.put("roleName", role.name());
        data.put("emoji", role.emoji());
        data.put("accent", role.accent());
        data.put("shiftToken", System.currentTimeMillis());
        data.put("scenes", picked);
        return data;
    }

    @Override
    public Map<String, Object> settle(Long userId, String roleCode, List<Map<String, Object>> picks) {
        WorkScript.Role role = WorkScript.roleOf(roleCode);
        if (role == null) {
            throw new IllegalStateException("没有这个角色");
        }
        if (picks == null || picks.isEmpty()) {
            throw new IllegalStateException("一次都没选，这班算白上了");
        }

        WorkProfile p = ensureProfile(userId, roleCode);
        LocalDate today = LocalDate.now();
        if (today.equals(p.getLastShiftDate())) {
            throw new IllegalStateException(role.name() + "今天已经打过卡了，明天再来吧");
        }

        // 按剧本重新算一遍，不信任前端传来的数值
        int coinsGain = 0;
        int moodDelta = 0;
        int perfDelta = 0;
        List<Map<String, Object>> timeline = new ArrayList<>();

        for (Map<String, Object> pick : picks) {
            String sceneId = String.valueOf(pick.get("sceneId"));
            int choiceIndex = toInt(pick.get("choiceIndex"), -1);

            WorkScript.Scene scene = WorkScript.scenesOf(roleCode).stream()
                    .filter(s -> s.id().equals(sceneId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("剧本里没有这一幕：" + sceneId));

            if (choiceIndex < 0 || choiceIndex >= scene.choices().size()) {
                throw new IllegalStateException("选项越界了，重来一次吧");
            }

            WorkScript.Choice choice = scene.choices().get(choiceIndex);
            coinsGain += choice.coins();
            moodDelta += choice.mood();
            perfDelta += choice.perf();

            Map<String, Object> step = new LinkedHashMap<>();
            step.put("sceneId", scene.id());
            step.put("title", scene.title());
            step.put("icon", scene.icon());
            step.put("time", scene.time());
            step.put("choice", choice.text());
            step.put("tone", choice.tone());
            step.put("coins", choice.coins());
            step.put("mood", choice.mood());
            step.put("perf", choice.perf());
            step.put("result", choice.result());
            timeline.add(step);
        }

        // 评级：绩效权重最高，其次金币，心情做微调
        int score = perfDelta * 2 + Math.round(coinsGain * 1.5f) + moodDelta;
        String grade = score >= 52 ? "S" : score >= 34 ? "A" : score >= 16 ? "B" : "C";

        int oldLevelIdx = levelIndex(p.getPerformance());
        int oldCoins = p.getCoins();
        int oldMood = p.getMood();

        int newCoins = Math.max(0, oldCoins + coinsGain);
        int newMood = clamp(oldMood + moodDelta);
        int newPerf = Math.max(0, p.getPerformance() + perfDelta);
        int newLevelIdx = levelIndex(newPerf);

        // 被 0/100 的上下限截断时，如实反映「实际变化」，
        // 否则会出现面板写着「心情 +31」而实际只涨到 100 的假账。
        int actualCoinsDelta = newCoins - oldCoins;
        int actualMoodDelta = newMood - oldMood;

        // 连续打卡：昨天打过就 +1，否则从 1 重新开始
        LocalDate last = p.getLastShiftDate();
        int streak = (last != null && last.equals(today.minusDays(1))) ? p.getStreak() + 1 : 1;

        p.setCoins(newCoins);
        p.setMood(newMood);
        p.setPerformance(newPerf);
        p.setShiftCount(p.getShiftCount() + 1);
        p.setStreak(streak);
        p.setLastShiftDate(today);
        profileMapper.updateById(p);

        String summary = gradeCopy(roleCode, grade)[0];
        String comment = gradeCopy(roleCode, grade)[1];

        WorkShift shift = new WorkShift();
        shift.setUserId(userId);
        shift.setRoleCode(roleCode);
        shift.setShiftDate(today);
        shift.setCoinsGain(actualCoinsDelta);
        shift.setMoodDelta(actualMoodDelta);
        shift.setPerformDelta(perfDelta);
        shift.setGrade(grade);
        shift.setSummary(summary);
        shift.setTimeline(writeJson(timeline));
        shiftMapper.insert(shift);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("roleCode", roleCode);
        data.put("roleName", role.name());
        data.put("emoji", role.emoji());
        data.put("grade", grade);
        data.put("score", score);
        data.put("coinsGain", actualCoinsDelta);
        data.put("moodDelta", actualMoodDelta);
        data.put("performDelta", perfDelta);
        data.put("coins", newCoins);
        data.put("mood", newMood);
        data.put("performance", newPerf);
        data.put("moodLabel", moodLabel(newMood));
        data.put("shiftCount", p.getShiftCount());
        data.put("streak", streak);
        data.put("level", levelOf(newPerf).name());
        data.put("levelUp", newLevelIdx > oldLevelIdx);
        data.put("title", summary);
        data.put("comment", comment);
        data.put("timeline", timeline);
        return data;
    }

    @Override
    public List<Map<String, Object>> history(Long userId, String roleCode, int limit) {
        return recentShifts(userId, roleCode, Math.max(1, Math.min(limit, 50)));
    }

    // ==================== 内部工具 ====================

    private WorkProfile ensureProfile(Long userId, String roleCode) {
        WorkProfile p = profileMapper.selectOne(new LambdaQueryWrapper<WorkProfile>()
                .eq(WorkProfile::getUserId, userId)
                .eq(WorkProfile::getRoleCode, roleCode));

        if (p == null) {
            WorkProfile fresh = new WorkProfile();
            fresh.setUserId(userId);
            fresh.setRoleCode(roleCode);
            // 入职给 20 个蟹堡币当本金。
            // 如果从 0 开始，第一局里「请客 / 让利 / 补货」这类花钱的选项会被
            // Math.max(0, ...) 直接吞掉，玩家选了 -12 却看到 +0，像是按钮坏了。
            fresh.setCoins(20);
            fresh.setMood(80);
            fresh.setPerformance(0);
            fresh.setShiftCount(0);
            fresh.setStreak(0);
            profileMapper.insert(fresh);
            p = profileMapper.selectById(fresh.getId());
        }
        return p;
    }

    private List<Map<String, Object>> recentShifts(Long userId, String roleCode, int limit) {
        LambdaQueryWrapper<WorkShift> qw = new LambdaQueryWrapper<WorkShift>()
                .eq(WorkShift::getUserId, userId)
                .orderByDesc(WorkShift::getId)
                .last("limit " + limit);
        if (roleCode != null && !roleCode.isBlank()) {
            qw.eq(WorkShift::getRoleCode, roleCode);
        }

        return shiftMapper.selectList(qw).stream().map(s -> {
            WorkScript.Role role = WorkScript.roleOf(s.getRoleCode());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("roleCode", s.getRoleCode());
            m.put("roleName", role == null ? s.getRoleCode() : role.name());
            m.put("emoji", role == null ? "🐚" : role.emoji());
            m.put("date", s.getShiftDate() == null ? "" : s.getShiftDate().toString());
            m.put("dateLabel", s.getShiftDate() == null ? "" : s.getShiftDate().format(DAY_FMT));
            m.put("grade", s.getGrade());
            m.put("summary", s.getSummary());
            m.put("coinsGain", s.getCoinsGain());
            m.put("moodDelta", s.getMoodDelta());
            m.put("performDelta", s.getPerformDelta());
            return m;
        }).toList();
    }

    private Map<String, Integer> gradeCounts(Long userId, String roleCode) {
        List<WorkShift> shifts = shiftMapper.selectList(new LambdaQueryWrapper<WorkShift>()
                .eq(WorkShift::getUserId, userId)
                .eq(WorkShift::getRoleCode, roleCode));

        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("S", 0);
        counts.put("A", 0);
        counts.put("B", 0);
        counts.put("C", 0);
        shifts.stream()
                .collect(Collectors.groupingBy(WorkShift::getGrade, Collectors.counting()))
                .forEach((k, v) -> counts.put(k, v.intValue()));
        return counts;
    }

    private Level levelOf(int perf) {
        Level current = LEVELS.get(0);
        for (Level l : LEVELS) {
            if (perf >= l.min()) {
                current = l;
            }
        }
        return current;
    }

    private int levelIndex(int perf) {
        int idx = 0;
        for (int i = 0; i < LEVELS.size(); i++) {
            if (perf >= LEVELS.get(i).min()) {
                idx = i;
            }
        }
        return idx;
    }

    /** 距离下一级还差多少（0-100），满级返回 100 */
    private int levelProgress(int perf) {
        int idx = levelIndex(perf);
        if (idx >= LEVELS.size() - 1) {
            return 100;
        }
        int cur = LEVELS.get(idx).min();
        int next = LEVELS.get(idx + 1).min();
        return Math.round((perf - cur) * 100f / (next - cur));
    }

    private String moodLabel(int mood) {
        if (mood >= 90) return "乐开花";
        if (mood >= 70) return "还不错";
        if (mood >= 50) return "就那样吧";
        if (mood >= 30) return "有点累";
        if (mood >= 15) return "想请假";
        return "濒临辞职";
    }

    private int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }

    private int toInt(Object o, int fallback) {
        if (o instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (Exception e) {
            return fallback;
        }
    }

    private String writeJson(Object o) {
        try {
            return json.writeValueAsString(o);
        } catch (Exception e) {
            return "[]";
        }
    }

    /** 评级对应的 [标题, 吐槽] */
    private String[] gradeCopy(String roleCode, String grade) {
        return switch (roleCode) {
            case "krabs" -> switch (grade) {
                case "S" -> new String[]{"今天的账，一分不差",
                        "你数钱数到手指有点麻。这是你二十年里最接近幸福的一种感觉——虽然你不确定这算不算幸福。"};
                case "A" -> new String[]{"账面过得去",
                        "多赚了十来个币，少花了三个。你在本子上画了两个圈，第二个画得比第一个圆。"};
                case "B" -> new String[]{"平平淡淡的一天",
                        "没有惊喜，也没有惊吓。你把零钱按面额排好，看了一会儿，然后关灯。"};
                default -> new String[]{"今天亏了",
                        "你在账本上写了一行字，又划掉了，最后什么也没留下。第二天你把价目表重新抄了一遍。"};
            };
            case "sandy" -> switch (grade) {
                case "S" -> new String[]{"数据漂亮得不像真的",
                        "你检查了三遍，确认没有算错，然后做了第四遍——不是因为怀疑，是因为舍不得收工。"};
                case "A" -> new String[]{"有进展的一天",
                        "三组数据里有两组能用。你在记录本上写「方向是对的」，然后在旁边画了个箭头。"};
                case "B" -> new String[]{"实验本来就是这样",
                        "大部分时间在洗器材、贴标签、等结果。科学听起来很酷，其实多数时候是这样。"};
                default -> new String[]{"今天炸了两次",
                        "你把灭火器放回原位，扫掉地上的玻璃。明天还是得来——因为数据还差一组。"};
            };
            case "squidward" -> switch (grade) {
                case "S" -> new String[]{"反常的一天",
                        "你今天甚至没看几次钟。这很可疑，但你不想深究，怕一深究就没了。"};
                case "A" -> new String[]{"比预期好一点",
                        "至少今天没人在你面前唱完整的肉饼歌。只有两遍，勉强算安静。"};
                case "B" -> new String[]{"四千三百八十二分之一",
                        "又一个工作日过去了，和前四千多个一样。你记得的只有中间那杯咖啡。"};
                default -> new String[]{"今天谁也别跟我说话",
                        "你把围裙摔在柜台上，然后弯腰捡起来，叠好，放回了原位。"};
            };
            default -> switch (grade) {
                case "S" -> new String[]{"今天你是蟹堡王的传说",
                        "蟹老板今天第 3 次路过你的工位，什么也没说，但多看了两眼。这已经是他的极限表达了。"};
                case "A" -> new String[]{"又稳又香的一天",
                        "收工时围裙上有三块芝士，你管这叫勋章。明天它们会变成四块。"};
                case "B" -> new String[]{"普通但完整的一天",
                        "没有什么特别的，但每一个蟹黄堡都熟透了。这也挺不容易的。"};
                default -> new String[]{"今天……也算是上过班了",
                        "你决定明天一定不一样。你每天都这么说，而且每次都信。"};
            };
        };
    }
}
