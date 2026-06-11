import { useState } from 'react';
import { Outlet, Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import {
  LayoutDashboard,
  Users,
  DollarSign,
  Table,
  Package,
  ShoppingCart,
  FileText,
  CheckCircle,
  Menu,
  X,
  LogOut,
  Coffee,
  CalendarCheck
} from 'lucide-react';

const Layout = () => {
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const location = useLocation();
  const navigate = useNavigate();
  const { userData, logout } = useAuth();

  const menuItems = [
    { path: '/', icon: LayoutDashboard, label: 'Dashboard' },
    { path: '/nhan-vien', icon: Users, label: 'Quản lý Nhân viên' },
    { path: '/doanh-thu', icon: DollarSign, label: 'Doanh thu' },
    { path: '/ban', icon: Table, label: 'Quản lý Bàn' },
    { path: '/loai-hang', icon: Package, label: 'Loại hàng' },
    { path: '/hang-hoa', icon: ShoppingCart, label: 'Hàng hóa' },
    { path: '/hoa-don', icon: FileText, label: 'Hóa đơn' },
    { path: '/duyet-hoa-don', icon: CheckCircle, label: 'Duyệt hóa đơn' },
    { path: '/duyet-dat-ban', icon: CalendarCheck, label: 'Duyệt đặt bàn' },
  ];

  const handleLogout = async () => {
    try {
      await logout();
      navigate('/login');
    } catch (error) {
      console.error('Logout error:', error);
    }
  };

  return (
    <div className="flex h-screen bg-gray-50">
      {/* Sidebar */}
      <aside
        className={`${
          sidebarOpen ? 'w-64' : 'w-20'
        } bg-white shadow-lg transition-all duration-300 flex flex-col`}
      >
        {/* Logo */}
        <div className="h-16 flex items-center justify-between px-4 border-b">
          {sidebarOpen && (
            <div className="flex items-center space-x-2">
              <Coffee className="w-8 h-8 text-primary-600" />
              <span className="text-xl font-bold text-gray-900">Coffee Admin</span>
            </div>
          )}
          <button
            onClick={() => setSidebarOpen(!sidebarOpen)}
            className="p-2 rounded-lg hover:bg-gray-100"
          >
            {sidebarOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
          </button>
        </div>

        {/* Navigation */}
        <nav className="flex-1 overflow-y-auto py-4">
          {menuItems.map((item) => {
            const Icon = item.icon;
            const isActive = location.pathname === item.path;
            return (
              <Link
                key={item.path}
                to={item.path}
                className={`flex items-center space-x-3 px-4 py-3 mx-2 rounded-lg transition-colors ${
                  isActive
                    ? 'bg-primary-50 text-primary-600'
                    : 'text-gray-700 hover:bg-gray-100'
                }`}
              >
                <Icon className="w-5 h-5 flex-shrink-0" />
                {sidebarOpen && <span className="font-medium">{item.label}</span>}
              </Link>
            );
          })}
        </nav>

        {/* User Info */}
        <div className="border-t p-4">
          {sidebarOpen && userData && (
            <div className="mb-3">
              <p className="text-sm font-medium text-gray-900">{userData.hoVaTen}</p>
              <p className="text-xs text-gray-500">{userData.email}</p>
            </div>
          )}
          <button
            onClick={handleLogout}
            className="w-full flex items-center space-x-3 px-4 py-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
          >
            <LogOut className="w-5 h-5" />
            {sidebarOpen && <span>Đăng xuất</span>}
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 overflow-y-auto">
        <Outlet />
      </main>
    </div>
  );
};

export default Layout;

