import { ArrowUpDown, ArrowUp, ArrowDown } from 'lucide-react';

/**
 * Component sắp xếp dữ liệu
 * 
 * @param {Object} props
 * @param {Array} props.options - Các option sắp xếp
 * @param {String} props.value - Giá trị hiện tại (format: "field:asc" hoặc "field:desc")
 * @param {Function} props.onChange - Callback khi thay đổi
 */
const SortPanel = ({ options = [], value, onChange }) => {
  const [field, direction] = (value || '').split(':');

  const handleChange = (newField) => {
    let newDirection = 'asc';
    
    // Nếu click vào field đang active, đổi direction
    if (newField === field) {
      newDirection = direction === 'asc' ? 'desc' : 'asc';
    }
    
    onChange(`${newField}:${newDirection}`);
  };

  return (
    <div className="flex items-center gap-2 flex-wrap">
      <ArrowUpDown className="w-4 h-4 text-gray-500" />
      <span className="text-sm text-gray-600 font-medium">Sắp xếp:</span>
      
      <div className="flex gap-2 flex-wrap">
        {options.map((option) => {
          const isActive = field === option.value;
          const isAsc = isActive && direction === 'asc';
          const isDesc = isActive && direction === 'desc';
          
          return (
            <button
              key={option.value}
              onClick={() => handleChange(option.value)}
              className={`
                flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium
                transition-all
                ${isActive 
                  ? 'bg-blue-100 text-blue-700 border border-blue-300' 
                  : 'bg-gray-100 text-gray-700 border border-gray-300 hover:bg-gray-200'
                }
              `}
            >
              {option.label}
              {isActive && (
                isAsc ? (
                  <ArrowUp className="w-3.5 h-3.5" />
                ) : (
                  <ArrowDown className="w-3.5 h-3.5" />
                )
              )}
            </button>
          );
        })}
      </div>
    </div>
  );
};

export default SortPanel;

