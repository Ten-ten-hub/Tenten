-- 1. p_hub 테이블의 모든 데이터에 created_at 현재 시간으로 업데이트
UPDATE p_hub SET created_at = NOW() WHERE created_at IS NULL;

-- 2. p_hub_route 테이블의 모든 데이터에 created_at 현재 시간으로 업데이트
UPDATE p_hub_route SET created_at = NOW() WHERE created_at IS NULL;
