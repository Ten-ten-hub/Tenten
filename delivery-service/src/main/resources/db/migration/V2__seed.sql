/* =========================================================
   DELIVERY-SERVICE : V2__seed.sql
   개발용 초기 데이터
   ========================================================= */

INSERT INTO p_delivery (
    id,
    order_id,
    delivery_status,
    origin_hub_id,
    destination_hub_id,
    receiver_company_id,
    delivery_address,
    delivery_address_detail,
    recipient_name,
    recipient_slack_id,
    company_delivery_manager_id,
    started_at,
    completed_at,
    final_dispatch_deadline_at,
    created_at,
    created_by
) VALUES (
             '33333333-3333-3333-3333-333333333333',
             '44444444-4444-4444-4444-444444444444',
             'WAITING_AT_HUB',
             'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
             'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
             '22222222-2222-2222-2222-222222222222',
             '부산광역시 해운대구 센텀로 10',
             '101호',
             '김수령',
             'U_RECEIVER_001',
             NULL,
             NULL,
             NULL,
             NOW() + INTERVAL '2 days',
             NOW(),
             '99999999-9999-9999-9999-999999999999'
         );

INSERT INTO p_delivery_route_log (
    id,
    delivery_id,
    sequence_no,
    departure_hub_id,
    arrival_hub_id,
    expected_distance_km,
    expected_duration_minutes,
    route_status,
    delivery_manager_id,
    departed_at,
    arrived_at,
    created_at,
    created_by
) VALUES (
             '55555555-5555-5555-5555-555555555555',
             '33333333-3333-3333-3333-333333333333',
             1,
             'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
             'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
             325.50,
             240,
             'WAITING_AT_HUB',
             NULL,
             NULL,
             NULL,
             NOW(),
             '99999999-9999-9999-9999-999999999999'
         );