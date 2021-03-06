/*
 * Copyright 2017 Adam Feinstein
 *
 * This file is part of MTG Familiar.
 *
 * MTG Familiar is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MTG Familiar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MTG Familiar.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.gelakinetic.mtgfam.helpers;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

import org.jetbrains.annotations.NotNull;


/**
 * The IndeterminateRefreshLayout should be used whenever the user can refresh the
 * contents of a view via a vertical swipe gesture. The activity that
 * instantiates this view should add an OnRefreshListener to be notified
 * whenever the swipe to refresh gesture is completed. The IndeterminateRefreshLayout
 * will notify the listener each and every time the gesture is completed again;
 * the listener is responsible for correctly determining when to actually
 * initiate a refresh of its content. If the listener determines there should
 * not be a refresh, it must call setRefreshing(false) to cancel any visual
 * indication of a refresh. If an activity wishes to show just the progress
 * animation, it should call setRefreshing(true). To disable the gesture and progress
 * animation, call setEnabled(false) on the view.
 *
 * <p> This layout should be made the parent of the view that will be refreshed as a
 * result of the gesture and can only support one direct child. This view will
 * also be made the target of the gesture and will be forced to match both the
 * width and the height supplied in this layout. The IndeterminateRefreshLayout does not
 * provide accessibility events; instead, a menu item must be provided to allow
 * refresh of the content wherever this gesture is used.</p>
 */
public class IndeterminateRefreshLayout extends ViewGroup {
    private static final float PROGRESS_BAR_HEIGHT = 4;
    private static final int[] LAYOUT_ATTRS = new int[]{android.R.attr.enabled};
    public boolean mRefreshing = false;
    private IndeterminateProgressBar mProgressBar; //the thing that shows progress is going
    private int mProgressBarHeight;

    /**
     * Constructor that is called when inflating IndeterminateRefreshLayout from XML.
     *
     * @param context A Context to pass to Super
     * @param attrs   An AttributeSet to pass to Super
     */
    public IndeterminateRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        assert getResources() != null;

        setWillNotDraw(false);
        mProgressBar = new IndeterminateProgressBar(this);
        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        mProgressBarHeight = (int) (metrics.density * PROGRESS_BAR_HEIGHT);

        final TypedArray typedArray = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
        assert typedArray != null;
        setEnabled(typedArray.getBoolean(0, true));
        typedArray.recycle();
    }

    /**
     * Notify the widget that refresh state has changed. Do not call this when
     * refresh is triggered by a swipe gesture.
     *
     * @param refreshing Whether or not the view should show refresh progress.
     */
    public void setRefreshing(boolean refreshing) {
        if (mRefreshing != refreshing) {
            mRefreshing = refreshing;
            if (mRefreshing) {
                mProgressBar.start();
            } else {
                mProgressBar.stop();
            }
        }
    }

    private View getTargetView() {
        if ((getChildCount() > 1 && !isInEditMode()) || getChildCount() == 0) {
            // IndeterminateRefreshLayout can host only one direct child
            // Also can't do much with zero children
            return null;
        } else if (getChildCount() == 1) {
            return getChildAt(0);
        }
        return null;
    }

    @Override
    public void draw(@NotNull Canvas canvas) {
        super.draw(canvas);
        mProgressBar.draw(canvas);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();
        mProgressBar.setBounds(width, mProgressBarHeight);
        View targetView = getTargetView();
        if (targetView != null) {
            final int childLeft = getPaddingLeft();
            final int childTop = getPaddingTop();
            final int childWidth = width - getPaddingLeft() - getPaddingRight();
            final int childHeight = height - getPaddingTop() - getPaddingBottom();
            try {
                targetView.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
            } catch (IllegalStateException e) {
                // eat it
            }
        }
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        View targetView = getTargetView();
        if (targetView != null) {
            int width = MeasureSpec.makeMeasureSpec(
                    getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                    MeasureSpec.EXACTLY);
            int height = MeasureSpec.makeMeasureSpec(
                    getMeasuredHeight() - getPaddingTop() - getPaddingBottom(),
                    MeasureSpec.EXACTLY);
            try {
                targetView.measure(width, height);
            } catch (IllegalStateException e) {
                // eat it
            }
        }
    }

    /**
     * Set the four colors used in the progress animation. The first color will
     * also be the color of the bar that grows in response to a user swipe
     * gesture.
     *
     * @param i  A color
     * @param i1 A color
     * @param i2 A color
     * @param i3 A color
     */
    public void setColors(int i, int i1, int i2, int i3) {
        mProgressBar.setColorScheme(i, i1, i2, i3);
    }
}