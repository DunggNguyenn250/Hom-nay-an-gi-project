package org.example.homnayangi.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

import org.example.homnayangi.dto.request.AuthenticationRequest;
import org.example.homnayangi.dto.request.IntrospectRequest;
import org.example.homnayangi.dto.response.AuthenticationResponse;
import org.example.homnayangi.dto.response.IntrospectResponse;
import org.example.homnayangi.entity.User;
import org.example.homnayangi.exception.AppException;
import org.example.homnayangi.exception.ErrorCode;
import org.example.homnayangi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.StringJoiner;

@Slf4j // 2. Thêm Annotation này ở đây
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
    UserRepository userRepository;

    @NonFinal
    // mã bí mật của hệ thống
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;

    // Xác thực token
    // Mỗi khi client gọi api khác thì không cần phải gửi lại mật khẩu nữa
    // Người dùng chỉ cần đăng nhập 1 lần - lưu lại token ở phía frontend - localStorage
    public IntrospectResponse introspect(IntrospectRequest request)
            throws JOSEException, ParseException {

        // lấy token từ request
        var token = request.getToken();

        // token có bị chỉnh sửa hay giả mạo không
        // verifier - mã bí mật của server
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());

        // tách chuỗi token
        // header
        // payload
        // signature
        SignedJWT signedJWT = SignedJWT.parse(token);


        // token còn hạn không
        Date expityTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        var verified = signedJWT.verify(verifier);

        return IntrospectResponse.builder()
                .valid(verified && expityTime.after(new Date()))
                .build();

    }

    // Đăng nhập và cấp token
    public AuthenticationResponse authentication(AuthenticationRequest request){
        // Khởi tạo cỗ máy mã hóa
        // encode - đăng ký - băm dữ liệu
        // matches - đăng nhập - so sánh với chuôi băm với nhau
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // matches: so sánh mật khẩu khi đăng nhập
        // vừa băm dữ liệu từ request (người dùng)
        // vừa so sánh với dữ liệu đã băm trong database
        boolean authenticated = passwordEncoder.matches(request.getPassword(),
                user.getPasswordHash());

        // mật khẩu không đúng - trả lỗi
        if(!authenticated)
            throw new AppException(ErrorCode.LOGIN_FAILED);

        // đúng thì sẽ tạo ra token giữa trên username của request
        var token = generateToken(user);

        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .build();

    }

    // Tạo JWT - token
    // truyền username bên request vào
    private String generateToken(User user){
        // HEARDER
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        // PAYLOAD
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername()) // bến tên username vào thẻ
                .issuer("devteria.com") // Đóng dấu: thẻ do website devteria.com cấp phát
                .issueTime(new Date()) // Ghi chú ngày giờ tạo thẻ (ngay bây giờ)
                .expirationTime(new Date(
                        Instant.now().plus(5, ChronoUnit.HOURS).toEpochMilli()
                )) // Lấy giờ hiện tại + thêm 5 tiếng. Hết 5 tiếng thẻ này tự động vô giá trị.
                .claim("scope",buildScope(user))
                // buildScope là 1 hàm tự tạo
                // Bạn có thể tự thêm bất kỳ thông tin gì bạn muốn, ví dụ: .claim("role", "ADMIN")
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(header,payload);

        try {
            // mã hóa/ ký dữ liệu = thuật toán HS512 thực sự diễn ra
            // chạy thuật toán hs512
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            // signer_key : mã bí mật của server
            // chữ ký - signature = thuật toán HS512 ( Header + Payload + SIGNER_KEY - chữ ký số (getBytes())
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token",e);
            throw new RuntimeException(e);
        }
    }

    private String buildScope(User user){
        StringJoiner stringJoiner = new StringJoiner(" ");
        if (!CollectionUtils.isEmpty(user.getRoles())) {
            user.getRoles().forEach(role -> stringJoiner.add(role.name()));
        }
        return stringJoiner.toString();
    }
}