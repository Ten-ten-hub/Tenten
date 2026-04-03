/* =========================================================
   [USER-SERVICE] DUMMY DATA (local only)
   - 비밀번호는 모두 '1234' (BCrypt 인코딩)
   - hub_id, company_id는 더미 UUID 사용
   - ON CONFLICT DO NOTHING: 기존 데이터와 충돌 시 스킵
   ========================================================= */

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================================================
-- p_user
-- =========================================================
INSERT INTO p_user (id, login_id, password, name, role, slack_id, email, phone_number,
                    signup_status, affiliated_status, created_at, created_by)
VALUES
    -- MASTER_ADMIN (소속 없음)
    ('00000000-0000-0000-0000-000000000001',
     'master_admin',
     crypt('1234', gen_salt('bf', 10)),
     '마스터관리자', 'MASTER_ADMIN', 'slack_master', 'master@test.com', '010-0000-0001',
     'APPROVED', 'NOT_APPLICABLE', NOW(), '00000000-0000-0000-0000-000000000001'),

    -- HUB_ADMIN (허브 소속)
    ('00000000-0000-0000-0000-000000000002',
     'hub_admin',
     crypt('1234', gen_salt('bf', 10)),
     '허브관리자', 'HUB_ADMIN', 'slack_hub_admin', 'hub_admin@test.com', '010-0000-0002',
     'APPROVED', 'HUB_AFFILIATED', NOW(), '00000000-0000-0000-0000-000000000001'),

    -- HUB_DELIVERY_MANAGER (허브 소속)
    ('00000000-0000-0000-0000-000000000003',
     'hub_delivery',
     crypt('1234', gen_salt('bf', 10)),
     '허브배송담당자', 'HUB_DELIVERY_MANAGER', 'slack_hub_delivery', 'hub_delivery@test.com', '010-0000-0003',
     'APPROVED', 'HUB_AFFILIATED', NOW(), '00000000-0000-0000-0000-000000000001'),

    -- COM_DELIVERY_MANAGER (업체 소속)
    ('00000000-0000-0000-0000-000000000004',
     'com_delivery',
     crypt('1234', gen_salt('bf', 10)),
     '업체배송담당자', 'COM_DELIVERY_MANAGER', 'slack_com_delivery', 'com_delivery@test.com', '010-0000-0004',
     'APPROVED', 'COM_AFFILIATED', NOW(), '00000000-0000-0000-0000-000000000001'),

    -- COMPANY_MANAGER (업체 소속)
    ('00000000-0000-0000-0000-000000000005',
     'company_manager',
     crypt('1234', gen_salt('bf', 10)),
     '업체담당자', 'COMPANY_MANAGER', 'slack_company', 'company@test.com', '010-0000-0005',
     'APPROVED', 'COM_AFFILIATED', NOW(), '00000000-0000-0000-0000-000000000001'),

    -- NONE (가입 대기, 소속 없음)
    ('00000000-0000-0000-0000-000000000006',
     'pending_user',
     crypt('1234', gen_salt('bf', 10)),
     '가입대기유저', 'NONE', 'slack_pending', 'pending@test.com', '010-0000-0006',
     'PENDING', 'NOT_APPLICABLE', NOW(), '00000000-0000-0000-0000-000000000006')

ON CONFLICT DO NOTHING;

-- =========================================================
-- p_hub_user (HUB_AFFILIATED 유저)
-- =========================================================
INSERT INTO p_hub_user (id, user_id, hub_id, created_at, created_by)
VALUES
    ('00000000-0000-0001-0000-000000000001',
     '00000000-0000-0000-0000-000000000002',
     '11111111-1111-1111-1111-111111111111',
     NOW(), '00000000-0000-0000-0000-000000000001'),

    ('00000000-0000-0001-0000-000000000002',
     '00000000-0000-0000-0000-000000000003',
     '11111111-1111-1111-1111-111111111111',
     NOW(), '00000000-0000-0000-0000-000000000001')

ON CONFLICT DO NOTHING;

-- =========================================================
-- p_company_user (COM_AFFILIATED 유저)
-- =========================================================
INSERT INTO p_company_user (id, user_id, company_id, created_at, created_by)
VALUES
    ('00000000-0000-0002-0000-000000000001',
     '00000000-0000-0000-0000-000000000004',
     '22222222-2222-2222-2222-222222222222',
     NOW(), '00000000-0000-0000-0000-000000000001'),

    ('00000000-0000-0002-0000-000000000002',
     '00000000-0000-0000-0000-000000000005',
     '22222222-2222-2222-2222-222222222222',
     NOW(), '00000000-0000-0000-0000-000000000001')

ON CONFLICT DO NOTHING;
