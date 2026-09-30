
[MODE_UPDATE] plan

Dưới đây là **kịch bản kiểm thử API chi tiết từng bước (API Testing Scenarios)** dành cho Module Bạn bè & Sở thích (`Friendships` & `Tags`). Bạn có thể sử dụng các kịch bản này để import vào Postman, viết script test hoặc test trực tiếp.

---

### BƯỚC CHUẨN BỊ: Đăng ký & Đăng nhập để lấy Token
Để thực hiện kiểm thử các API bảo mật, bạn cần tạo ít nhất 3 tài khoản người dùng và đăng nhập để lấy JWT Token của từng người.

#### 1. Đăng ký tài khoản (User A, User B, User C)
* **API:** `POST /api/v1/users` (Không cần Token)
* **Body (User A):**
  ```json
  {
    "username": "usera",
    "email": "usera@gmail.com",
    "password": "password123"
  }
  ```
* **Body (User B):**
  ```json
  {
    "username": "userb",
    "email": "userb@gmail.com",
    "password": "password123"
  }
  ```
* **Body (User C):**
  ```json
  {
    "username": "userc",
    "email": "userc@gmail.com",
    "password": "password123"
  }
  ```
* **Kết quả mong đợi:** Mã phản hồi `200 OK`, ghi lại `id` (UUID) của từng User từ kết quả trả về:
  * `id` của User A: `UUID_A`
  * `id` của User B: `UUID_B`
  * `id` của User C: `UUID_C`

#### 2. Đăng nhập để lấy JWT Token
* **API:** `POST /api/v1/auth/token`
* **Body:** Đăng nhập lần lượt từng tài khoản với `username` và `password`.
* **Kết quả mong đợi:** Ghi lại `token` trả về cho mỗi tài khoản:
  * `TOKEN_A` (Header Authorization: `Bearer TOKEN_A`)
  * `TOKEN_B` (Header Authorization: `Bearer TOKEN_B`)
  * `TOKEN_C` (Header Authorization: `Bearer TOKEN_C`)

---

### KỊCH BẢN 1: Quản lý Danh mục và Sở thích Cá nhân (`Tags`)

#### Kịch bản 1.1: Tạo các Tag ẩm thực/sở thích mới (Sử dụng Token A hoặc Token B)
* **API:** `POST /api/v1/tags`
* **Headers:** `Authorization: Bearer TOKEN_A`
* **Body (Tag 1):** `{"tagName": "Hàn Quốc"}`
* **Body (Tag 2):** `{"tagName": "Lẩu & Nướng"}`
* **Kết quả mong đợi:** `200 OK`, trả về thông tin Tag với `id` (`TAG_ID_1`, `TAG_ID_2`).
* **Trường hợp lỗi:** Gửi lại Body `{"tagName": "Hàn Quốc"}` một lần nữa.
  * **Kết quả mong đợi:** `400 Bad Request`, `code`: `1304`, `message`: `"Tag đã tồn tại"`.

#### Kịch bản 1.2: Lấy danh sách tất cả các Tag hiện có trong hệ thống
* **API:** `GET /api/v1/tags`
* **Headers:** `Authorization: Bearer TOKEN_A`
* **Kết quả mong đợi:** `200 OK`, trả về danh sách chứa ít nhất 2 Tag đã tạo ở trên.

#### Kịch bản 1.3: Thêm sở thích vào hồ sơ của User A
* **Thao tác 1 (Thêm sở thích cố định):**
  * **API:** `POST /api/v1/tags/user/{TAG_ID_1}?temporary=false`
  * **Headers:** `Authorization: Bearer TOKEN_A`
  * **Kết quả mong đợi:** `200 OK`, trả về thông tin `UserTagResponse` có `"temporary": false`.
* **Thao tác 2 (Thêm sở thích tạm thời - ví dụ hôm nay thèm ăn đồ nướng):**
  * **API:** `POST /api/v1/tags/user/{TAG_ID_2}?temporary=true`
  * **Headers:** `Authorization: Bearer TOKEN_A`
  * **Kết quả mong đợi:** `200 OK`, trả về thông tin có `"temporary": true`.

#### Kịch bản 1.4: Lấy danh sách sở thích hiện tại của User A
* **API:** `GET /api/v1/tags/user`
* **Headers:** `Authorization: Bearer TOKEN_A`
* **Kết quả mong đợi:** `200 OK`, danh sách trả về chứa đầy đủ cả 2 sở thích đã thêm của User A.

#### Kịch bản 1.5: Xóa sở thích khỏi hồ sơ của User A
* **API:** `DELETE /api/v1/tags/user/{TAG_ID_1}`
* **Headers:** `Authorization: Bearer TOKEN_A`
* **Kết quả mong đợi:** `200 OK`, `"message": "Xóa sở thích thành công"`.
* **Kiểm tra lại:** Gọi lại `GET /api/v1/tags/user` bằng `TOKEN_A` -> Danh sách chỉ còn lại duy nhất `TAG_ID_2` (Sở thích tạm thời).

---

### KỊCH BẢN 2: Tính năng Gửi lời mời kết bạn (`Friendship Request`)

#### Kịch bản 2.1: Tự gửi lời mời kết bạn cho chính mình
* **API:** `POST /api/v1/friendships/request/{UUID_A}` (User A gửi cho chính User A)
* **Headers:** `Authorization: Bearer TOKEN_A`
* **Kết quả mong đợi:** `400 Bad Request`, `code`: `1303`, `message`: `"Không thể kết bạn với chính mình"`.

#### Kịch bản 2.2: User A gửi lời mời kết bạn thành công cho User B
* **API:** `POST /api/v1/friendships/request/{UUID_B}` (User A gửi cho User B)
* **Headers:** `Authorization: Bearer TOKEN_A`
* **Kết quả mong đợi:** `200 OK`, `code`: `1000`, trạng thái `"status": "PENDING"`.

#### Kịch bản 2.3: User A gửi trùng lặp yêu cầu kết bạn cho User B
* **API:** `POST /api/v1/friendships/request/{UUID_B}`
* **Headers:** `Authorization: Bearer TOKEN_A`
* **Kết quả mong đợi:** `400 Bad Request`, `code`: `1301`, `message`: `"Yêu cầu kết bạn hoặc quan hệ bạn bè đã tồn tại"`.

#### Kịch bản 2.4: Kiểm tra danh sách lời mời chờ xử lý (Pending)
* **Thao tác 1: Xem lời mời kết bạn đã gửi đi của User A**
  * **API:** `GET /api/v1/friendships/pending/outgoing`
  * **Headers:** `Authorization: Bearer TOKEN_A`
  * **Kết quả mong đợi:** Trả về danh sách có 1 phần tử là yêu cầu gửi tới User B.
* **Thao tác 2: Xem lời mời kết bạn đã nhận được của User B**
  * **API:** `GET /api/v1/friendships/pending/incoming`
  * **Headers:** `Authorization: Bearer TOKEN_B`
  * **Kết quả mong đợi:** Trả về danh sách có 1 phần tử là yêu cầu nhận từ User A.

---

### KỊCH BẢN 3: Chấp nhận lời mời & Quản lý danh sách bạn bè

#### Kịch bản 3.1: User B chấp nhận lời mời kết bạn từ User A
* **API:** `PUT /api/v1/friendships/accept/{UUID_A}` (Chấp nhận yêu cầu từ User A)
* **Headers:** `Authorization: Bearer TOKEN_B`
* **Kết quả mong đợi:** `200 OK`, trạng thái chuyển sang `"status": "ACCEPTED"`.

#### Kịch bản 3.2: Thử chấp nhận lại lời mời đã được đồng ý
* **API:** `PUT /api/v1/friendships/accept/{UUID_A}`
* **Headers:** `Authorization: Bearer TOKEN_B`
* **Kết quả mong đợi:** `400 Bad Request`, `code`: `1301`, `message`: `"Yêu cầu kết bạn hoặc quan hệ bạn bè đã tồn tại"`.

#### Kịch bản 3.3: Kiểm tra danh sách bạn bè chính thức của cả hai bên
* **Gọi từ phía User A:**
  * **API:** `GET /api/v1/friendships`
  * **Headers:** `Authorization: Bearer TOKEN_A`
  * **Kết quả mong đợi:** Danh sách trả về chứa thông tin của **User B**.
* **Gọi từ phía User B:**
  * **API:** `GET /api/v1/friendships`
  * **Headers:** `Authorization: Bearer TOKEN_B`
  * **Kết quả mong đợi:** Danh sách trả về chứa thông tin của **User A**.

---

### KỊCH BẢN 4: Kiểm thử Tính năng Tự động Kết bạn chéo (Auto-Accept)
Đây là kịch bản rất hay: Nếu người dùng A đã gửi lời mời cho C, sau đó C (chưa xem danh sách lời mời) cũng chủ động gửi lời mời tới A. Hệ thống sẽ tự động chấp nhận mối quan hệ kết bạn ngay lập tức mà không bắt buộc phải gọi API `accept`.

#### Kịch bản 4.1: Thực hiện gửi chéo lời mời
1. **User A gửi lời mời cho User C:**
   * **API:** `POST /api/v1/friendships/request/{UUID_C}`
   * **Headers:** `Authorization: Bearer TOKEN_A`
   * **Kết quả mong đợi:** `200 OK`, trạng thái `"PENDING"`.
2. **User C chủ động gửi lời mời ngược lại cho User A:**
   * **API:** `POST /api/v1/friendships/request/{UUID_A}`
   * **Headers:** `Authorization: Bearer TOKEN_C`
   * **Kết quả mong đợi:** Hệ thống nhận diện được có yêu cầu PENDING từ phía đối diện, lập tức tự động đồng ý kết bạn! `200 OK` và trạng thái trả về là `"ACCEPTED"`.

#### Kịch bản 4.2: Kiểm tra danh sách bạn bè chính thức sau khi tự động kết bạn
* **API:** `GET /api/v1/friendships` bằng cả `TOKEN_A` và `TOKEN_C`
* **Kết quả mong đợi:** Cả hai đều nhìn thấy nhau trong danh sách bạn bè chính thức của mình.

---

### KỊCH BẢN 5: Từ chối yêu cầu / Hủy kết bạn (Decline or Unfriend)

#### Kịch bản 5.1: Hủy kết bạn giữa User A và User B
* **API:** `DELETE /api/v1/friendships/{UUID_B}` (User A gửi yêu cầu hủy kết bạn với User B)
* **Headers:** `Authorization: Bearer TOKEN_A`
* **Kết quả mong đợi:** `200 OK`, thông báo `"Hủy kết bạn hoặc từ chối yêu cầu thành công"`.

#### Kịch bản 5.2: Kiểm tra lại danh sách bạn bè sau khi hủy
* **API:** `GET /api/v1/friendships` bằng cả `TOKEN_A` và `TOKEN_B`
* **Kết quả mong đợi:** Danh sách của cả hai đều trống (không còn liên kết bạn bè với nhau nữa).