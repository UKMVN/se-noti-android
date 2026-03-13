# SE Noti Android

Ứng dụng Android đọc và lưu trữ thông báo điện thoại vào database nội bộ.

## Tính năng

- **Lắng nghe thông báo**: Tự động bắt tất cả thông báo từ các ứng dụng trên điện thoại
- **Lưu trữ nội bộ**: Sử dụng Room Database (SQLite) để lưu thông báo vào bộ nhớ trong
- **Hiển thị danh sách**: Xem tất cả thông báo đã lưu với giao diện hiện đại
- **Tìm kiếm**: Tìm kiếm thông báo theo tiêu đề hoặc nội dung
- **Lọc theo ứng dụng**: Lọc thông báo theo từng ứng dụng
- **Chi tiết thông báo**: Xem chi tiết từng thông báo
- **Quản lý**: Đánh dấu đã đọc, xóa từng thông báo hoặc xóa tất cả
- **Vuốt để xóa**: Vuốt sang trái để xóa nhanh thông báo
- **Dark mode**: Hỗ trợ chế độ tối và Dynamic Color (Android 12+)

## Công nghệ sử dụng

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Database | Room (SQLite) |
| Architecture | MVVM |
| Navigation | Navigation Compose |
| Async | Coroutines + Flow |
| Build | Gradle Kotlin DSL + Version Catalog |

## Yêu cầu

- Android Studio Hedgehog (2023.1.1) trở lên
- Android SDK 35
- Min SDK 26 (Android 8.0)
- JDK 17

## Cài đặt & Chạy

1. Clone repository:
   ```bash
   git clone <repository-url>
   ```

2. Mở project bằng Android Studio

3. Sync Gradle và chạy ứng dụng trên thiết bị/emulator

4. **Quan trọng**: Khi mở ứng dụng lần đầu, bạn cần cấp quyền **Notification Access**:
   - Nhấn nút "Cấp quyền truy cập" trên màn hình chính
   - Tìm ứng dụng "SE Noti" trong danh sách
   - Bật quyền truy cập thông báo
   - Quay lại ứng dụng

## Cấu trúc project

```
app/src/main/java/com/senoti/app/
├── SeNotiApplication.kt          # Application class
├── MainActivity.kt               # Activity chính
├── data/
│   ├── NotificationEntity.kt     # Room Entity
│   ├── NotificationDao.kt        # Room DAO
│   ├── AppDatabase.kt            # Room Database
│   ├── AppInfo.kt                # Data class cho thông tin app
│   └── NotificationRepository.kt # Repository pattern
├── service/
│   └── NotificationListenerService.kt  # Service lắng nghe thông báo
├── ui/
│   ├── navigation/
│   │   └── NavGraph.kt           # Navigation graph
│   ├── screens/
│   │   ├── NotificationListScreen.kt   # Màn hình danh sách
│   │   ├── NotificationDetailScreen.kt # Màn hình chi tiết
│   │   └── PermissionScreen.kt         # Màn hình yêu cầu quyền
│   └── theme/
│       ├── Color.kt              # Bảng màu
│       ├── Theme.kt              # Theme config
│       └── Type.kt               # Typography
└── viewmodel/
    └── NotificationViewModel.kt  # ViewModel
```

## Cách hoạt động

1. `NotificationListenerService` được đăng ký với hệ thống Android để nhận thông báo
2. Khi có thông báo mới, service sẽ trích xuất thông tin (app name, title, text, timestamp)
3. Thông tin được lưu vào Room Database (SQLite nội bộ)
4. UI sử dụng Flow để tự động cập nhật khi có thông báo mới
5. Người dùng có thể xem, tìm kiếm, lọc và quản lý thông báo

## License

MIT License
