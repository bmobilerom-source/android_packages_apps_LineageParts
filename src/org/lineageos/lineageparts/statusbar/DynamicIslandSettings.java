/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.statusbar;

import android.content.ContentResolver;
import android.os.Bundle;
import android.provider.Settings;

import androidx.preference.Preference;

import lineageos.providers.LineageSettings;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;

public class DynamicIslandSettings extends SettingsPreferenceFragment {

    private static final String KEY_ENABLE = LineageSettings.System.STATUS_BAR_DYNAMIC_ISLAND;

    private ContentResolver mResolver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.dynamic_island_settings);
        getActivity().setTitle(R.string.dynamic_island_settings_title);

        mResolver = getContext().getContentResolver();
        configureMasterToggleSync();
    }

    /**
     * Turning off the master switch must fully disable the island on notched devices where
     * auto-cutout defaults would otherwise keep it active.
     */
    private void configureMasterToggleSync() {
        Preference enablePref = findPreference(KEY_ENABLE);
        if (enablePref == null) {
            return;
        }
        enablePref.setOnPreferenceChangeListener((preference, newValue) -> {
            if (Boolean.TRUE.equals(newValue)) {
                // Mutual exclusion with Bubble Notice.
                LineageSettings.System.putInt(
                        mResolver,
                        LineageSettings.System.STATUS_BAR_BUBBLE_NOTICE,
                        0);
                Settings.System.putIntForUser(
                        mResolver,
                        LineageSettings.System.STATUS_BAR_BUBBLE_NOTICE,
                        0,
                        android.os.UserHandle.USER_CURRENT);
            } else {
                LineageSettings.System.putInt(
                        mResolver,
                        LineageSettings.System.STATUS_BAR_DYNAMIC_ISLAND_AUTO_CUTOUT,
                        0);
                Settings.System.putIntForUser(
                        mResolver,
                        LineageSettings.System.STATUS_BAR_DYNAMIC_ISLAND_AUTO_CUTOUT,
                        0,
                        android.os.UserHandle.USER_CURRENT);
            }
            return true;
        });
    }
}
