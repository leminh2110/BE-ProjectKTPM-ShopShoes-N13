# BE-ProjectKTPM-ShopShoes_N13

## Giới thiệu
BE-ProjectKTPM-ShopShoes_N13 là dự án Backend cho website bán giày dép, được xây dựng theo mô hình kiến trúc Microservices hiện đại. Dự án nhằm cung cấp các API và dịch vụ cần thiết để hỗ trợ hoạt động của hệ thống bán hàng trực tuyến, bao gồm quản lý sản phẩm, đơn hàng, người dùng, thanh toán và nhiều chức năng khác.

## Liên kết liên quan
- [Backend Project ShopShoes(BE-ProjectKTPM-ShopShoes_N13)](https://github.com/hoanghuytoi/BE-ProjectKTPM-ShopShoes_N13.git)
- [Frontend Project ShopShoes(FE-ProjectKTPM-ShopShoes_N13)](https://github.com/hoanghuytoi/FE-ProjectKTPM-ShopShoes.git)

## Kiến trúc & Công nghệ
- **Kiến trúc Microservices:** Các chức năng chính được tách thành các service độc lập, dễ dàng mở rộng và bảo trì.
- **Cơ sở dữ liệu:** Sử dụng MySQL để lưu trữ dữ liệu một cách an toàn và hiệu quả.
- **Giao tiếp giữa các service:** Sử dụng RESTful API hoặc gRPC (nếu có).
- **Bảo mật:** Xác thực và phân quyền người dùng với JWT.
- **Quản lý cấu hình và môi trường:** Sử dụng Docker, Docker Compose (nếu có).
- **Các công nghệ khác:** Node.js, Express.js (hoặc Spring Boot, .NET Core, tùy stack bạn dùng).

## Tính năng chính
- Quản lý sản phẩm (giày, dép, v.v.)
- Quản lý người dùng và xác thực
- Quản lý đơn hàng và thanh toán
- Tìm kiếm và lọc sản phẩm
- Thống kê, báo cáo doanh thu
- Hỗ trợ tích hợp với các dịch vụ bên ngoài (nếu có)

## Các service chính
- **User Service:** Quản lý thông tin người dùng, xác thực, phân quyền, đăng ký và đăng nhập.
- **Product Service:** Quản lý sản phẩm (giày, dép), danh mục, tồn kho, cập nhật thông tin sản phẩm.
- **Order Service:** Xử lý đơn hàng, quản lý trạng thái đơn hàng, lịch sử mua hàng.
- **Payment Service:** Xử lý thanh toán, tích hợp với các cổng thanh toán (nếu có).
- **Notification Service:** Gửi thông báo đến người dùng (email, SMS, v.v. nếu có).
- **API Gateway:** Làm đầu mối tiếp nhận request từ client, định tuyến đến các service phù hợp, xử lý xác thực chung.
- **Other Services:** Các service bổ sung như quản lý đánh giá sản phẩm, báo cáo/thống kê, v.v. (nếu có).

## Hướng dẫn cài đặt & chạy thử
1. Clone repository về máy
2. Cài đặt dependencies cho từng service: `npm install` hoặc lệnh tương ứng
3. Cấu hình các biến môi trường trong file `.env`
4. Khởi động các service (có thể dùng Docker Compose): `docker-compose up` hoặc chạy từng service riêng lẻ
5. Truy cập các API endpoint theo tài liệu hướng dẫn

## Demo
1. Trang chủ
![Trang chủ](https://github.com/hoanghuytoi/BE-ProjectKTPM-ShopShoes_N13/blob/main/demo/1.png)
2. Đăng ký
![Đăng ký](https://github.com/hoanghuytoi/BE-ProjectKTPM-ShopShoes_N13/blob/main/demo/2.png)
3. Đăng nhập
![Đăng nhập](https://github.com/hoanghuytoi/BE-ProjectKTPM-ShopShoes_N13/blob/main/demo/3.png)
4. Thông tin người dùng
![Thông tin người dùng](https://github.com/hoanghuytoi/BE-ProjectKTPM-ShopShoes_N13/blob/main/demo/4.png)
5. Lịch sử đơn hàng
![Lịch sử đơn hàng](https://github.com/hoanghuytoi/BE-ProjectKTPM-ShopShoes_N13/blob/main/demo/5.png)
6. Sản phẩm
![Sản phẩm](https://github.com/hoanghuytoi/BE-ProjectKTPM-ShopShoes_N13/blob/main/demo/6.png)
7. Cửa hàng
![Cửa hàng](https://github.com/hoanghuytoi/BE-ProjectKTPM-ShopShoes_N13/blob/main/demo/7.png)
8. Chi tiết sản phẩm
![Chi tiết sản phẩm](https://github.com/hoanghuytoi/BE-ProjectKTPM-ShopShoes_N13/blob/main/demo/8.png)
9. Giỏ hàng & thanh toán
![Giỏ hàng & thanh toán](https://github.com/hoanghuytoi/BE-ProjectKTPM-ShopShoes_N13/blob/main/demo/9.png)
10. Quản trị viên
![Quản trị viên](https://github.com/hoanghuytoi/BE-ProjectKTPM-ShopShoes_N13/blob/main/demo/10.png)