import { useEffect, useState, useMemo, useCallback } from 'react';
import { getAllFromRealtimeDB, REALTIME_NODES } from '../utils/realtimeDB';
import { parseRealtimeDateTime } from '../utils/dateUtils';
import { DollarSign, RefreshCw, X } from 'lucide-react';
import { format } from 'date-fns';
import { vi } from 'date-fns/locale';
import ImageDisplay from '../components/ImageDisplay';
import StatusBadge from '../components/StatusBadge';
import ExportPanel from '../components/ExportPanel';
import AdvancedSearchPanel from '../components/AdvancedSearchPanel';

const QuanLyHoaDon = () => {
  const [hoaDonList, setHoaDonList] = useState([]);
  const [filteredList, setFilteredList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedSearchFields, setSelectedSearchFields] = useState(['maHoaDon', 'maBan', 'maKhachHang']);
  const [showAdvancedFilters, setShowAdvancedFilters] = useState(false);
  const [filters, setFilters] = useState({
    trangThai: '',
    maBan: '',
    ngayGioVao: { from: '', to: '' },
    tongTien: { min: '', max: '' }
  });
  const [sortBy, setSortBy] = useState('gioVao:desc');

  // Search Fields
  const searchFields = [
    { key: 'maHoaDon', label: 'Mã HĐ' },
    { key: 'maBan', label: 'Số bàn' },
    { key: 'maKhachHang', label: 'Khách hàng' },
    { key: 'ghiChu', label: 'Ghi chú' }
  ];

  // Load data
  useEffect(() => {
    loadHoaDon();
  }, []);

  const loadHoaDon = async () => {
    setLoading(true);
    try {
      const [hoaDonArray, hoaDonChiTietArray, hangHoaList] = await Promise.all([
        getAllFromRealtimeDB(REALTIME_NODES.HOA_DON, true),
        getAllFromRealtimeDB(REALTIME_NODES.HOA_DON_CHI_TIET, true),
        getAllFromRealtimeDB(REALTIME_NODES.HANG_HOA, true)
      ]);
      
      const data = hoaDonArray.map((hoaDon) => {
        const maHoaDon = hoaDon.maHoaDon || parseInt(hoaDon.id);
        
        const chiTiet = hoaDonChiTietArray
          .filter(ct => {
            const ctMaHoaDon = ct.maHoaDon;
            return ctMaHoaDon === maHoaDon || ctMaHoaDon === parseInt(hoaDon.id);
          })
          .map(ct => {
            const hangHoa = hangHoaList.find(hh => 
              (hh.maHangHoa || parseInt(hh.id)) === ct.maHangHoa
            );
            return {
              ...ct,
              tenHangHoa: hangHoa?.tenHangHoa || `Hàng hóa #${ct.maHangHoa}`,
              hangHoa: hangHoa,
            };
          });
        
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

      setHoaDonList(data);
    } catch (error) {
      console.error('Error loading hoa don:', error);
    } finally {
      setLoading(false);
    }
  };

  // Apply filters and search - Using useEffect to force re-render
  useEffect(() => {
    let result = [...hoaDonList];

    // 1. Apply Search
    if (searchTerm) {
      const searchLower = searchTerm.toLowerCase();
      result = result.filter(item => {
        return selectedSearchFields.some(field => {
          const value = String(item[field] || '').toLowerCase();
          return value.includes(searchLower);
        });
      });
    }

    // 2. Apply Filters
    if (filters.trangThai !== '') {
      result = result.filter(item => {
        const itemStatus = String(item.trangThai);
        const filterStatus = filters.trangThai;
        return itemStatus === filterStatus;
      });
    }

    if (filters.maBan !== '') {
      result = result.filter(item => String(item.maBan) === filters.maBan);
    }

    if (filters.ngayGioVao.from || filters.ngayGioVao.to) {
      result = result.filter(item => {
        const itemDate = parseRealtimeDateTime(item.gioVao);
        if (!itemDate) return false;
        
        const fromDate = filters.ngayGioVao.from ? new Date(filters.ngayGioVao.from) : null;
        const toDate = filters.ngayGioVao.to ? new Date(filters.ngayGioVao.to) : null;
        
        if (fromDate && toDate) {
          fromDate.setHours(0, 0, 0, 0);
          toDate.setHours(23, 59, 59, 999);
          return itemDate >= fromDate && itemDate <= toDate;
        } else if (fromDate) {
          fromDate.setHours(0, 0, 0, 0);
          return itemDate >= fromDate;
        } else if (toDate) {
          toDate.setHours(23, 59, 59, 999);
          return itemDate <= toDate;
        }
        return true;
      });
    }

    if (filters.tongTien.min !== '' || filters.tongTien.max !== '') {
      result = result.filter(item => {
        const min = filters.tongTien.min !== '' ? parseFloat(filters.tongTien.min) : 0;
        const max = filters.tongTien.max !== '' ? parseFloat(filters.tongTien.max) : Infinity;
        return item.tongTien >= min && item.tongTien <= max;
      });
    }

    // 3. Apply Sort
    const [field, direction] = sortBy.split(':');
    result.sort((a, b) => {
      let aValue, bValue;
      
      switch (field) {
        case 'gioVao':
          aValue = parseRealtimeDateTime(a.gioVao) || new Date(0);
          bValue = parseRealtimeDateTime(b.gioVao) || new Date(0);
          break;
        case 'gioRa':
          aValue = parseRealtimeDateTime(a.gioRa) || new Date(0);
          bValue = parseRealtimeDateTime(b.gioRa) || new Date(0);
          break;
        case 'tongTien':
          aValue = a.tongTien || 0;
          bValue = b.tongTien || 0;
          break;
        case 'maBan':
          aValue = a.maBan || 0;
          bValue = b.maBan || 0;
          break;
        default:
          aValue = a.maHoaDon || 0;
          bValue = b.maHoaDon || 0;
      }

      if (direction === 'asc') {
        return aValue > bValue ? 1 : -1;
      } else {
        return aValue < bValue ? 1 : -1;
      }
    });

    setFilteredList(result);
  }, [hoaDonList, searchTerm, selectedSearchFields, filters, sortBy]);

  // Handle search
  const handleAdvancedSearch = useCallback((term, selectedFields) => {
    setSearchTerm(term || '');
    setSelectedSearchFields(selectedFields);
  }, []);

  // Handle filter change
  const handleFilterChange = useCallback((key, value) => {
    setFilters(prev => ({
      ...prev,
      [key]: value
    }));
  }, []);

  // Reset filters
  const handleResetFilters = useCallback(() => {
    setFilters({
      trangThai: '',
      maBan: '',
      ngayGioVao: { from: '', to: '' },
      tongTien: { min: '', max: '' }
    });
    setSearchTerm('');
    setSelectedSearchFields(['maHoaDon', 'maBan', 'maKhachHang']);
  }, []);

  // Format functions
  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('vi-VN').format(amount) + 'đ';
  };

  const formatDateTime = (dateTimeStr) => {
    if (!dateTimeStr) return '-';
    const date = parseRealtimeDateTime(dateTimeStr);
    if (!date) return '-';
    return format(date, 'dd/MM/yyyy HH:mm', { locale: vi });
  };

  // Get unique ban list for filter
  const banOptions = useMemo(() => {
    return [...new Set(hoaDonList.map(hd => hd.maBan))]
      .filter(Boolean)
      .sort((a, b) => a - b)
      .map(maBan => ({
        value: maBan.toString(),
        label: `Bàn ${maBan}`
      }));
  }, [hoaDonList]);

  // Filter options
  const filterOptions = [
    {
      key: 'trangThai',
      label: 'Trạng thái',
      type: 'select',
      value: filters.trangThai,
      options: [
        { value: '0', label: 'Chưa thanh toán' },
        { value: '1', label: 'Đã thanh toán' },
        { value: '2', label: 'Đã duyệt' },
        { value: '3', label: 'Đã hủy' }
      ]
    },
    {
      key: 'maBan',
      label: 'Bàn',
      type: 'select',
      value: filters.maBan,
      options: banOptions
    },
    {
      key: 'ngayGioVao',
      label: 'Thời gian vào',
      type: 'daterange',
      value: filters.ngayGioVao
    },
    {
      key: 'tongTien',
      label: 'Tổng tiền (VND)',
      type: 'numberrange',
      value: filters.tongTien,
      placeholder: { min: 'Từ', max: 'Đến' }
    }
  ];

  const sortOptions = [
    { value: 'gioVao', label: 'Thời gian vào' },
    { value: 'gioRa', label: 'Thời gian ra' },
    { value: 'tongTien', label: 'Tổng tiền' },
    { value: 'maBan', label: 'Số bàn' }
  ];

  // Quick Filters
  const today = new Date().toISOString().split('T')[0];
  const quickFilters = [
    {
      label: 'Hôm nay',
      values: {
        ngayGioVao: { from: today, to: today }
      }
    },
    {
      label: 'Chưa thanh toán',
      values: {
        trangThai: '0'
      }
    },
    {
      label: 'Hóa đơn cao (>500k)',
      values: {
        tongTien: { min: '500000', max: '' }
      }
    }
  ];

  // Export Columns
  const exportColumns = [
    { key: 'maHoaDon', header: 'Mã HĐ', accessor: (row) => row.maHoaDon || row.id },
    { key: 'maBan', header: 'Bàn' },
    { key: 'maKhachHang', header: 'Khách hàng', accessor: (row) => row.maKhachHang || 'Khách vãng lai' },
    { 
      key: 'trangThai', 
      header: 'Trạng thái',
      accessor: (row) => {
        const status = ['Chưa TT', 'Đã TT', 'Đã duyệt', 'Đã hủy'];
        return status[row.trangThai] || 'N/A';
      }
    },
    { key: 'gioVao', header: 'Giờ vào', accessor: (row) => formatDateTime(row.gioVao) },
    { key: 'gioRa', header: 'Giờ ra', accessor: (row) => formatDateTime(row.gioRa) },
    { key: 'tongTien', header: 'Tổng tiền', accessor: (row) => formatCurrency(row.tongTien || 0) },
    { key: 'ghiChu', header: 'Ghi chú', accessor: (row) => row.ghiChu || '' }
  ];

  // Calculate statistics from FILTERED list
  const statistics = useMemo(() => {
    const totalFiltered = filteredList.length;
    const paidCount = filteredList.filter(hd => hd.trangThai === 1).length;
    const unpaidCount = filteredList.filter(hd => hd.trangThai === 0).length;
    const totalRevenue = filteredList
      .filter(hd => hd.trangThai === 1)
      .reduce((sum, hd) => sum + (hd.tongTien || 0), 0);
    
    return { totalFiltered, paidCount, unpaidCount, totalRevenue };
  }, [filteredList]);

  // Check if filters are active
  const hasActiveFilters = searchTerm || 
    filters.trangThai !== '' || 
    filters.maBan !== '' || 
    filters.ngayGioVao.from || 
    filters.ngayGioVao.to || 
    filters.tongTien.min !== '' || 
    filters.tongTien.max !== '';

  return (
    <div className="p-4 sm:p-6 lg:p-8 max-w-7xl mx-auto">
      {/* Compact Header */}
      <div className="mb-4 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Quản lý Hóa đơn</h1>
          <p className="text-sm text-gray-500 mt-1">
            {hasActiveFilters ? `${filteredList.length} / ${hoaDonList.length} hóa đơn` : `${hoaDonList.length} hóa đơn`}
          </p>
        </div>
        <button
          onClick={loadHoaDon}
          disabled={loading}
          className="flex items-center gap-2 px-3 py-1.5 text-sm bg-white border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 disabled:bg-gray-100 disabled:cursor-not-allowed transition-colors shadow-sm"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          Làm mới
        </button>
      </div>

      {/* Ultra Compact Search Bar */}
      <div className="mb-3 bg-white rounded-lg border border-gray-200 p-2">
        <div className="flex flex-wrap items-center gap-2">
          {/* Search */}
          <div className="flex-1 min-w-[200px]">
            <AdvancedSearchPanel
              searchFields={searchFields}
              onSearch={handleAdvancedSearch}
              placeholder="🔍 Tìm HĐ, bàn, KH..."
              debounceMs={300}
              storageKey="quan_ly_hoa_don"
            />
          </div>
          
          {/* Quick Status Filter */}
          <select
            value={filters.trangThai}
            onChange={(e) => handleFilterChange('trangThai', e.target.value)}
            className="px-2 py-1.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">Tất cả trạng thái</option>
            <option value="0">Chưa TT</option>
            <option value="1">Đã TT</option>
            <option value="2">Đã duyệt</option>
            <option value="3">Đã hủy</option>
          </select>
          
          {/* Quick Table Filter */}
          <select
            value={filters.maBan}
            onChange={(e) => handleFilterChange('maBan', e.target.value)}
            className="px-2 py-1.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">Tất cả bàn</option>
            {banOptions.map(opt => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>
          
          {/* Sort */}
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value)}
            className="px-2 py-1.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="gioVao:desc">🕐 Mới nhất</option>
            <option value="gioVao:asc">🕐 Cũ nhất</option>
            <option value="tongTien:desc">💰 Giá cao</option>
            <option value="tongTien:asc">💰 Giá thấp</option>
            <option value="maBan:asc">🪑 Theo bàn</option>
          </select>
          
          {/* Advanced Filters Toggle */}
          <button
            onClick={() => setShowAdvancedFilters(!showAdvancedFilters)}
            className={`px-3 py-1.5 text-sm rounded-lg transition-colors ${
              showAdvancedFilters || filters.ngayGioVao.from || filters.ngayGioVao.to || filters.tongTien.min || filters.tongTien.max
                ? 'bg-blue-600 text-white'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
            title="Bộ lọc nâng cao"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4" />
            </svg>
          </button>
          
          {/* Export */}
          <ExportPanel
            data={filteredList}
            columns={exportColumns}
            filename={`hoa-don-${new Date().toISOString().split('T')[0]}`}
            title="Danh sách Hóa đơn"
          />
          
          {/* Reset */}
          {hasActiveFilters && (
            <button
              onClick={handleResetFilters}
              className="px-3 py-1.5 text-sm bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors"
              title="Xóa bộ lọc"
            >
              <X className="w-4 h-4" />
            </button>
          )}
        </div>
      </div>

      {/* Advanced Filters Panel (Collapsible) */}
      {showAdvancedFilters && (
        <div className="mb-3 bg-white rounded-lg border border-gray-200 p-3 space-y-3">
          {/* Quick Filter Chips */}
          <div className="flex flex-wrap gap-2">
            <span className="text-xs text-gray-500 font-medium">Lọc nhanh:</span>
            {quickFilters.map((qf, index) => (
              <button
                key={index}
                onClick={() => {
                  Object.entries(qf.values).forEach(([key, value]) => {
                    handleFilterChange(key, value);
                  });
                }}
                className="px-2 py-1 text-xs bg-yellow-50 text-yellow-700 border border-yellow-200 rounded hover:bg-yellow-100 transition-colors"
              >
                {qf.label}
              </button>
            ))}
          </div>
          
          {/* Date Range & Amount Range */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-3">
            {/* Date Range */}
            <div>
              <label className="text-xs text-gray-600 mb-1 block">Thời gian vào</label>
              <div className="flex items-center gap-2">
                <input
                  type="date"
                  value={filters.ngayGioVao.from || ''}
                  onChange={(e) => handleFilterChange('ngayGioVao', { ...filters.ngayGioVao, from: e.target.value })}
                  className="flex-1 px-2 py-1.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                <span className="text-gray-400">-</span>
                <input
                  type="date"
                  value={filters.ngayGioVao.to || ''}
                  onChange={(e) => handleFilterChange('ngayGioVao', { ...filters.ngayGioVao, to: e.target.value })}
                  className="flex-1 px-2 py-1.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>
            
            {/* Amount Range */}
            <div>
              <label className="text-xs text-gray-600 mb-1 block">Tổng tiền (VND)</label>
              <div className="flex items-center gap-2">
                <input
                  type="number"
                  value={filters.tongTien.min || ''}
                  onChange={(e) => handleFilterChange('tongTien', { ...filters.tongTien, min: e.target.value })}
                  placeholder="Từ"
                  className="flex-1 px-2 py-1.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                <span className="text-gray-400">-</span>
                <input
                  type="number"
                  value={filters.tongTien.max || ''}
                  onChange={(e) => handleFilterChange('tongTien', { ...filters.tongTien, max: e.target.value })}
                  placeholder="Đến"
                  className="flex-1 px-2 py-1.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Compact Statistics */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 mb-4">
        <div className="bg-white p-3 rounded-lg border border-gray-200">
          <div className="text-xs text-gray-500 mb-1">
            {hasActiveFilters ? 'Đã lọc' : 'Tổng'}
          </div>
          <div className="text-xl font-bold text-gray-900">{statistics.totalFiltered}</div>
        </div>
        <div className="bg-green-50 p-3 rounded-lg border border-green-200">
          <div className="text-xs text-green-700 mb-1 flex items-center gap-1">
            <DollarSign className="w-3 h-3" />
            Đã TT
          </div>
          <div className="text-xl font-bold text-green-900">
            {statistics.paidCount}
          </div>
        </div>
        <div className="bg-orange-50 p-3 rounded-lg border border-orange-200">
          <div className="text-xs text-orange-700 mb-1">Chưa TT</div>
          <div className="text-xl font-bold text-orange-900">
            {statistics.unpaidCount}
          </div>
        </div>
        <div className="bg-blue-50 p-3 rounded-lg border border-blue-200">
          <div className="text-xs text-blue-700 mb-1">Doanh thu</div>
          <div className="text-base font-bold text-blue-900">
            {formatCurrency(statistics.totalRevenue)}
          </div>
        </div>
      </div>

      {/* Invoice List */}
      {loading ? (
        <div className="space-y-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="bg-white rounded-lg border border-gray-200 p-3">
              <div className="animate-pulse space-y-2">
                <div className="h-4 bg-gray-200 rounded w-1/4"></div>
                <div className="h-3 bg-gray-200 rounded w-1/2"></div>
                <div className="h-3 bg-gray-200 rounded w-3/4"></div>
              </div>
            </div>
          ))}
        </div>
      ) : filteredList.length === 0 ? (
        <div className="bg-white rounded-lg border border-gray-200 text-center py-8">
          <div className="text-gray-400 mb-2">
            <svg className="w-12 h-12 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
          </div>
          <p className="text-sm text-gray-500 font-medium">
            {hasActiveFilters ? 'Không tìm thấy kết quả' : 'Chưa có hóa đơn'}
          </p>
          {hasActiveFilters && (
            <button
              onClick={handleResetFilters}
              className="mt-3 text-xs text-blue-600 hover:text-blue-700 hover:underline"
            >
              Xóa bộ lọc
            </button>
          )}
        </div>
      ) : (
        <div key={`invoice-list-${filteredList.length}-${filters.trangThai}-${filters.maBan}`} className="space-y-3">
          {filteredList.map((hoaDon, index) => (
            <div key={`hd-${hoaDon.id}-${hoaDon.maHoaDon}-${index}`} className="bg-white rounded-lg border border-gray-200 p-3 hover:shadow-md transition-shadow">
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-2">
                  <div className="flex-shrink-0 w-6 h-6 bg-blue-100 text-blue-700 rounded-full flex items-center justify-center font-bold text-xs">
                    {index + 1}
                  </div>
                  <div>
                    <h3 className="text-base font-bold text-gray-900">
                      HD #{hoaDon.maHoaDon || hoaDon.id}
                    </h3>
                    <p className="text-xs text-gray-500">
                      Bàn {hoaDon.maBan} • {hoaDon.maKhachHang || 'Vãng lai'}
                    </p>
                  </div>
                </div>
                <StatusBadge type="hoadon" value={hoaDon.trangThai} />
              </div>

              <div className="grid grid-cols-2 gap-2 mb-2 text-xs">
                <div>
                  <span className="text-gray-500">Vào: </span>
                  <span className="font-medium text-gray-700">{formatDateTime(hoaDon.gioVao)}</span>
                </div>
                <div>
                  <span className="text-gray-500">Ra: </span>
                  <span className="font-medium text-gray-700">{formatDateTime(hoaDon.gioRa)}</span>
                </div>
                {hoaDon.ghiChu && (
                  <div className="col-span-2">
                    <span className="text-gray-500">Ghi chú: </span>
                    <span className="text-gray-700 italic">{hoaDon.ghiChu}</span>
                  </div>
                )}
              </div>

              {hoaDon.chiTiet && hoaDon.chiTiet.length > 0 && (
                <div className="mb-2">
                  <h4 className="text-xs font-medium text-gray-600 mb-1.5">
                    Chi tiết ({hoaDon.chiTiet.length} món)
                  </h4>
                  <div className="space-y-1.5">
                    {hoaDon.chiTiet.map((ct, ctIndex) => (
                      <div
                        key={`ct-${hoaDon.id}-${ct.id || ct.maHDCT}-${ct.maHangHoa}-${ctIndex}`}
                        className="flex items-center gap-2 text-xs bg-gray-50 p-2 rounded hover:bg-gray-100 transition-colors"
                      >
                        {ct.hangHoa && (
                          <ImageDisplay
                            item={ct.hangHoa}
                            type="hanghoa"
                            alt={ct.tenHangHoa}
                            className="w-12 h-12 rounded object-cover flex-shrink-0 border border-gray-200"
                            fallback={
                              <div className="w-12 h-12 rounded bg-gray-200 flex items-center justify-center flex-shrink-0">
                                <span className="text-gray-400 text-[10px]">?</span>
                              </div>
                            }
                          />
                        )}
                        <div className="flex-1 min-w-0">
                          <div className="font-medium text-gray-900 truncate">
                            {ct.tenHangHoa || `#${ct.maHangHoa}`}
                          </div>
                          <div className="text-gray-500">
                            {ct.soLuong} × {formatCurrency(ct.giaTien || 0)}
                          </div>
                        </div>
                        <span className="font-semibold text-gray-900 text-xs">
                          {formatCurrency((ct.soLuong || 0) * (ct.giaTien || 0))}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              <div className="flex justify-between items-center pt-2 border-t border-gray-200">
                <span className="text-xs text-gray-600">Tổng:</span>
                <span className="text-base font-bold text-blue-600">
                  {formatCurrency(hoaDon.tongTien || 0)}
                </span>
              </div>
            </div>
          ))}
          
          {/* End of list indicator */}
          {filteredList.length > 5 && (
            <div className="text-center py-4 border-t border-dashed border-gray-300">
              <p className="text-xs text-gray-500">
                ✓ Hiển thị {filteredList.length} hóa đơn
                {hasActiveFilters && ` (lọc từ ${hoaDonList.length})`}
              </p>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default QuanLyHoaDon;
