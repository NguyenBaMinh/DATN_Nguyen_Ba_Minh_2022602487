import { useEffect, useState } from 'react';
import { queryRealtimeDB, getAllFromRealtimeDB, REALTIME_NODES } from '../utils/realtimeDB';
import { parseRealtimeDateTime } from '../utils/dateUtils';
import { 
  Users, DollarSign, Table, ShoppingCart, TrendingUp, Clock, 
  CheckCircle, XCircle, AlertCircle, Calendar, ArrowRight
} from 'lucide-react';
import { format, startOfDay, endOfDay, subDays, startOfMonth } from 'date-fns';
import { vi } from 'date-fns/locale';
import { Link } from 'react-router-dom';
import StatusBadge from '../components/StatusBadge';

const Dashboard = () => {
  const [stats, setStats] = useState({
    totalNhanVien: 0,
    totalDoanhThu: 0,
    doanhThuThang: 0,
    totalBan: 0,
    totalHangHoa: 0,
    hoaDonHomNay: 0,
    hoaDonChuaThanhToan: 0,
    hoaDonDaThanhToan: 0,
  });
  const [recentInvoices, setRecentInvoices] = useState([]);
  const [topProducts, setTopProducts] = useState([]);
  const [dailyRevenue, setDailyRevenue] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    try {
      // Load all data in parallel
      const [nhanVienList, banList, hangHoaList, hoaDonList, hoaDonChiTietList] = await Promise.all([
        queryRealtimeDB(REALTIME_NODES.NGUOI_DUNG, (user) => user.chucVu === 'NhanVien', false),
        getAllFromRealtimeDB(REALTIME_NODES.BAN, true),
        getAllFromRealtimeDB(REALTIME_NODES.HANG_HOA, true),
        getAllFromRealtimeDB(REALTIME_NODES.HOA_DON, true),
        getAllFromRealtimeDB(REALTIME_NODES.HOA_DON_CHI_TIET, true)
      ]);

      const today = new Date();
      const startDay = startOfDay(today);
      const endDay = endOfDay(today);
      const startMonth = startOfMonth(today);

      // Calculate revenue
      let doanhThuHomNay = 0;
      let doanhThuThang = 0;
      let hoaDonHomNay = 0;
      const hoaDonChuaThanhToan = hoaDonList.filter(hd => hd.trangThai === 0).length;
      const hoaDonDaThanhToan = hoaDonList.filter(hd => hd.trangThai === 1).length;

      // Process invoices with details
      const invoicesWithDetails = hoaDonList.map(hoaDon => {
        const chiTiet = hoaDonChiTietList.filter(ct => 
          ct.maHoaDon === hoaDon.maHoaDon || ct.maHoaDon === parseInt(hoaDon.id)
        );
        
        const tongTien = chiTiet.reduce((sum, ct) => 
          sum + (ct.soLuong || 0) * (ct.giaTien || 0), 0
        );

        return { ...hoaDon, chiTiet, tongTien };
      });

      // Calculate daily revenue for last 7 days
      const last7Days = [];
      for (let i = 6; i >= 0; i--) {
        const date = subDays(today, i);
        const dayStart = startOfDay(date);
        const dayEnd = endOfDay(date);
        
        const dayRevenue = invoicesWithDetails
          .filter(hd => {
            const gioVao = parseRealtimeDateTime(hd.gioVao);
            return gioVao && gioVao >= dayStart && gioVao <= dayEnd && hd.trangThai === 1;
          })
          .reduce((sum, hd) => sum + (hd.tongTien || 0), 0);
        
        last7Days.push({
          date: format(date, 'dd/MM'),
          revenue: dayRevenue
        });
      }

      // Calculate stats
      invoicesWithDetails.forEach(hoaDon => {
        const gioVao = parseRealtimeDateTime(hoaDon.gioVao);
        
        if (hoaDon.trangThai === 1) {
          if (gioVao && gioVao >= startDay && gioVao <= endDay) {
            doanhThuHomNay += hoaDon.tongTien || 0;
          }
          if (gioVao && gioVao >= startMonth) {
            doanhThuThang += hoaDon.tongTien || 0;
          }
        }
        
        if (gioVao && gioVao >= startDay && gioVao <= endDay) {
          hoaDonHomNay++;
        }
      });

      // Get recent invoices (last 5)
      const recent = invoicesWithDetails
        .sort((a, b) => {
          const dateA = parseRealtimeDateTime(a.gioVao) || new Date(0);
          const dateB = parseRealtimeDateTime(b.gioVao) || new Date(0);
          return dateB - dateA;
        })
        .slice(0, 5);

      // Calculate top products
      const productSales = {};
      hoaDonChiTietList.forEach(ct => {
        if (!productSales[ct.maHangHoa]) {
          const hangHoa = hangHoaList.find(hh => 
            (hh.maHangHoa || parseInt(hh.id)) === ct.maHangHoa
          );
          productSales[ct.maHangHoa] = {
            name: hangHoa?.tenHangHoa || `Hàng hóa #${ct.maHangHoa}`,
            quantity: 0,
            revenue: 0
          };
        }
        productSales[ct.maHangHoa].quantity += ct.soLuong || 0;
        productSales[ct.maHangHoa].revenue += (ct.soLuong || 0) * (ct.giaTien || 0);
      });

      const topProductsList = Object.entries(productSales)
        .map(([id, data]) => ({ id, ...data }))
        .sort((a, b) => b.revenue - a.revenue)
        .slice(0, 5);

      setStats({
        totalNhanVien: nhanVienList.length,
        totalDoanhThu: doanhThuHomNay,
        doanhThuThang,
        totalBan: banList.length,
        totalHangHoa: hangHoaList.length,
        hoaDonHomNay,
        hoaDonChuaThanhToan,
        hoaDonDaThanhToan,
      });
      setRecentInvoices(recent);
      setTopProducts(topProductsList);
      setDailyRevenue(last7Days);
    } catch (error) {
      console.error('Error loading dashboard data:', error);
    } finally {
      setLoading(false);
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

  if (loading) {
    return (
      <div className="p-4 sm:p-6 lg:p-8 max-w-7xl mx-auto">
        <div className="animate-pulse space-y-4">
          <div className="h-8 bg-gray-200 rounded w-1/4"></div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {[1, 2, 3, 4].map((i) => (
              <div key={i} className="h-24 bg-gray-200 rounded"></div>
            ))}
          </div>
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            <div className="h-64 bg-gray-200 rounded"></div>
            <div className="h-64 bg-gray-200 rounded"></div>
          </div>
        </div>
      </div>
    );
  }

  const maxRevenue = Math.max(...dailyRevenue.map(d => d.revenue), 1);

  return (
    <div className="p-4 sm:p-6 lg:p-8 max-w-7xl mx-auto">
      {/* Header */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
        <p className="text-sm text-gray-500 mt-1">
          {format(new Date(), 'EEEE, dd MMMM yyyy', { locale: vi })}
        </p>
      </div>

      {/* Main Stats Grid */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 mb-6">
        {/* Today Revenue */}
        <div className="bg-gradient-to-br from-blue-500 to-blue-600 rounded-lg p-4 text-white">
          <div className="flex items-center justify-between mb-2">
            <DollarSign className="w-8 h-8 opacity-80" />
            <TrendingUp className="w-4 h-4" />
          </div>
          <p className="text-xs opacity-90">Doanh thu hôm nay</p>
          <p className="text-xl font-bold mt-1">{formatCurrency(stats.totalDoanhThu)}</p>
        </div>

        {/* Monthly Revenue */}
        <div className="bg-gradient-to-br from-green-500 to-green-600 rounded-lg p-4 text-white">
          <div className="flex items-center justify-between mb-2">
            <Calendar className="w-8 h-8 opacity-80" />
            <TrendingUp className="w-4 h-4" />
          </div>
          <p className="text-xs opacity-90">Doanh thu tháng</p>
          <p className="text-xl font-bold mt-1">{formatCurrency(stats.doanhThuThang)}</p>
        </div>

        {/* Today Invoices */}
        <div className="bg-gradient-to-br from-purple-500 to-purple-600 rounded-lg p-4 text-white">
          <div className="flex items-center justify-between mb-2">
            <ShoppingCart className="w-8 h-8 opacity-80" />
            <Clock className="w-4 h-4" />
          </div>
          <p className="text-xs opacity-90">Hóa đơn hôm nay</p>
          <p className="text-xl font-bold mt-1">{stats.hoaDonHomNay}</p>
        </div>

        {/* Pending Invoices */}
        <div className="bg-gradient-to-br from-orange-500 to-orange-600 rounded-lg p-4 text-white">
          <div className="flex items-center justify-between mb-2">
            <AlertCircle className="w-8 h-8 opacity-80" />
            <span className="text-xs">Chưa TT</span>
          </div>
          <p className="text-xs opacity-90">Chờ thanh toán</p>
          <p className="text-xl font-bold mt-1">{stats.hoaDonChuaThanhToan}</p>
        </div>
      </div>

      {/* Secondary Stats */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 mb-6">
        <div className="bg-white rounded-lg border border-gray-200 p-3">
          <div className="flex items-center gap-2 mb-1">
            <Users className="w-4 h-4 text-gray-500" />
            <span className="text-xs text-gray-500">Nhân viên</span>
          </div>
          <p className="text-lg font-bold text-gray-900">{stats.totalNhanVien}</p>
        </div>

        <div className="bg-white rounded-lg border border-gray-200 p-3">
          <div className="flex items-center gap-2 mb-1">
            <Table className="w-4 h-4 text-gray-500" />
            <span className="text-xs text-gray-500">Bàn</span>
          </div>
          <p className="text-lg font-bold text-gray-900">{stats.totalBan}</p>
        </div>

        <div className="bg-white rounded-lg border border-gray-200 p-3">
          <div className="flex items-center gap-2 mb-1">
            <ShoppingCart className="w-4 h-4 text-gray-500" />
            <span className="text-xs text-gray-500">Hàng hóa</span>
          </div>
          <p className="text-lg font-bold text-gray-900">{stats.totalHangHoa}</p>
        </div>

        <div className="bg-white rounded-lg border border-gray-200 p-3">
          <div className="flex items-center gap-2 mb-1">
            <CheckCircle className="w-4 h-4 text-gray-500" />
            <span className="text-xs text-gray-500">Đã thanh toán</span>
          </div>
          <p className="text-lg font-bold text-gray-900">{stats.hoaDonDaThanhToan}</p>
        </div>
      </div>

      {/* Charts & Lists */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 mb-6">
        {/* Revenue Chart (7 days) */}
        <div className="bg-white rounded-lg border border-gray-200 p-4">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-base font-bold text-gray-900">Doanh thu 7 ngày</h2>
            <TrendingUp className="w-4 h-4 text-green-500" />
          </div>
          <div className="space-y-2">
            {dailyRevenue.map((day, index) => (
              <div key={index} className="flex items-center gap-2">
                <span className="text-xs text-gray-600 w-12">{day.date}</span>
                <div className="flex-1 bg-gray-100 rounded-full h-6 overflow-hidden">
                  <div 
                    className="bg-gradient-to-r from-blue-500 to-blue-600 h-full flex items-center justify-end pr-2 transition-all"
                    style={{ width: `${(day.revenue / maxRevenue) * 100}%` }}
                  >
                    {day.revenue > 0 && (
                      <span className="text-xs text-white font-medium">
                        {formatCurrency(day.revenue)}
                      </span>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Top Products */}
        <div className="bg-white rounded-lg border border-gray-200 p-4">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-base font-bold text-gray-900">Top 5 Món bán chạy</h2>
            <ShoppingCart className="w-4 h-4 text-purple-500" />
          </div>
          <div className="space-y-3">
            {topProducts.map((product, index) => (
              <div key={product.id} className="flex items-center gap-3">
                <div className="flex-shrink-0 w-6 h-6 rounded-full bg-purple-100 text-purple-700 flex items-center justify-center text-xs font-bold">
                  {index + 1}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-gray-900 truncate">{product.name}</p>
                  <p className="text-xs text-gray-500">{product.quantity} phần</p>
                </div>
                <span className="text-sm font-bold text-gray-900">
                  {formatCurrency(product.revenue)}
                </span>
              </div>
            ))}
            {topProducts.length === 0 && (
              <p className="text-sm text-gray-500 text-center py-4">Chưa có dữ liệu</p>
            )}
          </div>
        </div>
      </div>

      {/* Recent Invoices */}
      <div className="bg-white rounded-lg border border-gray-200 p-4">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-base font-bold text-gray-900">Hóa đơn gần đây</h2>
          <Link 
            to="/hoa-don" 
            className="flex items-center gap-1 text-sm text-blue-600 hover:text-blue-700"
          >
            Xem tất cả
            <ArrowRight className="w-4 h-4" />
          </Link>
        </div>
        
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-gray-200">
                <th className="text-left text-xs font-medium text-gray-500 pb-2">Mã HĐ</th>
                <th className="text-left text-xs font-medium text-gray-500 pb-2">Bàn</th>
                <th className="text-left text-xs font-medium text-gray-500 pb-2">Thời gian</th>
                <th className="text-left text-xs font-medium text-gray-500 pb-2">Trạng thái</th>
                <th className="text-right text-xs font-medium text-gray-500 pb-2">Tổng tiền</th>
              </tr>
            </thead>
            <tbody>
              {recentInvoices.map((invoice) => (
                <tr key={invoice.id} className="border-b border-gray-100 last:border-0">
                  <td className="py-3 text-sm font-medium text-gray-900">#{invoice.maHoaDon || invoice.id}</td>
                  <td className="py-3 text-sm text-gray-600">Bàn {invoice.maBan}</td>
                  <td className="py-3 text-xs text-gray-500">{formatDateTime(invoice.gioVao)}</td>
                  <td className="py-3">
                    <StatusBadge type="hoadon" value={invoice.trangThai} />
                  </td>
                  <td className="py-3 text-right text-sm font-bold text-gray-900">
                    {formatCurrency(invoice.tongTien || 0)}
                  </td>
                </tr>
              ))}
              {recentInvoices.length === 0 && (
                <tr>
                  <td colSpan="5" className="py-8 text-center text-sm text-gray-500">
                    Chưa có hóa đơn nào
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Quick Actions */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 mt-6">
        <Link 
          to="/hoa-don" 
          className="bg-white rounded-lg border border-gray-200 p-4 hover:shadow-md transition-shadow group"
        >
          <ShoppingCart className="w-6 h-6 text-blue-500 mb-2" />
          <p className="text-sm font-medium text-gray-900 group-hover:text-blue-600">Quản lý Hóa đơn</p>
        </Link>
        
        <Link 
          to="/hang-hoa" 
          className="bg-white rounded-lg border border-gray-200 p-4 hover:shadow-md transition-shadow group"
        >
          <ShoppingCart className="w-6 h-6 text-green-500 mb-2" />
          <p className="text-sm font-medium text-gray-900 group-hover:text-green-600">Quản lý Hàng hóa</p>
        </Link>
        
        <Link 
          to="/ban" 
          className="bg-white rounded-lg border border-gray-200 p-4 hover:shadow-md transition-shadow group"
        >
          <Table className="w-6 h-6 text-purple-500 mb-2" />
          <p className="text-sm font-medium text-gray-900 group-hover:text-purple-600">Quản lý Bàn</p>
        </Link>
        
        <Link 
          to="/nhan-vien" 
          className="bg-white rounded-lg border border-gray-200 p-4 hover:shadow-md transition-shadow group"
        >
          <Users className="w-6 h-6 text-orange-500 mb-2" />
          <p className="text-sm font-medium text-gray-900 group-hover:text-orange-600">Quản lý Nhân viên</p>
        </Link>
      </div>
    </div>
  );
};

export default Dashboard;
