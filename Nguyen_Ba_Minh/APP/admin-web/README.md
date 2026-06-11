# Coffee Shop - Admin Dashboard

Trang web quản trị cho ứng dụng Coffee Shop, được xây dựng bằng ReactJS và Firebase.

## Tính năng

### 🔐 Xác thực
- Đăng nhập với email và mật khẩu
- Kiểm tra quyền Admin
- Bảo vệ routes

### 📊 Dashboard
- Tổng quan thống kê nhanh
- Số lượng nhân viên, bàn, hàng hóa
- Doanh thu hôm nay

### 👥 Quản lý Nhân viên
- Xem danh sách nhân viên
- Thêm nhân viên mới
- Cập nhật thông tin nhân viên
- Xóa nhân viên
- Upload ảnh đại diện

### 💰 Quản lý Doanh thu
- Doanh thu theo ngày, tháng, năm
- Biểu đồ doanh thu theo tháng
- Top sản phẩm bán chạy

### 🪑 Quản lý Bàn
- Xem trạng thái các bàn (còn trống/có khách)
- Thêm bàn mới
- Xóa bàn

### 📦 Quản lý Loại hàng
- CRUD loại hàng
- Upload ảnh loại hàng

### 🛍️ Quản lý Hàng hóa
- CRUD hàng hóa
- Upload ảnh sản phẩm
- Quản lý giá tiền và trạng thái

### 📄 Quản lý Hóa đơn
- Xem danh sách hóa đơn
- Chi tiết hóa đơn
- Trạng thái thanh toán

### ✅ Duyệt Hóa đơn
- Xem hóa đơn mang về chờ duyệt
- Duyệt/từ chối hóa đơn

## Công nghệ sử dụng

- **React 18** - UI Framework
- **Vite** - Build tool
- **React Router** - Routing
- **Firebase** - Backend (Firestore, Auth, Storage)
- **Tailwind CSS** - Styling
- **Recharts** - Biểu đồ
- **Lucide React** - Icons
- **React Hot Toast** - Notifications
- **date-fns** - Xử lý ngày tháng

## Cài đặt

### 1. Clone repository hoặc tạo folder mới

```bash
cd admin-web
```

### 2. Cài đặt dependencies

```bash
npm install
```

### 3. Cấu hình Firebase

Mở file `src/config/firebase.js` và thay thế bằng thông tin Firebase của bạn:

```javascript
const firebaseConfig = {
  apiKey: "YOUR_API_KEY",
  authDomain: "YOUR_AUTH_DOMAIN",
  projectId: "YOUR_PROJECT_ID",
  storageBucket: "YOUR_STORAGE_BUCKET",
  messagingSenderId: "YOUR_MESSAGING_SENDER_ID",
  appId: "YOUR_APP_ID"
};
```

Bạn có thể lấy thông tin này từ:
- Firebase Console > Project Settings > General > Your apps
- File `google-services.json` trong project Android

### 4. Thiết lập Firebase Authentication

Trong Firebase Console:
1. Vào **Authentication** > **Sign-in method**
2. Bật **Email/Password**
3. (Tùy chọn) Cấu hình domain cho phép

### 5. Thiết lập Firestore Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Cho phép Admin đọc/ghi tất cả
    match /{document=**} {
      allow read, write: if request.auth != null 
        && get(/databases/$(database)/documents/NguoiDung/$(request.auth.uid)).data.chucVu == 'Admin';
    }
  }
}
```

### 6. Thiết lập Storage Rules

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /{allPaths=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null 
        && firestore.get(/databases/(default)/documents/NguoiDung/$(request.auth.uid)).data.chucVu == 'Admin';
    }
  }
}
```

### 7. Chạy ứng dụng

```bash
npm run dev
```

Ứng dụng sẽ chạy tại `http://localhost:3000`

## Cấu trúc dự án

```
admin-web/
├── src/
│   ├── components/       # Components tái sử dụng
│   │   ├── Layout.jsx
│   │   ├── Modal.jsx
│   │   ├── NhanVienForm.jsx
│   │   └── ProtectedRoute.jsx
│   ├── config/           # Cấu hình
│   │   └── firebase.js
│   ├── contexts/         # React Context
│   │   └── AuthContext.jsx
│   ├── pages/            # Các trang chính
│   │   ├── Dashboard.jsx
│   │   ├── Login.jsx
│   │   ├── QuanLyNhanVien.jsx
│   │   ├── QuanLyDoanhThu.jsx
│   │   ├── QuanLyBan.jsx
│   │   ├── QuanLyLoaiHang.jsx
│   │   ├── QuanLyHangHoa.jsx
│   │   ├── QuanLyHoaDon.jsx
│   │   └── DuyetHoaDon.jsx
│   ├── App.jsx           # Main App component
│   ├── main.jsx          # Entry point
│   └── index.css         # Global styles
├── index.html
├── package.json
├── vite.config.js
├── tailwind.config.js
└── README.md
```

## Collections trong Firestore

Dự án sử dụng các collections sau (tương ứng với Android app):

- `Ban` - Thông tin bàn
- `NguoiDung` - Người dùng (Admin, Nhân viên, Khách hàng)
- `LoaiHang` - Loại hàng
- `HangHoa` - Hàng hóa
- `HoaDon` - Hóa đơn
- `HoaDonChiTiet` - Chi tiết hóa đơn
- `HoaDonMangVe` - Hóa đơn mang về
- `ThongBao` - Thông báo

## Lưu ý quan trọng

1. **Authentication**: Hiện tại project sử dụng custom authentication với Firestore. Nếu bạn muốn dùng Firebase Auth chính thức, cần cập nhật logic trong `AuthContext.jsx`.

2. **Security Rules**: Đảm bảo thiết lập Security Rules đúng để bảo vệ dữ liệu.

3. **Error Handling**: Một số chức năng có thể cần xử lý lỗi tốt hơn tùy theo nhu cầu.

4. **Optimization**: Có thể tối ưu thêm bằng cách:
   - Sử dụng React Query cho data fetching
   - Implement pagination cho danh sách dài
   - Cache images

## Build cho production

```bash
npm run build
```

Files build sẽ được tạo trong folder `dist/`.

## Hỗ trợ

Nếu gặp vấn đề, vui lòng kiểm tra:
1. Firebase configuration
2. Security Rules
3. Network connection
4. Console logs trong browser

## License

MIT

