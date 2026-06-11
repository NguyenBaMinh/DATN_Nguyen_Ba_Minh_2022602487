package app.edu.app.fragment;

import android.content.DialogInterface;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import java.util.ArrayList;

import app.edu.app.R;
import app.edu.app.adapter.ThongBaoAdapter;
import app.edu.app.dao.ThongBaoDAO;
import app.edu.app.interfaces.ItemThongBaoOnClick;
import app.edu.app.model.ThongBao;
import app.edu.app.utils.MyToast;

/**
 * Fragment hiển thị danh sách thông báo
 * 
 * ✅ 100% FIREBASE DIRECT MODE
 * - Tất cả dữ liệu đọc (READ) đều load TRỰC TIẾP từ Firebase Realtime Database
 * - Không sử dụng SQLite để đọc dữ liệu
 * - Các thao tác ghi (UPDATE/DELETE) vẫn dùng DAO (sẽ tự sync lên Firebase)
 */
public class MeseegerFragment extends Fragment {
    RecyclerView recyclerViewThongBao;
    ThongBaoDAO thongBaoDAO;
    private ThongBaoAdapter adapterNotification; // Lưu adapter để update sau
    
    /**
     * Lấy mã người dùng hiện tại
     */
    private String getMaNguoiDung() {
        if (getActivity() != null && getActivity() instanceof app.edu.app.MainActivity) {
            app.edu.app.MainActivity mainActivity = (app.edu.app.MainActivity) getActivity();
            String keyUser = mainActivity.getKeyUser();
            return (keyUser != null && !keyUser.isEmpty()) ? keyUser : null;
        }
        return null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_meseeger, container, false);
        initView(view);
        thongBaoDAO = new ThongBaoDAO(getContext());
        loadListNotification();

        return view;
    }

    private void initView(View view) {
        recyclerViewThongBao = view.findViewById(R.id.recyclerViewThongBao);
    }

    /**
     * Load danh sách thông báo TRỰC TIẾP từ Firebase
     * ✅ 100% Firebase Direct - Không sử dụng SQLite để đọc
     * ✅ Lọc theo user - Chỉ hiển thị thông báo của user hiện tại + thông báo chung
     */
    private void loadListNotification() {
        Log.d("MeseegerFragment", "╔═══════════════════════════════╗");
        Log.d("MeseegerFragment", "║ LOAD NOTIFICATION FROM FIREBASE ║");
        Log.d("MeseegerFragment", "╚═══════════════════════════════╝");
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        recyclerViewThongBao.setLayoutManager(layoutManager);
        
        // Lấy mã người dùng hiện tại
        String maNguoiDung = getMaNguoiDung();
        Log.d("MeseegerFragment", "► User hiện tại: " + (maNguoiDung != null ? maNguoiDung : "Guest"));

        // ✅ Load TRỰC TIẾP từ Firebase (không dùng SQLite)
        thongBaoDAO.getAllFromFirebaseDirect(new ThongBaoDAO.OnThongBaoListListener() {
            @Override
            public void onListReceived(ArrayList<ThongBao> allNotifications) {
                Log.d("MeseegerFragment", "✓ Đã load " + allNotifications.size() + " thông báo từ Firebase");
                
                // Lọc thông báo cho user hiện tại
                ArrayList<ThongBao> filteredNotifications = new ArrayList<>();
                for (ThongBao tb : allNotifications) {
                    // Hiển thị nếu:
                    // 1. Thông báo chung (maNguoiDung == null)
                    // 2. Thông báo riêng cho user này (maNguoiDung == maNguoiDung hiện tại)
                    if (tb.getMaNguoiDung() == null || 
                        (maNguoiDung != null && maNguoiDung.equals(tb.getMaNguoiDung()))) {
                        filteredNotifications.add(tb);
                    }
                }
                
                Log.d("MeseegerFragment", "✓ Sau khi lọc: " + filteredNotifications.size() + " thông báo cho user này");
                
                // Hiển thị dữ liệu lên danh sách
                if (getContext() != null && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        adapterNotification = new ThongBaoAdapter(getContext(), filteredNotifications, new ItemThongBaoOnClick() {
                            @Override
                            public void itemOclick(View view, ThongBao thongBao) {
                                showPopupMenuDelete(view, thongBao);
                            }
                        });
                        
                        recyclerViewThongBao.setAdapter(adapterNotification);
                        Log.d("MeseegerFragment", "✓ Adapter đã được set với " + filteredNotifications.size() + " items");
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e("MeseegerFragment", "❌ Lỗi load thông báo từ Firebase", e);
                
                // Hiển thị danh sách rỗng khi lỗi
                if (getContext() != null && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        adapterNotification = new ThongBaoAdapter(getContext(), new ArrayList<>(), new ItemThongBaoOnClick() {
                            @Override
                            public void itemOclick(View view, ThongBao thongBao) {
                                showPopupMenuDelete(view, thongBao);
                            }
                        });
                        
                        recyclerViewThongBao.setAdapter(adapterNotification);
                        MyToast.error(getContext(), "Không thể load thông báo từ Firebase");
                    });
                }
            }
        });
    }

    private void showPopupMenuDelete(View view, ThongBao thongBao) {
        PopupMenu popup = new PopupMenu(getContext(), view);
        popup.getMenuInflater()
                .inflate(R.menu.menu_delete, popup.getMenu());

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.menu_delete) {
                    showComfirmDeleteDialog(thongBao);
                }
                return true;
            }
        });

        popup.show();
    }

    private void showComfirmDeleteDialog(ThongBao thongBao) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
                .setMessage("Bạn có muốn xoá thống báo")
                .setPositiveButton("Xoá", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Xoá thông báo
                        if (thongBaoDAO.deleteThongBao(String.valueOf(thongBao.getMaThongBao()))) {
                            MyToast.successful(getContext(), "Xoá thành công");
                            loadListNotification();
                        }
                    }
                })
                .setNegativeButton("Huỷ", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });

        builder.show();
    }

    /**
     * Cập nhật trạng thái thông báo chưa xem → đã xem
     * ✅ Load từ Firebase Direct để đảm bảo dữ liệu mới nhất
     */
    private void updateStatusNotification() {
        Log.d("MeseegerFragment", "▶ Update status notification");
        
        // Load từ Firebase để lấy danh sách mới nhất
        thongBaoDAO.getAllFromFirebaseDirect(new ThongBaoDAO.OnThongBaoListListener() {
            @Override
            public void onListReceived(ArrayList<ThongBao> listNotification) {
                // Update trong background thread
                new Thread(() -> {
                    int updatedCount = 0;
                    for (ThongBao thongBao : listNotification) {
                        // Chỉ cập nhật những thông báo chưa xem
                        if (thongBao.getTrangThai() == ThongBao.STATUS_CHUA_XEM) {
                            thongBao.setTrangThai(ThongBao.STATUS_DA_XEM);
                            thongBaoDAO.updateThongBao(thongBao);
                            updatedCount++;
                        }
                    }
                    
                    final int finalCount = updatedCount;
                    if (finalCount > 0) {
                        Log.d("MeseegerFragment", "✓ Đã cập nhật " + finalCount + " thông báo sang trạng thái ĐÃ XEM");
                    }
                }).start();
            }

            @Override
            public void onError(Exception e) {
                Log.e("MeseegerFragment", "❌ Lỗi load thông báo để update status", e);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("MeseegerFragment", "onResume: Reload thông báo từ Firebase");
        loadListNotification();
        updateStatusNotification();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Dừng Firebase Direct listener khi fragment bị destroy
        if (thongBaoDAO != null) {
            thongBaoDAO.stopFirebaseDirectListener();
            Log.d("MeseegerFragment", "✓ Stopped Firebase listener");
        }
    }
}