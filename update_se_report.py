import re

def update_file(filename):
    with open(filename, 'r', encoding='utf-8') as f:
        content = f.read()

    # Find CHƯƠNG 5, 6, 7 sections
    ch5_start = content.find("# CHƯƠNG 5: THIẾT KẾ HỆ THỐNG")
    ch6_start = content.find("# CHƯƠNG 6: THIẾT KẾ DỮ LIỆU")
    ch7_start = content.find("# CHƯƠNG 7: THIẾT KẾ GIAO DIỆN")
    ch8_start = content.find("# CHƯƠNG 8: TỔNG KẾT")

    if ch5_start == -1 or ch6_start == -1 or ch7_start == -1 or ch8_start == -1:
        print("Could not find all chapters.")
        return

    # Create new content for Chapter 5
    ch5_content = """# CHƯƠNG 5: THIẾT KẾ HỆ THỐNG

## 5.1 Tổng quan về mô hình kiến trúc

Hệ thống Hi Buddy được thiết kế dựa trên kiến trúc Client-Server hiện đại, chia tách rõ ràng giữa Front-end (Mobile App) và Back-end (API Services). Để đáp ứng các yêu cầu về tính năng AI (Hybrid Matching) và realtime (Chat), hệ thống kết hợp kiến trúc dịch vụ (Service-Oriented) với các thành phần chuyên biệt.

### 5.1.1 Sơ đồ kiến trúc

Kiến trúc tổng thể của hệ thống bao gồm 3 tầng chính:
- **Tầng Client (Presentation Layer):** Ứng dụng Android xây dựng bằng Kotlin và Jetpack Compose, xử lý giao diện người dùng và tương tác local.
- **Tầng Service (Business Logic Layer):** Hệ thống API xây dựng bằng FastAPI (Python) chạy trên Uvicorn, cung cấp các dịch vụ RESTful API và WebSocket cho realtime chat.
- **Tầng Data (Data Layer):** 
  - PostgreSQL lưu trữ dữ liệu có cấu trúc (User, Project, Task, Match...).
  - Milvus Vector Database lưu trữ vector nhúng (embeddings) cho thuật toán AI.
  - Redis đóng vai trò Cache và quản lý Session/Pub-Sub cho WebSocket.

## 5.2 Mô tả các thành phần trong hệ thống

### 5.2.1 Android Client
- Xây dựng theo mô hình MVVM (Model-View-ViewModel) kết hợp với Clean Architecture.
- Sử dụng Retrofit để giao tiếp API và OkHttp cho WebSocket.
- Room Database cho bộ nhớ đệm nội bộ trên thiết bị.

### 5.2.2 Backend Service (FastAPI)
- **Auth Service:** Quản lý vòng đời xác thực OAuth2, tạo và quay vòng Refresh/Access Token (Sliding Window), xác thực email tổ chức học thuật bằng OTP.
- **Matching Service:** Cốt lõi của hệ thống, xử lý điểm số kết hợp (Hybrid Score) từ Milvus (Cosine Similarity của AI) và điểm đánh giá kỹ năng/vai trò truyền thống.
- **Chat & Realtime Service:** Xử lý điều phối luồng tin nhắn qua WebSocket, tự động gán ID tin nhắn để loại bỏ trùng lặp từ phía Client.
- **Moderation Service:** Quản lý quy trình khiếu nại dự án, tự động ẩn (hide) dự án nếu bị cờ (flag) quá số lần quy định, dành cho Admin.

### 5.2.3 Data Storage & AI Models
- **PostgreSQL:** Quản lý Relationship thông qua SQLAlchemy ORM và Alembic để migration.
- **Milvus:** Lưu trữ các vector 384 chiều.
- **PyTorch & Sentence-Transformer:** Mô hình ngôn ngữ `all-MiniLM-L6-v2` chuyển đổi văn bản tự do (bio, mô tả dự án) thành vector để xử lý tìm kiếm theo ngữ nghĩa.

"""

    # Create new content for Chapter 6
    ch6_content = """# CHƯƠNG 6: THIẾT KẾ DỮ LIỆU

## 6.1 Sơ đồ lớp cơ sở dữ liệu

Cơ sở dữ liệu của Hi Buddy được chuẩn hóa cao để hỗ trợ mở rộng, bảo mật và truy xuất dữ liệu cực nhanh.

## 6.2 Danh sách các lớp đối tượng và quan hệ

### 6.2.1 User và UserProfile (Quan hệ 1-1)
- **User:** Chứa thông tin đăng nhập cốt lõi (ID, Email, Role, Trạng thái sinh viên). Tách biệt với profile để tăng tốc quá trình xác thực token.
- **UserProfile:** Chứa thông tin hiển thị (Tên, Tiểu sử, Avatar, Chế độ matching - CONTRIBUTOR/OWNER/BOTH, và `embedding_id` tham chiếu đến Milvus).

### 6.2.2 Project và ProjectRoleSlot (Quan hệ 1-N)
- **Project:** Đại diện cho một dự án, lưu `owner_id`, mô tả, trạng thái (RECRUITING, FLAGGED, COMPLETED).
- **ProjectRoleSlot:** Các vị trí cần tuyển dụng trong một dự án (Ví dụ: "Frontend Developer" với số lượng `count` và đã điền `filled`).

### 6.2.3 SwipeAction và Match (Quan hệ N-1)
- **SwipeAction:** Ghi nhận mọi thao tác vuốt (LIKE, PASS).
- **Match:** Được sinh ra khi có tương tác LIKE từ cả hai phía (Contributor và Owner). Liên kết trực tiếp giữa Contributor ID và Project ID.

### 6.2.4 Chat và Message (Quan hệ 1-N)
- **Chat:** Tương ứng 1-1 với một Match. Là một phòng trò chuyện giữa ứng viên và dự án.
- **Message:** Các tin nhắn cá nhân trong phòng chat, chứa `client_message_id` để chống gửi lặp (Replay/Deduplication).

## 6.3 Cơ chế đảm bảo tính toàn vẹn (Integrity Constraints)
- **Cascade Delete:** Xóa Project sẽ gỡ bỏ các RoleSlot, SwipeAction và Match tương ứng.
- **Token Hash:** Refresh token không được lưu dạng raw mà phải băm (SHA-256) tại cột `refresh_tokens`.

"""

    # Create new content for Chapter 7
    ch7_content = """# CHƯƠNG 7: THIẾT KẾ GIAO DIỆN

## 7.1 Nguyên tắc thiết kế (UI/UX Principles)
- **Material Design 3:** Ứng dụng Jetpack Compose với bảng màu động, tạo sự nhất quán trên hệ điều hành Android.
- **Card-Based UI cho Swipe:** Màn hình chính sử dụng dạng thẻ (Tinder-like) giúp tối ưu thao tác một tay (vuốt trái bỏ qua, vuốt phải kết nối).
- **Tập trung nội dung:** Phân cấp thông tin rõ ràng, đặt kỹ năng (Skills) và độ phù hợp (Match Percentage) lên vùng dễ nhìn nhất.

## 7.2 Danh sách và mô tả các màn hình chính

### 7.2.1 Nhóm màn hình Xác thực và Khởi tạo
- **Màn hình Đăng nhập/Đăng ký:** Hỗ trợ Email/Mật khẩu truyền thống và Google Single Sign-On.
- **Màn hình Xác thực OTP:** Giao diện nhập 6 chữ số để xác nhận email định dạng giáo dục (.edu).
- **Màn hình Onboarding (Thiết lập Profile):** Chọn Vai trò, Nhập tiểu sử cá nhân và Chọn kỹ năng từ danh sách gợi ý.

### 7.2.2 Nhóm màn hình Tính năng Cốt lõi
- **Màn hình Khám phá (Discover / Swipe):** Hiển thị thẻ Project hoặc Thẻ Ứng viên (tùy role). Thể hiện tỷ lệ phù hợp tính bằng AI và danh sách kỹ năng khớp nhau.
- **Màn hình Quản lý Dự án (Project Dashboard):** Dành cho Owner. Hiển thị tiến độ tuyển dụng (Filled/Total slots), danh sách người đã thích dự án và quản lý trạng thái dự án.
- **Màn hình Quản lý Công việc (Task Board):** Giao diện Kanban kéo thả đơn giản, cho phép chia việc, đặt thời hạn và theo dõi trạng thái công việc (To Do, In Progress, Done).

### 7.2.3 Nhóm màn hình Tương tác
- **Màn hình Hộp thư (Matches & Chat List):** Hiển thị danh sách các Match thành công và các đoạn hội thoại hiện tại, cùng với trạng thái Online theo thời gian thực (Real-time Presence).
- **Màn hình Trò chuyện (Chat Room):** Giao diện nhắn tin, báo đã xem, hiển thị trạng thái đang gửi/lỗi mạng đối với các message thông qua WebSocket.

"""

    # Construct the final content
    new_content = content[:ch5_start] + ch5_content + ch6_content + ch7_content + content[ch8_start:]

    with open(filename, 'w', encoding='utf-8') as f:
        f.write(new_content)

update_file('/home/pet/AndroidStudioProjects/HiBuddy/SE_Report.md')
