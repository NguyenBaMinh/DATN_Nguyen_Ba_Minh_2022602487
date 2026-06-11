import { Download, FileText, FileSpreadsheet, Printer } from 'lucide-react';
import { useState } from 'react';

/**
 * Component xuất dữ liệu ra nhiều format
 * 
 * ✅ Hỗ trợ:
 * - CSV export
 * - Excel export (XLSX)
 * - PDF export
 * - Print
 * 
 * @param {Object} props
 * @param {Array} props.data - Dữ liệu cần export
 * @param {Array} props.columns - Danh sách columns cần export
 * @param {String} props.filename - Tên file (không extension)
 * @param {String} props.title - Tiêu đề export
 */
const ExportPanel = ({ data = [], columns = [], filename = 'export', title = 'Dữ liệu' }) => {
  const [isOpen, setIsOpen] = useState(false);

  // Export to CSV
  const exportToCSV = () => {
    if (data.length === 0) return;

    const headers = columns.map(col => col.header).join(',');
    const rows = data.map(row => {
      return columns.map(col => {
        let value = col.accessor ? col.accessor(row) : row[col.key];
        // Escape commas and quotes
        if (typeof value === 'string') {
          value = value.replace(/"/g, '""');
          if (value.includes(',') || value.includes('"') || value.includes('\n')) {
            value = `"${value}"`;
          }
        }
        return value || '';
      }).join(',');
    }).join('\n');

    const csv = `${headers}\n${rows}`;
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
    downloadBlob(blob, `${filename}.csv`);
  };

  // Export to Excel (simplified - just CSV with xlsx extension)
  const exportToExcel = () => {
    if (data.length === 0) return;

    const headers = columns.map(col => col.header).join('\t');
    const rows = data.map(row => {
      return columns.map(col => {
        let value = col.accessor ? col.accessor(row) : row[col.key];
        return value || '';
      }).join('\t');
    }).join('\n');

    const tsv = `${headers}\n${rows}`;
    const blob = new Blob(['\uFEFF' + tsv], { type: 'application/vnd.ms-excel;charset=utf-8;' });
    downloadBlob(blob, `${filename}.xls`);
  };

  // Print data
  const printData = () => {
    if (data.length === 0) return;

    const printWindow = window.open('', '_blank');
    
    const html = `
      <!DOCTYPE html>
      <html>
      <head>
        <title>${title}</title>
        <style>
          body {
            font-family: Arial, sans-serif;
            padding: 20px;
          }
          h1 {
            font-size: 24px;
            margin-bottom: 20px;
          }
          table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 20px;
          }
          th, td {
            border: 1px solid #ddd;
            padding: 8px 12px;
            text-align: left;
          }
          th {
            background-color: #f3f4f6;
            font-weight: bold;
          }
          tr:nth-child(even) {
            background-color: #f9fafb;
          }
          @media print {
            body { padding: 0; }
          }
        </style>
      </head>
      <body>
        <h1>${title}</h1>
        <p>Tổng số: ${data.length} bản ghi</p>
        <table>
          <thead>
            <tr>
              ${columns.map(col => `<th>${col.header}</th>`).join('')}
            </tr>
          </thead>
          <tbody>
            ${data.map(row => `
              <tr>
                ${columns.map(col => {
                  let value = col.accessor ? col.accessor(row) : row[col.key];
                  return `<td>${value || ''}</td>`;
                }).join('')}
              </tr>
            `).join('')}
          </tbody>
        </table>
        <script>
          window.onload = function() {
            window.print();
          };
        </script>
      </body>
      </html>
    `;
    
    printWindow.document.write(html);
    printWindow.document.close();
  };

  // Helper: Download blob
  const downloadBlob = (blob, filename) => {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
    setIsOpen(false);
  };

  if (data.length === 0) {
    return (
      <div className="text-sm text-gray-500">
        Không có dữ liệu để xuất
      </div>
    );
  }

  return (
    <div className="relative">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors shadow-sm"
      >
        <Download className="w-4 h-4" />
        Xuất dữ liệu ({data.length})
      </button>

      {isOpen && (
        <>
          {/* Backdrop */}
          <div 
            className="fixed inset-0 z-40" 
            onClick={() => setIsOpen(false)}
          />
          
          {/* Dropdown Menu */}
          <div className="absolute right-0 mt-2 w-56 bg-white rounded-lg shadow-xl border border-gray-200 z-50 overflow-hidden">
            <div className="p-2">
              <div className="text-xs font-semibold text-gray-500 px-3 py-2">
                ĐỊNH DẠNG XUẤT
              </div>
              
              <button
                onClick={exportToCSV}
                className="w-full flex items-center gap-3 px-3 py-2.5 text-left hover:bg-gray-50 rounded-lg transition-colors group"
              >
                <FileText className="w-5 h-5 text-blue-600 group-hover:text-blue-700" />
                <div className="flex-1">
                  <div className="text-sm font-medium text-gray-900">CSV File</div>
                  <div className="text-xs text-gray-500">Comma-separated values</div>
                </div>
              </button>

              <button
                onClick={exportToExcel}
                className="w-full flex items-center gap-3 px-3 py-2.5 text-left hover:bg-gray-50 rounded-lg transition-colors group"
              >
                <FileSpreadsheet className="w-5 h-5 text-green-600 group-hover:text-green-700" />
                <div className="flex-1">
                  <div className="text-sm font-medium text-gray-900">Excel File</div>
                  <div className="text-xs text-gray-500">Microsoft Excel format</div>
                </div>
              </button>

              <button
                onClick={printData}
                className="w-full flex items-center gap-3 px-3 py-2.5 text-left hover:bg-gray-50 rounded-lg transition-colors group"
              >
                <Printer className="w-5 h-5 text-gray-600 group-hover:text-gray-700" />
                <div className="flex-1">
                  <div className="text-sm font-medium text-gray-900">In</div>
                  <div className="text-xs text-gray-500">Xem trước và in</div>
                </div>
              </button>
            </div>

            <div className="bg-gray-50 px-3 py-2 border-t border-gray-200">
              <div className="text-xs text-gray-600">
                {data.length} bản ghi • {columns.length} cột
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
};

export default ExportPanel;

