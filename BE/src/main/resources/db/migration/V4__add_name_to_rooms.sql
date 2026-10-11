-- Thêm cột name vào bảng rooms (cho phép NULL để tránh lỗi với các dữ liệu cũ nếu có)
ALTER TABLE rooms ADD COLUMN name VARCHAR(100);