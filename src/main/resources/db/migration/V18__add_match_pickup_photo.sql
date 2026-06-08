ALTER TABLE matches
    ADD COLUMN pickup_photo_object_path TEXT,
    ADD COLUMN pickup_photo_content_type VARCHAR(100),
    ADD COLUMN pickup_photo_uploaded_at TIMESTAMP;
