# HoziTech — Frontend Specification

## Tổng quan

**Tên**: HoziTech — Cửa hàng công nghệ  
**Stack gợi ý**: React + TypeScript, Tailwind CSS, Framer Motion (motion/react), react-icons  
**UI/UX**: Light Theme & Glassmorphism (nền slate-50/trắng, hiệu ứng kính mờ backdrop-blur cho nav/cards), font không chân hiện đại (Inter, Roboto), bo góc cực đại (rounded-2xl hoặc rounded-3xl), hệ màu gradient rực rỡ (Primary: from-purple-600 to-blue-500, Accent: đỏ/hồng, cam/vàng, xanh biển). Bố cục lưới (Grid) linh hoạt thoáng đãng, ngăn cách section mảng bg-slate-100 bo góc. Hover phóng to nhẹ (scale-105) và có shadow ngả gradient. Biểu tượng dùng hệ thống icon mảnh hiện đại dạng Feather. Banner auto-slide.  
**Roles**: Guest (chưa login), User (khách hàng), Admin (quản trị)

---

## Sitemap & Luồng chuyển trang

```mermaid
graph TD
    HOME[🏠 Trang chủ] --> PRODUCTS[📦 Danh sách SP]
    HOME --> CATEGORY[📂 SP theo danh mục]
    HOME --> PRODUCT_DETAIL[🔍 Chi tiết SP]
    HOME --> LOGIN[🔐 Đăng nhập]
    HOME --> REGISTER[📝 Đăng ký]

    PRODUCTS --> PRODUCT_DETAIL
    CATEGORY --> PRODUCT_DETAIL
    PRODUCT_DETAIL --> CART[🛒 Giỏ hàng (Cần login)]
    PRODUCT_DETAIL --> LOGIN

    CART --> CHECKOUT[💳 Thanh toán]
    CART --> PRODUCT_DETAIL

    CHECKOUT --> ORDER_SUCCESS[✅ Đặt hàng thành công]
    CHECKOUT --> VNPAY[🏦 VNPay/Momo]
    VNPAY --> ORDER_SUCCESS

    ORDER_SUCCESS --> ORDER_DETAIL[📋 Chi tiết đơn]
    ORDER_SUCCESS --> HOME

    LOGIN --> REGISTER
    LOGIN --> FORGOT_PW[🔑 Quên mật khẩu]
    REGISTER --> LOGIN
    HOME --> ABOUT[🏢 Giới thiệu]
    HOME --> FAQ[❓ Câu hỏi thường gặp]
    HOME --> TERMS[📜 Chính sách & Điều khoản]

    subgraph USER_PAGES [👤 Khu vực User - Cần đăng nhập]
        PROFILE[Hồ sơ cá nhân]
        ADDRESSES[Quản lý địa chỉ]
        ORDERS[Lịch sử đơn hàng]
        ORDER_DETAIL
        NOTIFICATIONS[Thông báo]
        CHANGE_PW[Đổi mật khẩu]
    end

    subgraph ADMIN_PAGES [⚙️ Khu vực Admin]
        DASHBOARD[Dashboard]
        ADMIN_PRODUCTS[Quản lý SP]
        ADMIN_ORDERS[Quản lý đơn hàng]
        ADMIN_USERS[Quản lý người dùng]
    ADMIN_PRODUCTS --> ADMIN_CATEGORIES
    ADMIN_ORDERS --> ADMIN_FEEDBACKS
    ADMIN_USERS --> ADMIN_COUPONS

    subgraph GLOBAL_COMPONENTS [🌐 Global]
        CHATBOT[🤖 AI Chatbot - Beebot]
    end
```

---

## A. TRANG KHÁCH (Guest + User)

### 1. 🏠 Trang chủ — `/`

| Khu vực              | Nội dung                                                                     |
| -------------------- | ---------------------------------------------------------------------------- |
| **Header**           | Logo, thanh tìm kiếm, icon giỏ hàng (số lượng), icon thông báo, avatar/login |
| **Banner**           | Carousel ảnh khuyến mãi (auto-slide)                                         |
| **Danh mục nổi bật** | Grid icon các danh mục chính (click → trang danh mục)                        |
| **Flash Sale**       | Countdown + grid SP đang giảm giá, hiện giá gốc gạch ngang + giá sale        |
| **SP mới nhất**      | Grid 8-12 sản phẩm mới (sort by createdAt DESC)                              |
| **SP bán chạy**      | Grid 8-12 SP bán chạy (sort by sold quantity DESC)                           |
| **Thương hiệu**      | Logo các brand nổi bật                                                       |
| **Footer**           | Thông tin liên hệ, chính sách, link mạng xã hội                              |

**Chuyển trang**: Click SP → `/products/:slug` | Click danh mục → `/category/:slug` | Click "Xem tất cả" → `/products`

---

### 2. 📦 Danh sách sản phẩm — `/products`

| Chức năng            | Chi tiết                                                                                                      |
| -------------------- | ------------------------------------------------------------------------------------------------------------- |
| **Bộ lọc (sidebar)** | Danh mục (checkbox tree), Thương hiệu (checkbox), Khoảng giá (slider/input min-max), Đánh giá (từ 4★ trở lên) |
| **Thanh sort**       | Phổ biến / Mới nhất / Giá thấp → cao / Giá cao → thấp / Bán chạy                                              |
| **Grid SP**          | Card: ảnh, tên, giá (giá gốc gạch + giá sale), rating ★, đã bán X                                             |
| **Pagination**       | Page numbers + prev/next, hiện "Trang 1/5 (48 sản phẩm)"                                                      |
| **Tìm kiếm**         | Keyword từ header → filter kết quả                                                                            |

**API**: `GET /api/v1/products?keyword=...&categoryId=...&brand=...&minPrice=...&maxPrice=...&sortBy=price&sortDir=asc&page=1&size=12`  
→ Dùng [ProductSpecification](file:///Users/hoinguyen/Documents/SpringFW/TechShop/server/src/main/java/com/hoz/hozitech/application/specifications/ProductSpecification.java#12-60) + [PaginationUtils](file:///Users/hoinguyen/Documents/SpringFW/DATN_HAVU/server/src/main/java/com/project/tmartweb/utils/PaginationUtils.java#8-30)

---

### 3. 📂 SP theo danh mục — `/category/:slug`

Giống trang `/products` nhưng đã chọn sẵn danh mục. Hiện **breadcrumb**: Trang chủ > Điện tử > Điện thoại. Nếu danh mục có con → hiện sub-categories.

---

### 4. 🔍 Chi tiết sản phẩm — `/products/:slug`

| Khu vực             | Nội dung                                                                       |
| ------------------- | ------------------------------------------------------------------------------ |
| **Breadcrumb**      | Trang chủ > Danh mục > Tên SP                                                  |
| **Gallery ảnh**     | Ảnh chung sản phẩm. Khi chọn variant → đổi sang ảnh riêng của variant đó       |
| **Thông tin chính** | Tên, rating ★ (X đánh giá), đã bán Y. Tóm tắt nhanh **cấu hình nổi bật**       |
| **Chọn biến thể**   | Các button màu sắc/dung lượng (VD: 8GB/256GB). Click → đổi giá + ảnh + tồn kho |
| **Giá**             | `compareAtPrice` (gạch ngang) + `price` (đỏ to) + % giảm                       |
| **Số lượng**        | Input +/- với max = stock của variant đang chọn                                |
| **Nút hành động**   | [Thêm vào giỏ] [Mua ngay]                                                      |
| **Nội dung**        | Tab: **Mô tả chi tiết** / **Bảng Thông Số Kỹ Thuật** / **Đánh giá**            |
| **Đánh giá**        | List feedback + ảnh, filter theo số sao, pagination                            |
| **SP tương tự**     | Grid 4-6 SP cùng danh mục                                                      |

**Luồng chọn variant**:

```
User click "Màu đỏ"
→ FE gọi GET /api/v1/products/{id}/images?variantId={variantId}
→ Gallery đổi sang ảnh đỏ
→ Giá, tồn kho cập nhật theo variant đỏ
→ Nút "Thêm giỏ" gửi variantId
```

**Chuyển trang**: [Thêm giỏ] → toast "Đã thêm" | [Mua ngay] → `/checkout` | Chưa login → redirect `/login?redirect=/products/:slug`

---

### 5. 🛒 Giỏ hàng — `/cart`

| Chức năng     | Chi tiết                                                                   |
| ------------- | -------------------------------------------------------------------------- |
| **Danh sách** | Ảnh, tên SP, tên variant, đơn giá, input số lượng +/-, thành tiền, nút xóa |
| **Chọn SP**   | Checkbox từng item + checkbox "Chọn tất cả"                                |
| **Tóm tắt**   | Tổng tiền hàng (các item đã chọn), nút [Mua hàng]                          |
| **Giỏ trống** | Illustration + "Giỏ hàng trống" + nút [Mua sắm ngay] → `/products`         |

**Lưu ý quan trọng**: Giỏ hàng **chỉ dành cho User đã đăng nhập**. Guest (Chưa đăng nhập) click "Thêm vào giỏ" ở bất kỳ đâu đều bị redirect sang trang `/login`. Không sử dụng LocalStorage cho giỏ hàng của Guest.

**Chuyển trang**: [Mua hàng] → `/checkout` (chỉ gồm items đã check) | Click tên SP → `/products/:slug`

---

### 6. 💳 Thanh toán — `/checkout` _(cần login)_

| Bước                       | Nội dung                                                          |
| -------------------------- | ----------------------------------------------------------------- |
| **Địa chỉ giao hàng**      | Chọn từ danh sách Address đã lưu, hoặc [Thêm địa chỉ mới] (modal) |
| **Danh sách SP**           | Readonly: ảnh, tên, variant, số lượng, giá                        |
| **Phương thức thanh toán** | Radio: COD / VNPay / Momo / Chuyển khoản                          |
| **Mã giảm giá**            | Input code + [Áp dụng] → hiện discount amount hoặc lỗi            |
| **Tổng kết**               | Tiền hàng, phí ship, giảm giá, **tổng thanh toán**                |
| **Ghi chú**                | Textarea (optional)                                               |
| **Nút**                    | [Đặt hàng]                                                        |

**Luồng đặt hàng**:

```
Click [Đặt hàng]
→ POST /api/v1/orders (body: addressId, items, paymentMethod, couponCode, note)
→ Nếu COD → redirect /order-success/:orderNumber
→ Nếu VNPay → redirect sang trang VNPay → callback → /order-success/:orderNumber
→ Nếu lỗi (hết hàng, coupon invalid) → hiện toast lỗi
```

---

### 7. ✅ Đặt hàng thành công — `/order-success/:orderNumber`

Hiện: icon ✓, mã đơn hàng, tóm tắt đơn, [Xem chi tiết đơn] [Tiếp tục mua sắm]

---

### 8. 🔐 Đăng nhập — `/login`

| Field            | Validation                |
| ---------------- | ------------------------- |
| Email/Username   | required                  |
| Password         | required, min 6 ký tự     |
| [Quên mật khẩu?] | Link → `/forgot-password` |
| [Đăng ký]        | Link → `/register`        |

**Luồng**: Submit → POST /api/v1/auth/login → nhận accessToken + refreshToken → lưu localStorage → redirect về trang trước (hoặc `/`)

---

### 9. 📝 Đăng ký — `/register`

| Field       | Validation                     |
| ----------- | ------------------------------ |
| Họ tên      | required                       |
| Email       | required, email format, unique |
| SĐT         | required, format VN            |
| Mật khẩu    | required, min 6                |
| Xác nhận MK | match password                 |

---

### 10. 🔑 Quên mật khẩu — `/forgot-password`

Nhập email → gửi OTP → xác nhận OTP → nhập mật khẩu mới → redirect `/login`

---

## B. TRANG TĨNH & LỖI NGOẠI LỆ (Static & Fallback Pages)

### 11. 🏢 Giới thiệu — `/about-us`

| Khu vực      | Nội dung                                               |
| ------------ | ------------------------------------------------------ |
| **Banner**   | Hình ảnh đội ngũ/kho hàng Hozitech hoành tráng         |
| **Nội dung** | Tầm nhìn, Sứ mệnh, Giá trị cốt lõi, Lịch sử hình thành |
| **Cam kết**  | Bảo hành 24 tháng, Lỗi đổi mới, Giao hỏa tốc 2h        |

### 12. 📜 Chính sách & Điều khoản — `/terms`

- Menu dọc bên trái chuyển các tab: Điều khoản sử dụng, Chính sách bảo mật, Chính sách đổi trả/bảo hành.
- Khung nội dung text bên phải, padding rộng, chữ to dễ đọc, hỗ trợ SEO.

### 13. ❓ Câu hỏi thường gặp — `/faq`

- Thanh tìm kiếm từ khóa. Danh sách liệt kê dạng **Accordion** chia theo nhóm chủ đề (Giao hàng, Thanh toán, Bảo hành).
- Thông tin liên hệ Hotline hỗ trợ trực tiếp.

### 14. 🚫 Trang Ngoại lệ (404 / 403 / Maintenance)

- **404 Not Found:** Hiển thị khi User truy cập Link hỏng. Ảnh minh hoạ lạc vũ trụ + Nút "Quay về trang chủ".
- **403 Forbidden:** Hiển thị khi User thường cố tình vào link Admin. Giao diện cảnh sát chặn cửa.
- **503 Maintenance:** Giao diện đang bảo trì hệ thống.

---

## C. KHU VỰC USER (cần đăng nhập)

Layout: Sidebar trái (menu) + Content phải

### 11. 👤 Hồ sơ cá nhân — `/account/profile`

Chỉnh sửa: họ tên, email, SĐT, ngày sinh, giới tính, avatar (upload ảnh)

### 12. 📍 Quản lý địa chỉ — `/account/addresses`

| Chức năng | Chi tiết                                                                                       |
| --------- | ---------------------------------------------------------------------------------------------- |
| Danh sách | Card: tên, SĐT, địa chỉ đầy đủ, badge "Mặc định"                                               |
| Thêm/sửa  | Modal: province → district → ward (cascading dropdown) + chi tiết, checkbox "Đặt làm mặc định" |
| Xóa       | Confirm dialog                                                                                 |

### 13. 📋 Lịch sử đơn hàng — `/account/orders`

| Chức năng     | Chi tiết                                                                    |
| ------------- | --------------------------------------------------------------------------- |
| Tabs          | Tất cả / Chờ xác nhận / Đang giao / Đã giao / Đã hủy                        |
| Card đơn hàng | Mã đơn, ngày đặt, trạng thái (badge màu), tổng tiền, danh sách SP thumbnail |
| Tìm kiếm      | Theo mã đơn hoặc tên SP                                                     |
| Hành động     | [Xem chi tiết] [Hủy đơn] (nếu PENDING) [Mua lại] [Đánh giá] (nếu SHIPPED)   |

### 14. 📋 Chi tiết đơn hàng — `/account/orders/:orderNumber`

| Khu vực                 | Nội dung                                                                       |
| ----------------------- | ------------------------------------------------------------------------------ |
| **Timeline**            | Trạng thái đơn: Đặt hàng → Xác nhận → Đang giao → Đã giao (có timestamp)       |
| **Thông tin giao hàng** | Tên, SĐT, địa chỉ, mã vận đơn (nếu có)                                         |
| **Danh sách SP**        | Ảnh, tên, variant, số lượng, giá                                               |
| **Thanh toán**          | Tiền hàng, ship, giảm giá, tổng, phương thức thanh toán, trạng thái thanh toán |
| **Đánh giá**            | Nếu SHIPPED → form đánh giá (chọn sao, viết nội dung, upload ảnh) cho từng SP  |

### 15. 🔔 Thông báo — `/account/notifications`

Danh sách thông báo: icon type, tiêu đề, nội dung, thời gian, đánh dấu đã đọc/chưa đọc. Nút "Đánh dấu tất cả đã đọc". Pagination.

### 16. 🔒 Đổi mật khẩu — `/account/change-password`

Mật khẩu cũ → Mật khẩu mới → Xác nhận MK mới

---

## D. KHU VỰC ADMIN — `/admin/*`

Layout: Sidebar trái (menu quản trị) + Header (admin name, notifications) + Content

### 17. 📊 Dashboard — `/admin`

| Widget                  | Nội dung                                                                                                                                                                                                                                             |
| ----------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Thẻ thống kê (Cards)**| - **Doanh thu**: Tổng doanh thu (chia theo Ngày, Tuần, Tháng, Năm)<br>- **Đơn hàng**: Số đơn mới (Hover/Click hiện Tổng số đơn)<br>- **Khách hàng**: Số khách mới (Hover/Click hiện Tổng User)<br>- **Sản phẩm**: Số SP bán ra<br>- **Hoàn/Hủy**: Số lượng (Hover/Click hiện Pie chart chi tiết)<br>- **Đánh giá**: Số đánh giá mới (Hover/Click hiện Tổng số) |
| **Biểu đồ (Charts)**    | - **Line chart**: Theo 7 ngày gần nhất, và theo tháng<br>- **Bar chart**: Theo quý                                                                                                                                                                   |
| **Danh sách Top**       | - **Top SP bán chạy nhất**<br>- **Top Danh mục (Category) bán chạy nhất**<br>- **Top khách hàng tiềm năng**<br>- **Đơn hàng gần nhất** (dạng bảng tóm tắt)                                                                                           |
| **Thống kê Đánh giá**   | Phân bố tổng số đánh giá (0 -> 5 sao) và Thống kê đánh giá trong tháng này                                                                                                                                                                           |

### 18. 📦 Quản lý sản phẩm — `/admin/products`

| Chức năng           | Chi tiết                                                                                       |
| ------------------- | ---------------------------------------------------------------------------------------------- |
| **Bảng**            | Ảnh, tên, danh mục, giá, tồn kho (tổng variants), trạng thái, ngày tạo                         |
| **Bộ lọc**          | Keyword, danh mục, brand, trạng thái, khoảng giá                                               |
| **Hành động**       | [Thêm SP] [Sửa] [Ẩn/Hiện] [Xóa]                                                                |
| **Trang thêm/sửa**  | Form: tên, slug (auto-generate), mô tả (rich editor), danh mục (dropdown tree), brand, giá gốc |
| **Quản lý variant** | Bảng con: thêm/sửa/xóa variant (SKU, tên, giá, giá so sánh, tồn kho)                           |
| **Quản lý ảnh**     | Upload ảnh chung + ảnh theo variant. Drag & drop sắp xếp thứ tự. Đánh dấu ảnh chính            |

### 19. 📋 Quản lý đơn hàng — `/admin/orders`

| Chức năng     | Chi tiết                                                                                                                             |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| **Bảng**      | Mã đơn, khách hàng, tổng tiền, trạng thái (badge), PTTT, ngày đặt                                                                    |
| **Bộ lọc**    | Trạng thái (tabs), keyword (mã đơn/tên/SĐT), khoảng ngày                                                                             |
| **Chi tiết**  | Modal/page: timeline, thông tin KH, danh sách SP, thanh toán                                                                         |
| **Hành động** | [Xác nhận] (PENDING→CONFIRMED), [Giao hàng] + nhập mã vận đơn (CONFIRMED→SHIPPING), [Đã giao] (SHIPPING→SHIPPED), [Hủy] + nhập lý do |

### 20. 👥 Quản lý người dùng — `/admin/users`

| Chức năng     | Chi tiết                                            |
| ------------- | --------------------------------------------------- |
| **Bảng**      | Avatar, tên, email, SĐT, role, ngày tạo, trạng thái |
| **Bộ lọc**    | Keyword, role, khoảng ngày                          |
| **Hành động** | [Xem chi tiết] [Đổi role] [Vô hiệu hóa]             |

### 21. 📂 Quản lý danh mục — `/admin/categories`

Bảng tree: tên, slug, ảnh, số SP, trạng thái. [Thêm] [Sửa] [Ẩn/Hiện]. Kéo thả sắp xếp thứ tự. Chọn danh mục cha khi thêm/sửa.

### 22. 🎟️ Quản lý mã giảm giá — `/admin/coupons`

Bảng: code, giảm giá (% hoặc cố định), đơn tối thiểu, giảm tối đa, đã dùng/tổng, hạn, trạng thái. [Thêm] [Sửa] [Tắt].

### 23. ⭐ Quản lý đánh giá — `/admin/feedbacks`

Bảng: SP, user, rating, nội dung, ảnh, ngày, trạng thái trả lời. [Trả lời] (nhập reply) [Ẩn].

### 24. 🤖 Quản lý AI Chatbot

Tích hợp giao diện quản lý kịch bản cho Alibaba Cloud Beebot, thống kê lượt chat, tỷ lệ giải quyết tự động, và lịch sử chat log của User.

---

## E. COMPONENTS DÙNG CHUNG

| Component                                                                                                                                         | Sử dụng ở                                               |
| ------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------- |
| `Header`                                                                                                                                          | Tất cả trang khách                                      |
| `Footer`                                                                                                                                          | Tất cả trang khách                                      |
| `AdminSidebar`                                                                                                                                    | Tất cả trang admin                                      |
| `ProductCard`                                                                                                                                     | Trang chủ, danh sách SP, SP tương tự                    |
| [Pagination](file:///Users/hoinguyen/Documents/SpringFW/DATN_HAVU/server/src/main/java/com/project/tmartweb/domain/paginate/Pagination.java#8-21) | Danh sách SP, đơn hàng, đánh giá, thông báo, bảng admin |
| `Breadcrumb`                                                                                                                                      | Chi tiết SP, danh mục                                   |
| `ImageGallery`                                                                                                                                    | Chi tiết SP                                             |
| `VariantSelector`                                                                                                                                 | Chi tiết SP, giỏ hàng                                   |
| `AddressForm`                                                                                                                                     | Checkout, quản lý địa chỉ                               |
| `OrderTimeline`                                                                                                                                   | Chi tiết đơn hàng                                       |
| `StarRating`                                                                                                                                      | Đánh giá, card SP                                       |
| `SearchBar`                                                                                                                                       | Header                                                  |
| `AiChatbotWidget`                                                                                                                                 | Nổi bật góc dưới phải ở mọi trang (Alibaba Beebot)      |
| `Toast/Notification`                                                                                                                              | Global                                                  |
| `Modal`                                                                                                                                           | Confirm, form nhập                                      |
| `DataTable`                                                                                                                                       | Tất cả bảng admin                                       |

---

## F. LUỒNG CHÍNH (User Flows)

### Flow 1: Mua hàng

```
Trang chủ → Tìm kiếm/lọc → Chi tiết SP → Chọn variant
→ Bấm [Thêm giỏ] → (Nếu chưa login) Redirect Login → Login thành công
→ Thêm giỏ thành công → Giỏ hàng → Checkout → Chọn địa chỉ → Chọn PTTT → Nhập coupon
→ Đặt hàng → (VNPay?) → Thành công → Xem đơn hàng
```

### Flow 2: Đánh giá sản phẩm

```
Lịch sử đơn hàng → Đơn "Đã giao" → Chi tiết → [Đánh giá]
→ Chọn sao + viết review + upload ảnh → Submit
```

### Flow 3: Admin xử lý đơn

```
Dashboard → Đơn mới → [Xác nhận] → [Giao hàng] + nhập mã vận đơn
→ Shipper giao → [Đã giao]
```

### Flow 4: Admin thêm sản phẩm

```
Quản lý SP → [Thêm SP] → Nhập thông tin chung → Tạo variants
→ Upload ảnh chung + ảnh cho từng variant → [Lưu]
```

---

## G. CÁC NGHIỆP VỤ THỰC CHIẾN (NON-FUNCTIONAL & CORE LOGIC)

Đây là các nghiệp vụ ngầm cực kỳ quan trọng đòi hỏi Frontend Developer phải xử lý kỹ thuật chặt chẽ ngoài phần hiển thị UI:

### 1. Logic Refresh Token (Axios Interceptor) 🔐

- Ứng dụng phải cài đặt **Axios Interceptor**.
- Bất cứ khi nào gọi API trả về lỗi `401 Unauthorized` do Token hết hạn (Access Token lấy từ `localStorage`), Frontend không được phép văng ngay ra trang Login.
- Phải tự động background call API `/api/v1/auth/refresh-token` gửi Refresh Token lên để lấy Access Token mới.
- Sau khi được cấp Access Token mới, Frontend tiếp tục vòng lặp tự động gửi lại Request đang bị lỗi dở dang. Chỉ khi Refresh Token cũng hết hạn (lỗi `403/401` tiếp) mới ép user đăng nhập lại.

### 2. Đăng nhập Mạng xã hội (Social OAuth2) 🌐

- Hỗ trợ Đăng nhập rảnh tay bằng **Google** hoặc **Facebook**.
- **Luồng Frontend**: Client redirect user sang màn ủy quyền của Google. Sau khi user đồng ý, Google ném về Client một đoạn Auth Code trên thanh URL.
- Frontend gom Authorization Code này + Provider name gửi xuống Backend API `POST /api/v1/auth/social-login`. Backend sẽ trả ngược lại Access Token & Refresh Token, user sẽ đăng nhập thành công.

### 3. Route Guards & Fallback Chặt chẽ hơn 🛡️

- Mặc định các trang `/account/*` và `/admin/*` đều bọc trong một Higher Order Component (HOC) `<ProtectedRoute />` chặn Guest.
- Bổ sung **chặn logic kinh doanh**:
  - Truy cập màn `/checkout` nhưng hàm kiểm tra giỏ hàng rỗng `cartItems.length === 0` → Ép redirect trả về màn `/cart`.
  - User đã Authenticated mà cố tình truy cập link `/login` hoặc `/register` → Ép redirect về `/`.
  - Truy cập màn kết quả đơn hàng `/order-success` khi State ứng dụng không chứa `orderId` thực thi trước đó → Ép redirect về `/`.

### 4. SEO & Meta Tags Tương tác (Open Graph) 🚀

- Các thẻ `<title>` và `<meta description>` phải thay đổi theo Page. Cần viết một wrapper như `<SEOHead />` tiêm vào Root Layout.
- **Quan trọng nhất là trang Product Detail (`/products/:slug`)**: Phải render dynamic các thẻ `og:title`, `og:image`, `og:description` dựa trên hình ảnh / thông tin sản phẩm.
  - Điều này giúp khi User dán Link sản phẩm lên Facebook Messenger / Zalo, tin nhắn có chứa Thumbnail sản phẩm kèm tiêu đề chuẩn (Rich Preview).

### 5. Debounce & Tối ưu Gọi API ⚡

- **Omni Search (Thanh tìm kiếm)**: Chặn gọi API liên tục theo từng ký tự user gõ. Bắt buộc có cơ chế **Debounce Delay 400ms-500ms** (Khi user ngừng gõ được nửa giây thì Frontend mới Call Query đi).
- **Cart Actions (Thêm Giỏ / Đổi số lượng +/-)**: Để ngăn User spam click gây lỗi trừ tồn kho hoặc DDoS nhẹ Database, các nút gọi API thao tác dữ liệu phải có trạng thái **Disabled / Loading State** hoặc Implement cơ chế **Debounce Callback** trước khi gửi Request PATCH.
# 🌐 HOZITECH — BỘ PROMPT THIẾT KẾ GIAO DIỆN FRONTEND

## 🎨 QUY CHUẨN THIẾT KẾ CHUNG (ÁP DỤNG CHO TẤT CẢ PROMPT)

> **Tên website:** Hozitech
> **Ngôn ngữ giao diện:** Tiếng Việt
> **Tech Stack:** React 18 / Next.js, TailwindCSS, Framer Motion, Recharts, React Icons

| Thuộc tính          | Giá trị                                                                                                                                                                              |
| ------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Theme mặc định**  | ☀️ **Light Theme & Glassmorphism** (sử dụng nền `slate-50` hoặc trắng, kết hợp hiệu ứng kính mờ `backdrop-blur` cho thanh điều hướng/thẻ thành phần)                                 |
| **Màu chủ đạo**     | **Gradient rực rỡ**: Primary (`from-purple-600 to-blue-500`), Flash Sale (`from-red-500 to-pink-500`), Trending (`from-orange-500 to-yellow-500`), New (`from-blue-500 to-cyan-500`) |
| **Font chữ**        | Font chữ **không chân hiện đại** (VD: Inter, Roboto hoặc hệ thống mặc định của Apple/Google)                                                                                         |
| **Bo góc**          | Bo góc cực đại (**rounded-2xl** hoặc **rounded-3xl**) cho tất cả khung hình, thẻ sản phẩm, nút bấm                                                                                   |
| **Bố cục / Layout** | Sử dụng lưới (**Grid**) linh hoạt, khoảng cách rộng rãi, ngăn cách các section bằng màu nền nhẹ (`bg-slate-100`) kèm bo góc lớn                                                      |
| **Hiệu ứng/Shadow** | Hover: Phóng to nhẹ (`scale-105`), đổ bóng rực rỡ (**shadow** ngả màu tương ứng của gradient). Sử dụng **icon mảnh và hiện đại** (Feather/React Icons)                               |
| **Chuyển động**     | Sử dụng **Framer Motion (`motion/react`)** cho hiệu ứng chuyển cảnh mềm mại. Mọi Banner tự động slide (auto-slide)                                                                   |
| **Kiến trúc**       | Sử dụng React, TypeScript, Tailwind CSS. Chia nhỏ Component mạch lạc                                                                                                                 |
| **Dữ liệu**         | Mock data phong phú, logic SPA, không reload trang, transitions mượt                                                                                                                 |

---

## 📌 PROMPT 1 — KHÁM PHÁ & TÌM KIẾM (5 Màn Hình)

**Yêu cầu chung:** Áp dụng đầy đủ Quy chuẩn Thiết kế: Giao diện tiếng Việt, **Light Theme & Glassmorphism** (`slate-50`/trắng, `backdrop-blur`), font chữ **không chân hiện đại** (Inter/Roboto), **bo góc cực đại (rounded-2xl/3xl)**, hệ màu **gradient rực rỡ** (Primary: purple→blue, Accent: đỏ/hồng, cam/vàng, xanh/cyan). Bố cục lưới (Grid) thoáng đãng, hiệu ứng hover `scale-105` + shadow màu tương ứng (sử dụng framer-motion), và icon mảnh hiện đại (Feather/react-icons). Tên website: **Hozitech**. Mọi thanh banner lớn phải tự động chạy (auto-slide).

### Màn 1 — Trang Chủ (Home)

- Hero Banner lướt tự động (auto-carousel) với hiệu ứng fade/slide mượt.
- Thanh Search ngang lớn nằm giữa Hero, có **Autocomplete** gợi ý tức thì khi gõ.
- Section sản phẩm theo nhóm:
  - 🔥 **Bán chạy nhất** — lưới card sản phẩm ngang cuộn được.
  - ⚡ **Flash Sale** — có đồng hồ đếm ngược (countdown timer) nổi bật.
  - 🆕 **Hàng mới về** — grid card với badge "Mới".
- Module **"Gợi ý cho bạn"** (AI Recommend) — dựa trên lịch sử duyệt web, hiển thị dạng carousel.
- Footer đầy đủ: Liên kết nhanh, Hotline, Mạng xã hội, Logo Hozitech.

### Màn 2 — Trang Tìm Kiếm / Danh Mục (Search/Category)

- **Sidebar trái** chứa bộ lọc mạnh:
  - 💰 Kéo thanh trượt **Khoảng giá** (Range slider).
  - ☑️ Checkbox: Thương hiệu, RAM, CPU, Dung lượng lưu trữ.
- **Khu vực chính:** Lưới sản phẩm (grid cards) có phân trang.
- **Dropdown Sort** phía trên lưới: Giá tăng/giảm, Bán chạy, Mới nhất.
- Breadcrumb hiển thị đường dẫn danh mục.
- Trạng thái trống: Hiển thị ảnh minh hoạ + text "Không tìm thấy sản phẩm nào".

### Màn 3 — Trang So Sánh Sản Phẩm (Compare)

- Giao diện **bảng (table)** đặt cạnh nhau 2–3 sản phẩm.
- Các hàng so sánh: CPU, RAM, Pin, Màn hình, Giá, Đánh giá.
- Nút **"Thêm sản phẩm"** mở popup chọn SP.
- Highlight hàng có giá trị tốt hơn bằng màu xanh nhạt.
- Nút "Thêm vào giỏ" nằm dưới mỗi cột sản phẩm.

### Màn 4 — Trang Yêu Thích (Wishlist)

- Danh sách dạng **card lưới** các sản phẩm đã lưu.
- Mỗi card có: Ảnh, Tên, Giá, icon ❤️ bỏ yêu thích, nút **"Thêm vào giỏ"**.
- Trạng thái trống: Ảnh minh hoạ + text "Chưa có sản phẩm yêu thích".
- Nút "Tiếp tục mua sắm" dẫn về trang chủ.

### Màn 5 — Trang Bài Viết / Tin Tức Công Nghệ (Tech Blog)

- Layout dạng **trang tin tức công nghệ** (như Tinh Tế, GenK) hiện đại.
- Card bài viết: Ảnh thumbnail, Tiêu đề, Mô tả ngắn, Ngày đăng, Tag danh mục (Đánh giá, Mẹo vặt, Tin đồn).
- Sidebar phải: **Bài viết nổi bật**, Tag cloud, Ô tìm kiếm bài viết.
- Trang chi tiết bài viết: Heading, Nội dung, Ảnh minh hoạ, Bài liên quan bên dưới. Mời review/bình luận công nghệ cuối bài.

---

## 📌 PROMPT 2 — CHI TIẾT SẢN PHẨM & MUA SẮM (5 Màn Hình)

**Yêu cầu chung:** Áp dụng đầy đủ Quy chuẩn Thiết kế: Giao diện tiếng Việt, **Light Theme & Glassmorphism** (`slate-50`/trắng, `backdrop-blur`), font chữ **không chân hiện đại** (Inter/Roboto), **bo góc cực đại (rounded-2xl/3xl)**, hệ màu **gradient rực rỡ** (Primary: purple→blue, Accent: đỏ/hồng, cam/vàng, xanh/cyan). Bố cục lưới (Grid) thoáng đãng, hiệu ứng hover `scale-105` + shadow màu tương ứng (sử dụng framer-motion), và icon mảnh hiện đại (Feather/react-icons). Tên website: **Hozitech**. Mọi thanh banner lớn phải tự động chạy (auto-slide).

### Màn 6 — Trang Chi Tiết Sản Phẩm (Product Detail)

- **Ảnh lớn** có tính năng **zoom khi hover**, carousel thumbnail bên dưới.
- Khu vực thông tin: Tên SP, Giá (gạch giá cũ nếu giảm), Badge "Giảm X%". Hiển thị tóm tắt **cấu hình nổi bật** (VD: Snapdragon 8 Gen 3, RAM 16GB, OLED 120Hz).
- **Chọn phiên bản** (Variants): Nút block chọn RAM/ROM (8GB/256GB, 12GB/512GB) và dung lượng, Màu sắc.
- Nút **"Thêm vào giỏ"** gradient nổi bật + nút **"Mua ngay"**.
- Tab nội dung bên dưới:
  - **Bài viết Mô tả chi tiết** (Kèm ảnh banner dài).
  - **Bảng Thông Số Kỹ Thuật** (Technical Specs): Phải hiển thị dạng bảng sọc ngựa vằn, chia nhóm (Màn hình, Camera, CPU, Pin, Cổng kết nối).
  - Đánh giá sản phẩm.
- Section **"Sản phẩm liên quan"** dạng carousel.

### Màn 7 — Section Đánh Giá (Review & Rating) — nằm trong trang Chi Tiết

- **Thanh tổng quan**: Điểm trung bình (VD: 4.5/5), biểu đồ bar ngang 1–5 sao.
- **Bộ lọc sao** (1–5 sao) + lọc "Có hình ảnh".
- Mỗi comment: Avatar, Tên, Số sao, Nội dung, Hình ảnh thực tế, Ngày.
- Nút "Viết đánh giá" mở modal form (chọn sao + textarea + upload ảnh).

### Màn 8 — Trang Khuyến Mãi / Flash Sale (Promotion)

- **Banner sự kiện** hoành tráng gradient full-width phía trên.
- Grid sản phẩm deal sốc, mỗi card có:
  - Giá gốc gạch ngang, Giá sale nổi bật đỏ, Badge "Giảm X%".
  - **Đồng hồ đếm ngược** cho từng deal.
  - Thanh tiến trình "Đã bán X%".
- Phân loại tab: Đang diễn ra, Sắp diễn ra.

### Màn 9 — Giỏ Hàng & Voucher (Cart)

- Bảng hiển thị SP: Ảnh, Tên, Đơn giá, Số lượng (tăng/giảm), Thành tiền, Nút xóa.
- Checkbox chọn từng SP hoặc "Chọn tất cả".
- **Sidebar/Popup "Chọn Voucher"**: Danh sách mã khả dụng, countdown hạn dùng, nút Áp dụng.
- Tổng cộng: Tạm tính, Giảm giá voucher, Phí ship, **Tổng thanh toán** nổi bật.
- Nút **"Tiến hành thanh toán"** gradient.

### Màn 10 — Thanh Toán Đa Bước & Trạng Thái (Checkout)

- **Progress Bar ngang** 3 bước, bước hiện tại highlight gradient:
  - **Bước 1:** Thông tin giao hàng — Form nhập hoặc chọn địa chỉ lưu sẵn (dropdown).
  - **Bước 2:** Áp dụng Voucher & Xác nhận đơn hàng — Bảng tóm tắt SP.
  - **Bước 3:** Chọn phương thức thanh toán — COD, Chuyển khoản Ngân hàng, Ví điện tử (icon minh hoạ).
- **Trang Kết quả:**
  - ✅ Thành công: Icon check xanh, Mã đơn hàng, Nút "Theo dõi đơn hàng".
  - ❌ Thất bại: Icon cảnh báo đỏ, Lý do, Nút "Thử lại".

---

## 📌 PROMPT 3 — TÀI KHOẢN CÁ NHÂN – PHẦN 1 (5 Màn Hình)

**Yêu cầu chung:** Áp dụng đầy đủ Quy chuẩn Thiết kế: Giao diện tiếng Việt, **Light Theme & Glassmorphism** (`slate-50`/trắng, `backdrop-blur`), font chữ **không chân hiện đại** (Inter/Roboto), **bo góc cực đại (rounded-2xl/3xl)**, hệ màu **gradient rực rỡ** (Primary: purple→blue, Accent: đỏ/hồng, cam/vàng, xanh/cyan). Bố cục lưới (Grid) thoáng đãng, hiệu ứng hover `scale-105` + shadow màu tương ứng (sử dụng framer-motion), và icon mảnh hiện đại (Feather/react-icons). Tên website: **Hozitech**. Mọi thanh banner lớn phải tự động chạy (auto-slide).

**Layout chung:** Sidebar trái chứa menu quản lý cá nhân (Avatar nhỏ + tên trên cùng). Nội dung chi tiết load ở khung bên phải. Chuyển mục không reload trang.

### Màn 11 — Hồ Sơ Cá Nhân (Profile)

- **Avatar** upload được, Tên hiển thị, Email (readonly), Số điện thoại.
- Form **Đổi mật khẩu** (Mật khẩu cũ, Mới, Xác nhận).
- Toggle **Bật/Tắt 2FA** (Xác thực 2 lớp) — hiển thị QR code giả lập.
- **Tùy chọn (Preferences):**
  - Switch Dark/Light mode.
  - Dropdown chọn Ngôn ngữ.
  - Toggle Nhận thông báo email/push.

### Màn 12 — Sổ Địa Chỉ (Address Book)

- Danh sách **card địa chỉ**: Tên người nhận, SĐT, Địa chỉ đầy đủ.
- Badge **"Mặc định"** cho địa chỉ chính.
- Nút: **Đặt mặc định**, Sửa, Xóa trên mỗi card.
- Nút **"+ Thêm địa chỉ mới"** mở modal form.

### Màn 13 — Phương Thức Thanh Toán (Payment Methods)

- Card **thẻ ngân hàng mini** trực quan (hình chữ nhật bo góc, gradient, số thẻ che bớt \*\*\*\*).
- Badge "Mặc định" cho thẻ chính.
- Nút: Đặt mặc định, Xóa.
- Nút **"+ Thêm thẻ mới"** mở modal form (Tên chủ thẻ, Số thẻ, Ngày hết hạn, CVV).

### Màn 14 — Lịch Sử Đơn Hàng (Orders)

- **Tabs lọc:** Tất cả | Đang xử lý | Đang giao | Đã giao | Đã huỷ.
- Mỗi đơn hàng: Card chứa Mã đơn, Ngày, Tổng tiền, Badge trạng thái (màu tương ứng).
- Nút: **"Xem chi tiết"**, **"Mua lại"** (Re-order), "Huỷ đơn" (nếu còn cho phép).
- Click chi tiết mở trang/modal: Danh sách SP, Địa chỉ, Trạng thái, Nút tải **Invoice PDF**.

### Màn 15 — Theo Dõi Đơn Hàng (Order Tracking)

- **UI Stepper dọc** (timeline): Các bước Đặt hàng → Xác nhận → Đang giao → Đã giao.
  - Bước hoàn thành: chấm tròn **xanh** + đường kẻ xanh.
  - Bước chưa tới: chấm tròn **xám** + đường kẻ xám.
- Mỗi bước: Mô tả ngắn + Thời gian cập nhật.
- Thông tin đơn: Mã vận đơn, Đơn vị vận chuyển, Dự kiến giao.
- Nút **"Tải hoá đơn PDF"**.

---

## 📌 PROMPT 4 — TÀI KHOẢN CÁ NHÂN – PHẦN 2 (5 Màn Hình)

**Yêu cầu chung:** Áp dụng đầy đủ Quy chuẩn Thiết kế: Giao diện tiếng Việt, **Light Theme & Glassmorphism** (`slate-50`/trắng, `backdrop-blur`), font chữ **không chân hiện đại** (Inter/Roboto), **bo góc cực đại (rounded-2xl/3xl)**, hệ màu **gradient rực rỡ** (Primary: purple→blue, Accent: đỏ/hồng, cam/vàng, xanh/cyan). Bố cục lưới (Grid) thoáng đãng, hiệu ứng hover `scale-105` + shadow màu tương ứng (sử dụng framer-motion), và icon mảnh hiện đại (Feather/react-icons). Tên website: **Hozitech**. Mọi thanh banner lớn phải tự động chạy (auto-slide).

**Layout chung:** Giữ nguyên layout User Dashboard — Sidebar trái + Nội dung bên phải. Chuyển mục không reload trang.

### Màn 16 — Kho Voucher (My Vouchers)

- Danh sách dạng **vé (ticket card)** — bo góc, đường cắt răng cưa ở giữa.
- Mỗi voucher: Tên mã, Mô tả giảm giá, Hạn dùng, Điều kiện áp dụng.
- **Phân loại tabs:** Có thể dùng | Đã dùng | Hết hạn | Sắp hết hạn (highlight đỏ).
- Nút **"Dùng ngay"** dẫn tới giỏ hàng.

### Màn 17 — Nhận Xét Của Tôi (My Reviews)

- **Tab 1: Cần đánh giá** — List SP đã mua chưa rate, nút "Viết đánh giá" mở modal.
- **Tab 2: Lịch sử đã đánh giá** — List các review đã gửi: Sao, Nội dung, Ảnh.
- Mỗi review có nút **Sửa** (mở modal chỉnh sửa) và **Xóa** (confirm dialog).

### Màn 18 — Sản Phẩm Đã Xem (Recently Viewed)

- Lưới card sản phẩm — giống trang danh mục thu nhỏ.
- Mỗi card: Ảnh, Tên, Giá, Nút **"Thêm vào giỏ"** nhanh.
- Sắp xếp theo thời gian xem gần nhất.
- Nút "Xoá lịch sử" phía trên.

### Màn 19 — Trung Tâm Thông Báo (Notification Center)

- List timeline thông báo theo ngày, mới nhất lên trên.
- Chấm tròn **đỏ** cho thông báo chưa đọc, xám cho đã đọc.
- **Badge phân loại:** 🔔 Hệ thống (System) | 📦 Đơn hàng (Order) | 🎁 Khuyến mãi (Promo).
- Nút **"Đánh dấu tất cả đã đọc"** phía trên.
- Click vào thông báo dẫn tới trang liên quan (VD: đơn hàng, deal…).

### Màn 20 — Hỗ Trợ Khách Hàng (Support / Ticket)

- **Form gửi Ticket:** Dropdown chọn loại vấn đề, Textarea mô tả, Upload ảnh đính kèm.
- Danh sách ticket đã gửi: Mã ticket, Chủ đề, Trạng thái (Đang xử lý / Đã trả lời / Đóng).
- Click vào ticket xem lịch sử chat/reply dạng thread.

### Màn 20.5 — Trợ Lý Ảo AI (Alibaba Cloud Beebot)

- **Widget Icon Chat** lúc nào cũng trôi nổi (floating) ở góc dưới cùng bên phải màn hình. Icon Robot AI phát sáng neon.
- Khi Click mở ra: **Cửa sổ Chatbot Popup** mượt mà.
  - Header: Avatar Robot, Tên "Hozi-AI Assistant", Trạng thái "Đang online".
  - Body: Giao diện chat bong bóng (Bubble chat). Tích hợp hiển thị cả Text lẫn **Card Sản Phẩm thu nhỏ** ngay trong khung chat khi AI tư vấn cấu hình rành mạch.
  - Footer: Khung nhập Text tự động resize độ rộng chữ, Nút Gửi (Icon Paper Plane). Có các nút gợi ý câu hỏi nhanh (Quick replies) dạng Pill-buttons: "Tư vấn Laptop Gaming", "Chính sách bảo hành", "Kiểm tra đơn hàng".

---

## 📌 PROMPT 5 — TỔNG QUẢN TRỊ ADMIN – PHẦN 1 (5 Màn Hình)

**Yêu cầu chung:** Áp dụng đầy đủ Quy chuẩn Thiết kế: Giao diện tiếng Việt, **Light Theme & Glassmorphism** (`slate-50`/trắng, `backdrop-blur`), font chữ **không chân hiện đại** (Inter/Roboto), **bo góc cực đại (rounded-2xl/3xl)**, hệ màu **gradient rực rỡ** (Primary: purple→blue, Accent: đỏ/hồng, cam/vàng, xanh/cyan). Bố cục lưới (Grid) thoáng đãng, hiệu ứng hover `scale-105` + shadow màu tương ứng (sử dụng framer-motion), và icon mảnh hiện đại (Feather/react-icons). Tên website: **Hozitech**. Mọi thanh banner lớn phải tự động chạy (auto-slide).

**Layout Admin:** Sidebar lớn bên trái có **Tree-view menu** phân cấp (cha/con, icon + text). Header trên cùng có avatar admin, thông báo, breadcrumb. Nội dung chính bên phải.

### Màn 21 — Tổng Quan (Overview Dashboard)

- **Các Thẻ chỉ số (Stat Cards)** thiết kế hiện đại, tương tác sâu:
  - **Doanh thu**: Hiển thị tổng doanh thu (chia theo ngày, tuần, tháng, năm).
  - **Đơn hàng**: Số đơn hàng mới (Hover/Click để xem tổng số đơn hàng).
  - **Khách hàng**: Số khách hàng mới (Hover/Click để xem tổng số người dùng).
  - **Sản phẩm**: Số lượng SP bán ra.
  - **Hoàn / Hủy**: Số lượng SP hoàn/hủy (Hover/Click hiển thị biểu đồ Pie phân tích chi tiết).
  - **Đánh giá**: Số lượng đánh giá mới (Hover/Click xem tổng đánh giá).
- **Khu vực Biểu đồ (Charts)**:
  - **Line Chart**: Biểu diễn doanh thu/đơn hàng theo 7 ngày gần nhất, và theo từng tháng.
  - **Bar Chart**: Dạng cột hiển thị doanh thu theo quý.
- **Khu vực Danh sách nổi bật & Bảng dữ liệu (Lists & Tables)**:
  - Bảng **Top sản phẩm bán chạy nhất**.
  - Bảng **Top Category bán chạy nhất**.
  - Bảng **Đơn hàng gần nhất**: Tóm tắt thông tin các đơn hàng mới cập nhật.
  - Bảng **Top khách hàng tiềm năng**: Dựa trên doanh số, tần suất mua hoặc tổng đơn đặt.
- **Thống kê Đánh giá (Reviews Analytics)**:
  - Biểu đồ phân bổ tỷ lệ đánh giá từ 0 đến 5 sao.
  - Thống kê tỷ lệ đánh giá đạt được trong tháng này.

### Màn 22 — Quản Lý Đơn Hàng (Admin Orders)

- **Data table** đầy đủ: Mã đơn, Khách hàng, Ngày, Tổng tiền, Trạng thái.
- Cột Status dùng **Badge màu**: Vàng (Pending), Xanh dương (Verified), Cam (Shipping), Xanh lá (Delivered), Đỏ (Cancelled).
- Logic **Update Status** tuần tự: Pending → Verified → Shipping → Delivered.
- Bộ lọc: Dropdown trạng thái, Range datepicker, Ô tìm kiếm.
- Nút **Export Excel** phía trên bảng.
- Click vào đơn mở **modal chi tiết**: Sản phẩm, Địa chỉ, Timeline trạng thái.

### Màn 23 — Quản Lý Sản Phẩm & Kho (Products & Inventory)

- **Bảng CRUD** phân trang: Ảnh, Tên, Danh mục, Giá, Tồn kho, Trạng thái.
- Ô tìm kiếm + Filter dropdown (Danh mục, Trạng thái).
- Nút **Soft delete** / **Bulk delete** (checkbox chọn nhiều).
- Cảnh báo **badge đỏ** nếu tồn kho < ngưỡng (cạn kho).
- **Trang Thêm/Sửa SP:** Form nhiều section:
  - Thông tin cơ bản (Tên, Mô tả, Giá).
  - **Cấu hình Variants** (RAM, ROM, Màu sắc) — thêm/xóa dòng linh hoạt.
  - **Quản lý Kho** — nhập số lượng, cảnh báo cạn kho.
  - **Upload ảnh** — kéo thả (drag & drop), xem trước thumbnail.

### Màn 24 — Quản Lý Danh Mục (Categories)

- Giao diện **Tree UI** — danh sách thu gọn/kéo giãn (folder-like) phân cấp cha-con.
- Mỗi node: Tên danh mục, Icon, Số SP thuộc danh mục.
- Nút: **Thêm con**, **Sửa**, **Xóa** trên mỗi node.
- Nút **"+ Thêm danh mục gốc"** phía trên.
- Modal thêm/sửa: Tên, Mô tả, Icon, Chọn danh mục cha.

### Màn 25 — Quản Lý Người Dùng & Phân Quyền (Users & Roles)

- **Bảng User:** Avatar, Tên, Email, Vai trò, Ngày đăng ký, Trạng thái.
- Nút **Lock/Unlock** tài khoản — toggle với icon ổ khoá.
- Badge vai trò: Admin, Sub-admin, Khách hàng.
- **Trang Phân Quyền (Roles):**
  - List vai trò, mỗi vai trò có danh sách quyền (checkbox matrix).
  - VD: Nhân viên Sale chỉ thấy Đơn hàng, không thấy Hệ thống.
  - Nút "Thêm vai trò mới" + form chọn quyền.

---

## 📌 PROMPT 6 — TỔNG QUẢN TRỊ ADMIN – PHẦN 2 (5 Màn Hình)

**Yêu cầu chung:** Áp dụng đầy đủ Quy chuẩn Thiết kế: Giao diện tiếng Việt, **Light Theme & Glassmorphism** (`slate-50`/trắng, `backdrop-blur`), font chữ **không chân hiện đại** (Inter/Roboto), **bo góc cực đại (rounded-2xl/3xl)**, hệ màu **gradient rực rỡ** (Primary: purple→blue, Accent: đỏ/hồng, cam/vàng, xanh/cyan). Bố cục lưới (Grid) thoáng đãng, hiệu ứng hover `scale-105` + shadow màu tương ứng (sử dụng framer-motion), và icon mảnh hiện đại (Feather/react-icons). Tên website: **Hozitech**. Mọi thanh banner lớn phải tự động chạy (auto-slide).

**Layout Admin:** Giữ nguyên layout Admin — Sidebar tree-view trái + Header trên + Nội dung phải.

### Màn 26 — Quản Lý Khuyến Mãi (Promotions / Vouchers)

- **Bảng danh sách** voucher: Mã, Loại giảm, Giá trị, Lượt đã dùng/Giới hạn, Hạn chót, Trạng thái.
- **Form tạo/sửa Voucher:**
  - Loại giảm: Radio **Giảm theo %** hoặc **Tiền cố định**.
  - Giá trị giảm, Đơn hàng tối thiểu.
  - Giới hạn lượt sử dụng.
  - Range datepicker Hạn chót.
  - **Tag User cụ thể** — multiselect chọn nhóm khách hàng áp dụng.
- Badge trạng thái: Đang hoạt động (xanh), Hết hạn (xám), Tạm dừng (vàng).

### Màn 27 — Quản Lý Đánh Giá (Reviews Management)

- **Bảng review:** SP, Khách hàng, Số sao, Nội dung (truncate), Ngày, Trạng thái.
- Bộ lọc: Dropdown số sao, Trạng thái (Hiện / Ẩn / Spam).
- Nút thao tác mỗi dòng: **Ẩn** (toggle), **Xóa**, **Đánh dấu Spam**.
- Click mở **modal xem chi tiết** review: Nội dung đầy đủ + Hình ảnh đính kèm.

### Màn 28 — Quản Lý Nội Dung & Blog (CMS / Banner)

- **Tab 1: Quản lý Banner trang chủ**
  - Danh sách banner (1, 2, 3…): Ảnh preview, Tiêu đề, Link đích, Thứ tự.
  - Upload ảnh kéo thả, Toggle ẩn/hiện.
- **Tab 2: Quản lý Bài viết**
  - Bảng bài viết: Tiêu đề, Danh mục, Ngày đăng, Trạng thái (Nháp / Đã xuất bản).
  - Nút: Sửa, Xóa, Xuất bản / Gỡ xuất bản.
  - Form thêm/sửa bài: Tên, Danh mục, Textarea nội dung (rich text giả lập), Upload ảnh bìa.

### Màn 29 — Hỗ Trợ (Tickets Center)

- **Bảng ticket** từ khách hàng: Mã, Khách hàng, Chủ đề, Trạng thái, Ngày tạo.
- Badge trạng thái: Mới (đỏ), Đang xử lý (cam), Đã trả lời (xanh), Đóng (xám).
- Click vào ticket mở **giao diện chat/reply thread:**
  - Tin nhắn của khách bên trái, Admin reply bên phải.
  - Textarea + nút Gửi + Upload đính kèm.
  - Nút **"Đóng ticket"** phía trên.

### Màn 30 — Hệ Thống & AI Settings (System & ML Ops)

- **Section Cấu hình hệ thống:**
  - Form chỉnh sửa: Thuế mặc định (%), Phí vận chuyển (VNĐ), Đơn vị tiền tệ.
  - Nút **Lưu cấu hình**.
- **Section AI Dashboard:**
  - Toggle **Bật/Tắt Recommendation Engine** (hệ thống gợi ý AI).
  - Toggle **Bật/Tắt AI Content** (tự động tạo mô tả SP).
  - Nút **"Cấu hình thuật toán gợi ý"** mở modal:
    - Dropdown chọn thuật toán (Collaborative Filtering, Content-based…).
    - Slider: Độ ưu tiên SP mới, Độ đa dạng gợi ý.
  - Bảng thống kê AI: Số lần gợi ý, Tỷ lệ click, Tỷ lệ chuyển đổi.

---

## 📌 PROMPT 7 — XÁC THỰC & TIỆN ÍCH (8 Màn Hình)

**Yêu cầu chung:** Áp dụng đầy đủ Quy chuẩn Thiết kế: Giao diện tiếng Việt, **Light Theme & Glassmorphism** (`slate-50`/trắng, `backdrop-blur`), font chữ **không chân hiện đại** (Inter/Roboto), **bo góc cực đại (rounded-2xl/3xl)**, hệ màu **gradient rực rỡ** (Primary: purple→blue, Accent: đỏ/hồng, cam/vàng, xanh/cyan). Bố cục lưới (Grid) thoáng đãng, hiệu ứng hover `scale-105` + shadow màu tương ứng (sử dụng framer-motion), và icon mảnh hiện đại (Feather/react-icons). Tên website: **Hozitech**. Mọi thanh banner lớn phải tự động chạy (auto-slide).

### Màn 31 — Đăng Nhập (Login)

- Khung nhập Email/SĐT và Mật khẩu. Nút icon con mắt ẩn/hiện mật khẩu.
- Nút "Đăng nhập" gradient nổi bật.
- Cụm **Đăng nhập Mạng xã hội**: Nút "Đăng nhập với Google" và "Đăng nhập với Facebook" có icon chuẩn.
- Link "Quên mật khẩu?" và "Đăng ký tài khoản mới".

### Màn 32 — Đăng Ký (Register)

- Form: Họ Tên, Email, SĐT, Mật khẩu, Xác nhận Mật khẩu.
- Hiển thị thanh đo độ mạnh mật khẩu (Yếu/Trung bình/Mạnh).
- Checkbox "Tôi đồng ý với Điều khoản và Chính sách bảo mật".
- Nút "Đăng ký" và link "Đã có tài khoản? Đăng nhập ngay".

### Màn 33 — Quên Mật Khẩu & Mật Khẩu Mới (Forgot Password / Reset)

- **Bước 1:** Form nhập Email/SĐT để nhận mã OTP. Nút "Gửi mã xác nhận".
- **Bước 2 (Xác thực):** Màn hình nhập 6 ô vuông OTP, countdown gửi lại mã (VD: 60s).
- **Bước 3 (Đổi MK):** Form nhập Mật khẩu mới và Xác nhận. Nút "Cập nhật mật khẩu".

### Màn 34 — Giới Thiệu (About Us)

- Banner Hero hình ảnh đội ngũ / kho hàng Hozitech.
- Section: Tầm nhìn & Sứ mệnh (Icon đồ họa).
- Section: Lịch sử hình thành (Timeline ngang).
- Section: Cam kết chất lượng (Bảo hành 24 tháng, Lỗi 1 đổi 1).

### Màn 35 — Điều Khoản & Chính Sách (Terms & Privacy)

- Sidebar trái dạng Menu (Điều khoản sử dụng, Chính sách bảo hành, Chính sách đổi trả, Chính sách bảo mật). Chọn mục nào màn hình phải hiển thị nội dung đó.
- Khung nội dung chính: Text dài được format heading rõ ràng, padding rộng, dễ đọc. Nút cuộn lên đầu trang.

### Màn 36 — Câu Hỏi Thường Gặp (FAQ)

- Thanh tìm kiếm câu hỏi.
- Danh sách câu hỏi dạng **Accordion** (Mở ra/Đóng vào) chia theo nhóm: Đơn hàng, Vận chuyển, Thanh toán, Bảo hành.
- Có nút "Vẫn cần hỗ trợ? Liên hệ ngay" dẫn về trang Liên hệ/Ticket.

### Màn 37 — Hệ Thống Bảo Trì (Maintenance)

- Hình ảnh Illustration vector (Robot đang sửa chữa hoặc bánh răng xoay).
- Text: "Hệ thống đang được nâng cấp". Thời gian dự kiến hoàn thành.

### Màn 38 — Ngoại Lệ (404 Not Found & 403 Forbidden)

- **Màn 404 (Không tìm thấy trang):** Ảnh minh hoạ phi hành gia lạc vũ trụ. Text: "Trang bạn tìm kiếm không tồn tại hoặc đã bị xóa". Nút "Quay lại trang chủ".
- **Màn 403 (Không có quyền truy cập):** Ảnh minh hoạ cảnh sát chặn cửa. Text: "Bạn không có quyền truy cập khu vực này". Nút "Quay lại trang chủ".

---

## 📋 TỔNG KẾT — DANH SÁCH 38 MÀN HÌNH

| Prompt | Nhóm                | Màn hình                                                                                                      |
| ------ | ------------------- | ------------------------------------------------------------------------------------------------------------- |
| **1**  | Khám phá & Tìm kiếm | 1. Home · 2. Search/Category · 3. Compare · 4. Wishlist · 5. Blog                                             |
| **2**  | Chi tiết & Mua sắm  | 6. Product Detail · 7. Review & Rating · 8. Flash Sale · 9. Cart · 10. Checkout                               |
| **3**  | Tài khoản (P1)      | 11. Profile · 12. Address Book · 13. Payment Methods · 14. Orders · 15. Tracking                              |
| **4**  | Tài khoản (P2)      | 16. Vouchers · 17. My Reviews · 18. Recently Viewed · 19. Notifications · 20. Support & AI Chatbot            |
| **5**  | Admin (P1)          | 21. Dashboard · 22. Admin Orders · 23. Products & Inventory · 24. Categories · 25. Users & Roles              |
| **6**  | Admin (P2)          | 26. Promotions · 27. Reviews Mgmt · 28. CMS/Banner · 29. Tickets · 30. System & AI                            |
| **7**  | Xác thực & Tiện Ích | 31-33. Login/Register/OTP · 34. About Us · 35. Terms/Privacy · 36. FAQ · 37. Maintenance · 38. 404/403 Errors |
