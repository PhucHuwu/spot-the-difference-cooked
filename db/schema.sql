-- MySQL schema for Spot The Difference
CREATE DATABASE IF NOT EXISTS spotgame CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE spotgame;

CREATE TABLE IF NOT EXISTS users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  total_points INT NOT NULL DEFAULT 0,
  total_wins INT NOT NULL DEFAULT 0,
  total_losses INT NOT NULL DEFAULT 0,
  total_draws INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS matches (
  id INT AUTO_INCREMENT PRIMARY KEY,
  player_a_id INT NOT NULL,
  player_b_id INT NOT NULL,
  score_a INT NOT NULL,
  score_b INT NOT NULL,
  result ENUM('A','B','DRAW') NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (player_a_id) REFERENCES users(id),
  FOREIGN KEY (player_b_id) REFERENCES users(id)
);

-- Image sets for game content
-- Store image file paths (relative to content.dir) instead of BLOBs
CREATE TABLE IF NOT EXISTS image_sets (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  width INT NOT NULL,
  height INT NOT NULL,
  img_left_path VARCHAR(255) NOT NULL,
  img_right_path VARCHAR(255) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS image_differences (
  id INT AUTO_INCREMENT PRIMARY KEY,
  set_id INT NOT NULL,
  x INT NOT NULL,
  y INT NOT NULL,
  radius INT NOT NULL,
  FOREIGN KEY (set_id) REFERENCES image_sets(id) ON DELETE CASCADE,
  INDEX idx_diffs_set (set_id)
);

-- Matchmaking queue for online matchmaking
CREATE TABLE IF NOT EXISTS matchmaking_queue (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  status ENUM('waiting','matched') NOT NULL DEFAULT 'waiting',
  joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE
);
