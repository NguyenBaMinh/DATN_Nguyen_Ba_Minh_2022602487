import { useState, useEffect } from 'react';
import { getImageUrl } from '../utils/imageUtils';

/**
 * Component hiển thị hình ảnh tự động load từ Storage nếu cần
 * @param {Object} item - Item cần hiển thị ảnh
 * @param {string} type - 'hanghoa', 'loaihang', hoặc 'nguoidung'
 * @param {string} alt - Alt text cho image
 * @param {string} className - CSS classes
 * @param {ReactNode} fallback - Component hiển thị khi không có ảnh
 */
const ImageDisplay = ({ item, type, alt = '', className = '', fallback = null }) => {
  const [imageUrl, setImageUrl] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  useEffect(() => {
    if (!item) {
      setImageUrl(null);
      setLoading(false);
      return;
    }

    // Nếu đã có URL thì dùng luôn
    if (item.hinhAnhUrl) {
      console.log(`[ImageDisplay] Using hinhAnhUrl from item:`, item.hinhAnhUrl);
      setImageUrl(item.hinhAnhUrl);
      setLoading(false);
      return;
    }

    // Luôn thử load từ Storage (vì có thể có ảnh trong Storage nhưng hasImage = false trong DB)
    setLoading(true);
    console.log(`[ImageDisplay] Loading image from Storage for item:`, { item, type });

    getImageUrl(item, type)
      .then((url) => {
        console.log(`[ImageDisplay] Successfully loaded image URL:`, url);
        if (url) {
          setImageUrl(url);
          setError(false);
        } else {
          console.warn(`[ImageDisplay] No image URL found for:`, { item, type });
          setImageUrl(null);
          setError(true);
        }
      })
      .catch((err) => {
        console.error('[ImageDisplay] Error loading image:', err);
        setImageUrl(null);
        setError(true);
      })
      .finally(() => {
        setLoading(false);
      });
  }, [item, type]);

  if (loading) {
    return (
      <div className={`bg-gray-200 animate-pulse ${className}`}>
        {fallback || <div className="w-full h-full flex items-center justify-center text-gray-400">Đang tải...</div>}
      </div>
    );
  }

  if (error || !imageUrl) {
    return (
      fallback || (
        <div className={`bg-gray-100 flex items-center justify-center ${className}`}>
          <span className="text-gray-400 text-sm">Không có hình ảnh</span>
        </div>
      )
    );
  }

  return <img src={imageUrl} alt={alt} className={className} onError={() => setError(true)} />;
};

export default ImageDisplay;

