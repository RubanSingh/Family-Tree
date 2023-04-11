package dna.familytree.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import dna.familytree.LauncherController;
import dna.familytree.R;
import dna.familytree.util.LoggerUtils;

/**
 * Created by rubansingh.john on 6/21/2021.
 */
public class FirebaseService extends FirebaseMessagingService {

    private static final String TAG = FirebaseService.class.getSimpleName();

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull @NotNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        LoggerUtils.DebugLog(TAG, "New FCM message Received");
        LoggerUtils.DebugLog(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            ArrayMap<String, String> remoteMsgHash = (ArrayMap<String, String>) remoteMessage.getData();
            sendNotification(this, remoteMsgHash, Objects.requireNonNull(remoteMessage.getNotification()));
        }
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param remoteMsgHash
     * @param notification  FCM message body received.
     */
    private void sendNotification(Context context, ArrayMap<String, String> remoteMsgHash, @NonNull RemoteMessage.Notification notification) {

        Intent notifyIntent = new Intent(context, LauncherController.class);
        if (remoteMsgHash != null) {
            Bundle bundle = new Bundle();

            for (String key : remoteMsgHash.keySet())
                bundle.putString(key, remoteMsgHash.get(key));
            notifyIntent.putExtras(bundle);
        }
        // Set the Activity to start in a new, empty task
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notifyIntent, getIntentFlag());

        String channelId = context.getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(notification.getTitle())
                        .setContentText(notification.getBody())
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(channel);
        }

        assert notificationManager != null;
        notificationManager.notify(0, notificationBuilder.build());
    }

    private int getIntentFlag() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_MUTABLE;
        }
        return PendingIntent.FLAG_ONE_SHOT;
    }
}
