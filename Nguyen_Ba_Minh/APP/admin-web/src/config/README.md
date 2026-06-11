# Firebase Configuration

File này chứa cấu hình Firebase cho React Web App.

## Cấu hình hiện tại

- **Project ID**: `thuoc-3e916`
- **Web App ID**: `1:163924833754:web:397c128d3cfe4249c2fce3` ✅
- **Storage Bucket**: `thuoc-3e916.appspot.com`
- **Database URL**: `https://thuoc-3e916-default-rtdb.firebaseio.com`

## Services được export

### Firestore
```javascript
import { db } from '../config/firebase';
import { COLLECTIONS } from '../config/firebase';

// Sử dụng
const snapshot = await getDocs(collection(db, COLLECTIONS.NGUOI_DUNG));
```

### Storage
```javascript
import { storage } from '../config/firebase';

// Sử dụng
const storageRef = ref(storage, 'path/to/file');
```

### Realtime Database (nếu cần)
```javascript
import { database } from '../config/firebase';

// Sử dụng
const dbRef = ref(database, 'path/to/data');
```

### Analytics (tùy chọn)
```javascript
import { analytics } from '../config/firebase';

// Analytics sẽ tự động khởi tạo nếu được hỗ trợ
```

## Collections

Các collection names được định nghĩa trong `COLLECTIONS`:
- `BAN` - Bàn
- `NGUOI_DUNG` - Người dùng
- `LOAI_HANG` - Loại hàng
- `HANG_HOA` - Hàng hóa
- `HOA_DON` - Hóa đơn
- `HOA_DON_CHI_TIET` - Chi tiết hóa đơn
- `HOA_DON_MANG_VE` - Hóa đơn mang về
- `THONG_BAO` - Thông báo

## Lưu ý

- Config này dùng cho **Client SDK** (React app)
- KHÔNG chứa private keys
- An toàn để expose trong browser
- Để bảo mật, sử dụng Firestore Security Rules

