-- ============================================================
-- Seed: Items  (admin-seeded; is_approved = true, created_by = null)
-- ============================================================

-- Electronics
INSERT INTO items (name, category, measurement_type, measurement_unit, is_active, is_approved) VALUES
    ('iPhone',           'ELECTRONICS', 'SOLID_PIECE',   'PIECE',     TRUE, TRUE),
    ('Laptop',           'ELECTRONICS', 'SOLID_PIECE',   'PIECE',     TRUE, TRUE),
    ('Tablet',           'ELECTRONICS', 'SOLID_PIECE',   'PIECE',     TRUE, TRUE),
    ('Smartwatch',       'ELECTRONICS', 'SOLID_PIECE',   'PIECE',     TRUE, TRUE),
    ('Headphones',       'ELECTRONICS', 'SOLID_PIECE',   'PIECE',     TRUE, TRUE);

-- Beauty
INSERT INTO items (name, category, measurement_type, measurement_unit, is_active, is_approved) VALUES
    ('Body Lotion',      'BEAUTY',      'LIQUID',        'MILLILITER', TRUE, TRUE),
    ('Perfume',          'BEAUTY',      'LIQUID',        'MILLILITER', TRUE, TRUE),
    ('Shampoo',          'BEAUTY',      'LIQUID',        'MILLILITER', TRUE, TRUE),
    ('Skincare Cream',   'BEAUTY',      'LIQUID',        'MILLILITER', TRUE, TRUE);

-- Food
INSERT INTO items (name, category, measurement_type, measurement_unit, is_active, is_approved) VALUES
    ('Coffee Beans',     'FOOD',        'SOLID_WEIGHT',  'KILOGRAM',  TRUE, TRUE),
    ('Spices',           'FOOD',        'SOLID_WEIGHT',  'KILOGRAM',  TRUE, TRUE),
    ('Protein Powder',   'FOOD',        'SOLID_WEIGHT',  'KILOGRAM',  TRUE, TRUE),
    ('Tea Leaves',       'FOOD',        'SOLID_WEIGHT',  'KILOGRAM',  TRUE, TRUE);

-- Clothing
INSERT INTO items (name, category, measurement_type, measurement_unit, is_active, is_approved) VALUES
    ('Clothing Item',    'CLOTHING',    'SOLID_PIECE',   'PIECE',     TRUE, TRUE),
    ('Shoes',            'CLOTHING',    'SOLID_PIECE',   'PIECE',     TRUE, TRUE);

-- Documents
INSERT INTO items (name, category, measurement_type, measurement_unit, is_active, is_approved) VALUES
    ('Documents',        'DOCUMENTS',   'SOLID_PIECE',   'PIECE',     TRUE, TRUE),
    ('Passport',         'DOCUMENTS',   'SOLID_PIECE',   'PIECE',     TRUE, TRUE);
