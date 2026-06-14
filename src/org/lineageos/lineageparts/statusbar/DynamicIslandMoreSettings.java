/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.statusbar;

import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.text.TextUtils;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import lineageos.providers.LineageSettings;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;
import org.lineageos.lineageparts.widget.PackageListAdapter;
import org.lineageos.lineageparts.widget.PackageListAdapter.PackageItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DynamicIslandMoreSettings extends SettingsPreferenceFragment {

    private static final String KEY_ENABLED_APPS = "dynamic_island_more_enabled_apps";
    private static final String KEY_ADD_APP = "dynamic_island_more_add_app";
    private static final String KEY_APPS_CATEGORY = "dynamic_island_more_apps_category";

    private ContentResolver mResolver;
    private PackageListAdapter mPackageAdapter;
    private PreferenceCategory mAppsCategory;
    private Preference mEnabledAppsSummaryPref;
    private Preference mAddAppPref;
    private Set<String> mEnabledApps = new LinkedHashSet<>();

    private final ContentObserver mSettingsObserver =
            new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    reloadEnabledApps();
                }
            };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.dynamic_island_more_settings);
        getActivity().setTitle(R.string.dynamic_island_more_settings_title);

        mResolver = getContext().getContentResolver();
        mAppsCategory = findPreference(KEY_APPS_CATEGORY);
        mEnabledAppsSummaryPref = findPreference(KEY_ENABLED_APPS);
        mAddAppPref = findPreference(KEY_ADD_APP);
        mPackageAdapter = new PackageListAdapter(getContext());

        configureSeekBars();
        configureAppPicker();
        updateNotificationDependentPreferences();

        mResolver.registerContentObserver(
                LineageSettings.System.getUriFor(
                        LineageSettings.System.STATUS_BAR_DYNAMIC_ISLAND_ENABLED_APPS),
                false,
                mSettingsObserver);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateNotificationDependentPreferences();
        reloadEnabledApps();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mResolver != null) {
            mResolver.unregisterContentObserver(mSettingsObserver);
        }
    }

    private void updateNotificationDependentPreferences() {
        final boolean notificationsOn =
                LineageSettings.System.getIntForUser(
                        mResolver,
                        LineageSettings.System.STATUS_BAR_DYNAMIC_ISLAND_PLUGIN_NOTIFICATION,
                        1,
                        UserHandle.USER_CURRENT) == 1;
        Preference badgeOpensShade =
                findPreference(LineageSettings.System.STATUS_BAR_DYNAMIC_ISLAND_BADGE_OPENS_SHADE);
        Preference priorityStyling =
                findPreference(LineageSettings.System.STATUS_BAR_DYNAMIC_ISLAND_PRIORITY_STYLING);
        if (badgeOpensShade != null) {
            badgeOpensShade.setEnabled(notificationsOn);
        }
        if (priorityStyling != null) {
            priorityStyling.setEnabled(notificationsOn);
        }
    }

    private void configureSeekBars() {
        configureSeekBar(
                LineageSettings.System.STATUS_BAR_DYNAMIC_ISLAND_POSITION_Y,
                -200,
                200,
                0,
                DynamicIslandSeekBarPreference.ValueFormatter.INTEGER,
                R.string.dynamic_island_more_seekbar_value);
        configureSeekBar(
                LineageSettings.System.STATUS_BAR_DYNAMIC_ISLAND_AUTO_HIDE_MS,
                500,
                60000,
                5000,
                DynamicIslandSeekBarPreference.ValueFormatter.TIMEOUT_SECONDS,
                R.string.dynamic_island_more_auto_hide_value);
    }

    private void configureSeekBar(
            String key,
            int min,
            int max,
            int defaultValue,
            DynamicIslandSeekBarPreference.ValueFormatter formatter,
            int valueStringRes) {
        DynamicIslandSeekBarPreference pref = findPreference(key);
        if (pref != null) {
            pref.configure(min, max, defaultValue, formatter, valueStringRes);
        }
    }

    private void configureAppPicker() {
        if (mEnabledAppsSummaryPref != null) {
            mEnabledAppsSummaryPref.setOnPreferenceClickListener(preference -> {
                showAppPickerDialog();
                return true;
            });
        }
        if (mAddAppPref != null) {
            mAddAppPref.setOnPreferenceClickListener(preference -> {
                showAppPickerDialog();
                return true;
            });
        }
    }

    private void showAppPickerDialog() {
        final List<String> alreadyAdded = new ArrayList<>(mEnabledApps);
        mPackageAdapter.setExcludedPackages(new HashSet<>(alreadyAdded));

        final ListView list = new ListView(requireActivity());
        list.setAdapter(mPackageAdapter);
        list.setDivider(null);
        int paddingTop = getResources().getDimensionPixelOffset(R.dimen.package_list_padding_top);
        list.setPadding(0, paddingTop, 0, 0);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle(R.string.dynamic_island_more_add_app_title);
        builder.setView(list);
        AlertDialog dialog = builder.create();

        list.setOnItemClickListener((parent, view, position, id) -> {
            PackageItem info = (PackageItem) parent.getItemAtPosition(position);
            addApp(info.packageName);
            dialog.dismiss();
        });
        dialog.show();
    }

    private void reloadEnabledApps() {
        if (mResolver == null || getContext() == null || mAppsCategory == null) {
            return;
        }
        final String raw = LineageSettings.System.getString(
                mResolver,
                LineageSettings.System.STATUS_BAR_DYNAMIC_ISLAND_ENABLED_APPS);
        mEnabledApps.clear();
        if (!TextUtils.isEmpty(raw)) {
            mEnabledApps.addAll(parseApps(raw));
        }
        updateAppsSummary();
        rebuildAppPreferences();
    }

    private void updateAppsSummary() {
        if (mEnabledAppsSummaryPref == null) {
            return;
        }
        if (mEnabledApps.isEmpty()) {
            mEnabledAppsSummaryPref.setSummary(R.string.dynamic_island_more_enabled_apps_all);
        } else {
            mEnabledAppsSummaryPref.setSummary(
                    getString(R.string.dynamic_island_more_enabled_apps_count, mEnabledApps.size()));
        }
    }

    private void rebuildAppPreferences() {
        if (mAppsCategory == null) {
            return;
        }
        mAppsCategory.removeAll();
        if (mEnabledAppsSummaryPref != null) {
            mAppsCategory.addPreference(mEnabledAppsSummaryPref);
        }
        if (mAddAppPref != null) {
            mAppsCategory.addPreference(mAddAppPref);
        }

        PackageManager pm = getContext().getPackageManager();
        for (String packageName : mEnabledApps) {
            Preference pref = new Preference(getContext());
            pref.setKey("app_" + packageName);
            try {
                pref.setTitle(pm.getApplicationLabel(
                        pm.getApplicationInfo(packageName, 0)));
            } catch (PackageManager.NameNotFoundException e) {
                pref.setTitle(packageName);
            }
            pref.setSummary(packageName);
            pref.setIcon(R.drawable.ic_menu_delete);
            pref.setOnPreferenceClickListener(p -> {
                removeApp(packageName);
                return true;
            });
            mAppsCategory.addPreference(pref);
        }
    }

    private void addApp(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return;
        }
        mEnabledApps.add(packageName);
        persistEnabledApps();
    }

    private void removeApp(String packageName) {
        mEnabledApps.remove(packageName);
        persistEnabledApps();
    }

    private void persistEnabledApps() {
        final String value = TextUtils.join(",", mEnabledApps);
        LineageSettings.System.putString(
                mResolver,
                LineageSettings.System.STATUS_BAR_DYNAMIC_ISLAND_ENABLED_APPS,
                value);
        reloadEnabledApps();
    }

    private static Set<String> parseApps(String raw) {
        Set<String> apps = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            final String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                apps.add(trimmed);
            }
        }
        return apps;
    }
}
