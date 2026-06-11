// Utility functions để làm việc với Firebase Realtime Database
// Tương tự như cách Android app làm

import { ref, get, set, remove, push } from 'firebase/database';
import { database } from '../config/firebase';

/**
 * Lấy tất cả documents từ một node trong Realtime Database
 * Xử lý cả array và object
 * @param {string} nodePath - Đường dẫn đến node (ví dụ: 'NguoiDung')
 * @param {boolean} isArray - Nếu true, node là array (Ban, HangHoa, etc), nếu false là object (NguoiDung)
 * @returns {Promise<Array>} - Mảng các objects với id và data
 */
export const getAllFromRealtimeDB = async (nodePath, isArray = false) => {
  try {
    const nodeRef = ref(database, nodePath);
    const snapshot = await get(nodeRef);
    
    if (!snapshot.exists()) {
      return [];
    }
    
    const data = snapshot.val();
    const result = [];
    
    if (isArray) {
      // Nếu là array (Ban, HangHoa, LoaiHang, HoaDon, HoaDonChiTiet, ThongBao, DatBan)
      // Firebase Realtime Database có thể lưu array dưới dạng object với numeric keys
      if (Array.isArray(data)) {
        // Nếu là array thực sự
        data.forEach((item, index) => {
          if (item !== null && item !== undefined) {
            // Lấy ID từ field (maBan, maHangHoa, etc) hoặc từ index
            const id = item.maBan || item.maHangHoa || item.maLoai || item.maHoaDon || item.maHDCT || item.maThongBao || item.maDatBan || index;
            result.push({
              id: id,
              ...item,
            });
          }
        });
      } else if (typeof data === 'object' && data !== null) {
        // Nếu là object với numeric keys (Firebase thường làm vậy với arrays)
        // Kiểm tra xem có phải là object với numeric keys không
        const keys = Object.keys(data);
        const hasNumericKeys = keys.length > 0 && keys.every(key => /^\d+$/.test(key));
        
        if (hasNumericKeys) {
          // Xử lý như array với numeric keys
          keys.forEach((key) => {
            const item = data[key];
            if (item !== null && item !== undefined) {
              const index = parseInt(key);
              // Lấy ID từ field (maBan, maHangHoa, etc) hoặc từ key/index
              const id = item.maBan || item.maHangHoa || item.maLoai || item.maHoaDon || item.maHDCT || item.maThongBao || item.maDatBan || index;
              result.push({
                id: id,
                ...item,
              });
            }
          });
        } else {
          // Nếu không phải numeric keys, xử lý như object bình thường
          for (const key in data) {
            if (data[key] !== null && data[key] !== undefined) {
              const item = data[key];
              const id = item.maBan || item.maHangHoa || item.maLoai || item.maHoaDon || item.maHDCT || item.maThongBao || item.maDatBan || key;
              result.push({
                id: id,
                ...item,
              });
            }
          }
        }
      }
    } else {
      // Nếu là object (NguoiDung, HoaDonMangVe)
      // Key = maNguoiDung, maHoaDon, etc
      for (const key in data) {
        if (data[key] !== null && data[key] !== undefined) {
          result.push({
            id: key,
            ...data[key],
          });
        }
      }
    }
    
    return result;
  } catch (error) {
    console.error(`Error getting all from ${nodePath}:`, error);
    throw error;
  }
};

/**
 * Lấy một document cụ thể từ Realtime Database
 * @param {string} nodePath - Đường dẫn đến node (ví dụ: 'NguoiDung/admin')
 * @returns {Promise<Object|null>} - Object với id và data, hoặc null nếu không tồn tại
 */
export const getOneFromRealtimeDB = async (nodePath) => {
  try {
    const nodeRef = ref(database, nodePath);
    const snapshot = await get(nodeRef);
    
    if (!snapshot.exists()) {
      return null;
    }
    
    const pathParts = nodePath.split('/');
    const id = pathParts[pathParts.length - 1];
    
    return {
      id,
      ...snapshot.val(),
    };
  } catch (error) {
    console.error(`Error getting one from ${nodePath}:`, error);
    throw error;
  }
};

/**
 * Tìm documents theo điều kiện
 * @param {string} nodePath - Đường dẫn đến node (ví dụ: 'NguoiDung')
 * @param {Function} filterFn - Hàm filter (ví dụ: (item) => item.chucVu === 'Admin')
 * @param {boolean} isArray - Nếu true, node là array, nếu false là object
 * @returns {Promise<Array>} - Mảng các objects thỏa điều kiện
 */
export const queryRealtimeDB = async (nodePath, filterFn, isArray = false) => {
  try {
    const all = await getAllFromRealtimeDB(nodePath, isArray);
    return all.filter(filterFn);
  } catch (error) {
    console.error(`Error querying ${nodePath}:`, error);
    throw error;
  }
};

/**
 * Thêm hoặc cập nhật một document trong Realtime Database
 * @param {string} nodePath - Đường dẫn đến node (ví dụ: 'NguoiDung/admin')
 * @param {Object} data - Data để lưu
 * @returns {Promise<void>}
 */
export const setRealtimeDB = async (nodePath, data) => {
  try {
    const nodeRef = ref(database, nodePath);
    await set(nodeRef, data);
  } catch (error) {
    console.error(`Error setting ${nodePath}:`, error);
    throw error;
  }
};

/**
 * Cập nhật một field cụ thể trong Realtime Database
 * @param {string} nodePath - Đường dẫn đến node (ví dụ: 'NguoiDung/admin/hoVaTen')
 * @param {any} value - Giá trị mới
 * @returns {Promise<void>}
 */
export const updateRealtimeDB = async (nodePath, value) => {
  try {
    const nodeRef = ref(database, nodePath);
    await set(nodeRef, value);
  } catch (error) {
    console.error(`Error updating ${nodePath}:`, error);
    throw error;
  }
};

/**
 * Xóa một document từ Realtime Database
 * @param {string} nodePath - Đường dẫn đến node (ví dụ: 'NguoiDung/admin')
 * @returns {Promise<void>}
 */
export const deleteFromRealtimeDB = async (nodePath) => {
  try {
    const nodeRef = ref(database, nodePath);
    await remove(nodeRef);
  } catch (error) {
    console.error(`Error deleting ${nodePath}:`, error);
    throw error;
  }
};

/**
 * Thêm document với auto-generated ID (chỉ dùng cho objects, không dùng cho arrays)
 * @param {string} nodePath - Đường dẫn đến node (ví dụ: 'NguoiDung')
 * @param {Object} data - Data để lưu
 * @returns {Promise<string>} - ID của document mới
 */
export const pushToRealtimeDB = async (nodePath, data) => {
  try {
    const nodeRef = ref(database, nodePath);
    const newRef = push(nodeRef);
    await set(newRef, data);
    return newRef.key;
  } catch (error) {
    console.error(`Error pushing to ${nodePath}:`, error);
    throw error;
  }
};

// Collection names (tương ứng với Android app)
export const REALTIME_NODES = {
  BAN: 'Ban',
  NGUOI_DUNG: 'NguoiDung',
  LOAI_HANG: 'LoaiHang',
  HANG_HOA: 'HangHoa',
  HOA_DON: 'HoaDon',
  HOA_DON_CHI_TIET: 'HoaDonChiTiet',
  HOA_DON_MANG_VE: 'HoaDonMangVe',
  THONG_BAO: 'ThongBao',
  DAT_BAN: 'DatBan',
};

// Helper để xác định node là array hay object
export const getNodeType = (nodeName) => {
  // Arrays: Ban, HangHoa, LoaiHang, HoaDon, HoaDonChiTiet, ThongBao
  const arrayNodes = [
    REALTIME_NODES.BAN,
    REALTIME_NODES.HANG_HOA,
    REALTIME_NODES.LOAI_HANG,
    REALTIME_NODES.HOA_DON,
    REALTIME_NODES.HOA_DON_CHI_TIET,
    REALTIME_NODES.THONG_BAO,
  ];
  
  // Objects: NguoiDung, HoaDonMangVe
  return arrayNodes.includes(nodeName);
};

/**
 * Update array item trong Realtime Database
 * @param {string} nodePath - Đường dẫn đến node (ví dụ: 'Ban')
 * @param {number} index - Index trong array (maBan, maHangHoa, etc)
 * @param {Object} data - Data mới
 * @returns {Promise<void>}
 */
export const updateArrayItem = async (nodePath, index, data) => {
  try {
    // Lấy toàn bộ array
    const nodeRef = ref(database, nodePath);
    const snapshot = await get(nodeRef);
    
    let array = [];
    if (snapshot.exists()) {
      array = snapshot.val();
      if (!Array.isArray(array)) {
        array = [];
      }
    }
    
    // Đảm bảo array đủ lớn
    while (array.length <= index) {
      array.push(null);
    }
    
    // Update tại index
    array[index] = data;
    
    // Set lại toàn bộ array
    await set(nodeRef, array);
  } catch (error) {
    console.error(`Error updating array item at ${nodePath}[${index}]:`, error);
    throw error;
  }
};

/**
 * Delete array item trong Realtime Database (set thành null)
 * @param {string} nodePath - Đường dẫn đến node (ví dụ: 'Ban')
 * @param {number} index - Index trong array (maBan, maHangHoa, etc)
 * @returns {Promise<void>}
 */
export const deleteArrayItem = async (nodePath, index) => {
  try {
    // Lấy toàn bộ array
    const nodeRef = ref(database, nodePath);
    const snapshot = await get(nodeRef);
    
    if (!snapshot.exists()) {
      return;
    }
    
    const array = snapshot.val();
    if (!Array.isArray(array) || index >= array.length) {
      return;
    }
    
    // Set thành null tại index
    array[index] = null;
    
    // Set lại toàn bộ array
    await set(nodeRef, array);
  } catch (error) {
    console.error(`Error deleting array item at ${nodePath}[${index}]:`, error);
    throw error;
  }
};
