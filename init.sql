-- 草花互动抽奖系统 - 数据库初始化脚本
-- 适用 MySQL 8.0+

CREATE DATABASE IF NOT EXISTS RaffleDrawing DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE RaffleDrawing;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id VARCHAR(30) NOT NULL UNIQUE,
    real_name VARCHAR(50) NULL,
    admin BOOLEAN NOT NULL DEFAULT FALSE,
    draw_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 奖品表（仅模板信息，不存数量和概率）
CREATE TABLE IF NOT EXISTS prizes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    image_base64 LONGTEXT,
    display_order INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 抽奖活动表
CREATE TABLE IF NOT EXISTS raffle_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200),
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户-活动抽奖次数表
CREATE TABLE IF NOT EXISTS user_event_draw_counts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    draw_count INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_event (user_id, event_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (event_id) REFERENCES raffle_events(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 活动-奖品关联表（每个活动中奖品的数量和概率）
CREATE TABLE IF NOT EXISTS lottery_event_prizes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    prize_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    remaining INT NOT NULL DEFAULT 0,
    probability DOUBLE NOT NULL DEFAULT 0.0,
    FOREIGN KEY (event_id) REFERENCES raffle_events(id) ON DELETE CASCADE,
    FOREIGN KEY (prize_id) REFERENCES prizes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 中奖记录表
CREATE TABLE IF NOT EXISTS raffle_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    prize_id BIGINT NOT NULL,
    event_id BIGINT,
    raffle_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (prize_id) REFERENCES prizes(id),
    FOREIGN KEY (event_id) REFERENCES raffle_events(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 默认管理员账号
INSERT IGNORE INTO users (account_id, real_name, admin) VALUES ('chfz-00000000', '管理员', TRUE);
