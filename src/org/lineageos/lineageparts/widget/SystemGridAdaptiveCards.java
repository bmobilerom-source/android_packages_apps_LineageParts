/*
 * Copyright (C) 2025 BashaMobile
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.widget;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceScreen;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** System Grid destinations hosted in LineageParts. */
public final class SystemGridAdaptiveCards {

    private static final Set<String> DESTINATIONS = new HashSet<>(Arrays.asList(
            "org.lineageos.lineageparts.input.ButtonSettings",
            "org.lineageos.lineageparts.statusbar.StatusBarSettings",
            "org.lineageos.lineageparts.statusbar.DynamicIslandSettings",
            "org.lineageos.lineageparts.statusbar.NetworkTrafficSettings",
            "org.lineageos.lineageparts.statusbar.DynamicIslandMoreSettings",
            "org.lineageos.lineageparts.sounds.ChargingSoundsSettings"
    ));

    private SystemGridAdaptiveCards() {}

    public static boolean isDestination(@Nullable String className) {
        return className != null && DESTINATIONS.contains(className);
    }

    public static boolean applyIfDestination(@Nullable Object fragment,
            @Nullable PreferenceScreen screen) {
        if (fragment == null || screen == null) {
            return false;
        }
        if (isDestination(fragment.getClass().getName())) {
            AdaptivePreferenceCardHelper.apply(screen);
            return true;
        }
        return false;
    }
}
