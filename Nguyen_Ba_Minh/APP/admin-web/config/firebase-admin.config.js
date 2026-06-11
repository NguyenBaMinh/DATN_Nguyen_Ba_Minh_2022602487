// ⚠️ CẢNH BÁO: File này chỉ dùng cho BACKEND/SERVER
// KHÔNG import file này trong React Client App!
// 
// File Admin SDK chỉ dùng cho:
// - Backend API server (Node.js/Express)
// - Cloud Functions
// - Server-side scripts
// - Cron jobs

// Sử dụng environment variables thay vì file trực tiếp (Bảo mật hơn)
let admin;

if (typeof window === 'undefined') {
  // Chỉ chạy trong Node.js environment (server-side)
  const adminSDK = require('firebase-admin');
  const serviceAccount = require('./thuoc-3e916-firebase-adminsdk-fi7pr-fb7b37f188.json');

  if (!adminSDK.apps.length) {
    admin = adminSDK.initializeApp({
      credential: adminSDK.credential.cert(serviceAccount),
      projectId: serviceAccount.project_id,
      storageBucket: serviceAccount.project_id + '.appspot.com',
    });
  } else {
    admin = adminSDK.app();
  }
}

// Export cho server-side use
export const getAdmin = () => {
  if (typeof window !== 'undefined') {
    throw new Error('Firebase Admin SDK chỉ dùng trong server-side!');
  }
  return admin;
};

export default admin;

