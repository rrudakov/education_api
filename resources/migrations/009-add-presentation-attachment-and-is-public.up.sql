ALTER TABLE presentations ADD COLUMN attachment VARCHAR(1000);
--;;
ALTER TABLE presentations ADD COLUMN is_public BOOLEAN NOT NULL DEFAULT TRUE;
