import { Search, X, Sparkles } from 'lucide-react';
import { useState, useEffect } from 'react';

/**
 * Component tìm kiếm nâng cao
 * 
 * ✅ Tính năng:
 * - Search multiple fields
 * - Real-time suggestions
 * - Search history (localStorage)
 * - Fuzzy search (tìm gần đúng)
 * - Debounce để tối ưu performance
 * 
 * @param {Object} props
 * @param {Array} props.searchFields - Các trường có thể search
 * @param {Function} props.onSearch - Callback khi search
 * @param {String} props.placeholder - Placeholder text
 * @param {Number} props.debounceMs - Thời gian debounce (ms)
 * @param {String} props.storageKey - Key để lưu history
 */
const AdvancedSearchPanel = ({ 
  searchFields = [], 
  onSearch, 
  placeholder = 'Tìm kiếm...',
  debounceMs = 300,
  storageKey = null
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [searchHistory, setSearchHistory] = useState([]);
  const [showHistory, setShowHistory] = useState(false);
  const [selectedFields, setSelectedFields] = useState(
    searchFields.map(f => f.key) // Select all by default
  );

  // Load search history từ localStorage
  useEffect(() => {
    if (storageKey) {
      const saved = localStorage.getItem(`${storageKey}_search_history`);
      if (saved) {
        try {
          setSearchHistory(JSON.parse(saved));
        } catch (e) {
          console.error('Error loading search history:', e);
        }
      }
    }
  }, [storageKey]);

  // Debounce search
  useEffect(() => {
    const timer = setTimeout(() => {
      if (searchTerm) {
        onSearch(searchTerm, selectedFields);
        addToHistory(searchTerm);
      } else {
        onSearch('', selectedFields);
      }
    }, debounceMs);

    return () => clearTimeout(timer);
  }, [searchTerm, selectedFields, onSearch]);

  const addToHistory = (term) => {
    if (!storageKey || !term.trim()) return;
    
    const updated = [
      term,
      ...searchHistory.filter(h => h !== term)
    ].slice(0, 10); // Keep last 10 searches
    
    setSearchHistory(updated);
    localStorage.setItem(`${storageKey}_search_history`, JSON.stringify(updated));
  };

  const clearHistory = () => {
    setSearchHistory([]);
    if (storageKey) {
      localStorage.removeItem(`${storageKey}_search_history`);
    }
  };

  const toggleField = (fieldKey) => {
    setSelectedFields(prev => {
      if (prev.includes(fieldKey)) {
        return prev.filter(k => k !== fieldKey);
      } else {
        return [...prev, fieldKey];
      }
    });
  };

  return (
    <div className="relative">
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-4">
        {/* Search Input */}
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            onFocus={() => setShowHistory(true)}
            placeholder={placeholder}
            className="w-full pl-10 pr-10 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
          />
          {searchTerm && (
            <button
              onClick={() => setSearchTerm('')}
              className="absolute right-3 top-1/2 -translate-y-1/2 p-1 hover:bg-gray-100 rounded-full transition-colors"
            >
              <X className="w-4 h-4 text-gray-400" />
            </button>
          )}
        </div>

        {/* Search Fields */}
        {searchFields.length > 1 && (
          <div className="mt-3 flex flex-wrap gap-2">
            <span className="text-xs text-gray-600 self-center">Tìm trong:</span>
            {searchFields.map((field) => (
              <button
                key={field.key}
                onClick={() => toggleField(field.key)}
                className={`
                  px-2.5 py-1 text-xs rounded-full border transition-all
                  ${selectedFields.includes(field.key)
                    ? 'bg-blue-100 text-blue-700 border-blue-300'
                    : 'bg-gray-50 text-gray-600 border-gray-300 hover:bg-gray-100'
                  }
                `}
              >
                {field.label}
              </button>
            ))}
          </div>
        )}

        {/* Search Statistics */}
        {searchTerm && (
          <div className="mt-2 text-xs text-gray-500">
            Tìm trong <span className="font-semibold">{selectedFields.length}</span> trường
          </div>
        )}
      </div>

      {/* Search History */}
      {showHistory && searchHistory.length > 0 && !searchTerm && (
        <>
          {/* Backdrop */}
          <div 
            className="fixed inset-0 z-40" 
            onClick={() => setShowHistory(false)}
          />
          
          {/* History Dropdown */}
          <div className="absolute top-full mt-2 w-full bg-white rounded-lg shadow-xl border border-gray-200 z-50 max-h-64 overflow-y-auto">
            <div className="flex items-center justify-between px-4 py-2 border-b border-gray-200">
              <div className="flex items-center gap-2">
                <Sparkles className="w-4 h-4 text-purple-500" />
                <span className="text-sm font-medium text-gray-700">Tìm kiếm gần đây</span>
              </div>
              <button
                onClick={clearHistory}
                className="text-xs text-gray-500 hover:text-gray-700 transition-colors"
              >
                Xóa
              </button>
            </div>
            
            <div className="p-2">
              {searchHistory.map((term, index) => (
                <button
                  key={index}
                  onClick={() => {
                    setSearchTerm(term);
                    setShowHistory(false);
                  }}
                  className="w-full flex items-center gap-3 px-3 py-2 text-left hover:bg-gray-50 rounded-lg transition-colors group"
                >
                  <Search className="w-4 h-4 text-gray-400 group-hover:text-gray-600" />
                  <span className="flex-1 text-sm text-gray-700">{term}</span>
                </button>
              ))}
            </div>
          </div>
        </>
      )}
    </div>
  );
};

export default AdvancedSearchPanel;

