/* =========================================================
   [DELIVERY-SERVICE] DUMMY DATA
   - hub UUID는 hub-server V2__seed_data.sql 기준
   - user UUID는 user-service V100__insert_dummy_data.sql 기준
   - company UUID는 company-service V2__seed.sql 기준
   - ON CONFLICT DO NOTHING: 기존 데이터와 충돌 시 스킵
   =========================================================
   허브 UUID 참조
     서울특별시 센터  : 550e8400-e29b-41d4-a716-446655440001
     경기 남부 센터   : 550e8400-e29b-41d4-a716-446655440003
     대전광역시 센터  : 550e8400-e29b-41d4-a716-446655440008
     대구광역시 센터  : 550e8400-e29b-41d4-a716-446655440011
     부산광역시 센터  : 550e8400-e29b-41d4-a716-446655440013
   ========================================================= */

-- =========================================================
-- p_delivery_manager
-- =========================================================
INSERT INTO p_delivery_manager (
    id, hub_id, slack_id, delivery_manager_type, delivery_sequence,
    created_at, created_by
) VALUES
    -- HUB_DELIVERY_MANAGER : 서울 허브 (user-service 허브배송담당자)
    ('d1000000-0000-0000-0000-000000000001',
     '550e8400-e29b-41d4-a716-446655440001',
     'slack_hub_delivery',
     'HUB_DELIVERY_MANAGER', 1,
     NOW(), '00000000-0000-0000-0000-000000000001'),

    -- HUB_DELIVERY_MANAGER : 서울 허브 2번
    ('d1000000-0000-0000-0000-000000000002',
     '550e8400-e29b-41d4-a716-446655440001',
     'slack_hub_delivery_2',
     'HUB_DELIVERY_MANAGER', 2,
     NOW(), '00000000-0000-0000-0000-000000000001'),

    -- HUB_DELIVERY_MANAGER : 부산 허브
    ('d1000000-0000-0000-0000-000000000003',
     '550e8400-e29b-41d4-a716-446655440013',
     'slack_hub_delivery_busan',
     'HUB_DELIVERY_MANAGER', 1,
     NOW(), '00000000-0000-0000-0000-000000000001'),

    -- COMPANY_DELIVERY_MANAGER : 부산 허브 소속 (user-service 업체배송담당자)
    ('d1000000-0000-0000-0000-000000000004',
     '550e8400-e29b-41d4-a716-446655440013',
     'slack_com_delivery',
     'COMPANY_DELIVERY_MANAGER', 1,
     NOW(), '00000000-0000-0000-0000-000000000001'),

    -- COMPANY_DELIVERY_MANAGER : 서울 허브 소속
    ('d1000000-0000-0000-0000-000000000005',
     '550e8400-e29b-41d4-a716-446655440001',
     'slack_com_delivery_seoul',
     'COMPANY_DELIVERY_MANAGER', 1,
     NOW(), '00000000-0000-0000-0000-000000000001')

ON CONFLICT DO NOTHING;

-- =========================================================
-- p_delivery
-- =========================================================
INSERT INTO p_delivery (
    id, order_id, delivery_status,
    origin_hub_id, destination_hub_id,
    receiver_company_id,
    delivery_address, delivery_address_detail,
    recipient_name, recipient_slack_id,
    company_delivery_manager_id,
    started_at, completed_at,
    final_dispatch_deadline_at,
    created_at, created_by
) VALUES
    -- 1. DELIVERED : 서울 → 부산, 완료된 배송
    ('d2000000-0000-0000-0000-000000000001',
     'd3000000-0000-0000-0000-000000000001',
     'DELIVERED',
     '550e8400-e29b-41d4-a716-446655440001',
     '550e8400-e29b-41d4-a716-446655440013',
     '22222222-2222-2222-2222-222222222222',
     '부산광역시 해운대구 센텀로 10', '101호',
     '김수령', 'U_RECEIVER_001',
     'd1000000-0000-0000-0000-000000000004',
     NOW() - INTERVAL '3 days', NOW() - INTERVAL '1 day',
     NOW() - INTERVAL '12 hours',
     NOW() - INTERVAL '4 days', '00000000-0000-0000-0000-000000000001'),

    -- 2. MOVING_BETWEEN_HUBS : 대전 → 부산, 허브 간 이동 중
    ('d2000000-0000-0000-0000-000000000002',
     'd3000000-0000-0000-0000-000000000002',
     'MOVING_BETWEEN_HUBS',
     '550e8400-e29b-41d4-a716-446655440008',
     '550e8400-e29b-41d4-a716-446655440013',
     '22222222-2222-2222-2222-222222222222',
     '부산광역시 연제구 중앙대로 100', '5층',
     '이수신', 'U_RECEIVER_002',
     NULL,
     NOW() - INTERVAL '6 hours', NULL,
     NOW() + INTERVAL '1 day',
     NOW() - INTERVAL '7 hours', '00000000-0000-0000-0000-000000000001'),

    -- 3. WAITING_AT_HUB : 서울 → 경기 남부, 허브 대기 중
    ('d2000000-0000-0000-0000-000000000003',
     'd3000000-0000-0000-0000-000000000003',
     'WAITING_AT_HUB',
     '550e8400-e29b-41d4-a716-446655440001',
     '550e8400-e29b-41d4-a716-446655440003',
     '11111111-1111-1111-1111-111111111111',
     '경기도 수원시 영통구 광교로 145', '2층',
     '박경기', 'U_RECEIVER_003',
     NULL,
     NULL, NULL,
     NOW() + INTERVAL '2 days',
     NOW(), '00000000-0000-0000-0000-000000000001')

ON CONFLICT DO NOTHING;

-- =========================================================
-- p_delivery_route_log
-- =========================================================
INSERT INTO p_delivery_route_log (
    id, delivery_id, sequence_no,
    departure_hub_id, arrival_hub_id,
    expected_distance_km, expected_duration_minutes,
    real_duration_minutes,
    route_status, delivery_manager_id,
    departed_at, arrived_at,
    created_at, created_by
) VALUES
    -- [배송 1 - DELIVERED] 서울 → 대전 (leg 1)
    ('d4000000-0000-0000-0000-000000000001',
     'd2000000-0000-0000-0000-000000000001', 1,
     '550e8400-e29b-41d4-a716-446655440001',
     '550e8400-e29b-41d4-a716-446655440008',
     155.0, 120, 130,
     'DELIVERED', 'd1000000-0000-0000-0000-000000000001',
     NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days' + INTERVAL '130 minutes',
     NOW() - INTERVAL '4 days', '00000000-0000-0000-0000-000000000001'),

    -- [배송 1 - DELIVERED] 대전 → 대구 (leg 2)
    ('d4000000-0000-0000-0000-000000000002',
     'd2000000-0000-0000-0000-000000000001', 2,
     '550e8400-e29b-41d4-a716-446655440008',
     '550e8400-e29b-41d4-a716-446655440011',
     145.2, 150, 155,
     'DELIVERED', 'd1000000-0000-0000-0000-000000000001',
     NOW() - INTERVAL '3 days' + INTERVAL '130 minutes',
     NOW() - INTERVAL '3 days' + INTERVAL '285 minutes',
     NOW() - INTERVAL '4 days', '00000000-0000-0000-0000-000000000001'),

    -- [배송 1 - DELIVERED] 대구 → 부산 (leg 3)
    ('d4000000-0000-0000-0000-000000000003',
     'd2000000-0000-0000-0000-000000000001', 3,
     '550e8400-e29b-41d4-a716-446655440011',
     '550e8400-e29b-41d4-a716-446655440013',
     98.5, 90, 95,
     'DELIVERED', 'd1000000-0000-0000-0000-000000000003',
     NOW() - INTERVAL '3 days' + INTERVAL '285 minutes',
     NOW() - INTERVAL '3 days' + INTERVAL '380 minutes',
     NOW() - INTERVAL '4 days', '00000000-0000-0000-0000-000000000001'),

    -- [배송 2 - MOVING_BETWEEN_HUBS] 대전 → 대구 (leg 1, 이동 중)
    ('d4000000-0000-0000-0000-000000000004',
     'd2000000-0000-0000-0000-000000000002', 1,
     '550e8400-e29b-41d4-a716-446655440008',
     '550e8400-e29b-41d4-a716-446655440011',
     145.2, 150, NULL,
     'MOVING_BETWEEN_HUBS', 'd1000000-0000-0000-0000-000000000002',
     NOW() - INTERVAL '6 hours', NULL,
     NOW() - INTERVAL '7 hours', '00000000-0000-0000-0000-000000000001'),

    -- [배송 2 - MOVING_BETWEEN_HUBS] 대구 → 부산 (leg 2, 대기)
    ('d4000000-0000-0000-0000-000000000005',
     'd2000000-0000-0000-0000-000000000002', 2,
     '550e8400-e29b-41d4-a716-446655440011',
     '550e8400-e29b-41d4-a716-446655440013',
     98.5, 90, NULL,
     'WAITING_AT_HUB', NULL,
     NULL, NULL,
     NOW() - INTERVAL '7 hours', '00000000-0000-0000-0000-000000000001'),

    -- [배송 3 - WAITING_AT_HUB] 서울 → 경기 남부 (leg 1, 대기)
    ('d4000000-0000-0000-0000-000000000006',
     'd2000000-0000-0000-0000-000000000003', 1,
     '550e8400-e29b-41d4-a716-446655440001',
     '550e8400-e29b-41d4-a716-446655440003',
     42.0, 50, NULL,
     'WAITING_AT_HUB', NULL,
     NULL, NULL,
     NOW(), '00000000-0000-0000-0000-000000000001')

ON CONFLICT DO NOTHING;
