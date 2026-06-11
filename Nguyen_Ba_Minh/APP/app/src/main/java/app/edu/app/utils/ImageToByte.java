package app.edu.app.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;

import de.hdodenhof.circleimageview.CircleImageView;

public class ImageToByte {
    public static byte[] drawableToByte(Context context, int imageID) {
        // chuyển id Drawable -> Byte []
        @SuppressLint("UseCompatLoadingForDrawables")
        Drawable drawable = context.getResources().getDrawable(imageID);
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);

        return stream.toByteArray();
    }

    public static byte[] circleImageViewToByte(Context context, CircleImageView circleImageView) {
        // chuyển image từ CircleImageView -> Byte []
        Bitmap bitmap = ((BitmapDrawable) circleImageView.getDrawable()).getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);

        return stream.toByteArray();
    }

    public static byte[] imageViewToByte(Context context, ImageView imageView) {
        // chuyển image từ ImageView -> Byte []
        Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);

        return stream.toByteArray();
    }
}
