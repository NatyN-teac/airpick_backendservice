ALTER TABLE offer_requests DROP COLUMN IF EXISTS src_airport_id;
ALTER TABLE offer_requests DROP COLUMN IF EXISTS dest_airport_id;
ALTER TABLE offer_requests ADD COLUMN source_city VARCHAR(100) NOT NULL DEFAULT '';
