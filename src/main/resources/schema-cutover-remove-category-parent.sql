-- Remove legacy category hierarchy.
-- Safe to run multiple times on PostgreSQL.

DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'fksaok720gsu4u2wrgbk10b5n8d'
  ) THEN
    ALTER TABLE categories
      DROP CONSTRAINT fksaok720gsu4u2wrgbk10b5n8d;
  END IF;
END$$;

ALTER TABLE IF EXISTS categories
  DROP COLUMN IF EXISTS parent_id;
