// Utility functions để xử lý date trong Realtime Database
// Realtime Database lưu date dưới dạng string, không phải Timestamp
// Format theo Android app:
// - Date: "yyyy-MM-dd" (XDate.toStringDate)
// - DateTime: "dd-MM-yyyy HH:mm:ss" (XDate.toStringDateTime)

/**
 * Parse date string từ Realtime Database
 * Format: "yyyy-MM-dd" (ngaySinh, ngayXuatHoaDon, ngayThongBao)
 */
export const parseRealtimeDate = (dateStr) => {
  if (!dateStr) return null;
  if (typeof dateStr === 'string') {
    // Format: "yyyy-MM-dd"
    if (dateStr.match(/^\d{4}-\d{2}-\d{2}$/)) {
      return new Date(dateStr + 'T00:00:00');
    }
    // Fallback: try parse directly
    const parsed = new Date(dateStr);
    if (!isNaN(parsed.getTime())) {
      return parsed;
    }
  }
  // Nếu là Timestamp object (Firestore - fallback)
  if (dateStr && typeof dateStr.toDate === 'function') {
    return dateStr.toDate();
  }
  // Nếu là Date object
  if (dateStr instanceof Date) {
    return dateStr;
  }
  return null;
};

/**
 * Parse datetime string từ Realtime Database
 * Format: "dd-MM-yyyy HH:mm:ss" (gioVao, gioRa)
 */
export const parseRealtimeDateTime = (dateTimeStr) => {
  if (!dateTimeStr) return null;

  // Nếu là Date object
  if (dateTimeStr instanceof Date) {
    return dateTimeStr;
  }

  // Nếu là Timestamp object (Firestore - fallback)
  if (typeof dateTimeStr.toDate === 'function') {
    return dateTimeStr.toDate();
  }

  // Nếu là kiểu số (Timestamp milliseconds)
  if (typeof dateTimeStr === 'number') {
    return new Date(dateTimeStr);
  }

  if (typeof dateTimeStr === 'string') {
    try {
      // Cắt bỏ khoảng trắng thừa và tách lấy Ngày - Giờ
      const parts = dateTimeStr.trim().split(' ');
      const datePart = parts[0];
      const timePart = parts[1] || '00:00:00'; // Nếu không có giờ, cho mặc định là 0h

      // Tách chuỗi ngày theo dấu gạch ngang
      const dateValues = datePart.split('-');
      
      if (dateValues.length === 3) {
        let day, month, year;

        // Xử lý linh hoạt: Kiểm tra xem Năm đang đứng ở đầu hay ở cuối
        if (dateValues[0].length === 4) {
          // Định dạng YYYY-MM-DD
          year = parseInt(dateValues[0]);
          month = parseInt(dateValues[1]);
          day = parseInt(dateValues[2]);
        } else {
          // Định dạng DD-MM-YYYY (Hoặc D-M-YYYY)
          day = parseInt(dateValues[0]);
          month = parseInt(dateValues[1]);
          year = parseInt(dateValues[2]);
        }

        // Tách chuỗi giờ
        const timeValues = timePart.split(':');
        const hour = parseInt(timeValues[0] || 0);
        const minute = parseInt(timeValues[1] || 0);
        const second = parseInt(timeValues[2] || 0);

        // Tạo Date object
        const parsedDate = new Date(year, month - 1, day, hour, minute, second);
        
        // Kiểm tra xem parse có thành công không
        if (!isNaN(parsedDate.getTime())) {
          return parsedDate;
        }
      }

      // Fallback: Lỡ định dạng lạ quá thì nhờ Javascript tự dịch (VD: 2026/02/26)
      const fallbackDate = new Date(dateTimeStr);
      if (!isNaN(fallbackDate.getTime())) {
        return fallbackDate;
      }

    } catch (e) {
      console.error("Không thể dịch ngày tháng:", dateTimeStr);
    }
  }

  return null;
};

/**
 * Format date thành string cho Realtime Database
 * Format: "yyyy-MM-dd" (theo XDate.toStringDate trong Android)
 */
export const formatRealtimeDate = (date) => {
  if (!date) return '';
  
  const d = date instanceof Date ? date : new Date(date);
  if (isNaN(d.getTime())) return '';
  
  // Format: "yyyy-MM-dd" (theo XDate.toStringDate)
  const day = String(d.getDate()).padStart(2, '0');
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const year = d.getFullYear();
  return `${year}-${month}-${day}`;
};

/**
 * Format datetime thành string cho Realtime Database
 * Format: "dd-MM-yyyy HH:mm:ss" (theo XDate.toStringDateTime trong Android)
 */
export const formatRealtimeDateTime = (date) => {
  if (!date) return '';
  
  const d = date instanceof Date ? date : new Date(date);
  if (isNaN(d.getTime())) return '';
  
  // Format: "dd-MM-yyyy HH:mm:ss" (theo XDate.toStringDateTime)
  const day = String(d.getDate()).padStart(2, '0');
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const year = d.getFullYear();
  const hour = String(d.getHours()).padStart(2, '0');
  const minute = String(d.getMinutes()).padStart(2, '0');
  const second = String(d.getSeconds()).padStart(2, '0');
  return `${day}-${month}-${year} ${hour}:${minute}:${second}`;
};

/**
 * Format date để hiển thị trong UI
 */
export const formatDisplayDate = (dateStr, format = 'dd/MM/yyyy') => {
  const date = parseRealtimeDate(dateStr);
  if (!date) return '-';
  
  if (format.includes('HH:mm')) {
    // Format với giờ
    return date.toLocaleString('vi-VN');
  } else {
    // Format chỉ ngày
    return date.toLocaleDateString('vi-VN');
  }
};

