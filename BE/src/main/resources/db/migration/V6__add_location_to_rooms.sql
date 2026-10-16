-- Thêm thông tin tọa độ vị trí và bán kính quét quán ăn cho phòng
ALTER TABLE rooms ADD COLUMN latitude DECIMAL(10, 8);
ALTER TABLE rooms ADD COLUMN longitude DECIMAL(11, 8);
ALTER TABLE rooms ADD COLUMN radius DOUBLE PRECISION DEFAULT 5.0;
