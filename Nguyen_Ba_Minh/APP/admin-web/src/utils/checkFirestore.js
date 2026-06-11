// Utility để kiểm tra dữ liệu trong Firestore
// Chạy trong browser console để debug

import { collection, getDocs, doc, getDoc } from 'firebase/firestore';
import { db, COLLECTIONS } from '../config/firebase';

export const checkFirestoreData = async () => {
  console.log('🔍 Checking Firestore data...');
  
  try {
    // Kiểm tra collection NguoiDung
    const nguoiDungRef = collection(db, COLLECTIONS.NGUOI_DUNG);
    const snapshot = await getDocs(nguoiDungRef);
    
    console.log(`📊 Total users: ${snapshot.size}`);
    
    snapshot.forEach((docSnap) => {
      const data = docSnap.data();
      console.log(`\n📄 Document ID: ${docSnap.id}`);
      console.log('Data:', {
        hoVaTen: data.hoVaTen,
        email: data.email,
        chucVu: data.chucVu,
        matKhau: data.matKhau ? '***' : 'NULL',
        maNguoiDung: data.maNguoiDung || docSnap.id,
      });
      
      // Kiểm tra admin
      if (data.chucVu === 'Admin') {
        console.log('✅ This is an Admin user');
        console.log('   - Can login with email:', data.email);
        console.log('   - Can login with document ID:', docSnap.id);
      }
    });
    
    // Thử lấy document "admin" trực tiếp
    console.log('\n🔍 Checking document "admin" directly...');
    const adminDoc = await getDoc(doc(db, COLLECTIONS.NGUOI_DUNG, 'admin'));
    if (adminDoc.exists()) {
      const adminData = adminDoc.data();
      console.log('✅ Admin document exists:', {
        id: adminDoc.id,
        hoVaTen: adminData.hoVaTen,
        email: adminData.email,
        chucVu: adminData.chucVu,
        hasPassword: !!adminData.matKhau,
      });
    } else {
      console.log('❌ Admin document does not exist');
    }
    
  } catch (error) {
    console.error('❌ Error checking Firestore:', error);
  }
};

// Export để có thể gọi từ console
if (typeof window !== 'undefined') {
  window.checkFirestoreData = checkFirestoreData;
}

