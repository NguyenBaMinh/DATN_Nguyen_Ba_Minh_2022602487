import { useEffect, useState } from 'react';
import { getAllFromRealtimeDB, queryRealtimeDB, setRealtimeDB, REALTIME_NODES } from '../utils/realtimeDB';
import { parseRealtimeDateTime } from '../utils/dateUtils';
import { CheckCircle, XCircle } from 'lucide-react';
import toast from 'react-hot-toast';
import { format } from 'date-fns';
import { vi } from 'date-fns/locale';
import ImageDisplay from '../components/ImageDisplay';

const DuyetHoaDon = () => {
  const [hoaDonMangVeList, setHoaDonMangVeList] = useState([]);
  const [hangHoaList, setHangHoaList] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadHoaDonMangVe();
    loadHangHoa();
  }, []);

  const loadHangHoa = async () => {
    try {
      const data = await getAllFromRealtimeDB(REALTIME_NODES.HANG_HOA, true);
      setHangHoaList(data);
    } catch (error) {
      console.error('Error loading hang hoa:', error);
    }
  };

  const loadHoaDonMangVe = async () => {
    try {
      // HoaDonMangVe là object trong Realtime Database (key = maHoaDon)
      const allHoaDonMangVe = await getAllFromRealtimeDB(REALTIME_NODES.HOA_DON_MANG_VE, false);
      
      // Lọc các hóa đơn chờ duyệt (trangThai === 1) hoặc chưa xác nhận (trangThai === -1)
      const hoaDonChuaDuyet = allHoaDonMangVe.filter(hd => {
        // TrangThai: 1 = CHUA_DUYET (chưa duyệt), -1 = CHUA_XAC_NHAN (chưa xác nhận), 0 = DA_DUYET (đã duyệt), 2 = HUY_HOA_DON (hủy)
        return hd.trangThai === 1 || hd.trangThai === -1;
      });
      
      // HoaDonChiTiet là array
      const hoaDonChiTietArray = await getAllFromRealtimeDB(REALTIME_NODES.HOA_DON_CHI_TIET, true);
      
      // Kết hợp với chi tiết
      const data = hoaDonChuaDuyet.map((hoaDon) => {
        const maHoaDon = hoaDon.maHoaDon || parseInt(hoaDon.id);
        
        // Lọc chi tiết theo maHoaDon
        const chiTiet = hoaDonChiTietArray.filter(ct => {
          const ctMaHoaDon = ct.maHoaDon;
          return ctMaHoaDon === maHoaDon || ctMaHoaDon === parseInt(hoaDon.id);
        }).map(ct => {
          // Thêm tên hàng hóa từ HangHoa
          const hangHoa = hangHoaList.find(hh => 
            (hh.maHangHoa || parseInt(hh.id)) === ct.maHangHoa
          );
          return {
            ...ct,
            tenHangHoa: hangHoa?.tenHangHoa || `Hàng hóa #${ct.maHangHoa}`,
            hangHoa: hangHoa, // Thêm thông tin hàng hóa để hiển thị ảnh
          };
        });
        
        // Tính tổng tiền
        const tongTien = chiTiet.reduce(
          (sum, ct) => sum + (ct.soLuong || 0) * (ct.giaTien || 0),
          0
        );

        return {
          ...hoaDon,
          chiTiet,
          tongTien,
        };
      });

      // Sắp xếp theo ngày mới nhất
      data.sort((a, b) => {
        const dateA = parseRealtimeDateTime(a.gioVao) || new Date(0);
        const dateB = parseRealtimeDateTime(b.gioVao) || new Date(0);
        return dateB - dateA;
      });

      setHoaDonMangVeList(data);
    } catch (error) {
      console.error('Error loading hoa don mang ve:', error);
      toast.error('Lỗi khi tải danh sách hóa đơn');
    } finally {
      setLoading(false);
    }
  };

  // Reload khi hangHoaList thay đổi
  useEffect(() => {
    if (hangHoaList.length > 0) {
      loadHoaDonMangVe();
    }
  }, [hangHoaList.length]);

  const handleDuyet = async (hoaDon, approved = true) => {
    try {
      const maHoaDon = hoaDon.maHoaDon || hoaDon.id;
      const now = Date.now();
      
      // Update trong Realtime Database (object)
      // 0: Đã duyệt (DA_DUYET), 2: Hủy (HUY_HOA_DON)
      await setRealtimeDB(`${REALTIME_NODES.HOA_DON_MANG_VE}/${maHoaDon}`, {
        ...hoaDon,
        trangThai: approved ? 0 : 2, // 0 = DA_DUYET, 2 = HUY_HOA_DON
        lastModified: now,
      });
      
      toast.success(approved ? 'Duyệt hóa đơn thành công' : 'Từ chối hóa đơn thành công');
      loadHoaDonMangVe();
    } catch (error) {
      console.error('Error updating hoa don:', error);
      toast.error('Lỗi khi cập nhật hóa đơn');
    }
  };

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('vi-VN').format(amount) + 'đ';
  };

  const formatDateTime = (dateTimeStr) => {
    if (!dateTimeStr) return '-';
    const date = parseRealtimeDateTime(dateTimeStr);
    if (!date) return '-';
    return format(date, 'dd/MM/yyyy HH:mm', { locale: vi });
  };

  return (
    <div className="p-8">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Duyệt Hóa đơn</h1>
        <p className="text-gray-600 mt-2">Duyệt các hóa đơn mang về chờ xử lý</p>
      </div>

      {loading ? (
        <div className="card">
          <div className="animate-pulse space-y-4">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-32 bg-gray-200 rounded"></div>
            ))}
          </div>
        </div>
      ) : (
        <div className="space-y-4">
          {hoaDonMangVeList.length === 0 ? (
            <div className="card text-center py-12">
              <p className="text-gray-500">Không có hóa đơn nào chờ duyệt</p>
            </div>
          ) : (
            hoaDonMangVeList.map((hoaDon) => (
              <div key={hoaDon.id || hoaDon.maHoaDon} className="card">
                <div className="flex items-start justify-between mb-4">
                  <div>
                    <h3 className="text-lg font-bold text-gray-900">
                      Hóa đơn mang về #{hoaDon.maHoaDon || hoaDon.id}
                    </h3>
                    <p className="text-sm text-gray-600">
                      Khách hàng: {hoaDon.maKhachHang}
                    </p>
                  </div>
                  <span className={`px-3 py-1 rounded-full text-sm font-medium ${
                    hoaDon.trangThai === -1 
                      ? 'bg-orange-100 text-orange-800' 
                      : 'bg-yellow-100 text-yellow-800'
                  }`}>
                    {hoaDon.trangThai === -1 ? 'Chưa xác nhận' : 'Chờ duyệt'}
                  </span>
                </div>

                <div className="grid grid-cols-2 gap-4 mb-4 text-sm">
                  <div>
                    <span className="text-gray-600">Giờ đặt: </span>
                    <span className="font-medium">{formatDateTime(hoaDon.gioVao)}</span>
                  </div>
                  <div>
                    <span className="text-gray-600">Giờ ra: </span>
                    <span className="font-medium">{formatDateTime(hoaDon.gioRa)}</span>
                  </div>
                  {hoaDon.ghiChu && (
                    <div className="col-span-2">
                      <span className="text-gray-600">Ghi chú: </span>
                      <span className="font-medium">{hoaDon.ghiChu}</span>
                    </div>
                  )}
                </div>

                {hoaDon.chiTiet && hoaDon.chiTiet.length > 0 && (
                  <div className="mb-4">
                    <h4 className="font-medium text-gray-900 mb-2">Chi tiết:</h4>
                    <div className="space-y-2">
                      {hoaDon.chiTiet.map((ct) => (
                        <div
                          key={ct.id || ct.maHDCT}
                          className="flex items-center gap-3 text-sm bg-gray-50 p-2 rounded"
                        >
                          {ct.hangHoa && (
                            <ImageDisplay
                              item={ct.hangHoa}
                              type="hanghoa"
                              alt={ct.tenHangHoa}
                              className="w-16 h-16 rounded object-cover flex-shrink-0"
                              fallback={
                                <div className="w-16 h-16 rounded bg-gray-200 flex items-center justify-center flex-shrink-0">
                                  <span className="text-gray-400 text-xs">No img</span>
                                </div>
                              }
                            />
                          )}
                          <div className="flex-1">
                            <div className="font-medium">
                              {ct.tenHangHoa || `Hàng hóa #${ct.maHangHoa}`} x{ct.soLuong}
                            </div>
                            {ct.ghiChu && (
                              <div className="text-gray-500 text-xs">{ct.ghiChu}</div>
                            )}
                          </div>
                          <span className="font-medium">
                            {formatCurrency((ct.soLuong || 0) * (ct.giaTien || 0))}
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                <div className="flex justify-between items-center pt-4 border-t">
                  <span className="text-gray-600">Tổng tiền:</span>
                  <span className="text-xl font-bold text-primary-600">
                    {formatCurrency(hoaDon.tongTien || 0)}
                  </span>
                </div>

                <div className="flex space-x-3 mt-4">
                  <button
                    onClick={() => handleDuyet(hoaDon, true)}
                    className="flex-1 btn-primary flex items-center justify-center space-x-2"
                  >
                    <CheckCircle className="w-5 h-5" />
                    <span>Duyệt</span>
                  </button>
                  <button
                    onClick={() => handleDuyet(hoaDon, false)}
                    className="flex-1 btn-danger flex items-center justify-center space-x-2"
                  >
                    <XCircle className="w-5 h-5" />
                    <span>Từ chối</span>
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
};

export default DuyetHoaDon;
