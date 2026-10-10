package org.example.homnayangi.configuration;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.homnayangi.constant.PredefinedTags;
import org.example.homnayangi.entity.Place;
import org.example.homnayangi.entity.Tag;
import org.example.homnayangi.entity.User;
import org.example.homnayangi.enums.Role;
import org.example.homnayangi.enums.UserStatus;
import org.example.homnayangi.repository.PlaceRepository;
import org.example.homnayangi.repository.TagRepository;
import org.example.homnayangi.repository.UserRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DataInitializer {

    PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner initDatabase(
            UserRepository userRepository,
            TagRepository tagRepository,
            PlaceRepository placeRepository) {
        return args -> {
            initUsers(userRepository);
            initPredefinedTags(tagRepository);
            initPlaces(placeRepository);
        };
    }

    private void initUsers(UserRepository userRepository) {
        // 1. Tạo tài khoản admin mặc định (nếu chưa có)
        if (!userRepository.existsByUsername("admin")) {
            Set<Role> adminRoles = new HashSet<>();
            adminRoles.add(Role.ROLE_ADMIN);

            User admin = User.builder()
                    .username("admin")
                    .email("admin@homnayangi.com")
                    .passwordHash(passwordEncoder.encode("admin"))
                    .status(UserStatus.ACTIVE)
                    .roles(adminRoles)
                    .build();

            userRepository.save(admin);
            log.warn("Tài khoản 'admin' đã được tạo với mật khẩu: 'admin'");
        }

        // 2. Danh sách 5 thành viên mẫu theo yêu cầu
        record SampleUserData(String username, String email, String password, Role role) {}

        List<SampleUserData> sampleUsers = List.of(
                new SampleUserData("tiendat", "tiendat@homnayangi.com", "123456", Role.ROLE_USER),       // Đỗ Tiến Đạt
                new SampleUserData("ducanh", "ducanh@homnayangi.com", "123456", Role.ROLE_USER),         // Vũ Đức Anh
                new SampleUserData("trunghieu", "trunghieu@homnayangi.com", "123456", Role.ROLE_USER),   // Nguyễn Trung Hiếu
                new SampleUserData("badung", "badung@homnayangi.com", "123456", Role.ROLE_USER),         // Nguyễn Bá Dũng
                new SampleUserData("lanhuong", "lanhuong@homnayangi.com", "123456", Role.ROLE_USER)      // Phùng Thị Lan Hương
        );

        for (SampleUserData data : sampleUsers) {
            if (!userRepository.existsByUsername(data.username())) {
                Set<Role> roles = new HashSet<>();
                roles.add(data.role());

                User user = User.builder()
                        .username(data.username())
                        .email(data.email())
                        .passwordHash(passwordEncoder.encode(data.password()))
                        .status(UserStatus.ACTIVE)
                        .roles(roles)
                        .build();

                userRepository.save(user);
                log.info("Đã khởi tạo tài khoản mẫu: {} ({})", data.username(), data.email());
            }
        }
    }

    private void initPredefinedTags(TagRepository tagRepository) {
        List<Tag> newTags = new ArrayList<>();
        for (String tagName : PredefinedTags.ALL_TAGS) {
            if (!tagRepository.existsByTagName(tagName)) {
                newTags.add(Tag.builder().tagName(tagName).build());
            }
        }

        if (!newTags.isEmpty()) {
            tagRepository.saveAll(newTags);
            log.info("Đã khởi tạo thành công {} tags mẫu vào cơ sở dữ liệu.", newTags.size());
        }
    }

    private void initPlaces(PlaceRepository placeRepository) {
        record SamplePlaceData(
                String name,
                String category,
                Integer priceRange,
                String address,
                BigDecimal latitude,
                BigDecimal longitude
        ) {}

        List<SamplePlaceData> samplePlaces = List.of(
                new SamplePlaceData("Bún Đậu Mắm Tôm Gốc Đa", "Bún/Phở", 1, "Số 15 Triều Khúc, Thanh Xuân, Hà Nội", new BigDecimal("20.985123"), new BigDecimal("105.797541")),
                new SamplePlaceData("Lẩu Nướng Sinh Viên 68", "Lẩu", 2, "Số 68 Triều Khúc, Thanh Xuân, Hà Nội", new BigDecimal("20.984210"), new BigDecimal("105.798122")),
                new SamplePlaceData("Cơm Rang Gà Quay Bà Hạnh", "Cơm", 1, "Ngõ 136 Triều Khúc, Thanh Xuân, Hà Nội", new BigDecimal("20.983544"), new BigDecimal("105.798831")),
                new SamplePlaceData("Nướng Chảo 23", "Nướng", 2, "Số 23 Triều Khúc, Thanh Xuân, Hà Nội", new BigDecimal("20.984850"), new BigDecimal("105.797011")),
                new SamplePlaceData("Bánh Mì Chảo Cột Điện", "Đồ ăn vặt", 1, "Số 54 Triều Khúc, Thanh Xuân, Hà Nội", new BigDecimal("20.985521"), new BigDecimal("105.796540")),
                new SamplePlaceData("Trà Chanh Bụi Phố", "Cafe/Trà sữa", 1, "Ngã tư Triều Khúc, Thanh Xuân, Hà Nội", new BigDecimal("20.986012"), new BigDecimal("105.796010")),
                new SamplePlaceData("Gà Rán Đôi Bạn", "Thức ăn nhanh", 1, "Số 10 Triều Khúc, Thanh Xuân, Hà Nội", new BigDecimal("20.985833"), new BigDecimal("105.797244")),
                new SamplePlaceData("Bún Cá Cay Hải Phòng", "Bún/Phở", 1, "Số 99 Triều Khúc, Thanh Xuân, Hà Nội", new BigDecimal("20.982511"), new BigDecimal("105.799512")),
                new SamplePlaceData("Lẩu Ếch Đồng Quê", "Lẩu", 2, "Số 112 Triều Khúc, Thanh Xuân, Hà Nội", new BigDecimal("20.981845"), new BigDecimal("105.800123")),
                new SamplePlaceData("Mixue Triều Khúc", "Tráng miệng", 1, "Số 33 Triều Khúc, Thanh Xuân, Hà Nội", new BigDecimal("20.984567"), new BigDecimal("105.797789"))
        );

        List<Place> newPlaces = new ArrayList<>();
        for (SamplePlaceData data : samplePlaces) {
            if (!placeRepository.existsByName(data.name())) {
                Place place = Place.builder()
                        .name(data.name())
                        .category(data.category())
                        .priceRange(data.priceRange())
                        .address(data.address())
                        .latitude(data.latitude())
                        .longitude(data.longitude())
                        .imageUrl(null) // Để null theo yêu cầu
                        .build();
                newPlaces.add(place);
            }
        }

        if (!newPlaces.isEmpty()) {
            placeRepository.saveAll(newPlaces);
            log.info("Đã khởi tạo thành công {} quán ăn/địa điểm mẫu vào cơ sở dữ liệu.", newPlaces.size());
        }
    }
}
