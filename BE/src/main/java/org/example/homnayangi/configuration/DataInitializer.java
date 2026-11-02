package org.example.homnayangi.configuration;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.homnayangi.constant.PredefinedTags;
import org.example.homnayangi.entity.*;
import org.example.homnayangi.enums.*;
import org.example.homnayangi.repository.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.*;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DataInitializer {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final PlaceRepository placeRepository;
    private final FriendshipRepository friendshipRepository;
    private final RoomRepository roomRepository;
    private final ExpenseRepository expenseRepository;

    @Bean
    ApplicationRunner initDatabase() {
        return args -> {
            initUsers();
            initPredefinedTags();
            initPlaces();
            initFriendships();
            initRooms();
            initExpenses();
        };
    }

    private void initUsers() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@homnayangi.com")
                    .passwordHash(passwordEncoder.encode("admin"))
                    .status(UserStatus.ACTIVE)
                    .roles(Set.of(Role.ROLE_ADMIN))
                    .build();
            userRepository.save(admin);
            log.warn("Tài khoản 'admin' đã được tạo với mật khẩu: 'admin'");
        }

        record SampleUserData(String username, String email, String password, Role role) {}

        List<SampleUserData> sampleUsers = List.of(
                new SampleUserData("tiendat", "tiendat@homnayangi.com", "123456", Role.ROLE_USER),
                new SampleUserData("ducanh", "ducanh@homnayangi.com", "123456", Role.ROLE_USER),
                new SampleUserData("trunghieu", "trunghieu@homnayangi.com", "123456", Role.ROLE_USER),
                new SampleUserData("badung", "badung@homnayangi.com", "123456", Role.ROLE_USER),
                new SampleUserData("lanhuong", "lanhuong@homnayangi.com", "123456", Role.ROLE_USER)
        );

        for (SampleUserData data : sampleUsers) {
            if (!userRepository.existsByUsername(data.username())) {
                User user = User.builder()
                        .username(data.username())
                        .email(data.email())
                        .passwordHash(passwordEncoder.encode(data.password()))
                        .status(UserStatus.ACTIVE)
                        .roles(Set.of(data.role()))
                        .build();
                userRepository.save(user);
                log.info("Đã khởi tạo tài khoản mẫu: {} ({})", data.username(), data.email());
            }
        }
    }

    private void initPredefinedTags() {
        List<Tag> newTags = new ArrayList<>();
        for (String tagName : PredefinedTags.ALL_TAGS) {
            if (!tagRepository.existsByTagName(tagName)) {
                newTags.add(Tag.builder().tagName(tagName).build());
            }
        }
        if (!newTags.isEmpty()) {
            tagRepository.saveAll(newTags);
            log.info("Đã khởi tạo thành công {} tags mẫu.", newTags.size());
        }
    }

    private void initPlaces() {
        record SamplePlaceData(String name, String category, Integer priceRange, String address, BigDecimal latitude, BigDecimal longitude) {}

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
                newPlaces.add(Place.builder()
                        .name(data.name())
                        .category(data.category())
                        .priceRange(data.priceRange())
                        .address(data.address())
                        .latitude(data.latitude())
                        .longitude(data.longitude())
                        .imageUrl(null)
                        .build());
            }
        }
        if (!newPlaces.isEmpty()) {
            placeRepository.saveAll(newPlaces);
            log.info("Đã khởi tạo thành công {} địa điểm mẫu.", newPlaces.size());
        }
    }

    private void initFriendships() {
        if (friendshipRepository.count() == 0) {
            Optional<User> badungOpt = userRepository.findByUsername("badung");
            Optional<User> tiendatOpt = userRepository.findByUsername("tiendat");
            Optional<User> ducanhOpt = userRepository.findByUsername("ducanh");

            if (badungOpt.isPresent() && tiendatOpt.isPresent() && ducanhOpt.isPresent()) {
                User badung = badungOpt.get();
                User tiendat = tiendatOpt.get();
                User ducanh = ducanhOpt.get();

                // Bạn bè đã chấp nhận (badung & tiendat)
                Friendship f1 = Friendship.builder()
                        .id(new FriendshipId(badung.getId(), tiendat.getId()))
                        .requester(badung)
                        .addressee(tiendat)
                        .status(FriendshipStatus.ACCEPTED)
                        .build();

                // Lời mời kết bạn đang chờ (ducanh gửi cho badung)
                Friendship f2 = Friendship.builder()
                        .id(new FriendshipId(ducanh.getId(), badung.getId()))
                        .requester(ducanh)
                        .addressee(badung)
                        .status(FriendshipStatus.PENDING)
                        .build();

                friendshipRepository.saveAll(List.of(f1, f2));
                log.info("Đã khởi tạo thành công dữ liệu bạn bè mẫu (ACCEPTED và PENDING).");
            }
        }
    }

    // 🌟 2. Khởi tạo phòng test mẫu (Sửa .name và RoomStatus.OPEN)
    private void initRooms() {
        if (roomRepository.count() == 0) {
            Optional<User> badungOpt = userRepository.findByUsername("badung");
            if (badungOpt.isPresent()) {
                Room room = Room.builder()
                        .roomCode("TEST68")
                        .name("Phòng Ăn Trưa Triều Khúc")
                        .host(badungOpt.get())
                        .status(RoomStatus.OPEN)
                        .build();

                roomRepository.save(room);
                log.info("Đã khởi tạo phòng test mẫu với mã phòng: TEST68");
            }
        }
    }

    // 🌟 3. Khởi tạo hóa đơn mẫu (Sửa ExpenseStatus.UNPAID)
    private void initExpenses() {
        if (expenseRepository.count() == 0) {
            Optional<User> badungOpt = userRepository.findByUsername("badung");
            Optional<User> tiendatOpt = userRepository.findByUsername("tiendat");

            if (badungOpt.isPresent() && tiendatOpt.isPresent()) {
                Expense expense = Expense.builder()
                        .totalAmount(new BigDecimal("350000"))
                        .payer(badungOpt.get())
                        .status(ExpenseStatus.UNPAID)
                        .build();

                expenseRepository.save(expense);
                log.info("Đã khởi tạo hóa đơn mẫu cho tính năng VietQR.");
            }
        }
    }
}