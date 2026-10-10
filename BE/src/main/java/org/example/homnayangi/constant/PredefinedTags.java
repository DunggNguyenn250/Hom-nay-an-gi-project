package org.example.homnayangi.constant;

import java.util.List;

public final class PredefinedTags {

    private PredefinedTags() {
    }

    public static final List<String> FOOD_TYPES = List.of(
            "Lẩu",
            "Nướng",
            "Bún / Phở / Miến",
            "Cơm",
            "Hải sản",
            "Ốc",
            "Đồ ăn vặt",
            "Đồ ngọt / Tráng miệng",
            "Đồ nhậu",
            "Đồ chay",
            "Thức ăn nhanh",
            "Cafe / Trà sữa"
    );

    public static final List<String> CUISINES = List.of(
            "Món Việt",
            "Món Hàn Quốc",
            "Món Nhật Bản",
            "Món Thái Lan",
            "Món Trung Hoa",
            "Món Âu / Mỹ",
            "Món Ấn Độ"
    );

    public static final List<String> VIBES_AND_OCCASIONS = List.of(
            "Bình dân / Vỉa hè",
            "Sang trọng",
            "Hẹn hò lãng mạn",
            "Tụ tập nhóm đông",
            "Gia đình",
            "Rooftop / Sân vườn",
            "View sống ảo",
            "Yên tĩnh",
            "Có phòng riêng"
    );

    public static final List<String> SPECIAL_DIETARY = List.of(
            "Healthy / Eat Clean",
            "Ăn đêm",
            "Mở cửa 24/7",
            "Thú cưng có thể vào"
    );

    public static final List<String> ALL_TAGS = java.util.stream.Stream.of(
            FOOD_TYPES,
            CUISINES,
            VIBES_AND_OCCASIONS,
            SPECIAL_DIETARY
    ).flatMap(List::stream).toList();
}
