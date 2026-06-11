package app.edu.app.database;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

/**
 * Singleton class to provide Firebase instances throughout the app
 */
public class FirebaseConfig {
    private static volatile FirebaseConfig instance;
    private final FirebaseFirestore firestore;
    private final FirebaseStorage storage;

    private FirebaseConfig() {
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public static FirebaseConfig getInstance() {
        if (instance == null) {
            synchronized (FirebaseConfig.class) {
                if (instance == null) {
                    instance = new FirebaseConfig();
                }
            }
        }
        return instance;
    }

    public FirebaseFirestore getFirestore() {
        return firestore;
    }

    public FirebaseStorage getStorage() {
        return storage;
    }

    public StorageReference getStorageReference() {
        return storage.getReference();
    }

    // Collection constants to avoid typos
    public static final String COLLECTION_BAN = "Ban";
    public static final String COLLECTION_NGUOI_DUNG = "NguoiDung";
    public static final String COLLECTION_LOAI_HANG = "LoaiHang";
    public static final String COLLECTION_HANG_HOA = "HangHoa";
    public static final String COLLECTION_HOA_DON = "HoaDon";
    public static final String COLLECTION_HOA_DON_CHI_TIET = "HoaDonChiTiet";
    public static final String COLLECTION_HOA_DON_MANG_VE = "HoaDonMangVe";
    public static final String COLLECTION_THONG_BAO = "ThongBao";
    public static final String COLLECTION_DAT_BAN = "DatBan";
}