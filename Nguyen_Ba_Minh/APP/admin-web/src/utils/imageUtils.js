// Utility functions để lấy URL hình ảnh từ Firebase Storage
// Tương tự như cách Android app làm

import { ref, getDownloadURL } from 'firebase/storage';
import { storage } from '../config/firebase';

/**
 * Lấy download URL từ Firebase Storage path
 * @param {string} storagePath - Đường dẫn trong Storage (ví dụ: 'hanghoa/1.jpg')
 * @returns {Promise<string|null>} - Download URL hoặc null nếu không tìm thấy
 */
export const getImageUrlFromStorage = async (storagePath) => {
  try {
    if (!storagePath) {
      console.warn(`[getImageUrlFromStorage] Empty storage path`);
      return null;
    }
    
    console.log(`[getImageUrlFromStorage] Getting download URL for:`, storagePath);
    const imageRef = ref(storage, storagePath);
    const url = await getDownloadURL(imageRef);
    console.log(`[getImageUrlFromStorage] Got download URL:`, url);
    return url;
  } catch (error) {
    // File không tồn tại là lỗi bình thường, chỉ log ở mức debug
    if (error.code === 'storage/object-not-found') {
      console.log(`[getImageUrlFromStorage] Image not found: ${storagePath}`);
    } else {
      console.error(`[getImageUrlFromStorage] Error getting image URL from ${storagePath}:`, error);
    }
    return null;
  }
};

/**
 * Lấy URL hình ảnh hàng hóa
 * @param {number|string} maHangHoa - Mã hàng hóa
 * @returns {Promise<string|null>} - Download URL hoặc null
 */
export const getHangHoaImageUrl = async (maHangHoa) => {
  if (!maHangHoa) return null;
  return getImageUrlFromStorage(`hanghoa/${maHangHoa}.jpg`);
};

/**
 * Lấy URL hình ảnh loại hàng
 * @param {number|string} maLoai - Mã loại hàng
 * @returns {Promise<string|null>} - Download URL hoặc null
 */
export const getLoaiHangImageUrl = async (maLoai) => {
  if (!maLoai) return null;
  return getImageUrlFromStorage(`loaihang/${maLoai}.jpg`);
};

/**
 * Lấy URL hình ảnh người dùng
 * @param {string} maNguoiDung - Mã người dùng
 * @returns {Promise<string|null>} - Download URL hoặc null
 */
export const getNguoiDungImageUrl = async (maNguoiDung) => {
  if (!maNguoiDung) return null;
  return getImageUrlFromStorage(`nguoidung/${maNguoiDung}.jpg`);
};

/**
 * Lấy URL hình ảnh từ item (tự động xác định loại)
 * Nếu có hinhAnhUrl thì dùng luôn, nếu không thì build từ path
 * @param {Object} item - Item (hangHoa, loaiHang, nguoiDung)
 * @param {string} type - 'hanghoa', 'loaihang', hoặc 'nguoidung'
 * @returns {Promise<string|null>} - Download URL hoặc null
 */
export const getImageUrl = async (item, type) => {
  // Nếu đã có URL thì dùng luôn
  if (item?.hinhAnhUrl) {
    console.log(`[getImageUrl] Using existing hinhAnhUrl:`, item.hinhAnhUrl);
    return item.hinhAnhUrl;
  }
  
  // Bỏ qua check hasImage === false vì hình ảnh có thể vẫn tồn tại trong Storage
  // Luôn thử load từ Storage để đảm bảo không bỏ sót
  
  let id = null;
  let path = null;
  
  switch (type) {
    case 'hanghoa':
      id = item?.maHangHoa || item?.id;
      if (id) {
        path = `hanghoa/${id}.jpg`;
      }
      break;
    case 'loaihang':
      id = item?.maLoai || item?.id;
      if (id) {
        path = `loaihang/${id}.jpg`;
      }
      break;
    case 'nguoidung':
      id = item?.maNguoiDung || item?.id;
      if (id) {
        path = `nguoidung/${id}.jpg`;
      }
      break;
    default:
      console.warn(`[getImageUrl] Unknown type:`, type);
      return null;
  }
  
  if (!path) {
    console.warn(`[getImageUrl] Cannot build path for:`, { item, type, id });
    return null;
  }
  
  console.log(`[getImageUrl] Attempting to load from Storage:`, path);
  const url = await getImageUrlFromStorage(path);
  if (url) {
    console.log(`[getImageUrl] Successfully loaded URL:`, url);
  } else {
    console.warn(`[getImageUrl] Failed to load URL from:`, path);
  }
  return url;
};


