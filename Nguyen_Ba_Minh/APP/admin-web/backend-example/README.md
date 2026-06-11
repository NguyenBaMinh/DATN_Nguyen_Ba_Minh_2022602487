# Backend Server Example (Sử dụng Firebase Admin SDK)

Đây là ví dụ về cách tạo backend server sử dụng Firebase Admin SDK.

## ⚠️ QUAN TRỌNG

- Backend này CHẠY RIÊNG BIỆT với React app
- React app vẫn dùng Client SDK (src/config/firebase.js)
- Backend dùng Admin SDK cho các tác vụ đặc biệt

## Cấu trúc

```
admin-web/
├── src/                    # React app (Client SDK)
├── backend/                # Backend server (Admin SDK)
│   ├── server.js
│   ├── package.json
│   └── ...
```

## Tại sao cần Backend?

Backend với Admin SDK hữu ích cho:
- Batch operations
- Scheduled tasks
- Admin-only operations
- Bypass security rules khi cần
- Cloud Functions

## Lưu ý

Backend không bắt buộc. React app có thể hoạt động độc lập với Client SDK.

