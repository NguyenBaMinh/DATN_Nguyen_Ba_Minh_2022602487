import { useEffect, useState } from 'react';
import { queryRealtimeDB, deleteFromRealtimeDB, REALTIME_NODES } from '../utils/realtimeDB';
import { Plus, Edit, Trash2, Search } from 'lucide-react';
import toast from 'react-hot-toast';
import Modal from '../components/Modal';
import NhanVienForm from '../components/NhanVienForm';
import ImageDisplay from '../components/ImageDisplay';

// Helper để parse date string
const parseDateString = (dateStr) => {
  if (!dateStr) return null;
  if (typeof dateStr === 'string') {
    return new Date(dateStr);
  }
  return dateStr;
};

const QuanLyNhanVien = () => {
  const [nhanVienList, setNhanVienList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [selectedNhanVien, setSelectedNhanVien] = useState(null);
  const [isEditing, setIsEditing] = useState(false);

  useEffect(() => {
    loadNhanVien();
  }, []);

  const loadNhanVien = async () => {
    try {
      // Lấy tất cả nhân viên từ Realtime Database (chức vụ = 'NhanVien')
      // NguoiDung là object, không phải array
      const data = await queryRealtimeDB(
        REALTIME_NODES.NGUOI_DUNG,
        (user) => user.chucVu === 'NhanVien',
        false  // isArray = false vì NguoiDung là object
      );
      
      // Đảm bảo có maNguoiDung = id
      const dataWithId = data.map(user => ({
        ...user,
        id: user.maNguoiDung || user.id,
        maNguoiDung: user.maNguoiDung || user.id,
      }));
      
      setNhanVienList(dataWithId);
    } catch (error) {
      console.error('Error loading nhan vien:', error);
      toast.error('Lỗi khi tải danh sách nhân viên');
    } finally {
      setLoading(false);
    }
  };

  const handleAdd = () => {
    setSelectedNhanVien(null);
    setIsEditing(false);
    setShowModal(true);
  };

  const handleEdit = (nhanVien) => {
    setSelectedNhanVien(nhanVien);
    setIsEditing(true);
    setShowModal(true);
  };

  const handleDelete = async (id, hoVaTen) => {
    if (!window.confirm(`Bạn có chắc muốn xóa nhân viên ${hoVaTen}?`)) {
      return;
    }

    try {
      // Xóa từ Realtime Database
      await deleteFromRealtimeDB(`${REALTIME_NODES.NGUOI_DUNG}/${id}`);
      toast.success('Xóa nhân viên thành công');
      loadNhanVien();
    } catch (error) {
      console.error('Error deleting nhan vien:', error);
      toast.error('Lỗi khi xóa nhân viên');
    }
  };

  const handleModalClose = () => {
    setShowModal(false);
    setSelectedNhanVien(null);
    setIsEditing(false);
  };

  const handleSaveSuccess = () => {
    handleModalClose();
    loadNhanVien();
  };

  const filteredList = nhanVienList.filter((nv) =>
    nv.hoVaTen?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    nv.email?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="p-8">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Quản lý Nhân viên</h1>
          <p className="text-gray-600 mt-2">Quản lý thông tin nhân viên trong hệ thống</p>
        </div>
        <button onClick={handleAdd} className="btn-primary flex items-center space-x-2">
          <Plus className="w-5 h-5" />
          <span>Thêm nhân viên</span>
        </button>
      </div>

      <div className="card mb-6">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
          <input
            type="text"
            placeholder="Tìm kiếm nhân viên..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="input pl-10"
          />
        </div>
      </div>

      {loading ? (
        <div className="card">
          <div className="animate-pulse space-y-4">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-16 bg-gray-200 rounded"></div>
            ))}
          </div>
        </div>
      ) : (
        <div className="card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Avatar
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Họ và tên
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Email
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Giới tính
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Ngày sinh
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Thao tác
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {filteredList.length === 0 ? (
                  <tr>
                    <td colSpan="6" className="px-6 py-4 text-center text-gray-500">
                      Không có nhân viên nào
                    </td>
                  </tr>
                ) : (
                  filteredList.map((nv) => (
                    <tr key={nv.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4 whitespace-nowrap">
                        <ImageDisplay
                          item={nv}
                          type="nguoidung"
                          alt={nv.hoVaTen}
                          className="w-12 h-12 rounded-full object-cover"
                          fallback={
                            <div className="w-12 h-12 rounded-full bg-gray-200 flex items-center justify-center">
                              <span className="text-gray-400 text-xs">
                                {nv.hoVaTen?.charAt(0)?.toUpperCase() || '?'}
                              </span>
                            </div>
                          }
                        />
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm font-medium text-gray-900">{nv.hoVaTen}</div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm text-gray-500">{nv.email}</div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm text-gray-500">{nv.gioiTinh}</div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm text-gray-500">
                          {nv.ngaySinh
                            ? (parseDateString(nv.ngaySinh)?.toLocaleDateString('vi-VN') || '-')
                            : '-'}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                        <div className="flex items-center justify-end space-x-2">
                          <button
                            onClick={() => handleEdit(nv)}
                            className="text-primary-600 hover:text-primary-900"
                          >
                            <Edit className="w-5 h-5" />
                          </button>
                          <button
                            onClick={() => handleDelete(nv.id, nv.hoVaTen)}
                            className="text-red-600 hover:text-red-900"
                          >
                            <Trash2 className="w-5 h-5" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {showModal && (
        <Modal
          title={isEditing ? 'Cập nhật nhân viên' : 'Thêm nhân viên mới'}
          onClose={handleModalClose}
        >
          <NhanVienForm
            nhanVien={selectedNhanVien}
            onSuccess={handleSaveSuccess}
            onCancel={handleModalClose}
          />
        </Modal>
      )}
    </div>
  );
};

export default QuanLyNhanVien;

