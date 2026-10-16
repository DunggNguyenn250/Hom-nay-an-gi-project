-- Thêm cấu hình số lượng quán ăn cho bộ bài quẹt của phòng (mặc định 15)
ALTER TABLE rooms ADD COLUMN deck_size INT DEFAULT 15;
