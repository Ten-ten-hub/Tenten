# 🚚 Tenten Hub

MSA(Microservice Architecture) 기반의 물류 관리 플랫폼입니다.

---

## 📌 프로젝트 목적 및 상세

본 프로젝트는 **MSA(Microservices Architecture) 기반의 B2B 물류 관리 및 배송 시스템**입니다.

**Tenten Hub**는 전국 각 지역에 허브 센터를 보유하고 있으며, 업체 간 상품 주문이 발생하면 출발 허브에서 목적지 허브까지 물품을 이동시키고 최종 수령 업체까지 배송하는 전 과정을 관리합니다.

### 서비스 흐름
1. 수령 업체가 공급 업체의 상품을 주문
2. 주문 생성 시 출발 허브 → 목적지 허브 간 배송 경로 자동 생성
3. 허브 배송 담당자가 허브 간 물품 이동
4. 업체 배송 담당자가 최종 허브에서 수령 업체까지 배송
5. 각 단계별 슬랙 알림 발송

### 주요 특징
- **MSA 아키텍처**: 각 도메인을 독립적인 서비스로 분리하여 개발 및 배포
- **서비스 디스커버리 & API Gateway**: Spring Cloud Eureka와 Gateway를 통해 서비스 등록/탐색 및 중앙 인증 처리
- **서비스 간 통신**: OpenFeign(동기)과 Kafka(비동기)를 활용하여 서비스 간 느슨한 결합 유지
- **배송 자동화**: 주문 생성 시 허브 간 배송 경로 자동 생성 및 담당자 순차 배정
- **AI 연동**: OpenAI API를 활용한 최종 발송 시한 계산 및 슬랙 알림 자동 발송
- **공통 모듈**: 예외 처리, 응답 형식 등 공통 로직을 별도 모듈로 분리하여 중복 최소화

---

## 👥 팀원 역할 분담

| 이름 | 담당 업무 및 기능 | GitHub |
|------|------------|----------|
| 김민지 | 업체, 배송, 배송 담당자 배정 | [M1Nj1M](https://github.com/M1Nj1M) |
| 김하진 | 상품, 주문, Docker 환경 구성 | [rlaxxwls13](https://github.com/rlaxxwls13) |
| 나웅철 | 인증인가, 사용자, API게이트웨이 | [No-366](https://github.com/No-366) |
| 조하연 | 허브, 허브 경로 | [gelong25](https://github.com/gelong25) |
| 최지원 | AI, 알림, 메시지 큐 | [ji-circle](https://github.com/ji-circle) |

---

## 🏗️ 서비스 구성

| 서비스 | 포트 | 설명 |
|--------|------|------|
| eureka-server | 8761 | 서비스 디스커버리 |
| api-gateway | 8000 | API 게이트웨이, JWT 인증 필터 |
| auth-service | 8086 | 로그인/로그아웃/토큰 갱신 |
| user-service | 8087 | 회원가입/사용자 관리 |
| company-service | 8081 | 업체 관리 |
| delivery-service | 8082 | 배송 관리 |
| notification-service | 8085 | 슬랙 알림 |
| hub-server | 8090 | 허브 관리 |
| product-service | 8083 | 상품 관리 |
| order-service | 8084 | 주문 관리 |
| ai-service | 8088 | AI 분석 (OpenAI, Naver API) |

---

## Architecture Diagram

<img width="1906" height="1642" alt="image" src="https://github.com/user-attachments/assets/3b93ac6b-efbb-40c7-a28a-6659baa51348" />

---
## 📁 프로젝트 구조

멀티모듈 Gradle 프로젝트로 구성되어 있으며, 공통 모듈을 통해 중복 코드를 최소화했습니다.
```
Tenten/
├── common/                   # 공통 모듈 (예외 처리, 응답 형식, 유틸리티)
├── eureka-server/            # 서비스 디스커버리
├── api-gateway/              # 인증 필터, 라우팅
├── auth-service/             # 인증/인가
├── user-service/             # 사용자 관리
├── hub-server/               # 허브 관리
├── company-service/          # 업체 관리
├── product-service/          # 상품 관리
├── order-service/            # 주문 관리
├── delivery-service/         # 배송 관리
├── notification-service/     # 슬랙 알림
├── ai-service/               # AI 연동
├── docker-compose.yml
└── scripts/
    └── init.sql              # DB 초기화 스크립트
```

각 서비스는 DDD(Domain-Driven Design) 기반의 레이어드 아키텍처로 구성되어 있습니다.
```
{service}/
└── src/main/java/
    └── com/team/{service}/
        ├── application/      # 유스케이스, 서비스 로직
        ├── domain/           # 엔티티, 도메인 모델, 레포지토리 인터페이스
        ├── infrastructure/   # JPA 구현체, 외부 API 클라이언트 (Feign 등)
        └── presentation/     # 컨트롤러, 요청/응답 DTO
```
---

## ⚙️ 서비스 실행 방법

### 사전 요구사항

- Java 17
- Docker & Docker Compose
- Gradle

### 환경 변수 설정

루트 디렉토리에 `.env` 파일 생성:

```
DB_USERNAME=
DB_PASSWORD=
JPA_DDL_AUTO=
JPA_SHOW_SQL=
JWT_SECRET=
REDIS_PASSWORD=
SLACK_BOT_TOKEN=
SLACK_WEBHOOK_URL=
NAVER_API_CLIENT_KEY=
NAVER_API_CLIENT_SECRET=
OPENAI_API_KEY=
KAFKA_EXTERNAL_HOST=
```

### 빌드 및 실행

```bash
# 1. JAR 빌드 (테스트 제외)
./gradlew build

# 2. Docker Compose 실행
docker-compose up --build -d

# 3. 서비스 상태 확인
docker-compose ps
```

### Zipkin (분산 추적)

```bash
docker run -d -p 9411:9411 openzipkin/zipkin
```

접속: http://localhost:9411

---

## 🛢️ ERD

* User: `p_user`, `p_hub_user`, `p_company_user`
* Hub: `p_hub`, `p_hub_route`
* Company: `p_company`
* Product: `p_product`, `p_stock`, `p_stock_history`
* Order: `p_order`, `p_order_item`
* Delivery: `p_delivery`, `p_delivery_route_log`
* Notification: `p_notification`
* AI: `p_ai_analysis`
  
<img width="3830" height="2618" alt="image" src="https://github.com/user-attachments/assets/aa44ca7b-d721-42bb-ac00-0aa0f13c2815" />


---

## 🛠️ 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.5.13 |
| Build | Gradle |
| DB | PostgreSQL 16 |
| Cache | Redis 7 |
| Message Queue | Apache Kafka |
| Service Discovery | Spring Cloud Netflix Eureka |
| API Gateway | Spring Cloud Gateway (WebMVC) |
| Load Balancing | Spring Cloud LoadBalancer |
| ORM | Spring Data JPA, Hibernate |
| DB Migration | Flyway |
| Inter-service Communication | OpenFeign |
| Auth | JWT (jjwt) |
| Distributed Tracing | Zipkin |
| Container | Docker, Docker Compose |
| API Docs | Spring REST Docs |
| External API | OpenAI, Naver, Slack, TMAP |

---

## 📡 API 문서

각 서비스 실행 후 아래 경로에서 확인 가능합니다.

| 서비스 | URL |
|--------|-----|
| user-service | http://localhost:8087/docs/index.html |
| auth-service | http://localhost:8086/docs/index.html |
| company-service | http://localhost:8081/docs/index.html |
| delivery-service | http://localhost:8082/docs/index.html |
| product-service | http://localhost:8083/docs/index.html |
| hub-server | http://localhost:8090/docs/index.html |
| order-service | http://localhost:8084/docs/index.html |
| notification-service | http://localhost:8085/docs/index.html |
| ai-service | http://localhost:8088/docs/index.html |

---

## 🔑 기본 테스트 흐름

```bash
# 1. 회원가입
POST http://localhost:8000/users/api/v1/users/signup

# 2. 로그인 (accessToken 발급)
POST http://localhost:8000/auth/api/v1/auth/login

# 3. 인증이 필요한 API 호출
GET http://localhost:8000/products/api/v1/products
Authorization: Bearer {accessToken}
```
