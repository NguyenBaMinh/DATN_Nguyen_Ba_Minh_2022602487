import { useEffect, useState } from 'react';
import { ref, uploadBytes, getDownloadURL } from 'firebase/storage';
import { storage } from '../config/firebase';
import { getAllFromRealtimeDB, updateArrayItem, deleteArrayItem, REALTIME_NODES } from '../utils/realtimeDB';
import { getImageUrl } from '../utils/imageUtils';
import { Plus, Edit, Trash2, Search, Package, DollarSign } from 'lucide-react';
import toast from 'react-hot-toast';
import Modal from '../components/Modal';
import ImageDisplay from '../components/ImageDisplay';
import FilterPanel from '../components/FilterPanel';
import StatusBadge from '../components/StatusBadge';
import SortPanel from '../components/SortPanel';

const QuanLyHangHoa = () => {
  const [hangHoaList, setHangHoaList] = useState([]);
  const [filteredList, setFilteredList] = useState([]);
  const [loaiHangList, setLoaiHangList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [editingItem, setEditingItem] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [previewImage, setPreviewImage] = useState(null);
  const [loadingImage, setLoadingImage] = useState(false);
  const [filters, setFilters] = useState({
    trangThai: '',
    maLoai: '',
    giaTien: { min: '', max: '' }
  });
  const [sortBy, setSortBy] = useState('tenHangHoa:asc');
  const [formData, setFormData] = useState({
    tenHangHoa: '',
    giaTien: '',
    maLoai: '',
    trangThai: 1,
    hinhAnh: null,
  });

  useEffect(() => {
    loadHangHoa();
    loadLoaiHang();
  }, []);

  const loadHangHoa = async () => {
    try {
      // HangHoa là array trong Realtime Database
      const data = await getAllFromRealtimeDB(REALTIME_NODES.HANG_HOA, true);

      // Sắp xếp theo maHangHoa
      data.sort((a, b) => {
        const aNum = a.maHangHoa || parseInt(a.id) || 0;
        const bNum = b.maHangHoa || parseInt(b.id) || 0;
        return aNum - bNum;
      });

      setHangHoaList(data);
    } catch (error) {
      console.error('Error loading hang hoa:', error);
      toast.error('Lỗi khi tải danh sách hàng hóa');
    } finally {
      setLoading(false);
    }
  };

  const loadLoaiHang = async () => {
    try {
      // LoaiHang là array trong Realtime Database
      const data = await getAllFromRealtimeDB(REALTIME_NODES.LOAI_HANG, true);
      setLoaiHangList(data);
    } catch (error) {
      console.error('Error loading loai hang:', error);
    }
  };

  // Filter và Sort
  useEffect(() => {
    let result = [...hangHoaList];

    // Apply filters
    if (filters.trangThai !== '') {
      result = result.filter(item => item.trangThai === parseInt(filters.trangThai));
    }

    if (filters.maLoai !== '') {
      result = result.filter(item => item.maLoai === parseInt(filters.maLoai));
    }

    if (filters.giaTien.min !== '' || filters.giaTien.max !== '') {
      result = result.filter(item => {
        const min = filters.giaTien.min !== '' ? parseInt(filters.giaTien.min) : 0;
        const max = filters.giaTien.max !== '' ? parseInt(filters.giaTien.max) : Infinity;
        return item.giaTien >= min && item.giaTien <= max;
      });
    }

    // Apply search
    if (searchTerm) {
      const searchLower = searchTerm.toLowerCase();
      result = result.filter(item =>
        item.tenHangHoa?.toLowerCase().includes(searchLower) ||
        item.maHangHoa?.toString().includes(searchLower)
      );
    }

    // Apply sort
    const [field, direction] = sortBy.split(':');
    result.sort((a, b) => {
      let aValue, bValue;

      switch (field) {
        case 'tenHangHoa':
          aValue = (a.tenHangHoa || '').toLowerCase();
          bValue = (b.tenHangHoa || '').toLowerCase();
          break;
        case 'giaTien':
          aValue = a.giaTien || 0;
          bValue = b.giaTien || 0;
          break;
        case 'maLoai':
          aValue = a.maLoai || 0;
          bValue = b.maLoai || 0;
          break;
        case 'trangThai':
          aValue = a.trangThai || 0;
          bValue = b.trangThai || 0;
          break;
        default:
          aValue = a.maHangHoa || 0;
          bValue = b.maHangHoa || 0;
      }

      if (direction === 'asc') {
        return aValue > bValue ? 1 : -1;
      } else {
        return aValue < bValue ? 1 : -1;
      }
    });

    setFilteredList(result);
  }, [hangHoaList, filters, searchTerm, sortBy]);

  const handleFilterChange = (key, value) => {
    setFilters(prev => ({
      ...prev,
      [key]: value
    }));
  };

  const handleResetFilters = () => {
    setFilters({
      trangThai: '',
      maLoai: '',
      giaTien: { min: '', max: '' }
    });
    setSearchTerm('');
  };

  const handleAdd = () => {
    setEditingItem(null);
    setFormData({
      tenHangHoa: '',
      giaTien: '',
      maLoai: '',
      trangThai: 1,
      hinhAnh: null,
    });
    setShowModal(true);
  };

  const handleEdit = async (item) => {
    setEditingItem(item);
    setFormData({
      tenHangHoa: item.tenHangHoa || '',
      giaTien: item.giaTien || '',
      maLoai: item.maLoai?.toString() || '',
      trangThai: item.trangThai !== undefined ? item.trangThai : 1,
      hinhAnh: null,
    });

    // Hiển thị ảnh ngay nếu có hinhAnhUrl


    //tại chỗ này sửa hinhAnhUrl thành hinhAnh
    if (item.hinhAnhUrl) {
      setPreviewImage(item.hinhAnhUrl);
      setLoadingImage(false);
    } else {
      // Thử load từ Storage
      setLoadingImage(true);
      setPreviewImage(null);

      try {
        const imageUrl = await getImageUrl(item, 'hanghoa');
        if (imageUrl) {
          console.log('✅ Loaded image from Storage for preview:', imageUrl);
          setPreviewImage(imageUrl);
        } else {
          console.log('⚠️ No image found in Storage for maHangHoa:', item.maHangHoa);
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
    setFormData({
      tenHangHoa: '',
      giaTien: '',
      maLoai: '',
      trangThai: 1,
      hinhAnh: null,
    });
  };

  const handleDelete = async (item) => {
    if (!window.confirm(`Bạn có chắc muốn xóa hàng hóa "${item.tenHangHoa}"?`)) {
      return;
    }

    try {
      const maHangHoa = item.maHangHoa || parseInt(item.id);
      await deleteArrayItem(REALTIME_NODES.HANG_HOA, maHangHoa);
      toast.success('Xóa hàng hóa thành công');
      loadHangHoa();
    } catch (error) {
      console.error('Error deleting hang hoa:', error);
      toast.error('Lỗi khi xóa hàng hóa');
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    // Validation
    if (!formData.tenHangHoa || !formData.tenHangHoa.trim()) {
      toast.error('Vui lòng nhập tên hàng hóa');
      return;
    }

    if (!formData.giaTien || isNaN(parseInt(formData.giaTien)) || parseInt(formData.giaTien) <= 0) {
      toast.error('Vui lòng nhập giá tiền hợp lệ (lớn hơn 0)');
      return;
    }

    if (!formData.maLoai || !formData.maLoai.trim()) {
      toast.error('Vui lòng chọn loại hàng');
      return;
    }

    setSubmitting(true);

    try {
      let maHangHoa;

      if (editingItem) {
        // Cập nhật
        maHangHoa = editingItem.maHangHoa || parseInt(editingItem.id);
      } else {
        // Tạo mới - tìm maHangHoa cao nhất
        const maxMaHangHoa = hangHoaList.reduce((max, item) => {
          const num = item.maHangHoa || parseInt(item.id) || 0;
          return num > max ? num : max;
        }, 0);
        maHangHoa = maxMaHangHoa + 1;
      }

      let hinhAnhUrl = editingItem?.hinhAnhUrl;

      // Upload hình ảnh với format: hanghoa/{maHangHoa}.jpg (theo Android app)
      if (formData.hinhAnh) {
        try {
          // File validation đã được check ở handleFileChange

          const fileRef = ref(storage, `hanghoa/${maHangHoa}.jpg`);
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
        tenHangHoa: formData.tenHangHoa.trim(),
        giaTien: parseInt(formData.giaTien),
        maLoai: parseInt(formData.maLoai),
        maHangHoa: maHangHoa,
        trangThai: formData.trangThai,
        hasImage: !!hinhAnhUrl,
        lastModified: now,
        ...(hinhAnhUrl && { hinhAnhUrl }),
        //sửa dòng cuối này thành hinhAnh: hinhAnhUrl || ""
      };

      if (editingItem) {
        // Cập nhật - merge với data cũ
        const updatedData = {
          ...editingItem, // Giữ lại tất cả fields cũ
          ...newData, // Cập nhật các fields mới
        };
        await updateArrayItem(REALTIME_NODES.HANG_HOA, maHangHoa, updatedData);
        toast.success('Cập nhật hàng hóa thành công');
      } else {
        // Tạo mới
        await updateArrayItem(REALTIME_NODES.HANG_HOA, maHangHoa, newData);
        toast.success('Thêm hàng hóa thành công');
      }

      handleModalClose();
      loadHangHoa();
    } catch (error) {
      console.error('Error saving hang hoa:', error);
      toast.error(error.message || 'Lỗi khi lưu hàng hóa. Vui lòng thử lại.');
    } finally {
      setSubmitting(false);
    }
  };

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('vi-VN').format(amount) + 'đ';
  };

  const getLoaiHangName = (maLoai) => {
    const loaiHang = loaiHangList.find(lh =>
      (lh.maLoai || parseInt(lh.id)) === parseInt(maLoai)
    );
    return loaiHang?.tenLoai || 'N/A';
  };

  // Prepare filter options
  const filterOptions = [
    {
      key: 'trangThai',
      label: 'Trạng thái',
      type: 'select',
      value: filters.trangThai,
      options: [
        { value: '1', label: 'Còn hàng' },
        { value: '0', label: 'Hết hàng' }
      ]
    },
    {
      key: 'maLoai',
      label: 'Loại hàng',
      type: 'select',
      value: filters.maLoai,
      options: loaiHangList.map(loai => ({
        value: (loai.maLoai || loai.id)?.toString(),
        label: loai.tenLoai
      }))
    }
  ];

  const sortOptions = [
    { value: 'tenHangHoa', label: 'Tên' },
    { value: 'giaTien', label: 'Giá tiền' },
    { value: 'maLoai', label: 'Loại' },
    { value: 'trangThai', label: 'Trạng thái' }
  ];

  // Calculate statistics
  const totalValue = hangHoaList.reduce((sum, item) => sum + (item.giaTien || 0), 0);
  const avgPrice = hangHoaList.length > 0 ? Math.round(totalValue / hangHoaList.length) : 0;

  return (
    <div className="p-8">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Quản lý Hàng hóa</h1>
          <p className="text-gray-600 mt-2">Quản lý sản phẩm trong cửa hàng</p>
        </div>
        <button onClick={handleAdd} className="btn-primary flex items-center space-x-2 hover:scale-105 transition-transform">
          <Plus className="w-5 h-5" />
          <span>Thêm hàng hóa</span>
        </button>
      </div>

      {/* Search */}
      <div className="mb-4">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
          <input
            type="text"
            placeholder="Tìm kiếm hàng hóa theo tên, mã..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
        </div>
      </div>

      {/* Filters */}
      <FilterPanel
        filters={filterOptions}
        onFilterChange={handleFilterChange}
        onReset={handleResetFilters}
      />

      {/* Sort */}
      <div className="mb-4">
        <SortPanel
          options={sortOptions}
          value={sortBy}
          onChange={setSortBy}
        />
      </div>

      {/* Statistics */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
        <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-200">
          <div className="text-sm text-gray-600 flex items-center gap-1">
            <Package className="w-4 h-4" />
            Tổng sản phẩm
          </div>
          <div className="text-2xl font-bold text-gray-900">{hangHoaList.length}</div>
        </div>
        <div className="bg-green-50 p-4 rounded-lg shadow-sm border border-green-200">
          <div className="text-sm text-green-800">Còn hàng</div>
          <div className="text-2xl font-bold text-green-900">
            {hangHoaList.filter(h => h.trangThai === 1).length}
          </div>
        </div>
        <div className="bg-red-50 p-4 rounded-lg shadow-sm border border-red-200">
          <div className="text-sm text-red-800">Hết hàng</div>
          <div className="text-2xl font-bold text-red-900">
            {hangHoaList.filter(h => h.trangThai === 0).length}
          </div>
        </div>
        <div className="bg-blue-50 p-4 rounded-lg shadow-sm border border-blue-200">
          <div className="text-sm text-blue-800 flex items-center gap-1">
            <DollarSign className="w-4 h-4" />
            Giá TB
          </div>
          <div className="text-lg font-bold text-blue-900">{formatCurrency(avgPrice)}</div>
        </div>
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
        <>
          {filteredList.length === 0 ? (
            <div className="card text-center py-12 col-span-full">
              <Package className="w-16 h-16 mx-auto text-gray-400 mb-4" />
              <p className="text-gray-500 text-lg">Không tìm thấy sản phẩm nào</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {filteredList.map((item) => (
                <div key={item.id || item.maHangHoa} className="card hover:shadow-lg transition-shadow">
              <ImageDisplay
                item={item}
                type="hanghoa"
                alt={item.tenHangHoa}
                className="w-full h-48 object-cover rounded-lg mb-4"
                fallback={
                  <div className="w-full h-48 bg-gray-100 flex items-center justify-center rounded-lg mb-4">
                    <span className="text-gray-400 text-sm">Không có hình ảnh</span>
                  </div>
                }
              />
              <h3 className="text-xl font-bold text-gray-900 mb-2">{item.tenHangHoa}</h3>
              <p className="text-lg font-semibold text-primary-600 mb-2">
                {formatCurrency(item.giaTien || 0)}
              </p>
              <p className="text-sm text-gray-600 mb-2">
                Loại: {getLoaiHangName(item.maLoai)}
              </p>
              <div className="flex items-center justify-between mb-4">
                <StatusBadge type="hanghoa" value={item.trangThai} size="sm" />
              </div>
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
        </>
      )}

      {!loading && filteredList.length === 0 && (
        <div className="card text-center py-12">
          <p className="text-gray-500">
            {searchTerm ? 'Không tìm thấy hàng hóa nào' : 'Chưa có hàng hóa nào. Hãy thêm hàng hóa mới!'}
          </p>
        </div>
      )}

      {showModal && (
        <Modal
          title={editingItem ? 'Cập nhật hàng hóa' : 'Thêm hàng hóa mới'}
          onClose={handleModalClose}
        >
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="label">Tên hàng hóa</label>
              <input
                type="text"
                value={formData.tenHangHoa}
                onChange={(e) => setFormData({ ...formData, tenHangHoa: e.target.value })}
                className="input"
                required
              />
            </div>
            <div>
              <label className="label">Giá tiền</label>
              <input
                type="number"
                value={formData.giaTien}
                onChange={(e) => setFormData({ ...formData, giaTien: e.target.value })}
                className="input"
                required
                min="0"
              />
            </div>
            <div>
              <label className="label">Loại hàng</label>
              <select
                value={formData.maLoai}
                onChange={(e) => setFormData({ ...formData, maLoai: e.target.value })}
                className="input"
                required
              >
                <option value="">Chọn loại hàng</option>
                {loaiHangList.map((lh) => (
                  <option key={lh.id || lh.maLoai} value={lh.maLoai || lh.id}>
                    {lh.tenLoai}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="label">Trạng thái</label>
              <select
                value={formData.trangThai}
                onChange={(e) => setFormData({ ...formData, trangThai: parseInt(e.target.value) })}
                className="input"
                required
              >
                <option value={1}>Còn hàng</option>
                <option value={0}>Hết hàng</option>
              </select>
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
                <p className="text-gray-500 text-sm mt-2">Hàng hóa này chưa có hình ảnh</p>
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

export default QuanLyHangHoa;
