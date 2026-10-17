-- Tạo bảng room_places cố định danh sách quán ăn cho từng phòng quẹt thẻ
CREATE TABLE room_places (
    room_id UUID REFERENCES rooms(id) ON DELETE CASCADE,
    place_id UUID REFERENCES places(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (room_id, place_id)
);
