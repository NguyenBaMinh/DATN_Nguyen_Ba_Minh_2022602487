package app.edu.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.LruCache;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;

import app.edu.app.interfaces.OnUrlFetchedListener;

/**
 * Cache utility for images to improve performance and reduce network usage
 */
public class ImageCache {
    private static final String TAG = "ImageCache";

    // Memory cache
    private static LruCache<String, Bitmap> memoryCache;
    private static LruCache<String, byte[]> blobCache;

    // Disk cache directory
    private static File cacheDir;

    private static SharedPreferences urlCache;

    /**
     * Initialize the cache
     */
    public static void initialize(Context context) {
        if (memoryCache != null) {
            return; // Already initialized
        }

        // Calculate available memory
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8 of available memory for bitmap cache
        final int cacheSize = maxMemory / 8;

        urlCache = context.getSharedPreferences("image_urls", Context.MODE_PRIVATE);


        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // Size is measured in kilobytes
                return bitmap.getByteCount() / 1024;
            }
        };

        // Use 1/8 of available memory for blob cache
        blobCache = new LruCache<String, byte[]>(cacheSize) {
            @Override
            protected int sizeOf(String key, byte[] data) {
                return data.length / 1024;
            }
        };

        // Create disk cache directory
        cacheDir = new File(context.getCacheDir(), "images");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
    }

    /**
     * Add a bitmap to the memory cache
     */
    public static void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (memoryCache == null || key == null || bitmap == null) {
            return;
        }

        if (getBitmapFromMemoryCache(key) == null) {
            memoryCache.put(key, bitmap);
        }
    }

    public static void addUrlToCache(String key, String url) {
        if (key == null || url == null || url.isEmpty()) {
            return;
        }

        // Initialize SharedPreferences lazily if needed
        if (urlCache == null) {
            // This requires a context, so it might need to be initialized elsewhere
            // or passed as a parameter. For now, we'll assume it's initialized in initialize()
            Log.w(TAG, "URL cache not initialized properly");
            return;
        }

        // Store the URL mapping in SharedPreferences
        urlCache.edit().putString(key, url).apply();

        // Log for debugging
        Log.d(TAG, "Added URL to cache: " + key + " -> " + url);
    }

    /**
     * Get an image URL from the cache
     * @param key The identifier for the image
     * @return The URL or null if not found
     */
    public static void getUrlFromCache(String key, OnUrlFetchedListener listener) {
        if (urlCache == null || key == null) {
            listener.onUrlFetched(null);
            return;
        }

        // Check if URL exists in cache
        String cachedUrl = urlCache.getString(key, null);

        // If URL found in cache, return it immediately
        if (cachedUrl != null) {
            listener.onUrlFetched(cachedUrl);
            return;
        }

        // Parse the key to get the folder and filename
        if(!key.contains("_")) {
           key = "hanghoa_"+key;
        }
        String[] parts = key.split("_");
        if (parts.length != 2) {
            listener.onUrlFetched(null);
            return;
        }

        String folder = parts[0]; // nguoidung, hanghoa, loaihang
        String fileName = parts[1] + ".jpg"; // 1.jpg, 2.jpg, etc.

        // Build the path for Firebase Storage
        String storagePath = folder + "/" + fileName;

        // Fetch from Firebase Storage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference imageRef = storageRef.child(storagePath);

        String finalKey = key;
        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            String url = uri.toString();
            // Save the URL to cache for future use
            urlCache.edit().putString(finalKey, url).apply();
            listener.onUrlFetched(url);
        }).addOnFailureListener(e -> {
            Log.e("Storage", "Error getting download URL for " + storagePath, e);
            listener.onUrlFetched(null);
        });
    }

    /**
     * Add a blob (byte array) to the memory cache
     */
    public static void addBitmapToMemoryCache(String key, byte[] imageData) {
        if (memoryCache == null || key == null || imageData == null) {
            return;
        }

        if (getBitmapFromMemoryCache(key) == null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
            if (bitmap != null) {
                memoryCache.put(key, bitmap);
            }
        }
    }

    /**
     * Add a blob to both memory and disk cache
     */
    public static void addBlobToCache(String key, byte[] data) {
        if (key == null || data == null) {
            return;
        }

        // Add to memory cache
        blobCache.put(key, data);

        // Add to disk cache in background
        new Thread(() -> {
            try {
                String hashedKey = hashKey(key);
                File file = new File(cacheDir, hashedKey);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(data);
                fos.close();
            } catch (Exception e) {
                Log.e(TAG, "Error saving image to disk cache", e);
            }
        }).start();
    }

    /**
     * Get a bitmap from the memory cache
     */
    public static Bitmap getBitmapFromMemoryCache(String key) {
        if (memoryCache == null || key == null) {
            return null;
        }
        return memoryCache.get(key);
    }

    /**
     * Get a blob from cache (memory or disk)
     */
    public static byte[] getBlobFromCache(String key) {
        if (key == null) {
            return null;
        }

        // Check memory cache first
        byte[] data = blobCache.get(key);
        if (data != null) {
            return data;
        }

        // Check disk cache
        try {
            String hashedKey = hashKey(key);
            File file = new File(cacheDir, hashedKey);
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[(int) file.length()];
                fis.read(buffer);
                fis.close();

                // Add to memory cache for faster access next time
                blobCache.put(key, buffer);

                return buffer;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading image from disk cache", e);
        }

        return null;
    }

    /**
     * Convert bitmap to byte array
     */
    public static byte[] bitmapToByteArray(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
    }

    /**
     * Remove a bitmap from memory cache
     */
    public static void removeBitmapFromMemoryCache(String key) {
        if (memoryCache != null && key != null) {
            memoryCache.remove(key);
        }
    }

    /**
     * Clear all caches
     */
    public static void clearCache() {
        if (memoryCache != null) {
            memoryCache.evictAll();
        }

        if (blobCache != null) {
            blobCache.evictAll();
        }

        // Clear disk cache in background
        new Thread(() -> {
            if (cacheDir != null && cacheDir.exists()) {
                File[] files = cacheDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
            }
        }).start();
    }

    /**
     * Convert URL or key to a valid filename
     */
    private static String hashKey(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(key.getBytes());
            byte[] bytes = digest.digest();

            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            // If hashing fails, use a simplified approach
            return key.replaceAll("[^a-zA-Z0-9]", "_");
        }
    }

}