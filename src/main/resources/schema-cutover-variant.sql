-- Cut-over to normalized variant/spec schema.
-- Safe to run multiple times on PostgreSQL.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================================
-- 1) Clean legacy columns / tables
-- ============================================================================
ALTER TABLE IF EXISTS products DROP COLUMN IF EXISTS specs_json;
ALTER TABLE IF EXISTS product_variants DROP COLUMN IF EXISTS color;
ALTER TABLE IF EXISTS product_variants DROP COLUMN IF EXISTS capacity;
ALTER TABLE IF EXISTS product_variants DROP COLUMN IF EXISTS storage_capacity;
DROP TABLE IF EXISTS category_spec_mappings;

-- ============================================================================
-- 2) Ensure product/product_variant columns for new model
-- ============================================================================
ALTER TABLE IF EXISTS products
  ADD COLUMN IF NOT EXISTS product_code VARCHAR(12);

UPDATE products
SET product_code = COALESCE(
  product_code,
  'PRD' || UPPER(SUBSTRING(REPLACE(id::text, '-', '') FROM 1 FOR 6))
)
WHERE product_code IS NULL;

ALTER TABLE IF EXISTS products
  ALTER COLUMN product_code SET NOT NULL;

ALTER TABLE IF EXISTS products
  ALTER COLUMN product_code TYPE VARCHAR(12);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'uk_products_product_code'
  ) THEN
    ALTER TABLE products
      ADD CONSTRAINT uk_products_product_code UNIQUE (product_code);
  END IF;
END$$;

ALTER TABLE IF EXISTS product_variants
  ADD COLUMN IF NOT EXISTS variant_signature VARCHAR(500);

ALTER TABLE IF EXISTS product_variants
  ADD COLUMN IF NOT EXISTS display_name VARCHAR(255);

ALTER TABLE IF EXISTS product_variants
  ADD COLUMN IF NOT EXISTS variant_name VARCHAR(255);

UPDATE product_variants
SET display_name = COALESCE(display_name, variant_name, 'Mặc định')
WHERE display_name IS NULL;

ALTER TABLE IF EXISTS product_variants
  ALTER COLUMN display_name SET NOT NULL;

UPDATE product_variants
SET variant_signature = COALESCE(
  variant_signature,
  'LEGACY-' || SUBSTRING(REPLACE(id::text, '-', '') FROM 1 FOR 16)
)
WHERE variant_signature IS NULL;

ALTER TABLE IF EXISTS product_variants
  ALTER COLUMN variant_signature SET NOT NULL;

ALTER TABLE IF EXISTS product_variants DROP COLUMN IF EXISTS variant_name;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'uk_product_variants_product_signature'
  ) THEN
    ALTER TABLE product_variants
      ADD CONSTRAINT uk_product_variants_product_signature
      UNIQUE (product_id, variant_signature);
  END IF;
END$$;

-- ============================================================================
-- 3) Extend spec_attributes for reusable schema config
-- ============================================================================
ALTER TABLE IF EXISTS spec_attributes
  ADD COLUMN IF NOT EXISTS code VARCHAR(80),
  ADD COLUMN IF NOT EXISTS sort_order INTEGER DEFAULT 0,
  ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;

UPDATE spec_attributes
SET
  code = COALESCE(
    code,
    UPPER(REGEXP_REPLACE(COALESCE(name, 'SPEC'), '[^A-Za-z0-9]+', '_', 'g'))
  ),
  sort_order = COALESCE(sort_order, 0),
  active = COALESCE(active, TRUE);

ALTER TABLE IF EXISTS spec_attributes
  ALTER COLUMN code SET NOT NULL,
  ALTER COLUMN sort_order SET NOT NULL,
  ALTER COLUMN active SET NOT NULL;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'uk_spec_attributes_code'
  ) THEN
    ALTER TABLE spec_attributes
      ADD CONSTRAINT uk_spec_attributes_code UNIQUE (code);
  END IF;
END$$;

-- ============================================================================
-- 4) New normalized schema tables
-- ============================================================================
CREATE TABLE IF NOT EXISTS variant_attributes (
  id UUID PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  code VARCHAR(80) NOT NULL UNIQUE,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL,
  created_by VARCHAR(255),
  updated_at TIMESTAMP,
  updated_by VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS variant_attribute_options (
  id UUID PRIMARY KEY,
  variant_attribute_id UUID NOT NULL REFERENCES variant_attributes(id),
  label VARCHAR(120) NOT NULL,
  code VARCHAR(80) NOT NULL,
  sort_order INTEGER NOT NULL DEFAULT 0,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL,
  created_by VARCHAR(255),
  updated_at TIMESTAMP,
  updated_by VARCHAR(255),
  CONSTRAINT uk_variant_attribute_option UNIQUE (variant_attribute_id, code)
);

CREATE TABLE IF NOT EXISTS category_variant_attributes (
  id UUID PRIMARY KEY,
  category_id UUID NOT NULL REFERENCES categories(id),
  variant_attribute_id UUID NOT NULL REFERENCES variant_attributes(id),
  sort_order INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL,
  created_by VARCHAR(255),
  updated_at TIMESTAMP,
  updated_by VARCHAR(255),
  CONSTRAINT uk_category_variant_attribute UNIQUE (category_id, variant_attribute_id)
);

CREATE TABLE IF NOT EXISTS category_spec_attributes (
  id UUID PRIMARY KEY,
  category_id UUID NOT NULL REFERENCES categories(id),
  spec_attribute_id UUID NOT NULL REFERENCES spec_attributes(id),
  custom_hint VARCHAR(255),
  sort_order INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL,
  created_by VARCHAR(255),
  updated_at TIMESTAMP,
  updated_by VARCHAR(255),
  CONSTRAINT uk_category_spec_attribute UNIQUE (category_id, spec_attribute_id)
);

CREATE TABLE IF NOT EXISTS product_variant_attribute_values (
  id UUID PRIMARY KEY,
  product_variant_id UUID NOT NULL REFERENCES product_variants(id),
  variant_attribute_id UUID NOT NULL REFERENCES variant_attributes(id),
  option_id UUID NOT NULL REFERENCES variant_attribute_options(id),
  created_at TIMESTAMP NOT NULL,
  created_by VARCHAR(255),
  updated_at TIMESTAMP,
  updated_by VARCHAR(255),
  CONSTRAINT uk_product_variant_attr_value UNIQUE (product_variant_id, variant_attribute_id)
);

CREATE TABLE IF NOT EXISTS product_spec_values (
  id UUID PRIMARY KEY,
  product_id UUID NOT NULL REFERENCES products(id),
  spec_attribute_id UUID NOT NULL REFERENCES spec_attributes(id),
  value_text TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  created_by VARCHAR(255),
  updated_at TIMESTAMP,
  updated_by VARCHAR(255),
  CONSTRAINT uk_product_spec_value UNIQUE (product_id, spec_attribute_id)
);

CREATE INDEX IF NOT EXISTS idx_cva_category ON category_variant_attributes(category_id);
CREATE INDEX IF NOT EXISTS idx_csa_category ON category_spec_attributes(category_id);
CREATE INDEX IF NOT EXISTS idx_vaov_variant ON product_variant_attribute_values(product_variant_id);
CREATE INDEX IF NOT EXISTS idx_psv_product ON product_spec_values(product_id);
