---
name: springboot-api-testing
description: Chuyên gia kiểm thử API và review code cho dự án Spring Boot "Hôm Nay Ăn Gì". Kích hoạt skill này khi người dùng yêu cầu viết kịch bản test cURL, kiểm thử Controller/Service, tạo file Postman, hoặc xử lý các ca kiểm thử Security, Validation và Exception.
---

# Spring Boot API Testing Skill - Dự án "Hôm Nay Ăn Gì"

## 1. API Standards & Security Rules

- **Base URL**: `http://localhost:8080/api/v1`
- **Security Strategy**: JWT Authentication (`Bearer ${JWT_TOKEN}`)
- **Public Endpoints (Không cần Token)**:
    - `POST /api/v1/users` (Đăng ký)
    - `POST /api/v1/auth/token` (Đăng nhập)
    - `POST /api/v1/auth/introspect` (Kiểm tra Token)
    - `POST /api/v1/auth/send-otp`, `POST /api/v1/auth/verify-otp`, `POST /api/v1/auth/forgot-password`, `POST /api/v1/auth/reset-password`
    - Swagger UI: `/v3/api-docs/**`, `/swagger-ui/**`
- **Authenticated Endpoints (Cần Bearer Token)**:
    - `/api/v1/users/me`, `/api/v1/tags/**`, `/api/v1/swipes/**`, `/api/v1/friendships/**`, `/api/v1/rooms/**`, `/api/v1/expenses/**`
- **Admin Only Endpoints**:
    - Tất cả các endpoint còn lại thuộc `/api/v1/**` yêu cầu role `ADMIN` (`hasRole('ADMIN')`).

- **Cấu trúc DTO Response chuẩn (`ApiResponse<T>`)**:
  ```json
  {
    "code": 1000,
    "message": "Thành công",
    "result": { ... }
  }