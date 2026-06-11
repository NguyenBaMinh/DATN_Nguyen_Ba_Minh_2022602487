import { useEffect, useState, useRef } from 'react';
import { getAllFromRealtimeDB, setRealtimeDB, queryRealtimeDB, REALTIME_NODES } from '../utils/realtimeDB';
import { parseRealtimeDateTime, formatRealtimeDateTime } from '../utils/dateUtils';
import { CheckCircle, XCircle, Clock, Search } from 'lucide-react';
import toast from 'react-hot-toast';
import { format } from 'date-fns';
import { vi } from 'date-fns/locale';
import FilterPanel from '../components/FilterPanel';
import StatusBadge from '../components/StatusBadge';
import SortPanel from '../components/SortPanel';

const DuyetDatBan = () => {
  const [datBanList, setDatBanList] = useState([]);
  const [filteredList, setFilteredList] = useState([]);
  const [banList, setBanList] = useState([]);
  const [nguoiDungList, setNguoiDungList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [filters, setFilters] = useState({
    trangThai: '',
    maBan: '',
    ngayGioSuDung: { from: '', to: '' },
  });
  const [sortBy, setSortBy] = useState('ngayGioDat:desc');
  
  // Refs để lưu giá trị mới nhất của banList và nguoiDungList
  const banListRef = useRef([]);
  const nguoiDungListRef = useRef([]);

  useEffect(() => {
    const loadInitialData = async () => {
      setLoading(true);
      try {
        // Load dependencies first
        const [banData, nguoiDungData] = await Promise.all([
          getAllFromRealtimeDB(REALTIME_NODES.BAN, true),
          getAllFromRealtimeDB(REALTIME_NODES.NGUOI_DUNG, false)
        ]);
        
        const filteredBanList = banData.filter(item => item !== null);
        const filteredNguoiDungList = nguoiDungData.filter(item => item !== null);
        
        setBanList(filteredBanList);
        setNguoiDungList(filteredNguoiDungList);
        
        // Update refs
        banListRef.current = filteredBanList;
        nguoiDungListRef.current = filteredNguoiDungList;

        // Then load dat ban with dependencies available
        await loadDatBan(filteredBanList, filteredNguoiDungList);
      } catch (error) {
        console.error('Error loading initial data:', error);
        toast.error('Lỗi khi tải dữ liệu');
        setLoading(false);
      }
    };

    loadInitialData();
  }, []);

  const loadBan = async () => {
    try {
      const data = await getAllFromRealtimeDB(REALTIME_NODES.BAN, true);
      const filtered = data.filter(item => item !== null);
      setBanList(filtered);
      banListRef.current = filtered;
    } catch (error) {
      console.error('Error loading ban:', error);
    }
  };

  const loadNguoiDung = async () => {
    try {
      const data = await getAllFromRealtimeDB(REALTIME_NODES.NGUOI_DUNG, false);
      const filtered = data.filter(item => item !== null);
      setNguoiDungList(filtered);
      nguoiDungListRef.current = filtered;
    } catch (error) {
      console.error('Error loading nguoi dung:', error);
    }
  };

  const loadDatBan = async (banListData = null, nguoiDungListData = null) => {
    try {
      // Sử dụng dữ liệu được truyền vào hoặc lấy từ ref (luôn có giá trị mới nhất)
      const currentBanList = banListData || banListRef.current;
      const currentNguoiDungList = nguoiDungListData || nguoiDungListRef.current;

      // Lấy tất cả đặt bàn (không chỉ chờ duyệt)
      const allDatBan = await getAllFromRealtimeDB(REALTIME_NODES.DAT_BAN, true);

      // Enrich với thông tin bàn và khách hàng
      const enrichedData = allDatBan
        .filter(db => db !== null)
        .map(datBan => {
          const ban = currentBanList.find(b => (b.maBan || parseInt(b.id)) === datBan.maBan);
          const khachHang = currentNguoiDungList.find(nd => (nd.maNguoiDung || nd.id) === datBan.maKhachHang);

          return {
            ...datBan,
            id: datBan.maDatBan || datBan.id,
            tenBan: ban ? `Bàn ${datBan.maBan}` : `Bàn ${datBan.maBan}`,
            tenKhachHang: khachHang?.hoVaTen || datBan.maKhachHang,
            ngayGioDatParsed: parseRealtimeDateTime(datBan.ngayGioDat),
            ngayGioSuDungParsed: parseRealtimeDateTime(datBan.ngayGioSuDung),
          };
        });

      setDatBanList(enrichedData);
    } catch (error) {
      console.error('Error loading dat ban:', error);
      toast.error('Lỗi khi tải danh sách đặt bàn');
    } finally {
      setLoading(false);
    }
  };

  // Filter và Sort dữ liệu
  useEffect(() => {
    let result = [...datBanList];

    // Apply filters
    if (filters.trangThai !== '') {
      result = result.filter(item => item.trangThai === parseInt(filters.trangThai));
    }

    if (filters.maBan !== '') {
      result = result.filter(item => item.maBan === parseInt(filters.maBan));
    }

    if (filters.ngayGioSuDung.from || filters.ngayGioSuDung.to) {
      result = result.filter(item => {
        const itemDate = item.ngayGioSuDungParsed;
        if (!itemDate) return false;
        
        const fromDate = filters.ngayGioSuDung.from ? new Date(filters.ngayGioSuDung.from) : null;
        const toDate = filters.ngayGioSuDung.to ? new Date(filters.ngayGioSuDung.to) : null;
        
        if (fromDate && toDate) {
          return itemDate >= fromDate && itemDate <= toDate;
        } else if (fromDate) {
          return itemDate >= fromDate;
        } else if (toDate) {
          return itemDate <= toDate;
        }
        return true;
      });
    }

    // Apply search
    if (searchTerm) {
      const searchLower = searchTerm.toLowerCase();
      result = result.filter(item =>
        item.tenBan?.toLowerCase().includes(searchLower) ||
        item.tenKhachHang?.toLowerCase().includes(searchLower) ||
        item.maKhachHang?.toLowerCase().includes(searchLower) ||
        item.ghiChu?.toLowerCase().includes(searchLower)
      );
    }

    // Apply sort
    const [field, direction] = sortBy.split(':');
    result.sort((a, b) => {
      let aValue, bValue;
      
      switch (field) {
        case 'ngayGioDat':
          aValue = a.ngayGioDatParsed || new Date(0);
          bValue = b.ngayGioDatParsed || new Date(0);
          break;
        case 'ngayGioSuDung':
          aValue = a.ngayGioSuDungParsed || new Date(0);
          bValue = b.ngayGioSuDungParsed || new Date(0);
          break;
        case 'maBan':
          aValue = a.maBan || 0;
          bValue = b.maBan || 0;
          break;
        case 'trangThai':
          aValue = a.trangThai || 0;
          bValue = b.trangThai || 0;
          break;
        default:
          aValue = a.maDatBan || 0;
          bValue = b.maDatBan || 0;
      }

      if (direction === 'asc') {
        return aValue > bValue ? 1 : -1;
      } else {
        return aValue < bValue ? 1 : -1;
      }
    });

    setFilteredList(result);
  }, [datBanList, filters, searchTerm, sortBy]);

  const handleFilterChange = (key, value) => {
    setFilters(prev => ({
      ...prev,
      [key]: value
    }));
  };

  const handleResetFilters = () => {
    setFilters({
      trangThai: '',
      maBan: '',
      ngayGioSuDung: { from: '', to: '' },
    });
    setSearchTerm('');
  };

  const handleDuyet = async (datBan, approved = true) => {
    try {
      const maDatBan = datBan.maDatBan || datBan.id;
      const now = Date.now();

      // Cập nhật trạng thái đặt bàn
      const newTrangThai = approved ? 1 : -1; // 1: Đã duyệt, -1: Từ chối
      
      await setRealtimeDB(`${REALTIME_NODES.DAT_BAN}/${maDatBan}`, {
        ...datBan,
        trangThai: newTrangThai,
        lastModified: now,
      });

      // Nếu được duyệt, tạo hóa đơn sẵn và gửi thông báo
      if (approved) {
        // Tạo hóa đơn sẵn cho khách hàng
        await taoHoaDonTuDatBan(datBan);

        // Gửi thông báo cho khách hàng
        await guiThongBao(datBan, true);
      } else {
        // Gửi thông báo từ chối
        await guiThongBao(datBan, false);
      }

      toast.success(approved ? 'Duyệt đặt bàn thành công và đã tạo hóa đơn' : 'Từ chối đặt bàn thành công');
      loadDatBan();
    } catch (error) {
      console.error('Error updating dat ban:', error);
      toast.error('Lỗi khi cập nhật đặt bàn');
    }
  };

  /**
   * Tạo hóa đơn sẵn khi duyệt đặt bàn
   * Hóa đơn sẽ được tạo với thông tin từ DatBan
   * 
   * LOGIC:
   * - Tạo HoaDon với maBan và maKhachHang từ DatBan
   * - gioVao = ngayGioSuDung từ DatBan (thời gian khách đặt sử dụng bàn)
   * - gioRa = gioVao (ban đầu, sẽ cập nhật khi thanh toán)
   * - trangThai = 0 (CHUA_THANH_TOAN)
   */
  const taoHoaDonTuDatBan = async (datBan) => {
    try {
      // Lấy tất cả hóa đơn để tìm maHoaDon cao nhất
      const allHoaDon = await getAllFromRealtimeDB(REALTIME_NODES.HOA_DON, true);
      const maxMaHoaDon = allHoaDon.reduce((max, hd) => {
        if (!hd || hd === null) return max;
        const num = hd?.maHoaDon || parseInt(hd?.id) || 0;
        return num > max ? num : max;
      }, 0);
      const newMaHoaDon = maxMaHoaDon + 1;

      // Parse ngày giờ sử dụng từ DatBan
      // Format: "dd-MM-yyyy HH:mm:ss"
      const ngayGioSuDung = parseRealtimeDateTime(datBan.ngayGioSuDung);
      
      // Nếu không parse được, dùng thời gian hiện tại
      const gioVao = ngayGioSuDung || new Date();
      
      // Format lại theo format của HoaDon: "dd-MM-yyyy HH:mm:ss"
      const gioVaoStr = formatRealtimeDateTime(gioVao);

      // Tạo hóa đơn mới
      const hoaDon = {
        maHoaDon: newMaHoaDon,
        maBan: datBan.maBan,
        maKhachHang: datBan.maKhachHang,
        gioVao: gioVaoStr,
        gioRa: gioVaoStr, // Ban đầu gioRa = gioVao (sẽ cập nhật khi thanh toán)
        trangThai: 0, // CHUA_THANH_TOAN
        ghiChu: datBan.ghiChu || '',
        lastModified: Date.now(),
      };

      // Lưu hóa đơn vào Realtime Database (array, index = maHoaDon)
      await setRealtimeDB(`${REALTIME_NODES.HOA_DON}/${newMaHoaDon}`, hoaDon);
      
      console.log('Đã tạo hóa đơn từ đặt bàn:', hoaDon);
    } catch (error) {
      console.error('Error creating hoa don from dat ban:', error);
      toast.error('Lỗi khi tạo hóa đơn');
      throw error; // Re-throw để handleDuyet biết có lỗi
    }
  };

  const guiThongBao = async (datBan, approved) => {
    try {
      // Lấy tất cả thông báo để tìm maThongBao cao nhất
      const allThongBao = await getAllFromRealtimeDB(REALTIME_NODES.THONG_BAO, true);
      const maxMaThongBao = allThongBao.reduce((max, tb) => {
        const num = tb?.maThongBao || parseInt(tb?.id) || 0;
        return num > max ? num : max;
      }, 0);
      const newMaThongBao = maxMaThongBao + 1;

      const noiDung = approved
        ? `Đặt bàn của bạn đã được duyệt. Bàn ${datBan.maBan} đã được đặt cho bạn vào ${format(parseRealtimeDateTime(datBan.ngayGioSuDung), 'dd/MM/yyyy HH:mm', { locale: vi })}.`
        : `Đặt bàn của bạn đã bị từ chối. Bàn ${datBan.maBan} vào ${format(parseRealtimeDateTime(datBan.ngayGioSuDung), 'dd/MM/yyyy HH:mm', { locale: vi })} không thể đặt được.`;

      // Tạo thông báo mới
      const thongBao = {
        maThongBao: newMaThongBao,
        trangThai: 0, // Chưa xem
        noiDung: noiDung,
        ngayThongBao: format(new Date(), 'yyyy-MM-dd'),
        lastModified: Date.now(),
      };

      // Lưu thông báo vào Realtime Database (array)
      await setRealtimeDB(`${REALTIME_NODES.THONG_BAO}/${newMaThongBao}`, thongBao);
    } catch (error) {
      console.error('Error sending notification:', error);
      toast.error('Lỗi khi gửi thông báo');
    }
  };

  const formatDateTime = (dateTimeStr) => {
    if (!dateTimeStr) return '-';
    const date = parseRealtimeDateTime(dateTimeStr);
    if (!date) return '-';
    return format(date, 'dd/MM/yyyy HH:mm', { locale: vi });
  };

  // Prepare filter options
  const filterOptions = [
    {
      key: 'trangThai',
      label: 'Trạng thái',
      type: 'select',
      value: filters.trangThai,
      options: [
        { value: '0', label: 'Chờ duyệt' },
        { value: '1', label: 'Đã duyệt' },
        { value: '-1', label: 'Từ chối' },
        { value: '2', label: 'Đã sử dụng' },
        { value: '3', label: 'Đã hủy' },
      ]
    },
    {
      key: 'maBan',
      label: 'Bàn',
      type: 'select',
      value: filters.maBan,
      options: banList.map(ban => ({
        value: ban.maBan?.toString() || ban.id?.toString(),
        label: `Bàn ${ban.maBan || ban.id}`
      }))
    },
    {
      key: 'ngayGioSuDung',
      label: 'Ngày sử dụng',
      type: 'daterange',
      value: filters.ngayGioSuDung,
    }
  ];

  const sortOptions = [
    { value: 'ngayGioDat', label: 'Ngày đặt' },
    { value: 'ngayGioSuDung', label: 'Ngày sử dụng' },
    { value: 'maBan', label: 'Số bàn' },
    { value: 'trangThai', label: 'Trạng thái' },
  ];

  return (
    <div className="p-8">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Quản lý Đặt Bàn</h1>
        <p className="text-gray-600 mt-2">Duyệt và quản lý các yêu cầu đặt bàn từ khách hàng</p>
      </div>

      {/* Search */}
      <div className="mb-4">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
          <input
            type="text"
            placeholder="Tìm kiếm theo bàn, khách hàng, ghi chú..."
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
          <div className="text-sm text-gray-600">Tổng số</div>
          <div className="text-2xl font-bold text-gray-900">{datBanList.length}</div>
        </div>
        <div className="bg-yellow-50 p-4 rounded-lg shadow-sm border border-yellow-200">
          <div className="text-sm text-yellow-800">Chờ duyệt</div>
          <div className="text-2xl font-bold text-yellow-900">
            {datBanList.filter(d => d.trangThai === 0).length}
          </div>
        </div>
        <div className="bg-green-50 p-4 rounded-lg shadow-sm border border-green-200">
          <div className="text-sm text-green-800">Đã duyệt</div>
          <div className="text-2xl font-bold text-green-900">
            {datBanList.filter(d => d.trangThai === 1).length}
          </div>
        </div>
        <div className="bg-blue-50 p-4 rounded-lg shadow-sm border border-blue-200">
          <div className="text-sm text-blue-800">Đã hiển thị</div>
          <div className="text-2xl font-bold text-blue-900">{filteredList.length}</div>
        </div>
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
          {filteredList.length === 0 ? (
            <div className="card text-center py-12">
              <Clock className="w-16 h-16 mx-auto text-gray-400 mb-4" />
              <p className="text-gray-500 text-lg">Không tìm thấy đặt bàn nào</p>
            </div>
          ) : (
            filteredList.map((datBan) => (
              <div key={datBan.id || datBan.maDatBan} className="card hover:shadow-lg transition-shadow">
                <div className="flex items-start justify-between mb-4">
                  <div>
                    <h3 className="text-lg font-bold text-gray-900">
                      Đặt bàn #{datBan.maDatBan || datBan.id}
                    </h3>
                    <p className="text-sm text-gray-600">
                      Khách hàng: <span className="font-medium">{datBan.tenKhachHang}</span>
                    </p>
                  </div>
                  <StatusBadge type="datban" value={datBan.trangThai} />
                </div>

                <div className="grid grid-cols-2 gap-4 mb-4 text-sm">
                  <div>
                    <span className="text-gray-600">Bàn: </span>
                    <span className="font-medium">{datBan.tenBan}</span>
                  </div>
                  <div>
                    <span className="text-gray-600">Thời gian đặt: </span>
                    <span className="font-medium">{formatDateTime(datBan.ngayGioDat)}</span>
                  </div>
                  <div className="col-span-2">
                    <span className="text-gray-600">Thời gian sử dụng: </span>
                    <span className="font-medium">{formatDateTime(datBan.ngayGioSuDung)}</span>
                  </div>
                  {datBan.ghiChu && (
                    <div className="col-span-2">
                      <span className="text-gray-600">Ghi chú: </span>
                      <span className="font-medium">{datBan.ghiChu}</span>
                    </div>
                  )}
                </div>

                {datBan.trangThai === 0 && (
                  <div className="flex space-x-3 mt-4 pt-4 border-t border-gray-200">
                    <button
                      onClick={() => handleDuyet(datBan, true)}
                      className="flex-1 btn-primary flex items-center justify-center space-x-2 hover:scale-105 transition-transform"
                    >
                      <CheckCircle className="w-5 h-5" />
                      <span>Duyệt</span>
                    </button>
                    <button
                      onClick={() => handleDuyet(datBan, false)}
                      className="flex-1 btn-danger flex items-center justify-center space-x-2 hover:scale-105 transition-transform"
                    >
                      <XCircle className="w-5 h-5" />
                      <span>Từ chối</span>
                    </button>
                  </div>
                )}
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
};

export default DuyetDatBan;

