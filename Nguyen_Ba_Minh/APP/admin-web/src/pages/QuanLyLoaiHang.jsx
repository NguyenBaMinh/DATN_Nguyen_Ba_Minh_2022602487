import { useEffect, useState } from 'react';
import { ref, uploadBytes, getDownloadURL } from 'firebase/storage';
import { storage } from '../config/firebase';
import { getAllFromRealtimeDB, updateArrayItem, deleteArrayItem, REALTIME_NODES } from '../utils/realtimeDB';
import { getImageUrl } from '../utils/imageUtils';
import { Plus, Edit, Trash2 } from 'lucide-react';
import toast from 'react-hot-toast';
import Modal from '../components/Modal';
import ImageDisplay from '../components/ImageDisplay';

const QuanLyLoaiHang = () => {
  const [loaiHangList, setLoaiHangList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editingItem, setEditingItem] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [previewImage, setPreviewImage] = useState(null);
  const [loadingImage, setLoadingImage] = useState(false);
  const [formData, setFormData] = useState({ tenLoai: '', hinhAnh: null });

  useEffect(() => {
    loadLoaiHang();
  }, []);

  const loadLoaiHang = async () => {
    try {
      // LoaiHang là array trong Realtime Database
      const data = await getAllFromRealtimeDB(REALTIME_NODES.LOAI_HANG, true);
      
      // Sắp xếp theo maLoai
      data.sort((a, b) => {
        const aNum = a.maLoai || parseInt(a.id) || 0;
        const bNum = b.maLoai || parseInt(b.id) || 0;
        return aNum - bNum;
      });
      
      setLoaiHangList(data);
    } catch (error) {
      console.error('Error loading loai hang:', error);
      toast.error('Lỗi khi tải danh sách loại hàng');
    } finally {
      setLoading(false);
    }
  };

  const handleAdd = () => {
    setEditingItem(null);
    setPreviewImage(null);
    setFormData({ tenLoai: '', hinhAnh: null });
    setShowModal(true);
  };

  const handleEdit = async (item) => {
    setEditingItem(item);
    setFormData({ tenLoai: item.tenLoai || '', hinhAnh: null });
    
    // Hiển thị ảnh ngay nếu có hinhAnhUrl
    if (item.hinhAnhUrl) {
      setPreviewImage(item.hinhAnhUrl);
      setLoadingImage(false);
    } else {
      // Thử load từ Storage
      setLoadingImage(true);
      setPreviewImage(null);
      
      try {
        const imageUrl = await getImageUrl(item, 'loaihang');
        if (imageUrl) {
          console.log('✅ Loaded image from Storage for preview:', imageUrl);
          setPreviewImage(imageUrl);
        } else {
          console.log('⚠️ No image found in Storage for maLoai:', item.maLoai);
        }
      } catch (error) {
        console.error('❌ Error loading image for preview:', error);
      } finally {
        setLoadingImage(false);
      }
    }
    
    setShowModal(true);
  };
  
  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      // Validate file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        toast.error('Kích thước file không được vượt quá 5MB');
        return;
      }
      
      // Validate file type
      if (!file.type.startsWith('image/')) {
        toast.error('Vui lòng chọn file hình ảnh');
        return;
      }
      
      setFormData({ ...formData, hinhAnh: file });
      
      // Preview image
      const reader = new FileReader();
      reader.onloadend = () => {
        setPreviewImage(reader.result);
      };
      reader.readAsDataURL(file);
    }
  };
  
  const handleModalClose = () => {
    setShowModal(false);
    setEditingItem(null);
    setPreviewImage(null);
    setLoadingImage(false);
    setFormData({ tenLoai: '', hinhAnh: null });
  };

  const handleDelete = async (item) => {
    if (!window.confirm(`Bạn có chắc muốn xóa loại hàng "${item.tenLoai}"?`)) {
      return;
    }

    try {
      const maLoai = item.maLoai || parseInt(item.id);
      await deleteArrayItem(REALTIME_NODES.LOAI_HANG, maLoai);
      toast.success('Xóa loại hàng thành công');
      loadLoaiHang();
    } catch (error) {
      console.error('Error deleting loai hang:', error);
      toast.error('Lỗi khi xóa loại hàng');
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Validation
    if (!formData.tenLoai || !formData.tenLoai.trim()) {
      toast.error('Vui lòng nhập tên loại hàng');
      return;
    }
    
    setSubmitting(true);
    
    try {
      let hinhAnhUrl = editingItem?.hinhAnhUrl;

      let maLoai;
      
      if (editingItem) {
        // Cập nhật
        maLoai = editingItem.maLoai || parseInt(editingItem.id);
      } else {
        // Tạo mới - tìm maLoai cao nhất
        const maxMaLoai = loaiHangList.reduce((max, item) => {
          const num = item.maLoai || parseInt(item.id) || 0;
          return num > max ? num : max;
        }, 0);
        maLoai = maxMaLoai + 1;
      }

      // Upload hình ảnh với format: loaihang/{maLoai}.jpg (theo Android app)
      if (formData.hinhAnh) {
        try {
          const fileRef = ref(storage, `loaihang/${maLoai}.jpg`);
          await uploadBytes(fileRef, formData.hinhAnh);
          hinhAnhUrl = await getDownloadURL(fileRef);
        } catch (uploadError) {
          console.error('Error uploading image:', uploadError);
          toast.error('Lỗi khi upload hình ảnh: ' + (uploadError.message || 'Vui lòng thử lại'));
          setSubmitting(false);
          return;
        }
      }

      const now = Date.now();
      const newData = {
        tenLoai: formData.tenLoai.trim(),
        maLoai: maLoai,
        hasImage: !!hinhAnhUrl,
        lastModified: now,
        ...(hinhAnhUrl && { hinhAnhUrl }),
      };

      if (editingItem) {
        // Cập nhật - merge với data cũ
        const updatedData = {
          ...editingItem, // Giữ lại tất cả fields cũ
          ...newData, // Cập nhật các fields mới
        };
        await updateArrayItem(REALTIME_NODES.LOAI_HANG, maLoai, updatedData);
        toast.success('Cập nhật loại hàng thành công');
      } else {
        // Tạo mới
        await updateArrayItem(REALTIME_NODES.LOAI_HANG, maLoai, newData);
        toast.success('Thêm loại hàng thành công');
      }

      handleModalClose();
      loadLoaiHang();
    } catch (error) {
      console.error('Error saving loai hang:', error);
      toast.error(error.message || 'Lỗi khi lưu loại hàng. Vui lòng thử lại.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="p-8">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Quản lý Loại hàng</h1>
          <p className="text-gray-600 mt-2">Quản lý các loại hàng trong cửa hàng</p>
        </div>
        <button onClick={handleAdd} className="btn-primary flex items-center space-x-2">
          <Plus className="w-5 h-5" />
          <span>Thêm loại hàng</span>
        </button>
      </div>

      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[1, 2, 3, 4, 5, 6].map((i) => (
            <div key={i} className="card animate-pulse">
              <div className="h-48 bg-gray-200 rounded"></div>
            </div>
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {loaiHangList.map((item) => (
            <div key={item.id || item.maLoai} className="card">
              <ImageDisplay
                item={item}
                type="loaihang"
                alt={item.tenLoai}
                className="w-full h-48 object-cover rounded-lg mb-4"
                fallback={
                  <div className="w-full h-48 bg-gray-100 flex items-center justify-center rounded-lg mb-4">
                    <span className="text-gray-400 text-sm">Không có hình ảnh</span>
                  </div>
                }
              />
              <h3 className="text-xl font-bold text-gray-900 mb-4">{item.tenLoai}</h3>
              <div className="flex space-x-2">
                <button
                  onClick={() => handleEdit(item)}
                  className="flex-1 btn-secondary flex items-center justify-center space-x-2"
                >
                  <Edit className="w-4 h-4" />
                  <span>Sửa</span>
                </button>
                <button
                  onClick={() => handleDelete(item)}
                  className="flex-1 btn-danger flex items-center justify-center space-x-2"
                >
                  <Trash2 className="w-4 h-4" />
                  <span>Xóa</span>
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {!loading && loaiHangList.length === 0 && (
        <div className="card text-center py-12">
          <p className="text-gray-500">Chưa có loại hàng nào. Hãy thêm loại hàng mới!</p>
        </div>
      )}

      {showModal && (
        <Modal
          title={editingItem ? 'Cập nhật loại hàng' : 'Thêm loại hàng mới'}
          onClose={handleModalClose}
        >
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="label">Tên loại hàng</label>
              <input
                type="text"
                value={formData.tenLoai}
                onChange={(e) => setFormData({ ...formData, tenLoai: e.target.value })}
                className="input"
                required
              />
            </div>
            <div>
              <label className="label">
                Hình ảnh
                {!editingItem && <span className="text-gray-500 text-sm ml-2">(tùy chọn)</span>}
                {editingItem && <span className="text-gray-500 text-sm ml-2">(chọn file mới để thay thế)</span>}
              </label>
              <input
                type="file"
                accept="image/*"
                onChange={handleFileChange}
                className="input"
                disabled={submitting}
              />
              {loadingImage && (
                <div className="mt-2 w-full h-48 bg-gray-200 animate-pulse rounded-lg flex items-center justify-center">
                  <span className="text-gray-500 text-sm">Đang tải hình ảnh...</span>
                </div>
              )}
              {!loadingImage && previewImage && (
                <div className="mt-2 relative">
                  <img
                    src={previewImage}
                    alt="Preview"
                    className="w-full h-48 object-cover rounded-lg border border-gray-200"
                    onError={(e) => {
                      console.error('Failed to load preview image:', previewImage);
                      setPreviewImage(null);
                    }}
                  />
                  {formData.hinhAnh && (
                    <div className="absolute top-2 right-2 bg-green-500 text-white text-xs px-2 py-1 rounded">
                      Ảnh mới
                    </div>
                  )}
                </div>
              )}
              {!loadingImage && !previewImage && editingItem && (
                <p className="text-gray-500 text-sm mt-2">Loại hàng này chưa có hình ảnh</p>
              )}
            </div>
            <div className="flex space-x-3 pt-4">
              <button 
                type="submit" 
                className="flex-1 btn-primary disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center"
                disabled={submitting}
              >
                {submitting ? (
                  <>
                    <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    Đang lưu...
                  </>
                ) : (
                  editingItem ? 'Cập nhật' : 'Thêm mới'
                )}
              </button>
              <button
                type="button"
                onClick={handleModalClose}
                className="flex-1 btn-secondary disabled:opacity-50 disabled:cursor-not-allowed"
                disabled={submitting}
              >
                Hủy
              </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
};

export default QuanLyLoaiHang;
