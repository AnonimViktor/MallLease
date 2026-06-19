BEGIN;

INSERT INTO role (code, name) VALUES
    ('admin',   'Администратор'),
    ('manager', 'Менеджер'),
    ('client',  'Клиент');

INSERT INTO users (role_id, login, email, phone, full_name, password_hash, is_active)
VALUES (
    (SELECT role_id FROM role WHERE code = 'admin'),
    'admin', 'admin@malllease.local', '+7 (999) 000-00-01',
    'Системный Администратор',
    '$2a$10$RGcxzGF6hYQbTl17ehiXK.melvrHOwCiWKLjqGv.6yAziOnmdAuzG', TRUE);

INSERT INTO users (role_id, login, email, phone, full_name, password_hash, is_active)
VALUES (
    (SELECT role_id FROM role WHERE code = 'manager'),
    'manager', 'manager@malllease.local', '+7 (999) 000-00-02',
    'Иванов Петр Сергеевич',
    '$2a$10$rtsA.YEyWK6AfWHyxuv.weF//IqOr6Tya3kpz5G4xflWDCDsEpFMS', TRUE);

INSERT INTO users (role_id, login, email, phone, full_name, password_hash, is_active)
VALUES (
    (SELECT role_id FROM role WHERE code = 'client'),
    'client', 'client@malllease.local', '+7 (999) 000-00-03',
    'Анна Смирнова',
    '$2a$10$8mB1W6OYY7SlyNZo0wIPAuduPtKXvbmu.wLy8TYvh6x9lGrrxg5Hu', TRUE);

INSERT INTO shopping_center (name, address, image_url, map_path) VALUES
    ('ТЦ «Галерея»',   'г. Москва, ул. Тверская, д. 12',
        'https://picsum.photos/seed/mall-galereya/1200/600', 'maps/center-1'),
    ('ТЦ «Мега Молл»', 'г. Москва, Ленинградское шоссе, д. 45',
        'https://picsum.photos/seed/mall-mega/1200/600', 'maps/center-2'),
    ('ТЦ «Орбита Плаза»', 'г. Москва, проспект Мира, д. 188',
        'https://picsum.photos/seed/mall-orbita/1200/600', 'maps/center-3'),
    ('ТЦ «Сквер»', 'г. Москва, ул. Садовая, д. 3',
        'https://picsum.photos/seed/mall-skver/1200/600', NULL);

INSERT INTO trade_point (shopping_center_id, point_code, floor, area_m2, has_air_conditioner, current_daily_rate, is_active)
VALUES
    (1, 'A-101', 1, 45.00, TRUE, 5500.00, TRUE),
    (1, 'A-102', 1, 32.50, TRUE, 4200.00, TRUE),
    (1, 'A-103', 1, 60.00, TRUE, 7800.00, TRUE),
    (1, 'A-201', 2, 80.00, TRUE, 9000.00, TRUE),
    (1, 'A-202', 2, 25.00, FALSE, 3100.00, TRUE),
    (1, 'A-203', 2, 55.00, TRUE, 6500.00, FALSE),
    (1, 'A-301', 3, 120.00, TRUE, 14000.00, TRUE),
    (1, 'A-302', 3, 40.00, FALSE, 4800.00, TRUE),
    (1, 'A-104', 1, 48.00, TRUE, 6100.00, TRUE),
    (1, 'A-105', 1, 58.00, TRUE, 7200.00, TRUE),
    (1, 'A-106', 1, 38.00, TRUE, 4900.00, TRUE),
    (1, 'A-107', 1, 36.00, FALSE, 4300.00, TRUE),
    (1, 'A-108', 1, 62.00, TRUE, 7800.00, TRUE),
    (1, 'A-109', 1, 54.00, TRUE, 6700.00, TRUE),
    (1, 'A-110', 1, 72.00, TRUE, 8800.00, TRUE),
    (1, 'A-111', 1, 46.00, FALSE, 5200.00, TRUE),
    (1, 'A-112', 1, 51.00, TRUE, 6400.00, FALSE),
    (1, 'A-204', 2, 44.00, TRUE, 5600.00, TRUE),
    (1, 'A-205', 2, 39.00, TRUE, 5000.00, TRUE),
    (1, 'A-206', 2, 42.00, FALSE, 4700.00, TRUE),
    (1, 'A-207', 2, 58.00, TRUE, 7100.00, TRUE),
    (1, 'A-208', 2, 73.00, TRUE, 8400.00, TRUE),
    (1, 'A-209', 2, 96.00, TRUE, 11200.00, TRUE),
    (1, 'A-210', 2, 90.00, TRUE, 6500.00, FALSE),
    (1, 'A-211', 2, 130.00, TRUE, 2600.00, FALSE),
    (1, 'A-212', 2, 100.00, TRUE, 2300.00, TRUE),
    (1, 'A-213', 2, 70.00, TRUE, 2100.00, TRUE),
    (1, 'A-214', 2, 80.00, FALSE, 2200.00, TRUE),
    (1, 'A-215', 2, 200.00, TRUE, 3200.00, TRUE),
    (1, 'A-303', 3, 82.00, TRUE, 9700.00, TRUE),
    (1, 'A-304', 3, 66.00, TRUE, 7900.00, TRUE),
    (1, 'A-305', 3, 88.00, TRUE, 10300.00, TRUE),
    (1, 'A-306', 3, 105.00, TRUE, 12200.00, TRUE);

INSERT INTO trade_point (shopping_center_id, point_code, floor, area_m2, has_air_conditioner, current_daily_rate, is_active)
VALUES
    (2, 'B-101', 1, 50.00, TRUE, 6000.00, TRUE),
    (2, 'B-102', 1, 35.00, TRUE, 4500.00, TRUE),
    (2, 'B-103', 1, 70.00, TRUE, 8500.00, TRUE),
    (2, 'B-201', 2, 90.00, TRUE, 10500.00, TRUE),
    (2, 'B-202', 2, 28.00, FALSE, 3400.00, FALSE),
    (2, 'B-301', 3, 150.00, TRUE, 16000.00, TRUE),
    (2, 'B-104', 1, 64.00, TRUE, 7600.00, TRUE),
    (2, 'B-105', 1, 41.00, TRUE, 5300.00, TRUE),
    (2, 'B-106', 1, 44.00, FALSE, 5100.00, TRUE),
    (2, 'B-107', 1, 70.00, TRUE, 8200.00, TRUE),
    (2, 'B-108', 1, 62.00, TRUE, 7400.00, TRUE),
    (2, 'B-109', 1, 55.00, TRUE, 6900.00, TRUE),
    (2, 'B-110', 1, 78.00, TRUE, 9100.00, FALSE),
    (2, 'B-203', 2, 86.00, TRUE, 9900.00, TRUE),
    (2, 'B-204', 2, 74.00, TRUE, 8700.00, TRUE),
    (2, 'B-205', 2, 68.00, TRUE, 8200.00, TRUE),
    (2, 'B-206', 2, 94.00, TRUE, 10800.00, TRUE),
    (2, 'B-302', 3, 118.00, TRUE, 13200.00, TRUE),
    (2, 'B-303', 3, 96.00, TRUE, 11200.00, TRUE),
    (2, 'B-304', 3, 90.00, TRUE, 10500.00, TRUE),
    (2, 'B-305', 3, 104.00, TRUE, 12100.00, TRUE),
    (2, 'B-306', 3, 110.00, TRUE, 12600.00, TRUE);

INSERT INTO trade_point (shopping_center_id, point_code, floor, area_m2, has_air_conditioner, current_daily_rate, is_active)
VALUES
    (3, 'C1-01', 1, 120.00, FALSE, 15400.00, TRUE),
    (3, 'C1-02', 1, 40.00, FALSE, 3980.00, FALSE),
    (3, 'C1-03', 1, 85.00, FALSE, 9680.00, TRUE),
    (3, 'C1-04', 1, 32.00, FALSE, 3760.00, TRUE),
    (3, 'C1-05', 1, 85.00, FALSE, 10560.00, TRUE),
    (3, 'C1-06', 1, 60.00, FALSE, 7290.00, TRUE),
    (3, 'C1-07', 1, 36.00, TRUE, 3970.00, TRUE),
    (3, 'C1-08', 1, 40.00, TRUE, 4400.00, TRUE),
    (3, 'C1-09', 1, 60.00, FALSE, 7980.00, TRUE),
    (3, 'C1-10', 1, 45.00, TRUE, 5750.00, TRUE),
    (3, 'C1-11', 1, 32.00, TRUE, 3580.00, TRUE),
    (3, 'C1-12', 1, 45.00, TRUE, 5520.00, TRUE),
    (3, 'C1-13', 1, 95.00, FALSE, 9320.00, FALSE),
    (3, 'C1-14', 1, 40.00, TRUE, 5570.00, TRUE),
    (3, 'C1-15', 1, 32.00, TRUE, 3690.00, TRUE),
    (3, 'C1-16', 1, 36.00, TRUE, 3750.00, TRUE),
    (3, 'C1-17', 1, 120.00, TRUE, 14680.00, TRUE),
    (3, 'C1-18', 1, 85.00, TRUE, 8700.00, TRUE),
    (3, 'C1-19', 1, 45.00, TRUE, 5570.00, TRUE),
    (3, 'C1-20', 1, 40.00, TRUE, 5310.00, TRUE),
    (3, 'C1-21', 1, 40.00, TRUE, 5240.00, TRUE),
    (3, 'C1-22', 1, 45.00, FALSE, 6120.00, TRUE),
    (3, 'C2-01', 2, 52.00, FALSE, 6100.00, FALSE),
    (3, 'C2-02', 2, 72.00, FALSE, 7290.00, FALSE),
    (3, 'C2-03', 2, 85.00, TRUE, 10930.00, TRUE),
    (3, 'C2-04', 2, 95.00, TRUE, 9960.00, TRUE),
    (3, 'C2-05', 2, 85.00, TRUE, 10960.00, TRUE),
    (3, 'C2-06', 2, 36.00, TRUE, 4700.00, TRUE),
    (3, 'C2-07', 2, 95.00, FALSE, 10650.00, TRUE),
    (3, 'C2-08', 2, 85.00, TRUE, 10190.00, TRUE),
    (3, 'C2-09', 2, 120.00, TRUE, 15080.00, TRUE),
    (3, 'C2-10', 2, 45.00, TRUE, 4960.00, TRUE),
    (3, 'C2-11', 2, 60.00, FALSE, 5700.00, FALSE),
    (3, 'C2-12', 2, 45.00, TRUE, 5810.00, TRUE),
    (3, 'C2-13', 2, 32.00, TRUE, 3460.00, FALSE),
    (3, 'C2-14', 2, 85.00, TRUE, 8650.00, TRUE),
    (3, 'C2-15', 2, 85.00, TRUE, 11600.00, TRUE),
    (3, 'C2-16', 2, 95.00, TRUE, 9100.00, TRUE),
    (3, 'C2-17', 2, 45.00, FALSE, 4760.00, TRUE),
    (3, 'C2-18', 2, 32.00, FALSE, 3730.00, TRUE),
    (3, 'C2-19', 2, 85.00, TRUE, 8560.00, TRUE),
    (3, 'C2-20', 2, 85.00, FALSE, 10090.00, TRUE),
    (3, 'C2-21', 2, 60.00, TRUE, 8200.00, FALSE),
    (3, 'C2-22', 2, 40.00, TRUE, 4510.00, FALSE),
    (3, 'C3-01', 3, 120.00, TRUE, 16250.00, TRUE),
    (3, 'C3-02', 3, 32.00, FALSE, 3130.00, TRUE),
    (3, 'C3-03', 3, 95.00, TRUE, 11540.00, TRUE),
    (3, 'C3-04', 3, 32.00, TRUE, 3120.00, TRUE),
    (3, 'C3-05', 3, 28.00, TRUE, 2740.00, TRUE),
    (3, 'C3-06', 3, 45.00, TRUE, 4700.00, TRUE),
    (3, 'C3-07', 3, 95.00, TRUE, 10060.00, TRUE),
    (3, 'C3-08', 3, 60.00, FALSE, 5960.00, TRUE),
    (3, 'C3-09', 3, 52.00, TRUE, 6030.00, FALSE),
    (3, 'C3-10', 3, 28.00, TRUE, 3900.00, TRUE),
    (3, 'C3-11', 3, 28.00, TRUE, 3080.00, TRUE),
    (3, 'C3-12', 3, 40.00, FALSE, 4760.00, TRUE),
    (3, 'C3-13', 3, 60.00, FALSE, 6940.00, TRUE),
    (3, 'C3-14', 3, 72.00, TRUE, 9610.00, TRUE),
    (3, 'C3-15', 3, 28.00, TRUE, 3340.00, TRUE),
    (3, 'C3-16', 3, 32.00, TRUE, 4260.00, TRUE),
    (3, 'C3-17', 3, 60.00, TRUE, 6270.00, TRUE),
    (3, 'C3-18', 3, 28.00, FALSE, 2660.00, TRUE),
    (3, 'C3-19', 3, 45.00, TRUE, 5860.00, TRUE),
    (3, 'C3-20', 3, 60.00, TRUE, 7670.00, TRUE),
    (3, 'C3-21', 3, 120.00, TRUE, 12230.00, TRUE),
    (3, 'C3-22', 3, 40.00, TRUE, 4840.00, TRUE),
    (3, 'C4-01', 4, 28.00, TRUE, 2730.00, TRUE),
    (3, 'C4-02', 4, 72.00, TRUE, 9600.00, TRUE),
    (3, 'C4-03', 4, 28.00, TRUE, 2760.00, TRUE),
    (3, 'C4-04', 4, 32.00, TRUE, 4010.00, TRUE),
    (3, 'C4-05', 4, 60.00, FALSE, 8100.00, TRUE),
    (3, 'C4-06', 4, 95.00, TRUE, 11670.00, TRUE),
    (3, 'C4-07', 4, 120.00, TRUE, 14220.00, TRUE),
    (3, 'C4-08', 4, 40.00, TRUE, 4360.00, TRUE),
    (3, 'C4-09', 4, 60.00, FALSE, 7440.00, TRUE),
    (3, 'C4-10', 4, 52.00, TRUE, 7120.00, TRUE),
    (3, 'C4-11', 4, 72.00, TRUE, 8660.00, TRUE),
    (3, 'C4-12', 4, 32.00, TRUE, 3760.00, TRUE),
    (3, 'C4-13', 4, 52.00, TRUE, 6990.00, TRUE),
    (3, 'C4-14', 4, 45.00, FALSE, 5960.00, FALSE),
    (3, 'C4-15', 4, 45.00, TRUE, 6270.00, FALSE),
    (3, 'C4-16', 4, 85.00, FALSE, 11200.00, TRUE),
    (3, 'C4-17', 4, 120.00, FALSE, 16140.00, TRUE),
    (3, 'C4-18', 4, 32.00, TRUE, 4100.00, TRUE),
    (3, 'C4-19', 4, 45.00, TRUE, 4700.00, TRUE),
    (3, 'C4-20', 4, 40.00, TRUE, 5330.00, TRUE),
    (3, 'C4-21', 4, 72.00, TRUE, 9780.00, TRUE),
    (3, 'C4-22', 4, 32.00, TRUE, 4230.00, TRUE),
    (3, 'C5-01', 5, 28.00, TRUE, 2820.00, TRUE),
    (3, 'C5-02', 5, 36.00, TRUE, 4310.00, TRUE),
    (3, 'C5-03', 5, 85.00, FALSE, 8360.00, FALSE),
    (3, 'C5-04', 5, 36.00, TRUE, 4770.00, TRUE),
    (3, 'C5-05', 5, 85.00, FALSE, 8560.00, TRUE),
    (3, 'C5-06', 5, 52.00, TRUE, 6800.00, TRUE),
    (3, 'C5-07', 5, 52.00, FALSE, 5520.00, TRUE),
    (3, 'C5-08', 5, 52.00, TRUE, 7000.00, TRUE),
    (3, 'C5-09', 5, 95.00, TRUE, 12980.00, TRUE),
    (3, 'C5-10', 5, 36.00, TRUE, 4730.00, TRUE),
    (3, 'C5-11', 5, 28.00, FALSE, 3820.00, TRUE),
    (3, 'C5-12', 5, 120.00, TRUE, 15770.00, TRUE),
    (3, 'C5-13', 5, 36.00, TRUE, 3590.00, TRUE),
    (3, 'C5-14', 5, 72.00, FALSE, 9480.00, TRUE),
    (3, 'C5-15', 5, 52.00, TRUE, 6800.00, TRUE),
    (3, 'C5-16', 5, 40.00, FALSE, 4140.00, TRUE),
    (3, 'C5-17', 5, 45.00, TRUE, 6230.00, TRUE),
    (3, 'C5-18', 5, 52.00, TRUE, 5870.00, TRUE),
    (3, 'C5-19', 5, 52.00, TRUE, 5200.00, TRUE),
    (3, 'C5-20', 5, 36.00, TRUE, 5010.00, TRUE),
    (3, 'C5-21', 5, 32.00, TRUE, 3530.00, TRUE),
    (3, 'C5-22', 5, 60.00, TRUE, 7080.00, TRUE),
    (3, 'C6-01', 6, 95.00, FALSE, 9210.00, TRUE),
    (3, 'C6-02', 6, 28.00, TRUE, 3670.00, FALSE),
    (3, 'C6-03', 6, 120.00, FALSE, 13720.00, FALSE),
    (3, 'C6-04', 6, 52.00, TRUE, 6490.00, TRUE),
    (3, 'C6-05', 6, 45.00, TRUE, 5620.00, TRUE),
    (3, 'C6-06', 6, 60.00, TRUE, 7190.00, TRUE),
    (3, 'C6-07', 6, 60.00, TRUE, 6720.00, FALSE),
    (3, 'C6-08', 6, 36.00, TRUE, 3900.00, TRUE),
    (3, 'C6-09', 6, 28.00, TRUE, 2920.00, TRUE),
    (3, 'C6-10', 6, 95.00, TRUE, 11010.00, TRUE),
    (3, 'C6-11', 6, 120.00, FALSE, 13950.00, FALSE),
    (3, 'C6-12', 6, 36.00, TRUE, 3870.00, FALSE),
    (3, 'C6-13', 6, 120.00, TRUE, 11900.00, TRUE),
    (3, 'C6-14', 6, 120.00, TRUE, 15750.00, TRUE),
    (3, 'C6-15', 6, 28.00, FALSE, 3890.00, TRUE),
    (3, 'C6-16', 6, 32.00, TRUE, 4310.00, TRUE),
    (3, 'C6-17', 6, 40.00, TRUE, 4490.00, TRUE),
    (3, 'C6-18', 6, 40.00, FALSE, 5030.00, TRUE),
    (3, 'C6-19', 6, 60.00, FALSE, 7870.00, FALSE),
    (3, 'C6-20', 6, 85.00, TRUE, 10200.00, TRUE),
    (3, 'C6-21', 6, 72.00, FALSE, 8340.00, TRUE),
    (3, 'C6-22', 6, 85.00, TRUE, 11700.00, TRUE);

INSERT INTO trade_point (shopping_center_id, point_code, floor, area_m2, has_air_conditioner, current_daily_rate, is_active)
VALUES
    (4, 'D-101', 1, 55.00, TRUE, 6200.00, TRUE),
    (4, 'D-102', 1, 42.00, TRUE, 4800.00, TRUE),
    (4, 'D-103', 1, 70.00, FALSE, 7600.00, TRUE),
    (4, 'D-104', 1, 38.00, TRUE, 4300.00, FALSE);

UPDATE trade_point
SET image_url = 'https://picsum.photos/seed/tp' || trade_point_id || '/640/420';

INSERT INTO client (user_id, company_name, legal_address, bank_details, status)
VALUES (
    (SELECT user_id FROM users WHERE login = 'client'),
    'ООО «Северный кофе»',
    'г. Москва, ул. Лесная, д. 10',
    'ИНН 7700000000, р/с 40702810000000000001',
    'active');

INSERT INTO contract (client_id, contract_no, signed_at, status, comment) VALUES
    ((SELECT client_id FROM client WHERE company_name = 'ООО «Северный кофе»'),
     'ML-2026-001', CURRENT_DATE, 'active', 'Демо-договор (Галерея)'),
    ((SELECT client_id FROM client WHERE company_name = 'ООО «Северный кофе»'),
     'ML-2026-002', CURRENT_DATE, 'active', 'Демо-договор (Мега Молл)'),
    ((SELECT client_id FROM client WHERE company_name = 'ООО «Северный кофе»'),
     'ML-2026-003', CURRENT_DATE, 'active', 'Демо-договор (Орбита Плаза)'),
    ((SELECT client_id FROM client WHERE company_name = 'ООО «Северный кофе»'),
     'ML-2026-004', CURRENT_DATE - 95, 'active', 'Демо-договор с просрочкой (Галерея)'),
    ((SELECT client_id FROM client WHERE company_name = 'ООО «Северный кофе»'),
     'ML-2026-005', CURRENT_DATE - 150, 'active', 'Демо-договор истекает через ~15 дней (Галерея)');

INSERT INTO contract_rental (contract_id, point_id, date_from, date_to, daily_rate_fixed, status)
SELECT ct.contract_id, tp.trade_point_id, CURRENT_DATE, CURRENT_DATE + 180, tp.current_daily_rate, 'active'
FROM (VALUES
    ('ML-2026-001', 1, 'A-103'),
    ('ML-2026-001', 1, 'A-106'),
    ('ML-2026-001', 1, 'A-109'),
    ('ML-2026-002', 2, 'B-103'),
    ('ML-2026-002', 2, 'B-105'),
    ('ML-2026-003', 3, 'C1-05'),
    ('ML-2026-003', 3, 'C1-09')
) AS r(contract_no, center, point_code)
JOIN contract ct ON ct.contract_no = r.contract_no
JOIN trade_point tp ON tp.shopping_center_id = r.center AND tp.point_code = r.point_code;

INSERT INTO contract_rental (contract_id, point_id, date_from, date_to, daily_rate_fixed, status)
SELECT ct.contract_id, tp.trade_point_id, CURRENT_DATE - 95, CURRENT_DATE + 95, tp.current_daily_rate, 'active'
FROM contract ct
JOIN trade_point tp ON tp.shopping_center_id = 1 AND tp.point_code = 'A-101'
WHERE ct.contract_no = 'ML-2026-004';

INSERT INTO contract_rental (contract_id, point_id, date_from, date_to, daily_rate_fixed, status)
SELECT ct.contract_id, tp.trade_point_id, CURRENT_DATE - 150, CURRENT_DATE + 15, tp.current_daily_rate, 'active'
FROM contract ct
JOIN trade_point tp ON tp.shopping_center_id = 1 AND tp.point_code = 'A-111'
WHERE ct.contract_no = 'ML-2026-005';

INSERT INTO monthly_charges (contract_id, point_id, month, amount_due, status)
SELECT cr.contract_id, cr.point_id, gs::date AS month,
       cr.daily_rate_fixed
         * ((LEAST((gs + INTERVAL '1 month - 1 day')::date, cr.date_to)) - gs::date + 1),
       'unpaid'
FROM contract_rental cr
CROSS JOIN LATERAL generate_series(cr.date_from::timestamp, cr.date_to::timestamp, INTERVAL '1 month') AS gs;

INSERT INTO payment (charge_id, paid_at, amount, document_no, comment)
SELECT mc.charge_id, CURRENT_DATE, mc.amount_due, 'PAY-2026-001', 'Демо-платеж'
FROM monthly_charges mc
JOIN contract ct ON ct.contract_id = mc.contract_id
JOIN trade_point tp ON tp.trade_point_id = mc.point_id
WHERE ct.contract_no = 'ML-2026-001' AND tp.point_code = 'A-103'
ORDER BY mc.month
LIMIT 1;

UPDATE monthly_charges SET status = 'paid'
WHERE charge_id IN (SELECT charge_id FROM payment);

COMMIT;
