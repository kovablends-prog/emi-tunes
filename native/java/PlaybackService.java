package app.emitunes;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.IBinder;

/** Keeps the music playing in the background and shows lock-screen / notification controls. */
public class PlaybackService extends Service {
    public static final String ACT_TOGGLE = "app.emitunes.TOGGLE";
    public static final String ACT_NEXT = "app.emitunes.NEXT";
    public static final String ACT_PREV = "app.emitunes.PREV";
    private static final String CHANNEL = "emi_playback";
    static PlaybackService instance;
    private boolean started = false;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        instance = this;
        String t = i != null && i.getStringExtra("title") != null ? i.getStringExtra("title") : "Emi Tunes";
        String a = i != null && i.getStringExtra("artist") != null ? i.getStringExtra("artist") : "";
        show(t, a, i == null || i.getBooleanExtra("playing", true));
        return START_NOT_STICKY;
    }

    private PendingIntent action(String act, int code) {
        Intent in = new Intent(act).setPackage(getPackageName());
        return PendingIntent.getBroadcast(this, code, in, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    void show(String title, String artist, boolean playing) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Playback", NotificationManager.IMPORTANCE_LOW));
        Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
        PendingIntent open = PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_IMMUTABLE);
        Notification.Action prev = new Notification.Action.Builder(Icon.createWithResource(this, android.R.drawable.ic_media_previous), "Previous", action(ACT_PREV, 1)).build();
        Notification.Action toggle = new Notification.Action.Builder(Icon.createWithResource(this,
            playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play), playing ? "Pause" : "Play", action(ACT_TOGGLE, 2)).build();
        Notification.Action next = new Notification.Action.Builder(Icon.createWithResource(this, android.R.drawable.ic_media_next), "Next", action(ACT_NEXT, 3)).build();
        Notification n = new Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(artist)
            .setContentIntent(open)
            .setOngoing(playing)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(prev).addAction(toggle).addAction(next)
            .setStyle(new Notification.MediaStyle().setShowActionsInCompactView(0, 1, 2))
            .build();
        if (!started) {
            if (Build.VERSION.SDK_INT >= 29) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            else startForeground(1, n);
            started = true;
        } else {
            nm.notify(1, n);
        }
    }

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
    }
}
