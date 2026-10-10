# 📖 API Documentation — Hôm Nay Ăn Gì

> Base URL: `http://localhost:8080`  
> Tất cả response đều được bọc trong wrapper `ApiResponse<T>`:
>
> ```json
> {
>   "code": 1000,
>   "message": "Thành công",
>   "result": { ... }
> }
> ```
>
> Các endpoint có 🔒 yêu cầu header: `Authorization: Bearer <token>`

---

## 📑 Mục lục

- [🔐 Auth](#-auth)
- [👤 Users](#-users)
- [📍 Places](#-places)
- [🏷️ Tags](#️-tags)
- [🤝 Friendships](#-friendships)
- [🏠 Rooms](#-rooms)
- [👆 Swipes](#-swipes)
- [💰 Expenses](#-expenses)

---

## 🔐 Auth

### `POST /api/v1/auth/token`

Đăng nhập, lấy JWT token.

**Request Body:**

```json
{
  "username": "string",
  "password": "string"
}
```

**Response (`result`):**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "authenticated": true
}
```

---

### `POST /api/v1/auth/introspect`

Kiểm tra token có hợp lệ không.

**Request Body:**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Response (`result`):**

```json
{
  "valid": true
}
```

---

## 👤 Users

### `POST /api/v1/users`

Tạo tài khoản mới (đăng ký).

**Request Body:**

```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "123456",
  "avatarUrl": "https://..."
}
```

> Ràng buộc: `username` min 3 ký tự, `email` phải đúng định dạng, `password` min 6 ký tự. `avatarUrl` tùy chọn.

**Response (`result`):**

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "username": "johndoe",
  "email": "john@example.com",
  "avatarUrl": "https://...",
  "status": "ACTIVE",
  "createdAt": "2024-01-01T00:00:00",
  "roles": ["USER"]
}
```

---

### `GET /api/v1/users` 🔒

Lấy danh sách tất cả người dùng.

**Request:** Không có body/params.

**Response (`result`):**

```json
[
  {
    "id": "uuid",
    "username": "johndoe",
    "email": "john@example.com",
    "avatarUrl": "https://...",
    "status": "ACTIVE",
    "createdAt": "2024-01-01T00:00:00",
    "roles": ["USER"]
  }
]
```

---

### `GET /api/v1/users/{userId}` 🔒

Lấy thông tin một người dùng theo ID.

**Path Variable:**
| Tên | Kiểu | Mô tả |
|-----|------|-------|
| `userId` | UUID | ID của người dùng |

**Response (`result`):** Trả về `UserResponse` (xem ví dụ ở trên).

---

### `PUT /api/v1/users/{userId}` 🔒

Cập nhật thông tin người dùng.

**Path Variable:**
| Tên | Kiểu | Mô tả |
|-----|------|-------|
| `userId` | UUID | ID của người dùng |

**Request Body:** (tất cả tùy chọn)

```json
{
  "password": "newpass123",
  "email": "new@example.com",
  "avatarUrl": "https://...",
  "roles": ["USER", "ADMIN"]
}
```

**Response (`result`):** `UserResponse` đã được cập nhật.

---

### `DELETE /api/v1/users/{userId}` 🔒

Xóa người dùng.

**Path Variable:**
| Tên | Kiểu | Mô tả |
|-----|------|-------|
| `userId` | UUID | ID của người dùng |

**Response (`result`):**

```json
"User has been deleted successfully"
```

---

## 📍 Places

### `POST /api/v1/places` 🔒

Tạo địa điểm mới.

**Request Body:**

```json
{
  "name": "Bún bò Huế Mụ Rú",
  "category": "Bún bò",
  "priceRange": 2,
  "imageUrl": "https://...",
  "address": "123 Lê Lợi, Q.1",
  "latitude": 10.776889,
  "longitude": 106.700806
}
```

> Ràng buộc: `name` bắt buộc, `latitude` bắt buộc trong khoảng [-90, 90], `longitude` bắt buộc trong khoảng [-180, 180]. Các trường còn lại tùy chọn.

**Response (`result`):**

```json
{
  "id": "uuid",
  "name": "Bún bò Huế Mụ Rú",
  "category": "Bún bò",
  "priceRange": 2,
  "imageUrl": "https://...",
  "address": "123 Lê Lợi, Q.1",
  "latitude": 10.776889,
  "longitude": 106.700806
}
```

---

### `GET /api/v1/places` 🔒

Lấy danh sách địa điểm. Hỗ trợ tìm kiếm và lọc qua query params.

**Query Params:** (tất cả tùy chọn)
| Tên | Kiểu | Mô tả |
|-----|------|-------|
| `name` | string | Tìm theo tên địa điểm |
| `category` | string | Lọc theo danh mục |
| `latitude` | double | Vĩ độ (cần kết hợp với `longitude` và `radius`) |
| `longitude` | double | Kinh độ |
| `radius` | double | Bán kính tìm kiếm (km) |

**Logic ưu tiên:**

1. Có đủ `latitude` + `longitude` + `radius` → tìm địa điểm gần đó (có thể kết hợp `category`)
2. Có `name` → tìm theo tên
3. Có `category` → lọc theo danh mục
4. Không có gì → trả về tất cả

**Ví dụ URL:**

```
GET /api/v1/places?latitude=10.77&longitude=106.70&radius=5&category=Phở
GET /api/v1/places?name=phở
GET /api/v1/places?category=Cơm tấm
GET /api/v1/places
```

**Response (`result`):** Mảng `PlaceResponse[]`.

---

### `GET /api/v1/places/{placeId}` 🔒

Lấy chi tiết một địa điểm.

**Path Variable:**
| Tên | Kiểu | Mô tả |
|-----|------|-------|
| `placeId` | UUID | ID địa điểm |

**Response (`result`):** `PlaceResponse`.

---

### `PUT /api/v1/places/{placeId}` 🔒

Cập nhật thông tin địa điểm.

**Path Variable:**
| Tên | Kiểu | Mô tả |
|-----|------|-------|
| `placeId` | UUID | ID địa điểm |

**Request Body:** (tất cả tùy chọn)

```json
{
  "name": "Tên mới",
  "category": "Danh mục mới",
  "priceRange": 3,
  "imageUrl": "https://...",
  "address": "Địa chỉ mới",
  "latitude": 10.776889,
  "longitude": 106.700806
}
```

**Response (`result`):** `PlaceResponse` đã cập nhật.

---

### `DELETE /api/v1/places/{placeId}` 🔒

Xóa địa điểm.

**Path Variable:**
| Tên | Kiểu | Mô tả |
|-----|------|-------|
| `placeId` | UUID | ID địa điểm |

**Response (`result`):**

```json
"Place has been deleted successfully"
```

Dưới đây là tài liệu API hoàn chỉnh đã được bổ sung **2 API `PUT**`(Cập nhật Tag hệ thống & Cập nhật Tag cá nhân) và sửa lại các lỗi thiếu dấu gạch chéo`/`ở đường dẫn URL (như`/me{tagId}`$\rightarrow$`/me/{tagId}`).

Bạn có thể sao chép trực tiếp tài liệu này:

---

## 🏷️ Tags

### `POST /api/v1/tags` 🔒

Tạo tag sở thích mới.

**Request Body:**

```json
{
  "tagName": "Ẩm thực Nhật"
}
```

> Ràng buộc: bắt buộc, tối đa 50 ký tự.

**Response (`result`):**

```json
{
  "id": "uuid",
  "tagName": "Ẩm thực Nhật"
}
```

---

### `GET /api/v1/tags` 🔒

Lấy danh sách tất cả tags.

**Response (`result`):**

```json
[
  {
    "id": "uuid",
    "tagName": "Ẩm thực Nhật"
  }
]
```

---

### `GET /api/v1/tags/{id}` 🔒

Lấy thông tin một tag.

**Path Variable:**

| Tên  | Kiểu | Mô tả  |
| ---- | ---- | ------ |
| `id` | UUID | ID tag |

**Response (`result`):** `TagResponse`.

---

### `PUT /api/v1/tags/{id}` 🔒

Cập nhật thông tin tag hệ thống (Dành cho Admin).

**Path Variable:**

| Tên  | Kiểu | Mô tả               |
| ---- | ---- | ------------------- |
| `id` | UUID | ID tag cần cập nhật |

**Request Body:**

```json
{
  "tagName": "Ẩm thực Nhật Bản"
}
```

**Response (`result`):** `TagResponse`.

---

### `DELETE /api/v1/tags/{id}` 🔒

Xóa tag.

**Path Variable:**

| Tên  | Kiểu | Mô tả  |
| ---- | ---- | ------ |
| `id` | UUID | ID tag |

**Response (`result`):**

```json
"Tag has been deleted successfully"
```

---

### `GET /api/v1/tags/user/me` 🔒

Lấy danh sách tags sở thích của người dùng hiện tại (từ JWT token).

**Response (`result`):**

```json
[
  {
    "tag": {
      "id": "uuid",
      "tagName": "Ẩm thực Nhật"
    },
    "temporary": false
  }
]
```

---

### `POST /api/v1/tags/user/me/{tagId}` 🔒

Thêm tag sở thích cho người dùng hiện tại.

**Path Variable:**

| Tên     | Kiểu | Mô tả            |
| ------- | ---- | ---------------- |
| `tagId` | UUID | ID tag muốn thêm |

**Query Params:**

| Tên         | Kiểu    | Mặc định | Mô tả                                    |
| ----------- | ------- | -------- | ---------------------------------------- |
| `temporary` | boolean | `false`  | `true` = sở thích tạm thời cho phiên này |

**Ví dụ:**

```
POST /api/v1/tags/user/me/3fa85f64-5717-4562-b3fc-2c963f66afa6?temporary=true

```

**Response (`result`):**

```json
{
  "tag": {
    "id": "uuid",
    "tagName": "Ẩm thực Nhật"
  },
  "temporary": true
}
```

---

### `PUT /api/v1/tags/user/me/{tagId}` 🔒

Cập nhật trạng thái tag sở thích của người dùng hiện tại.

**Path Variable:**

| Tên     | Kiểu | Mô tả                |
| ------- | ---- | -------------------- |
| `tagId` | UUID | ID tag muốn cập nhật |

**Query Params:**

| Tên         | Kiểu    | Mặc định   | Mô tả                                         |
| ----------- | ------- | ---------- | --------------------------------------------- |
| `temporary` | boolean | _Bắt buộc_ | `true` = sở thích tạm thời, `false` = cố định |

**Ví dụ:**

```
PUT /api/v1/tags/user/me/3fa85f64-5717-4562-b3fc-2c963f66afa6?temporary=false

```

**Response (`result`):**

```json
{
  "tag": {
    "id": "uuid",
    "tagName": "Ẩm thực Nhật"
  },
  "temporary": false
}
```

---

### `DELETE /api/v1/tags/user/me/{tagId}` 🔒

Xóa tag sở thích của người dùng hiện tại.

**Path Variable:**

| Tên     | Kiểu | Mô tả           |
| ------- | ---- | --------------- |
| `tagId` | UUID | ID tag muốn xóa |

**Response (`result`):**

```json
"User tag has been removed successfully"
```

## 🤝 Friendships

### `POST /api/v1/friendships/request/{addresseeId}` 🔒

Gửi lời mời kết bạn.

**Path Variable:**
| Tên | Kiểu | Mô tả |
|-----|------|-------|
| `addresseeId` | UUID | ID người nhận lời mời |

**Request:** Không có body.

**Response (`result`):**

```json
{
  "requester": {
    "id": "uuid",
    "username": "alice",
    "email": "alice@example.com",
    "avatarUrl": "https://...",
    "status": "ACTIVE",
    "createdAt": "2024-01-01T00:00:00",
    "roles": ["USER"]
  },
  "addressee": {
    "id": "uuid",
    "username": "bob",
    "email": "bob@example.com",
    "avatarUrl": null,
    "status": "ACTIVE",
    "createdAt": "2024-01-01T00:00:00",
    "roles": ["USER"]
  },
  "status": "PENDING",
  "createdAt": "2024-01-01T00:00:00"
}
```

> **FriendshipStatus:** `PENDING` | `ACCEPTED` | `DECLINED` | `BLOCKED`

---

### `PUT /api/v1/friendships/accept/{requesterId}` 🔒

Chấp nhận lời mời kết bạn.

**Path Variable:**
| Tên | Kiểu | Mô tả |
|-----|------|-------|
| `requesterId` | UUID | ID người đã gửi lời mời |

**Request:** Không có body.

**Response (`result`):** `FriendshipResponse` với `status: "ACCEPTED"`.

---

### `DELETE /api/v1/friendships/{targetUserId}` 🔒

Từ chối lời mời kết bạn hoặc hủy kết bạn.

**Path Variable:**
| Tên | Kiểu | Mô tả |
|-----|------|-------|
| `targetUserId` | UUID | ID người dùng đối phương |

**Response (`result`):**

```json
"Hủy kết bạn hoặc từ chối yêu cầu thành công"
```

---

### `GET /api/v1/friendships` 🔒

Lấy danh sách bạn bè của người dùng hiện tại.

**Response (`result`):** Mảng `UserResponse[]`.

---

### `GET /api/v1/friendships/pending/incoming` 🔒

Lấy danh sách lời mời kết bạn đến (người khác gửi cho mình).

**Response (`result`):** Mảng `FriendshipResponse[]` với `status: "PENDING"`.

---

### `GET /api/v1/friendships/pending/outgoing` 🔒

Lấy danh sách lời mời kết bạn đi (mình đã gửi cho người khác).

**Response (`result`):** Mảng `FriendshipResponse[]` với `status: "PENDING"`.

---

## 🏠 Rooms

### `POST /api/v1/rooms` 🔒

Tạo phòng mới để chọn địa điểm ăn nhóm.

**Request Body:**

```json
{
  "name": "Nhóm ăn trưa"
}
```

**Response (`result`):**

```json
{
  "id": "uuid",
  "roomCode": "ABC123",
  "host": {
    "id": "uuid",
    "username": "alice",
    "email": "alice@example.com",
    "avatarUrl": null,
    "status": "ACTIVE",
    "createdAt": "2024-01-01T00:00:00",
    "roles": ["USER"]
  },
  "status": "WAITING",
  "matchedPlace": null,
  "createdAt": "2024-01-01T00:00:00"
}
```

> **RoomStatus:** `WAITING` | `SWIPING` | `CLOSED`

---

### `POST /api/v1/rooms/join/{roomCode}` 🔒

Tham gia phòng bằng mã phòng.

**Path Variable:**
| Tên | Kiểu | Mô tả |
|-----|------|-------|
| `roomCode` | string | Mã phòng (vd: `ABC123`) |

**Request:** Không có body.

**Response (`result`):** `RoomResponse` như trên.

---

### `POST /api/v1/rooms/{roomId}/start` 🔒

Bắt đầu phiên swipe (chỉ host mới có quyền).

**Path Variable:**
| Tên | Kiểu | Mô tả |
|-----|------|-------|
| `roomId` | UUID | ID phòng |

**Request:** Không có body.

**Response (`result`):** `null` — chỉ trả về wrapper với code/message thành công.

---

## 👆 Swipes

### `POST /api/v1/swipes` 🔒

Vuốt thích hoặc không thích một địa điểm trong phòng.

**Request Body:**

```json
{
  "roomId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "placeId": "7fa85f64-5717-4562-b3fc-2c963f66afa9",
  "liked": true
}
```

**Response (`result`):** `null`

> **Lưu ý:** Khi tất cả thành viên trong phòng đều `liked = true` cho cùng một địa điểm, phòng tự động chuyển sang `CLOSED` và `matchedPlace` được cập nhật.

---

## 💰 Expenses

### `POST /api/v1/expenses` 🔒

Tạo hóa đơn mới cho phòng đã kết thúc (status = `CLOSED`).

**Request Body:**

```json
{
  "roomId": "uuid",
  "payerId": "uuid",
  "totalAmount": 500000,
  "splitType": "EVENLY",
  "items": [
    {
      "itemName": "Bún bò",
      "price": 60000,
      "sharerIds": ["uuid-user1", "uuid-user2"]
    },
    {
      "itemName": "Cơm tấm",
      "price": 50000,
      "sharerIds": null
    }
  ]
}
```

> **Ràng buộc:**
>
> - `roomId`, `payerId`, `totalAmount` bắt buộc
> - `totalAmount` phải > 0
> - `items` không được rỗng; mỗi item cần `itemName` và `price` > 0
> - `sharerIds` tùy chọn — nếu `null`/rỗng thì tất cả thành viên phòng chia sẻ món đó
>
> **SplitType:**
>
> - `EVENLY` (mặc định): Chia đều tổng tiền cho tất cả thành viên
> - `BY_ITEM`: Mỗi người trả theo những món họ order (`sharerIds`)

**Response (`result`):**

```json
{
  "id": "uuid",
  "room": {
    "id": "uuid",
    "roomCode": "ABC123",
    "host": { ... },
    "status": "CLOSED",
    "matchedPlace": { ... },
    "createdAt": "2024-01-01T00:00:00"
  },
  "payer": {
    "id": "uuid",
    "username": "alice",
    ...
  },
  "totalAmount": 500000,
  "splitType": "EVENLY",
  "createdAt": "2024-01-01T00:00:00",
  "items": [
    {
      "id": "uuid",
      "itemName": "Bún bò",
      "price": 60000,
      "sharerCount": 2,
      "pricePerPerson": 30000
    }
  ],
  "splits": [
    {
      "expenseId": "uuid",
      "user": { "id": "uuid", "username": "bob", ... },
      "amountOwed": 250000,
      "paid": false
    }
  ],
  "fullyPaid": false
}
```

---

### `GET /api/v1/expenses/{expenseId}` 🔒

Lấy chi tiết một hóa đơn.

**Path Variable:**
| Tên | Kiểu | Mô tả |
|-----|------|-------|
| `expenseId` | UUID | ID hóa đơn |

**Response (`result`):** `ExpenseResponse` như trên.

---

### `GET /api/v1/expenses/room/{roomId}` 🔒

Lấy tất cả hóa đơn của một phòng.

**Path Variable:**
| Tên | Kiểu | Mô tả |
|-----|------|-------|
| `roomId` | UUID | ID phòng |

**Response (`result`):** Mảng `ExpenseResponse[]`.

---

### `GET /api/v1/expenses/my-debts` 🔒

Lấy danh sách khoản nợ chưa thanh toán của người dùng hiện tại (từ JWT token).

**Request:** Không có params.

**Response (`result`):**

```json
[
  {
    "expenseId": "uuid",
    "user": {
      "id": "uuid",
      "username": "bob",
      ...
    },
    "amountOwed": 250000,
    "paid": false
  }
]
```

---

### `PATCH /api/v1/expenses/{expenseId}/splits/{userId}/pay` 🔒

Xác nhận một người đã thanh toán khoản của họ (chỉ payer xác nhận).

**Path Variables:**
| Tên | Kiểu | Mô tả |
|-----|------|-------|
| `expenseId` | UUID | ID hóa đơn |
| `userId` | UUID | ID người đã thanh toán |

**Request:** Không có body.

**Response (`result`):**

```json
{
  "expenseId": "uuid",
  "user": { "id": "uuid", "username": "bob", ... },
  "amountOwed": 250000,
  "paid": true
}
```

---

### `PATCH /api/v1/expenses/{expenseId}/pay-all` 🔒

Đánh dấu toàn bộ hóa đơn là đã thanh toán (chỉ payer).

**Path Variable:**
| Tên | Kiểu | Mô tả |
|-----|------|-------|
| `expenseId` | UUID | ID hóa đơn |

**Request:** Không có body.

**Response (`result`):** `ExpenseResponse` với `fullyPaid: true` và tất cả `splits[].paid: true`.

---

## ❌ Error Response

Khi có lỗi, response trả về:

```json
{
  "code": 4001,
  "message": "Mô tả lỗi cụ thể"
}
```

| HTTP Status | Ý nghĩa                                         |
| ----------- | ----------------------------------------------- |
| `400`       | Dữ liệu đầu vào không hợp lệ                    |
| `401`       | Chưa xác thực / Token không hợp lệ hoặc hết hạn |
| `403`       | Không có quyền truy cập                         |
| `404`       | Không tìm thấy tài nguyên                       |
| `500`       | Lỗi server nội bộ                               |
