-- ============================================================
-- 海绵宝宝登录注册系统数据库脚本
-- 数据库: spongebob  |  MySQL 8.0+
-- 使用方法: 在 IDEA 的 Database 面板或 MySQL 命令行中执行本文件
-- ============================================================

-- 如果数据库已存在先删除（演示用，生产环境请勿 DROP）
DROP DATABASE IF EXISTS spongebob;

CREATE DATABASE spongebob
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE spongebob;

-- ----------------------------
-- 用户表 t_user
-- ----------------------------
DROP TABLE IF EXISTS t_user;
CREATE TABLE t_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username    VARCHAR(30)  NOT NULL                COMMENT '用户名',
    password    VARCHAR(100) NOT NULL                COMMENT '密码（BCrypt 加密存储）',
    nickname    VARCHAR(30)           DEFAULT NULL    COMMENT '昵称',
    create_time DATETIME              DEFAULT NULL    COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL    COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

-- ----------------------------
-- 初始测试数据（账号 spongebob / patrick / squidward，密码均为 123456，已用 BCrypt 加密）
-- 哈希由 bcrypt.hashpw('123456', gensalt(10)) 生成，可直接用于登录验证
-- ----------------------------
INSERT INTO t_user (username, password, nickname, create_time, update_time)
VALUES ('spongebob', '$2b$10$bc1D4xL0YIRoTb8IZIUTd.lOSNSjYbz1TMfvDxKlINYVywBUjq2R.', '海绵宝宝', NOW(), NOW()),
       ('patrick',   '$2b$10$bc1D4xL0YIRoTb8IZIUTd.lOSNSjYbz1TMfvDxKlINYVywBUjq2R.', '派大星',   NOW(), NOW()),
       ('squidward', '$2b$10$bc1D4xL0YIRoTb8IZIUTd.lOSNSjYbz1TMfvDxKlINYVywBUjq2R.', '章鱼哥',   NOW(), NOW());
