package com.besome.sketch.export;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.airbnb.lottie.LottieAnimationView;
import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import a.a.a.KB;
import a.a.a.ProjectBuilder;
import a.a.a.eC;
import a.a.a.hC;
import a.a.a.iC;
import a.a.a.kC;
import a.a.a.lC;
import a.a.a.oB;
import a.a.a.wq;
import a.a.a.xq;
import a.a.a.yB;
import a.a.a.yq;
import mod.hey.studios.util.Helper;
import mod.jbk.export.GetKeyStoreCredentialsDialog;
import com.besome.sketch.build.BuildForegroundService;
import com.besome.sketch.build.BuildWorkManager;
import pro.sketchware.R;
import pro.sketchware.utility.FilePathUtil;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;

public class ExportProjectActivity extends BaseAppCompatActivity {

    private final oB file_utility = new oB();
    /**
     * /sketchware/signed_apk
     */
    private String signed_apk_postfix;
    /**
     * /sketchware/export_src
     */
    private String export_src_postfix;
    /**
     * /sdcard/sketchware/export_src
     */
    private String export_src_full_path;
    private String export_src_filename;
    private String sc_id;
    private HashMap<String, Object> sc_metadata = null;
    private yq project_metadata = null;
    private boolean isExportBuildRunning = false;
    private yq.ExportType currentExportType = null;
    private boolean currentExportIsAppBundle = false;
    private boolean currentExportIsSigningUi = false;
    private final BroadcastReceiver exportBuildReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String buildScId = intent.getStringExtra(BuildForegroundService.EXTRA_SC_ID);
            String buildType = intent.getStringExtra(BuildForegroundService.EXTRA_BUILD_TYPE);
            if (!BuildForegroundService.BUILD_TYPE_EXPORT.equals(buildType) || buildScId == null || !buildScId.equals(sc_id)) {
                return;
            }

            if (BuildForegroundService.ACTION_BUILD_PROGRESS.equals(action)) {
                String progress = intent.getStringExtra(BuildForegroundService.EXTRA_PROGRESS_TEXT);
                if (progress != null) {
                    a(progress);
                }
            } else if (BuildForegroundService.ACTION_BUILD_FINISHED.equals(action)) {
                isExportBuildRunning = false;
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                i();
                handleExportFinish(intent.getStringExtra(BuildForegroundService.EXTRA_RESULT_PATH));
            } else if (BuildForegroundService.ACTION_BUILD_CANCELED.equals(action)) {
                isExportBuildRunning = false;
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                i();
                resetExportUi();
            } else if (BuildForegroundService.ACTION_BUILD_FAILED.equals(action)) {
                isExportBuildRunning = false;
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                i();
                handleExportFailure(intent);
            }
        }
    };

    private Button sign_apk_button;
    private Button export_aab_button;
    private Button export_source_button;
    private TextView sign_apk_output_path;
    private Button export_source_send_button;
    private LinearLayout sign_apk_output_stage;
    private TextView export_source_output_path;
    private LinearLayout export_source_output_stage;
    private com.airbnb.lottie.LottieAnimationView sign_apk_loading_anim;
    private com.airbnb.lottie.LottieAnimationView export_source_loading_anim;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.export_project);

        ImageView sign_apk_ic = findViewById(R.id.sign_apk_ic);
        ImageView export_aab_ic = findViewById(R.id.export_aab_ic);
        TextView sign_apk_title = findViewById(R.id.sign_apk_title);
        sign_apk_button = findViewById(R.id.sign_apk_button);
        ImageView export_source_ic = findViewById(R.id.export_source_ic);
        TextView export_aab_title = findViewById(R.id.export_aab_title);
        export_aab_button = findViewById(R.id.export_aab_button);
        TextView export_source_title = findViewById(R.id.export_source_title);
        sign_apk_output_path = findViewById(R.id.sign_apk_output_path);
        export_source_button = findViewById(R.id.export_source_button);
        sign_apk_output_stage = findViewById(R.id.sign_apk_output_stage);
        sign_apk_loading_anim = findViewById(R.id.sign_apk_loading_anim);
        export_source_output_path = findViewById(R.id.export_source_output_path);
        export_source_send_button = findViewById(R.id.export_source_send_button);
        export_source_output_stage = findViewById(R.id.export_source_output_stage);
        export_source_loading_anim = findViewById(R.id.export_source_loading_anim);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        findViewById(R.id.layout_main_logo).setVisibility(View.GONE);
        getSupportActionBar().setTitle(Helper.getResString(R.string.myprojects_export_project_actionbar_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        toolbar.setNavigationOnClickListener(Helper.getBackPressedClickListener(this));

        if (savedInstanceState == null) {
            sc_id = getIntent().getStringExtra("sc_id");
        } else {
            sc_id = savedInstanceState.getString("sc_id");
        }

        sc_metadata = lC.b(sc_id);
        project_metadata = new yq(getApplicationContext(), wq.d(sc_id), sc_metadata);

        initializeOutputDirectories();
        initializeSignApkViews();
        initializeExportSrcViews();
        initializeAppBundleExportViews();

        IntentFilter filter = new IntentFilter();
        filter.addAction(BuildForegroundService.ACTION_BUILD_PROGRESS);
        filter.addAction(BuildForegroundService.ACTION_BUILD_FINISHED);
        filter.addAction(BuildForegroundService.ACTION_BUILD_FAILED);
        filter.addAction(BuildForegroundService.ACTION_BUILD_CANCELED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(exportBuildReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(exportBuildReceiver, filter);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(exportBuildReceiver);
        if (export_source_loading_anim.isAnimating()) {
            export_source_loading_anim.cancelAnimation();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString("sc_id", sc_id);
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Sets exported signed APK file path texts' content.
     */
    private void f(String filePath) {
        sign_apk_output_stage.setVisibility(View.VISIBLE);
        sign_apk_button.setVisibility(View.GONE);
        if (sign_apk_loading_anim.isAnimating()) {
            sign_apk_loading_anim.cancelAnimation();
        }
        sign_apk_loading_anim.setVisibility(View.GONE);
        sign_apk_output_path.setText(signed_apk_postfix + File.separator + filePath);
        SketchwareUtil.toast(Helper.getResString(R.string.sign_apk_title_export_apk_file));
    }

    private void exportSrc() {
        try {
            FileUtil.deleteFile(project_metadata.projectMyscPath);

            hC hCVar = new hC(sc_id);
            kC kCVar = new kC(sc_id);
            eC eCVar = new eC(sc_id);
            iC iCVar = new iC(sc_id);
            hCVar.i();
            kCVar.s();
            eCVar.g();
            eCVar.e();
            iCVar.i();

            /* Extract project type template */
            project_metadata.a(getApplicationContext(), wq.e(xq.a(sc_id) ? "600" : sc_id));

            /* Start generating project files */
            ProjectBuilder builder = new ProjectBuilder(this, project_metadata);
            project_metadata.a(iCVar, hCVar, eCVar, yq.ExportType.ANDROID_STUDIO);
            builder.buildBuiltInLibraryInformation();
            project_metadata.b(hCVar, eCVar, iCVar, builder.getBuiltInLibraryManager());
            if (yB.a(lC.b(sc_id), "custom_icon")) {
                project_metadata.aa(wq.e() + File.separator + sc_id + File.separator + "mipmaps");
                if (yB.a(lC.b(sc_id), "isIconAdaptive", false)) {
                    project_metadata.createLauncherIconXml("""
                            <?xml version="1.0" encoding="utf-8"?>
                            <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android" >
                            <background android:drawable="@mipmap/ic_launcher_background"/>
                            <foreground android:drawable="@mipmap/ic_launcher_foreground"/>
                            <monochrome android:drawable="@mipmap/ic_launcher_monochrome"/>
                            </adaptive-icon>""");
                }
            }
            project_metadata.a();
            kCVar.b(project_metadata.resDirectoryPath + File.separator + "drawable-xhdpi");
            kCVar.c(project_metadata.resDirectoryPath + File.separator + "raw");
            kCVar.a(project_metadata.assetsPath + File.separator + "fonts");
            project_metadata.f();

            /* It makes no sense that those methods aren't static */
            FilePathUtil util = new FilePathUtil();
            File pathJava = new File(util.getPathJava(sc_id));
            File pathResources = new File(util.getPathResource(sc_id));
            File pathAssets = new File(util.getPathAssets(sc_id));
            File pathNativeLibraries = new File(util.getPathNativelibs(sc_id));

            if (pathJava.exists()) {
                FileUtil.copyDirectory(pathJava, new File(project_metadata.javaFilesPath + File.separator + project_metadata.packageNameAsFolders));
            }
            if (pathResources.exists()) {
                FileUtil.copyDirectory(pathResources, new File(project_metadata.resDirectoryPath));
            }
            String pathProguard = util.getPathProguard(sc_id);
            if (FileUtil.isExistFile(pathProguard)) {
                FileUtil.copyFile(pathProguard, project_metadata.proguardFilePath);
            }
            if (pathAssets.exists()) {
                FileUtil.copyDirectory(pathAssets, new File(project_metadata.assetsPath));
            }
            if (pathNativeLibraries.exists()) {
                FileUtil.copyDirectory(pathNativeLibraries, new File(project_metadata.generatedFilesPath, "jniLibs"));
            }

            ArrayList<String> toCompress = new ArrayList<>();
            toCompress.add(project_metadata.projectMyscPath);
            String exportedFilename = yB.c(sc_metadata, "my_ws_name") + ".zip";

            String exportedSourcesZipPath = wq.s() + File.separator + "export_src" + File.separator + exportedFilename;
            if (file_utility.e(exportedSourcesZipPath)) {
                file_utility.c(exportedSourcesZipPath);
            }

            ArrayList<String> toExclude = new ArrayList<>();
            if (!new File(new FilePathUtil().getPathJava(sc_id) + File.separator + "SketchApplication.java").exists()) {
                toExclude.add("SketchApplication.java");
            }
            toExclude.add("DebugActivity.java");

            new KB().a(exportedSourcesZipPath, toCompress, toExclude);
            project_metadata.e();
            runOnUiThread(() -> initializeAfterExportedSourceViews(exportedFilename));
        } catch (Exception e) {
            runOnUiThread(() -> {
                Log.e("ProjectExporter", "While trying to export project's sources: "
                        + e.getMessage(), e);
                SketchwareUtil.showAnErrorOccurredDialog(this, Log.getStackTraceString(e));
                export_source_output_stage.setVisibility(View.GONE);
                export_source_loading_anim.setVisibility(View.GONE);
                export_source_button.setVisibility(View.VISIBLE);
            });
        }
    }

    private void initializeAppBundleExportViews() {
        export_aab_button.setOnClickListener(view -> {
            MaterialAlertDialogBuilder confirmationDialog = new MaterialAlertDialogBuilder(this);
            confirmationDialog.setTitle("Important note");
            confirmationDialog.setMessage("The generated .aab file must be signed.\nCopy your keystore to /Internal storage/sketchware/keystore/release_key.jks and enter the alias' password.");
            confirmationDialog.setIcon(R.drawable.ic_mtrl_info);

            confirmationDialog.setPositiveButton("Understood", (v, which) -> {
                showAabSigningDialog();
                v.dismiss();
            });
            confirmationDialog.show();
        });
    }

    private void showAabSigningDialog() {
        GetKeyStoreCredentialsDialog credentialsDialog = new GetKeyStoreCredentialsDialog(this,
                R.drawable.ic_mtrl_key, "Sign outputted AAB", "Fill in the keystore details to sign the AAB.");
        credentialsDialog.setListener(credentials -> {
            startExportBuild(yq.ExportType.AAB, true, false, credentials);
        });
        credentialsDialog.show();
    }

    /**
     * Initialize Export to Android Studio views
     */
    private void initializeExportSrcViews() {
        export_source_loading_anim.setVisibility(View.GONE);
        export_source_output_stage.setVisibility(View.GONE);
        export_source_button.setOnClickListener(v -> {
            export_source_button.setVisibility(View.GONE);
            export_source_output_stage.setVisibility(View.GONE);
            export_source_loading_anim.setVisibility(View.VISIBLE);
            export_source_loading_anim.playAnimation();
            new Thread() {
                @Override
                public void run() {
                    super.run();
                    exportSrc();
                }
            }.start();
        });
        export_source_send_button.setOnClickListener(v -> shareExportedSourceCode());
    }

    /**
     * Initialize APK Export views
     */
    private void initializeSignApkViews() {
        sign_apk_loading_anim.setVisibility(View.GONE);
        sign_apk_output_stage.setVisibility(View.GONE);

        sign_apk_button.setOnClickListener(view -> {
            MaterialAlertDialogBuilder confirmationDialog = new MaterialAlertDialogBuilder(this);
            confirmationDialog.setTitle("Important note");
            confirmationDialog.setMessage("""
                    To sign an APK, you need a keystore. Use your already created one, and copy it to \
                    /Internal storage/sketchware/keystore/release_key.jks and enter the alias's password.
                    
                    Note that this only signs your APK using signing scheme V1, to target Android 11+ for example, \
                    use a 3rd-party tool (for now).""");
            confirmationDialog.setIcon(R.drawable.ic_mtrl_info);

            confirmationDialog.setPositiveButton("Understood", (v, which) -> {
                showApkSigningDialog();
                v.dismiss();
            });
            confirmationDialog.show();
        });
    }

    private void showApkSigningDialog() {
        GetKeyStoreCredentialsDialog credentialsDialog = new GetKeyStoreCredentialsDialog(this,
                R.drawable.ic_mtrl_key,
                "Sign an APK",
                "Fill in the keystore details to sign the APK. " +
                        "If you don't have a keystore, you can use a test key.");
        credentialsDialog.setListener(credentials -> {
            sign_apk_button.setVisibility(View.GONE);
            sign_apk_output_stage.setVisibility(View.GONE);
            sign_apk_loading_anim.setVisibility(View.VISIBLE);
            sign_apk_loading_anim.playAnimation();

            startExportBuild(yq.ExportType.SIGN_APP, false, true, credentials);
        });
        credentialsDialog.show();
    }

    private void startExportBuild(yq.ExportType exportType, boolean buildAppBundle, boolean usesSigningUi, GetKeyStoreCredentialsDialog.Credentials credentials) {
        if (isExportBuildRunning) {
            return;
        }
        isExportBuildRunning = true;
        currentExportType = exportType;
        currentExportIsAppBundle = buildAppBundle;
        currentExportIsSigningUi = usesSigningUi;
        progressDialog.setCancelable(false);
        progressDialog.setOnCancelListener(this::handleExportCancel);
        a(this::handleExportCancel);
        a("Starting build...");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        boolean signWithTestkey = credentials != null && credentials.isForSigningWithTestkey();
        String keystorePath = credentials != null && !signWithTestkey ? wq.j() : null;
        String keystorePassword = credentials != null && !signWithTestkey ? credentials.getKeyStorePassword() : null;
        String aliasName = credentials != null && !signWithTestkey ? credentials.getKeyAlias() : null;
        String aliasPassword = credentials != null && !signWithTestkey ? credentials.getKeyPassword() : null;
        String signingAlgorithm = credentials != null ? credentials.getSigningAlgorithm() : null;

        BuildWorkManager.enqueueExportBuild(
                this,
                sc_id,
                exportType,
                buildAppBundle,
                signWithTestkey,
                keystorePath,
                keystorePassword,
                aliasName,
                aliasPassword,
                signingAlgorithm
        );
    }

    private void handleExportCancel(DialogInterface dialog) {
        if (!progressDialog.isCancelable()) {
            progressDialog.setCancelable(true);
            progressDialog.setOnCancelListener(this::handleExportCancel);
            a("Canceling process...");
            BuildWorkManager.cancelExportBuild(this, sc_id);
        }
    }

    private void handleExportFinish(String resultPath) {
        if (currentExportIsSigningUi) {
            resetExportUi();
        }

        if (resultPath == null) {
            return;
        }

        if (currentExportIsAppBundle) {
            String aabFilename = new File(resultPath).getName();
            MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
            dialog.setIcon(R.drawable.open_box_48);
            dialog.setTitle("Finished exporting AAB");
            dialog.setMessage("You can find the generated, signed AAB file at:\n" +
                    "/Internal storage/sketchware/signed_aab/" + aabFilename);
            dialog.setPositiveButton(Helper.getResString(R.string.common_word_ok), null);
            dialog.show();
        } else if (currentExportType == yq.ExportType.SIGN_APP) {
            if (new File(resultPath).exists()) {
                f(new File(resultPath).getName());
            }
        }
    }

    private void handleExportFailure(Intent intent) {
        resetExportUi();
        int errorType = intent.getIntExtra(BuildForegroundService.EXTRA_ERROR_TYPE, BuildForegroundService.ERROR_TYPE_GENERIC);
        String errorMessage = intent.getStringExtra(BuildForegroundService.EXTRA_ERROR_MESSAGE);
        if (errorType == BuildForegroundService.ERROR_TYPE_KEYSTORE &&
                "Incorrect password, or integrity check failed.".equals(errorMessage)) {
            SketchwareUtil.showAnErrorOccurredDialog(this,
                    "Either an incorrect password was entered, or your key store is corrupt.");
        } else {
            String errorStack = intent.getStringExtra(BuildForegroundService.EXTRA_ERROR_STACKTRACE);
            if (errorStack == null) {
                errorStack = errorMessage;
            }
            SketchwareUtil.showAnErrorOccurredDialog(this, errorStack);
        }
    }

    private void resetExportUi() {
        sign_apk_output_stage.setVisibility(View.GONE);
        if (sign_apk_loading_anim.isAnimating()) {
            sign_apk_loading_anim.cancelAnimation();
        }
        sign_apk_loading_anim.setVisibility(View.GONE);
        sign_apk_button.setVisibility(View.VISIBLE);
    }

    private void initializeOutputDirectories() {
        signed_apk_postfix = File.separator + "sketchware" + File.separator + "signed_apk";
        export_src_postfix = File.separator + "sketchware" + File.separator + "export_src";
        /* /sdcard/sketchware/signed_apk */
        String signed_apk_full_path = wq.s() + File.separator + "signed_apk";
        export_src_full_path = wq.s() + File.separator + "export_src";

        /* Check if they exist, if not, create them */
        file_utility.f(signed_apk_full_path);
        file_utility.f(export_src_full_path);
    }

    private void shareExportedSourceCode() {
        if (!export_src_filename.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("plain/text");
            intent.putExtra(Intent.EXTRA_SUBJECT, Helper.getResString(R.string.myprojects_export_src_title_email_subject, export_src_filename));
            intent.putExtra(Intent.EXTRA_TEXT, Helper.getResString(R.string.myprojects_export_src_title_email_body, export_src_filename));
            String filePath = export_src_full_path + File.separator + export_src_filename;
            intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".provider", new File(filePath)));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(Intent.createChooser(intent, Helper.getResString(R.string.myprojects_export_src_chooser_title_email)));
        }
    }

    /**
     * Set content of exported source views
     */
    private void initializeAfterExportedSourceViews(String exportedSrcFilename) {
        export_src_filename = exportedSrcFilename;
        export_source_loading_anim.cancelAnimation();
        export_source_loading_anim.setVisibility(View.GONE);
        export_source_output_stage.setVisibility(View.VISIBLE);
        export_source_output_path.setText(export_src_postfix + File.separator + export_src_filename);
    }

}
