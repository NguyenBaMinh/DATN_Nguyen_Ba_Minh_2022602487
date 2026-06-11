import { useState, useEffect } from 'react';
import { ref, uploadBytes, getDownloadURL } from 'firebase/storage';
import { storage } from '../config/firebase';
import { queryRealtimeDB, setRealtimeDB, REALTIME_NODES } from '../utils/realtimeDB';
import { getImageUrl } from '../utils/imageUtils';
import { formatRealtimeDate } from '../utils/dateUtils';
import toast from 'react-hot-toast';

const NhanVienForm = ({ nhanVien, onSuccess, onCancel }) => {
  const [formData, setFormData] = useState({
    hoVaTen: '',
    email: '',
    matKhau: '',
    gioiTinh: 'Nam',
    ngaySinh: '',
    chucVu: 'NhanVien',
  });
  const [avatarFile, setAvatarFile] = useState(null);
  const [avatarUrl, setAvatarUrl] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const loadNhanVienData = async () => {
      if (nhanVien) {
        // Parse ngaySinh từ string "yyyy-MM-dd"
        const ngaySinh = nhanVien.ngaySinh
          ? (typeof nhanVien.ngaySinh === 'string'
              ? nhanVien.ngaySinh
              : formatRealtimeDate(nhanVien.ngaySinh))
          : '';
        
        setFormData({
          hoVaTen: nhanVien.hoVaTen || '',
          email: nhanVien.email || '',
          matKhau: '',
          gioiTinh: nhanVien.gioiTinh || 'Nam',
          ngaySinh: ngaySinh,
          chucVu: 'NhanVien',
        });
        
        // Thử load ảnh từ Storage nếu không có hinhAnhUrl
        let imageUrl = nhanVien.hinhAnhUrl || null;
        if (!imageUrl) {
          try {
            imageUrl = await getImageUrl(nhanVien, 'nguoidung');
            console.log('Loaded avatar from Storage for preview:', imageUrl);
          } catch (error) {
            console.error('Error loading avatar for preview:', error);
          }
        }
        
        if (imageUrl) {
          setAvatarUrl(imageUrl);
        }
      }
    };
    
    loadNhanVienData();
  }, [nhanVien]);

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      setAvatarFile(file);
      const reader = new FileReader();
      reader.onloadend = () => {
        setAvatarUrl(reader.result);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Validation
    if (!formData.hoVaTen || !formData.hoVaTen.trim()) {
      toast.error('Vui lòng nhập họ và tên');
      return;
    }
    
    if (!formData.email || !formData.email.trim()) {
      toast.error('Vui lòng nhập email');
      return;
    }
    
    // Validate email format
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(formData.email.trim())) {
      toast.error('Email không đúng định dạng');
      return;
    }
    
    if (!nhanVien && (!formData.matKhau || !formData.matKhau.trim())) {
      toast.error('Vui lòng nhập mật khẩu');
      return;
    }
    
    if (!formData.ngaySinh) {
      toast.error('Vui lòng chọn ngày sinh');
      return;
    }
    
    setLoading(true);

    try {
      // Kiểm tra email trùng (chỉ khi thêm mới hoặc email thay đổi)
      if (!nhanVien || (nhanVien.email !== formData.email.trim())) {
        const allUsers = await queryRealtimeDB(
          REALTIME_NODES.NGUOI_DUNG,
          (user) => user.email === formData.email.trim(),
          false  // isArray = false vì NguoiDung là object
        );
        
        if (allUsers.length > 0) {
          const existingUser = allUsers[0];
          // Bỏ qua chính user đang edit
          if (!nhanVien || existingUser.maNguoiDung !== nhanVien.maNguoiDung) {
            toast.error('Email đã tồn tại');
            setLoading(false);
            return;
          }
        }
      }

      // Xác định maNguoiDung trước
      let maNguoiDung;
      if (nhanVien) {
        maNguoiDung = nhanVien.id || nhanVien.maNguoiDung;
      } else {
        if (!formData.matKhau) {
          toast.error('Vui lòng nhập mật khẩu');
          setLoading(false);
          return;
        }
        // Tạo ID mới (dùng email prefix)
        const emailPrefix = formData.email.split('@')[0];
        maNguoiDung = emailPrefix || `NV${Date.now()}`;
      }

      let avatarUrlToSave = avatarUrl;

      // Upload avatar nếu có file mới
      // Format: nguoidung/{maNguoiDung}.jpg (theo Android app)
      if (avatarFile) {
        const avatarRef = ref(storage, `nguoidung/${maNguoiDung}.jpg`);
        await uploadBytes(avatarRef, avatarFile);
        avatarUrlToSave = await getDownloadURL(avatarRef);
      }

      // Tạo hoặc cập nhật document trong Realtime Database
      const now = Date.now();
      const nhanVienData = {
        hoVaTen: formData.hoVaTen.trim(),
        email: formData.email.trim().toLowerCase(),
        gioiTinh: formData.gioiTinh,
        ngaySinh: formData.ngaySinh || formatRealtimeDate(new Date()),
        chucVu: 'NhanVien',
        maNguoiDung: maNguoiDung,
        lastModified: now,
        hasImage: !!avatarUrlToSave,
        ...(avatarUrlToSave && { hinhAnhUrl: avatarUrlToSave }),
      };

      if (nhanVien) {
        // Cập nhật - merge với data cũ để giữ lại các fields không thay đổi
        const updatedData = {
          ...nhanVien, // Giữ lại tất cả fields cũ
          ...nhanVienData, // Cập nhật các fields mới
        };
        
        // Xử lý mật khẩu
        if (formData.matKhau && formData.matKhau.trim()) {
          updatedData.matKhau = formData.matKhau.trim();
        } else {
          // Giữ nguyên mật khẩu cũ
          updatedData.matKhau = nhanVien.matKhau || '';
        }
        
        // Đảm bảo có đầy đủ các fields bắt buộc
        if (!updatedData.maNguoiDung) {
          updatedData.maNguoiDung = maNguoiDung;
        }
        
        // Lưu vào Realtime Database
        await setRealtimeDB(`${REALTIME_NODES.NGUOI_DUNG}/${maNguoiDung}`, updatedData);
        toast.success('Cập nhật nhân viên thành công');
      } else {
        // Tạo mới
        nhanVienData.matKhau = formData.matKhau;
        
        // Lưu vào Realtime Database (object với key = maNguoiDung)
        await setRealtimeDB(`${REALTIME_NODES.NGUOI_DUNG}/${maNguoiDung}`, nhanVienData);
        toast.success('Thêm nhân viên thành công');
      }

      onSuccess();
    } catch (error) {
      console.error('Error saving nhan vien:', error);
      toast.error('Lỗi khi lưu nhân viên');
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label className="label">Họ và tên</label>
        <input
          type="text"
          name="hoVaTen"
          value={formData.hoVaTen}
          onChange={handleChange}
          className="input"
          required
        />
      </div>

      <div>
        <label className="label">Email</label>
        <input
          type="email"
          name="email"
          value={formData.email}
          onChange={handleChange}
          className="input"
          required
          disabled={!!nhanVien}
        />
      </div>

      <div>
        <label className="label">
          Mật khẩu {nhanVien && <span className="text-gray-500 text-sm">(để trống nếu không đổi)</span>}
        </label>
        <input
          type="password"
          name="matKhau"
          value={formData.matKhau}
          onChange={handleChange}
          className="input"
          required={!nhanVien}
          minLength={nhanVien ? 0 : 6}
          placeholder={nhanVien ? "Để trống để giữ mật khẩu cũ" : "Tối thiểu 6 ký tự"}
        />
        {!nhanVien && formData.matKhau && formData.matKhau.length > 0 && formData.matKhau.length < 6 && (
          <p className="text-red-500 text-sm mt-1">Mật khẩu phải có ít nhất 6 ký tự</p>
        )}
      </div>

      <div>
        <label className="label">Giới tính</label>
        <select
          name="gioiTinh"
          value={formData.gioiTinh}
          onChange={handleChange}
          className="input"
          required
        >
          <option value="Nam">Nam</option>
          <option value="Nu">Nữ</option>
        </select>
      </div>

      <div>
        <label className="label">Ngày sinh</label>
        <input
          type="date"
          name="ngaySinh"
          value={formData.ngaySinh}
          onChange={handleChange}
          className="input"
          required
        />
      </div>

      <div>
        <label className="label">Ảnh đại diện</label>
        <input
          type="file"
          accept="image/*"
          onChange={handleFileChange}
          className="input"
        />
        {avatarUrl && (
          <img
            src={avatarUrl}
            alt="Avatar"
            className="mt-2 w-24 h-24 rounded-full object-cover"
          />
        )}
      </div>

      <div className="flex space-x-3 pt-4">
        <button
          type="submit"
          disabled={loading}
          className="flex-1 btn-primary disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center"
        >
          {loading ? (
            <>
              <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              Đang lưu...
            </>
          ) : (
            nhanVien ? 'Cập nhật' : 'Thêm mới'
          )}
        </button>
        <button
          type="button"
          onClick={onCancel}
          className="flex-1 btn-secondary disabled:opacity-50 disabled:cursor-not-allowed"
          disabled={loading}
        >
          Hủy
        </button>
      </div>
    </form>
  );
};

export default NhanVienForm;
