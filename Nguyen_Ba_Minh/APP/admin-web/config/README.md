# Firebase Configuration Folder

Folder này chứa các file cấu hình Firebase cho các mục đích khác nhau.

## File trong folder này

### 1. `thuoc-3e916-firebase-adminsdk-*.json`
- **Firebase Admin SDK Service Account Key**
- **CỰC KỲ NHẠY CẢM** - Không bao giờ commit vào Git!
- Chỉ dùng cho backend/server-side
- Đã được thêm vào `.gitignore`

### 2. `firebase-admin.config.js`
- Config file để sử dụng Admin SDK
- Chỉ hoạt động trong Node.js environment
- KHÔNG dùng trong React Client App

## Sử dụng

### React Client App (Browser)
```javascript
// ✅ ĐÚNG - Dùng file này
import { db, storage } from '../src/config/firebase.js';
```

### Backend Server (Node.js)
```javascript
// ✅ ĐÚNG - Dùng trong backend/server
import { getAdmin } from '../config/firebase-admin.config.js';
const admin = getAdmin();
```

## Lưu ý bảo mật

1. File Admin SDK đã được thêm vào `.gitignore`
2. KHÔNG commit file này vào Git repository
3. Chỉ dùng trong server-side code
4. Nếu cần, sử dụng environment variables thay vì file

