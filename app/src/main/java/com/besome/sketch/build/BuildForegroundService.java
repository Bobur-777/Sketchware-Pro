package com.besome.sketch.build;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import a.a.a.yq;
import kellinwood.security.zipsigner.optional.LoadKeystoreException;
import mod.jbk.build.BuildProgressReceiver;
import mod.jbk.diagnostic.MissingFileException;
import pro.sketchware.R;

public class BuildForegroundService extends Service {
    public static final String ACTION_START_BUILD = "com.besome.sketch.build.ACTION_START_BUILD";
    public static final String ACTION_CANCEL_BUILD = "com.besome.sketch.build.ACTION_CANCEL_BUILD";
    public static final String ACTION_BUILD_PROGRESS = "com.besome.sketch.build.ACTION_BUILD_PROGRESS";
    public static final String ACTION_BUILD_FINISHED = "com.besome.sketch.build.ACTION_BUILD_FINISHED";
    public static final String ACTION_BUILD_FAILED = "com.besome.sketch.build.ACTION_BUILD_FAILED";
    public static final String ACTION_BUILD_CANCELED = "com.besome.sketch.build.ACTION_BUILD_CANCELED";

    public static final String EXTRA_SC_ID = "extra_sc_id";
    public static final String EXTRA_BUILD_TYPE = "extra_build_type";
    public static final String EXTRA_EXPORT_TYPE = "extra_export_type";
    public static final String EXTRA_BUILD_APP_BUNDLE = "extra_build_app_bundle";
    public static final String EXTRA_SIGN_WITH_TESTKEY = "extra_sign_with_testkey";
    public static final String EXTRA_SIGNING_KEYSTORE_PATH = "extra_signing_keystore_path";
    public static final String EXTRA_SIGNING_KEYSTORE_PASSWORD = "extra_signing_keystore_password";
    public static final String EXTRA_SIGNING_ALIAS_NAME = "extra_signing_alias_name";
    public static final String EXTRA_SIGNING_ALIAS_PASSWORD = "extra_signing_alias_password";
    public static final String EXTRA_SIGNING_ALGORITHM = "extra_signing_algorithm";

    public static final String EXTRA_PROGRESS_TEXT = "extra_progress_text";
    public static final String EXTRA_PROGRESS_STEP = "extra_progress_step";
    public static final String EXTRA_PROGRESS_TOTAL = "extra_progress_total";
    public static final String EXTRA_RESULT_PATH = "extra_result_path";
    public static final String EXTRA_ERROR_MESSAGE = "extra_error_message";
    public static final String EXTRA_ERROR_STACKTRACE = "extra_error_stacktrace";
    public static final String EXTRA_ERROR_TYPE = "extra_error_type";
    public static final String EXTRA_MISSING_FILE_PATH = "extra_missing_file_path";
    public static final String EXTRA_MISSING_FILE_IS_DIRECTORY = "extra_missing_file_is_directory";

    public static final String BUILD_TYPE_DEBUG = "build_type_debug";
    public static final String BUILD_TYPE_EXPORT = "build_type_export";

    public static final int ERROR_TYPE_GENERIC = 0;
    public static final int ERROR_TYPE_MISSING_FILE = 1;
    public static final int ERROR_TYPE_COMPILE = 2;
    public static final int ERROR_TYPE_KEYSTORE = 3;

    private static final String CHANNEL_ID = "build_pipeline_channel";
    private static final int NOTIFICATION_ID = 1001;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean canceled = new AtomicBoolean(false);
    private NotificationManager notificationManager;

    public static Intent createDebugBuildIntent(Context context, String scId) {
        Intent intent = new Intent(context, BuildForegroundService.class);
        intent.setAction(ACTION_START_BUILD);
        intent.putExtra(EXTRA_SC_ID, scId);
        intent.putExtra(EXTRA_BUILD_TYPE, BUILD_TYPE_DEBUG);
        return intent;
    }

    public static Intent createExportBuildIntent(Context context, String scId, yq.ExportType exportType, boolean buildingAppBundle, boolean signWithTestkey, String signingKeystorePath, String signingKeystorePassword, String signingAliasName, String signingAliasPassword, String signingAlgorithm) {
        Intent intent = new Intent(context, BuildForegroundService.class);
        intent.setAction(ACTION_START_BUILD);
        intent.putExtra(EXTRA_SC_ID, scId);
        intent.putExtra(EXTRA_BUILD_TYPE, BUILD_TYPE_EXPORT);
        intent.putExtra(EXTRA_EXPORT_TYPE, exportType.name());
        intent.putExtra(EXTRA_BUILD_APP_BUNDLE, buildingAppBundle);
        intent.putExtra(EXTRA_SIGN_WITH_TESTKEY, signWithTestkey);
        intent.putExtra(EXTRA_SIGNING_KEYSTORE_PATH, signingKeystorePath);
        intent.putExtra(EXTRA_SIGNING_KEYSTORE_PASSWORD, signingKeystorePassword);
        intent.putExtra(EXTRA_SIGNING_ALIAS_NAME, signingAliasName);
        intent.putExtra(EXTRA_SIGNING_ALIAS_PASSWORD, signingAliasPassword);
        intent.putExtra(EXTRA_SIGNING_ALGORITHM, signingAlgorithm);
        return intent;
    }

    public static Intent createCancelIntent(Context context) {
        Intent intent = new Intent(context, BuildForegroundService.class);
        intent.setAction(ACTION_CANCEL_BUILD);
        return intent;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannelIfNeeded();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        if (ACTION_CANCEL_BUILD.equals(action)) {
            canceled.set(true);
            updateNotification("Canceling build...");
            return START_NOT_STICKY;
        }

        if (!ACTION_START_BUILD.equals(action)) {
            return START_NOT_STICKY;
        }

        if (!isRunning.compareAndSet(false, true)) {
            return START_NOT_STICKY;
        }

        canceled.set(false);
        startForeground(NOTIFICATION_ID, buildNotification("Starting build..."));
        executorService.execute(() -> runBuild(intent));

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow();
        isRunning.set(false);
    }

    private void runBuild(Intent intent) {
        String scId = intent.getStringExtra(EXTRA_SC_ID);
        String buildType = intent.getStringExtra(EXTRA_BUILD_TYPE);
        if (scId == null || buildType == null) {
            stopSelfSafely();
            return;
        }

        BuildCancellationToken cancellationToken = canceled::get;
        BuildProgressReceiver receiver = (progress, step) -> {
            int totalSteps = BUILD_TYPE_DEBUG.equals(buildType) ? BuildPipelineRunner.TOTAL_STEPS_DEBUG : -1;
            String notificationText = progress;
            if (step > 0 && totalSteps > 0) {
                notificationText = progress + " (" + step + " / " + totalSteps + ")";
            }
            updateNotification(notificationText);
            sendProgressBroadcast(scId, buildType, progress, step, totalSteps);
        };

        try {
            if (BUILD_TYPE_DEBUG.equals(buildType)) {
                String apkPath = BuildPipelineRunner.runDebugBuild(getApplicationContext(), scId, receiver, cancellationToken);
                sendFinishedBroadcast(scId, buildType, apkPath);
            } else if (BUILD_TYPE_EXPORT.equals(buildType)) {
                String exportTypeName = intent.getStringExtra(EXTRA_EXPORT_TYPE);
                if (exportTypeName == null) {
                    throw new IllegalArgumentException("Missing export type for build");
                }
                BuildPipelineRunner.ExportBuildConfig config = new BuildPipelineRunner.ExportBuildConfig(
                        yq.ExportType.valueOf(exportTypeName),
                        intent.getBooleanExtra(EXTRA_BUILD_APP_BUNDLE, false),
                        intent.getBooleanExtra(EXTRA_SIGN_WITH_TESTKEY, false),
                        intent.getStringExtra(EXTRA_SIGNING_KEYSTORE_PATH),
                        intent.getStringExtra(EXTRA_SIGNING_KEYSTORE_PASSWORD),
                        intent.getStringExtra(EXTRA_SIGNING_ALIAS_NAME),
                        intent.getStringExtra(EXTRA_SIGNING_ALIAS_PASSWORD),
                        intent.getStringExtra(EXTRA_SIGNING_ALGORITHM)
                );
                BuildPipelineRunner.ExportBuildResult result = BuildPipelineRunner.runExportBuild(getApplicationContext(), scId, config, receiver, cancellationToken);
                sendFinishedBroadcast(scId, buildType, result.getOutputPath());
            }
        } catch (BuildPipelineRunner.BuildCanceledException canceledException) {
            sendCanceledBroadcast(scId, buildType);
        } catch (MissingFileException missingFileException) {
            sendMissingFileBroadcast(scId, buildType, missingFileException);
        } catch (LoadKeystoreException loadKeystoreException) {
            sendErrorBroadcast(scId, buildType, ERROR_TYPE_KEYSTORE, loadKeystoreException.getMessage(), null);
        } catch (a.a.a.zy compileError) {
            sendErrorBroadcast(scId, buildType, ERROR_TYPE_COMPILE, compileError.getMessage(), null);
        } catch (Throwable throwable) {
            sendErrorBroadcast(scId, buildType, ERROR_TYPE_GENERIC, throwable.getMessage(), android.util.Log.getStackTraceString(throwable));
        } finally {
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelfSafely();
        }
    }

    private Notification buildNotification(String contentText) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_mtrl_code)
                .setContentTitle("Building project")
                .setContentText(contentText)
                .setOngoing(true)
                .setProgress(0, 0, true)
                .addAction(R.drawable.ic_cancel_white_96dp, "Cancel Build", getCancelPendingIntent())
                .build();
    }

    private void updateNotification(String progress) {
        notificationManager.notify(NOTIFICATION_ID, buildNotification(progress));
    }

    private PendingIntent getCancelPendingIntent() {
        Intent cancelIntent = createCancelIntent(this);
        return PendingIntent.getService(this, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void createNotificationChannelIfNeeded() {
        CharSequence name = "Build Notifications";
        String description = "Notifications for build progress";
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        notificationManager.createNotificationChannel(channel);
    }

    private void sendProgressBroadcast(String scId, String buildType, String progress, int step, int totalSteps) {
        Intent broadcast = new Intent(ACTION_BUILD_PROGRESS);
        broadcast.setPackage(getPackageName());
        broadcast.putExtra(EXTRA_SC_ID, scId);
        broadcast.putExtra(EXTRA_BUILD_TYPE, buildType);
        broadcast.putExtra(EXTRA_PROGRESS_TEXT, progress);
        broadcast.putExtra(EXTRA_PROGRESS_STEP, step);
        broadcast.putExtra(EXTRA_PROGRESS_TOTAL, totalSteps);
        sendBroadcast(broadcast);
    }

    private void sendFinishedBroadcast(String scId, String buildType, String resultPath) {
        Intent broadcast = new Intent(ACTION_BUILD_FINISHED);
        broadcast.setPackage(getPackageName());
        broadcast.putExtra(EXTRA_SC_ID, scId);
        broadcast.putExtra(EXTRA_BUILD_TYPE, buildType);
        broadcast.putExtra(EXTRA_RESULT_PATH, resultPath);
        sendBroadcast(broadcast);
    }

    private void sendCanceledBroadcast(String scId, String buildType) {
        Intent broadcast = new Intent(ACTION_BUILD_CANCELED);
        broadcast.setPackage(getPackageName());
        broadcast.putExtra(EXTRA_SC_ID, scId);
        broadcast.putExtra(EXTRA_BUILD_TYPE, buildType);
        sendBroadcast(broadcast);
    }

    private void sendMissingFileBroadcast(String scId, String buildType, MissingFileException exception) {
        Intent broadcast = new Intent(ACTION_BUILD_FAILED);
        broadcast.setPackage(getPackageName());
        broadcast.putExtra(EXTRA_SC_ID, scId);
        broadcast.putExtra(EXTRA_BUILD_TYPE, buildType);
        broadcast.putExtra(EXTRA_ERROR_TYPE, ERROR_TYPE_MISSING_FILE);
        broadcast.putExtra(EXTRA_ERROR_MESSAGE, exception.getMessage());
        broadcast.putExtra(EXTRA_MISSING_FILE_PATH, exception.getMissingFile().getAbsolutePath());
        broadcast.putExtra(EXTRA_MISSING_FILE_IS_DIRECTORY, exception.isMissingDirectory());
        sendBroadcast(broadcast);
    }

    private void sendErrorBroadcast(String scId, String buildType, int errorType, String message, String stackTrace) {
        Intent broadcast = new Intent(ACTION_BUILD_FAILED);
        broadcast.setPackage(getPackageName());
        broadcast.putExtra(EXTRA_SC_ID, scId);
        broadcast.putExtra(EXTRA_BUILD_TYPE, buildType);
        broadcast.putExtra(EXTRA_ERROR_TYPE, errorType);
        broadcast.putExtra(EXTRA_ERROR_MESSAGE, message);
        broadcast.putExtra(EXTRA_ERROR_STACKTRACE, stackTrace);
        sendBroadcast(broadcast);
    }

    private void stopSelfSafely() {
        isRunning.set(false);
        stopSelf();
    }
}
