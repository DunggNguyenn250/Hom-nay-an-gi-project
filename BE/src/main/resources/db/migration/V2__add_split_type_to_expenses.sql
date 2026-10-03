-- Migration V2: Thêm cột split_type vào bảng expenses
-- Cho phép theo dõi kiểu chia tiền của từng hóa đơn

ALTER TABLE expenses
    ADD COLUMN IF NOT EXISTS split_type VARCHAR(20) DEFAULT 'EVENLY';
