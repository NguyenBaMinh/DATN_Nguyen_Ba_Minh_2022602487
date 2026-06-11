import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Layout from './components/Layout';
import QuanLyNhanVien from './pages/QuanLyNhanVien';
import QuanLyDoanhThu from './pages/QuanLyDoanhThu';
import QuanLyBan from './pages/QuanLyBan';
import QuanLyLoaiHang from './pages/QuanLyLoaiHang';
import QuanLyHangHoa from './pages/QuanLyHangHoa';
import QuanLyHoaDon from './pages/QuanLyHoaDon';
import DuyetHoaDon from './pages/DuyetHoaDon';
import DuyetDatBan from './pages/DuyetDatBan';
import ProtectedRoute from './components/ProtectedRoute';

function AppRoutes() {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  return (
    <Routes>
      <Route path="/login" element={user ? <Navigate to="/" replace /> : <Login />} />
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Dashboard />} />
        <Route path="nhan-vien" element={<QuanLyNhanVien />} />
        <Route path="doanh-thu" element={<QuanLyDoanhThu />} />
        <Route path="ban" element={<QuanLyBan />} />
        <Route path="loai-hang" element={<QuanLyLoaiHang />} />
        <Route path="hang-hoa" element={<QuanLyHangHoa />} />
        <Route path="hoa-don" element={<QuanLyHoaDon />} />
        <Route path="duyet-hoa-don" element={<DuyetHoaDon />} />
        <Route path="duyet-dat-ban" element={<DuyetDatBan />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
        <Toaster position="top-right" />
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;

