CREATE TABLE countries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    country_code VARCHAR(10) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO countries (name, country_code) VALUES
    ('United States',     'US'),
    ('Ethiopia',          'ET'),
    ('United Kingdom',    'GB'),
    ('France',            'FR'),
    ('United Arab Emirates', 'AE'),
    ('Saudi Arabia',      'SA'),
    ('South Africa',      'ZA'),
    ('Canada',            'CA'),
    ('Nigeria',           'NG'),
    ('Germany',           'DE'),
    ('Italy',             'IT'),
    ('Spain',             'ES'),
    ('China',             'CN'),
    ('India',             'IN'),
    ('Brazil',            'BR'),
    ('Australia',         'AU'),
    ('Ghana',             'GH'),
    ('Kenya',             'KE'),
    ('Egypt',             'EG'),
    ('Turkey',            'TR');
