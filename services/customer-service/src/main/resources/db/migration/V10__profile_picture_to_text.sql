-- V10: Widen profile_picture column to TEXT to support Base64-encoded image data
-- VARCHAR(500) is too small for typical profile picture files
ALTER TABLE customers
    ALTER COLUMN profile_picture TYPE TEXT;
