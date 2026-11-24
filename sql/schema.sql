CREATE DATABASE IF NOT EXISTS college_parking CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE college_parking;

-- Empty schema for a new deployment; no demo rows are inserted.
CREATE TABLE IF NOT EXISTS parking_slots (
  slot_id INT NOT NULL AUTO_INCREMENT,
  slot_number VARCHAR(20) NOT NULL,
  slot_type ENUM('2-WHEELER','4-WHEELER') NOT NULL,
  user_category ENUM('STUDENT','FACULTY') NOT NULL,
  is_occupied TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (slot_id),
  UNIQUE KEY uq_parking_slots_number (slot_number),
  KEY ix_parking_slots_group (user_category, slot_type, is_occupied)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS vehicles (
  vehicle_id INT NOT NULL AUTO_INCREMENT,
  vehicle_number VARCHAR(20) NOT NULL,
  vehicle_type ENUM('2-WHEELER','4-WHEELER') NOT NULL,
  owner_category ENUM('STUDENT','FACULTY') NOT NULL,
  owner_name VARCHAR(100) NOT NULL,
  user_id INT NULL,
  PRIMARY KEY (vehicle_id),
  UNIQUE KEY uq_vehicles_number (vehicle_number),
  KEY ix_vehicles_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS parking_records (
  record_id INT NOT NULL AUTO_INCREMENT,
  vehicle_id INT NOT NULL,
  slot_id INT NOT NULL,
  entry_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  exit_time DATETIME NULL DEFAULT NULL,
  PRIMARY KEY (record_id),
  KEY ix_parking_records_vehicle (vehicle_id),
  KEY ix_parking_records_slot (slot_id),
  KEY ix_parking_records_exit (exit_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
