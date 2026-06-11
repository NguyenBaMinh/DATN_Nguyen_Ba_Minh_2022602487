import { CheckCircle, XCircle, Clock, AlertCircle, DollarSign, Package, Calendar, Coffee } from 'lucide-react';

/**
 * Component hiển thị status badge
 */
const StatusBadge = ({ type, value, label, size = 'md' }) => {
  const getConfig = () => {
    const configs = {
      datban: {
        0: { label: 'Chờ duyệt', bg: 'bg-yellow-100', text: 'text-yellow-800', border: 'border-yellow-300', icon: Clock },
        1: { label: 'Đã duyệt', bg: 'bg-green-100', text: 'text-green-800', border: 'border-green-300', icon: CheckCircle },
        '-1': { label: 'Từ chối', bg: 'bg-red-100', text: 'text-red-800', border: 'border-red-300', icon: XCircle },
        2: { label: 'Đã sử dụng', bg: 'bg-blue-100', text: 'text-blue-800', border: 'border-blue-300', icon: CheckCircle },
        3: { label: 'Đã hủy', bg: 'bg-gray-100', text: 'text-gray-800', border: 'border-gray-300', icon: XCircle },
      },
      hoadon: {
        0: { label: 'Chưa thanh toán', bg: 'bg-orange-100', text: 'text-orange-800', border: 'border-orange-300', icon: Clock },
        1: { label: 'Đã thanh toán', bg: 'bg-green-100', text: 'text-green-800', border: 'border-green-300', icon: DollarSign },
        2: { label: 'Đã duyệt', bg: 'bg-blue-100', text: 'text-blue-800', border: 'border-blue-300', icon: CheckCircle },
        3: { label: 'Đã hủy', bg: 'bg-gray-100', text: 'text-gray-800', border: 'border-gray-300', icon: XCircle },
      },
      hanghoa: {
        0: { label: 'Hết hàng', bg: 'bg-red-100', text: 'text-red-800', border: 'border-red-300', icon: AlertCircle },
        1: { label: 'Còn hàng', bg: 'bg-green-100', text: 'text-green-800', border: 'border-green-300', icon: Package },
      },
      // ✅ ĐÃ CẬP NHẬT: Thêm trạng thái "Đang sử dụng"
      ban: {
        0: {
          label: 'Còn trống',
          bg: 'bg-green-100',
          text: 'text-green-800',
          border: 'border-green-300',
          icon: CheckCircle,
        },
        1: {
          label: 'Đang sử dụng',  // ✅ Đổi từ "Có khách" → "Đang sử dụng"
          bg: 'bg-blue-100',
          text: 'text-blue-800',
          border: 'border-blue-300',
          icon: Coffee,            // ✅ Dùng icon Coffee cho trực quan hơn
        },
        2: {
          label: 'Đã đặt',
          bg: 'bg-orange-100',
          text: 'text-orange-800',
          border: 'border-orange-300',
          icon: Calendar,
        },
      },
    };

    return configs[type]?.[value?.toString()] || {
      label: label || 'Không xác định',
      bg: 'bg-gray-100',
      text: 'text-gray-800',
      border: 'border-gray-300',
      icon: AlertCircle,
    };
  };

  const config = getConfig();
  const Icon = config.icon;

  const sizeClasses = { sm: 'px-2 py-1 text-xs', md: 'px-3 py-1.5 text-sm', lg: 'px-4 py-2 text-base' };
  const iconSizes = { sm: 'w-3 h-3', md: 'w-4 h-4', lg: 'w-5 h-5' };

  return (
    <span className={`inline-flex items-center gap-1.5 font-medium rounded-full border
      ${config.bg} ${config.text} ${config.border} ${sizeClasses[size]}`}>
      <Icon className={iconSizes[size]} />
      {label || config.label}
    </span>
  );
};

export default StatusBadge;