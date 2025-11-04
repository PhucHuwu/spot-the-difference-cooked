CREATE DATABASE  IF NOT EXISTS `spotgame` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `spotgame`;
-- MySQL dump 10.13  Distrib 8.0.38, for Win64 (x86_64)
--
-- Host: localhost    Database: spotgame
-- ------------------------------------------------------
-- Server version	8.0.39

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `image_differences`
--

DROP TABLE IF EXISTS `image_differences`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `image_differences` (
  `id` int NOT NULL AUTO_INCREMENT,
  `set_id` int NOT NULL,
  `x` int NOT NULL,
  `y` int NOT NULL,
  `radius` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `set_id` (`set_id`),
  CONSTRAINT `image_differences_ibfk_1` FOREIGN KEY (`set_id`) REFERENCES `image_sets` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=57 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `image_differences`
--

LOCK TABLES `image_differences` WRITE;
/*!40000 ALTER TABLE `image_differences` DISABLE KEYS */;
INSERT INTO `image_differences` VALUES (31,5,73,26,32),(32,5,145,27,32),(33,5,96,99,32),(34,5,64,176,32),(35,5,273,177,32),(36,5,243,118,32),(37,5,239,64,32),(38,6,165,43,25),(39,6,2,168,25),(40,6,207,339,25),(41,6,63,38,25),(42,6,236,190,25),(43,6,153,176,25),(44,6,74,159,25),(45,6,114,246,25),(46,6,204,135,25),(47,7,988,53,110),(48,7,709,69,110),(49,7,609,480,110),(50,7,78,613,110),(51,7,963,421,110),(52,7,288,73,110),(53,7,436,101,110),(54,7,405,432,110),(55,7,518,321,110),(56,7,18,170,110);
/*!40000 ALTER TABLE `image_differences` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `image_sets`
--

DROP TABLE IF EXISTS `image_sets`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `image_sets` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `width` int NOT NULL,
  `height` int NOT NULL,
  `img_left_path` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `img_right_path` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `image_sets`
--

LOCK TABLES `image_sets` WRITE;
/*!40000 ALTER TABLE `image_sets` DISABLE KEYS */;
INSERT INTO `image_sets` VALUES (5,'skibidi',320,224,'1762063653457_left.jpg','1762063653457_right.jpg','2025-11-02 06:07:33'),(6,'set-1762099271457',249,375,'1762099271462_left.jpg','1762099271462_right.jpg','2025-11-02 16:01:11'),(7,'set-1762099872903',1104,670,'1762099872910_left.jpg','1762099872910_right.jpg','2025-11-02 16:11:13');
/*!40000 ALTER TABLE `image_sets` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `matches`
--

DROP TABLE IF EXISTS `matches`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `matches` (
  `id` int NOT NULL AUTO_INCREMENT,
  `player_a_id` int NOT NULL,
  `player_b_id` int NOT NULL,
  `score_a` int NOT NULL,
  `score_b` int NOT NULL,
  `result` enum('A','B','DRAW') COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `player_a_id` (`player_a_id`),
  KEY `player_b_id` (`player_b_id`),
  CONSTRAINT `matches_ibfk_1` FOREIGN KEY (`player_a_id`) REFERENCES `users` (`id`),
  CONSTRAINT `matches_ibfk_2` FOREIGN KEY (`player_b_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matches`
--

LOCK TABLES `matches` WRITE;
/*!40000 ALTER TABLE `matches` DISABLE KEYS */;
INSERT INTO `matches` VALUES (1,1,2,10,8,'A','2025-10-25 12:22:09'),(2,1,3,7,9,'B','2025-10-25 12:22:09'),(3,2,3,8,8,'DRAW','2025-10-25 12:22:09'),(4,3,4,10,5,'A','2025-10-25 12:22:09'),(5,4,5,6,7,'B','2025-10-25 12:22:09'),(6,5,1,9,9,'DRAW','2025-10-25 12:22:09'),(7,2,5,10,7,'A','2025-10-25 12:22:09'),(8,6,7,3,4,'B','2025-11-02 06:14:55'),(9,7,6,2,5,'B','2025-11-02 09:27:53'),(10,6,7,5,2,'A','2025-11-02 11:39:48'),(11,7,6,4,3,'A','2025-11-02 15:02:22'),(12,7,6,3,4,'B','2025-11-02 15:10:46'),(13,7,6,3,4,'B','2025-11-02 15:48:53'),(14,7,6,3,4,'B','2025-11-02 15:57:30'),(15,7,6,4,5,'B','2025-11-02 16:05:40');
/*!40000 ALTER TABLE `matches` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `total_points` int NOT NULL DEFAULT '0',
  `total_wins` int NOT NULL DEFAULT '0',
  `total_losses` int NOT NULL DEFAULT '0',
  `total_draws` int NOT NULL DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  KEY `idx_users_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'alice','ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f',120,3,1,0,'2025-10-25 12:22:09'),(2,'bob','ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f',90,2,2,0,'2025-10-25 12:22:09'),(3,'charlie','ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f',150,4,0,0,'2025-10-25 12:22:09'),(4,'david','ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f',60,1,3,0,'2025-10-25 12:22:09'),(5,'eva','ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f',100,2,1,1,'2025-10-25 12:22:09'),(6,'huy','123',133,7,3,1,'2025-10-25 12:22:09'),(7,'hung','123',125,3,7,1,'2025-10-25 12:22:09');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-11-02 23:13:43
