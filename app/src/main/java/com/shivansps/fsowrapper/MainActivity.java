package com.shivansps.fsowrapper;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import androidx.core.content.FileProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Iterator;
import android.view.View;
import android.widget.RadioGroup;

public class MainActivity extends AppCompatActivity {
    private static final String shader_file_name = "0_shaders_v2.vp";
    private static final String demo_filename = "fs2_demo.vpc"; //empty to disable demo install function
    private static final String FSO_INI = "fs2_open.ini";
    private static final String LOG_RELATIVE_PATH = "data/fs2_open.log";
    private static final String defaultArgs = "-fps -no_geo_effects -threads 0";
    private Spinner spEngine;
    private Spinner spWorkingFolder;
    private RadioGroup rgRootSubdir;
    private String lastSelectedSubdir = "";
    private List<StorageOption> workingFolderOptions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rgRootSubdir = findViewById(R.id.rgRootSubdir);
        if(!demo_filename.isEmpty()) CopyFs2Demo();
        LoadFSOVersions();
        DetectStorage();
        CreatePlayButton();
        setupOpenLogButton();
    }

    @Override
    protected void onResume() {
        super.onResume();
        workingFolderOptions = StorageDetector.listOptions(this);
        ArrayAdapter<StorageOption> wfAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, workingFolderOptions
        );
        wfAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spWorkingFolder.setAdapter(wfAdapter);
    }

    private void CreatePlayButton()
    {
        Button btnPlay = findViewById(R.id.btnPlay);
        EditText etArgs = findViewById(R.id.etArgs);
        CheckBox touchControls = findViewById(R.id.touchControlsCheckBox);

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        etArgs.setText(sharedPref.getString("fso_args", defaultArgs));
        touchControls.setChecked(sharedPref.getBoolean("touch_overlay", true));

        btnPlay.setOnClickListener(v -> {
            // Click Play Logic
            EngineVariant chosen = (EngineVariant) spEngine.getSelectedItem();
            if (chosen == null) {
                Toast.makeText(this, "First select a FSO version from the list.", Toast.LENGTH_SHORT).show();
                return;
            }

            String userLine = etArgs.getText() != null ? etArgs.getText().toString() : defaultArgs;
            java.util.List<String> extra = tokenizeArgs(userLine);
            ArrayList<String> argv = new ArrayList<>(extra);

            if (!containsFlag(extra, "-working_folder")) {
                StorageOption sel = (StorageOption) spWorkingFolder.getSelectedItem();
                if (sel != null && sel.path != null && !sel.path.isEmpty()) {
                    argv.add("-working_folder");
                    String actualRootPath = sel.path+ "/" + lastSelectedSubdir + "/";
                    argv.add(actualRootPath);
                    // delete old shader file
                    //deleteFileIfExist(actualRootPath+"0_shader_v1.vp");
                    // copy
                    boolean ok = ensureAssetPresent(shader_file_name, actualRootPath);
                    if (!ok) {
                        android.widget.Toast.makeText(this,
                                "Unable to copy shader files to working folder.",
                                android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }

            sharedPref.edit().putString("fso_args", userLine).apply();
            sharedPref.edit().putBoolean("touch_overlay", touchControls.isChecked()).apply();

            ensureIniPresent();

            Intent i = new Intent(this, GameActivity.class);
            i.putExtra("engineLibName", chosen.baseName);
            i.putStringArrayListExtra("fsoArgs", argv);
            i.putExtra("touchOverlay",touchControls.isChecked());
            //i.putExtra("externalFolderPath",true);
            startActivity(i);
        });
    }

    private void CopyFs2Demo()
    {
        SharedPreferences p = getSharedPreferences("first_run", MODE_PRIVATE);
        if (p.getBoolean("copied", false)) return;

        File destDir = getExternalFilesDir(null);
        if (destDir == null) destDir = getFilesDir();

        copyAssetToFile(demo_filename, new File(destDir+"/fs2_demo", demo_filename));

        p.edit().putBoolean("copied", true).apply();
    }

    private void DetectStorage()
    {
        spWorkingFolder = findViewById(R.id.spWfolder);
        workingFolderOptions = StorageDetector.listOptions(this);

        ArrayAdapter<StorageOption> wfAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, workingFolderOptions
        );
        wfAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spWorkingFolder.setAdapter(wfAdapter);

        spWorkingFolder.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshRootSubdirList();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        if (!workingFolderOptions.isEmpty()) spWorkingFolder.setSelection(0);
    }

    private static int generateViewIdCompat() {
        return View.generateViewId();
    }

    private void setupOpenLogButton() {
        Button btnOpenLog = findViewById(R.id.btnOpenLog);
        btnOpenLog.setOnClickListener(v -> openFs2Log());
    }

    private void openFs2Log() {
        java.io.File logFile = new java.io.File(getFilesDir(), LOG_RELATIVE_PATH);

        if (!logFile.exists() || !logFile.isFile()) {
            android.widget.Toast.makeText(this, "The log file does not exist.", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            android.net.Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    logFile
            );

            android.content.Intent view = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            view.setDataAndType(uri, "text/plain");
            view.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (view.resolveActivity(getPackageManager()) != null) {
                startActivity(android.content.Intent.createChooser(view, "Open fs2_open.log"));
                return;
            }

            android.content.Intent send = new android.content.Intent(android.content.Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(android.content.Intent.EXTRA_STREAM, uri);
            send.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (send.resolveActivity(getPackageManager()) != null) {
                startActivity(android.content.Intent.createChooser(send, "Share fs2_open.log"));
            } else {
                android.widget.Toast.makeText(this, "No apps installed to open this type of file (.log).", android.widget.Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            android.widget.Toast.makeText(this, "Unable to open log: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
        }
    }

    private void refreshRootSubdirList() {
        rgRootSubdir.setEnabled(true);
        rgRootSubdir.setOnCheckedChangeListener(null);
        rgRootSubdir.clearCheck();
        rgRootSubdir.removeAllViews();

        StorageOption sel = (StorageOption) spWorkingFolder.getSelectedItem();
        String w_folder = sel.path;
        java.io.File root = new java.io.File(w_folder);

        android.widget.RadioButton rbNone = new android.widget.RadioButton(this);
        rbNone.setId(generateViewIdCompat());
        rbNone.setEnabled(true);
        rbNone.setText("/");
        rbNone.setTag("/");
        rgRootSubdir.addView(rbNone);

        java.io.File[] kids = root.listFiles();
        if (kids != null) {
            java.util.Arrays.sort(kids, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            for (java.io.File f : kids) {
                if (!f.isDirectory()) continue;
                String name = f.getName();
                if ("data".equalsIgnoreCase(name)) continue;
                if (name.startsWith(".")) continue;

                android.widget.RadioButton rb = new android.widget.RadioButton(this);
                rb.setId(generateViewIdCompat());
                rb.setEnabled(true);
                rb.setText(name);
                rb.setTag(name);
                rgRootSubdir.addView(rb);
            }
        }

        boolean restored = false;
        if (lastSelectedSubdir != null) {
            for (int i = 0; i < rgRootSubdir.getChildCount(); i++) {
                android.view.View v = rgRootSubdir.getChildAt(i);
                Object tag = v.getTag();
                if (tag instanceof String && (tag).equals(lastSelectedSubdir)) {
                    rgRootSubdir.check(v.getId());
                    restored = true;
                    break;
                }
            }
        }
        if (!restored) {
            rgRootSubdir.check((rgRootSubdir.getChildAt(0)).getId());
            lastSelectedSubdir = "";
        }

        rgRootSubdir.setOnCheckedChangeListener((group, checkedId) -> {
            android.view.View v = group.findViewById(checkedId);
            Object tag = (v != null) ? v.getTag() : "";
            lastSelectedSubdir = (tag instanceof String) ? (String) tag : "";
        });

        for (int i = 0; i < rgRootSubdir.getChildCount(); i++) {
            rgRootSubdir.getChildAt(i).setEnabled(true);
        }
    }

    private void LoadFSOVersions()
    {
        spEngine = findViewById(R.id.spEngine);

        // Scan jniLibs folder
        NativeLibScanner.Catalog catalog = NativeLibScanner.scan(this);
        List<EngineVariant> engineList = NativeLibScanner.flatListSorted(catalog);

        if (engineList.isEmpty()) {
            Toast.makeText(this, "No FSO binaries found in jniLibs.", Toast.LENGTH_LONG).show();
        }

        // Fill spinner
        ArrayAdapter<EngineVariant> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, engineList
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spEngine.setAdapter(adapter);

        int defaultIndex = 0;
        String newestVersion = null;
        if (!catalog.versions.isEmpty()) {
            Iterator<String> it = catalog.versions.iterator();
            newestVersion = it.hasNext() ? it.next() : null;
        }
        if (newestVersion != null) {
            for (int i = 0; i < engineList.size(); i++) {
                EngineVariant ev = engineList.get(i);
                if (ev.version.equals(newestVersion) && !ev.debug) {
                    defaultIndex = i;
                    break;
                }
            }
        }
        if (!engineList.isEmpty()) {
            spEngine.setSelection(defaultIndex);
        }
    }

    private static final Pattern ARG_PATTERN =
            Pattern.compile("\"([^\"]*)\"|'([^']*)'|\\S+");

    private static java.util.List<String> tokenizeArgs(String line) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        if (line == null) return out;
        line = line.trim();
        if (line.isEmpty()) return out;

        Matcher m = ARG_PATTERN.matcher(line);
        while (m.find()) {
            String g1 = m.group(1);
            String g2 = m.group(2);
            String g3 = m.group(0);
            String tok = (g1 != null) ? g1 : (g2 != null) ? g2 : g3;
            if (tok != null && !tok.isEmpty()) out.add(tok);
        }
        return out;
    }

    private static boolean containsFlag(java.util.List<String> tokens, String flag) {
        for (int i = 0; i < tokens.size(); i++) {
            if (flag.equals(tokens.get(i))) return true;
        }
        return false;
    }

    private boolean ensureAssetPresent(String assetName, String wfolderAbsPath) {
        if (wfolderAbsPath == null || wfolderAbsPath.isEmpty()) return false;

        java.io.File dest = new java.io.File(wfolderAbsPath, assetName);
        if (dest.exists()) return true;

        if (dest.getParentFile() != null) dest.getParentFile().mkdirs();

        java.io.InputStream in = null;
        java.io.OutputStream out = null;
        try {
            in = getAssets().open(assetName);
            out = Files.newOutputStream(dest.toPath());
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) {
                out.write(buf, 0, r);
            }
            out.flush();
            return true;
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            try { if (out != null) out.close(); } catch (Exception ignored) {}
        }
    }

    private void ensureIniPresent() {
        java.io.File dest = new java.io.File(getFilesDir(), FSO_INI);
        if (dest.exists()) return;

        copyAssetToFile(FSO_INI, dest);
    }

    public void deleteFileIfExist(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
    }

    private void copyAssetToFile(String assetName, File outFile) {
        if (outFile.exists()) return;
        java.io.InputStream in = null;
        java.io.OutputStream out = null;
        try {
            if (outFile.getParentFile() != null && !outFile.getParentFile().exists()) {
                outFile.getParentFile().mkdirs();
            }
            in = getAssets().open(assetName);
            out = Files.newOutputStream(outFile.toPath());
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
            out.flush();
        } catch (Exception ignored) {
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            try { if (out != null) out.close(); } catch (Exception ignored) {}
        }
    }
}