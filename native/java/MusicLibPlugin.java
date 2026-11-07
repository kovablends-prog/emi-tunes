package app.emitunes;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Size;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/** Reads the phone's music library (MediaStore) and drives the playback notification. */
@CapacitorPlugin(
    name = "MusicLib",
    permissions = {
        @Permission(alias = "audio", strings = { Manifest.permission.READ_MEDIA_AUDIO }),
        @Permission(alias = "storage", strings = { Manifest.permission.READ_EXTERNAL_STORAGE })
    }
)
public class MusicLibPlugin extends Plugin {

    private static final Uri AUDIO = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    private BroadcastReceiver receiver;

    private String alias() {
        return Build.VERSION.SDK_INT >= 33 ? "audio" : "storage";
    }

    @Override
    public void load() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String a = intent.getAction();
                if (a == null) return;
                JSObject o = new JSObject();
                if (a.endsWith(".TOGGLE")) o.put("c", "toggle");
                else if (a.endsWith(".NEXT")) o.put("c", "next");
                else if (a.endsWith(".PREV")) o.put("c", "prev");
                else return;
                notifyListeners("cmd", o);
            }
        };
        IntentFilter f = new IntentFilter();
        f.addAction(PlaybackService.ACT_TOGGLE);
        f.addAction(PlaybackService.ACT_NEXT);
        f.addAction(PlaybackService.ACT_PREV);
        ContextCompat.registerReceiver(getContext(), receiver, f, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @PluginMethod
    public void scan(PluginCall call) {
        if (getPermissionState(alias()) != PermissionState.GRANTED) {
            requestPermissionForAlias(alias(), call, "scanPerm");
            return;
        }
        doScan(call);
    }

    @PermissionCallback
    private void scanPerm(PluginCall call) {
        if (getPermissionState(alias()) == PermissionState.GRANTED) doScan(call);
        else call.reject("Permission to read music was denied");
    }

    private void doScan(PluginCall call) {
        JSArray out = new JSArray();
        String[] proj = {
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK, MediaStore.Audio.Media.DATE_MODIFIED, MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DISPLAY_NAME
        };
        Cursor c = null;
        try {
            c = getContext().getContentResolver().query(AUDIO, proj,
                MediaStore.Audio.Media.IS_MUSIC + " != 0", null, MediaStore.Audio.Media.DATA + " COLLATE NOCASE");
            while (c != null && c.moveToNext()) {
                JSObject o = new JSObject();
                long id = c.getLong(0);
                String data = c.getString(9);
                String name = c.getString(10);
                String rel = data != null ? data.replaceFirst("^/storage/[^/]+/", "") : (name != null ? name : String.valueOf(id));
                o.put("id", id);
                o.put("title", c.getString(1));
                String artist = c.getString(2);
                o.put("artist", (artist == null || artist.equals("<unknown>")) ? "Unknown Artist" : artist);
                String album = c.getString(3);
                o.put("album", (album == null || album.equals("<unknown>")) ? "Unknown Album" : album);
                o.put("albumId", c.getLong(4));
                o.put("dur", c.getLong(5) / 1000.0);
                o.put("track", c.getInt(6) % 1000);
                o.put("mtime", c.getLong(7));
                o.put("size", c.getLong(8));
                o.put("path", data == null ? "" : data);
                o.put("rel", rel);
                o.put("uri", ContentUris.withAppendedId(AUDIO, id).toString());
                out.put(o);
            }
        } catch (Exception e) {
            call.reject(String.valueOf(e.getMessage()));
            return;
        } finally {
            if (c != null) c.close();
        }
        JSObject r = new JSObject();
        r.put("songs", out);
        call.resolve(r);
    }

    @PluginMethod
    public void art(PluginCall call) {
        long id = call.getData().optLong("id", 0);
        long alb = call.getData().optLong("alb", 0);
        Bitmap b = null;
        try {
            if (Build.VERSION.SDK_INT >= 29 && id != 0) {
                b = getContext().getContentResolver().loadThumbnail(ContentUris.withAppendedId(AUDIO, id), new Size(360, 360), null);
            }
        } catch (Exception ignored) { }
        if (b == null && alb != 0) {
            try (InputStream in = getContext().getContentResolver().openInputStream(
                    ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), alb))) {
                if (in != null) b = BitmapFactory.decodeStream(in);
            } catch (Exception ignored) { }
        }
        JSObject r = new JSObject();
        if (b != null) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            b.compress(Bitmap.CompressFormat.JPEG, 72, bo);
            r.put("data", "data:image/jpeg;base64," + Base64.encodeToString(bo.toByteArray(), Base64.NO_WRAP));
        }
        call.resolve(r);
    }

    @PluginMethod
    public void nowPlaying(PluginCall call) {
        String title = call.getString("title", "");
        String artist = call.getString("artist", "");
        boolean playing = Boolean.TRUE.equals(call.getBoolean("playing", true));
        if (Build.VERSION.SDK_INT >= 33 && getActivity() != null
            && ContextCompat.checkSelfPermission(getContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[] { Manifest.permission.POST_NOTIFICATIONS }, 77);
        }
        try {
            if (PlaybackService.instance != null) {
                PlaybackService.instance.show(title, artist, playing);
            } else {
                Intent i = new Intent(getContext(), PlaybackService.class);
                i.putExtra("title", title);
                i.putExtra("artist", artist);
                i.putExtra("playing", playing);
                ContextCompat.startForegroundService(getContext(), i);
            }
        } catch (Exception ignored) { }
        call.resolve();
    }

    @PluginMethod
    public void stopPlayback(PluginCall call) {
        try { getContext().stopService(new Intent(getContext(), PlaybackService.class)); } catch (Exception ignored) { }
        call.resolve();
    }
}
