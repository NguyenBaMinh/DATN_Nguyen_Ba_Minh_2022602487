package app.edu.app.database;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.StorageReference;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import app.edu.app.R;
import app.edu.app.utils.ImageToByte;

/**
 * Helper class to initialize Firebase with sample data
 */
public class FirebaseInitializer {
    private static final String TAG = "FirebaseInitializer";
    private final Context context;
    private final FirebaseFirestore firestore;
    private final StorageReference storageRef;

    public FirebaseInitializer(Context context) {
        this.context = context;
        this.firestore = FirebaseConfig.getInstance().getFirestore();
        this.storageRef = FirebaseConfig.getInstance().getStorageReference();
    }

    /**
     * Initialize Firebase with sample data
     */
    public void initSampleData() {
        // Check if data already exists
        firestore.collection(FirebaseConfig.COLLECTION_NGUOI_DUNG)
                .document("admin")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        // Data doesn't exist, initialize it
                        initializeBan();
                        initializeNguoiDung();
                        initializeLoaiHang();
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking if sample data exists", e));
    }

    /**
     * Initialize tables (Ban)
     */
    private void initializeBan() {
        for (int i = 1; i <= 12; i++) {
            Map<String, Object> ban = new HashMap<>();
            ban.put("trangThai", 0);

            firestore.collection(FirebaseConfig.COLLECTION_BAN)
                    .document(String.valueOf(i))
                    .set(ban)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Ban added successfully"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error adding ban", e));
        }
    }

    /**
     * Initialize users (NguoiDung)
     */
    private void initializeNguoiDung() {
        // Admin user
        Map<String, Object> admin = new HashMap<>();
        admin.put("hoVaTen", "ADMIN");
        admin.put("ngaySinh", new Date(103, 0, 1)); // 2003-01-01
        admin.put("email", "admin@gmail.com");
        admin.put("chucVu", "Admin");
        admin.put("gioiTinh", "Nam");
        admin.put("matKhau", "1212");

        uploadImageAndAddUser("admin", admin, R.drawable.avatar_user_md);

        // Staff
        Map<String, Object> staff1 = new HashMap<>();
        staff1.put("hoVaTen", "Nguyễn Viết Tín");
        staff1.put("ngaySinh", new Date(103, 0, 1)); // 2003-01-01
        staff1.put("email", "tinthq@gmail.com");
        staff1.put("chucVu", "NhanVien");
        staff1.put("gioiTinh", "Nam");
        staff1.put("matKhau", "1212");

        uploadImageAndAddUser("nhanvien", staff1, R.drawable.avatar_user_md);

        // Additional staff
        Map<String, Object> staff2 = new HashMap<>();
        staff2.put("hoVaTen", "Trần Hồ Quốc An");
        staff2.put("ngaySinh", new Date(103, 0, 1)); // 2003-01-01
        staff2.put("email", "anthq@gmail.com");
        staff2.put("chucVu", "NhanVien");
        staff2.put("gioiTinh", "Nam");
        staff2.put("matKhau", "1212");

        uploadImageAndAddUser("ND2", staff2, R.drawable.avatar_user_md);

        Map<String, Object> staff3 = new HashMap<>();
        staff3.put("hoVaTen", "Hồ Minh Phú");
        staff3.put("ngaySinh", new Date(103, 0, 1)); // 2003-01-01
        staff3.put("email", "phuhm@gmail.com");
        staff3.put("chucVu", "NhanVien");
        staff3.put("gioiTinh", "Nam");
        staff3.put("matKhau", "1212");

        uploadImageAndAddUser("ND3", staff3, R.drawable.avatar_user_md);

        // User
        Map<String, Object> user = new HashMap<>();
        user.put("hoVaTen", "Nguyễn Anh");
        user.put("ngaySinh", new Date(103, 0, 1)); // 2003-01-01
        user.put("email", "NADev@gmail.com");
        user.put("chucVu", "User");
        user.put("gioiTinh", "Nam");
        user.put("matKhau", "1212");

        uploadImageAndAddUser("US1", user, R.drawable.avatar_user_md);
    }

    /**
     * Helper to upload image and add user
     */
    private void uploadImageAndAddUser(String userId, Map<String, Object> userData, int imageResId) {
        byte[] imageData = ImageToByte.drawableToByte(context, imageResId);

        // Upload image to storage
        String imagePath = "nguoidung/" + userId + ".jpg";
        StorageReference imageRef = storageRef.child(imagePath);

        imageRef.putBytes(imageData)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return imageRef.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> {
                    // Add image URL to user data
                    userData.put("hinhAnhUrl", uri.toString());

                    // Add user to Firestore
                    firestore.collection(FirebaseConfig.COLLECTION_NGUOI_DUNG)
                            .document(userId)
                            .set(userData)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "User added successfully: " + userId))
                            .addOnFailureListener(e -> Log.e(TAG, "Error adding user", e));
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error uploading image", e));
    }

    /**
     * Initialize product categories (LoaiHang)
     */
    private void initializeLoaiHang() {
        // Array of product category data
        int[] imageResIds = {
                R.drawable.sample_data_loai_hang_caphe,
                R.drawable.sample_data_loai_hang_nuocep,
                R.drawable.sample_data_loai_hang_soda,
                R.drawable.sample_data_loai_hang_trasua
        };

        String[] categoryNames = {
                "Cà phê",
                "Nước ép",
                "Soda",
                "Trà sữa"
        };

        // Add categories to Firestore
        for (int i = 0; i < categoryNames.length; i++) {
            final int categoryId = i + 1;
            final int imageResId = imageResIds[i];
            final String categoryName = categoryNames[i];

            Map<String, Object> category = new HashMap<>();
            category.put("tenLoai", categoryName);

            uploadImageAndAddCategory(categoryId, category, imageResId);
        }

        // Once categories are added, initialize products
        firestore.collection(FirebaseConfig.COLLECTION_LOAI_HANG)
                .document("4") // Wait for the last category to be added
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // All categories added, now add products
                        initializeHangHoa();
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking if categories exist", e));
    }

    /**
     * Helper to upload image and add category
     */
    private void uploadImageAndAddCategory(int categoryId, Map<String, Object> categoryData, int imageResId) {
        byte[] imageData = ImageToByte.drawableToByte(context, imageResId);

        // Upload image to storage
        String imagePath = "loaihang/" + categoryId + ".jpg";
        StorageReference imageRef = storageRef.child(imagePath);

        imageRef.putBytes(imageData)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return imageRef.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> {
                    // Add image URL to category data
                    categoryData.put("hinhAnhUrl", uri.toString());

                    // Add category to Firestore
                    firestore.collection(FirebaseConfig.COLLECTION_LOAI_HANG)
                            .document(String.valueOf(categoryId))
                            .set(categoryData)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Category added successfully: " + categoryId))
                            .addOnFailureListener(e -> Log.e(TAG, "Error adding category", e));
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error uploading image", e));
    }

    /**
     * Initialize products (HangHoa)
     */
    private void initializeHangHoa() {
        // Coffee products (Category 1)
        addProduct(1, "Cà phê máy", R.drawable.sample_data_hanghoa_cfmay, 15000, 1, 1);
        addProduct(2, "Cà phê phin", R.drawable.sample_data_hanghoa_cfphin, 12000, 1, 1);
        addProduct(3, "Cà phê sài gòn", R.drawable.sample_data_hanghoa_cfsaigon, 20000, 1, 1);
        addProduct(4, "Cà phê bọt biển", R.drawable.sample_data_hanghoa_cfbotbien, 25000, 1, 0);

        // Juice products (Category 2)
        addProduct(5, "Nước ép cam", R.drawable.sample_data_hanghoa_epcam, 27000, 2, 1);
        addProduct(6, "Nước ép dứa", R.drawable.sample_data_hanghoa_epdua, 25000, 2, 0);
        addProduct(7, "Nước ép ổi", R.drawable.sample_data_hanghoa_epoi, 23000, 2, 0);
        addProduct(8, "Chanh đá", R.drawable.sample_data_hanghoa_chanhda, 20000, 2, 1);

        // Soda products (Category 3)
        addProduct(9, "Soda bạc hà", R.drawable.sample_data_loai_hang_soda_bacha, 33000, 3, 1);
        addProduct(10, "Soda việt quất", R.drawable.sample_data_loai_hang_soda_vietquat, 35000, 3, 0);
        addProduct(11, "Soda trái cây", R.drawable.sample_data_loai_hang_soda_traicay, 35000, 3, 1);

        // Milk tea products (Category 4)
        addProduct(12, "Trà sữa khoai môn", R.drawable.sample_data_hanghoa_trasuamon, 23000, 4, 1);
        addProduct(13, "Trà sữa thái xanh", R.drawable.sample_data_hanghoa_trasuathaixanh, 24000, 4, 1);
        addProduct(14, "Trà sữa truyền thống", R.drawable.sample_data_hanghoa_trasuatruyenthong, 25000, 4, 1);
    }

    /**
     * Helper to add a product with image
     */
    private void addProduct(int productId, String name, int imageResId, int price, int categoryId, int status) {
        byte[] imageData = ImageToByte.drawableToByte(context, imageResId);

        // Upload image to storage
        String imagePath = "hanghoa/" + productId + ".jpg";
        StorageReference imageRef = storageRef.child(imagePath);

        imageRef.putBytes(imageData)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return imageRef.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> {
                    // Create product data
                    Map<String, Object> product = new HashMap<>();
                    product.put("tenHangHoa", name);
                    product.put("giaTien", price);
                    product.put("maLoai", categoryId);
                    product.put("trangThai", status);
                    product.put("hinhAnhUrl", uri.toString());

                    // Add product to Firestore
                    firestore.collection(FirebaseConfig.COLLECTION_HANG_HOA)
                            .document(String.valueOf(productId))
                            .set(product)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Product added successfully: " + name))
                            .addOnFailureListener(e -> Log.e(TAG, "Error adding product", e));
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error uploading image", e));
    }
}