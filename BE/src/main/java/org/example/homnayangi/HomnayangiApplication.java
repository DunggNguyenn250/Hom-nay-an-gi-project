package org.example.homnayangi;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class HomnayangiApplication {

    public static void main(String[] args) {
        SpringApplication.run(HomnayangiApplication.class, args);
    }

//    @Bean
//    CommandLineRunner printEncodedPassword(PasswordEncoder passwordEncoder) {
//        return args -> {
//            String rawPassword = "123456";
//            String encodedPassword = passwordEncoder.encode(rawPassword);
//
//            System.out.println("\n==================================================");
//            System.out.println("MẬT KHẨU GỐC   : " + rawPassword);
//            System.out.println("MẬT KHẨU MÃ HÓA: " + encodedPassword);
//            System.out.println("==================================================\n");
//        };
//    }
}
