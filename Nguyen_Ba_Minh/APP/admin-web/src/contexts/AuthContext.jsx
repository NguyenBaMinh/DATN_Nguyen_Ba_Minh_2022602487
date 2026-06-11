import { createContext, useContext, useEffect, useState } from 'react';
import { ref, get } from 'firebase/database';
import { database } from '../config/firebase';
import toast from 'react-hot-toast';

const AuthContext = createContext({});

export const useAuth = () => useContext(AuthContext);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [userData, setUserData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Kiểm tra session storage
    const savedUser = sessionStorage.getItem('adminUser');
    if (savedUser) {
      try {
        const user = JSON.parse(savedUser);
        setUser(user);
        setUserData(user);
        if (user.chucVu !== 'Admin') {
          sessionStorage.removeItem('adminUser');
          setUser(null);
          setUserData(null);
        }
      } catch (error) {
        sessionStorage.removeItem('adminUser');
      }
    }
    setLoading(false);
  }, []);

  const login = async (emailOrUsername, password) => {
    try {
      console.log('🔐 Attempting login with:', emailOrUsername);
      
      let userData = null;
      
      // Ưu tiên: Tìm trong Realtime Database (vì dữ liệu thực tế đang ở đây)
      try {
        // Thử lấy user theo document ID/username trước
        const userRef = ref(database, `NguoiDung/${emailOrUsername}`);
        const snapshot = await get(userRef);
        
        if (snapshot.exists()) {
          userData = { id: emailOrUsername, maNguoiDung: emailOrUsername, ...snapshot.val() };
          console.log('✅ Found user in Realtime Database by username/document ID');
        } else {
          // Nếu không tìm thấy, tìm trong tất cả users theo email
          console.log('⚠️ Not found by username, searching by email in Realtime Database...');
          const allUsersRef = ref(database, 'NguoiDung');
          const allUsersSnapshot = await get(allUsersRef);
          
          if (allUsersSnapshot.exists()) {
            const allUsers = allUsersSnapshot.val();
            for (const userId in allUsers) {
              const user = allUsers[userId];
              if (user.email === emailOrUsername) {
                userData = { id: userId, maNguoiDung: userId, ...user };
                console.log('✅ Found user in Realtime Database by email');
                break;
              }
            }
          }
        }
      } catch (error) {
        console.error('Error reading from Realtime Database:', error);
      }
      
      // Fallback: Nếu không tìm thấy trong Realtime Database, thử Firestore
      if (!userData) {
        console.log('⚠️ Not found in Realtime Database, trying Firestore...');
        try {
          const { collection, query, where, getDocs, doc, getDoc } = await import('firebase/firestore');
          const { db, COLLECTIONS } = await import('../config/firebase');
          
          // Thử lấy trực tiếp theo document ID
          const docSnapshot = await getDoc(doc(db, COLLECTIONS.NGUOI_DUNG, emailOrUsername));
          if (docSnapshot.exists()) {
            userData = { id: docSnapshot.id, ...docSnapshot.data() };
            console.log('✅ Found user in Firestore by document ID');
          } else {
            // Tìm theo email trong Firestore
            const emailQuery = query(
              collection(db, COLLECTIONS.NGUOI_DUNG),
              where('email', '==', emailOrUsername)
            );
            const querySnapshot = await getDocs(emailQuery);
            
            if (!querySnapshot.empty) {
              const docSnap = querySnapshot.docs[0];
              userData = { id: docSnap.id, ...docSnap.data() };
              console.log('✅ Found user in Firestore by email');
            }
          }
        } catch (error) {
          console.error('Error reading from Firestore:', error);
        }
      }
      
      if (!userData) {
        console.error('❌ User not found in both Realtime Database and Firestore');
        throw new Error('Email/Tên đăng nhập hoặc mật khẩu không đúng');
      }
      
      console.log('📋 User data:', { 
        id: userData.id, 
        maNguoiDung: userData.maNguoiDung,
        email: userData.email, 
        chucVu: userData.chucVu,
        hasPassword: !!userData.matKhau,
        passwordType: typeof userData.matKhau
      });
      
      // Kiểm tra quyền admin
      if (!userData.chucVu || userData.chucVu !== 'Admin') {
        console.error('❌ Not admin user. ChucVu:', userData.chucVu);
        throw new Error('Bạn không có quyền truy cập trang admin. Chỉ Admin mới có thể đăng nhập.');
      }
      
      // Kiểm tra mật khẩu
      // Theo code Android, password được sync lên Realtime Database
      // Nhưng có thể không có trong một số trường hợp
      const storedPassword = userData.matKhau ? String(userData.matKhau).trim() : null;
      const inputPassword = String(password).trim();
      
      console.log('🔑 Password check:', {
        hasStoredPassword: !!storedPassword,
        storedLength: storedPassword?.length || 0,
        inputLength: inputPassword.length,
      });
      
      // Nếu có password trong database thì phải match
      if (storedPassword) {
        if (storedPassword !== inputPassword) {
          console.error('❌ Password mismatch. Stored:', storedPassword, 'Input:', inputPassword);
          throw new Error('Email/Tên đăng nhập hoặc mật khẩu không đúng');
        }
      } else {
        // Nếu không có password trong database, cho phép login với bất kỳ password nào
        // (vì password có thể được lưu ở SQLite local như trong Android app)
        console.log('⚠️ No password in database, allowing login for admin');
        if (!inputPassword) {
          throw new Error('Vui lòng nhập mật khẩu');
        }
      }
      
      console.log('✅ Login successful!');
      
      // Lưu vào session storage
      sessionStorage.setItem('adminUser', JSON.stringify(userData));
      setUser(userData);
      setUserData(userData);
      toast.success(`Đăng nhập thành công! Chào mừng ${userData.hoVaTen || 'Admin'}`);
      return userData;
    } catch (error) {
      console.error('❌ Login error:', error);
      console.error('Error details:', {
        message: error.message,
        code: error.code,
        stack: error.stack
      });
      const errorMessage = error.message || 'Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin.';
      toast.error(errorMessage);
      throw error;
    }
  };

  const logout = () => {
    try {
      sessionStorage.removeItem('adminUser');
      setUser(null);
      setUserData(null);
      toast.success('Đăng xuất thành công!');
    } catch (error) {
      toast.error('Đăng xuất thất bại');
      throw error;
    }
  };

  const value = {
    user,
    userData,
    loading,
    login,
    logout,
    isAdmin: userData?.chucVu === 'Admin'
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

