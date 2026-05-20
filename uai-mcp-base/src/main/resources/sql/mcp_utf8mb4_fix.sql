-- Fix MySQL error 1366: Incorrect string value for emoji / 4-byte UTF-8 characters.
-- Run once against existing databases created before utf8mb4 was enforced.

ALTER DATABASE `mcp` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE `mcp`.`t_chat_message`
  CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  MODIFY COLUMN `content` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '消息的内容';
