Các thông tin về dự án “Hôm nay ăn gì”  
1. Công Nghệ Sử Dụng (Tech Stack)

**Frontend**

·       **Ngôn ngữ & Thư viện:** React kết hợp TypeScript.

·       **CSS Framework:** Tailwind CSS (giúp chia layout và làm UI nhanh chóng).

·       **Công cụ build:** Vite.

·       **Thư viện bổ trợ:** framer-motion (hiệu ứng quẹt thẻ), Socket.io-client hoặc SockJS-client (kết nối thời gian thực).

**Backend (Cập nhật sang Java)**

·       **Framework:** Java Spring Boot.

·       **Kiến trúc:** Phân lớp (Controller - Service - Repository).

·       **ORM & Data Access:** Spring Data JPA (Hibernate) để thao tác với cơ sở dữ liệu.

·       **Realtime:** Spring WebSocket kết hợp STOMP để đồng bộ trạng thái phòng và lượt quẹt thẻ.

·       **Bảo mật:** Spring Security (nếu cần phân quyền người dùng phức tạp).

**Cơ Sở Dữ Liệu**

·       **Hệ quản trị:** PostgreSQL.

·       **Tính năng:** Hỗ trợ xử lý truy vấn đồng thời tốt, đảm bảo tính toàn vẹn dữ liệu khi có nhiều người cùng thực hiện thao tác chia tiền.

**Công Cụ Phát Triển**

·       **Môi trường:** IntelliJ IDEA (cho Backend) và Visual Studio Code (cho Frontend).

·       **Quản lý Database:** pgAdmin 4 hoặc DBeaver.

·       **Quản lý Source Code:** Git và GitHub.

·       **Tài liệu API:** Swagger (Springfox / Springdoc-openapi).

2. Các tính năng cốt lỗi**  
Quản lý Người dùng & Xác thực (User & Profile Management)**

- **Định danh cơ bản:** Lưu trữ username, avatar_url để hiển thị trong phòng.
- **Xác thực (Auth):** Hỗ trợ Guest Mode (lưu local/session) cho người dùng nhanh, hoặc Social Login (Google) để lưu trữ lịch sử nợ nần dài hạn.
- **Cấp quyền GPS:** Yêu cầu quyền truy cập vị trí để phục vụ việc tính toán khoảng cách quán ăn.

**Quản lý Phiên làm việc & Realtime (Room Management)**

- **Vòng đời phòng:** Tạo phòng (sinh room_code), Tham gia phòng, và Trạng thái sẵn sàng (is_ready).
- **Đồng bộ STOMP/WebSocket:** Phát sóng (broadcast) trạng thái vào/ra và ready của mọi người theo thời gian thực.
- **Xử lý ngắt kết nối (Disconnect Handling):** Tạm dừng phòng hoặc thông báo khi có thành viên rớt mạng để bảo toàn dữ liệu lúc đang quẹt thẻ.

**Danh sách & Khám phá địa điểm (Place Discovery & Directory) - _Tính năng mới_**

- **Danh sách tổng hợp:** Giao diện dạng list cho phép người dùng lướt xem toàn bộ các quán ăn có trong hệ thống (Tên quán, Địa chỉ, Phân loại, Hình ảnh).
- **Bộ lọc cơ bản:** Sắp xếp danh sách dựa trên tọa độ GPS (Gần tôi nhất) hoặc theo tầm giá (price_range).
- **Chi tiết quán:** Nhấn vào quán trong danh sách để xem thông tin chi tiết trước khi quyết định đưa vào phòng quẹt thẻ.

**Cơ chế "Quẹt thẻ" & Matching (Tinder-like Swiping)**

- **Tương tác thẻ:** Hiệu ứng vuốt trái/phải với framer-motion ghi nhận vào bảng swipes (is_like).
- **Chống Spam (Debounce):** Giới hạn tần suất bấm nút/vuốt thẻ ở Frontend để tránh làm sập Backend.
- **Thuật toán Chốt đơn (Matching Logic):** Xử lý luồng thành công (quán đạt 100% lượt Like) và luồng Deadlock (quét hết thẻ nhưng không ai đồng thuận -> tự động đề xuất quán có lượt Like cao nhất).

**Tài chính & Chia tiền (Bill Splitting)**

- **Quản lý Hóa đơn:** Tạo Bill (total_amount) và ghi nhận người thanh toán (payer_id).
- **Chia tiền linh hoạt:** Hệ thống tự động chia đều (amount_owed), hoặc cho phép chỉnh sửa tay số tiền của từng cá nhân.
- **Nhắc nợ:** Tracking trạng thái is_paid để hiển thị cảnh báo "Ai nợ mình, mình nợ ai" ở màn hình chính.

## 3. Các tính năng bổ sung (Sẽ tập trung sau)

**Tích hợp Mã QR Thanh toán (VietQR/Momo)**

- **Cơ chế:** Dựa vào bảng expense_splits và total_amount, hệ thống gọi API của các dịch vụ thanh toán (như VietQR) để gen ra một mã QR động.
- **Liên kết cốt lõi:** Giải quyết triệt để khâu "Chia tiền chi tiết". Thay vì người dùng phải tự mở app ngân hàng, nhập số tài khoản và số tiền lẻ, họ chỉ cần quét mã QR hiển thị trong app để chuyển đúng số tiền amount_owed, sau đó hệ thống tự cập nhật is_paid = True.

**Bộ lọc & Thiết lập Rule cho Phòng (Advanced Room Filters)**

- **Cơ chế:** Thêm bảng room_settings liên kết với rooms. Khi Host tạo phòng, họ có thể cài đặt rule: "Chỉ hiện quán ăn dưới 50k", "Chỉ quán chay", hoặc "Bán kính dưới 3km".
- **Liên kết cốt lõi:** Can thiệp trực tiếp vào bước load dữ liệu của "Cơ chế Quẹt thẻ". Backend sẽ dùng các rule này cùng tọa độ GPS để query lọc danh sách thẻ bài trước khi đẩy về cho các member quẹt, giúp tăng tỷ lệ Match nhanh hơn.

**Hệ thống AI Đề xuất (Recommendation Engine)**

- **Cơ chế:** Sử dụng thuật toán Collaborative Filtering (Lọc cộng tác) phân tích data người dùng.
- **Liên kết cốt lõi:** Khai thác kho vàng dữ liệu từ bảng swipes. Dựa vào lịch sử "Like" của người dùng A, hệ thống biết gu của họ và sẽ ưu tiên xếp các thẻ món ăn phù hợp lên đầu tiên trong tập bài quẹt, thay vì random, giúp đẩy nhanh quá trình "Chốt đơn".

**Hệ thống Đánh giá & Xếp hạng (Rating & Leaderboard)**

- **Cơ chế:** Thêm bảng reviews liên kết với places.
- **Liên kết cốt lõi:** Xảy ra sau vòng đời "Quản lý phòng" và "Chia tiền". Sau khi nhóm ăn xong, hệ thống mời họ đánh giá quán. Dữ liệu này sẽ làm phong phú thêm tính năng "Danh sách & Khám phá địa điểm" (Tính năng mới em vừa yêu cầu), cho phép người dùng sort quán theo Rating của cộng đồng. Đồng thời, có thể cấp huy hiệu "Uy tín" cho user luôn thanh toán nợ nhanh nhất dựa trên lịch sử expense_splits.

**Hệ thông Kết bạn (Friend System)**

·        **Quản lý danh bạ:** Tiện ích gửi lời mời kết bạn (qua tìm kiếm Username), chấp nhận, từ chối và quản lý danh sách bạn bè.

·        **Trạng thái Hoạt động (Presence System):** Tận dụng luồng WebSockets hiện có để theo dõi trạng thái Online/Offline. Khi user mở app, avatar trong danh sách bạn bè sẽ hiện chấm xanh lá.

·        **Mời trực tiếp (One-click Invite):** Trong màn hình RoomLobby, Host bấm "Mời bạn bè". Hệ thống bắn trực tiếp một bản tin STOMP tới user kia. Trên màn hình của họ sẽ bật Pop-up: *"Đức Anh mời bạn đi ăn" -> Bấm Đồng ý -> Tự động ghi vào room_members và đẩy thẳng vào phòng.

**Hệ thống Ghép phòng ngẫu nhiên (Tag-based Matchmaking)**

·        **Khai báo Tâm trạng:** Thay vì bấm "Tạo phòng", user chọn tính năng "Ghép hội". Màn hình hiển thị danh sách tag để user chọn 1-2 tag (VD: "Đang buồn", "Thèm bia").

·        **Vào Hàng đợi (Queueing):** Hệ thống không ghi ngay vào Database. User được đưa vào một "Phòng chờ ảo" (Matchmaking Queue). Màn hình hiển thị radar quét tìm người.

·        **Thuật toán Ghép (Worker Logic):** Một tiến trình ngầm (Background Task) trên Spring Boot liên tục quét hàng đợi. Khi phát hiện 2 đến 4 người rảnh rỗi có trùng ít nhất 1 tag_id, hệ thống chốt nhóm.

·        **Khởi tạo Phòng tự động:** Thay vì để user tạo phòng, Backend tự sinh ra room_code, tự gán những người vừa match vào phòng này với quyền ngang nhau, và bắn WebSockets ép tất cả chuyển sang màn hình Quẹt thẻ.

**Khuyến nghị kiến trúc:** Tuyệt đối không dùng PostgreSQL để lưu danh sách những người đang tìm phòng ngẫu nhiên (Queue). Tốc độ đọc/ghi liên tục của SQL sẽ làm sập server. Em cần cấu hình thêm Redis vào Spring Boot để lưu Hàng đợi này trên RAM, đảm bảo tốc độ ghép cặp diễn ra trong vài mili-giây.

## 4. luồng hoạt động

**1. Luồng Quản lý Tài khoản & Xã hội (Account & Social Flow)**

- **Đăng ký & Xác thực (Auth):** Người dùng có thể trải nghiệm nhanh qua Guest Mode (dữ liệu lưu tạm ở Local Storage) hoặc Đăng nhập qua Google/Email để đồng bộ lịch sử nợ nần.
- **Cấp quyền Hệ thống:** Ngay sau khi đăng nhập, hệ thống hiển thị Pop-up xin quyền truy cập vị trí GPS để phục vụ thuật toán tìm quán ăn gần nhất.
- **Kết nối Bạn bè:** Người dùng tìm kiếm username để gửi lời mời. Hệ thống (qua STOMP WebSockets) đẩy thông báo realtime. Khi hai bên chấp nhận, trạng thái chuyển thành "ACCEPTED", cho phép thấy trạng thái Online/Offline của nhau.
- **Quản lý Dữ liệu Cá nhân:** Cập nhật Avatar, Username. Người dùng có quyền "Xóa tài khoản" — hệ thống sẽ trigger cascade trong Database để gỡ sạch dữ liệu định danh, nhưng giữ lại các record nợ nần (chuyển thành tên "Người dùng ẩn danh") để không làm hỏng hóa đơn của người khác.

**2. Luồng Trải nghiệm Cá nhân (Personal Flow)**

- **Khám phá Địa điểm (Place Directory):** Mở app, người dùng lướt xem danh sách toàn bộ quán ăn. Filter nhanh theo bán kính (từ tọa độ GPS) hoặc phân khúc giá. Nhấn vào thẻ để xem chi tiết địa chỉ, hình ảnh.
- **Chế độ Đi ăn Solo:** Nếu không có ai đi cùng, người dùng bấm "Ăn một mình". Hệ thống lọc tập thẻ gần nhất, người dùng tự quẹt trái/phải. Lượt "Thích" đầu tiên sẽ chốt ngay quán đó.
- **Ví cá nhân & Nhắc nợ:** Truy cập tab "Tài chính" để quản lý sổ nợ. Nếu đang có khoản chưa thanh toán (is_paid = false), hệ thống hiển thị cảnh báo đỏ rực ở ngay màn hình Home.

**3. Luồng Hội nhóm Chủ động (Private Group Flow)**

- **Khởi tạo Phòng:** Host bấm "Tạo phòng mới". Hệ thống sinh mã room_code.
- **Tham gia Phòng:**

- **_Cách 1 (Truyền thống):_** Khách nhập room_code thủ công.
- **_Cách 2 (Mạng xã hội):_** Host chọn bạn bè đang Online trong danh sách, bấm "Mời". Bạn bè nhận Pop-up, bấm "Đồng ý" để bay thẳng vào phòng chờ (Lobby).

- **Sẵn sàng (Ready Check):** Mọi người đổi trạng thái is_ready = true. Host bấm "Bắt đầu quẹt" để chuyển cả phòng sang giao diện thẻ bài.

**4. Luồng Ghép phòng Ngẫu nhiên (Matchmaking Flow)**

- **Khai báo Tâm trạng:** Người dùng chọn tab "Ghép hội", pick 1-2 tag đại diện cho nhu cầu (Ví dụ: "Lẩu sinh viên", "Thèm nhậu").
- **Hàng đợi (Queueing):** Hệ thống đẩy user vào phòng chờ ảo (xử lý bằng Redis) và quét radar.
- **Tự động Chốt nhóm (Auto-Match):** Khi tìm thấy 2-4 người có chung ít nhất 1 Tag, Background Worker của Spring Boot tự động tạo một phòng (không cần room_code), ép toàn bộ các user này vào phòng và nổ còi bắt đầu quẹt thẻ.

**5. Luồng Cốt lõi: Quẹt thẻ & Chốt đơn (Core Swiping Flow)**

- **Tương tác Quẹt:** Vuốt thẻ (kèm debounce delay để chống spam API). Mỗi lượt quẹt được ghi vào bảng swipes.
- **Hiệu ứng Thời gian thực:** Có người vừa Like, hệ thống broadcast tim bay lên góc màn hình để giục giã những người còn lại.
- **Thuật toán Chốt đơn (Match):**

- **_Kịch bản lý tưởng:_** Quán đầu tiên nhận 100% Like -> Khóa phòng, bung pháo hoa, chốt đơn.
- **_Kịch bản Deadlock:_** Hết danh sách mà không ai đồng thuận -> Hệ thống fallback, tự động đề xuất quán có số lượt Like cao nhất.

**6. Luồng Tài chính (Billing Flow)**

Quá trình chia tiền giờ đây sẽ đáp ứng được mọi tình huống thực tế tại quán ăn, được chia thành 4 bước với 2 cơ chế tùy chọn:

- **Bước 1: Khởi tạo Hóa đơn (Create Expense)**

- Host (hoặc người đứng ra trả tiền) mở phòng đã chốt, nhấn "Tạo hóa đơn".
- Người này sẽ được hệ thống tự động gán là payer_id (Chủ nợ).

- **Bước 2: Lựa chọn Cơ chế tính tiền**

- **Cơ chế 1:** Order chi tiết theo món (Minh bạch tuyệt đối)

- **Nhập menu tạm:** Đại diện nhóm nhập danh sách các món đã gọi (VD: Cơm sườn 40k, Trà đá 10k, Lẩu Thái 300k).
- **Tự nhận món (Self-Selection):** Mỗi thành viên mở app, tick chọn vào những món mình đã ăn.
- **Chia sẻ món chung:** Với các món ăn chung (như nồi Lẩu Thái), những ai tick vào hệ thống sẽ tự động lấy giá món đó chia đều cho số người tick.
- **Chốt số:** Hệ thống tự động cộng dồn giá các món mỗi người đã tick để ra tổng tiền cuối cùng (amount_owed).

- **Cơ chế 2:** Nhập tay siêu tốc (Nhanh gọn lẹ)

- Dành cho quán ăn đơn giản. Người trả tiền nhập Tổng hóa đơn thực tế của quán.
- Giao diện hiện ra danh sách các thành viên. Người trả tiền tự gõ số tiền mỗi người phải chịu vào từng ô.
- **Smart Validation (Kiểm tra thông minh):** Nút "Tiếp tục" chỉ sáng lên khi _Tổng tiền các cá nhân = Tổng hóa đơn của quán_. Nếu lệch dù chỉ 1.000đ, hệ thống sẽ báo lỗi để chống thất thoát quỹ.

- **Bước 3: Tạo QR & Bắn thông báo nhắc nợ**

- Khi hóa đơn được chốt, hệ thống lưu dữ liệu và tự động sinh ra một Mã VietQR động cho từng người nợ (chứa sẵn số tài khoản của Chủ nợ và đúng số tiền phải trả).
- **WebSockets bắn thông báo về máy từng người:** _"Bạn nợ [Tên] [Số tiền] VNĐ"_.

- **Bước 4: Hoàn tất thanh toán**

- Người nợ quét QR chuyển khoản xong -> Bấm "Đã chuyển".
- Chủ nợ check biến động số dư -> Bấm "Xác nhận" -> Hệ thống đóng khoản nợ (is_paid = true).

## 5. luồng WebSockets (STOMP) khi bấm "Like"

**1. Frontend phát tín hiệu (Gửi bản tin)**

- **Hành động:** Khi user vuốt thẻ món ăn sang phải trên component FramerCard.tsx.
- **Kiểm soát tần suất:** Hàm xử lý sự kiện sẽ đi qua custom hook useDebounce (trì hoãn ~300ms) để tránh việc user quẹt quá nhanh gây spam API.
- **Gửi STOMP Message:** Hook useSocket sử dụng stompClient.send() để bắn một payload (chuỗi JSON) lên server.

- _Endpoint đích:_ /app/room/{room_code}/swipe
- _Payload gửi đi:_ { "user_id": "...", "place_id": "...", "is_like": true }.

**2. Backend tiếp nhận & Lưu trữ (Xử lý Controller & Service)**

- **Bắt bản tin:** Tại RoomSocketController.java, một hàm được đánh dấu @MessageMapping("/room/{room_code}/swipe") sẽ đón payload này.
- **Lưu lịch sử:** Controller gọi SwipeMatchService.java để insert một dòng mới vào bảng swipes với các thông tin tham chiếu đến room_id, user_id, place_id và cờ is_like. Ràng buộc UNIQUE database sẽ chặn lỗi nếu có bản tin trùng lặp gửi lên do mạng lag.
- **Kiểm tra điều kiện Match:** Service query bảng swipes đếm tổng số lượt is_like = true của place_id này trong phòng hiện tại. Đối chiếu con số đó với tổng số thành viên đã tham gia trong bảng room_members.

**3. Backend phát sóng phản hồi (Broadcasting)**

Sử dụng SimpMessagingTemplate của Spring Boot, Backend lập tức phát sóng (broadcast) một bản tin về lại phòng đó qua kênh đăng ký (Topic).

- **Kịch bản A (Chưa đủ lượt Like):** Backend bắn bản tin tới kênh /topic/room/{room_code} với payload: { "type": "SWIPE_ACTION", "user_id": "...", "is_like": true }.
- **Kịch bản B (Đã đủ 100% Like - Chốt đơn):** Service gọi Repository cập nhật trường matched_place_id cho bảng rooms, chuyển trạng thái phòng sang 'CLOSED'. Sau đó bắn bản tin: { "type": "MATCH_FOUND", "matched_place_id": "..." }.

**4. Frontend cập nhật Giao diện (Render Realtime)**

- **Lắng nghe:** Ở màn hình SwipeDeck.tsx, hook useSocket đã gọi lệnh stompClient.subscribe('/topic/room/{room_code}') từ lúc mới vào phòng.
- **Phản ứng với Kịch bản A:** State cục bộ cập nhật. Frontend render một animation nhỏ (ví dụ: Avatar của người vừa Like kèm hình trái tim bay vút lên góc màn hình) để tạo hiệu ứng tâm lý đám đông (FOMO).
- **Phản ứng với Kịch bản B:** State toàn cục (Redux/Zustand) cập nhật biến hasMatched = true. React unmount component SwipeDeck.tsx, kích hoạt component MatchResult.tsx rơi xuống cùng hiệu ứng pháo hoa và hiển thị thông tin quán ăn chốt cuối cùng.

**Mẹo Senior:** Ở bước 2, khi nhiều user cùng quẹt Like cho 1 quán ở đúng _cùng một phần nghìn giây_, hiện tượng Race Condition (Xung đột dữ liệu đồng thời) có thể xảy ra khiến thuật toán đếm Like bị sai lệch. Cần sử dụng cơ chế Khóa (Pessimistic Locking trong Spring Data JPA) hoặc Redis Distributed Lock cho hàm kiểm tra Match để hệ thống chạy ổn định.


-- Kích hoạt extension để tạo UUID tự động

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

/* =========================================

   1. MODULE ĐỊNH DANH & MẠNG XÃ HỘI (Identity & Social)

   ========================================= */

-- Bảng Users: Chứa thông tin cốt lõi và trạng thái tài khoản

CREATE TABLE users (

    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    username VARCHAR(50) NOT NULL,

    email VARCHAR(100) UNIQUE,

    password_hash VARCHAR(255),

    avatar_url VARCHAR(255),

    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE (Hoạt động), BANNED (Khóa), SUSPENDED (Khóa tạm)

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP

);

-- Bảng User_Roles: Phân quyền linh hoạt 1 User có nhiều Role

CREATE TABLE user_roles (

    user_id UUID REFERENCES users(id) ON DELETE CASCADE,

    role VARCHAR(50) NOT NULL, -- ROLE_GUEST, ROLE_USER, ROLE_ADMIN

    PRIMARY KEY (user_id, role)

);

-- Bảng Friendships: Quản lý danh bạ bạn bè

CREATE TABLE friendships (

    requester_id UUID REFERENCES users(id) ON DELETE CASCADE,

    addressee_id UUID REFERENCES users(id) ON DELETE CASCADE,

    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, ACCEPTED, BLOCKED

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (requester_id, addressee_id),

    CHECK (requester_id != addressee_id) -- Chặn tự kết bạn với chính mình

);

/* =========================================

   2. MODULE GHÉP CẶP & TÂM TRẠNG (Matchmaking)

   ========================================= */

-- Bảng Tags: Danh mục sở thích/tâm trạng

CREATE TABLE tags (

    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    tag_name VARCHAR(50) UNIQUE NOT NULL

);

-- Bảng User_Tags: Ánh xạ sở thích cho thuật toán ghép phòng

CREATE TABLE user_tags (

    user_id UUID REFERENCES users(id) ON DELETE CASCADE,

    tag_id UUID REFERENCES tags(id) ON DELETE CASCADE,

    is_temporary BOOLEAN DEFAULT FALSE, -- Phân biệt tag cố định và tag dùng 1 lần

    PRIMARY KEY (user_id, tag_id)

);

/* =========================================

   3. MODULE KHÁM PHÁ ĐỊA ĐIỂM (Places)

   ========================================= */

-- Bảng Places: Kho dữ liệu quán ăn với tọa độ GPS

CREATE TABLE places (

    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    name VARCHAR(255) NOT NULL,

    category VARCHAR(100),

    price_range INT CHECK (price_range IN (1, 2, 3)),

    image_url VARCHAR(255),

    address TEXT,

    latitude DECIMAL(10, 8),  -- Phục vụ truy vấn bán kính

    longitude DECIMAL(11, 8)

);

/* =========================================

   4. MODULE PHIÊN LÀM VIỆC & QUẸT THẺ (Room & Swiping)

   ========================================= */

-- Bảng Rooms: Vòng đời của một nhóm ăn uống

CREATE TABLE rooms (

    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    room_code VARCHAR(10) UNIQUE,

    host_id UUID REFERENCES users(id) ON DELETE CASCADE,

    status VARCHAR(20) DEFAULT 'OPEN', -- OPEN, SWIPING, CLOSED

    matched_place_id UUID REFERENCES places(id) ON DELETE SET NULL, -- Lưu kết quả chốt đơn

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP

);

-- Bảng Room_Members: Những ai đang ở trong phòng

CREATE TABLE room_members (

    room_id UUID REFERENCES rooms(id) ON DELETE CASCADE,

    user_id UUID REFERENCES users(id) ON DELETE CASCADE,

    is_ready BOOLEAN DEFAULT FALSE,

    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (room_id, user_id)

);

-- Bảng Swipes: Trái tim của cơ chế Tinder-like Matching

CREATE TABLE swipes (

    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    room_id UUID REFERENCES rooms(id) ON DELETE CASCADE,

    user_id UUID REFERENCES users(id) ON DELETE CASCADE,

    place_id UUID REFERENCES places(id) ON DELETE CASCADE,

    is_like BOOLEAN NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (room_id, user_id, place_id) -- Bắt buộc để chống spam click

);

/* =========================================

   5. MODULE TÀI CHÍNH & KẾ TOÁN (Billing & Splitting)

   ========================================= */

-- Bảng Expenses: Ghi nhận tổng tiền của cả bàn

CREATE TABLE expenses (

    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    room_id UUID REFERENCES rooms(id) ON DELETE CASCADE,

    payer_id UUID REFERENCES users(id) ON DELETE CASCADE, -- Người trả tiền trước

    total_amount DECIMAL(12,2) NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP

);

-- Bảng Expense_Items: Danh sách món ăn thực tế (Cho Cơ chế chia chi tiết)

CREATE TABLE expense_items (

    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    expense_id UUID REFERENCES expenses(id) ON DELETE CASCADE,

    item_name VARCHAR(255) NOT NULL,

    price DECIMAL(12,2) NOT NULL

);

-- Bảng Item_Sharers: Ghi nhận ai ăn món nào để chia tiền món đó

CREATE TABLE item_sharers (

    item_id UUID REFERENCES expense_items(id) ON DELETE CASCADE,

    user_id UUID REFERENCES users(id) ON DELETE CASCADE,

    PRIMARY KEY (item_id, user_id)

);

-- Bảng Expense_Splits: Con số nợ nần cuối cùng (Dùng để nhắc nợ & Gen QR)

CREATE TABLE expense_splits (

    expense_id UUID REFERENCES expenses(id) ON DELETE CASCADE,

    user_id UUID REFERENCES users(id) ON DELETE CASCADE,

    amount_owed DECIMAL(12,2) NOT NULL, -- Số tiền phải trả

    is_paid BOOLEAN DEFAULT FALSE, -- Trạng thái trả nợ

    PRIMARY KEY (expense_id, user_id)

);ư