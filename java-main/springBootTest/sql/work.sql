-- ============================================================
--  比奇堡 · 打工日常 & 小游戏
--  依赖：已有 spongebob 库、t_user 表
--  执行：mysql --default-character-set=utf8mb4 -uroot -p < work.sql
-- ============================================================

USE spongebob;

-- ------------------------------------------------------------
-- 打工档案：一个用户在「海绵宝宝」「章鱼哥」两个身份下各有一份
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_work_profile (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id         BIGINT       NOT NULL COMMENT '用户 ID',
    role_code       VARCHAR(32)  NOT NULL COMMENT '角色：spongebob / squidward',
    coins           INT          NOT NULL DEFAULT 0   COMMENT '攒下的蟹堡币',
    mood            INT          NOT NULL DEFAULT 80  COMMENT '心情 0-100',
    performance     INT          NOT NULL DEFAULT 0   COMMENT '累计绩效',
    shift_count     INT          NOT NULL DEFAULT 0   COMMENT '累计上班次数',
    streak          INT          NOT NULL DEFAULT 0   COMMENT '连续打卡天数',
    last_shift_date DATE         NULL     DEFAULT NULL COMMENT '最后一次打卡日期',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '打工档案';

-- ------------------------------------------------------------
-- 打卡记录：每上完一次班写一条，供前端画时间轴
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_work_shift (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id       BIGINT       NOT NULL COMMENT '用户 ID',
    role_code     VARCHAR(32)  NOT NULL COMMENT '角色',
    shift_date    DATE         NOT NULL COMMENT '所属日期',
    coins_gain    INT          NOT NULL DEFAULT 0 COMMENT '本次赚的蟹堡币',
    mood_delta    INT          NOT NULL DEFAULT 0 COMMENT '心情变化',
    perform_delta INT          NOT NULL DEFAULT 0 COMMENT '绩效变化',
    grade         VARCHAR(4)   NOT NULL DEFAULT 'B' COMMENT '班次评级 S/A/B/C',
    summary       VARCHAR(255) NOT NULL DEFAULT '' COMMENT '一句话总结',
    timeline      TEXT         NULL COMMENT '过程回放（JSON 字符串）',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_role_time (user_id, role_code, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '打工打卡记录';

-- ------------------------------------------------------------
-- 小游戏成绩：每个用户每个游戏一条，存最好成绩
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_game_score (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT      NOT NULL COMMENT '用户 ID',
    game_code   VARCHAR(32) NOT NULL COMMENT '游戏标识：burger',
    best_score  INT         NOT NULL DEFAULT 0 COMMENT '最高分',
    best_combo  INT         NOT NULL DEFAULT 0 COMMENT '最高连击',
    burgers     INT         NOT NULL DEFAULT 0 COMMENT '单局最多出餐数',
    plays       INT         NOT NULL DEFAULT 0 COMMENT '游玩次数',
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_game (user_id, game_code),
    KEY idx_game_score (game_code, best_score)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '小游戏成绩';
