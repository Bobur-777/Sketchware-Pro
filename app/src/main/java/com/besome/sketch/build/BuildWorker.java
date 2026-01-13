package com.besome.sketch.build;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import a.a.a.yq;
import kellinwood.security.zipsigner.optional.LoadKeystoreException;
import mod.jbk.build.BuildProgressReceiver;
import mod.jbk.diagnostic.MissingFileException;
import pro.sketchware.R;

public class BuildWorker extends Worker {
    public static final String INPUT_SC_ID = "input_sc_id";
    public static final String INPUT_BUILD_TYPE = "input_build_type";
    public static final String INPUT_EXPORT_TYPE = "input_export_type";
    public static final String INPUT_BUILD_APP_BUNDLE = "input_build_app_bundle";
    public static final String INPUT_SIGN_WITH_TESTKEY = "input_sign_with_testkey";
    public static final String INPUT_SIGNING_KEYSTORE_PATH = "input_signing_keystore_path";
    public static final String INPUT_SIGNING_KEYSTORE_PASSWORD = "input_signing_keystore_password";
    public static final String INPUT_SIGNING_ALIAS_NAME = "input_signing_alias_name";
    public static final String INPUT_SIGNING_ALIAS_PASSWORD = "input_signing_alias_password";
    public static final String INPUT_SIGNING_ALGORITHM = "input_signing_algorithm";

    public static final String BUILD_TYPE_DEBUG = "build_type_debug";
    public static final String BUILD_TYPE_EXPORT = "build_type_export";

    private static final String CHANNEL_ID = "build_pipeline_channel";
    private static final int NOTIFICATION_ID = 1001;

    private final NotificationManager notificationManager;
    private PowerManager.WakeLock wakeLock;
    private String currentScId;
    private String currentBuildType;

    public BuildWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannelIfNeeded();
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SketchwarePro:BuildPipeline");
            wakeLock.setReferenceCounted(false);
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        acquireWakeLock();
        currentScId = getInputData().getString(INPUT_SC_ID);
        currentBuildType = getInputData().getString(INPUT_BUILD_TYPE);
        if (currentScId == null || currentBuildType == null) {
            releaseWakeLock();
            return Result.failure();
        }

        try {
            setForegroundAsync(new ForegroundInfo(
                    NOTIFICATION_ID,
                    buildNotification("Starting build..."),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )).get();
        } catch (Exception exception) {
            releaseWakeLock();
            return Result.failure();
        }

        BuildCancellationToken cancellationToken = this::isStopped;
        BuildProgressReceiver receiver = (progress, step) -> {
            int totalSteps = BUILD_TYPE_DEBUG.equals(currentBuildType) ? BuildPipelineRunner.TOTAL_STEPS_DEBUG : -1;
            String notificationText = progress;
            if (step > 0 && totalSteps > 0) {
                notificationText = progress + " (" + step + " / " + totalSteps + ")";
            }
            updateNotification(notificationText);
            sendProgressBroadcast(currentScId, currentBuildType, progress, step, totalSteps);
        };

        try {
            if (BUILD_TYPE_DEBUG.equals(currentBuildType)) {
                String apkPath = BuildPipelineRunner.runDebugBuild(getApplicationContext(), currentScId, receiver, cancellationToken);
                sendFinishedBroadcast(currentScId, currentBuildType, apkPath);
            } else if (BUILD_TYPE_EXPORT.equals(currentBuildType)) {
                String exportTypeName = getInputData().getString(INPUT_EXPORT_TYPE);
                if (exportTypeName == null) {
                    throw new IllegalArgumentException("Missing export type for build");
                }
                BuildPipelineRunner.ExportBuildConfig config = new BuildPipelineRunner.ExportBuildConfig(
                        yq.ExportType.valueOf(exportTypeName),
                        getInputData().getBoolean(INPUT_BUILD_APP_BUNDLE, false),
                        getInputData().getBoolean(INPUT_SIGN_WITH_TESTKEY, false),
                        getInputData().getString(INPUT_SIGNING_KEYSTORE_PATH),
                        getInputData().getString(INPUT_SIGNING_KEYSTORE_PASSWORD),
                        getInputData().getString(INPUT_SIGNING_ALIAS_NAME),
                        getInputData().getString(INPUT_SIGNING_ALIAS_PASSWORD),
                        getInputData().getString(INPUT_SIGNING_ALGORITHM)
                );
                BuildPipelineRunner.ExportBuildResult result = BuildPipelineRunner.runExportBuild(getApplicationContext(), currentScId, config, receiver, cancellationToken);
                sendFinishedBroadcast(currentScId, currentBuildType, result.getOutputPath());
            }
        } catch (BuildPipelineRunner.BuildCanceledException canceledException) {
            sendCanceledBroadcast(currentScId, currentBuildType);
            releaseWakeLock();
            return Result.success();
        } catch (MissingFileException missingFileException) {
            sendMissingFileBroadcast(currentScId, currentBuildType, missingFileException);
            releaseWakeLock();
            return Result.failure();
        } catch (LoadKeystoreException loadKeystoreException) {
            sendErrorBroadcast(currentScId, currentBuildType, BuildForegroundService.ERROR_TYPE_KEYSTORE, loadKeystoreException.getMessage(), null);
            releaseWakeLock();
            return Result.failure();
        } catch (a.a.a.zy compileError) {
            sendErrorBroadcast(currentScId, currentBuildType, BuildForegroundService.ERROR_TYPE_COMPILE, compileError.getMessage(), null);
            releaseWakeLock();
            return Result.failure();
        } catch (Throwable throwable) {
            sendErrorBroadcast(currentScId, currentBuildType, BuildForegroundService.ERROR_TYPE_GENERIC, throwable.getMessage(), android.util.Log.getStackTraceString(throwable));
            releaseWakeLock();
            return Result.failure();
        } finally {
            releaseWakeLock();
        }

        return Result.success();
    }

    @Override
    public void onStopped() {
        super.onStopped();
        if (currentScId != null && currentBuildType != null) {
            sendCanceledBroadcast(currentScId, currentBuildType);
        }
        releaseWakeLock();
    }

    private Notification buildNotification(String contentText) {
        return new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_mtrl_code)
                .setContentTitle("Building project")
                .setContentText(contentText)
                .setOngoing(true)
                .setProgress(0, 0, true)
                .build();
    }

    private void updateNotification(String progress) {
        notificationManager.notify(NOTIFICATION_ID, buildNotification(progress));
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
        Intent broadcast = new Intent(BuildForegroundService.ACTION_BUILD_PROGRESS);
        broadcast.setPackage(getApplicationContext().getPackageName());
        broadcast.putExtra(BuildForegroundService.EXTRA_SC_ID, scId);
        broadcast.putExtra(BuildForegroundService.EXTRA_BUILD_TYPE, buildType);
        broadcast.putExtra(BuildForegroundService.EXTRA_PROGRESS_TEXT, progress);
        broadcast.putExtra(BuildForegroundService.EXTRA_PROGRESS_STEP, step);
        broadcast.putExtra(BuildForegroundService.EXTRA_PROGRESS_TOTAL, totalSteps);
        getApplicationContext().sendBroadcast(broadcast);
    }

    private void sendFinishedBroadcast(String scId, String buildType, String resultPath) {
        Intent broadcast = new Intent(BuildForegroundService.ACTION_BUILD_FINISHED);
        broadcast.setPackage(getApplicationContext().getPackageName());
        broadcast.putExtra(BuildForegroundService.EXTRA_SC_ID, scId);
        broadcast.putExtra(BuildForegroundService.EXTRA_BUILD_TYPE, buildType);
        broadcast.putExtra(BuildForegroundService.EXTRA_RESULT_PATH, resultPath);
        getApplicationContext().sendBroadcast(broadcast);
    }

    private void sendCanceledBroadcast(String scId, String buildType) {
        Intent broadcast = new Intent(BuildForegroundService.ACTION_BUILD_CANCELED);
        broadcast.setPackage(getApplicationContext().getPackageName());
        broadcast.putExtra(BuildForegroundService.EXTRA_SC_ID, scId);
        broadcast.putExtra(BuildForegroundService.EXTRA_BUILD_TYPE, buildType);
        getApplicationContext().sendBroadcast(broadcast);
    }

    private void sendMissingFileBroadcast(String scId, String buildType, MissingFileException exception) {
        Intent broadcast = new Intent(BuildForegroundService.ACTION_BUILD_FAILED);
        broadcast.setPackage(getApplicationContext().getPackageName());
        broadcast.putExtra(BuildForegroundService.EXTRA_SC_ID, scId);
        broadcast.putExtra(BuildForegroundService.EXTRA_BUILD_TYPE, buildType);
        broadcast.putExtra(BuildForegroundService.EXTRA_ERROR_TYPE, BuildForegroundService.ERROR_TYPE_MISSING_FILE);
        broadcast.putExtra(BuildForegroundService.EXTRA_ERROR_MESSAGE, exception.getMessage());
        broadcast.putExtra(BuildForegroundService.EXTRA_MISSING_FILE_PATH, exception.getMissingFile().getAbsolutePath());
        broadcast.putExtra(BuildForegroundService.EXTRA_MISSING_FILE_IS_DIRECTORY, exception.isMissingDirectory());
        getApplicationContext().sendBroadcast(broadcast);
    }

    private void sendErrorBroadcast(String scId, String buildType, int errorType, String message, String stackTrace) {
        Intent broadcast = new Intent(BuildForegroundService.ACTION_BUILD_FAILED);
        broadcast.setPackage(getApplicationContext().getPackageName());
        broadcast.putExtra(BuildForegroundService.EXTRA_SC_ID, scId);
        broadcast.putExtra(BuildForegroundService.EXTRA_BUILD_TYPE, buildType);
        broadcast.putExtra(BuildForegroundService.EXTRA_ERROR_TYPE, errorType);
        broadcast.putExtra(BuildForegroundService.EXTRA_ERROR_MESSAGE, message);
        broadcast.putExtra(BuildForegroundService.EXTRA_ERROR_STACKTRACE, stackTrace);
        getApplicationContext().sendBroadcast(broadcast);
    }

    private void acquireWakeLock() {
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire();
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }
}
