-- V9: Add profile_picture column to customers table
ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS profile_picture VARCHAR(500);
