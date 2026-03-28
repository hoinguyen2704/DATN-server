-- Seed spec templates (many-to-many design)
-- Chạy SAU KHI Hibernate tạo bảng spec_attributes + category_spec_mappings
-- Dùng pgAdmin / DBeaver / IntelliJ Database Tool

-- Bước 1: Tạo master spec attributes (dùng chung)
INSERT INTO spec_attributes (id, name, default_hint, created_at, updated_at) VALUES
  (gen_random_uuid(), 'Màn hình',            'VD: 6.7 inch OLED, 120Hz',              NOW(), NOW()),
  (gen_random_uuid(), 'Hệ điều hành',       'VD: Android 14, iOS 17',                NOW(), NOW()),
  (gen_random_uuid(), 'Camera sau',           'VD: 48MP + 12MP + 12MP',                NOW(), NOW()),
  (gen_random_uuid(), 'Camera trước',        'VD: 12MP, TrueDepth',                   NOW(), NOW()),
  (gen_random_uuid(), 'Chip',                 'VD: Apple A17 Pro, Snapdragon 8 Gen 3', NOW(), NOW()),
  (gen_random_uuid(), 'RAM',                  'VD: 8GB, 16GB',                         NOW(), NOW()),
  (gen_random_uuid(), 'Dung lượng lưu trữ', 'VD: 128GB, 256GB, 512GB',               NOW(), NOW()),
  (gen_random_uuid(), 'Pin, Sạc',            'VD: 5000mAh, Sạc nhanh 67W',           NOW(), NOW()),
  (gen_random_uuid(), 'CPU',                  'VD: Intel Core i7-13700H',              NOW(), NOW()),
  (gen_random_uuid(), 'Ổ cứng',             'VD: SSD 512GB NVMe',                     NOW(), NOW()),
  (gen_random_uuid(), 'Card đồ họa',        'VD: NVIDIA RTX 4060 6GB',               NOW(), NOW()),
  (gen_random_uuid(), 'Cổng kết nối',       'VD: USB-C, HDMI, Jack 3.5mm',           NOW(), NOW()),
  (gen_random_uuid(), 'Trọng lượng',        'VD: 150g, 1.8kg',                        NOW(), NOW()),
  (gen_random_uuid(), 'Pin',                  'VD: 10 tiếng, 72Wh',                   NOW(), NOW()),
  (gen_random_uuid(), 'Phụ kiện',            'VD: Bút S Pen, Bao da',                NOW(), NOW()),
  (gen_random_uuid(), 'Kiểu dáng',          'VD: In-ear, Over-ear, True Wireless',   NOW(), NOW()),
  (gen_random_uuid(), 'Thời gian sử dụng', 'VD: 8 tiếng, 30 tiếng (với hộp sạc)', NOW(), NOW()),
  (gen_random_uuid(), 'Thời gian sạc',      'VD: 1.5 tiếng, Sạc nhanh 10 phút',    NOW(), NOW()),
  (gen_random_uuid(), 'Cổng sạc',            'VD: USB-C, Lightning',                  NOW(), NOW()),
  (gen_random_uuid(), 'Chống ồn',            'VD: Chống ồn chủ động ANC',            NOW(), NOW()),
  (gen_random_uuid(), 'Mic thoại',           'VD: 3 mic, Lọc ồn AI',                 NOW(), NOW()),
  (gen_random_uuid(), 'Tương thích',         'VD: iOS, Android, Windows',              NOW(), NOW()),
  (gen_random_uuid(), 'Công suất',           'VD: 20W, 50W RMS',                      NOW(), NOW()),
  (gen_random_uuid(), 'Kết nối',             'VD: Bluetooth 5.3, USB, 2.4GHz',        NOW(), NOW()),
  (gen_random_uuid(), 'Thời lượng pin',     'VD: 12 tiếng, 24 tiếng',               NOW(), NOW()),
  (gen_random_uuid(), 'Phím điều khiển',    'VD: Cảm ứng, Nút vật lý',             NOW(), NOW()),
  (gen_random_uuid(), 'Thương hiệu',        'VD: JBL, Sony, Marshall',               NOW(), NOW()),
  (gen_random_uuid(), 'Kiểu bàn phím',     'VD: Cơ, Membrance, TKL, Full-size',    NOW(), NOW()),
  (gen_random_uuid(), 'Loại Switch',         'VD: Cherry MX Red, Gateron Brown',      NOW(), NOW()),
  (gen_random_uuid(), 'Đèn LED',            'VD: RGB 16 triệu màu, Đơn sắc',       NOW(), NOW()),
  (gen_random_uuid(), 'Kích thước',         'VD: 27 inch, 34 inch',                   NOW(), NOW()),
  (gen_random_uuid(), 'Độ phân giải (DPI)', 'VD: 25600 DPI, Điều chỉnh được',       NOW(), NOW()),
  (gen_random_uuid(), 'Số nút bấm',         'VD: 6 nút, 11 nút',                     NOW(), NOW()),
  (gen_random_uuid(), 'Loại pin',            'VD: Pin sạc, 1x AA',                    NOW(), NOW()),
  (gen_random_uuid(), 'Độ phân giải',       'VD: 2560x1440 (2K QHD)',                 NOW(), NOW()),
  (gen_random_uuid(), 'Tần số quét',        'VD: 144Hz, 165Hz, 240Hz',               NOW(), NOW()),
  (gen_random_uuid(), 'Độ sáng',            'VD: 350 nits, HDR 400',                  NOW(), NOW()),
  (gen_random_uuid(), 'Độ tương phản',     'VD: 1000:1, 3000:1',                     NOW(), NOW()),
  (gen_random_uuid(), 'Tấm nền',            'VD: IPS, VA, OLED',                      NOW(), NOW()),
  (gen_random_uuid(), 'Tương thích VESA',   'VD: 100x100mm',                          NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- Bước 2: Mapping cho từng danh mục (dùng chung spec_attributes)
-- Điện thoại
INSERT INTO category_spec_mappings (id, category_id, spec_attribute_id, custom_hint, sort_order, created_at, updated_at)
SELECT gen_random_uuid(), c.id, s.id, NULL, t.ord, NOW(), NOW()
FROM categories c, (VALUES
  ('Màn hình', 0), ('Hệ điều hành', 1), ('Camera sau', 2), ('Camera trước', 3),
  ('Chip', 4), ('RAM', 5), ('Dung lượng lưu trữ', 6), ('Pin, Sạc', 7)
) AS t(spec_name, ord)
JOIN spec_attributes s ON s.name = t.spec_name
WHERE c.name ILIKE '%Điện thoại%'
ON CONFLICT DO NOTHING;

-- Laptop
INSERT INTO category_spec_mappings (id, category_id, spec_attribute_id, custom_hint, sort_order, created_at, updated_at)
SELECT gen_random_uuid(), c.id, s.id, NULL, t.ord, NOW(), NOW()
FROM categories c, (VALUES
  ('CPU', 0), ('RAM', 1), ('Ổ cứng', 2), ('Màn hình', 3),
  ('Card đồ họa', 4), ('Cổng kết nối', 5), ('Trọng lượng', 6), ('Pin', 7)
) AS t(spec_name, ord)
JOIN spec_attributes s ON s.name = t.spec_name
WHERE c.name ILIKE '%Laptop%'
ON CONFLICT DO NOTHING;

-- Máy tính bảng
INSERT INTO category_spec_mappings (id, category_id, spec_attribute_id, custom_hint, sort_order, created_at, updated_at)
SELECT gen_random_uuid(), c.id, s.id, NULL, t.ord, NOW(), NOW()
FROM categories c, (VALUES
  ('Màn hình', 0), ('Hệ điều hành', 1), ('Chip', 2), ('RAM', 3),
  ('Dung lượng lưu trữ', 4), ('Phụ kiện', 5), ('Pin, Sạc', 6)
) AS t(spec_name, ord)
JOIN spec_attributes s ON s.name = t.spec_name
WHERE c.name ILIKE '%Máy tính bảng%'
ON CONFLICT DO NOTHING;

-- Tai nghe
INSERT INTO category_spec_mappings (id, category_id, spec_attribute_id, custom_hint, sort_order, created_at, updated_at)
SELECT gen_random_uuid(), c.id, s.id, NULL, t.ord, NOW(), NOW()
FROM categories c, (VALUES
  ('Kiểu dáng', 0), ('Thời gian sử dụng', 1), ('Thời gian sạc', 2),
  ('Cổng sạc', 3), ('Chống ồn', 4), ('Mic thoại', 5), ('Tương thích', 6)
) AS t(spec_name, ord)
JOIN spec_attributes s ON s.name = t.spec_name
WHERE c.name ILIKE '%Tai nghe%'
ON CONFLICT DO NOTHING;

-- Loa
INSERT INTO category_spec_mappings (id, category_id, spec_attribute_id, custom_hint, sort_order, created_at, updated_at)
SELECT gen_random_uuid(), c.id, s.id, NULL, t.ord, NOW(), NOW()
FROM categories c, (VALUES
  ('Công suất', 0), ('Kết nối', 1), ('Thời lượng pin', 2),
  ('Thời gian sạc', 3), ('Phím điều khiển', 4), ('Trọng lượng', 5), ('Thương hiệu', 6)
) AS t(spec_name, ord)
JOIN spec_attributes s ON s.name = t.spec_name
WHERE c.name ILIKE '%Loa%'
ON CONFLICT DO NOTHING;

-- Bàn phím
INSERT INTO category_spec_mappings (id, category_id, spec_attribute_id, custom_hint, sort_order, created_at, updated_at)
SELECT gen_random_uuid(), c.id, s.id, NULL, t.ord, NOW(), NOW()
FROM categories c, (VALUES
  ('Kiểu bàn phím', 0), ('Loại Switch', 1), ('Kết nối', 2),
  ('Đèn LED', 3), ('Kích thước', 4), ('Trọng lượng', 5), ('Tương thích', 6)
) AS t(spec_name, ord)
JOIN spec_attributes s ON s.name = t.spec_name
WHERE c.name ILIKE '%Bàn phím%'
ON CONFLICT DO NOTHING;

-- Chuột
INSERT INTO category_spec_mappings (id, category_id, spec_attribute_id, custom_hint, sort_order, created_at, updated_at)
SELECT gen_random_uuid(), c.id, s.id, NULL, t.ord, NOW(), NOW()
FROM categories c, (VALUES
  ('Độ phân giải (DPI)', 0), ('Số nút bấm', 1), ('Kết nối', 2),
  ('Loại pin', 3), ('Đèn LED', 4), ('Trọng lượng', 5), ('Tương thích', 6)
) AS t(spec_name, ord)
JOIN spec_attributes s ON s.name = t.spec_name
WHERE c.name ILIKE '%Chuột%'
ON CONFLICT DO NOTHING;

-- Màn hình
INSERT INTO category_spec_mappings (id, category_id, spec_attribute_id, custom_hint, sort_order, created_at, updated_at)
SELECT gen_random_uuid(), c.id, s.id, NULL, t.ord, NOW(), NOW()
FROM categories c, (VALUES
  ('Kích thước', 0), ('Độ phân giải', 1), ('Tần số quét', 2), ('Độ sáng', 3),
  ('Độ tương phản', 4), ('Cổng kết nối', 5), ('Tấm nền', 6), ('Tương thích VESA', 7)
) AS t(spec_name, ord)
JOIN spec_attributes s ON s.name = t.spec_name
WHERE c.name ILIKE '%Màn hình%' AND c.name NOT ILIKE '%Điện thoại%'
ON CONFLICT DO NOTHING;
