# Báo Cáo Chi Tiết Các Service Trong Hệ Thống ShopShoes

## Mục Lục
1. [Tổng Quan Hệ Thống](#tổng-quan-hệ-thống)
2. [Authentication Service](#authentication-service)
3. [Product Service](#product-service)
4. [Cart Service](#cart-service)
5. [Payment Service](#payment-service)
6. [Invoice Service](#invoice-service)
7. [Email Sender Service](#email-sender-service)
8. [API Gateway](#api-gateway)
9. [Eureka Server](#eureka-server)

## Tổng Quan Hệ Thống

Hệ thống ShopShoes được xây dựng theo kiến trúc microservices, bao gồm 8 service chính. Mỗi service được thiết kế để thực hiện một chức năng cụ thể và có thể hoạt động độc lập.

### Công Nghệ Sử Dụng
- Backend: Java Spring Boot
- Frontend: React.js
- Database: MySQL
- Service Discovery: Eureka
- API Gateway: Spring Cloud Gateway
- Message Queue: RabbitMQ/Kafka
- Container: Docker
- CI/CD: Jenkins

### RabbitMQ trong Hệ Thống
RabbitMQ là một message broker giúp các service giao tiếp với nhau. Ví dụ trong ShopShoes:

1. **Luồng xử lý đơn hàng**:
   - Cart Service tạo đơn hàng
   - RabbitMQ chuyển thông tin đến:
     + Invoice Service (tạo hóa đơn)
     + Payment Service (xử lý thanh toán)
     + Email Service (gửi email xác nhận)

2. **Lợi ích**:
   - Các service hoạt động độc lập
   - Không bị mất dữ liệu khi service lỗi
   - Dễ dàng mở rộng hệ thống
   - Xử lý được nhiều đơn hàng cùng lúc

### JSON Web Token (JWT)
JWT là chuẩn mở để truyền thông tin an toàn giữa các service. Trong ShopShoes:

1. **Cấu trúc JWT**:
   - Header: Loại token và thuật toán mã hóa
   - Payload: Thông tin người dùng (id, role, quyền)
   - Signature: Chữ ký xác thực

2. **Luồng xác thực**:
   - User đăng nhập -> Authentication Service
   - Service tạo JWT token
   - User gửi token trong mỗi request
   - Các service khác xác thực token

3. **Lợi ích**:
   - Không cần lưu trữ session
   - Dễ dàng chia sẻ giữa các service
   - Bảo mật thông tin người dùng
   - Hỗ trợ phân quyền (RBAC)

### Cơ Chế Retry
Cơ chế tự động thử lại khi gọi API giữa các service thất bại:

1. **Cấu hình Retry**:
   - Số lần thử lại: 3 lần
   - Thời gian chờ: 3-5 giây
   - Chỉ áp dụng cho lỗi tạm thời (timeout, network error)

2. **Ví dụ trong ShopShoes**:
   - Cart Service gọi Payment Service
   - Nếu thất bại -> đợi 3s -> thử lại
   - Nếu vẫn thất bại -> đợi 5s -> thử lần cuối
   - Nếu vẫn không thành công -> báo lỗi

3. **Lợi ích**:
   - Tăng độ tin cậy của hệ thống
   - Tự động xử lý lỗi tạm thời
   - Giảm thiểu lỗi do mạng
   - Cải thiện trải nghiệm người dùng

### Rate Limiter
Cơ chế giới hạn số lượng request từ client đến service:

1. **Cấu hình Rate Limiter**:
   - Số request tối đa: 5 request/phút/client
   - Thời gian chờ: 0.5 giây
   - Áp dụng cho tất cả API endpoints
   - Hiển thị thông báo sau 5 lần gọi

2. **Ví dụ trong ShopShoes**:
   - Client gọi API liên tục
   - Rate Limiter đếm số request trong 1 phút
   - Nếu vượt quá 5 request:
     + Trả về lỗi 429 (Too Many Requests)
     + Hiển thị thông báo "Vui lòng đợi 0.5 giây"
     + Ghi log vi phạm

3. **Lợi ích**:
   - Bảo vệ service khỏi quá tải
   - Ngăn chặn tấn công DDoS
   - Đảm bảo công bằng cho người dùng
   - Tối ưu tài nguyên hệ thống

### Docker Container
Tất cả các service được đóng gói và chạy trong Docker container:

1. **Cấu trúc Container**:
   - Mỗi service một container riêng biệt
   - Các container giao tiếp qua network
   - Sử dụng Docker Compose để quản lý
   - Các service chính:
     + Authentication Service
     + Product Service
     + Cart Service
     + Payment Service
     + Invoice Service
     + Email Service
     + API Gateway
     + Eureka Server

2. **Docker Compose**:
   - File `docker-compose.yml` quản lý toàn bộ hệ thống
   - Cấu hình các service:
     ```yaml
     services:
       eureka-server:
         build: ./eureka-server
         ports: ["8761:8761"]
       
       api-gateway:
         build: ./api-gateway
         ports: ["8080:8080"]
         depends_on: ["eureka-server"]
       
       auth-service:
         build: ./authentication-service
         ports: ["8081:8081"]
         depends_on: ["eureka-server"]
       
       # Các service khác tương tự
     ```
   - Quản lý network và volume
   - Environment variables cho từng service
   - Health check và restart policy

3. **Cấu hình Docker**:
   - Port mapping riêng cho từng service
   - Volume cho dữ liệu persistent
   - Environment variables cho cấu hình
   - Health check cho monitoring

4. **Lợi ích**:
   - Dễ dàng triển khai
   - Môi trường đồng nhất
   - Scale linh hoạt
   - Quản lý tài nguyên hiệu quả

### Jenkins CI/CD
Jenkins được sử dụng để tự động hóa quá trình build và deploy:

1. **Hiểu về Jenkins**:
   - Jenkins là công cụ CI/CD mã nguồn mở
   - Tự động hóa quá trình build, test, deploy
   - Tích hợp với Git, Docker, Maven
   - Quản lý pipeline cho từng service

2. **Cài đặt Jenkins**:
   - Cài đặt Java JDK
   - Tải và cài đặt Jenkins
   - Cấu hình port và security
   - Cài đặt các plugin cần thiết:
     + Docker plugin
     + Git plugin
     + Maven plugin

3. **Chạy Jenkins**:
   - Khởi động Jenkins server
   - Tạo pipeline cho từng service
   - Cấu hình webhook với Git
   - Tự động build và deploy khi có code mới

### GitLab CI/CD
GitLab CI/CD được sử dụng để tự động hóa quá trình phát triển:

1. **Hiểu về CI/CD**:
   - CI (Continuous Integration):
     + Tự động build code
     + Chạy unit test
     + Kiểm tra code quality
     + Phát hiện lỗi sớm
   
   - CD (Continuous Deployment):
     + Tự động deploy
     + Kiểm tra môi trường
     + Rollback nếu cần
     + Theo dõi trạng thái

2. **Chạy CI/CD với GitLab**:
   - File `.gitlab-ci.yml` định nghĩa pipeline
   - Các stage chính:
     ```yaml
     stages:
       - build
       - test
       - deploy
     
     build:
       stage: build
       script:
         - mvn clean package
     
     test:
       stage: test
       script:
         - mvn test
     
     deploy:
       stage: deploy
       script:
         - docker-compose up -d
     ```
   - Tự động chạy khi push code
   - Báo cáo kết quả build

### Agile-Scrum Methodology
Phương pháp phát triển phần mềm linh hoạt:

1. **Các Vai Trò**:
   - Product Owner:
     + Quản lý Product Backlog
     + Ưu tiên các tính năng
     + Đại diện cho khách hàng
   
   - Scrum Master:
     + Hỗ trợ team
     + Loại bỏ rào cản
     + Đảm bảo quy trình Scrum
   
   - Development Team:
     + Phát triển sản phẩm
     + Tự quản lý công việc
     + Đa kỹ năng

2. **Các Sự Kiện**:
   - Sprint Planning:
     + Lập kế hoạch 2 tuần
     + Chọn user stories
     + Ước tính thời gian
   
   - Daily Scrum:
     + Họp 15 phút mỗi ngày
     + Cập nhật tiến độ
     + Giải quyết vấn đề
   
   - Sprint Review:
     + Demo sản phẩm
     + Nhận phản hồi
     + Cập nhật backlog
   
   - Sprint Retrospective:
     + Đánh giá quy trình
     + Cải thiện phương pháp
     + Lập kế hoạch cải tiến

3. **Công Cụ**:
   - Jira: Quản lý công việc
   - Confluence: Tài liệu
   - GitLab: Quản lý code
   - Slack: Giao tiếp team

## Authentication Service

### Mục Đích
Service xác thực và phân quyền người dùng trong hệ thống.

### Chức Năng Chính
1. **Quản Lý Người Dùng**
   - Đăng ký tài khoản
   - Đăng nhập/Đăng xuất
   - Quản lý thông tin cá nhân
   - Đổi mật khẩu
   - Quản lý người dùng (Admin)

2. **Xác Thực & Phân Quyền**
   - JWT token generation và validation
   - Role-based access control (RBAC)
   - Session management
   - OAuth2 integration

3. **Bảo Mật**
   - Mã hóa mật khẩu
   - Rate limiting
   - IP blocking
   - Two-factor authentication

### API Endpoints
1. **Authentication APIs** (`/api/auth`)
   - POST `/signin` - Đăng nhập
   - POST `/signup` - Đăng ký
   - POST `/logout` - Đăng xuất

2. **User Management APIs** (`/api/users`)
   - GET `/me` - Lấy thông tin người dùng hiện tại
   - GET `/all` - Lấy danh sách người dùng (Admin)
   - GET `/{id}` - Lấy thông tin người dùng theo ID
   - PUT `/{id}` - Cập nhật thông tin người dùng
   - PUT `/{id}/change-password` - Đổi mật khẩu
   - DELETE `/{id}` - Xóa người dùng (Admin)

## Product Service

### Mục Đích
Quản lý thông tin sản phẩm và danh mục trong hệ thống.

### Chức Năng Chính
1. **Quản Lý Sản Phẩm**
   - CRUD operations cho sản phẩm
   - Quản lý danh mục
   - Quản lý kho hàng
   - Upload hình ảnh

2. **Tìm Kiếm & Lọc**
   - Full-text search
   - Filter theo danh mục
   - Filter theo giá
   - Filter theo kích thước
   - Filter theo màu sắc

3. **Quản Lý Kho**
   - Cập nhật số lượng
   - Cảnh báo hết hàng
   - Quản lý nhập/xuất

### API Endpoints
- GET /api/products
- GET /api/products/{id}
- POST /api/products
- PUT /api/products/{id}
- DELETE /api/products/{id}
- GET /api/categories
- GET /api/products/search

## Cart Service

### Mục Đích
Quản lý giỏ hàng và các hoạt động liên quan đến giỏ hàng.

### Chức Năng Chính
1. **Quản Lý Giỏ Hàng**
   - Thêm sản phẩm vào giỏ hàng
   - Xóa sản phẩm khỏi giỏ hàng
   - Cập nhật số lượng sản phẩm
   - Xem chi tiết giỏ hàng
   - Xóa toàn bộ giỏ hàng

2. **Thanh Toán & Hóa Đơn**
   - Thanh toán giỏ hàng
   - Xem chi tiết hóa đơn
   - Khởi tạo thanh toán
   - Kiểm tra trạng thái thanh toán

### API Endpoints
1. **Cart Management** (`/api/carts`)
   - GET `/user/{userId}` - Lấy giỏ hàng của người dùng
   - POST `/user/{userId}/items` - Thêm sản phẩm vào giỏ hàng
   - GET `/{cartId}/items` - Lấy chi tiết giỏ hàng
   - DELETE `/{cartId}/items/{productId}` - Xóa sản phẩm khỏi giỏ hàng
   - PATCH `/{cartId}/items/{productId}` - Cập nhật số lượng sản phẩm
   - DELETE `/{cartId}/clear` - Xóa toàn bộ giỏ hàng

2. **Checkout & Payment** (`/api/carts`)
   - POST `/{cartId}/checkout` - Thanh toán giỏ hàng
   - GET `/invoice/{invoiceId}` - Xem chi tiết hóa đơn
   - POST `/invoice/{invoiceId}/payment` - Khởi tạo thanh toán
   - GET `/payment/{invoiceId}/status` - Kiểm tra trạng thái thanh toán

## Payment Service

### Mục Đích
Xử lý các giao dịch thanh toán trong hệ thống.

### Chức Năng Chính
1. **Xử Lý Thanh Toán**
   - Tích hợp VNPay
   - Tích hợp Momo
   - Thanh toán bằng thẻ
   - Thanh toán COD

2. **Quản Lý Giao Dịch**
   - Theo dõi trạng thái
   - Hoàn tiền
   - Lịch sử giao dịch
   - Báo cáo doanh thu

3. **Bảo Mật**
   - Mã hóa thông tin thẻ
   - Xác thực giao dịch
   - Phát hiện gian lận

### API Endpoints
- POST /api/payments/create
- GET /api/payments/{id}
- POST /api/payments/refund
- GET /api/payments/history

## Invoice Service

### Mục Đích
Quản lý hóa đơn và đơn hàng trong hệ thống.

### Chức Năng Chính
1. **Quản Lý Hóa Đơn**
   - Tạo hóa đơn
   - Cập nhật trạng thái
   - Xuất PDF
   - Gửi email

2. **Quản Lý Đơn Hàng**
   - Theo dõi đơn hàng
   - Cập nhật trạng thái
   - Hủy đơn hàng
   - Hoàn trả

3. **Báo Cáo**
   - Thống kê doanh số
   - Báo cáo theo thời gian
   - Báo cáo theo sản phẩm
   - Báo cáo theo khách hàng

### API Endpoints
- POST /api/invoices
- GET /api/invoices/{id}
- PUT /api/invoices/{id}
- GET /api/invoices/reports

## Email Sender Service

### Mục Đích
Xử lý việc gửi email trong hệ thống.

### Chức Năng Chính
1. **Gửi Email**
   - Xác nhận đơn hàng
   - Quên mật khẩu
   - Thông báo
   - Marketing

2. **Quản Lý Template**
   - Tạo template
   - Chỉnh sửa template
   - Đa ngôn ngữ
   - Personalization

3. **Theo Dõi**
   - Trạng thái gửi
   - Tỷ lệ mở
   - Tỷ lệ click
   - Báo cáo

### API Endpoints
- POST /api/emails/send
- GET /api/emails/templates
- POST /api/emails/templates
- GET /api/emails/reports

## API Gateway

### Mục Đích
Điểm vào duy nhất của hệ thống, quản lý và định tuyến các request.

### Chức Năng Chính
1. **Định Tuyến**
   - Route requests
   - Load balancing
   - Service discovery
   - Circuit breaking

2. **Bảo Mật**
   - Authentication
   - Authorization
   - Rate limiting
   - CORS

3. **Monitoring**
   - Logging
   - Metrics
   - Tracing
   - Health checks

### API Endpoints
- Tất cả các endpoints của các service khác

## Eureka Server

### Mục Đích
Quản lý việc đăng ký và phát hiện các service trong hệ thống.

### Chức Năng Chính
1. **Service Discovery**
   - Đăng ký service
   - Phát hiện service
   - Health checking
   - Load balancing

2. **Quản Lý**
   - Service status
   - Instance management
   - Configuration
   - Monitoring

3. **High Availability**
   - Failover
   - Replication
   - Backup
   - Recovery

### Cấu Hình
- Port: 8761
- Dashboard: /eureka
- Health check: /actuator/health

## Kết Luận

Hệ thống ShopShoes được thiết kế theo kiến trúc microservices, cho phép:
- Scalability: Mỗi service có thể scale độc lập
- Maintainability: Dễ dàng bảo trì và cập nhật
- Reliability: Tăng tính sẵn sàng của hệ thống
- Flexibility: Có thể sử dụng công nghệ khác nhau cho từng service
- Security: Tăng cường bảo mật với nhiều lớp

## Đề Xuất Cải Thiện

1. **Performance**
   - Implement caching
   - Optimize database queries
   - Use CDN for static content

2. **Security**
   - Implement OAuth2
   - Add rate limiting
   - Enhance encryption

3. **Monitoring**
   - Add centralized logging
   - Implement tracing
   - Set up alerts

4. **Scalability**
   - Implement auto-scaling
   - Use message queues
   - Add caching layer 