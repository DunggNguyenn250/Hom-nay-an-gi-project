package org.example.homnayangi.configuration;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.homnayangi.constant.PredefinedTags;
import org.example.homnayangi.entity.Tag;
import org.example.homnayangi.entity.User;
import org.example.homnayangi.enums.Role;
import org.example.homnayangi.enums.UserStatus;
import org.example.homnayangi.repository.TagRepository;
import org.example.homnayangi.repository.UserRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

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
    ApplicationRunner initDatabase(UserRepository userRepository, TagRepository tagRepository) {
        return args -> {
            initUsers(userRepository);
            initPredefinedTags(tagRepository);
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
}
