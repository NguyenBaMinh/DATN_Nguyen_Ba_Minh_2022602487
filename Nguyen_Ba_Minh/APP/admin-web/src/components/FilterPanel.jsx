import { Filter, X, ChevronDown, Save, Star, Download, Bookmark } from 'lucide-react';
import { useState, useEffect } from 'react';

/**
 * Component Filter Panel tái sử dụng - NÂNG CẤP
 * 
 * ✅ Tính năng mới:
 * - Saved Filters (lưu bộ lọc yêu thích)
 * - Quick Filters (preset filters)
 * - Filter Chips (hiển thị active filters)
 * - Multi-Select support
 * - Range Slider cho number
 * - LocalStorage persistence
 * 
 * @param {Object} props
 * @param {Array} props.filters - Mảng các filter options
 * @param {Function} props.onFilterChange - Callback khi filter thay đổi
 * @param {Function} props.onReset - Callback khi reset filters
 * @param {Boolean} props.showReset - Hiển thị nút reset
 * @param {String} props.storageKey - Key để lưu filters vào localStorage
 * @param {Array} props.quickFilters - Danh sách quick filters preset
 * @param {Function} props.onExport - Callback khi export data
 */
const FilterPanel = ({ 
  filters = [], 
  onFilterChange, 
  onReset, 
  showReset = true,
  storageKey = null,
  quickFilters = [],
  onExport = null
}) => {
  const [isOpen, setIsOpen] = useState(true);
  const [savedFilters, setSavedFilters] = useState([]);
  const [showSaveDialog, setShowSaveDialog] = useState(false);
  const [filterName, setFilterName] = useState('');

  // Load saved filters từ localStorage
  useEffect(() => {
    if (storageKey) {
      const saved = localStorage.getItem(`${storageKey}_saved_filters`);
      if (saved) {
        try {
          setSavedFilters(JSON.parse(saved));
        } catch (e) {
          console.error('Error loading saved filters:', e);
        }
      }
    }
  }, [storageKey]);

  // Lưu filter hiện tại
  const handleSaveFilter = () => {
    if (!filterName.trim()) return;
    
    const newFilter = {
      id: Date.now(),
      name: filterName,
      filters: filters.reduce((acc, f) => {
        if (f.value) {
          acc[f.key] = f.value;
        }
        return acc;
      }, {})
    };
    
    const updated = [...savedFilters, newFilter];
    setSavedFilters(updated);
    
    if (storageKey) {
      localStorage.setItem(`${storageKey}_saved_filters`, JSON.stringify(updated));
    }
    
    setFilterName('');
    setShowSaveDialog(false);
  };

  // Load saved filter
  const handleLoadFilter = (savedFilter) => {
    Object.entries(savedFilter.filters).forEach(([key, value]) => {
      onFilterChange(key, value);
    });
  };

  // Xóa saved filter
  const handleDeleteSavedFilter = (id) => {
    const updated = savedFilters.filter(f => f.id !== id);
    setSavedFilters(updated);
    
    if (storageKey) {
      localStorage.setItem(`${storageKey}_saved_filters`, JSON.stringify(updated));
    }
  };

  // Apply quick filter
  const handleQuickFilter = (quickFilter) => {
    Object.entries(quickFilter.values).forEach(([key, value]) => {
      onFilterChange(key, value);
    });
  };

  // Count active filters
  const activeCount = filters.filter(f => {
    if (typeof f.value === 'object' && f.value !== null) {
      return f.value.from || f.value.to || (Array.isArray(f.value) && f.value.length > 0);
    }
    return f.value !== '' && f.value !== null && f.value !== undefined;
  }).length;

  // Get active filter chips
  const getActiveFilterChips = () => {
    return filters.filter(f => {
      if (typeof f.value === 'object' && f.value !== null) {
        return f.value.from || f.value.to || (Array.isArray(f.value) && f.value.length > 0);
      }
      return f.value !== '' && f.value !== null && f.value !== undefined;
    }).map(f => {
      let displayValue = f.value;
      
      if (f.type === 'select' && f.options) {
        const option = f.options.find(opt => opt.value === f.value);
        displayValue = option?.label || f.value;
      } else if (f.type === 'daterange') {
        displayValue = `${f.value.from || '...'} - ${f.value.to || '...'}`;
      } else if (Array.isArray(f.value)) {
        displayValue = f.value.length + ' items';
      }
      
      return {
        key: f.key,
        label: f.label,
        value: displayValue
      };
    });
  };

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 mb-6">
      {/* Header */}
      <div 
        className="flex items-center justify-between p-4 cursor-pointer hover:bg-gray-50 transition-colors"
        onClick={() => setIsOpen(!isOpen)}
      >
        <div className="flex items-center gap-3">
          <Filter className="w-5 h-5 text-gray-600" />
          <span className="font-semibold text-gray-700">Bộ lọc</span>
          {activeCount > 0 && (
            <span className="px-2 py-0.5 bg-blue-100 text-blue-700 text-xs font-semibold rounded-full">
              {activeCount}
            </span>
          )}
        </div>
        
        <div className="flex items-center gap-2">
          {/* Export button */}
          {onExport && activeCount > 0 && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                onExport();
              }}
              className="flex items-center gap-1 px-3 py-1.5 text-sm text-green-700 bg-green-50 border border-green-200 rounded-lg hover:bg-green-100 transition-colors"
              title="Xuất dữ liệu đã lọc"
            >
              <Download className="w-4 h-4" />
              <span className="hidden sm:inline">Xuất</span>
            </button>
          )}
          
          {/* Save filter button */}
          {storageKey && activeCount > 0 && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                setShowSaveDialog(true);
              }}
              className="flex items-center gap-1 px-3 py-1.5 text-sm text-blue-700 bg-blue-50 border border-blue-200 rounded-lg hover:bg-blue-100 transition-colors"
              title="Lưu bộ lọc hiện tại"
            >
              <Save className="w-4 h-4" />
              <span className="hidden sm:inline">Lưu</span>
            </button>
          )}
          
          <ChevronDown 
            className={`w-5 h-5 text-gray-400 transition-transform ${isOpen ? 'rotate-180' : ''}`}
          />
        </div>
      </div>

      {/* Active Filter Chips */}
      {activeCount > 0 && !isOpen && (
        <div className="px-4 pb-3 flex flex-wrap gap-2">
          {getActiveFilterChips().map((chip) => (
            <div
              key={chip.key}
              className="flex items-center gap-1.5 px-3 py-1 bg-blue-50 text-blue-700 text-sm rounded-full border border-blue-200"
            >
              <span className="font-medium">{chip.label}:</span>
              <span>{chip.value}</span>
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  onFilterChange(chip.key, filters.find(f => f.key === chip.key)?.type === 'daterange' ? { from: '', to: '' } : '');
                }}
                className="ml-1 hover:bg-blue-200 rounded-full p-0.5 transition-colors"
              >
                <X className="w-3 h-3" />
              </button>
            </div>
          ))}
        </div>
      )}

      {/* Filter Options */}
      {isOpen && (
        <div className="p-4 pt-0 border-t border-gray-100">
          {/* Quick Filters */}
          {quickFilters.length > 0 && (
            <div className="mb-4 pb-4 border-b border-gray-100">
              <div className="flex items-center gap-2 mb-3">
                <Star className="w-4 h-4 text-yellow-500" />
                <span className="text-sm font-medium text-gray-700">Lọc nhanh:</span>
              </div>
              <div className="flex flex-wrap gap-2">
                {quickFilters.map((qf, index) => (
                  <button
                    key={index}
                    onClick={() => handleQuickFilter(qf)}
                    className="px-3 py-1.5 text-sm bg-yellow-50 text-yellow-700 border border-yellow-200 rounded-lg hover:bg-yellow-100 transition-colors"
                  >
                    {qf.label}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Saved Filters */}
          {savedFilters.length > 0 && (
            <div className="mb-4 pb-4 border-b border-gray-100">
              <div className="flex items-center gap-2 mb-3">
                <Bookmark className="w-4 h-4 text-purple-500" />
                <span className="text-sm font-medium text-gray-700">Bộ lọc đã lưu:</span>
              </div>
              <div className="flex flex-wrap gap-2">
                {savedFilters.map((sf) => (
                  <div
                    key={sf.id}
                    className="flex items-center gap-2 px-3 py-1.5 text-sm bg-purple-50 text-purple-700 border border-purple-200 rounded-lg group"
                  >
                    <button
                      onClick={() => handleLoadFilter(sf)}
                      className="hover:underline"
                    >
                      {sf.name}
                    </button>
                    <button
                      onClick={() => handleDeleteSavedFilter(sf.id)}
                      className="opacity-0 group-hover:opacity-100 hover:bg-purple-200 rounded-full p-0.5 transition-all"
                    >
                      <X className="w-3 h-3" />
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Filter Inputs */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
            {filters.map((filter, index) => (
              <div key={index} className="flex flex-col">
                <label className="text-sm font-medium text-gray-700 mb-2">
                  {filter.label}
                  {filter.required && <span className="text-red-500 ml-1">*</span>}
                </label>
                
                {filter.type === 'select' && (
                  <select
                    value={filter.value || ''}
                    onChange={(e) => onFilterChange(filter.key, e.target.value)}
                    className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                  >
                    <option value="">{filter.placeholder || 'Tất cả'}</option>
                    {filter.options?.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                )}

                {filter.type === 'date' && (
                  <input
                    type="date"
                    value={filter.value || ''}
                    onChange={(e) => onFilterChange(filter.key, e.target.value)}
                    className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                  />
                )}

                {filter.type === 'daterange' && (
                  <div className="flex gap-2">
                    <input
                      type="date"
                      value={filter.value?.from || ''}
                      onChange={(e) => onFilterChange(filter.key, { ...filter.value, from: e.target.value })}
                      placeholder="Từ ngày"
                      className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                    />
                    <span className="self-center text-gray-400">-</span>
                    <input
                      type="date"
                      value={filter.value?.to || ''}
                      onChange={(e) => onFilterChange(filter.key, { ...filter.value, to: e.target.value })}
                      placeholder="Đến ngày"
                      className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                    />
                  </div>
                )}

                {filter.type === 'number' && (
                  <input
                    type="number"
                    value={filter.value || ''}
                    onChange={(e) => onFilterChange(filter.key, e.target.value)}
                    placeholder={filter.placeholder}
                    className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                  />
                )}

                {filter.type === 'text' && (
                  <input
                    type="text"
                    value={filter.value || ''}
                    onChange={(e) => onFilterChange(filter.key, e.target.value)}
                    placeholder={filter.placeholder}
                    className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                  />
                )}

                {filter.type === 'multiselect' && (
                  <div className="relative">
                    <select
                      multiple
                      value={filter.value || []}
                      onChange={(e) => {
                        const selected = Array.from(e.target.selectedOptions, option => option.value);
                        onFilterChange(filter.key, selected);
                      }}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                      size={Math.min(4, filter.options?.length || 4)}
                    >
                      {filter.options?.map((option) => (
                        <option key={option.value} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                    <div className="text-xs text-gray-500 mt-1">
                      Hold Ctrl/Cmd để chọn nhiều
                    </div>
                  </div>
                )}

                {filter.type === 'range' && (
                  <div className="space-y-2">
                    <input
                      type="range"
                      min={filter.min || 0}
                      max={filter.max || 100}
                      step={filter.step || 1}
                      value={filter.value || filter.min || 0}
                      onChange={(e) => onFilterChange(filter.key, parseInt(e.target.value))}
                      className="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer accent-blue-500"
                    />
                    <div className="flex justify-between text-xs text-gray-600">
                      <span>{filter.min || 0}</span>
                      <span className="font-semibold text-blue-600">
                        {filter.value || filter.min || 0} {filter.unit || ''}
                      </span>
                      <span>{filter.max || 100}</span>
                    </div>
                  </div>
                )}

                {filter.type === 'numberrange' && (
                  <div className="flex gap-2 items-center">
                    <input
                      type="number"
                      value={filter.value?.min || ''}
                      onChange={(e) => onFilterChange(filter.key, { ...filter.value, min: e.target.value })}
                      placeholder={filter.placeholder?.min || 'Từ'}
                      className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                    />
                    <span className="text-gray-400">-</span>
                    <input
                      type="number"
                      value={filter.value?.max || ''}
                      onChange={(e) => onFilterChange(filter.key, { ...filter.value, max: e.target.value })}
                      placeholder={filter.placeholder?.max || 'Đến'}
                      className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                    />
                  </div>
                )}
              </div>
            ))}
          </div>

          {/* Action Buttons */}
          {showReset && (
            <div className="flex justify-between items-center gap-2 mt-4 pt-4 border-t border-gray-100">
              <div className="text-sm text-gray-600">
                {activeCount > 0 && (
                  <span>
                    Đang áp dụng <span className="font-semibold text-blue-600">{activeCount}</span> bộ lọc
                  </span>
                )}
              </div>
              <button
                onClick={onReset}
                className="flex items-center gap-2 px-4 py-2 text-gray-600 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors"
              >
                <X className="w-4 h-4" />
                Xóa tất cả
              </button>
            </div>
          )}
        </div>
      )}

      {/* Save Filter Dialog */}
      {showSaveDialog && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl p-6 max-w-md w-full mx-4">
            <h3 className="text-lg font-semibold text-gray-800 mb-4">
              Lưu bộ lọc
            </h3>
            <input
              type="text"
              value={filterName}
              onChange={(e) => setFilterName(e.target.value)}
              placeholder="Nhập tên bộ lọc..."
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent mb-4"
              autoFocus
              onKeyPress={(e) => {
                if (e.key === 'Enter') {
                  handleSaveFilter();
                }
              }}
            />
            <div className="flex justify-end gap-2">
              <button
                onClick={() => {
                  setShowSaveDialog(false);
                  setFilterName('');
                }}
                className="px-4 py-2 text-gray-600 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors"
              >
                Hủy
              </button>
              <button
                onClick={handleSaveFilter}
                disabled={!filterName.trim()}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors"
              >
                Lưu
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default FilterPanel;

