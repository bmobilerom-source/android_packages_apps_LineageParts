/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.preference;

import android.content.Context;
import android.util.AttributeSet;

import lineageos.preference.LineageSystemSettingSwitchPreference;
import lineageos.providers.LineageSettings;

/**
 * Keeps the legacy volume-button music toggle in sync with Music Tap settings.
 */
public class MusicTapVolumeControlsPreference extends LineageSystemSettingSwitchPreference {

    public MusicTapVolumeControlsPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public MusicTapVolumeControlsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MusicTapVolumeControlsPreference(Context context) {
        super(context);
    }

    @Override
    protected void putBoolean(String key, boolean value) {
        super.putBoolean(key, value);
        LineageSettings.System.putInt(getContext().getContentResolver(),
                LineageSettings.System.MUSICTAP_ENABLED, value ? 1 : 0);
    }

    @Override
    protected boolean getBoolean(String key, boolean defaultValue) {
        final int musicTap = LineageSettings.System.getInt(getContext().getContentResolver(),
                LineageSettings.System.MUSICTAP_ENABLED, -1);
        final int volbtn = LineageSettings.System.getInt(getContext().getContentResolver(),
                LineageSettings.System.VOLBTN_MUSIC_CONTROLS, 0);
        return musicTap == 1 || volbtn == 1;
    }
}
