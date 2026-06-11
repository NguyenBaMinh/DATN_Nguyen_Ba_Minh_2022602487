package app.edu.app.utils;

import android.app.Activity;

import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;

import app.edu.app.R;

public class Loading {
    Activity activity;
    Dialog dialog;

    public Loading(Activity activity) {
        this.activity = activity;
    }

    public void startLoading() {
        LayoutInflater layoutInflater = activity.getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.layout_loading, null);

        dialog = new Dialog(activity);
        dialog.setContentView(view);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.show();
    }

    public void stopLoading(){
        dialog.dismiss();
    }
}
