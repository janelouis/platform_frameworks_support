/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.support.v17.leanback.widget;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v17.leanback.R;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ListRowPresenterTest {

    static final float DELTA = 1f;
    // default overlay color when setSelectLevel(0.5f)
    static final int HALF_OVERLAY_COLOR = 0x4C000000;
    static int sFocusedZ;

    static class DummyPresenter extends Presenter {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            View view = new View(parent.getContext());
            view.setFocusable(true);
            view.setId(R.id.lb_action_button);
            view.setLayoutParams(new ViewGroup.LayoutParams(100, 100));
            return new Presenter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {
        }
    }

    Context mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    ListRowPresenter mListRowPresenter;
    ListRow mRow;
    ListRowPresenter.ViewHolder mListVh;

    void setup(ListRowPresenter listRowPresenter, ObjectAdapter adapter) {
        sFocusedZ = mContext.getResources().getDimensionPixelSize(
                R.dimen.lb_material_shadow_focused_z);
        assertTrue(sFocusedZ > 0);
        mListRowPresenter = listRowPresenter;
        mRow = new ListRow(adapter);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                final ViewGroup parent = new FrameLayout(mContext);
                Presenter.ViewHolder containerVh = mListRowPresenter.onCreateViewHolder(parent);
                parent.addView(containerVh.view, 1000, 1000);
                mListVh = (ListRowPresenter.ViewHolder) mListRowPresenter.getRowViewHolder(
                        containerVh);
                mListRowPresenter.onBindViewHolder(mListVh, mRow);
                runRecyclerViewLayout();
            }
        });
    }

    void runRecyclerViewLayout() {
        mListVh.view.measure(View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));
        mListVh.view.layout(0, 0, 1000, 1000);
    }

    static void assertChildrenHaveAlpha(ViewGroup group, int numChildren, float alpha) {
        assertEquals(numChildren, group.getChildCount());
        for (int i = 0; i < numChildren; i++) {
            assertEquals(alpha, group.getChildAt(i).getAlpha());
        }
    }

    static Drawable getForeground(View view) {
        if (Build.VERSION.SDK_INT >= 23) {
            return view.getForeground();
        }
        return null;
    }

    static void assertChildrenHaveColorOverlay(ViewGroup group, int numChildren, int overlayColor,
            boolean keepChildForeground) {
        assertEquals(numChildren, group.getChildCount());
        for (int i = 0; i < numChildren; i++) {
            View view = group.getChildAt(i);
            if (keepChildForeground) {
                assertTrue("When keepChildForeground, always create a"
                        + " ShadowOverlayContainer", view instanceof ShadowOverlayContainer);
                assertEquals(overlayColor, ((ShadowOverlayContainer) view).mOverlayColor);
            } else {
                if (view instanceof ShadowOverlayContainer) {
                    assertEquals(overlayColor, ((ShadowOverlayContainer) view).mOverlayColor);
                } else {
                    Drawable foreground = getForeground(view);
                    assertEquals(overlayColor,
                            foreground instanceof ColorDrawable
                                    ? ((ColorDrawable) foreground).getColor() : Color.TRANSPARENT);
                }
            }
        }
    }

    public void defaultListRowOverlayColor(ListRowPresenter listRowPresenter) {
        final ArrayObjectAdapter arrayAdapter = new ArrayObjectAdapter(new DummyPresenter());
        arrayAdapter.add("abc");
        setup(listRowPresenter, arrayAdapter);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mListVh.getGridView().setItemAnimator(null);
                mListRowPresenter.setSelectLevel(mListVh, 0.5f);
            }
        });
        boolean keepChildForeground = listRowPresenter.isKeepChildForeground();
        assertChildrenHaveColorOverlay(mListVh.getGridView(), 1, HALF_OVERLAY_COLOR,
                keepChildForeground);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                arrayAdapter.add("def");
                runRecyclerViewLayout();
            }
        });
        assertChildrenHaveColorOverlay(mListVh.getGridView(), 2, HALF_OVERLAY_COLOR,
                keepChildForeground);
    }

    @Test
    public void defaultListRowOverlayColor() {
        defaultListRowOverlayColor(new ListRowPresenter());
    }

    @Test
    public void defaultListRowOverlayColorCombinations() {
        for (boolean keepChildForeground: new boolean[] {false, true}) {
            for (boolean isUzingZorder : new boolean[]{false, true}) {
                for (boolean isUsingDefaultShadow : new boolean[]{false, true}) {
                    for (boolean enableRoundedCorner : new boolean[]{false, true}) {
                        for (boolean shadowEnabled : new boolean[]{false, true}) {
                            final boolean paramIsUsingZorder = isUzingZorder;
                            final boolean paramIsUsingDefaultShadow = isUsingDefaultShadow;
                            ListRowPresenter presenter = new ListRowPresenter() {
                                @Override
                                public boolean isUsingZOrder(Context context) {
                                    return paramIsUsingZorder;
                                }

                                @Override
                                public boolean isUsingDefaultShadow() {
                                    return paramIsUsingDefaultShadow;
                                }
                            };
                            presenter.setKeepChildForeground(keepChildForeground);
                            presenter.setShadowEnabled(shadowEnabled);
                            presenter.enableChildRoundedCorners(enableRoundedCorner);
                            defaultListRowOverlayColor(presenter);
                        }
                    }
                }
            }
        }
    }

    static class CustomSelectEffectRowPresenter extends ListRowPresenter {
        @Override
        public boolean isUsingDefaultListSelectEffect() {
            return false;
        }

        @Override
        protected void applySelectLevelToChild(ViewHolder rowViewHolder, View childView) {
            childView.setAlpha(rowViewHolder.getSelectLevel());
        }
    };

    public void customListRowSelectEffect(ListRowPresenter presenter) {
        final ArrayObjectAdapter arrayAdapter = new ArrayObjectAdapter(new DummyPresenter());
        arrayAdapter.add("abc");
        setup(presenter, arrayAdapter);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mListVh.getGridView().setItemAnimator(null);
                mListRowPresenter.setSelectLevel(mListVh, 0.5f);
            }
        });
        assertChildrenHaveAlpha(mListVh.getGridView(), 1, 0.5f);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                arrayAdapter.add("def");
                runRecyclerViewLayout();
            }
        });
        assertChildrenHaveAlpha(mListVh.getGridView(), 2, 0.5f);
    }

    @Test
    public void customListRowSelectEffect() {
        customListRowSelectEffect(new CustomSelectEffectRowPresenter());
    }

    @Test
    public void customListRowSelectEffectCombinations() {
        for (boolean keepChildForeground: new boolean[] {false, true}) {
            for (boolean isUzingZorder: new boolean[] {false, true}) {
                for (boolean isUsingDefaultShadow: new boolean[] {false, true}) {
                    for (boolean enableRoundedCorner : new boolean[]{false, true}) {
                        for (boolean shadowEnabled : new boolean[]{false, true}) {
                            final boolean paramIsUsingZorder = isUzingZorder;
                            final boolean paramIsUsingDefaultShadow = isUsingDefaultShadow;
                            ListRowPresenter presenter = new CustomSelectEffectRowPresenter() {
                                @Override
                                public boolean isUsingZOrder(Context context) {
                                    return paramIsUsingZorder;
                                }

                                @Override
                                public boolean isUsingDefaultShadow() {
                                    return paramIsUsingDefaultShadow;
                                }
                            };
                            presenter.setKeepChildForeground(keepChildForeground);
                            presenter.setShadowEnabled(shadowEnabled);
                            presenter.enableChildRoundedCorners(enableRoundedCorner);
                            customListRowSelectEffect(presenter);
                        }
                    }
                }
            }
        }
    }

    static class ShadowOverlayResult {
        boolean mShadowOverlayContainer;
        int mShadowOverlayContainerOverlayColor;
        float mShadowOverlayContainerOverlayZ;
        boolean mShadowOverlayContainerOpticalBounds;
        int mViewOverlayColor;
        float mViewZ;
        boolean mViewOpticalBounds;
        void expect(boolean shadowOverlayContainer, int shadowOverlayContainerOverlayColor,
                float shadowOverlayContainerOverlayZ, boolean shadowOverlayContainerOpticalBounds,
                int viewOverlayColor, float viewZ, boolean viewOpticalBounds) {
            assertEquals(this.mShadowOverlayContainer, shadowOverlayContainer);
            assertEquals(this.mShadowOverlayContainerOverlayColor,
                    shadowOverlayContainerOverlayColor);
            assertEquals(this.mShadowOverlayContainerOverlayZ, shadowOverlayContainerOverlayZ,
                    DELTA);
            assertEquals(this.mShadowOverlayContainerOpticalBounds,
                    shadowOverlayContainerOpticalBounds);
            assertEquals(this.mViewOverlayColor, viewOverlayColor);
            assertEquals(this.mViewZ, viewZ, DELTA);
            assertEquals(this.mViewOpticalBounds, viewOpticalBounds);
        }
    }

    ShadowOverlayResult setupDefaultPresenterWithSingleElement(final boolean isUsingZorder,
            final boolean isUsingDefaultShadow,
            final boolean enableRoundedCorner,
            final boolean shadowEnabled,
            final boolean keepChildForeground) {
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(new DummyPresenter());
        adapter.add("abc");
        ListRowPresenter listRowPresenter = new ListRowPresenter() {
            @Override
            public boolean isUsingZOrder(Context context) {
                return isUsingZorder;
            }

            @Override
            public boolean isUsingDefaultShadow() {
                return isUsingDefaultShadow;
            }
        };
        listRowPresenter.setShadowEnabled(shadowEnabled);
        listRowPresenter.enableChildRoundedCorners(enableRoundedCorner);
        listRowPresenter.setKeepChildForeground(keepChildForeground);
        setup(listRowPresenter, adapter);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mListVh.getGridView().setItemAnimator(null);
                mListRowPresenter.setSelectLevel(mListVh, 0.5f);
                View child = mListVh.getGridView().getChildAt(0);
                FocusHighlightHelper.FocusAnimator animator = (FocusHighlightHelper.FocusAnimator)
                        child.getTag(R.id.lb_focus_animator);
                animator.animateFocus(true, true);
            }
        });
        return collectResult();
    }

    ShadowOverlayResult collectResult() {
        ShadowOverlayResult result = new ShadowOverlayResult();
        View view = mListVh.getGridView().getChildAt(0);
        if (view instanceof ShadowOverlayContainer) {
            result.mShadowOverlayContainer = true;
            result.mShadowOverlayContainerOverlayColor = ((ShadowOverlayContainer) view)
                    .mOverlayColor;
            result.mShadowOverlayContainerOverlayZ = ViewCompat.getZ(view);
            result.mShadowOverlayContainerOpticalBounds = view.getWidth() > 100;
        } else {
            result.mShadowOverlayContainer = false;
        }
        view = view.findViewById(R.id.lb_action_button);
        Drawable d = getForeground(view);
        result.mViewOverlayColor = d instanceof ColorDrawable ? ((ColorDrawable) d).getColor()
                : Color.TRANSPARENT;
        result.mViewZ = ViewCompat.getZ(view);
        result.mViewOpticalBounds = view.getWidth() > 100;
        return result;
    }

    @Test
    public void shadowOverlayContainerTest01() {
        final boolean isUsingZorder = false;
        final boolean isUsingDefaultShadow = false;
        final boolean enableRoundedCorner = false;
        final boolean shadowEnabled = false;
        final boolean keepChildForeground = false;
        ShadowOverlayResult result = setupDefaultPresenterWithSingleElement(isUsingZorder,
                isUsingDefaultShadow, enableRoundedCorner, shadowEnabled, keepChildForeground);
        final int version = Build.VERSION.SDK_INT;
        if (version < 21) {
            // nothing supported
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else if (version < 23) {
            // supports static/dynamic shadow, supports rounded corner
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else {
            // supports foreground
            result.expect(false, 0, 0, false,
                    HALF_OVERLAY_COLOR, 0, false);
        }
    }

    @Test
    public void shadowOverlayContainerTest02() {
        final boolean isUsingZorder = false;
        final boolean isUsingDefaultShadow = false;
        final boolean enableRoundedCorner = false;
        final boolean shadowEnabled = false;
        final boolean keepChildForeground = true;
        ShadowOverlayResult result = setupDefaultPresenterWithSingleElement(isUsingZorder,
                isUsingDefaultShadow, enableRoundedCorner, shadowEnabled, keepChildForeground);
        final int version = Build.VERSION.SDK_INT;
        if (version < 21) {
            // nothing supported
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else if (version < 23) {
            // supports static/dynamic shadow, supports rounded corner
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else {
            // supports foreground
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        }
    }

    @Test
    public void shadowOverlayContainerTest03() {
        final boolean isUsingZorder = false;
        final boolean isUsingDefaultShadow = false;
        final boolean enableRoundedCorner = false;
        final boolean shadowEnabled = true;
        final boolean keepChildForeground = false;
        ShadowOverlayResult result = setupDefaultPresenterWithSingleElement(isUsingZorder,
                isUsingDefaultShadow, enableRoundedCorner, shadowEnabled, keepChildForeground);
        final int version = Build.VERSION.SDK_INT;
        if (version < 21) {
            // nothing supported
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else if (version < 23) {
            // supports static/dynamic shadow, supports rounded corner
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else {
            // supports foreground
            result.expect(false, 0, 0, false,
                    HALF_OVERLAY_COLOR, 0, false);
        }
    }

    @Test
    public void shadowOverlayContainerTest04() {
        final boolean isUsingZorder = false;
        final boolean isUsingDefaultShadow = false;
        final boolean enableRoundedCorner = false;
        final boolean shadowEnabled = true;
        final boolean keepChildForeground = true;
        ShadowOverlayResult result = setupDefaultPresenterWithSingleElement(isUsingZorder,
                isUsingDefaultShadow, enableRoundedCorner, shadowEnabled, keepChildForeground);
        final int version = Build.VERSION.SDK_INT;
        if (version < 21) {
            // nothing supported
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else if (version < 23) {
            // supports static/dynamic shadow, supports rounded corner
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else {
            // supports foreground
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        }
    }

    @Test
    public void shadowOverlayContainerTest05() {
        final boolean isUsingZorder = false;
        final boolean isUsingDefaultShadow = false;
        final boolean enableRoundedCorner = true;
        final boolean shadowEnabled = false;
        final boolean keepChildForeground = false;
        ShadowOverlayResult result = setupDefaultPresenterWithSingleElement(isUsingZorder,
                isUsingDefaultShadow, enableRoundedCorner, shadowEnabled, keepChildForeground);
        final int version = Build.VERSION.SDK_INT;
        if (version < 21) {
            // nothing supported
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else if (version < 23) {
            // supports static/dynamic shadow, supports rounded corner
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else {
            // supports foreground
            result.expect(false, 0, 0, false,
                    HALF_OVERLAY_COLOR, 0, false);
        }
    }

    @Test
    public void shadowOverlayContainerTest06() {
        final boolean isUsingZorder = false;
        final boolean isUsingDefaultShadow = false;
        final boolean enableRoundedCorner = true;
        final boolean shadowEnabled = false;
        final boolean keepChildForeground = true;
        ShadowOverlayResult result = setupDefaultPresenterWithSingleElement(isUsingZorder,
                isUsingDefaultShadow, enableRoundedCorner, shadowEnabled, keepChildForeground);
        final int version = Build.VERSION.SDK_INT;
        if (version < 21) {
            // nothing supported
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else if (version < 23) {
            // supports static/dynamic shadow, supports rounded corner
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else {
            // supports foreground
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        }
    }

    @Test
    public void shadowOverlayContainerTest07() {
        final boolean isUsingZorder = false;
        final boolean isUsingDefaultShadow = false;
        final boolean enableRoundedCorner = true;
        final boolean shadowEnabled = true;
        final boolean keepChildForeground = false;
        ShadowOverlayResult result = setupDefaultPresenterWithSingleElement(isUsingZorder,
                isUsingDefaultShadow, enableRoundedCorner, shadowEnabled, keepChildForeground);
        final int version = Build.VERSION.SDK_INT;
        if (version < 21) {
            // nothing supported
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else if (version < 23) {
            // supports static/dynamic shadow, supports rounded corner
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else {
            // supports foreground
            result.expect(false, 0, 0, false,
                    HALF_OVERLAY_COLOR, 0, false);
        }
    }

    @Test
    public void shadowOverlayContainerTest08() {
        final boolean isUsingZorder = false;
        final boolean isUsingDefaultShadow = false;
        final boolean enableRoundedCorner = true;
        final boolean shadowEnabled = true;
        final boolean keepChildForeground = true;
        ShadowOverlayResult result = setupDefaultPresenterWithSingleElement(isUsingZorder,
                isUsingDefaultShadow, enableRoundedCorner, shadowEnabled, keepChildForeground);
        final int version = Build.VERSION.SDK_INT;
        if (version < 21) {
            // nothing supported
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else if (version < 23) {
            // supports static/dynamic shadow, supports rounded corner
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else {
            // supports foreground
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        }
    }

    @Test
    public void shadowOverlayContainerTest09() {
        final boolean isUsingZorder = false;
        final boolean isUsingDefaultShadow = true;
        final boolean enableRoundedCorner = false;
        final boolean shadowEnabled = false;
        final boolean keepChildForeground = false;
        ShadowOverlayResult result = setupDefaultPresenterWithSingleElement(isUsingZorder,
                isUsingDefaultShadow, enableRoundedCorner, shadowEnabled, keepChildForeground);
        final int version = Build.VERSION.SDK_INT;
        if (version < 21) {
            // nothing supported
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else if (version < 23) {
            // supports static/dynamic shadow, supports rounded corner
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else {
            // supports foreground
            result.expect(false, 0, 0, false,
                    HALF_OVERLAY_COLOR, 0, false);
        }
    }

    @Test
    public void shadowOverlayContainerTest10() {
        final boolean isUsingZorder = false;
        final boolean isUsingDefaultShadow = true;
        final boolean enableRoundedCorner = false;
        final boolean shadowEnabled = false;
        final boolean keepChildForeground = true;
        ShadowOverlayResult result = setupDefaultPresenterWithSingleElement(isUsingZorder,
                isUsingDefaultShadow, enableRoundedCorner, shadowEnabled, keepChildForeground);
        final int version = Build.VERSION.SDK_INT;
        if (version < 21) {
            // nothing supported
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else if (version < 23) {
            // supports static/dynamic shadow, supports rounded corner
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else {
            // supports foreground
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        }
    }

    @Test
    public void shadowOverlayContainerTest11() {
        final boolean isUsingZorder = false;
        final boolean isUsingDefaultShadow = true;
        final boolean enableRoundedCorner = false;
        final boolean shadowEnabled = true;
        final boolean keepChildForeground = false;
        ShadowOverlayResult result = setupDefaultPresenterWithSingleElement(isUsingZorder,
                isUsingDefaultShadow, enableRoundedCorner, shadowEnabled, keepChildForeground);
        final int version = Build.VERSION.SDK_INT;
        if (version < 21) {
            // nothing supported
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else if (version < 23) {
            // supports static/dynamic shadow, supports rounded corner
            result.expect(true, HALF_OVERLAY_COLOR, 0, true,
                    0, 0, false);
        } else {
            // supports foreground
            result.expect(true, HALF_OVERLAY_COLOR, 0, true,
                    0, 0, false);
        }
    }

    @Test
    public void shadowOverlayContainerTest12() {
        final boolean isUsingZorder = false;
        final boolean isUsingDefaultShadow = true;
        final boolean enableRoundedCorner = false;
        final boolean shadowEnabled = true;
        final boolean keepChildForeground = true;
        ShadowOverlayResult result = setupDefaultPresenterWithSingleElement(isUsingZorder,
                isUsingDefaultShadow, enableRoundedCorner, shadowEnabled, keepChildForeground);
        final int version = Build.VERSION.SDK_INT;
        if (version < 21) {
            // nothing supported
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else if (version < 23) {
            // supports static/dynamic shadow, supports rounded corner
            result.expect(true, HALF_OVERLAY_COLOR, 0, true,
                    0, 0, false);
        } else {
            // supports foreground
            result.expect(true, HALF_OVERLAY_COLOR, 0, true,
                    0, 0, false);
        }
    }

    @Test
    public void shadowOverlayContainerTest13() {
        final boolean isUsingZorder = false;
        final boolean isUsingDefaultShadow = true;
        final boolean enableRoundedCorner = true;
        final boolean shadowEnabled = false;
        final boolean keepChildForeground = false;
        ShadowOverlayResult result = setupDefaultPresenterWithSingleElement(isUsingZorder,
                isUsingDefaultShadow, enableRoundedCorner, shadowEnabled, keepChildForeground);
        final int version = Build.VERSION.SDK_INT;
        if (version < 21) {
            // nothing supported
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else if (version < 23) {
            // supports static/dynamic shadow, supports rounded corner
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else {
            // supports foreground
            result.expect(false, 0, 0, false,
                    HALF_OVERLAY_COLOR, 0, false);
        }
    }

    @Test
    public void shadowOverlayContainerTest14() {
        final boolean isUsingZorder = false;
        final boolean isUsingDefaultShadow = true;
        final boolean enableRoundedCorner = true;
        final boolean shadowEnabled = false;
        final boolean keepChildForeground = true;
        ShadowOverlayResult result = setupDefaultPresenterWithSingleElement(isUsingZorder,
                isUsingDefaultShadow, enableRoundedCorner, shadowEnabled, keepChildForeground);
        final int version = Build.VERSION.SDK_INT;
        if (version < 21) {
            // nothing supported
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else if (version < 23) {
            // supports static/dynamic shadow, supports rounded corner
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else {
            // supports foreground
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        }
    }

    @Test
    public void shadowOverlayContainerTest15() {
        final boolean isUsingZorder = false;
        final boolean isUsingDefaultShadow = true;
        final boolean enableRoundedCorner = true;
        final boolean shadowEnabled = true;
        final boolean keepChildForeground = false;
        ShadowOverlayResult result = setupDefaultPresenterWithSingleElement(isUsingZorder,
                isUsingDefaultShadow, enableRoundedCorner, shadowEnabled, keepChildForeground);
        final int version = Build.VERSION.SDK_INT;
        if (version < 21) {
            // nothing supported
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else if (version < 23) {
            // supports static/dynamic shadow, supports rounded corner
            result.expect(true, HALF_OVERLAY_COLOR, 0, true,
                    0, 0, false);
        } else {
            // supports foreground
            result.expect(true, HALF_OVERLAY_COLOR, 0, true,
                    0, 0, false);
        }
    }

    @Test
    public void shadowOverlayContainerTest16() {
        final boolean isUsingZorder = false;
        final boolean isUsingDefaultShadow = true;
        final boolean enableRoundedCorner = true;
        final boolean shadowEnabled = true;
        final boolean keepChildForeground = true;
        ShadowOverlayResult result = setupDefaultPresenterWithSingleElement(isUsingZorder,
                isUsingDefaultShadow, enableRoundedCorner, shadowEnabled, keepChildForeground);
        final int version = Build.VERSION.SDK_INT;
        if (version < 21) {
            // nothing supported
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else if (version < 23) {
            // supports static/dynamic shadow, supports rounded corner
            result.expect(true, HALF_OVERLAY_COLOR, 0, true,
                    0, 0, false);
        } else {
            // supports foreground
            result.expect(true, HALF_OVERLAY_COLOR, 0, true,
                    0, 0, false);
        }
    }

    @Test
    public void shadowOverlayContainerTest17() {
        final boolean isUsingZorder = true;
        final boolean isUsingDefaultShadow = false;
        final boolean enableRoundedCorner = false;
        final boolean shadowEnabled = false;
        final boolean keepChildForeground = false;
        ShadowOverlayResult result = setupDefaultPresenterWithSingleElement(isUsingZorder,
                isUsingDefaultShadow, enableRoundedCorner, shadowEnabled, keepChildForeground);
        final int version = Build.VERSION.SDK_INT;
        if (version < 21) {
            // nothing supported
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else if (version < 23) {
            // supports static/dynamic shadow, supports rounded corner
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else {
            // supports foreground
            result.expect(false, 0, 0, false,
                    HALF_OVERLAY_COLOR, 0, false);
        }
    }

    @Test
    public void shadowOverlayContainerTest18() {
        final boolean isUsingZorder = true;
        final boolean isUsingDefaultShadow = false;
        final boolean enableRoundedCorner = false;
        final boolean shadowEnabled = false;
        final boolean keepChildForeground = true;
        ShadowOverlayResult result = setupDefaultPresenterWithSingleElement(isUsingZorder,
                isUsingDefaultShadow, enableRoundedCorner, shadowEnabled, keepChildForeground);
        final int version = Build.VERSION.SDK_INT;
        if (version < 21) {
            // nothing supported
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else if (version < 23) {
            // supports static/dynamic shadow, supports rounded corner
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else {
            // supports foreground
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        }
    }

    @Test
    public void shadowOverlayContainerTest19() {
        final boolean isUsingZorder = true;
        final boolean isUsingDefaultShadow = false;
        final boolean enableRoundedCorner = false;
        final boolean shadowEnabled = true;
        final boolean keepChildForeground = false;
        ShadowOverlayResult result = setupDefaultPresenterWithSingleElement(isUsingZorder,
                isUsingDefaultShadow, enableRoundedCorner, shadowEnabled, keepChildForeground);
        final int version = Build.VERSION.SDK_INT;
        if (version < 21) {
            // nothing supported
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else if (version < 23) {
            // supports static/dynamic shadow, supports rounded corner
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else {
            // supports foreground
            result.expect(false, 0, 0, false,
                    HALF_OVERLAY_COLOR, 0, false);
        }
    }

    @Test
    public void shadowOverlayContainerTest20() {
        final boolean isUsingZorder = true;
        final boolean isUsingDefaultShadow = false;
        final boolean enableRoundedCorner = false;
        final boolean shadowEnabled = true;
        final boolean keepChildForeground = true;
        ShadowOverlayResult result = setupDefaultPresenterWithSingleElement(isUsingZorder,
                isUsingDefaultShadow, enableRoundedCorner, shadowEnabled, keepChildForeground);
        final int version = Build.VERSION.SDK_INT;
        if (version < 21) {
            // nothing supported
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else if (version < 23) {
            // supports static/dynamic shadow, supports rounded corner
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else {
            // supports foreground
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        }
    }

    @Test
    public void shadowOverlayContainerTest21() {
        final boolean isUsingZorder = true;
        final boolean isUsingDefaultShadow = false;
        final boolean enableRoundedCorner = true;
        final boolean shadowEnabled = false;
        final boolean keepChildForeground = false;
        ShadowOverlayResult result = setupDefaultPresenterWithSingleElement(isUsingZorder,
                isUsingDefaultShadow, enableRoundedCorner, shadowEnabled, keepChildForeground);
        final int version = Build.VERSION.SDK_INT;
        if (version < 21) {
            // nothing supported
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else if (version < 23) {
            // supports static/dynamic shadow, supports rounded corner
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else {
            // supports foreground
            result.expect(false, 0, 0, false,
                    HALF_OVERLAY_COLOR, 0, false);
        }
    }

    @Test
    public void shadowOverlayContainerTest22() {
        final boolean isUsingZorder = true;
        final boolean isUsingDefaultShadow = false;
        final boolean enableRoundedCorner = true;
        final boolean shadowEnabled = false;
        final boolean keepChildForeground = true;
        ShadowOverlayResult result = setupDefaultPresenterWithSingleElement(isUsingZorder,
                isUsingDefaultShadow, enableRoundedCorner, shadowEnabled, keepChildForeground);
        final int version = Build.VERSION.SDK_INT;
        if (version < 21) {
            // nothing supported
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else if (version < 23) {
            // supports static/dynamic shadow, supports rounded corner
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else {
            // supports foreground
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        }
    }

    @Test
    public void shadowOverlayContainerTest23() {
        final boolean isUsingZorder = true;
        final boolean isUsingDefaultShadow = false;
        final boolean enableRoundedCorner = true;
        final boolean shadowEnabled = true;
        final boolean keepChildForeground = false;
        ShadowOverlayResult result = setupDefaultPresenterWithSingleElement(isUsingZorder,
                isUsingDefaultShadow, enableRoundedCorner, shadowEnabled, keepChildForeground);
        final int version = Build.VERSION.SDK_INT;
        if (version < 21) {
            // nothing supported
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else if (version < 23) {
            // supports static/dynamic shadow, supports rounded corner
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else {
            // supports foreground
            result.expect(false, 0, 0, false,
                    HALF_OVERLAY_COLOR, 0, false);
        }
    }

    @Test
    public void shadowOverlayContainerTest24() {
        final boolean isUsingZorder = true;
        final boolean isUsingDefaultShadow = false;
        final boolean enableRoundedCorner = true;
        final boolean shadowEnabled = true;
        final boolean keepChildForeground = true;
        ShadowOverlayResult result = setupDefaultPresenterWithSingleElement(isUsingZorder,
                isUsingDefaultShadow, enableRoundedCorner, shadowEnabled, keepChildForeground);
        final int version = Build.VERSION.SDK_INT;
        if (version < 21) {
            // nothing supported
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else if (version < 23) {
            // supports static/dynamic shadow, supports rounded corner
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else {
            // supports foreground
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        }
    }

    @Test
    public void shadowOverlayContainerTest25() {
        final boolean isUsingZorder = true;
        final boolean isUsingDefaultShadow = true;
        final boolean enableRoundedCorner = false;
        final boolean shadowEnabled = false;
        final boolean keepChildForeground = false;
        ShadowOverlayResult result = setupDefaultPresenterWithSingleElement(isUsingZorder,
                isUsingDefaultShadow, enableRoundedCorner, shadowEnabled, keepChildForeground);
        final int version = Build.VERSION.SDK_INT;
        if (version < 21) {
            // nothing supported
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else if (version < 23) {
            // supports static/dynamic shadow, supports rounded corner
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else {
            // supports foreground
            result.expect(false, 0, 0, false,
                    HALF_OVERLAY_COLOR, 0, false);
        }
    }

    @Test
    public void shadowOverlayContainerTest26() {
        final boolean isUsingZorder = true;
        final boolean isUsingDefaultShadow = true;
        final boolean enableRoundedCorner = false;
        final boolean shadowEnabled = false;
        final boolean keepChildForeground = true;
        ShadowOverlayResult result = setupDefaultPresenterWithSingleElement(isUsingZorder,
                isUsingDefaultShadow, enableRoundedCorner, shadowEnabled, keepChildForeground);
        final int version = Build.VERSION.SDK_INT;
        if (version < 21) {
            // nothing supported
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else if (version < 23) {
            // supports static/dynamic shadow, supports rounded corner
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else {
            // supports foreground
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        }
    }

    @Test
    public void shadowOverlayContainerTest27() {
        final boolean isUsingZorder = true;
        final boolean isUsingDefaultShadow = true;
        final boolean enableRoundedCorner = false;
        final boolean shadowEnabled = true;
        final boolean keepChildForeground = false;
        ShadowOverlayResult result = setupDefaultPresenterWithSingleElement(isUsingZorder,
                isUsingDefaultShadow, enableRoundedCorner, shadowEnabled, keepChildForeground);
        final int version = Build.VERSION.SDK_INT;
        if (version < 21) {
            // nothing supported
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else if (version < 23) {
            // supports static/dynamic shadow, supports rounded corner
            result.expect(true, HALF_OVERLAY_COLOR, sFocusedZ, false,
                    0, 0, false);
        } else {
            // supports foreground
            result.expect(false, 0, 0, false,
                    HALF_OVERLAY_COLOR, sFocusedZ, false);
        }
    }

    @Test
    public void shadowOverlayContainerTest28() {
        final boolean isUsingZorder = true;
        final boolean isUsingDefaultShadow = true;
        final boolean enableRoundedCorner = false;
        final boolean shadowEnabled = true;
        final boolean keepChildForeground = true;
        ShadowOverlayResult result = setupDefaultPresenterWithSingleElement(isUsingZorder,
                isUsingDefaultShadow, enableRoundedCorner, shadowEnabled, keepChildForeground);
        final int version = Build.VERSION.SDK_INT;
        if (version < 21) {
            // nothing supported
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else if (version < 23) {
            // supports static/dynamic shadow, supports rounded corner
            result.expect(true, HALF_OVERLAY_COLOR, sFocusedZ, false,
                    0, 0, false);
        } else {
            // supports foreground
            result.expect(true, HALF_OVERLAY_COLOR, sFocusedZ, false,
                    0, 0, false);
        }
    }

    @Test
    public void shadowOverlayContainerTest29() {
        final boolean isUsingZorder = true;
        final boolean isUsingDefaultShadow = true;
        final boolean enableRoundedCorner = true;
        final boolean shadowEnabled = false;
        final boolean keepChildForeground = false;
        ShadowOverlayResult result = setupDefaultPresenterWithSingleElement(isUsingZorder,
                isUsingDefaultShadow, enableRoundedCorner, shadowEnabled, keepChildForeground);
        final int version = Build.VERSION.SDK_INT;
        if (version < 21) {
            // nothing supported
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else if (version < 23) {
            // supports static/dynamic shadow, supports rounded corner
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else {
            // supports foreground
            result.expect(false, 0, 0, false,
                    HALF_OVERLAY_COLOR, 0, false);
        }
    }

    @Test
    public void shadowOverlayContainerTest30() {
        final boolean isUsingZorder = true;
        final boolean isUsingDefaultShadow = true;
        final boolean enableRoundedCorner = true;
        final boolean shadowEnabled = false;
        final boolean keepChildForeground = true;
        ShadowOverlayResult result = setupDefaultPresenterWithSingleElement(isUsingZorder,
                isUsingDefaultShadow, enableRoundedCorner, shadowEnabled, keepChildForeground);
        final int version = Build.VERSION.SDK_INT;
        if (version < 21) {
            // nothing supported
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else if (version < 23) {
            // supports static/dynamic shadow, supports rounded corner
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else {
            // supports foreground
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        }
    }

    @Test
    public void shadowOverlayContainerTest31() {
        final boolean isUsingZorder = true;
        final boolean isUsingDefaultShadow = true;
        final boolean enableRoundedCorner = true;
        final boolean shadowEnabled = true;
        final boolean keepChildForeground = false;
        ShadowOverlayResult result = setupDefaultPresenterWithSingleElement(isUsingZorder,
                isUsingDefaultShadow, enableRoundedCorner, shadowEnabled, keepChildForeground);
        final int version = Build.VERSION.SDK_INT;
        if (version < 21) {
            // nothing supported
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else if (version < 23) {
            // supports static/dynamic shadow, supports rounded corner
            result.expect(true, HALF_OVERLAY_COLOR, sFocusedZ, false,
                    0, 0, false);
        } else {
            // supports foreground
            result.expect(false, 0, 0, false,
                    HALF_OVERLAY_COLOR, sFocusedZ, false);
        }
    }

    @Test
    public void shadowOverlayContainerTest32() {
        final boolean isUsingZorder = true;
        final boolean isUsingDefaultShadow = true;
        final boolean enableRoundedCorner = true;
        final boolean shadowEnabled = true;
        final boolean keepChildForeground = true;
        ShadowOverlayResult result = setupDefaultPresenterWithSingleElement(isUsingZorder,
                isUsingDefaultShadow, enableRoundedCorner, shadowEnabled, keepChildForeground);
        final int version = Build.VERSION.SDK_INT;
        if (version < 21) {
            // nothing supported
            result.expect(true, HALF_OVERLAY_COLOR, 0, false,
                    0, 0, false);
        } else if (version < 23) {
            // supports static/dynamic shadow, supports rounded corner
            result.expect(true, HALF_OVERLAY_COLOR, sFocusedZ, false,
                    0, 0, false);
        } else {
            // supports foreground
            result.expect(true, HALF_OVERLAY_COLOR, sFocusedZ, false,
                    0, 0, false);
        }
    }

    @Test
    public void shadowOverlayContainerTestDefaultSettings() {
        ListRowPresenter presenter = new ListRowPresenter();
        final int version = Build.VERSION.SDK_INT;
        if (version < 21) {
            assertFalse(presenter.isUsingZOrder(mContext));
            assertFalse(presenter.isUsingDefaultShadow());
        } else {
            assertTrue(presenter.isUsingZOrder(mContext));
            assertTrue(presenter.isUsingDefaultShadow());
        }
        assertTrue(presenter.areChildRoundedCornersEnabled());
        assertTrue(presenter.getShadowEnabled());
        assertTrue(presenter.isKeepChildForeground());
    }
}
