package com.shivansps.fsowrapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import androidx.core.content.FileProvider;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.content.Intent;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Iterator;
import android.view.View;
import android.widget.RadioGroup;

public class MainActivity extends AppCompatActivity {
    private static final String shader_file_name = "0_shaders_v5.vp";
    private static final String demo_filename = "fs2_demo.vpc"; //empty to disable demo install function
    private static final String LOG_RELATIVE_PATH = "data/fs2_open.log";
    private static final String defaultArgs = "-fps -threads 0 -no_large_shaders";
    private static final int RENDERER_OPENGL = 0;
    private static final int RENDERER_VULKAN_SYSTEM = 1;
    private static final int RENDERER_VULKAN_CUSTOM = 2;
    private static final String DRIVER_ID_PREF = "vulkan_driver_id";

    private Spinner spEngine;
    private Spinner spRenderer;
    private Button btnImportVulkanDriver;
    private TextView driverStatus;
    private VulkanDriver selectedDriver;
    private boolean importingDriver;
    private final ActivityResultLauncher<String> driverPicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(), this::importVulkanDriver);
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
        setupRenderer();
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

    private void setupRenderer() {
        spRenderer = findViewById(R.id.spRenderer);
        btnImportVulkanDriver = findViewById(R.id.btnImportVulkanDriver);
        driverStatus = findViewById(R.id.vulkanDriverStatus);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.renderer_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRenderer.setAdapter(adapter);

        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        int previousMode = prefs.getBoolean("use_vulkan", false) ? RENDERER_VULKAN_SYSTEM : RENDERER_OPENGL;
        spRenderer.setSelection(prefs.getInt("renderer_mode", previousMode));

        String driverId = prefs.getString(DRIVER_ID_PREF, null);
        if (driverId != null) {
            try {
                selectedDriver = VulkanDriver.find(this, driverId);
                driverStatus.setText(selectedDriver.name);
            } catch (IOException e) {
                driverStatus.setText(e.getMessage());
            }
        }
        spRenderer.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateDriverControls();
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        btnImportVulkanDriver.setOnClickListener(v -> driverPicker.launch("application/zip"));
        updateDriverControls();
    }

    private void updateDriverControls() {
        boolean custom = spRenderer.getSelectedItemPosition() == RENDERER_VULKAN_CUSTOM;
        btnImportVulkanDriver.setVisibility(custom ? View.VISIBLE : View.GONE);
        btnImportVulkanDriver.setEnabled(!importingDriver);
        driverStatus.setVisibility(custom ? View.VISIBLE : View.GONE);
    }

    private void importVulkanDriver(Uri uri) {
        if (uri == null) return;

        importingDriver = true;
        updateDriverControls();
        driverStatus.setText(R.string.vulkan_driver_importing);
        Context context = getApplicationContext();
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        new Thread(() -> {
            try {
                VulkanDriver driver = VulkanDriver.importPackage(context, uri);
                prefs.edit().putString(DRIVER_ID_PREF, driver.directory.getName()).apply();
                runOnUiThread(() -> {
                    if (isDestroyed()) return;
                    selectedDriver = driver;
                    driverStatus.setText(driver.name);
                    importingDriver = false;
                    updateDriverControls();
                });
            } catch (IOException | IllegalArgumentException | SecurityException e) {
                runOnUiThread(() -> {
                    if (isDestroyed()) return;
                    String message = getString(R.string.vulkan_driver_import_failed, e.getMessage());
                    driverStatus.setText(selectedDriver == null ? message : selectedDriver.name);
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    importingDriver = false;
                    updateDriverControls();
                });
            }
        }, "VulkanDriverImport").start();
    }

    private void CreatePlayButton()
    {
        Button btnPlay = findViewById(R.id.btnPlay);
        EditText etArgs = findViewById(R.id.etArgs);
        CheckBox touchControls = findViewById(R.id.touchControlsCheckBox);

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        etArgs.setText(sharedPref.getString("fso_args", defaultArgs));
        touchControls.setChecked(sharedPref.getBoolean("touch_overlay", false));

        btnPlay.setOnClickListener(v -> {
            int renderer = spRenderer.getSelectedItemPosition();
            if (renderer == RENDERER_VULKAN_CUSTOM && (importingDriver || selectedDriver == null)) {
                int message = importingDriver ? R.string.vulkan_driver_importing : R.string.vulkan_driver_missing;
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                return;
            }

            // Click Play Logic
            EngineVariant chosen = (EngineVariant) spEngine.getSelectedItem();
            if (chosen == null) {
                Toast.makeText(this, "First select a FSO version from the list.", Toast.LENGTH_SHORT).show();
                return;
            }

            String userLine = etArgs.getText() != null ? etArgs.getText().toString() : defaultArgs;
            List<String> extra = tokenizeArgs(userLine);
            ArrayList<String> argv = new ArrayList<>(extra);

            StorageOption sel = (StorageOption) spWorkingFolder.getSelectedItem();
            String actualRootPath = "";
            if (sel != null && sel.path != null && !sel.path.isEmpty()) {
                actualRootPath = sel.path+ "/" + lastSelectedSubdir + "/";
            }

            // delete gles shaders if they exist
            ensureAssetDeleted(shader_file_name, actualRootPath);

            if (renderer != RENDERER_OPENGL && !containsFlag(extra, "-vulkan")) {
                argv.add("-vulkan");
            }

            sharedPref.edit()
                    .putString("fso_args", userLine)
                    .putBoolean("touch_overlay", touchControls.isChecked())
                    .putInt("renderer_mode", renderer)
                    .apply();

            Intent i = new Intent(this, GameActivity.class);
            if (renderer == RENDERER_VULKAN_CUSTOM) {
                i.putExtra(VulkanDriver.EXTRA_ID, selectedDriver.directory.getName());
            }
            i.putExtra("engineLibName", chosen.baseName);
            i.putStringArrayListExtra("fsoArgs", argv);
            i.putExtra("forceTouchOverlay",touchControls.isChecked());
            i.putExtra("workingFolder", actualRootPath);
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

    @SuppressLint("QueryPermissionsNeeded")
    private void openFs2Log() {
        File logFile = new File(getFilesDir(), LOG_RELATIVE_PATH);

        if (!logFile.exists() || !logFile.isFile()) {
            Toast.makeText(this, "The log file does not exist.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    logFile
            );

            Intent view = new Intent(Intent.ACTION_VIEW);
            view.setDataAndType(uri, "text/plain");
            view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (view.resolveActivity(getPackageManager()) != null) {
                startActivity(Intent.createChooser(view, "Open fs2_open.log"));
                return;
            }

            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (send.resolveActivity(getPackageManager()) != null) {
                startActivity(Intent.createChooser(send, "Share fs2_open.log"));
            } else {
                Toast.makeText(this, "No apps installed to open this type of file (.log).", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Unable to open log: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void refreshRootSubdirList() {
        rgRootSubdir.setEnabled(true);
        rgRootSubdir.setOnCheckedChangeListener(null);
        rgRootSubdir.clearCheck();
        rgRootSubdir.removeAllViews();

        StorageOption sel = (StorageOption) spWorkingFolder.getSelectedItem();
        String w_folder = sel.path;
        File root = new File(w_folder);

        RadioButton rbNone = new RadioButton(this);
        rbNone.setId(generateViewIdCompat());
        rbNone.setEnabled(true);
        rbNone.setText("/");
        rbNone.setTag("/");
        rgRootSubdir.addView(rbNone);

        File[] kids = root.listFiles();
        if (kids != null) {
            Arrays.sort(kids, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            for (File f : kids) {
                if (!f.isDirectory()) continue;
                String name = f.getName();
                if ("data".equalsIgnoreCase(name)) continue;
                if (name.startsWith(".")) continue;

                RadioButton rb = new RadioButton(this);
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
                View v = rgRootSubdir.getChildAt(i);
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
            View v = group.findViewById(checkedId);
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

    private static List<String> tokenizeArgs(String line) {
        ArrayList<String> out = new ArrayList<>();
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

    private static boolean containsFlag(List<String> tokens, String flag) {
        for (int i = 0; i < tokens.size(); i++) {
            if (flag.equals(tokens.get(i))) return true;
        }
        return false;
    }

    private void ensureAssetDeleted(String assetName, String wfolderAbsPath) {
        if (wfolderAbsPath == null || wfolderAbsPath.isEmpty()) return;

        File dest = new File(wfolderAbsPath, assetName);
        if (dest.exists())
        {
            dest.delete();
            return;
        }
    }

    private void copyAssetToFile(String assetName, File outFile) {
        if (outFile.exists()) return;
        InputStream in = null;
        OutputStream out = null;
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