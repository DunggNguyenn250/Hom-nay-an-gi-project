-- Thêm cột status vào bảng expenses (cho phép DEFAULT 'UNPAID' và NOT NULL)
ALTER TABLE expenses ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'UNPAID';
