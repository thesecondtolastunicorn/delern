/*
 * Copyright (C) 2017 Katarina Sheremet
 * This file is part of Delern.
 *
 * Delern is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * Delern is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with  Delern.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.dasfoo.delern;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import org.dasfoo.delern.remoteconfig.ForceUpdateChecker;
import org.dasfoo.delern.util.LogUtil;

/**
 * Splash Activity that check whether user needs force update of app or not.
 * If not, it starts DelernMainActivity.
 */
public class SplashScreenActivity extends AppCompatActivity
        implements ForceUpdateChecker.OnUpdateNeededListener {

    private static final String TAG = LogUtil.tagFor(SplashScreenActivity.class);
    private static final long ONE_HOUR = 3600;

    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    // 1 hour in seconds.
    private long mCacheExpirationSeconds = ONE_HOUR;
    private OnCompleteListener<Void> mFetchRemoteConfigListener;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get Remote Config instance.
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        // set in-app defaults
        // We set default values in case of some parameters could not found in Remote Config Server.
        mFirebaseRemoteConfig.setDefaults(R.xml.remote_config_default_settigs);

        // Create a Remote Config Setting to enable developer mode, which you can use to increase
        // the number of fetches available per hour during development.
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        mFirebaseRemoteConfig.setConfigSettings(configSettings);

        // If your app is using developer mode, cacheExpiration is set to 0, so each fetch will
        // retrieve values from the service.
        if (getApplicationContext().getPackageName().endsWith(".debug") &&
                mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            mCacheExpirationSeconds = 0;

        }

        mFetchRemoteConfigListener = new OnCompleteListener<Void>() {

            @Override
            public void onComplete(@NonNull final Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "remote config is fetched.");
                    // After config data is successfully fetched, it must be activated
                    // before newly fetched values are returned.
                    mFirebaseRemoteConfig.activateFetched();
                } else {
                    Log.e(TAG, "Remote config error:", task.getException());
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseRemoteConfig.fetch(mCacheExpirationSeconds)
                .addOnCompleteListener(mFetchRemoteConfigListener);
        ForceUpdateChecker forceUpdateChecker = new ForceUpdateChecker(this, this);
        if (forceUpdateChecker.updateIsNeeded()) {
            forceUpdateChecker.update();
        } else {
            Intent intent = new Intent(this, DelernMainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUpdateNeeded(final String updateUrl) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.new_app_version_dialog_title)
                .setMessage(R.string.update_app_user_message)
                .setPositiveButton(R.string.update,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                redirectForUpdate(updateUrl);
                            }
                        })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(final DialogInterface dialog) {
                        finish();
                    }
                })
                .create();
        dialog.show();
    }

    private void redirectForUpdate(final String googlePlayUrl) {
        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(googlePlayUrl));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}