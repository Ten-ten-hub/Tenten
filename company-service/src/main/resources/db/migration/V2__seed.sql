/* =========================================================
   COMPANY-SERVICE : V2__seed.sql
   개발용 초기 데이터
   ========================================================= */

INSERT INTO p_company (
    id,
    name,
    company_type,
    hub_id,
    address,
    address_detail,
    zipcode,
    contact_name,
    contact_phone,
    contact_slack_id,
    is_active,
    created_at,
    created_by
) VALUES
      (
          '11111111-1111-1111-1111-111111111111',
          '서울공급업체A',
          'PRODUCER',
          'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
          '서울특별시 강남구 테헤란로 1',
          '3층',
          '06100',
          '홍길동',
          '010-1111-1111',
          'U_COMPANY_001',
          TRUE,
          NOW(),
          '99999999-9999-9999-9999-999999999999'
      ),
      (
          '22222222-2222-2222-2222-222222222222',
          '부산수령업체B',
          'RECEIVER',
          'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
          '부산광역시 해운대구 센텀로 10',
          '101호',
          '48058',
          '김수령',
          '010-2222-2222',
          'U_COMPANY_002',
          TRUE,
          NOW(),
          '99999999-9999-9999-9999-999999999999'
      );