/* =========================================================
   [USER-SERVICE] DUMMY DATA (local only)
   - 기존 테스트 계정 + 실제 로컬 테스트 계정 반영
   - 실제 조회된 bcrypt password 사용
   - 현재 기준 소속 배정 안 된 계정은 p_hub_user / p_company_user에 넣지 않음
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
     'PENDING', 'NOT_APPLICABLE', NOW(), '00000000-0000-0000-0000-000000000006'),

    -- =====================================================
    -- 실제 로컬 테스트 계정 추가
    -- =====================================================

    -- NONE / PENDING
    ('1ddedba7-2cd5-4ae6-9369-56cce5b0dff6',
     'user1234',
     '$2a$10$cmNwJlESV3H5lAJ8XMxIQ./UfPhZtIsYuWFRHcD4.SVCvas3Bb5Cu',
     '홍길동', 'NONE', 'U12345678', 'hong@example.com', '010-1234-5678',
     'PENDING', 'NOT_APPLICABLE', '2026-04-07 03:14:20.157116', '00000000-0000-0000-0000-000000000000'),

    -- MASTER_ADMIN
    ('6b8e87b3-4f1b-4559-abcd-ae42959ed217',
     'master1',
     '$2a$10$OIAHNNLziDp4yfDZljiLg.b7fUz/8cn917ENgvi3w60s8bGUetPdS',
     '마스터관리자', 'MASTER_ADMIN', 'U_MASTER_001', 'master1@test.com', '010-1111-1111',
     'APPROVED', 'NOT_APPLICABLE', '2026-04-07 04:56:28.321475', '00000000-0000-0000-0000-000000000000'),

    -- HUB_ADMIN (현재는 아직 소속 미배정)
    ('3ee9ad49-9363-42e2-9614-37dde86f4afb',
     'hubadmin1',
     '$2a$10$h1n89Cvr31qq65lwfW8/tOswGlRMjJaGF.w6OEesm2pqT0OLhxoWi',
     '허브관리자1', 'HUB_ADMIN', 'U_HUBADMIN_001', 'hubadmin1@test.com', '010-2222-1111',
     'APPROVED', 'UNAFFILIATED', '2026-04-07 05:42:04.676114', '00000000-0000-0000-0000-000000000000'),

    -- COMPANY_MANAGER 후보 (현재는 아직 승인/권한 미완료)
    ('06cfdffc-91c0-4205-ab22-ff12a4341596',
     'companymg1',
     '$2a$10$zBMN2AuGM4tTyM.S8e8JFeeuPsgDozW/jIJZaBBDrvPJ3u1xJ9pH.',
     '업체담당자1', 'NONE', 'U_COMPANY_001', 'companymgr1@test.com', '010-3333-1111',
     'PENDING', 'NOT_APPLICABLE', '2026-04-07 05:44:55.96645', '00000000-0000-0000-0000-000000000000'),

    -- HUB_DELIVERY_MANAGER (현재는 아직 소속 미배정)
    ('cab560cc-75d1-44d4-8ad7-e852595e7b2c',
     'hubdeliv1',
     '$2a$10$0JwTtmPRL10qhI9ooCy4qOTKyMtdJMn.UPfm3sIwpKyXO50NkwA3a',
     '허브배송담당자1', 'HUB_DELIVERY_MANAGER', 'U_HDELIV_001', 'hubdeliv1@test.com', '010-4444-1111',
     'APPROVED', 'UNAFFILIATED', '2026-04-07 05:45:43.380148', '00000000-0000-0000-0000-000000000000'),

    -- COM_DELIVERY_MANAGER (현재는 아직 소속 미배정)
    ('b95f637e-af5c-452b-ab04-5df877e361ee',
     'comdeliv1',
     '$2a$10$zPwVh7qFRoPRAYexXefXx.X2bicoaI0oFB/RDob0V5262lalj098q',
     '업체배송담당자1', 'COM_DELIVERY_MANAGER', 'U_CDELIV_001', 'comdeliv1@test.com', '010-5555-1111',
     'APPROVED', 'UNAFFILIATED', '2026-04-07 05:46:12.997291', '00000000-0000-0000-0000-000000000000')

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
