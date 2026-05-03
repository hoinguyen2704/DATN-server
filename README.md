# Hozitech Server

Backend API cho Hozitech, xây bằng Spring Boot, JPA, Security, WebSocket và PostgreSQL.

## Tổng Quan

- REST API cho storefront, account, admin và checkout
- JWT authentication + phân quyền user/admin
- i18n tiếng Việt / tiếng Anh
- Email, S3, Excel export, PDF invoice, WebSocket realtime
- Tích hợp payment webhook, voucher, flash sale, ticket/support

## Tech Stack

- Java 25
- Spring Boot 4.0.3
- Spring Data JPA
- Spring Security + JWT
- PostgreSQL
- WebSocket
- Thymeleaf mail template

## Cấu Trúc Chính

```
src/main/java/com/hoz/hozitech/
├── application/   # service, mapper, constant
├── config/        # cors, i18n, jpa, aws, async, exceptions
├── domain/        # entity, dto, enum
├── security/      # JWT, auth filter, security config
└── web/           # controller, base annotation, exception handler
```

## Chạy Local

### Yêu cầu

- Java 25
- Maven wrapper (`./mvnw`)
- PostgreSQL

### Khởi động

```bash
./mvnw spring-boot:run
```

Hoặc build:

```bash
./mvnw clean package
```

## Cấu Hình

Server đọc biến môi trường từ `.env`, `.env.dev`, `server/.env`, `server/.env.dev`.

### Biến bắt buộc

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET_KEY`

### Biến thường dùng

- `SERVER_PORT`
- `ALLOWED_ORIGINS`
- `URL_FRONTEND`
- `APP_TIMEZONE`
- `AWS_ACCESS_KEY`, `AWS_SECRET_KEY`, `AWS_REGION`, `AWS_BUCKET_NAME`
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`
- `VNPAY_*`, `MOMO_WEBHOOK_SECRET`, `BANK_TRANSFER_WEBHOOK_SECRET`

## API Chính

- `/api/v1/auth`
- `/api/v1/products`
- `/api/v1/cart`
- `/api/v1/orders`
- `/api/v1/coupons`
- `/api/v1/flash-sales`
- `/api/v1/tickets`
- `/api/v1/notifications`
- `/api/v1/cms`
- `/api/v1/payments`
- `/admin/api/v1/**`

## Ghi Chú

- `server/skills-lock.json` và các file debug/log không phải phần runtime cốt lõi.
- `src/main/resources/i18n/` chứa message key cho response và lỗi localized.

