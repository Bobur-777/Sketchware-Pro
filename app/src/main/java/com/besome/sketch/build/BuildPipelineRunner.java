package com.besome.sketch.build;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.security.Security;
import java.util.HashMap;

import a.a.a.ProjectBuilder;
import a.a.a.eC;
import a.a.a.hC;
import a.a.a.iC;
import a.a.a.jC;
import a.a.a.kC;
import a.a.a.lC;
import a.a.a.oB;
import a.a.a.wq;
import a.a.a.yB;
import a.a.a.yq;
import kellinwood.security.zipsigner.ZipSigner;
import kellinwood.security.zipsigner.optional.CustomKeySigner;
import mod.hey.studios.compiler.kotlin.KotlinCompilerBridge;
import mod.hey.studios.project.proguard.ProguardHandler;
import mod.hey.studios.project.stringfog.StringfogHandler;
import mod.hey.studios.util.Helper;
import mod.jbk.build.BuildProgressReceiver;
import mod.jbk.build.BuiltInLibraries;
import mod.jbk.build.compiler.bundle.AppBundleCompiler;
import mod.jbk.export.GetKeyStoreCredentialsDialog;
import mod.jbk.util.TestkeySignBridge;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import pro.sketchware.R;
import pro.sketchware.utility.FileUtil;

public final class BuildPipelineRunner {
    public static final int TOTAL_STEPS_DEBUG = 20;

    private BuildPipelineRunner() {
    }

    public static String runDebugBuild(Context context, String scId, BuildProgressReceiver receiver, BuildCancellationToken token) throws Exception {
        yq projectMetadata = createProjectMetadata(context, scId);
        receiver.onProgress("Deleting temporary files...", 1);
        FileUtil.deleteFile(projectMetadata.projectMyscPath);

        projectMetadata.c(context);
        projectMetadata.a();
        projectMetadata.a(context, wq.e("600"));
        if (yB.a(lC.b(scId), "custom_icon")) {
            projectMetadata.aa(wq.e() + File.separator + scId + File.separator + "mipmaps");
            if (yB.a(lC.b(scId), "isIconAdaptive", false)) {
                projectMetadata.createLauncherIconXml("""
                        <?xml version="1.0" encoding="utf-8"?>
                        <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android" >
                        <background android:drawable="@mipmap/ic_launcher_background"/>
                        <foreground android:drawable="@mipmap/ic_launcher_foreground"/>
                        <monochrome android:drawable="@mipmap/ic_launcher_monochrome"/>
                        </adaptive-icon>""");
            } else {
                projectMetadata.a(wq.e() + File.separator + scId + File.separator + "icon.png");
            }
        }

        receiver.onProgress("Generating source code...", 2);
        kC kC = jC.d(scId);
        kC.b(projectMetadata.resDirectoryPath + File.separator + "drawable-xhdpi");
        kC = jC.d(scId);
        kC.c(projectMetadata.resDirectoryPath + File.separator + "raw");
        kC = jC.d(scId);
        kC.a(projectMetadata.assetsPath + File.separator + "fonts");

        ProjectBuilder builder = new ProjectBuilder(receiver, context, projectMetadata);

        var fileManager = jC.b(scId);
        var dataManager = jC.a(scId);
        var libraryManager = jC.c(scId);
        projectMetadata.a(libraryManager, fileManager, dataManager);
        builder.buildBuiltInLibraryInformation();
        projectMetadata.b(fileManager, dataManager, libraryManager, builder.getBuiltInLibraryManager());
        projectMetadata.f();
        projectMetadata.e();

        builder.maybeExtractAapt2();
        throwIfCanceled(token);

        receiver.onProgress("Extracting built-in libraries...", 3);
        BuiltInLibraries.extractCompileAssets(receiver);
        throwIfCanceled(token);

        receiver.onProgress("AAPT2 is running...", 8);
        builder.compileResources();
        throwIfCanceled(token);

        receiver.onProgress("Generating view binding...", 11);
        builder.generateViewBinding();
        throwIfCanceled(token);

        KotlinCompilerBridge.compileKotlinCodeIfPossible(receiver, builder);
        throwIfCanceled(token);

        receiver.onProgress("Java is compiling...", 13);
        builder.compileJavaCode();
        throwIfCanceled(token);

        StringfogHandler stringfogHandler = new StringfogHandler(scId);
        stringfogHandler.start(receiver, builder);
        throwIfCanceled(token);

        ProguardHandler proguardHandler = new ProguardHandler(scId);
        proguardHandler.start(receiver, builder);
        throwIfCanceled(token);

        receiver.onProgress(builder.getDxRunningText(), 17);
        builder.createDexFilesFromClasses();
        throwIfCanceled(token);

        receiver.onProgress("Merging DEX files...", 18);
        builder.getDexFilesReady();
        throwIfCanceled(token);

        receiver.onProgress("Building APK...", 19);
        builder.buildApk();
        throwIfCanceled(token);

        receiver.onProgress("Signing APK...", 20);
        builder.signDebugApk();
        throwIfCanceled(token);

        return projectMetadata.finalToInstallApkPath;
    }

    public static ExportBuildResult runExportBuild(Context context, String scId, ExportBuildConfig config, BuildProgressReceiver receiver, BuildCancellationToken token) throws Exception {
        yq projectMetadata = createProjectMetadata(context, scId);

        receiver.onProgress("Deleting temporary files...", -1);
        FileUtil.deleteFile(projectMetadata.projectMyscPath);

        receiver.onProgress(Helper.getResString(R.string.design_run_title_ready_to_build), -1);
        oB oBVar = new oB();
        if (!oBVar.e(wq.o())) {
            oBVar.f(wq.o());
        }
        hC hCVar = new hC(scId);
        kC kCVar = new kC(scId);
        eC eCVar = new eC(scId);
        iC iCVar = new iC(scId);
        hCVar.i();
        kCVar.s();
        eCVar.g();
        eCVar.e();
        iCVar.i();
        throwIfCanceled(token);

        File outputFile = new File(getCorrectResultFilename(config, projectMetadata.releaseApkPath));
        if (outputFile.exists() && !outputFile.delete()) {
            throw new IllegalStateException("Couldn't delete file " + outputFile.getAbsolutePath());
        }
        projectMetadata.c(context);
        throwIfCanceled(token);

        projectMetadata.a(context, wq.e("600"));
        throwIfCanceled(token);

        if (yB.a(lC.b(scId), "custom_icon")) {
            projectMetadata.aa(wq.e() + File.separator + scId + File.separator + "mipmaps");
            if (yB.a(lC.b(scId), "isIconAdaptive", false)) {
                projectMetadata.createLauncherIconXml("""
                        <?xml version="1.0" encoding="utf-8"?>
                        <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android" >
                        <background android:drawable="@mipmap/ic_launcher_background"/>
                        <foreground android:drawable="@mipmap/ic_launcher_foreground"/>
                        <monochrome android:drawable="@mipmap/ic_launcher_monochrome"/>
                        </adaptive-icon>""");
            } else {
                projectMetadata.a(wq.e() + File.separator + scId + File.separator + "icon.png");
            }
        }
        projectMetadata.a();
        kCVar.b(projectMetadata.resDirectoryPath + File.separator + "drawable-xhdpi");
        kCVar.c(projectMetadata.resDirectoryPath + File.separator + "raw");
        kCVar.a(projectMetadata.assetsPath + File.separator + "fonts");

        ProjectBuilder builder = new ProjectBuilder(receiver, context, projectMetadata);
        builder.setBuildAppBundle(config.buildingAppBundle);

        projectMetadata.a(iCVar, hCVar, eCVar, config.exportType);
        builder.buildBuiltInLibraryInformation();
        projectMetadata.b(hCVar, eCVar, iCVar, builder.getBuiltInLibraryManager());
        throwIfCanceled(token);

        receiver.onProgress("Extracting AAPT/AAPT2 binaries...", -1);
        builder.maybeExtractAapt2();
        throwIfCanceled(token);

        receiver.onProgress("Extracting built-in libraries...", -1);
        BuiltInLibraries.extractCompileAssets(receiver);
        throwIfCanceled(token);

        builder.buildBuiltInLibraryInformation();

        receiver.onProgress("AAPT2 is running...", -1);
        builder.compileResources();
        throwIfCanceled(token);

        KotlinCompilerBridge.compileKotlinCodeIfPossible(receiver, builder);
        throwIfCanceled(token);

        receiver.onProgress("Java is compiling...", -1);
        builder.compileJavaCode();
        throwIfCanceled(token);

        StringfogHandler stringfogHandler = new StringfogHandler(projectMetadata.sc_id);
        stringfogHandler.start(receiver, builder);
        throwIfCanceled(token);

        ProguardHandler proguardHandler = new ProguardHandler(projectMetadata.sc_id);
        proguardHandler.start(receiver, builder);
        throwIfCanceled(token);

        receiver.onProgress(builder.getDxRunningText(), -1);
        builder.createDexFilesFromClasses();
        throwIfCanceled(token);

        receiver.onProgress("Merging libraries' DEX files...", -1);
        builder.getDexFilesReady();
        throwIfCanceled(token);

        String outputPath = null;
        if (config.buildingAppBundle) {
            AppBundleCompiler compiler = new AppBundleCompiler(builder);
            receiver.onProgress("Creating app module...", -1);
            compiler.createModuleMainArchive();
            receiver.onProgress("Building app bundle...", -1);
            compiler.buildBundle();

            receiver.onProgress("Signing app bundle...", -1);
            String createdBundlePath = AppBundleCompiler.getDefaultAppBundleOutputFile(projectMetadata).getAbsolutePath();
            String signedAppBundleDirectoryPath = FileUtil.getExternalStorageDir()
                    + File.separator + "sketchware"
                    + File.separator + "signed_aab";
            FileUtil.makeDir(signedAppBundleDirectoryPath);
            outputPath = signedAppBundleDirectoryPath + File.separator +
                    Uri.fromFile(new File(createdBundlePath)).getLastPathSegment();

            if (config.signWithTestkey) {
                ZipSigner signer = new ZipSigner();
                signer.setKeymode(ZipSigner.KEY_TESTKEY);
                signer.signZip(createdBundlePath, outputPath);
            } else if (config.isResultJarSigningEnabled()) {
                Security.addProvider(new BouncyCastleProvider());
                CustomKeySigner.signZip(
                        new ZipSigner(),
                        config.signingKeystorePath,
                        config.signingKeystorePassword.toCharArray(),
                        config.signingAliasName,
                        config.signingAliasPassword.toCharArray(),
                        config.signingAlgorithm,
                        createdBundlePath,
                        outputPath
                );
            } else {
                outputPath = getCorrectResultFilename(config, outputPath);
                FileUtil.copyFile(createdBundlePath, outputPath);
            }
        } else {
            receiver.onProgress("Building APK...", -1);
            builder.buildApk();
            throwIfCanceled(token);

            receiver.onProgress("Aligning APK...", -1);
            builder.runZipalign(builder.yq.unsignedUnalignedApkPath, builder.yq.unsignedAlignedApkPath);
            throwIfCanceled(token);

            receiver.onProgress("Signing APK...", -1);
            outputPath = getCorrectResultFilename(config, builder.yq.releaseApkPath);
            if (config.signWithTestkey) {
                TestkeySignBridge.signWithTestkey(builder.yq.unsignedAlignedApkPath, outputPath);
            } else if (config.isResultJarSigningEnabled()) {
                Security.addProvider(new BouncyCastleProvider());
                CustomKeySigner.signZip(
                        new ZipSigner(),
                        wq.j(),
                        config.signingKeystorePassword.toCharArray(),
                        config.signingAliasName,
                        config.signingKeystorePassword.toCharArray(),
                        config.signingAlgorithm,
                        builder.yq.unsignedAlignedApkPath,
                        outputPath
                );
            } else {
                FileUtil.copyFile(builder.yq.unsignedAlignedApkPath, outputPath);
            }
        }

        return new ExportBuildResult(outputPath, config.buildingAppBundle);
    }

    private static yq createProjectMetadata(Context context, String scId) {
        HashMap<String, Object> projectInfo = lC.b(scId);
        return new yq(context, wq.d(scId), projectInfo);
    }

    private static String getCorrectResultFilename(ExportBuildConfig config, String oldFormatFilename) {
        if (!config.isResultJarSigningEnabled() && !config.signWithTestkey) {
            if (config.buildingAppBundle) {
                return oldFormatFilename.replace(".aab", ".unsigned.aab");
            }
            return oldFormatFilename.replace("_release", "_release.unsigned");
        }
        return oldFormatFilename;
    }

    private static void throwIfCanceled(BuildCancellationToken token) throws BuildCanceledException {
        if (token.isCanceled()) {
            throw new BuildCanceledException();
        }
    }

    public static final class BuildCanceledException extends Exception {
        public BuildCanceledException() {
            super("Build canceled");
        }
    }

    public static final class ExportBuildConfig {
        private final yq.ExportType exportType;
        private final boolean buildingAppBundle;
        private final boolean signWithTestkey;
        private final String signingKeystorePath;
        private final String signingKeystorePassword;
        private final String signingAliasName;
        private final String signingAliasPassword;
        private final String signingAlgorithm;

        public ExportBuildConfig(yq.ExportType exportType, boolean buildingAppBundle, GetKeyStoreCredentialsDialog.Credentials credentials) {
            this.exportType = exportType;
            this.buildingAppBundle = buildingAppBundle;
            if (credentials != null && credentials.isForSigningWithTestkey()) {
                signWithTestkey = true;
                signingKeystorePath = null;
                signingKeystorePassword = null;
                signingAliasName = null;
                signingAliasPassword = null;
                signingAlgorithm = credentials.getSigningAlgorithm();
            } else if (credentials != null) {
                signWithTestkey = false;
                signingKeystorePath = wq.j();
                signingKeystorePassword = credentials.getKeyStorePassword();
                signingAliasName = credentials.getKeyAlias();
                signingAliasPassword = credentials.getKeyPassword();
                signingAlgorithm = credentials.getSigningAlgorithm();
            } else {
                signWithTestkey = false;
                signingKeystorePath = null;
                signingKeystorePassword = null;
                signingAliasName = null;
                signingAliasPassword = null;
                signingAlgorithm = null;
            }
        }

        public ExportBuildConfig(yq.ExportType exportType, boolean buildingAppBundle, boolean signWithTestkey, String signingKeystorePath, String signingKeystorePassword, String signingAliasName, String signingAliasPassword, String signingAlgorithm) {
            this.exportType = exportType;
            this.buildingAppBundle = buildingAppBundle;
            this.signWithTestkey = signWithTestkey;
            this.signingKeystorePath = signingKeystorePath;
            this.signingKeystorePassword = signingKeystorePassword;
            this.signingAliasName = signingAliasName;
            this.signingAliasPassword = signingAliasPassword;
            this.signingAlgorithm = signingAlgorithm;
        }

        public boolean isResultJarSigningEnabled() {
            return signingKeystorePath != null && signingKeystorePassword != null &&
                    signingAliasName != null && signingAliasPassword != null && signingAlgorithm != null;
        }
    }

    public static final class ExportBuildResult {
        private final String outputPath;
        private final boolean appBundle;

        public ExportBuildResult(String outputPath, boolean appBundle) {
            this.outputPath = outputPath;
            this.appBundle = appBundle;
        }

        public String getOutputPath() {
            return outputPath;
        }

        public boolean isAppBundle() {
            return appBundle;
        }
    }
}
