import { initializeApp } from 'firebase/app';
import { getFirestore } from 'firebase/firestore';
import { getStorage } from 'firebase/storage';
import { getAnalytics, isSupported } from 'firebase/analytics';
import { getDatabase } from 'firebase/database';

// Firebase configuration - Web App configuration từ Firebase Console
// Project: thuoc-3e916
// Đã tạo Web App trong Firebase Console
const firebaseConfig = {
  apiKey: "AIzaSyA3IFhIq1CGR4F2rZuW6OL6GUYGTm8RIiU",
  authDomain: "thuoc-3e916.firebaseapp.com",
  databaseURL: "https://thuoc-3e916-default-rtdb.firebaseio.com",
  projectId: "thuoc-3e916",
  storageBucket: "thuoc-3e916.appspot.com",
  messagingSenderId: "163924833754",
  appId: "1:163924833754:web:397c128d3cfe4249c2fce3",
  measurementId: "G-HZSFQLWNMF"
};

// Khởi tạo Firebase
const app = initializeApp(firebaseConfig);

// Export các service
export const db = getFirestore(app);
export const storage = getStorage(app);
export const database = getDatabase(app);

// Initialize Analytics (chỉ khi được hỗ trợ và trong browser)
let analytics = null;
if (typeof window !== 'undefined') {
  isSupported().then((supported) => {
    if (supported) {
      analytics = getAnalytics(app);
    }
  }).catch(() => {
    // Analytics không được hỗ trợ
    analytics = null;
  });
}
export { analytics };

// Collection names (tương ứng với Android app)
export const COLLECTIONS = {
  BAN: 'Ban',
  NGUOI_DUNG: 'NguoiDung',
  LOAI_HANG: 'LoaiHang',
  HANG_HOA: 'HangHoa',
  HOA_DON: 'HoaDon',
  HOA_DON_CHI_TIET: 'HoaDonChiTiet',
  HOA_DON_MANG_VE: 'HoaDonMangVe',
  THONG_BAO: 'ThongBao'
};

export default app;

