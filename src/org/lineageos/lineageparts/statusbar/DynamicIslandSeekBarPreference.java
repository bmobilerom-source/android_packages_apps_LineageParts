/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.statusbar;

import android.content.Context;
import android.os.UserHandle;
import android.util.AttributeSet;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import lineageos.providers.LineageSettings;

import org.lineageos.lineageparts.R;

public class DynamicIslandSeekBarPreference extends Preference
        implements SeekBar.OnSeekBarChangeListener {

    private TextView mValueView;
    private SeekBar mSeekBar;

    private int mMin;
    private int mMax;
    private int mDefaultValue;
    private int mStep = 1;
    private ValueFormatter mFormatter = ValueFormatter.INTEGER;

    public DynamicIslandSeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_dynamic_island_seekbar);
    }

    public void configure(int min, int max, int defaultValue, ValueFormatter formatter) {
        mMin = min;
        mMax = max;
        mDefaultValue = defaultValue;
        mFormatter = formatter;
        if (formatter == ValueFormatter.TIMEOUT_SECONDS) {
            mStep = 500;
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        mValueView = (TextView) holder.findViewById(R.id.value);
        mSeekBar = (SeekBar) holder.findViewById(R.id.seekbar_widget);
        mSeekBar.setOnSeekBarChangeListener(this);

        final int span = (mMax - mMin) / mStep;
        mSeekBar.setMax(span);
        final int current = getSetting();
        mSeekBar.setProgress(valueToProgress(current));
        updateValue(current);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        setSetting(progressToValue(seekBar.getProgress()));
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        updateValue(progressToValue(progress));
    }

    private int getSetting() {
        return LineageSettings.System.getIntForUser(
                getContext().getContentResolver(),
                getKey(),
                mDefaultValue,
                UserHandle.USER_CURRENT);
    }

    private void setSetting(int value) {
        final int clamped = Math.max(mMin, Math.min(mMax, value));
        LineageSettings.System.putIntForUser(
                getContext().getContentResolver(),
                getKey(),
                clamped,
                UserHandle.USER_CURRENT);
        updateValue(clamped);
    }

    private int valueToProgress(int value) {
        return (value - mMin) / mStep;
    }

    private int progressToValue(int progress) {
        return mMin + progress * mStep;
    }

    private void updateValue(int value) {
        if (mValueView == null) {
            return;
        }
        switch (mFormatter) {
            case TIMEOUT_SECONDS:
                mValueView.setText(
                        getContext().getString(
                                R.string.dynamic_island_auto_hide_value,
                                value / 1000f));
                break;
            case INTEGER:
            default:
                mValueView.setText(
                        getContext().getString(
                                R.string.dynamic_island_seekbar_value,
                                value));
                break;
        }
    }

    public enum ValueFormatter {
        INTEGER,
        TIMEOUT_SECONDS,
    }
}
