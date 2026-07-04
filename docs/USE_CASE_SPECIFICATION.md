# HI BUDDY - TÀI LIỆU ĐẶC TẢ BIỂU ĐỒ USE CASE
## Cập nhật dựa trên cấu trúc hệ thống mới nhất (Tháng 6/2026)

Tài liệu này cung cấp đặc tả chi tiết cho Biểu đồ Use Case để phản ánh chính xác kiến trúc và tính năng của hệ thống hiện tại, bao gồm cả phân hệ AI gợi ý khóa học và mentor.

---

## 1. TÁC NHÂN (ACTORS)

### Tác nhân chính (Primary Actors)
1. **Khách (Guest)** - Người dùng chưa xác thực (Unauthenticated user).
2. **Quản trị viên (Admin)** - Người quản lý hệ thống cao nhất.
3. **Người tham gia (Participant)** - Người dùng cá nhân đã xác thực, tham gia hệ thống để tìm kiếm cơ hội và nhóm.
4. **Chủ dự án (Project Owner)** - Người dùng đóng vai trò tạo và quản lý các dự án trên hệ thống.

### Tác nhân phụ (Secondary Actors)
5. **Hệ thống / AI Engine** - Tác nhân tự động của nền tảng thực hiện: Gợi ý khóa học/mentor, phân tích điểm yếu từ feedback, tính toán Vector Embedding, thực thi Hybrid Matching, tự động ẩn người dùng bị report, và gửi thông báo đẩy (FCM).

---

## 2. PHÂN TÍCH CHI TIẾT CÁC PHÂN HỆ (MODULES)

### 2.1. Phân hệ Quản lý tài khoản (Account Management)
**Tác nhân tương tác:** Khách, Quản trị viên (và tất cả user).

**Các Use Case:**
- **Đăng ký**
  - `<<include>>` **Xác thực email sinh viên (OTP):** Bắt buộc đối với đăng ký bằng email truyền thống.
  - `<<include>>` **Xác thực Google OAuth:** Phương thức đăng ký/đăng nhập tự động kết nối tài khoản.
- **Đăng nhập**
  - `<<extend>>` **Quên mật khẩu**
  - `<<extend>>` **Đăng nhập bằng Google**
- **Đăng xuất**

### 2.2. Phân hệ Quản lý hệ thống (System Management)
**Tác nhân tương tác:** Quản trị viên.

**Các Use Case:**
- **Quản lý hệ thống**
  - `<<include>>` **Xử lý report**
  - `<<include>>` **Duyệt dự án**
  - `<<extend>>` **Khóa/Xóa tài khoản (Ban/Delete User)**
  - `<<extend>>` **Mở khóa tài khoản**
  - `<<extend>>` **Xóa dự án**
- **Xem nhật ký kiểm toán (Audit Log)**

### 2.3. Phân hệ Phát triển kỹ năng (Skill Development)
**Tác nhân tương tác:** Người tham gia, Hệ thống / AI Engine.

**Các Use Case:**
- **Gợi ý khóa học & Mentor** - *Hệ thống / AI Engine tự động chạy*
  - AI đọc các task trễ hạn (LATE) và kết quả phân tích feedback để đưa ra gợi ý khóa học (từ catalog) và mentor (những người có reputation cao).
- **Phân tích điểm yếu từ feedback** - *Hệ thống / AI Engine*
  - Trích xuất điểm yếu từ `AnonymousFeedback`.
- **Xem gợi ý khóa học** - *Người tham gia*
  - `<<extend>>` **Xem chi tiết khóa học**
  - `<<extend>>` **Bỏ qua gợi ý (Dismiss)**
  - `<<extend>>` **Làm mới gợi ý (Refresh)**
- **Xem gợi ý mentor** - *Người tham gia*

### 2.4. Phân hệ Quản lý profile (Profile Management)
**Tác nhân tương tác:** Người tham gia, Chủ dự án, Hệ thống / AI Engine.

**Các Use Case:**
- **Ẩn profile**
  - `<<extend>>` **Hiện profile**
- **Quản lý profile**
  - `<<include>>` **Xem profile**
  *Từ "Xem profile":*
  - `<<extend>>` **Chỉnh sửa profile**
    - `<<extend>>` **Cập nhật profile:** Kích hoạt Hệ thống cập nhật AI Vector Embeddings trên database Milvus.
  - `<<extend>>` **Quản lý kỹ năng (Skills)**
  - `<<extend>>` **Quản lý sở thích (Interests)**
  - `<<extend>>` **Quản lý vai trò (Roles)**
  - `<<extend>>` **Xem tóm tắt feedback**

### 2.5. Phân hệ Swipe & Matching (Ghép cặp)
**Tác nhân tương tác:** Người tham gia, Hệ thống / AI Engine.

**Các Use Case:**
- **Tạo danh sách gợi ý (Generate Swipe Deck)** - *Hệ thống / AI Engine*
  - Chạy luồng Hybrid Matching (Deterministic + Vector Cosine similarity).
- **Swipe card (Quẹt thẻ)**
  - `<<extend>>` **Pass (Bỏ qua)**
  - `<<extend>>` **Like (Thích)**
    - `<<include>>` **Tạo match:** Nếu 2 bên cùng Like.
  - `<<extend>>` **Xem trạng thái online**
- **Chat sau match**
  - `<<extend>>` **Gửi tin nhắn**
  - `<<extend>>` **Xem typing indicator**
  - `<<extend>>` **Gửi lời mời dự án**
  - `<<extend>>` **Unmatch**

### 2.6. Phân hệ Quản lý Project (Project Management)
**Tác nhân tương tác:** Chủ dự án, Người tham gia.

**Các Use Case (Chủ dự án):**
- **Quản lý project**
  - `<<include>>` **Xem project**
  *Từ "Quản lý project":*
  - `<<extend>>` **Đăng project:** Phải thiết lập Role Slot.
  - `<<extend>>` **Cập nhật project**
  - `<<extend>>` **Đóng project:** Kích hoạt gửi Đánh giá & Feedback.
  - `<<extend>>` **Xóa project**
- **Xem ứng viên**
  - `<<include>>` **Phê duyệt thành viên**
  - `<<extend>>` **Từ chối ứng viên**
- **Gửi lời mời dự án**

**Các Use Case (Người tham gia):**
- **Tham gia dự án**
  - `<<extend>>` **Xem project**
  - `<<extend>>` **Gửi yêu cầu tham gia (Apply)**
- **Rời khỏi dự án**

### 2.7. Phân hệ Quản lý Task (Task Management)
**Tác nhân tương tác:** Chủ dự án, Người tham gia.

**Các Use Case (Chủ dự án):**
- **Quản lý task**
  - `<<include>>` **Tạo task:**
    - `<<include>>` **Phân công task**
  - `<<extend>>` **Sửa task**
  - `<<extend>>` **Đóng task**
  - `<<extend>>` **Xóa task**
- **Theo dõi tiến độ (Kanban)**

**Các Use Case (Người tham gia):**
- **Theo dõi task của tôi**
  - `<<extend>>` **Cập nhật trạng thái task:** Lưu log `TaskCheckoutHistory`.
  - `<<extend>>` **Báo cáo hoàn thành**

### 2.8. Phân hệ Đánh giá & Feedback (Sau dự án)
**Tác nhân tương tác:** Chủ dự án, Người tham gia, Hệ thống / AI Engine.

**Các Use Case:**
- **Thực hiện đánh giá thành viên (Project Evaluation)** - *Chủ dự án*
- **Gửi feedback ẩn danh (Anonymous Feedback)** - *Người tham gia*
- **Phân tích feedback bằng AI** - *Hệ thống / AI Engine*

### 2.9. Phân hệ Trust & Safety (Kiểm duyệt an toàn)
**Tác nhân tương tác:** Người tham gia, Hệ thống / AI Engine.

**Các Use Case:**
- **Báo cáo vi phạm (Report)**
- **Tự động gắn cờ (Auto-flagging)** - *Hệ thống / AI Engine*
- **Chặn người dùng (Block)**

---

## 3. TỔNG HỢP MỐI QUAN HỆ CHÍNH

### Include Relationships (Bắt buộc)
- Đăng ký → Xác thực email sinh viên (OTP) / Xác thực Google OAuth
- Quản lý hệ thống → Xử lý report / Duyệt dự án
- Like → Tạo match (Nếu 2 bên cùng Like)
- Tạo match → Chat sau match
- Tạo task → Phân công task
- Quản lý profile → Xem profile
- Quản lý project → Xem project
- Xem ứng viên → Phê duyệt thành viên

### Các luồng tự động / Nền (Background System Triggers)
- Đóng project → Kích hoạt đánh giá
- Cập nhật profile → Cập nhật AI Vector Embeddings
- Task trễ/Feedback có điểm yếu → Tạo gợi ý khóa học & mentor
- User bị report đủ số lượng → Auto-flagging

---

**Phiên bản tài liệu:** 2.1 (Đã khôi phục và tinh chỉnh phân hệ Gợi ý khóa học)  
**Ngày cập nhật:** Tháng 6/2026  
