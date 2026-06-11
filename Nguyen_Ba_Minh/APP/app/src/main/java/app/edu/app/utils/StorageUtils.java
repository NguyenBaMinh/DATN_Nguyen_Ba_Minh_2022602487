package app.edu.app.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utility class for handling Firebase Storage operations
 * Optimized to prevent UI freezing
 */
public class StorageUtils {
    private static final String TAG = "StorageUtils";
    private static final FirebaseStorage storage = FirebaseStorage.getInstance();
    private static final ExecutorService executor = Executors.newFixedThreadPool(3);

    /**
     * Maximum image size for upload (in pixels)
     */
    private static final int MAX_IMAGE_SIZE = 1024;

    /**
     * JPEG quality for compressing images (0-100)
     */
    private static final int JPEG_QUALITY = 80;

    /**
     * Interface for upload completion callbacks
     */
    public interface OnUploadCompleteListener {
        void onSuccess(String downloadUrl);
        void onFailure(Exception e);
    }

    /**
     * Upload image to Firebase Storage asynchronously
     */
    public static void uploadImage(byte[] imageData, String path, OnUploadCompleteListener listener) {
        if (imageData == null || imageData.length == 0) {
            if (listener != null) {
                listener.onFailure(new IllegalArgumentException("Image data is null or empty"));
            }
            return;
        }

        // Process image in background thread
        executor.execute(() -> {
            try {
                // Compress and resize image before upload
                byte[] compressedData = compressImage(imageData);

                // Get storage reference
                StorageReference storageRef = storage.getReference().child(path);

                // Upload image
                UploadTask uploadTask = storageRef.putBytes(compressedData);
                uploadTask.continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return storageRef.getDownloadUrl();
                }).addOnSuccessListener(uri -> {
                    if (listener != null) {
                        listener.onSuccess(uri.toString());
                    }
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Image upload failed", e);
                    if (listener != null) {
                        listener.onFailure(e);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error processing image for upload", e);
                if (listener != null) {
                    listener.onFailure(e);
                }
            }
        });
    }

    /**
     * Delete image from Firebase Storage
     */
    public static void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        try {
            StorageReference storageRef = storage.getReferenceFromUrl(imageUrl);
            storageRef.delete()
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Image deleted successfully"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to delete image", e));
        } catch (Exception e) {
            Log.e(TAG, "Error parsing storage URL", e);
        }
    }

    /**
     * Compress and resize image to reduce storage and bandwidth usage
     */
    public static byte[] compressImage(byte[] imageData) {
        if (imageData == null || imageData.length == 0) {
            return null;
        }

        try {
            // Decode image size first
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(imageData, 0, imageData.length, options);

            // Calculate scaling factor
            int width = options.outWidth;
            int height = options.outHeight;
            float scaleFactor = 1.0f;

            if (width > MAX_IMAGE_SIZE || height > MAX_IMAGE_SIZE) {
                scaleFactor = Math.max(width, height) / (float) MAX_IMAGE_SIZE;
            }

            // Decode with scaling
            options = new BitmapFactory.Options();
            options.inSampleSize = Math.round(scaleFactor);
            options.inPreferredConfig = Bitmap.Config.RGB_565; // Use RGB_565 to reduce memory usage

            Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length, options);

            // Compress to JPEG
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream);

            // Recycle bitmap to free memory
            bitmap.recycle();

            return outputStream.toByteArray();
        } catch (Exception e) {
            Log.e(TAG, "Error compressing image", e);
            return imageData; // Return original data if compression fails
        }
    }

    /**
     * Download image from Firebase Storage asynchronously
     */
    public static void downloadImage(String imageUrl, OnDownloadCompleteListener listener) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            if (listener != null) {
                listener.onFailure(new IllegalArgumentException("Image URL is null or empty"));
            }
            return;
        }

        // Check cache first
        byte[] cachedData = ImageCache.getBlobFromCache(imageUrl);
        if (cachedData != null) {
            if (listener != null) {
                listener.onSuccess(cachedData);
            }
            return;
        }

        try {
            StorageReference storageRef = storage.getReferenceFromUrl(imageUrl);

            // Download up to 2MB (adjust as needed)
            final long TWO_MEGABYTES = 2 * 1024 * 1024;
            storageRef.getBytes(TWO_MEGABYTES)
                    .addOnSuccessListener(bytes -> {
                        // Cache the downloaded image
                        ImageCache.addBlobToCache(imageUrl, bytes);

                        if (listener != null) {
                            listener.onSuccess(bytes);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to download image", e);
                        if (listener != null) {
                            listener.onFailure(e);
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error parsing storage URL", e);
            if (listener != null) {
                listener.onFailure(e);
            }
        }
    }

    /**
     * Interface for download completion callbacks
     */
    public interface OnDownloadCompleteListener {
        void onSuccess(byte[] imageData);
        void onFailure(Exception e);
    }
}