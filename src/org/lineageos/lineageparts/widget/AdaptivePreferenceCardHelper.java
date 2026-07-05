/*
 * Copyright (C) 2025 LineageOS
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.widget;

import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.preference.SwitchPreferenceCompat;
import androidx.preference.TwoStatePreference;

import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.widget.BannerMessagePreference;
import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.SelectorWithWidgetPreference;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.IllustrationPreference;
import com.android.settingslib.widget.LayoutPreference;
import com.android.settingslib.widget.TopIntroPreference;

import org.lineageos.lineageparts.R;

import java.util.ArrayList;
import java.util.List;

/** LineageParts copy of Settings {@code AdaptivePreferenceCardHelper}. */
public final class AdaptivePreferenceCardHelper {

    private static final String TAG = "AdaptivePrefCardHelper";
    private static final int DEFAULT_LAYOUT = androidx.preference.R.layout.preference;

    private AdaptivePreferenceCardHelper() {}

    public static void apply(@Nullable PreferenceScreen screen) {
        if (screen == null) {
            return;
        }

        final List<Preference> rootCards = new ArrayList<>();
        for (int i = 0; i < screen.getPreferenceCount(); i++) {
            final Preference child = screen.getPreference(i);
            if (child instanceof PreferenceCategory) {
                applyToCategory((PreferenceCategory) child);
            } else if (isCardCandidate(child)) {
                rootCards.add(child);
            }
        }
        applyLayoutGroup(rootCards);
        ensureSeekBarCardLayouts(screen);
    }

    public static boolean ensureSeekBarCardLayouts(@Nullable PreferenceScreen screen) {
        if (screen == null) {
            return false;
        }
        boolean changed = false;
        for (int i = 0; i < screen.getPreferenceCount(); i++) {
            final Preference child = screen.getPreference(i);
            if (child instanceof PreferenceCategory) {
                changed |= ensureSeekBarCardLayouts((PreferenceCategory) child);
            }
            changed |= ensureSeekBarCardLayout(child);
        }
        return changed;
    }

    private static boolean ensureSeekBarCardLayouts(@NonNull PreferenceCategory category) {
        boolean changed = false;
        for (int i = 0; i < category.getPreferenceCount(); i++) {
            final Preference child = category.getPreference(i);
            if (child instanceof PreferenceCategory) {
                changed |= ensureSeekBarCardLayouts((PreferenceCategory) child);
            }
            changed |= ensureSeekBarCardLayout(child);
        }
        return changed;
    }

    private static boolean ensureSeekBarCardLayout(@Nullable Preference preference) {
        if (preference == null || !isSeekBarPreference(preference)) {
            return false;
        }
        if (hasAdaptiveSeekbarCardLayout(preference)) {
            return false;
        }
        preference.setLayoutResource(R.layout.adaptive_preference_card_seekbar);
        return true;
    }

    private static void applyToCategory(@NonNull PreferenceCategory category) {
        final List<Preference> cards = new ArrayList<>();
        for (int i = 0; i < category.getPreferenceCount(); i++) {
            final Preference child = category.getPreference(i);
            if (child instanceof PreferenceCategory) {
                applyToCategory((PreferenceCategory) child);
                continue;
            }
            if (isCardCandidate(child)) {
                cards.add(child);
            }
        }
        applyLayoutGroup(cards);
    }

    private static void applyLayoutGroup(@NonNull List<Preference> cards) {
        cards.removeIf(pref -> !pref.isVisible());
        final int size = cards.size();
        if (size == 0) {
            return;
        }

        for (int i = 0; i < size; i++) {
            final Preference pref = cards.get(i);
            final boolean isSwitch = isSwitchPreference(pref);
            final int layout;
            if (size == 1) {
                layout = isSwitch
                        ? R.layout.adaptive_preference_card_top_switch
                        : R.layout.adaptive_preference_card;
            } else if (i == 0) {
                layout = isSwitch
                        ? R.layout.adaptive_preference_card_top_switch
                        : R.layout.adaptive_preference_card_top;
            } else if (i == size - 1) {
                layout = isSwitch
                        ? R.layout.adaptive_preference_card_bottom_switch
                        : R.layout.adaptive_preference_card_bottom;
            } else {
                layout = isSwitch
                        ? R.layout.adaptive_preference_card_middle_switch
                        : R.layout.adaptive_preference_card_middle;
            }
            pref.setLayoutResource(layout);
        }
    }

    public static boolean hasAdaptiveCardContainer(@Nullable View itemView) {
        return itemView != null && itemView.findViewById(R.id.container) != null;
    }

    public static void releaseCardSurfaceTouchHandling(@Nullable View itemView) {
        final View cardSurface = findCardSurface(itemView);
        if (cardSurface == null) {
            return;
        }
        cardSurface.setClickable(false);
        cardSurface.setLongClickable(false);
        cardSurface.setFocusable(false);
        cardSurface.setOnClickListener(null);
        cardSurface.setOnTouchListener((v, event) -> false);
    }

    public static boolean isSeekBarRowPreference(@Nullable Preference preference) {
        return isSeekBarPreference(preference) || hasAdaptiveSeekbarCardLayout(preference);
    }

    public static void finalizeSeekBarRowTouchHandling(
            @Nullable View itemView, @Nullable View seekBar) {
        releaseCardSurfaceTouchHandling(itemView);
        if (itemView != null) {
            itemView.setClickable(false);
            itemView.setLongClickable(false);
        }
        if (seekBar != null) {
            seekBar.setClickable(true);
            seekBar.setEnabled(seekBar.isEnabled());
            seekBar.setFocusable(true);
            seekBar.setFocusableInTouchMode(true);
            seekBar.setOnTouchListener((v, event) -> {
                final int action = event.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN) {
                    for (ViewParent parent = v.getParent(); parent != null;
                            parent = parent.getParent()) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                } else if (action == MotionEvent.ACTION_UP
                        || action == MotionEvent.ACTION_CANCEL) {
                    for (ViewParent parent = v.getParent(); parent != null;
                            parent = parent.getParent()) {
                        parent.requestDisallowInterceptTouchEvent(false);
                    }
                }
                return false;
            });
        }
    }

    @Nullable
    public static SeekBar findSeekBarInRow(@Nullable View itemView) {
        if (itemView == null) {
            return null;
        }
        View seekBar = itemView.findViewById(com.android.internal.R.id.seekbar);
        if (seekBar == null) {
            seekBar = itemView.findViewById(R.id.seekbar_widget);
        }
        return seekBar instanceof SeekBar ? (SeekBar) seekBar : null;
    }

    public static boolean prefersDialogOnRowClick(@Nullable Preference preference) {
        if (preference == null) {
            return false;
        }
        return preference instanceof ListPreference
                || preference instanceof MultiSelectListPreference
                || preference instanceof EditTextPreference
                || preference instanceof DialogPreference;
    }

    public static boolean shouldBindAdaptiveCardClickTarget(@Nullable Preference preference) {
        if (preference == null || !preference.isSelectable()) {
            return false;
        }
        if (prefersDirectWidgetInteraction(preference) || prefersDialogOnRowClick(preference)) {
            return false;
        }
        if (preference instanceof SelectorWithWidgetPreference) {
            return hasAdaptiveClickableCardLayout(preference);
        }
        return hasAdaptiveClickableCardLayout(preference);
    }

    /** True for switch / toggle rows on adaptive switch-card layouts. */
    public static boolean isSwitchRowPreference(@Nullable Preference preference) {
        if (preference == null) {
            return false;
        }
        return isSwitchPreference(preference) || hasAdaptiveSwitchCardLayout(preference);
    }

    /**
     * Ensures adaptive card ripples do not intercept switch widget touches; row clicks still
     * toggle via the preference framework.
     */
    public static void finalizeSwitchRowTouchHandling(
            @Nullable View itemView, @Nullable Preference preference) {
        releaseCardSurfaceTouchHandling(itemView);
        if (itemView == null) {
            return;
        }
        final boolean active = preference != null && preference.isSelectable()
                && preference.isEnabled();
        itemView.setClickable(active);
        itemView.setLongClickable(false);

        final View switchWidget = findSwitchInRow(itemView);
        if (switchWidget != null) {
            switchWidget.setClickable(true);
            switchWidget.setEnabled(switchWidget.isEnabled());
            switchWidget.setFocusable(true);
            switchWidget.setFocusableInTouchMode(true);
        }
    }

    @Nullable
    public static View findSwitchInRow(@Nullable View itemView) {
        if (itemView == null) {
            return null;
        }
        final View widgetFrame = itemView.findViewById(android.R.id.widget_frame);
        if (widgetFrame instanceof android.view.ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                final View child = group.getChildAt(i);
                if (isSwitchWidget(child)) {
                    return child;
                }
            }
        }
        if (itemView instanceof android.view.ViewGroup root) {
            return findSwitchWidgetInTree(root);
        }
        return null;
    }

    private static boolean isSwitchWidget(@Nullable View view) {
        if (view == null) {
            return false;
        }
        final String name = view.getClass().getSimpleName();
        return name.contains("Switch") || name.contains("Toggle");
    }

    @Nullable
    private static View findSwitchWidgetInTree(@NonNull android.view.ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            final View child = group.getChildAt(i);
            if (isSwitchWidget(child)) {
                return child;
            }
            if (child instanceof android.view.ViewGroup childGroup) {
                final View nested = findSwitchWidgetInTree(childGroup);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    /** True when the row hosts a widget that must receive touches directly (not via itemView). */
    public static boolean prefersDirectWidgetInteraction(@Nullable Preference preference) {
        if (preference == null) {
            return false;
        }
        return isSeekBarPreference(preference)
                || hasAdaptiveSeekbarCardLayout(preference);
    }

    public static boolean applyGroupedLayoutsToCategory(@Nullable PreferenceCategory category) {
        if (category == null) {
            return false;
        }
        final List<Preference> cards = new ArrayList<>();
        for (int i = 0; i < category.getPreferenceCount(); i++) {
            final Preference child = category.getPreference(i);
            if (isCardCandidate(child)) {
                cards.add(child);
            }
        }
        final int before = cards.size();
        applyLayoutGroup(cards);
        return before > 0;
    }

    public static void activateCardRowClick(
            @NonNull Preference preference, @NonNull View itemView) {
        if (!preference.isEnabled() || !preference.isSelectable()) {
            return;
        }
        if (preference instanceof TwoStatePreference twoState) {
            twoState.setChecked(!twoState.isChecked());
            return;
        }
        if (prefersDialogOnRowClick(preference)) {
            final PreferenceManager manager = preference.getPreferenceManager();
            if (manager != null) {
                final PreferenceManager.OnDisplayPreferenceDialogListener listener =
                        manager.getOnDisplayPreferenceDialogListener();
                if (listener != null) {
                    listener.onDisplayPreferenceDialog(preference);
                    return;
                }
            }
        }
        final Preference.OnPreferenceClickListener clickListener =
                preference.getOnPreferenceClickListener();
        if (clickListener != null && clickListener.onPreferenceClick(preference)) {
            return;
        }
        final PreferenceManager manager = preference.getPreferenceManager();
        if (manager != null) {
            final PreferenceManager.OnPreferenceTreeClickListener treeListener =
                    manager.getOnPreferenceTreeClickListener();
            if (treeListener != null && treeListener.onPreferenceTreeClick(preference)) {
                return;
            }
        }
        if (!itemView.callOnClick()) {
            itemView.setClickable(true);
            itemView.performClick();
        }
    }

    private static boolean isCardCandidate(@Nullable Preference preference) {
        if (preference == null || !preference.isVisible() || !preference.isSelectable()) {
            return false;
        }
        if (preference instanceof MainSwitchPreference) {
            return false;
        }
        if (isSeekBarPreference(preference) || hasCustomLayout(preference)) {
            return false;
        }
        if (preference instanceof FooterPreference
                || preference instanceof IllustrationPreference
                || preference instanceof LayoutPreference
                || preference instanceof TopIntroPreference
                || preference instanceof BannerMessagePreference) {
            return false;
        }
        final String simpleName = preference.getClass().getSimpleName();
        if (simpleName.contains("SeekBar")
                || simpleName.contains("BacklightBrightness")
                || simpleName.contains("ProgressBar")) {
            return false;
        }
        return true;
    }

    private static boolean isSwitchPreference(@NonNull Preference preference) {
        if (preference instanceof SwitchPreference
                || preference instanceof SwitchPreferenceCompat
                || preference instanceof RestrictedSwitchPreference
                || preference instanceof SelectorWithWidgetPreference
                || preference instanceof TwoStatePreference) {
            return true;
        }
        return preference.getClass().getName().contains("SwitchPreference");
    }

    private static boolean hasAdaptiveSwitchCardLayout(@NonNull Preference preference) {
        final int layout = preference.getLayoutResource();
        if (layout == 0 || layout == DEFAULT_LAYOUT) {
            return false;
        }
        try {
            final String layoutName = preference.getContext().getResources()
                    .getResourceEntryName(layout);
            return layoutName.startsWith("adaptive_preference_card")
                    && layoutName.contains("_switch");
        } catch (Resources.NotFoundException e) {
            return false;
        }
    }

    private static boolean isSeekBarPreference(@NonNull Preference preference) {
        return preference instanceof androidx.preference.SeekBarPreference
                || preference.getClass().getSimpleName().contains("SeekBar");
    }

    private static boolean hasCustomLayout(@NonNull Preference preference) {
        final int layout = preference.getLayoutResource();
        if (layout == 0 || layout == DEFAULT_LAYOUT) {
            return false;
        }

        try {
            final Resources resources = preference.getContext().getResources();
            final String layoutName = resources.getResourceEntryName(layout);
            if (TextUtils.equals(layoutName, "switch_preference")
                    || TextUtils.equals(layoutName, "switch_preference_compat")
                    || TextUtils.equals(layoutName, "preference")) {
                return false;
            }
            if (layoutName.startsWith("adaptive_preference_card")) {
                return true;
            }
            if (layoutName.contains("preference_card") || layoutName.contains("_card_")) {
                return true;
            }
        } catch (Resources.NotFoundException e) {
            Log.w(TAG, "Unknown layout resource on " + preference.getKey(), e);
            return true;
        }
        return true;
    }

    private static boolean hasAdaptiveClickableCardLayout(@NonNull Preference preference) {
        final int layout = preference.getLayoutResource();
        if (layout == 0 || layout == DEFAULT_LAYOUT) {
            return false;
        }
        try {
            final String layoutName = preference.getContext().getResources()
                    .getResourceEntryName(layout);
            return layoutName.startsWith("adaptive_preference_card")
                    && !layoutName.contains("seekbar")
                    && !layoutName.contains("_switch");
        } catch (Resources.NotFoundException e) {
            return false;
        }
    }

    private static boolean hasAdaptiveSeekbarCardLayout(@NonNull Preference preference) {
        final int layout = preference.getLayoutResource();
        if (layout == 0 || layout == DEFAULT_LAYOUT) {
            return false;
        }
        try {
            final String layoutName = preference.getContext().getResources()
                    .getResourceEntryName(layout);
            return layoutName.startsWith("adaptive_preference_card_seekbar")
                    || layoutName.startsWith("adaptive_dynamic_island_seekbar_card");
        } catch (Resources.NotFoundException e) {
            return false;
        }
    }

    @Nullable
    private static View findCardSurface(@Nullable View itemView) {
        if (itemView == null) {
            return null;
        }
        View cardSurface = itemView.findViewById(R.id.container);
        if (cardSurface == null && itemView instanceof android.view.ViewGroup group
                && group.getChildCount() > 0) {
            cardSurface = group.getChildAt(0);
        }
        return cardSurface;
    }
}
