import { useEffect, useState } from 'react';
import { getAllFromRealtimeDB, REALTIME_NODES } from '../utils/realtimeDB';
import { parseRealtimeDateTime } from '../utils/dateUtils';
import { DollarSign, TrendingUp, TrendingDown, BarChart3, PieChart, Calendar, CalendarCheck } from 'lucide-react';
import { 
  format, 
  startOfDay, 
  endOfDay, 
  startOfMonth, 
  endOfMonth, 
  startOfYear, 
  endOfYear,
  subMonths,
  subDays,
  eachDayOfInterval,
  eachWeekOfInterval,
  startOfWeek
} from 'date-fns';
import { 
  BarChart, 
  Bar, 
  LineChart, 
  Line,
  PieChart as RechartsPieChart,
  Pie,
  Cell,
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  Legend, 
  ResponsiveContainer 
} from 'recharts';

const COLORS = ['#0ea5e9', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#14b8a6', '#f97316'];

const QuanLyDoanhThu = () => {
  const [doanhThu, setDoanhThu] = useState({
    ngay: 0,
    thang: 0,
    nam: 0,
    thangTruoc: 0,
    namTruoc: 0,
  });
  const [chartData, setChartData] = useState([]);
  const [compareChartData, setCompareChartData] = useState([]);
  const [dailyChartData, setDailyChartData] = useState([]);
  const [topProducts, setTopProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('monthly'); // 'monthly', 'compare', 'daily', 'products'
  const [selectedYear, setSelectedYear] = useState(() => new Date().getFullYear());
  const [selectedMonth, setSelectedMonth] = useState(() => new Date().getMonth() + 1); // Cho biểu đồ theo ngày

  useEffect(() => {
    loadDoanhThu();
  }, []);

  // Reload chart data khi năm hoặc tháng thay đổi
  useEffect(() => {
    if (!loading) {
      const reloadData = async () => {
        await Promise.all([
          loadChartData(),
          loadCompareChartData(),
          loadDailyChartData(),
          loadTopProducts(),
        ]);
      };
      reloadData();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedYear, selectedMonth]);

  const loadDoanhThu = async () => {
    try {
      const now = new Date();
      
      // Doanh thu ngày
      const startDay = startOfDay(now);
      const endDay = endOfDay(now);
      const doanhThuNgay = await calculateDoanhThu(startDay, endDay);
      
      // Doanh thu tháng
      const startMonth = startOfMonth(now);
      const endMonth = endOfMonth(now);
      const doanhThuThang = await calculateDoanhThu(startMonth, endMonth);
      
      // Doanh thu tháng trước
      const lastMonthStart = startOfMonth(subMonths(now, 1));
      const lastMonthEnd = endOfMonth(subMonths(now, 1));
      const doanhThuThangTruoc = await calculateDoanhThu(lastMonthStart, lastMonthEnd);
      
      // Doanh thu năm
      const startYear = startOfYear(now);
      const endYear = endOfYear(now);
      const doanhThuNam = await calculateDoanhThu(startYear, endYear);
      
      // Doanh thu năm trước
      const lastYearStart = startOfYear(subMonths(now, 12));
      const lastYearEnd = endOfYear(subMonths(now, 12));
      const doanhThuNamTruoc = await calculateDoanhThu(lastYearStart, lastYearEnd);
      
      setDoanhThu({
        ngay: doanhThuNgay,
        thang: doanhThuThang,
        nam: doanhThuNam,
        thangTruoc: doanhThuThangTruoc,
        namTruoc: doanhThuNamTruoc,
      });
      
      // Load các chart data
      await Promise.all([
        loadChartData(),
        loadCompareChartData(),
        loadDailyChartData(),
        loadTopProducts(),
      ]);
    } catch (error) {
      console.error('Error loading doanh thu:', error);
    } finally {
      setLoading(false);
    }
  };

  const calculateDoanhThu = async (startDate, endDate) => {
    try {
      // Load tất cả hóa đơn và chi tiết từ Realtime Database
      const [hoaDonArray, hoaDonChiTietArray] = await Promise.all([
        getAllFromRealtimeDB(REALTIME_NODES.HOA_DON, true),
        getAllFromRealtimeDB(REALTIME_NODES.HOA_DON_CHI_TIET, true),
      ]);
      
      // Lọc hóa đơn đã thanh toán (trangThai === 1)
      const hoaDonPaid = hoaDonArray.filter(hd => hd.trangThai === 1);
      
      let total = 0;
      
      for (const hoaDon of hoaDonPaid) {
        const gioVao = parseRealtimeDateTime(hoaDon.gioVao);
        
        if (gioVao && gioVao >= startDate && gioVao <= endDate) {
          // Lấy chi tiết hóa đơn theo maHoaDon
          const maHoaDon = hoaDon.maHoaDon || parseInt(hoaDon.id);
          const chiTiet = hoaDonChiTietArray.filter(ct => {
            const ctMaHoaDon = ct.maHoaDon;
            return ctMaHoaDon === maHoaDon || ctMaHoaDon === parseInt(hoaDon.id);
          });
          
          // Tính tổng tiền
          chiTiet.forEach(ct => {
            total += (ct.soLuong || 0) * (ct.giaTien || 0);
          });
        }
      }
      
      return total;
    } catch (error) {
      console.error('Error calculating doanh thu:', error);
      return 0;
    }
  };

  const loadChartData = async () => {
    try {
      const months = [];
      
      for (let i = 0; i < 12; i++) {
        const monthDate = new Date(selectedYear, i, 1);
        const start = startOfMonth(monthDate);
        const end = endOfMonth(monthDate);
        const revenue = await calculateDoanhThu(start, end);
        
        months.push({
          name: format(monthDate, 'MM/yyyy'),
          doanhThu: revenue,
        });
      }
      
      setChartData(months);
    } catch (error) {
      console.error('Error loading chart data:', error);
    }
  };

  const loadCompareChartData = async () => {
    try {
      const months = [];
      const now = new Date();
      const currentMonth = now.getMonth(); // 0-11
      const currentYear = now.getFullYear();
      
      // Xác định 6 tháng cuối cùng của năm được chọn
      let startMonthIndex = 0;
      let monthCount = 12;
      
      if (selectedYear === currentYear) {
        // Nếu là năm hiện tại, chỉ lấy từ tháng 1 đến tháng hiện tại
        monthCount = currentMonth + 1;
      }
      
      // Lấy 6 tháng gần nhất (hoặc tất cả nếu < 6 tháng)
      const monthsToShow = Math.min(6, monthCount);
      startMonthIndex = monthCount - monthsToShow;
      
      for (let i = startMonthIndex; i < monthCount; i++) {
        const monthDate = new Date(selectedYear, i, 1);
        const start = startOfMonth(monthDate);
        const end = endOfMonth(monthDate);
        const revenue = await calculateDoanhThu(start, end);
        
        // Tháng tương ứng năm trước
        const lastYearMonth = new Date(selectedYear - 1, i, 1);
        const lastYearStart = startOfMonth(lastYearMonth);
        const lastYearEnd = endOfMonth(lastYearMonth);
        const lastYearRevenue = await calculateDoanhThu(lastYearStart, lastYearEnd);
        
        months.push({
          name: format(monthDate, 'MM/yyyy'),
          [`Năm ${selectedYear}`]: revenue,
          [`Năm ${selectedYear - 1}`]: lastYearRevenue,
        });
      }
      
      setCompareChartData(months);
    } catch (error) {
      console.error('Error loading compare chart data:', error);
    }
  };

  const loadDailyChartData = async () => {
    try {
      const monthDate = new Date(selectedYear, selectedMonth - 1, 1);
      const startMonth = startOfMonth(monthDate);
      const endMonth = endOfMonth(monthDate);
      
      const days = eachDayOfInterval({ start: startMonth, end: endMonth });
      const dailyData = [];
      
      for (const day of days) {
        const start = startOfDay(day);
        const end = endOfDay(day);
        const revenue = await calculateDoanhThu(start, end);
        
        dailyData.push({
          name: format(day, 'dd/MM'),
          doanhThu: revenue,
        });
      }
      
      setDailyChartData(dailyData);
    } catch (error) {
      console.error('Error loading daily chart data:', error);
    }
  };

  const loadTopProducts = async () => {
    try {
      // Tính toán theo năm được chọn (tất cả tháng trong năm)
      const startYear = startOfYear(new Date(selectedYear, 0, 1));
      const endYear = endOfYear(new Date(selectedYear, 11, 31));
      
      // Load tất cả hóa đơn, chi tiết và hàng hóa từ Realtime Database
      const [hoaDonArray, hoaDonChiTietArray, hangHoaArray] = await Promise.all([
        getAllFromRealtimeDB(REALTIME_NODES.HOA_DON, true),
        getAllFromRealtimeDB(REALTIME_NODES.HOA_DON_CHI_TIET, true),
        getAllFromRealtimeDB(REALTIME_NODES.HANG_HOA, true),
      ]);
      
      // Lọc hóa đơn đã thanh toán trong năm được chọn
      const hoaDonPaid = hoaDonArray.filter(hd => {
        if (hd.trangThai !== 1) return false;
        const gioVao = parseRealtimeDateTime(hd.gioVao);
        return gioVao && gioVao >= startYear && gioVao <= endYear;
      });
      
      const productMap = new Map();
      
      for (const hoaDon of hoaDonPaid) {
        const maHoaDon = hoaDon.maHoaDon || parseInt(hoaDon.id);
        const chiTiet = hoaDonChiTietArray.filter(ct => {
          const ctMaHoaDon = ct.maHoaDon;
          return ctMaHoaDon === maHoaDon || ctMaHoaDon === parseInt(hoaDon.id);
        });
        
        chiTiet.forEach(ct => {
          const maHangHoa = ct.maHangHoa;
          const soLuong = ct.soLuong || 0;
          const giaTien = ct.giaTien || 0;
          const tongTien = soLuong * giaTien;
          
          // Lấy tên hàng hóa
          const hangHoa = hangHoaArray.find(hh => 
            (hh.maHangHoa || parseInt(hh.id)) === maHangHoa
          );
          const tenHangHoa = hangHoa?.tenHangHoa || `Sản phẩm #${maHangHoa}`;
          
          if (productMap.has(maHangHoa)) {
            const existing = productMap.get(maHangHoa);
            existing.soLuong += soLuong;
            existing.tongTien += tongTien;
          } else {
            productMap.set(maHangHoa, {
              maHangHoa: maHangHoa,
              tenHangHoa: tenHangHoa,
              soLuong: soLuong,
              tongTien: tongTien,
            });
          }
        });
      }
      
      // Sắp xếp và lấy top 8
      const topProductsArray = Array.from(productMap.values())
        .sort((a, b) => b.tongTien - a.tongTien)
        .slice(0, 8);
      
      setTopProducts(topProductsArray);
    } catch (error) {
      console.error('Error loading top products:', error);
    }
  };

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('vi-VN').format(amount) + 'đ';
  };

  const calculatePercentageChange = (current, previous) => {
    if (previous === 0) return current > 0 ? 100 : 0;
    return Math.round(((current - previous) / previous) * 100);
  };

  const getTrendIcon = (current, previous) => {
    const change = calculatePercentageChange(current, previous);
    if (change > 0) {
      return <TrendingUp className="w-5 h-5 text-green-600" />;
    } else if (change < 0) {
      return <TrendingDown className="w-5 h-5 text-red-600" />;
    }
    return null;
  };

  const getTrendText = (current, previous) => {
    const change = calculatePercentageChange(current, previous);
    if (change > 0) {
      return `+${change}% so với trước`;
    } else if (change < 0) {
      return `${change}% so với trước`;
    }
    return 'Không thay đổi';
  };

  if (loading) {
    return (
      <div className="p-8">
        <div className="animate-pulse space-y-4">
          <div className="h-8 bg-gray-200 rounded w-1/4"></div>
          <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-5 gap-6">
            {[1, 2, 3, 4, 5].map((i) => (
              <div key={i} className="h-32 bg-gray-200 rounded"></div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="p-8">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Quản lý Doanh thu</h1>
        <p className="text-gray-600 mt-2">Theo dõi và phân tích doanh thu chi tiết của cửa hàng</p>
      </div>

      {/* Thống kê tổng quan */}
      <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-5 gap-6 mb-8">
        <div className="card">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600 mb-1">Doanh thu hôm nay</p>
              <p className="text-2xl font-bold text-gray-900">
                {formatCurrency(doanhThu.ngay)}
              </p>
            </div>
            <div className="bg-green-500 p-3 rounded-lg">
              <DollarSign className="w-6 h-6 text-white" />
            </div>
          </div>
        </div>

        <div className="card">
          <div className="flex items-center justify-between">
            <div className="flex-1">
              <p className="text-sm text-gray-600 mb-1">Doanh thu tháng này</p>
              <p className="text-2xl font-bold text-gray-900 mb-1">
                {formatCurrency(doanhThu.thang)}
              </p>
              {doanhThu.thangTruoc > 0 && (
                <div className="flex items-center space-x-1 text-xs">
                  {getTrendIcon(doanhThu.thang, doanhThu.thangTruoc)}
                  <span className={doanhThu.thang > doanhThu.thangTruoc ? 'text-green-600' : 'text-red-600'}>
                    {getTrendText(doanhThu.thang, doanhThu.thangTruoc)}
                  </span>
                </div>
              )}
            </div>
            <div className="bg-blue-500 p-3 rounded-lg">
              <BarChart3 className="w-6 h-6 text-white" />
            </div>
          </div>
        </div>

        <div className="card">
          <div className="flex items-center justify-between">
            <div className="flex-1">
              <p className="text-sm text-gray-600 mb-1">Doanh thu năm nay</p>
              <p className="text-2xl font-bold text-gray-900 mb-1">
                {formatCurrency(doanhThu.nam)}
              </p>
              {doanhThu.namTruoc > 0 && (
                <div className="flex items-center space-x-1 text-xs">
                  {getTrendIcon(doanhThu.nam, doanhThu.namTruoc)}
                  <span className={doanhThu.nam > doanhThu.namTruoc ? 'text-green-600' : 'text-red-600'}>
                    {getTrendText(doanhThu.nam, doanhThu.namTruoc)}
                  </span>
                </div>
              )}
            </div>
            <div className="bg-orange-500 p-3 rounded-lg">
              <Calendar className="w-6 h-6 text-white" />
            </div>
          </div>
        </div>

        <div className="card">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600 mb-1">Tháng trước</p>
              <p className="text-2xl font-bold text-gray-900">
                {formatCurrency(doanhThu.thangTruoc)}
              </p>
            </div>
            <div className="bg-purple-500 p-3 rounded-lg">
              <DollarSign className="w-6 h-6 text-white" />
            </div>
          </div>
        </div>

        <div className="card">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600 mb-1">Năm trước</p>
              <p className="text-2xl font-bold text-gray-900">
                {formatCurrency(doanhThu.namTruoc)}
              </p>
            </div>
            <div className="bg-pink-500 p-3 rounded-lg">
              <DollarSign className="w-6 h-6 text-white" />
            </div>
          </div>
        </div>
      </div>

      {/* Filter: Chọn năm và tháng */}
      <div className="mb-6 bg-white p-4 rounded-lg shadow-sm border border-gray-200">
        <div className="flex items-center gap-4 flex-wrap">
          <div className="flex items-center gap-2">
            <CalendarCheck className="w-5 h-5 text-gray-600" />
            <label className="text-sm font-medium text-gray-700">Lọc theo năm:</label>
          </div>
          <select
            value={selectedYear}
            onChange={(e) => setSelectedYear(parseInt(e.target.value))}
            className="input border-gray-300 focus:border-primary-500 focus:ring-primary-500"
          >
            {Array.from({ length: 10 }, (_, i) => {
              const year = new Date().getFullYear() - i;
              return (
                <option key={year} value={year}>
                  {year}
                </option>
              );
            })}
          </select>
          <button
            onClick={() => setSelectedYear(new Date().getFullYear())}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
              selectedYear === new Date().getFullYear()
                ? 'bg-primary-600 text-white'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            Năm nay
          </button>
          
          {/* Chọn tháng cho biểu đồ theo ngày */}
          {activeTab === 'daily' && (
            <>
              <div className="flex items-center gap-2 ml-4">
                <label className="text-sm font-medium text-gray-700">Tháng:</label>
                <select
                  value={selectedMonth}
                  onChange={(e) => setSelectedMonth(parseInt(e.target.value))}
                  className="input border-gray-300 focus:border-primary-500 focus:ring-primary-500"
                >
                  {Array.from({ length: 12 }, (_, i) => {
                    const month = i + 1;
                    return (
                      <option key={month} value={month}>
                        Tháng {month}
                      </option>
                    );
                  })}
                </select>
              </div>
              <button
                onClick={() => setSelectedMonth(new Date().getMonth() + 1)}
                className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                  selectedMonth === new Date().getMonth() + 1
                    ? 'bg-primary-600 text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                Tháng này
              </button>
            </>
          )}
        </div>
      </div>

      {/* Tabs để chuyển đổi giữa các biểu đồ */}
      <div className="mb-6">
        <div className="flex space-x-2 border-b">
          {[
            { id: 'monthly', label: 'Theo tháng', icon: BarChart3 },
            { id: 'compare', label: 'So sánh năm', icon: TrendingUp },
            { id: 'daily', label: 'Theo ngày', icon: Calendar },
            { id: 'products', label: 'Top sản phẩm', icon: PieChart },
          ].map((tab) => {
            const Icon = tab.icon;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`flex items-center space-x-2 px-4 py-2 font-medium transition-colors ${
                  activeTab === tab.id
                    ? 'border-b-2 border-primary-600 text-primary-600'
                    : 'text-gray-600 hover:text-gray-900'
                }`}
              >
                <Icon className="w-4 h-4" />
                <span>{tab.label}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Biểu đồ theo tháng */}
      {activeTab === 'monthly' && (
        <div className="card">
          <h2 className="text-xl font-bold text-gray-900 mb-4">
            Biểu đồ doanh thu theo tháng ({selectedYear})
          </h2>
          <ResponsiveContainer width="100%" height={400}>
            <BarChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" />
              <YAxis />
              <Tooltip formatter={(value) => formatCurrency(value)} />
              <Legend />
              <Bar dataKey="doanhThu" fill="#0ea5e9" name="Doanh thu (đ)" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}

      {/* Biểu đồ so sánh */}
      {activeTab === 'compare' && (
        <div className="card">
          <h2 className="text-xl font-bold text-gray-900 mb-4">
            So sánh doanh thu năm {selectedYear} và {selectedYear - 1}
          </h2>
          <ResponsiveContainer width="100%" height={400}>
            <LineChart data={compareChartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" />
              <YAxis />
              <Tooltip formatter={(value) => formatCurrency(value)} />
              <Legend />
              <Line 
                type="monotone" 
                dataKey={`Năm ${selectedYear}`} 
                stroke="#0ea5e9" 
                strokeWidth={2}
                name={`Năm ${selectedYear}`}
              />
              <Line 
                type="monotone" 
                dataKey={`Năm ${selectedYear - 1}`} 
                stroke="#f59e0b" 
                strokeWidth={2}
                name={`Năm ${selectedYear - 1}`}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}

      {/* Biểu đồ theo ngày */}
      {activeTab === 'daily' && (
        <div className="card">
          <h2 className="text-xl font-bold text-gray-900 mb-4">
            Doanh thu theo ngày trong tháng {selectedMonth}/{selectedYear}
          </h2>
          <ResponsiveContainer width="100%" height={400}>
            <LineChart data={dailyChartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" angle={-45} textAnchor="end" height={80} />
              <YAxis />
              <Tooltip formatter={(value) => formatCurrency(value)} />
              <Legend />
              <Line 
                type="monotone" 
                dataKey="doanhThu" 
                stroke="#10b981" 
                strokeWidth={2}
                name="Doanh thu (đ)"
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}

      {/* Top sản phẩm */}
      {activeTab === 'products' && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="card">
            <h2 className="text-xl font-bold text-gray-900 mb-4">
              Top sản phẩm bán chạy (Năm {selectedYear})
            </h2>
            <ResponsiveContainer width="100%" height={400}>
              <RechartsPieChart>
                <Pie
                  data={topProducts}
                  dataKey="tongTien"
                  nameKey="tenHangHoa"
                  cx="50%"
                  cy="50%"
                  outerRadius={120}
                  label={(entry) => `${entry.tenHangHoa}: ${formatCurrency(entry.tongTien)}`}
                >
                  {topProducts.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip formatter={(value) => formatCurrency(value)} />
                <Legend />
              </RechartsPieChart>
            </ResponsiveContainer>
          </div>

          <div className="card">
            <h2 className="text-xl font-bold text-gray-900 mb-4">
              Bảng xếp hạng sản phẩm
            </h2>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">STT</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Sản phẩm</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Số lượng</th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Doanh thu</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200">
                  {topProducts.length === 0 ? (
                    <tr>
                      <td colSpan="4" className="px-4 py-4 text-center text-gray-500">
                        Chưa có dữ liệu
                      </td>
                    </tr>
                  ) : (
                    topProducts.map((product, index) => (
                      <tr key={product.maHangHoa} className="hover:bg-gray-50">
                        <td className="px-4 py-3 text-sm text-gray-900">{index + 1}</td>
                        <td className="px-4 py-3 text-sm font-medium text-gray-900">
                          {product.tenHangHoa}
                        </td>
                        <td className="px-4 py-3 text-sm text-gray-500">{product.soLuong}</td>
                        <td className="px-4 py-3 text-sm font-semibold text-gray-900 text-right">
                          {formatCurrency(product.tongTien)}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default QuanLyDoanhThu;
