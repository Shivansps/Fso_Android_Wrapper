package com.shivansps.fsowrapper.ipc;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.util.Log;
import androidx.annotation.Nullable;
import com.shivansps.fsowrapper.BuildConfig;
import com.shivansps.fsowrapper.EngineVariant;
import com.shivansps.fsowrapper.NativeLibScanner;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IpcService extends Service {
    public static final int MSG_GET_VERSION       = 1;
    public static final int MSG_LIST_NATIVE_LIBS  = 2;
    public static final int MSG_REQUEST_ALL_FILES = 3;
    public static final int MSG_IMPORT_LIB        = 4;


    public static final String KEY_VERSION = "wrapperVersion";
    public static final String KEY_IMPORTED_LIBS    = "importedLibs";
    public static final String KEY_BUNDLED_LIBS   = "bundledLibs";
    public static final String KEY_URI     = "uri";
    public static final String KEY_DSTNAME = "dstName";

    private final Messenger messenger = new Messenger(new IncomingHandler());

    class IncomingHandler extends Handler {
        @Override public void handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case MSG_GET_VERSION: {
                        Bundle b = new Bundle();
                        b.putString(KEY_VERSION, BuildConfig.VERSION_NAME);
                        reply(msg, b);
                        break;
                    }
                    case MSG_LIST_NATIVE_LIBS: {
                        //List Imported
                        File dir = new File(getFilesDir(), "natives");
                        String[] arr = dir.isDirectory() ? dir.list((d, name) -> name.endsWith(".so")) : new String[0];
                        ArrayList<String> libs = new ArrayList<>();
                        if (arr != null) libs.addAll(Arrays.asList(arr));
                        Bundle b = new Bundle();
                        b.putStringArrayList(KEY_IMPORTED_LIBS, libs);

                        //List Bundled
                        NativeLibScanner.Catalog catalog = NativeLibScanner.scan(IpcService.this.getApplicationContext());
                        List<EngineVariant> engineList = NativeLibScanner.flatListSorted(catalog);
                        ArrayList<String> bundledLibs = new ArrayList<>();
                        if (!engineList.isEmpty()) {
                            for (int i = 0; i < engineList.size(); i++) {
                                EngineVariant ev = engineList.get(i);
                                bundledLibs.add(ev.fileName);
                            }
                        }
                        b.putStringArrayList(KEY_BUNDLED_LIBS, bundledLibs);
                        reply(msg, b);
                        break;
                    }
                    case MSG_REQUEST_ALL_FILES: {
                        if(Build.VERSION.SDK_INT >= 30) {
                            Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION );
                            i.setData(Uri.parse("package:" + getPackageName()));
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(i);
                            reply(msg, null);
                        }
                        break;
                    }
                    case MSG_IMPORT_LIB: {
                        Bundle data = msg.getData();
                        String uriStr = data.getString(KEY_URI);
                        String dstName = data.getString(KEY_DSTNAME, null);
                        if (uriStr != null) {
                            Uri src = Uri.parse(uriStr);
                            if (dstName == null) {
                                String last = src.getLastPathSegment();
                                dstName = (last != null) ? last.replaceAll(".*/", "") : "incoming.so";
                            }
                            File dstDir = new File(getFilesDir(), "natives");
                            dstDir.mkdirs();
                            File dst = new File(dstDir, dstName);
                            try (InputStream in = getContentResolver().openInputStream(src);
                                 OutputStream out = Files.newOutputStream(dst.toPath())) {
                                byte[] buf = new byte[8192];
                                int n;
                                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                            }
                        }
                        reply(msg, null);
                        break;
                    }
                    default:
                        super.handleMessage(msg);
                }
            } catch (Exception e) {
                Log.e("WrapperIpcService", "IPC error", e);
            }
        }

        private void reply(Message req, @Nullable Bundle data) throws RemoteException {
            if (req.replyTo != null) {
                Message m = Message.obtain(null, req.what);
                if (data != null) m.setData(data);
                req.replyTo.send(m);
            }
        }
    }

    @Nullable @Override public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }
}