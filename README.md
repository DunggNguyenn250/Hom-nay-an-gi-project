# 🍜 Hôm Nay Ăn Gì? — Back-end API

> **"Hôm Nay Ăn Gì?"** là ứng dụng giúp các nhóm bạn đồng thuận chọn quán ăn theo cơ chế vuốt thẻ kiểu Tinder, sau đó tự động chia hóa đơn minh bạch. Tài liệu này mô tả toàn bộ kiến trúc và thiết kế phần **Back-end (REST API)**.

---

## 📋 Mục lục

- [Tech Stack](#-tech-stack)
- [Kiến trúc tổng quan](#-kiến-trúc-tổng-quan)
- [Cấu trúc thư mục](#-cấu-trúc-thư-mục)
- [Cơ sở dữ liệu](#-cơ-sở-dữ-liệu)
- [Các Module đã triển khai](#-các-module-đã-triển-khai)
  - [Module 1 — Người dùng & Xác thực](#module-1--người-dùng--xác-thực)
  - [Module 2 — Địa điểm ăn uống](#module-2--địa-điểm-ăn-uống)
  - [Module 3 — Phòng ăn & Quẹt thẻ](#module-3--phòng-ăn--quẹt-thẻ)
  - [Module 3b — Bạn bè & Sở thích](#module-3b--bạn-bè--sở-thích)
  - [Module 4 — Tài chính & Chia tiền](#module-4--tài-chính--chia-tiền)
- [API Reference đầy đủ](#-api-reference-đầy-đủ)
- [Cấu hình & Khởi động](#-cấu-hình--khởi-động)
- [Database Migration Guide](#-database-migration-guide)
- [Ghi chú kỹ thuật](#-ghi-chú-kỹ-thuật)
- [Lộ trình phát triển tiếp theo](#-lộ-trình-phát-triển-tiếp-theo)

---

## 🛠 Tech Stack

| Thành phần | Công nghệ | Phiên bản |
|---|---|---|
| **Ngôn ngữ** | Java | 17 |
| **Framework** | Spring Boot | 3.4.3 |
| **ORM** | Spring Data JPA (Hibernate) | — |
| **Bảo mật** | Spring Security + JWT (OAuth2 Resource Server) | — |
| **Cơ sở dữ liệu chính** | PostgreSQL | — |
| **Migration DB** | Flyway | — |
| **Cache / Queue** | Redis | — |
| **Mapping DTO** | MapStruct | 1.6.3 |
| **Giảm boilerplate** | Lombok | 1.18.30 |
| **Tiện ích** | Apache Commons Lang3 | — |
| **Build tool** | Maven | — |
| **Dev server** | Spring Boot DevTools | — |
| **Monitoring** | Spring Boot Actuator | — |

---

## 🏛 Kiến trúc tổng quan

```
Client (React / Mobile)
        │  HTTP/REST  (JWT Bearer Token)
        ▼
┌─────────────────────────────────────────────────┐
│              Controller Layer                    │
│  (AuthenticationController, RoomController, ...) │
└─────────────────┬───────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────┐
│               Service Layer                      │
│  (Business Logic, Validation, Calculation)       │
└─────────────────┬───────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────┐
│             Repository Layer                     │
│  (Spring Data JPA + JPQL custom queries)         │
└─────────────────┬───────────────────────────────┘
                  │
        ┌─────────┴──────────┐
        ▼                    ▼
   PostgreSQL             Redis
  (Dữ liệu chính)    (Queue / Cache)
```

**Luồng dữ liệu:** `Request → Controller → Service → Repository → DB → Mapper → Response DTO → Client`

---

## 📁 Cấu trúc thư mục

```
src/main/java/org/example/homnayangi/
│
├── controller/          # Tiếp nhận HTTP request, gọi Service
│   ├── AuthenticationController.java
│   ├── ExpenseController.java
│   ├── FriendshipController.java
│   ├── PlaceController.java
│   ├── RoomController.java
│   ├── SwipeController.java
│   ├── TagController.java
│   └── UserController.java
│
├── service/             # Business Logic
│   ├── AuthenticationService.java
│   ├── ExpenseService.java
│   ├── FriendshipService.java
│   ├── PlaceService.java
│   ├── RoomService.java
│   ├── SwipeService.java
│   ├── TagService.java
│   └── UserService.java
│
├── repository/          # Giao tiếp với DB (Spring Data JPA)
│   ├── ExpenseRepository.java
│   ├── ExpenseItemRepository.java
│   ├── ExpenseSplitRepository.java
│   ├── ItemSharerRepository.java
│   ├── FriendshipRepository.java
│   ├── PlaceRepository.java
│   ├── RoomRepository.java
│   ├── RoomMemberRepository.java
│   ├── SwipeRepository.java
│   ├── TagRepository.java
│   ├── UserRepository.java
│   └── UserTagRepository.java
│
├── entity/              # JPA Entities ánh xạ bảng DB
│   ├── Expense.java
│   ├── ExpenseItem.java
│   ├── ExpenseSplit.java  + ExpenseSplitId.java
│   ├── ItemSharer.java    + ItemSharerId.java
│   ├── Friendship.java    + FriendshipId.java
│   ├── Place.java
│   ├── Room.java
│   ├── RoomMember.java    + RoomMemberId.java
│   ├── Swipe.java
│   ├── Tag.java
│   ├── User.java
│   └── UserTag.java       + UserTagId.java
│
├── dto/
│   ├── request/         # DTO nhận dữ liệu từ client
│   └── response/        # DTO trả về cho client
│
├── mapper/              # MapStruct (Entity <-> DTO)
│   ├── ExpenseMapper.java
│   ├── FriendshipMapper.java
│   ├── PlaceMapper.java
│   ├── RoomMapper.java
│   ├── TagMapper.java
│   └── UserMapper.java
│
├── enums/               # Các kiểu liệt kê dùng toàn hệ thống
│   ├── FriendshipStatus.java  (PENDING, ACCEPTED, BLOCKED)
│   ├── Role.java              (ROLE_GUEST, ROLE_USER, ROLE_ADMIN)
│   ├── RoomStatus.java        (OPEN, SWIPING, CLOSED)
│   ├── SplitType.java         (EVENLY, BY_ITEM)
│   └── UserStatus.java        (ACTIVE, BANNED, SUSPENDED)
│
└── exception/           # Xử lý lỗi tập trung
    ├── AppException.java
    ├── ErrorCode.java
    └── GlobalExceptionHandler.java

src/main/resources/
├── application.yaml
└── db/migration/
    ├── V1__init_database.sql
    └── V2__add_split_type_to_expenses.sql
```

---

## 🗄 Cơ sở dữ liệu

### Quan hệ giữa các bảng

```
users
  ├─ 1:N ──> user_roles
  ├─ 1:N ──> friendships (requester / addressee)
  ├─ 1:N ──> user_tags ──> tags
  ├─ 1:N ──> room_members ──> rooms
  │              rooms ──> expenses
  │                  expenses ──> expense_items ──> item_sharers ──> users
  │                  expenses ──> expense_splits ──────────────────> users
  └─ 1:N ──> swipes ──> places
```

### Mô tả các bảng

| Bảng | Khóa chính | Mô tả |
|---|---|---|
| `users` | UUID | Thông tin tài khoản người dùng |
| `user_roles` | (user_id, role) | Phân quyền linh hoạt |
| `friendships` | (requester_id, addressee_id) | Quan hệ bạn bè với trạng thái |
| `tags` | UUID | Danh mục sở thích / tâm trạng |
| `user_tags` | (user_id, tag_id) | Sở thích của người dùng |
| `places` | UUID | Kho dữ liệu quán ăn với tọa độ GPS |
| `rooms` | UUID | Phòng ăn nhóm (OPEN→SWIPING→CLOSED) |
| `room_members` | (room_id, user_id) | Thành viên trong phòng |
| `swipes` | UUID | Lịch sử vuốt thẻ, UNIQUE(room, user, place) |
| `expenses` | UUID | Hóa đơn tổng của cả bàn |
| `expense_items` | UUID | Từng món ăn trong hóa đơn |
| `item_sharers` | (item_id, user_id) | Ai ăn món nào |
| `expense_splits` | (expense_id, user_id) | Số tiền nợ cuối của từng người |

---

## 📦 Các Module đã triển khai

---

### Module 1 — Người dùng & Xác thực

**Mục đích:** Quản lý vòng đời tài khoản và cấp phát JWT token.

#### API Endpoints

| Method | URL | Mô tả | Auth |
|---|---|---|---|
| `POST` | `/api/v1/users` | Đăng ký tài khoản mới | ❌ Public |
| `GET` | `/api/v1/users` | Danh sách tất cả users | ✅ |
| `GET` | `/api/v1/users/my-info` | Thông tin bản thân | ✅ |
| `GET` | `/api/v1/users/{userId}` | Thông tin user theo ID | ✅ |
| `PUT` | `/api/v1/users/{userId}` | Cập nhật thông tin cá nhân | ✅ |
| `DELETE` | `/api/v1/users/{userId}` | Xóa tài khoản | ✅ |
| `POST` | `/api/v1/auth/token` | Đăng nhập → nhận JWT | ❌ Public |
| `POST` | `/api/v1/auth/introspect` | Kiểm tra tính hợp lệ token | ❌ Public |

**Luồng xác thực:**
```
POST /auth/token { username, password }
  → Xác thực BCrypt hash
  → Cấp JWT (HS512, ký bằng JWT_SIGNER_KEY)
  → Mọi request tiếp theo gửi kèm: Authorization: Bearer {token}
```

---

### Module 2 — Địa điểm ăn uống

**Mục đích:** Quản lý kho dữ liệu quán ăn — tên, phân loại, mức giá, tọa độ GPS.

#### Entity: `Place`
`id · name · category · priceRange (1/2/3) · imageUrl · address · latitude · longitude`

#### API Endpoints

| Method | URL | Mô tả | Auth |
|---|---|---|---|
| `POST` | `/api/v1/places` | Thêm quán ăn mới | ✅ |
| `GET` | `/api/v1/places` | Danh sách tất cả quán | ✅ |
| `GET` | `/api/v1/places/{placeId}` | Chi tiết một quán | ✅ |
| `PUT` | `/api/v1/places/{placeId}` | Cập nhật thông tin quán | ✅ |
| `DELETE` | `/api/v1/places/{placeId}` | Xóa quán ăn | ✅ |

---

### Module 3 — Phòng ăn & Quẹt thẻ

**Mục đích:** Quản lý vòng đời phòng nhóm và ghi nhận lịch sử vuốt thẻ (Like/Dislike).

#### Vòng đời phòng
```
OPEN ──(Host startSwiping)──> SWIPING ──(100% Like / Deadlock)──> CLOSED
```

#### API Endpoints

| Method | URL | Mô tả | Auth |
|---|---|---|---|
| `POST` | `/api/v1/rooms` | Tạo phòng mới (Host tự động là thành viên, isReady=true) | ✅ |
| `POST` | `/api/v1/rooms/join` | Tham gia phòng bằng roomCode | ✅ |
| `POST` | `/api/v1/rooms/{roomId}/start` | Bắt đầu quẹt thẻ (chỉ Host) | ✅ |
| `POST` | `/api/v1/swipes` | Ghi nhận lượt quẹt (like/dislike) | ✅ |

**Logic Matching:**
- Khi tổng `is_like = true` của một `place_id` **= tổng số thành viên** → phòng tự CLOSED, ghi `matched_place_id`
- UNIQUE(room_id, user_id, place_id) ở DB ngăn spam click

---

### Module 3b — Bạn bè & Sở thích

**Mục đích:** Hệ thống kết bạn và tag sở thích phục vụ thuật toán ghép phòng.

#### Tính năng đặc biệt — Auto-Accept
> Nếu A đã gửi lời mời cho C (PENDING), C chủ động gửi lại cho A → hệ thống **tự động chấp nhận** và trả về ACCEPTED ngay lập tức, không cần bước `accept` riêng.

#### API Endpoints — Bạn bè

| Method | URL | Mô tả | Auth |
|---|---|---|---|
| `POST` | `/api/v1/friendships/request/{addresseeId}` | Gửi lời mời kết bạn | ✅ |
| `PUT` | `/api/v1/friendships/accept/{requesterId}` | Chấp nhận lời mời | ✅ |
| `DELETE` | `/api/v1/friendships/{targetId}` | Hủy kết bạn / Từ chối | ✅ |
| `GET` | `/api/v1/friendships` | Danh sách bạn bè (ACCEPTED) | ✅ |
| `GET` | `/api/v1/friendships/pending/incoming` | Lời mời đang nhận được | ✅ |
| `GET` | `/api/v1/friendships/pending/outgoing` | Lời mời đã gửi đi | ✅ |

#### API Endpoints — Sở thích (Tags)

| Method | URL | Mô tả | Auth |
|---|---|---|---|
| `POST` | `/api/v1/tags` | Tạo tag mới trong hệ thống | ✅ |
| `GET` | `/api/v1/tags` | Lấy tất cả tags | ✅ |
| `POST` | `/api/v1/tags/user/{tagId}?temporary=` | Thêm sở thích vào hồ sơ | ✅ |
| `DELETE` | `/api/v1/tags/user/{tagId}` | Xóa sở thích | ✅ |
| `GET` | `/api/v1/tags/user` | Xem sở thích của bản thân | ✅ |

---

### Module 4 — Tài chính & Chia tiền

**Mục đích:** Tạo và quản lý hóa đơn sau khi nhóm đã chốt quán (phòng CLOSED), phân bổ số tiền từng người nợ, theo dõi trạng thái thanh toán.

#### Entities & Quan hệ

```
Expense (1) ──────────────> (N) ExpenseSplit  [ai nợ bao nhiêu]
    │
    └──(1)──> (N) ExpenseItem ──> (N) ItemSharer  [ai chia sẻ món nào]
```

#### Hai kiểu chia tiền (`SplitType`)

**`EVENLY` — Chia đều:**
```
amount_owed = totalAmount ÷ số thành viên  (làm tròn xuống 2 chữ số)
Phần dư → cộng vào người đầu tiên trong danh sách
```

**`BY_ITEM` — Chia theo món:**
```
Với mỗi ExpenseItem:
    pricePerPerson = price ÷ số ItemSharer của món đó
    → Cộng dồn tất cả món vào từng người → ra amount_owed
```

#### API Endpoints

| Method | URL | Mô tả | Phân quyền |
|---|---|---|---|
| `POST` | `/api/v1/expenses` | Tạo hóa đơn mới | Thành viên phòng |
| `GET` | `/api/v1/expenses/{expenseId}` | Chi tiết hóa đơn | ✅ |
| `GET` | `/api/v1/expenses/room/{roomId}` | Tất cả HĐ của phòng | ✅ |
| `GET` | `/api/v1/expenses/my-debts` | Khoản nợ chưa trả của tôi | ✅ |
| `PATCH` | `/api/v1/expenses/{id}/splits/{userId}/pay` | Xác nhận 1 người đã trả | Payer hoặc chính người nợ |
| `PATCH` | `/api/v1/expenses/{id}/pay-all` | Xác nhận tất cả đã trả | Chỉ Payer |

#### Request body — Tạo hóa đơn

```json
{
  "roomId": "uuid-cua-phong",
  "payerId": "uuid-nguoi-tra-truoc",
  "totalAmount": 350000,
  "splitType": "BY_ITEM",
  "items": [
    {
      "itemName": "Lẩu Thái",
      "price": 300000,
      "sharerIds": ["uuid-a", "uuid-b", "uuid-c"]
    },
    {
      "itemName": "Trà đá",
      "price": 50000,
      "sharerIds": []
    }
  ]
}
```

> **Lưu ý:** `sharerIds` rỗng `[]` hoặc `null` → hệ thống tự gán **tất cả thành viên phòng** là sharer của món đó.

#### Validation bắt buộc

| Điều kiện vi phạm | Error Code |
|---|---|
| Phòng chưa ở trạng thái `CLOSED` | `3003` |
| Phòng đã có hóa đơn rồi | `3004` |
| Tổng `items.price` ≠ `totalAmount` | `3002` |
| Sharer không phải thành viên phòng | `3009` |

#### Response mẫu

```json
{
  "code": 1000,
  "message": "Thành công",
  "result": {
    "id": "uuid-hoa-don",
    "room": { "roomCode": "ABC123", "status": "CLOSED" },
    "payer": { "username": "alice" },
    "totalAmount": 350000,
    "splitType": "BY_ITEM",
    "createdAt": "2026-10-03T09:00:00",
    "fullyPaid": false,
    "items": [
      {
        "itemName": "Lẩu Thái",
        "price": 300000,
        "sharerCount": 3,
        "pricePerPerson": 100000
      }
    ],
    "splits": [
      { "user": { "username": "alice" }, "amountOwed": 100000, "paid": false },
      { "user": { "username": "bob" },   "amountOwed": 100000, "paid": false },
      { "user": { "username": "carol" }, "amountOwed": 150000, "paid": false }
    ]
  }
}
```

---

## 📡 API Reference đầy đủ

### Chuẩn Response

```json
{
  "code": 1000,
  "message": "Thành công",
  "result": { }
}
```

### Bảng Error Codes đầy đủ

| Code | Tên | HTTP | Ý nghĩa |
|---|---|---|---|
| `9999` | `UNCATEGORIZED_EXCEPTION` | 500 | Lỗi hệ thống không xác định |
| `1002` | `UNAUTHENTICATED` | 401 | Chưa đăng nhập / token hết hạn |
| `1003` | `UNAUTHORIZED` | 403 | Không có quyền thực hiện |
| `1004` | `USER_NOT_EXISTED` | 404 | Người dùng không tồn tại |
| `1005` | `USER_EXISTED` | 400 | Username/email đã tồn tại |
| `1006` | `LOGIN_FAILED` | 401 | Sai tài khoản hoặc mật khẩu |
| `1101` | `USERNAME_INVALID` | 400 | Username phải ≥ 3 ký tự |
| `1102` | `PASSWORD_INVALID` | 400 | Password phải ≥ 6 ký tự |
| `1103` | `EMAIL_INVALID` | 400 | Sai định dạng email |
| `1201` | `PLACE_NOT_FOUND` | 404 | Địa điểm không tồn tại |
| `1202` | `PLACE_NAME_INVALID` | 400 | Tên địa điểm không được rỗng |
| `1203` | `PLACE_LATITUDE_INVALID` | 400 | Vĩ độ không hợp lệ |
| `1204` | `PLACE_LONGITUDE_INVALID` | 400 | Kinh độ không hợp lệ |
| `2001` | `ROOM_NOT_FOUND` | 404 | Không tìm thấy phòng ăn |
| `2002` | `ROOM_FULL` | 400 | Phòng đã đủ thành viên |
| `2003` | `ROOM_CLOSED` | 400 | Phòng đã đóng |
| `2004` | `NOT_ROOM_HOST` | 403 | Chỉ Host mới có quyền |
| `2005` | `ROOM_NOT_OPEN` | 400 | Phòng không còn mở để tham gia |
| `2006` | `USER_ALREADY_IN_ROOM` | 400 | Bạn đã ở trong phòng này rồi |
| `2008` | `ROOM_NOT_SWIPING` | 400 | Phòng chưa bắt đầu quẹt thẻ |
| `3001` | `EXPENSE_NOT_FOUND` | 404 | Không tìm thấy hóa đơn |
| `3002` | `BILL_AMOUNT_MISMATCH` | 400 | Tổng tiền items không khớp tổng HĐ |
| `3003` | `ROOM_NOT_CLOSED` | 400 | Phòng phải CLOSED mới tạo hóa đơn |
| `3004` | `EXPENSE_ALREADY_EXISTS` | 400 | Phòng đã có hóa đơn rồi |
| `3005` | `SPLIT_NOT_FOUND` | 404 | Không tìm thấy khoản chia tiền |
| `3006` | `ALREADY_PAID` | 400 | Khoản này đã được thanh toán |
| `3007` | `NOT_EXPENSE_PAYER` | 403 | Chỉ người trả tiền mới có quyền |
| `3008` | `INVALID_SPLIT_TYPE` | 400 | Kiểu chia không hợp lệ |
| `3009` | `SHARER_NOT_IN_ROOM` | 400 | Sharer phải là thành viên phòng |
| `1301` | `FRIENDSHIP_ALREADY_EXISTS` | 400 | Quan hệ bạn bè đã tồn tại |
| `1302` | `FRIENDSHIP_NOT_FOUND` | 404 | Không tìm thấy quan hệ bạn bè |
| `1303` | `CANNOT_FRIEND_SELF` | 400 | Không thể tự kết bạn với mình |
| `1304` | `TAG_ALREADY_EXISTS` | 400 | Tag đã tồn tại |
| `1305` | `TAG_NOT_FOUND` | 404 | Tag không tồn tại |
| `1306` | `CANNOT_BLOCK_SELF` | 400 | Không thể tự chặn mình |
| `4001` | `EXTERNAL_SERVICE_ERROR` | 503 | Lỗi dịch vụ bên ngoài |

---

## ⚙️ Cấu hình & Khởi động

### Biến môi trường

```env
# Server
SERVER_PORT=8080

# PostgreSQL
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/homnayangi
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=yourpassword
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver

# JPA / Hibernate
SPRING_JPA_HIBERNATE_NAMING_PHYSICAL_STRATEGY=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_JPA_SHOW_SQL=true

# Flyway
SPRING_FLYWAY_ENABLED=true
SPRING_FLYWAY_BASELINE_ON_MIGRATE=true
SPRING_FLYWAY_BASELINE_VERSION=0
SPRING_FLYWAY_LOCATIONS=classpath:db/migration

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_SSL_ENABLED=false

# JWT (khóa HS512, tối thiểu 256 bit)
JWT_SIGNER_KEY=your-very-secret-key-at-least-256-bits-long
```

### Lệnh khởi động

```bash
# Chạy dev với Maven wrapper
./mvnw spring-boot:run

# Build JAR
./mvnw clean package -DskipTests

# Chạy JAR
java -jar target/Homnayangi-0.0.1-SNAPSHOT.jar
```

> Flyway sẽ **tự động** chạy `V1__init_database.sql` và `V2__add_split_type_to_expenses.sql` khi ứng dụng khởi động lần đầu.

---

## 📝 Ghi chú kỹ thuật

### Chống N+1 Problem
Các query phức tạp dùng `JOIN FETCH` để load tất cả trong một lần:
```java
@Query("""
    SELECT DISTINCT e FROM Expense e
    LEFT JOIN FETCH e.splits s
    LEFT JOIN FETCH s.user
    WHERE e.room.id = :roomId
""")
List<Expense> findAllByRoomIdWithDetails(@Param("roomId") UUID roomId);
```

### Composite Primary Keys
Các bảng junction dùng `@EmbeddedId` với class implements `Serializable`:

| Class | Fields |
|---|---|
| `ExpenseSplitId` | expenseId, userId |
| `ItemSharerId` | expenseItemId, userId |
| `FriendshipId` | requesterId, addresseeId |
| `RoomMemberId` | roomId, userId |
| `UserTagId` | userId, tagId |

### Xử lý phần dư khi chia đều
Khi `totalAmount % memberCount != 0`, phần dư được cộng vào người đầu tiên để tổng luôn khớp với `totalAmount` không mất một đồng nào.

---

## 🗺 Lộ trình phát triển tiếp theo

| Tính năng | Ưu tiên | Ghi chú |
|---|---|---|
| **WebSocket / STOMP** | 🔴 Cao | Đồng bộ realtime khi quẹt thẻ, broadcast match result |
| **Tích hợp VietQR** | 🟠 Trung bình | Sinh mã QR thanh toán từ `amount_owed` |
| **Ghép phòng ngẫu nhiên** | 🟠 Trung bình | Redis Queue + Matchmaking Worker dựa trên `user_tags` |
| **Mời bạn qua WebSocket** | 🟠 Trung bình | Host bấm mời → bạn nhận popup realtime |
| **Advanced Room Filters** | 🟡 Thấp | Lọc quán theo bán kính GPS, mức giá |
| **Rating & Reviews** | 🟡 Thấp | Đánh giá quán sau khi đi ăn |
| **AI Recommendation** | 🟢 Tương lai | Collaborative Filtering từ lịch sử `swipes` |

---

## 🗃 Database Migration Guide

> Nội dung từ `Migration_Guide.md` — Quy chuẩn bắt buộc khi làm việc với Database.

### Tại sao dùng Flyway Migration?

**❌ Cách cũ (nguy hiểm):**
- Mỗi người tự mở pgAdmin/DBeaver gõ lệnh tạo bảng bằng tay
- Để `ddl-auto: update` cho Hibernate tự sửa ngầm
- **Hậu quả:** Máy người này chạy được nhưng máy người kia thiếu cột/lỗi bảng, deploy lên server không biết database đang ở version nào, dễ mất dữ liệu thật

**✅ Cách chuẩn với Flyway Migration:**
- Mọi thay đổi cấu trúc DB đều viết thành file SQL đánh số phiên bản (`V1__...`, `V2__...`) và lưu vào Git
- Khi bất kỳ ai `git pull` code mới về và bấm **Run**, Spring Boot sẽ **tự động chạy các file SQL mới** để đồng bộ database
- Cả team và server production sẽ luôn có database **giống nhau 100%**

---

### 🚀 Hướng dẫn chạy dự án lần đầu (thành viên mới)

**Yêu cầu môi trường:**
- JDK 17 (LTS)
- PostgreSQL 14+
- IntelliJ IDEA (khuyên dùng) hoặc VS Code

**Các bước cài đặt:**

```bash
# 1. Clone repository
git clone <repo_url>
cd Hom-nay-an-gi-project
```

```sql
-- 2. Tạo Database trên PostgreSQL
CREATE DATABASE homnayangi_db;
```

```bash
# 3. Cấu hình biến môi trường (xem mục Cấu hình & Khởi động)
# 4. Khởi động ứng dụng
cd BE
.\mvnw.cmd spring-boot:run
```

> **Kết quả:** Ngay khi ứng dụng chạy xong, mở database `homnayangi_db` bạn sẽ thấy toàn bộ các bảng và bảng lịch sử `flyway_schema_history` đã được tạo tự động!

---

### ⚙️ Quy trình làm việc khi thêm/sửa Database

Bất cứ khi nào cần **tạo bảng mới, thêm cột, đổi kiểu dữ liệu, tạo khóa ngoại**, làm theo đúng 3 bước:

#### Bước 1: Tạo file Migration mới

Vào thư mục: `BE/src/main/resources/db/migration/`

Tạo một file `.sql` mới có số version tiếp theo.

> **⚠️ Quy tắc đặt tên file (CỰC KỲ QUAN TRỌNG):**
> - Cú pháp: `V<SỐ_version>__<mô_tả_ngắn_gọn>.sql`
> - Bắt đầu bằng chữ **`V`** in hoa
> - Số phiên bản tăng dần: `V1`, `V2`, `V3`,...
> - **BẮT BUỘC có đúng 2 DẤU GẠCH DƯỚI `__`** trước phần mô tả
> - Ví dụ hợp lệ:
>   - `V2__add_split_type_to_expenses.sql`
>   - `V3__create_reviews_table.sql`

#### Bước 2: Viết câu lệnh SQL thuần

```sql
-- Ví dụ: Thêm cột phone_number vào bảng users
ALTER TABLE users ADD COLUMN phone_number VARCHAR(15);
```

#### Bước 3: Cập nhật Entity trong Java rồi Run

1. Vào class Entity tương ứng thêm thuộc tính (ví dụ trong `User.java` thêm `String phoneNumber;`)
2. Bấm **Run** ứng dụng: Flyway sẽ tự phát hiện file mới và áp dụng vào Database!
3. **Commit** file SQL migration cùng với code Java lên Git

---

### ❌ Các điều CẤM KỴ

| Hành động | Hậu quả |  
|---|---|
| **Sửa nội dung file migration cũ đã commit** | Flyway báo lỗi `checksum mismatch`, sập ứng dụng khi khởi động |
| **Xóa bảng `flyway_schema_history`** | Flyway tưởng DB trống, chạy lại từ V1 gây lỗi trùng bảng |
| **2 người cùng tạo file cùng version** | Conflict migration, ai pull sau bị lỗi |

> **Quy tắc vàng:** File đã commit vào Git rồi thì **bất khả xâm phạm**. Muốn sửa gì thì tạo file version tiếp theo.

---

### 🔧 Xử lý sự cố thường gặp (Troubleshooting)

**Lỗi: `Migration checksum mismatch`**
- **Nguyên nhân:** Ai đó đã sửa nội dung file migration cũ sau khi nó đã được thực thi
- **Cách khắc phục (Local/Dev):**
  ```sql
  DROP DATABASE homnayangi_db;
  CREATE DATABASE homnayangi_db;
  ```
  Sau đó chạy lại app để Flyway migrate lại từ đầu.

**Lỗi: `Could not find or load main class`**
- **Nguyên nhân:** IntelliJ chưa biên dịch mã nguồn Java ra thư mục `target/classes`
- **Cách khắc phục:**
  ```bash
  cd BE
  .\mvnw.cmd compile
  ```

**Các file migration hiện có:**

| File | Nội dung |
|---|---|
| `V1__init_database.sql` | Tạo toàn bộ schema ban đầu (13 bảng) |
| `V2__add_split_type_to_expenses.sql` | Thêm cột `split_type` vào bảng `expenses` |

---

## 📝 Ghi chú kỹ thuật

### Chống N+1 Problem
Các query phức tạp dùng `JOIN FETCH` để load tất cả trong một lần:
```java
@Query("""
    SELECT DISTINCT e FROM Expense e
    LEFT JOIN FETCH e.splits s
    LEFT JOIN FETCH s.user
    WHERE e.room.id = :roomId
""")
List<Expense> findAllByRoomIdWithDetails(@Param("roomId") UUID roomId);
```

### Composite Primary Keys
Các bảng junction dùng `@EmbeddedId` với class implements `Serializable`:

| Class | Fields |
|---|---|
| `ExpenseSplitId` | expenseId, userId |
| `ItemSharerId` | expenseItemId, userId |
| `FriendshipId` | requesterId, addresseeId |
| `RoomMemberId` | roomId, userId |
| `UserTagId` | userId, tagId |

### Xử lý phần dư khi chia đều
Khi `totalAmount % memberCount != 0`, phần dư được cộng vào người đầu tiên để tổng luôn khớp với `totalAmount` — không mất một đồng nào.

### MapStruct + Lombok tích hợp đúng cách
Dự án dùng `lombok-mapstruct-binding` để đảm bảo annotation processor chạy theo đúng thứ tự:
```xml
<annotationProcessorPaths>
    <path>lombok</path>
    <path>lombok-mapstruct-binding</path>
    <path>mapstruct-processor</path>
</annotationProcessorPaths>
```

---

*Cập nhật lần cuối: 03/10/2026 — Review toàn bộ source code + Migration_Guide.md.*
