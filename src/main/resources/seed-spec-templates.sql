-- Seed for normalized product schema (variant attributes + spec attributes)
-- Run after Hibernate creates schema-cutover tables.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================================
-- 1) Master spec attributes
-- ============================================================================
INSERT INTO spec_attributes (id, name, code, default_hint, sort_order, active, created_at, updated_at) VALUES
  (gen_random_uuid(), 'Màn hình', 'SCREEN', 'VD: 6.7 inch OLED, 120Hz', 0, true, NOW(), NOW()),
  (gen_random_uuid(), 'Chip', 'CHIP', 'VD: Snapdragon 8 Gen 3', 1, true, NOW(), NOW()),
  (gen_random_uuid(), 'GPU', 'GPU', 'VD: NVIDIA RTX 4060', 2, true, NOW(), NOW()),
  (gen_random_uuid(), 'RAM', 'RAM', 'VD: 8GB, 12GB, 16GB', 3, true, NOW(), NOW()),
  (gen_random_uuid(), 'Bộ nhớ', 'STORAGE', 'VD: 128GB, 256GB, 512GB', 4, true, NOW(), NOW()),
  (gen_random_uuid(), 'Pin', 'BATTERY', 'VD: 5000mAh, sạc nhanh 67W', 5, true, NOW(), NOW()),
  (gen_random_uuid(), 'Kết nối', 'CONNECTIVITY', 'VD: USB-C, Bluetooth 5.3, Wi-Fi 6', 6, true, NOW(), NOW()),
  (gen_random_uuid(), 'Layout', 'LAYOUT', 'VD: TKL, 75%, Full-size', 7, true, NOW(), NOW()),
  (gen_random_uuid(), 'Tính năng âm thanh', 'AUDIO_FEATURES', 'VD: Spatial Audio, Hi-Res', 8, true, NOW(), NOW()),
  (gen_random_uuid(), 'Chống ồn', 'NOISE_CANCELLING', 'VD: ANC chủ động', 9, true, NOW(), NOW()),
  (gen_random_uuid(), 'Công suất', 'POWER', 'VD: 30W RMS', 10, true, NOW(), NOW()),
  (gen_random_uuid(), 'Cảm biến', 'SENSOR', 'VD: PixArt 3395', 11, true, NOW(), NOW()),
  (gen_random_uuid(), 'DPI', 'DPI', 'VD: 26000 DPI', 12, true, NOW(), NOW()),
  (gen_random_uuid(), 'Trọng lượng', 'WEIGHT', 'VD: 63g', 13, true, NOW(), NOW()),
  (gen_random_uuid(), 'Độ phân giải', 'RESOLUTION', 'VD: 2560 x 1440', 14, true, NOW(), NOW()),
  (gen_random_uuid(), 'Tần số quét', 'REFRESH_RATE', 'VD: 165Hz', 15, true, NOW(), NOW()),
  (gen_random_uuid(), 'Tấm nền', 'PANEL', 'VD: IPS, VA, OLED', 16, true, NOW(), NOW())
ON CONFLICT (code) DO UPDATE
SET
  name = EXCLUDED.name,
  default_hint = EXCLUDED.default_hint,
  sort_order = EXCLUDED.sort_order,
  active = EXCLUDED.active,
  updated_at = NOW();

-- ============================================================================
-- 2) Master variant attributes
-- ============================================================================
INSERT INTO variant_attributes (id, name, code, active, created_at, updated_at) VALUES
  (gen_random_uuid(), 'Màu', 'COLOR', true, NOW(), NOW()),
  (gen_random_uuid(), 'RAM', 'RAM', true, NOW(), NOW()),
  (gen_random_uuid(), 'Bộ nhớ', 'STORAGE', true, NOW(), NOW()),
  (gen_random_uuid(), 'Switch', 'SWITCH', true, NOW(), NOW())
ON CONFLICT (code) DO UPDATE
SET
  name = EXCLUDED.name,
  active = EXCLUDED.active,
  updated_at = NOW();

-- Options for COLOR
INSERT INTO variant_attribute_options (id, variant_attribute_id, label, code, sort_order, active, created_at, updated_at)
SELECT gen_random_uuid(), va.id, opt.label, opt.code, opt.sort_order, true, NOW(), NOW()
FROM variant_attributes va
JOIN (VALUES
  ('Đen', 'BLACK', 0),
  ('Trắng', 'WHITE', 1),
  ('Xanh', 'BLUE', 2),
  ('Hồng', 'PINK', 3),
  ('Bạc', 'SILVER', 4)
) AS opt(label, code, sort_order) ON true
WHERE va.code = 'COLOR'
ON CONFLICT (variant_attribute_id, code) DO UPDATE
SET
  label = EXCLUDED.label,
  sort_order = EXCLUDED.sort_order,
  active = EXCLUDED.active,
  updated_at = NOW();

-- Options for RAM
INSERT INTO variant_attribute_options (id, variant_attribute_id, label, code, sort_order, active, created_at, updated_at)
SELECT gen_random_uuid(), va.id, opt.label, opt.code, opt.sort_order, true, NOW(), NOW()
FROM variant_attributes va
JOIN (VALUES
  ('4GB', '4GB', 0),
  ('6GB', '6GB', 1),
  ('8GB', '8GB', 2),
  ('12GB', '12GB', 3),
  ('16GB', '16GB', 4)
) AS opt(label, code, sort_order) ON true
WHERE va.code = 'RAM'
ON CONFLICT (variant_attribute_id, code) DO UPDATE
SET
  label = EXCLUDED.label,
  sort_order = EXCLUDED.sort_order,
  active = EXCLUDED.active,
  updated_at = NOW();

-- Options for STORAGE
INSERT INTO variant_attribute_options (id, variant_attribute_id, label, code, sort_order, active, created_at, updated_at)
SELECT gen_random_uuid(), va.id, opt.label, opt.code, opt.sort_order, true, NOW(), NOW()
FROM variant_attributes va
JOIN (VALUES
  ('64GB', '64GB', 0),
  ('128GB', '128GB', 1),
  ('256GB', '256GB', 2),
  ('512GB', '512GB', 3),
  ('1TB', '1TB', 4)
) AS opt(label, code, sort_order) ON true
WHERE va.code = 'STORAGE'
ON CONFLICT (variant_attribute_id, code) DO UPDATE
SET
  label = EXCLUDED.label,
  sort_order = EXCLUDED.sort_order,
  active = EXCLUDED.active,
  updated_at = NOW();

-- Options for SWITCH
INSERT INTO variant_attribute_options (id, variant_attribute_id, label, code, sort_order, active, created_at, updated_at)
SELECT gen_random_uuid(), va.id, opt.label, opt.code, opt.sort_order, true, NOW(), NOW()
FROM variant_attributes va
JOIN (VALUES
  ('Red', 'RED', 0),
  ('Brown', 'BROWN', 1),
  ('Blue', 'BLUE', 2)
) AS opt(label, code, sort_order) ON true
WHERE va.code = 'SWITCH'
ON CONFLICT (variant_attribute_id, code) DO UPDATE
SET
  label = EXCLUDED.label,
  sort_order = EXCLUDED.sort_order,
  active = EXCLUDED.active,
  updated_at = NOW();

-- ============================================================================
-- 3) Category variant schema bindings
-- ============================================================================

-- Điện thoại
INSERT INTO category_variant_attributes (id, category_id, variant_attribute_id, sort_order, created_at, updated_at)
SELECT gen_random_uuid(), c.id, va.id, map.sort_order, NOW(), NOW()
FROM categories c
JOIN (VALUES
  ('COLOR', 0),
  ('RAM', 1),
  ('STORAGE', 2)
) AS map(code, sort_order) ON true
JOIN variant_attributes va ON va.code = map.code
WHERE c.slug = 'dien-thoai'
ON CONFLICT (category_id, variant_attribute_id) DO UPDATE
SET sort_order = EXCLUDED.sort_order, updated_at = NOW();

-- Laptop
INSERT INTO category_variant_attributes (id, category_id, variant_attribute_id, sort_order, created_at, updated_at)
SELECT gen_random_uuid(), c.id, va.id, map.sort_order, NOW(), NOW()
FROM categories c
JOIN (VALUES
  ('COLOR', 0),
  ('RAM', 1),
  ('STORAGE', 2)
) AS map(code, sort_order) ON true
JOIN variant_attributes va ON va.code = map.code
WHERE c.slug = 'laptop'
ON CONFLICT (category_id, variant_attribute_id) DO UPDATE
SET sort_order = EXCLUDED.sort_order, updated_at = NOW();

-- Máy tính bảng
INSERT INTO category_variant_attributes (id, category_id, variant_attribute_id, sort_order, created_at, updated_at)
SELECT gen_random_uuid(), c.id, va.id, map.sort_order, NOW(), NOW()
FROM categories c
JOIN (VALUES
  ('COLOR', 0),
  ('RAM', 1),
  ('STORAGE', 2)
) AS map(code, sort_order) ON true
JOIN variant_attributes va ON va.code = map.code
WHERE c.slug = 'may-tinh-bang'
ON CONFLICT (category_id, variant_attribute_id) DO UPDATE
SET sort_order = EXCLUDED.sort_order, updated_at = NOW();

-- Tai nghe
INSERT INTO category_variant_attributes (id, category_id, variant_attribute_id, sort_order, created_at, updated_at)
SELECT gen_random_uuid(), c.id, va.id, 0, NOW(), NOW()
FROM categories c
JOIN variant_attributes va ON va.code = 'COLOR'
WHERE c.slug = 'tai-nghe'
ON CONFLICT (category_id, variant_attribute_id) DO UPDATE
SET sort_order = 0, updated_at = NOW();

-- Loa
INSERT INTO category_variant_attributes (id, category_id, variant_attribute_id, sort_order, created_at, updated_at)
SELECT gen_random_uuid(), c.id, va.id, 0, NOW(), NOW()
FROM categories c
JOIN variant_attributes va ON va.code = 'COLOR'
WHERE c.slug = 'loa'
ON CONFLICT (category_id, variant_attribute_id) DO UPDATE
SET sort_order = 0, updated_at = NOW();

-- Bàn phím
INSERT INTO category_variant_attributes (id, category_id, variant_attribute_id, sort_order, created_at, updated_at)
SELECT gen_random_uuid(), c.id, va.id, map.sort_order, NOW(), NOW()
FROM categories c
JOIN (VALUES
  ('COLOR', 0),
  ('SWITCH', 1)
) AS map(code, sort_order) ON true
JOIN variant_attributes va ON va.code = map.code
WHERE c.slug = 'ban-phim'
ON CONFLICT (category_id, variant_attribute_id) DO UPDATE
SET sort_order = EXCLUDED.sort_order, updated_at = NOW();

-- Chuột
INSERT INTO category_variant_attributes (id, category_id, variant_attribute_id, sort_order, created_at, updated_at)
SELECT gen_random_uuid(), c.id, va.id, 0, NOW(), NOW()
FROM categories c
JOIN variant_attributes va ON va.code = 'COLOR'
WHERE c.slug = 'chuot'
ON CONFLICT (category_id, variant_attribute_id) DO UPDATE
SET sort_order = 0, updated_at = NOW();

-- ============================================================================
-- 4) Category spec schema bindings
-- ============================================================================

-- Điện thoại
INSERT INTO category_spec_attributes (id, category_id, spec_attribute_id, custom_hint, sort_order, created_at, updated_at)
SELECT gen_random_uuid(), c.id, sa.id, NULL, map.sort_order, NOW(), NOW()
FROM categories c
JOIN (VALUES
  ('SCREEN', 0),
  ('CHIP', 1),
  ('RAM', 2),
  ('STORAGE', 3),
  ('BATTERY', 4),
  ('CONNECTIVITY', 5)
) AS map(code, sort_order) ON true
JOIN spec_attributes sa ON sa.code = map.code
WHERE c.slug = 'dien-thoai'
ON CONFLICT (category_id, spec_attribute_id) DO UPDATE
SET sort_order = EXCLUDED.sort_order, updated_at = NOW();

-- Laptop
INSERT INTO category_spec_attributes (id, category_id, spec_attribute_id, custom_hint, sort_order, created_at, updated_at)
SELECT gen_random_uuid(), c.id, sa.id, NULL, map.sort_order, NOW(), NOW()
FROM categories c
JOIN (VALUES
  ('SCREEN', 0),
  ('CHIP', 1),
  ('GPU', 2),
  ('RAM', 3),
  ('STORAGE', 4),
  ('BATTERY', 5),
  ('CONNECTIVITY', 6)
) AS map(code, sort_order) ON true
JOIN spec_attributes sa ON sa.code = map.code
WHERE c.slug = 'laptop'
ON CONFLICT (category_id, spec_attribute_id) DO UPDATE
SET sort_order = EXCLUDED.sort_order, updated_at = NOW();

-- Máy tính bảng
INSERT INTO category_spec_attributes (id, category_id, spec_attribute_id, custom_hint, sort_order, created_at, updated_at)
SELECT gen_random_uuid(), c.id, sa.id, NULL, map.sort_order, NOW(), NOW()
FROM categories c
JOIN (VALUES
  ('SCREEN', 0),
  ('CHIP', 1),
  ('RAM', 2),
  ('STORAGE', 3),
  ('BATTERY', 4),
  ('CONNECTIVITY', 5)
) AS map(code, sort_order) ON true
JOIN spec_attributes sa ON sa.code = map.code
WHERE c.slug = 'may-tinh-bang'
ON CONFLICT (category_id, spec_attribute_id) DO UPDATE
SET sort_order = EXCLUDED.sort_order, updated_at = NOW();

-- Tai nghe
INSERT INTO category_spec_attributes (id, category_id, spec_attribute_id, custom_hint, sort_order, created_at, updated_at)
SELECT gen_random_uuid(), c.id, sa.id, NULL, map.sort_order, NOW(), NOW()
FROM categories c
JOIN (VALUES
  ('CONNECTIVITY', 0),
  ('BATTERY', 1),
  ('AUDIO_FEATURES', 2),
  ('NOISE_CANCELLING', 3),
  ('WEIGHT', 4)
) AS map(code, sort_order) ON true
JOIN spec_attributes sa ON sa.code = map.code
WHERE c.slug = 'tai-nghe'
ON CONFLICT (category_id, spec_attribute_id) DO UPDATE
SET sort_order = EXCLUDED.sort_order, updated_at = NOW();

-- Loa
INSERT INTO category_spec_attributes (id, category_id, spec_attribute_id, custom_hint, sort_order, created_at, updated_at)
SELECT gen_random_uuid(), c.id, sa.id, NULL, map.sort_order, NOW(), NOW()
FROM categories c
JOIN (VALUES
  ('CONNECTIVITY', 0),
  ('BATTERY', 1),
  ('POWER', 2),
  ('AUDIO_FEATURES', 3),
  ('WEIGHT', 4)
) AS map(code, sort_order) ON true
JOIN spec_attributes sa ON sa.code = map.code
WHERE c.slug = 'loa'
ON CONFLICT (category_id, spec_attribute_id) DO UPDATE
SET sort_order = EXCLUDED.sort_order, updated_at = NOW();

-- Bàn phím
INSERT INTO category_spec_attributes (id, category_id, spec_attribute_id, custom_hint, sort_order, created_at, updated_at)
SELECT gen_random_uuid(), c.id, sa.id, NULL, map.sort_order, NOW(), NOW()
FROM categories c
JOIN (VALUES
  ('CONNECTIVITY', 0),
  ('LAYOUT', 1),
  ('BATTERY', 2),
  ('WEIGHT', 3)
) AS map(code, sort_order) ON true
JOIN spec_attributes sa ON sa.code = map.code
WHERE c.slug = 'ban-phim'
ON CONFLICT (category_id, spec_attribute_id) DO UPDATE
SET sort_order = EXCLUDED.sort_order, updated_at = NOW();

-- Chuột
INSERT INTO category_spec_attributes (id, category_id, spec_attribute_id, custom_hint, sort_order, created_at, updated_at)
SELECT gen_random_uuid(), c.id, sa.id, NULL, map.sort_order, NOW(), NOW()
FROM categories c
JOIN (VALUES
  ('CONNECTIVITY', 0),
  ('SENSOR', 1),
  ('DPI', 2),
  ('WEIGHT', 3),
  ('BATTERY', 4)
) AS map(code, sort_order) ON true
JOIN spec_attributes sa ON sa.code = map.code
WHERE c.slug = 'chuot'
ON CONFLICT (category_id, spec_attribute_id) DO UPDATE
SET sort_order = EXCLUDED.sort_order, updated_at = NOW();

-- Màn hình
INSERT INTO category_spec_attributes (id, category_id, spec_attribute_id, custom_hint, sort_order, created_at, updated_at)
SELECT gen_random_uuid(), c.id, sa.id, NULL, map.sort_order, NOW(), NOW()
FROM categories c
JOIN (VALUES
  ('SCREEN', 0),
  ('RESOLUTION', 1),
  ('REFRESH_RATE', 2),
  ('PANEL', 3),
  ('CONNECTIVITY', 4),
  ('POWER', 5)
) AS map(code, sort_order) ON true
JOIN spec_attributes sa ON sa.code = map.code
WHERE c.slug = 'man-hinh'
ON CONFLICT (category_id, spec_attribute_id) DO UPDATE
SET sort_order = EXCLUDED.sort_order, updated_at = NOW();
