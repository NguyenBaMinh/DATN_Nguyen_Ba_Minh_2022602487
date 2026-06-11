package app.edu.app.interfaces;


import com.google.firebase.database.DataSnapshot;

public interface RealtimeUpdateCallback {
    void onDataChanged(DataSnapshot dataSnapshot);
}