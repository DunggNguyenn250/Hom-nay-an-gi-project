# 🍽️ Dự án "Hôm Nay Ăn Gì" - Backend & Database Migration Guide

Tài liệu này hướng dẫn cách thiết lập môi trường, chạy dự án và đặc biệt là **quy chuẩn bắt buộc khi làm việc với Database Migration (Flyway)** dành cho toàn bộ thành viên trong team.

---

## 📌 1. Database Migration là gì? Tại sao team phải dùng nó?

### ❌ Cách làm cũ nguy hiểm:
- Mỗi người tự mở pgAdmin/DBeaver gõ lệnh tạo bảng bằng tay, hoặc để Hibernate `ddl-auto: update` tự sửa ngầm.
- **Hậu quả:** Máy người này chạy được nhưng máy người kia thiếu cột/lỗi bảng, lên server deploy không biết database đang ở version nào, dễ mất dữ liệu thật.

### ✅ Cách làm chuẩn với Flyway Migration:
- Mọi thay đổi về cấu trúc Database (tạo bảng, thêm cột, sửa khóa ngoại, xóa bảng,...) đều được viết thành các file SQL đánh số phiên bản (`V1__...`, `V2__...`) và lưu vào Git.
- Khi bất kỳ ai trong team `git pull` code mới về và bấm **Run**, Spring Boot sẽ **tự động chạy các file SQL mới** để đồng bộ database trên máy người đó.
- Cả team và server production sẽ luôn có database **giống nhau 100%**.

---

## 🚀 2. Hướng dẫn chạy dự án lần đầu cho thành viên mới

### Yêu cầu môi trường:
- **JDK 17** (LTS)
- **PostgreSQL 14+**
- **IntelliJ IDEA** (khuyên dùng) hoặc VS Code

### Các bước cài đặt:
1. **Clone repository về máy:**
   ```bash
   git clone <repo_url>
   cd Hom-nay-an-gi-project
   ```

2. **Tạo Database trên PostgreSQL:**
   Mở pgAdmin 4 hoặc DBeaver, tạo một database mới tên là:
   ```sql
   CREATE DATABASE homnayangi_db;
   ```

3. **Cấu hình file `application.yaml` (Bảo mật thông tin):**
   Vì lý do bảo mật (không lộ mật khẩu DB lên GitHub), file cấu hình thực tế đã được đưa vào `.gitignore`. Bạn hãy tạo file cấu hình cho máy mình như sau:
   - Copy file mẫu `BE/src/main/resources/application.yaml.example` thành `BE/src/main/resources/application.yaml`.
   - Mở file `application.yaml` vừa tạo và điền mật khẩu PostgreSQL của máy bạn vào dòng:
     ```yaml
     spring:
       datasource:
         password: ${SPRING_DATASOURCE_PASSWORD:mật_khẩu_của_bạn}
     ```

4. **Khởi động dự án:**
   - Trong IntelliJ: Mở file `BE/src/main/java/org/example/homnayangi/HomnayangiApplication.java` và bấm **Run (tam giác xanh)**.
   - Hoặc chạy bằng Terminal:
     ```powershell
     cd BE
     .\mvnw.cmd spring-boot:run
     ```
   - **Kết quả:** Ngay khi ứng dụng chạy xong, mở database `homnayangi_db` bạn sẽ thấy toàn bộ các bảng và bảng lịch sử `flyway_schema_history` đã được tạo tự động!

---

## 📖 3. Quy trình làm việc khi muốn Thêm / Sửa Database

Bất cứ khi nào bạn cần **tạo bảng mới, thêm cột, đổi kiểu dữ liệu, tạo khóa ngoại**, hãy làm theo đúng 3 bước sau:

### 👉 Bước 1: Tạo file Migration mới
Vào thư mục:  
📁 `BE/src/main/resources/db/migration/`

Tạo một file `.sql` mới có số version tiếp theo.

> ⚠️ **Quy tắc đặt tên file (CỰC KỲ QUAN TRỌNG):**
> Cú pháp: `V<Số_version>__<mô_tả_ngắn_gọn>.sql`  
> - Bắt đầu bằng chữ **`V`** in hoa.
> - Số phiên bản tăng dần: `V1`, `V2`, `V3`,...
> - **BẮT BUỘC có đúng 2 DẤU GẠCH DƯỚI `__`** trước phần mô tả.
> - Ví dụ hợp lệ:
>   - `V2__add_phone_number_to_users.sql`
>   - `V3__create_reviews_table.sql`

### 👉 Bước 2: Viết câu lệnh SQL thuần
Mở file vừa tạo và viết câu lệnh SQL cần thực hiện:
```sql
-- Ví dụ: Thêm cột phone_number vào bảng users
ALTER TABLE users ADD COLUMN phone_number VARCHAR(15);
```

### 👉 Bước 3: Cập nhật Entity trong Java và Run
1. Vào class Java Entity tương ứng thêm thuộc tính (ví dụ trong `User.java` thêm `String phoneNumber;`).
2. Bấm **Run** ứng dụng: Flyway sẽ tự phát hiện file mới và áp dụng ngay vào Database!
3. Commit file SQL migration cùng với code Java lên Git.

---

## ⛔ 4. Các điều CẤM KỴ (Đọc kỹ để không làm hỏng Database)

1. ❌ **TUYỆT ĐỐI KHÔNG sửa file migration cũ đã merge:**
   - Khi một file migration (ví dụ `V1__init_database.sql`) đã chạy, Flyway sẽ tính mã băm (checksum) của file đó và lưu vào bảng `flyway_schema_history`.
   - Nếu bạn vào file `V1` sửa dù chỉ một dấu cách, Flyway sẽ báo lỗi:
     `Migration checksum mismatch for migration version 1` và sập ứng dụng ngay khi khởi động.
   - **Quy tắc:** Đã commit vào Git rồi thì file cũ là "bất khả xâm phạm". Muốn sửa gì thì tạo file version tiếp theo (`V2`, `V3`).

2. ❌ **KHÔNG bao giờ xóa bảng `flyway_schema_history`:**
   - Bảng này là "cuốn sổ nhật ký" duy nhất để Flyway biết máy bạn đang ở phiên bản nào. Nếu xóa bảng này, Flyway sẽ tưởng DB trống và chạy lại từ `V1` gây lỗi trùng bảng.

3. ❌ **Tránh trùng Version khi làm việc song song 2 người:**
   - Trước khi tạo file migration mới, hãy kéo branch mới nhất về hoặc nhắn cho đồng đội xem hiện tại đang ở version mấy để không đặt tên trùng nhau (ví dụ cả 2 cùng tạo `V2__...`).

---

## 🛠️ 5. Xử lý sự cố thường gặp (Troubleshooting)

### Lỗi 1: `Migration checksum mismatch`
* **Nguyên nhân:** Ai đó đã sửa nội dung file migration cũ sau khi nó đã được thực thi.
* **Cách khắc phục:**
  - Nếu đang ở môi trường Local/Dev: Xóa database đi tạo lại (`DROP DATABASE homnayangi_db; CREATE DATABASE homnayangi_db;`), sau đó chạy lại app để Flyway migrate lại từ đầu.

### Lỗi 2: `Could not find or load main class ... ClassNotFoundException`
* **Nguyên nhân:** IntelliJ chưa biên dịch mã nguồn Java ra thư mục `target/classes`.
* **Cách khắc phục:**
  - Click chuột phải vào `BE/pom.xml` -> chọn **Maven** -> **Sync Project**.
  - Hoặc mở terminal chạy: `cd BE; .\mvnw.cmd compile`.
