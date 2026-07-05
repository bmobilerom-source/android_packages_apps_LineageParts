/*
 * SPDX-FileCopyrightText: 2018 The Android Open Source Project
 * SPDX-FileCopyrightText: 2020-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.TwoStatePreference;
import androidx.recyclerview.widget.RecyclerView;

import org.lineageos.lineageparts.R;

public class HighlightablePreferenceGroupAdapter extends PreferenceGroupAdapter {

    private static final String TAG = "HighlightableAdapter";
    @VisibleForTesting
    static final long DELAY_HIGHLIGHT_DURATION_MILLIS = 600L;
    private static final long HIGHLIGHT_DURATION = 15000L;
    private static final long HIGHLIGHT_FADE_OUT_DURATION = 500L;
    private static final long HIGHLIGHT_FADE_IN_DURATION = 200L;

    @VisibleForTesting
    final int mHighlightColor;
    @VisibleForTesting
    boolean mFadeInAnimated;

    private final int mNormalBackgroundRes;
    private final String mHighlightKey;
    private boolean mHighlightRequested;
    private int mHighlightPosition = RecyclerView.NO_POSITION;

    public HighlightablePreferenceGroupAdapter(PreferenceGroup preferenceGroup, String key,
            boolean highlightRequested) {
        super(preferenceGroup);
        mHighlightKey = key;
        mHighlightRequested = highlightRequested;
        final Context context = preferenceGroup.getContext();
        final TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground,
                outValue, true /* resolveRefs */);
        mNormalBackgroundRes = outValue.resourceId;
        mHighlightColor = context.getColor(R.color.preference_highlight_color);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        final Preference preference = getItem(position);
        if (AdaptivePreferenceCardHelper.isSwitchRowPreference(preference)
                && preference instanceof TwoStatePreference twoState
                && AdaptivePreferenceCardHelper.hasAdaptiveCardContainer(holder.itemView)) {
            bindSwitchCardRow(holder, twoState);
        } else if (AdaptivePreferenceCardHelper.hasAdaptiveCardContainer(holder.itemView)) {
            if (AdaptivePreferenceCardHelper.prefersDialogOnRowClick(preference)) {
                bindDialogPreferenceCardRow(holder, preference);
            } else if (AdaptivePreferenceCardHelper.shouldBindAdaptiveCardClickTarget(preference)) {
                bindAdaptiveCardClickTarget(holder, preference);
            } else {
                normalizeAdaptiveCardTouches(holder, preference);
            }
        }
        if (AdaptivePreferenceCardHelper.isSeekBarRowPreference(preference)) {
            AdaptivePreferenceCardHelper.finalizeSeekBarRowTouchHandling(
                    holder.itemView,
                    AdaptivePreferenceCardHelper.findSeekBarInRow(holder.itemView));
        }
        updateBackground(holder, position);
    }

    private static void normalizeAdaptiveCardTouches(
            @NonNull PreferenceViewHolder holder, @Nullable Preference preference) {
        final View itemView = holder.itemView;
        AdaptivePreferenceCardHelper.releaseCardSurfaceTouchHandling(itemView);
        if (preference == null) {
            return;
        }
        if (AdaptivePreferenceCardHelper.prefersDirectWidgetInteraction(preference)) {
            itemView.setClickable(false);
            itemView.setLongClickable(false);
        } else if (AdaptivePreferenceCardHelper.prefersDialogOnRowClick(preference)) {
            final boolean selectable = preference.isSelectable() && preference.isEnabled();
            itemView.setClickable(selectable);
            itemView.setLongClickable(selectable);
        }
    }

    private static void bindDialogPreferenceCardRow(
            @NonNull PreferenceViewHolder holder, @NonNull Preference preference) {
        final View itemView = holder.itemView;
        AdaptivePreferenceCardHelper.releaseCardSurfaceTouchHandling(itemView);
        final boolean active = preference.isSelectable() && preference.isEnabled();
        itemView.setClickable(active);
        itemView.setLongClickable(active);

        final Runnable openDialog = () ->
                AdaptivePreferenceCardHelper.activateCardRowClick(preference, itemView);
        itemView.setOnClickListener(v -> openDialog.run());

        final View cardSurface = findCardSurface(itemView);
        if (cardSurface != null) {
            cardSurface.setClickable(active);
            cardSurface.setFocusable(false);
            cardSurface.setOnTouchListener(null);
            cardSurface.setOnClickListener(v -> openDialog.run());
        }
    }

    private static void bindSwitchCardRow(
            @NonNull PreferenceViewHolder holder, @NonNull TwoStatePreference preference) {
        final View itemView = holder.itemView;
        AdaptivePreferenceCardHelper.releaseCardSurfaceTouchHandling(itemView);

        final boolean active = preference.isSelectable() && preference.isEnabled();
        final Runnable toggle = () -> preference.setChecked(!preference.isChecked());

        itemView.setClickable(active);
        itemView.setLongClickable(false);
        itemView.setOnClickListener(active ? v -> toggle.run() : null);

        final View switchWidget = AdaptivePreferenceCardHelper.findSwitchInRow(itemView);
        if (switchWidget != null) {
            switchWidget.setClickable(true);
            switchWidget.setEnabled(active);
            switchWidget.setFocusable(true);
            switchWidget.setFocusableInTouchMode(true);
            switchWidget.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    for (ViewParent parent = v.getParent(); parent != null;
                            parent = parent.getParent()) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }
                return false;
            });
        }
    }

    private static void bindAdaptiveCardClickTarget(
            @NonNull PreferenceViewHolder holder, @NonNull Preference preference) {
        final View itemView = holder.itemView;
        final View cardSurface = findCardSurface(itemView);
        if (cardSurface == null) {
            return;
        }
        cardSurface.setClickable(true);
        cardSurface.setLongClickable(false);
        cardSurface.setFocusable(false);
        cardSurface.setOnTouchListener(null);
        cardSurface.setOnClickListener(
                v -> AdaptivePreferenceCardHelper.activateCardRowClick(preference, itemView));
    }

    @Nullable
    private static View findCardSurface(@NonNull View itemView) {
        View cardSurface = itemView.findViewById(R.id.container);
        if (cardSurface == null && itemView instanceof ViewGroup group
                && group.getChildCount() > 0) {
            cardSurface = group.getChildAt(0);
        }
        return cardSurface;
    }

    @VisibleForTesting
    void updateBackground(PreferenceViewHolder holder, int position) {
        View v = holder.itemView;
        if (position == mHighlightPosition) {
            addHighlightBackground(v, !mFadeInAnimated);
        } else if (Boolean.TRUE.equals(v.getTag(R.id.preference_highlighted))) {
            removeHighlightBackground(v, false /* animate */);
        }
    }

    public void requestHighlight(View root, RecyclerView recyclerView) {
        if (mHighlightRequested || recyclerView == null || TextUtils.isEmpty(mHighlightKey)) {
            return;
        }
        root.postDelayed(() -> {
            final int position = getPreferenceAdapterPosition(mHighlightKey);
            if (position < 0) {
                return;
            }
            mHighlightRequested = true;
            recyclerView.smoothScrollToPosition(position);
            mHighlightPosition = position;
            notifyItemChanged(position);
        }, DELAY_HIGHLIGHT_DURATION_MILLIS);
    }

    public boolean isHighlightRequested() {
        return mHighlightRequested;
    }

    @VisibleForTesting
    void requestRemoveHighlightDelayed(View v) {
        v.postDelayed(() -> {
            mHighlightPosition = RecyclerView.NO_POSITION;
            removeHighlightBackground(v, true /* animate */);
        }, HIGHLIGHT_DURATION);
    }

    private void addHighlightBackground(View v, boolean animate) {
        v.setTag(R.id.preference_highlighted, true);
        if (!animate) {
            v.setBackgroundColor(mHighlightColor);
            Log.d(TAG, "AddHighlight: Not animation requested - setting highlight background");
            requestRemoveHighlightDelayed(v);
            return;
        }
        mFadeInAnimated = true;
        final ValueAnimator fadeInLoop = ValueAnimator.ofObject(
                new ArgbEvaluator(), Color.WHITE, mHighlightColor);
        fadeInLoop.setDuration(HIGHLIGHT_FADE_IN_DURATION);
        fadeInLoop.addUpdateListener(
                animator -> v.setBackgroundColor((int) animator.getAnimatedValue()));
        fadeInLoop.setRepeatMode(ValueAnimator.REVERSE);
        fadeInLoop.setRepeatCount(4);
        fadeInLoop.start();
        Log.d(TAG, "AddHighlight: starting fade in animation");
        requestRemoveHighlightDelayed(v);
    }

    private void removeHighlightBackground(View v, boolean animate) {
        if (!animate) {
            v.setTag(R.id.preference_highlighted, false);
            v.setBackgroundResource(mNormalBackgroundRes);
            Log.d(TAG, "RemoveHighlight: No animation requested - setting normal background");
            return;
        }

        if (!Boolean.TRUE.equals(v.getTag(R.id.preference_highlighted))) {
            Log.d(TAG, "RemoveHighlight: Not highlighted - skipping");
            return;
        }

        v.setTag(R.id.preference_highlighted, false);
        final ValueAnimator colorAnimation = ValueAnimator.ofObject(
                new ArgbEvaluator(), mHighlightColor, Color.WHITE);
        colorAnimation.setDuration(HIGHLIGHT_FADE_OUT_DURATION);
        colorAnimation.addUpdateListener(
                animator -> v.setBackgroundColor((int) animator.getAnimatedValue()));
        colorAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                v.setBackgroundResource(mNormalBackgroundRes);
            }
        });
        colorAnimation.start();
        Log.d(TAG, "Starting fade out animation");
    }
}
