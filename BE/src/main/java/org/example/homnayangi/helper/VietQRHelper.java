package org.example.homnayangi.helper;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class VietQRHelper {

    private static final String VIETQR_BASE_URL = "https://img.vietqr.io/image";
    private static final String DEFAULT_TEMPLATE = "compact2";

    /**
     * Sinh đường dẫn ảnh mã QR theo chuẩn VietQR Quick Link.
     *
     * @param bankBin     Mã BIN hoặc mã viết tắt ngân hàng (ví dụ: 970422, MB, VCB)
     * @param accountNo   Số tài khoản ngân hàng
     * @param accountName Tên chủ tài khoản (ví dụ: NGUYEN VAN A)
     * @param amount      Số tiền cần chuyển
     * @param memo        Nội dung chuyển khoản
     * @return URL ảnh QR hoặc null nếu thiếu thông tin ngân hàng của người nhận
     */
    public String generateQrUrl(String bankBin, String accountNo, String accountName, BigDecimal amount, String memo) {
        if (bankBin == null || bankBin.isBlank() || accountNo == null || accountNo.isBlank()) {
            return null;
        }

        StringBuilder urlBuilder = new StringBuilder(VIETQR_BASE_URL)
                .append("/")
                .append(bankBin.trim())
                .append("-")
                .append(accountNo.trim())
                .append("-")
                .append(DEFAULT_TEMPLATE)
                .append(".png");

        boolean hasQueryParam = false;

        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            urlBuilder.append("?amount=").append(amount.longValue());
            hasQueryParam = true;
        }

        if (memo != null && !memo.isBlank()) {
            urlBuilder.append(hasQueryParam ? "&" : "?")
                    .append("addInfo=")
                    .append(URLEncoder.encode(memo.trim(), StandardCharsets.UTF_8));
            hasQueryParam = true;
        }

        if (accountName != null && !accountName.isBlank()) {
            urlBuilder.append(hasQueryParam ? "&" : "?")
                    .append("accountName=")
                    .append(URLEncoder.encode(accountName.trim(), StandardCharsets.UTF_8));
        }

        return urlBuilder.toString();
    }
}
