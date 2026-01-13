package com.besome.sketch.build;

import android.content.Context;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import a.a.a.yq;

public final class BuildWorkManager {
    private BuildWorkManager() {
    }

    public static void enqueueDebugBuild(Context context, String scId) {
        Data input = new Data.Builder()
                .putString(BuildWorker.INPUT_SC_ID, scId)
                .putString(BuildWorker.INPUT_BUILD_TYPE, BuildWorker.BUILD_TYPE_DEBUG)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BuildWorker.class)
                .setInputData(input)
                .addTag(debugTag(scId))
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(debugTag(scId), ExistingWorkPolicy.REPLACE, request);
    }

    public static void enqueueExportBuild(Context context, String scId, yq.ExportType exportType, boolean buildAppBundle, boolean signWithTestkey, String keystorePath, String keystorePassword, String aliasName, String aliasPassword, String signingAlgorithm) {
        Data input = new Data.Builder()
                .putString(BuildWorker.INPUT_SC_ID, scId)
                .putString(BuildWorker.INPUT_BUILD_TYPE, BuildWorker.BUILD_TYPE_EXPORT)
                .putString(BuildWorker.INPUT_EXPORT_TYPE, exportType.name())
                .putBoolean(BuildWorker.INPUT_BUILD_APP_BUNDLE, buildAppBundle)
                .putBoolean(BuildWorker.INPUT_SIGN_WITH_TESTKEY, signWithTestkey)
                .putString(BuildWorker.INPUT_SIGNING_KEYSTORE_PATH, keystorePath)
                .putString(BuildWorker.INPUT_SIGNING_KEYSTORE_PASSWORD, keystorePassword)
                .putString(BuildWorker.INPUT_SIGNING_ALIAS_NAME, aliasName)
                .putString(BuildWorker.INPUT_SIGNING_ALIAS_PASSWORD, aliasPassword)
                .putString(BuildWorker.INPUT_SIGNING_ALGORITHM, signingAlgorithm)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BuildWorker.class)
                .setInputData(input)
                .addTag(exportTag(scId))
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(exportTag(scId), ExistingWorkPolicy.REPLACE, request);
    }

    public static void cancelDebugBuild(Context context, String scId) {
        WorkManager.getInstance(context).cancelUniqueWork(debugTag(scId));
    }

    public static void cancelExportBuild(Context context, String scId) {
        WorkManager.getInstance(context).cancelUniqueWork(exportTag(scId));
    }

    private static String debugTag(String scId) {
        return "build_debug_" + scId;
    }

    private static String exportTag(String scId) {
        return "build_export_" + scId;
    }
}
