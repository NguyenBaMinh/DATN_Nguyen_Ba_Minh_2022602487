package app.edu.app.dao;

import android.net.Uri;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.concurrent.CompletableFuture;

import app.edu.app.database.FirebaseConfig;

/**
 * Base class for all DAO objects providing common Firebase operations
 */
public abstract class BaseDAO {
    protected final FirebaseFirestore firestore;
    protected final String collectionName;
    protected final CollectionReference collectionRef;

    public BaseDAO(String collectionName) {
        this.firestore = FirebaseConfig.getInstance().getFirestore();
        this.collectionName = collectionName;
        this.collectionRef = firestore.collection(collectionName);
    }

    /**
     * Helper method to upload image to Firebase Storage
     *
     * @param imageData byte array of the image
     * @param path      path to save image in Firebase Storage
     * @return CompletableFuture with download URL
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    protected CompletableFuture<String> uploadImage(byte[] imageData, String path) {
        if (imageData == null || imageData.length == 0) {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.complete(null);
            return future;
        }

        CompletableFuture<String> future = new CompletableFuture<>();
        StorageReference imageRef = FirebaseConfig.getInstance().getStorageReference().child(path);
        UploadTask uploadTask = imageRef.putBytes(imageData);

        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful() && task.getException() != null) {
                throw task.getException();
            }
            return imageRef.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Uri downloadUri = task.getResult();
                future.complete(downloadUri.toString());
            } else {
                future.completeExceptionally(task.getException());
            }
        });

        return future;
    }

    /**
     * Helper to convert a Task to a CompletableFuture
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    protected <T> CompletableFuture<T> toCompletableFuture(Task<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        task.addOnSuccessListener(future::complete)
                .addOnFailureListener(future::completeExceptionally);
        return future;
    }
}