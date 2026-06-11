package app.edu.app.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Date;

import app.edu.app.R;

public class MyNotification {
    public static final String CHANNEL_ID = "APTCoffee";
    public static void checkSDK(Context context){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager  = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
    public static void getNotification(Context context, String strNotification){
        checkSDK(context);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID);
        builder.setContentTitle("MILK TEA");
        builder.setContentText(strNotification);
        builder.setColor(context.getResources().getColor(R.color.Oranger));
        builder.setSmallIcon(R.drawable.ic_notification);
        builder.setAutoCancel(true);
        int numberId = (int) ((new Date(). getTime () / 1000L)% Integer.MAX_VALUE);
        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
        managerCompat.notify(numberId, builder.build());
    }
}
