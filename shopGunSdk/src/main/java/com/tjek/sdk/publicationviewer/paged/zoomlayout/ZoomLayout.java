package com.tjek.sdk.publicationviewer.paged.zoomlayout;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.OverScroller;

import androidx.core.view.ViewCompat;

import com.tjek.sdk.TjekLogCat;
import com.tjek.sdk.publicationviewer.paged.NumberUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("unused")
public class ZoomLayout extends FrameLayout {

    public static final String TAG = ZoomLayout.class.getSimpleName();

    private static final int DEF_ZOOM_DURATION = 250;
    public boolean DEBUG = false;

    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    private GestureListener gestureListener;
    private SimpleOnGlobalLayoutChangedListener simpleOnGlobalLayoutChangedListener;

    private final Matrix scaleMatrix = new Matrix();
    private final Matrix scaleMatrixInverse = new Matrix();
    private final Matrix translateMatrix = new Matrix();
    private final Matrix translateMatrixInverse = new Matrix();

    // helper array to save heap
    private final float[] matrixValues = new float[9];

    private float focusY;
    private float focusX;

    // Helper array to save heap
    float[] array = new float[6];

    // for set scale
    private boolean allowOverScale = true;

    RectF drawRect = new RectF();
    RectF viewPortRect = new RectF();

    private FlingRunnable flingRunnable;
    private AnimatedZoomRunnable animatedZoomRunnable;
    private Interpolator animationInterpolator = new DecelerateInterpolator();
    private int zoomDuration = DEF_ZOOM_DURATION;

    // allow parent views to intercept any touch events that we do not consume
    boolean allowParentInterceptOnEdge = true;
    // allow parent views to intercept any touch events that we do not consume even if we are in a scaled state
    boolean allowParentInterceptOnScaled = false;
    // minimum scale of the content
    private float minScale = 1.0f;
    // maximum scale of the content
    private float maxScale = 3.0f;

    private boolean allowZoom = true;

    // Listeners
    private final ZoomDispatcher zoomDispatcher = new ZoomDispatcher();
    private final PanDispatcher panDispatcher = new PanDispatcher();
    private List<ZoomLayoutInterface.OnZoomListener> onZoomListeners;
    private List<ZoomLayoutInterface.OnPanListener> onPanListeners;
    private List<ZoomLayoutInterface.OnZoomTouchListener> onTouchListeners;
    private List<ZoomLayoutInterface.OnTapListener> onTapListeners;
    private List<ZoomLayoutInterface.OnDoubleTapListener> onDoubleTapListeners;
    private List<ZoomLayoutInterface.OnLongTapListener> onLongTapListeners;

    public ZoomLayout(Context context) {
        super(context);
        init(context, null);
    }

    public ZoomLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ZoomLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(21)
    public ZoomLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        gestureListener = new GestureListener();
        scaleGestureDetector = new ScaleGestureDetector(context, gestureListener);
        scaleGestureDetector.setQuickScaleEnabled(false);
        gestureDetector = new GestureDetector(context, gestureListener);
        simpleOnGlobalLayoutChangedListener = new SimpleOnGlobalLayoutChangedListener();
        getViewTreeObserver().addOnGlobalLayoutListener(simpleOnGlobalLayoutChangedListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        ZoomLayout.removeGlobal(this, simpleOnGlobalLayoutChangedListener);
        super.onDetachedFromWindow();
    }

    public static void removeGlobal(View v, ViewTreeObserver.OnGlobalLayoutListener listener) {
        ViewTreeObserver obs = v.getViewTreeObserver();
        obs.removeOnGlobalLayoutListener(listener);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.save();
        canvas.translate(-getPosX(), -getPosY());
        float scale = getScale();
        canvas.scale(scale, scale, focusX, focusY);
        try {
            super.dispatchDraw(canvas);
        } catch (Exception e) {
            // Few issues here, ignore for now.
            // NullPointerException
            // StackOverflowError when drawing childViews
        }
        if (DEBUG) {
            ZoomUtils.debugDraw(canvas, getContext(), getPosX(), getPosY(), focusX, focusY, getMatrixValue(scaleMatrixInverse, Matrix.MSCALE_X));
        }
        canvas.restore();
    }

    /**
     * Although the docs say that you shouldn't override this, I decided to do
     * so because it offers me an easy way to change the invalidated area to my
     * likening.
     */
    @Override
    public ViewParent invalidateChildInParent(int[] location, Rect dirty) {
        scaledPointsToScreenPoints(dirty);
        float scale = getScale();
        location[0] *= scale;
        location[1] *= scale;
        return super.invalidateChildInParent(location, dirty);
    }

    private void scaledPointsToScreenPoints(Rect rect) {
        ZoomUtils.setArray(array, rect);
        scaledPointsToScreenPoints(array);
        ZoomUtils.setRect(rect, array);
    }

    private void scaledPointsToScreenPoints(RectF rect) {
        ZoomUtils.setArray(array, rect);
        scaledPointsToScreenPoints(array);
        ZoomUtils.setRect(rect, array);
    }

    private void scaledPointsToScreenPoints(float[] a) {
        scaleMatrix.mapPoints(a);
        translateMatrix.mapPoints(a);
    }

    void screenPointsToScaledPoints(float[] a){
        translateMatrixInverse.mapPoints(a);
        scaleMatrixInverse.mapPoints(a);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        array[0] = ev.getX();
        array[1] = ev.getY();
        screenPointsToScaledPoints(array);
        ev.setLocation(array[0], array[1]);
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return allowZoom;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        array[0] = ev.getX();
        array[1] = ev.getY();
        scaledPointsToScreenPoints(array);
        ev.setLocation(array[0], array[1]);

        if (!allowZoom) {
            return false;
        }

        final int action = ev.getAction() & MotionEvent.ACTION_MASK;
        dispatchOnTouch(action, ev);

        boolean consumed = scaleGestureDetector.onTouchEvent(ev);
        consumed = gestureDetector.onTouchEvent(ev) || consumed;
        if (action == MotionEvent.ACTION_UP) {
            // manually call up
            consumed = gestureListener.onUp(ev) || consumed;
        }
        return consumed;
    }

    class GestureListener implements ScaleGestureDetector.OnScaleGestureListener,
            GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

        private float mScaleOnActionDown;
        private boolean mScrolling = false;

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            dispatchOnTab(e);
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            if ((e.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                dispatchOnDoubleTap(e);
            }
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            if (!scaleGestureDetector.isInProgress()) {
                dispatchOnLongTap(e);
            }
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            boolean consumed = false;
            if (e2.getPointerCount() == 1 && !scaleGestureDetector.isInProgress()) {
                // only drag if we have one pointer and aren't already scaling
                if (!mScrolling) {
                    panDispatcher.onPanBegin();
                    mScrolling = true;
                }
                consumed = internalMoveBy(distanceX, distanceY, true);
                if (consumed) {
                    panDispatcher.onPan();
                }
                if (allowParentInterceptOnEdge && !consumed && (!isScaled() || allowParentInterceptOnScaled)) {
                    requestDisallowInterceptTouchEvent(false);
                }
            }
            return consumed;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float scale = getScale();
            float newScale = NumberUtils.clamp(minScale, scale, maxScale);
            if (NumberUtils.isEqual(newScale, scale)) {
                // only fling if no scale is needed - scale will happen on ACTION_UP
                flingRunnable = new FlingRunnable(getContext());
                flingRunnable.fling((int) velocityX, (int) velocityY);
                ViewCompat.postOnAnimation(ZoomLayout.this, flingRunnable);
                return true;
            }
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onDown(MotionEvent e) {
            mScaleOnActionDown = getScale();
            requestDisallowInterceptTouchEvent(true);
            cancelFling();
            cancelZoom();
            return false;
        }

        boolean onUp(MotionEvent e) {
            boolean consumed = false;
            if (mScrolling) {
                panDispatcher.onPanEnd();
                mScrolling = false;
                consumed = true;
            }
            if (animatedZoomRunnable == null || animatedZoomRunnable.mFinished) {
                animatedZoomRunnable = new AnimatedZoomRunnable();
                consumed = animatedZoomRunnable.runValidation() || consumed;
            }
            return consumed;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            zoomDispatcher.onZoomBegin(getScale());
            fixFocusPoint(detector.getFocusX(), detector.getFocusY());
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scale = getScale() * detector.getScaleFactor();
            float scaleFactor = detector.getScaleFactor();
            if (Float.isNaN(scaleFactor) || Float.isInfinite(scaleFactor))
                return false;

            internalScale(scale, focusX, focusY);
            zoomDispatcher.onZoom(scale);
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            animatedZoomRunnable = new AnimatedZoomRunnable();
            animatedZoomRunnable.runValidation();
            zoomDispatcher.onZoomEnd(getScale());
        }
    }

    /**
     * When setting a new focus point, the translations on scale-matrix will change,
     * to counter that we'll first read old translation values, then apply the new focus-point
     * (with the old scale), then read the new translation values. Lastly we'll ensureTranslation
     * out ensureTranslation-matrix by the delta given by the scale-matrix translations.
     * @param focusX focus-focusX in screen coordinate
     * @param focusY focus-focusY in screen coordinate
     */
    private void fixFocusPoint(float focusX, float focusY) {
        array[0] = focusX;
        array[1] = focusY;
        screenPointsToScaledPoints(array);
        // The first scale event translates the content, so we'll counter that ensureTranslation
        float x1 = getMatrixValue(scaleMatrix, Matrix.MTRANS_X);
        float y1 = getMatrixValue(scaleMatrix, Matrix.MTRANS_Y);
        internalScale(getScale(), array[0], array[1]);
        float dX = getMatrixValue(scaleMatrix, Matrix.MTRANS_X)-x1;
        float dY = getMatrixValue(scaleMatrix, Matrix.MTRANS_Y)-y1;
        internalMove(dX + getPosX(), dY + getPosY(), false);
    }

    private void cancelFling() {
        if (flingRunnable != null) {
            flingRunnable.cancelFling();
            flingRunnable = null;
        }
    }

    private void cancelZoom() {
        if (animatedZoomRunnable != null) {
            animatedZoomRunnable.cancel();
            animatedZoomRunnable = null;
        }
    }

    /**
     * The rectangle representing the location of the view inside the ZoomView. including scale and translations.
     */
    public RectF getDrawRect() {
        return new RectF(drawRect);
    }

    public boolean isAllowOverScale() {
        return allowOverScale;
    }

    public void setAllowOverScale(boolean allowOverScale) {
        this.allowOverScale = allowOverScale;
    }

    public boolean isAllowParentInterceptOnEdge() {
        return allowParentInterceptOnEdge;
    }

    public void setAllowParentInterceptOnEdge(boolean allowParentInterceptOnEdge) {
        this.allowParentInterceptOnEdge = allowParentInterceptOnEdge;
    }

    public boolean isAllowParentInterceptOnScaled() {
        return allowParentInterceptOnScaled;
    }

    public void setAllowParentInterceptOnScaled(boolean allowParentInterceptOnScaled) {
        this.allowParentInterceptOnScaled = allowParentInterceptOnScaled;
    }

    public int getZoomDuration() {
        return zoomDuration;
    }

    public void setZoomDuration(int zoomDuration) {
        this.zoomDuration = zoomDuration < 0 ? DEF_ZOOM_DURATION : zoomDuration;
    }

    public void setZoomInterpolator(Interpolator zoomAnimationInterpolator) {
        animationInterpolator = zoomAnimationInterpolator;
    }

    public float getMaxScale() {
        return maxScale;
    }

    public void setMaxScale(float maxScale) {
        this.maxScale = maxScale;
        if (this.maxScale < minScale) {
            setMinScale(maxScale);
        }
    }

    public float getMinScale() {
        return minScale;
    }

    public void setMinScale(float minScale) {
        this.minScale = minScale;
        if (this.minScale > maxScale) {
            setMaxScale(this.minScale);
        }
    }

    public boolean isAllowZoom() {
        return allowZoom;
    }

    public void setAllowZoom(boolean allowZoom) {
        this.allowZoom = allowZoom;
    }

    public float getScale() {
        return getMatrixValue(scaleMatrix, Matrix.MSCALE_X);
    }

    public void setScale(float scale) {
        setScale(scale, true);
    }

    public void setScale(float scale, boolean animate) {
        final View c = getChildAt(0);
        setScale(scale, getRight()/2f, getBottom()/2f, animate);
    }

    public boolean isTranslating() {
        return gestureListener.mScrolling;
    }

    public boolean isScaling() {
        return scaleGestureDetector.isInProgress();
    }

    public boolean isScaled() {
        return !NumberUtils.isEqual(getScale(), 1.0f, 0.05f);
    }

    public void setScale(float scale, float focusX, float focusY, boolean animate) {
        if (!allowZoom) {
            return;
        }
        fixFocusPoint(focusX, focusY);
        if (!allowOverScale) {
            scale = NumberUtils.clamp(minScale, scale, maxScale);
        }
        if (animate) {
            animatedZoomRunnable = new AnimatedZoomRunnable();
            animatedZoomRunnable.scale(getScale(), scale, this.focusX, this.focusY, true);
            ViewCompat.postOnAnimation(ZoomLayout.this, animatedZoomRunnable);
        } else {
            zoomDispatcher.onZoomBegin(getScale());
            internalScale(scale, this.focusX, this.focusY);
            zoomDispatcher.onZoom(scale);
            zoomDispatcher.onZoomEnd(scale);
        }
    }

    public boolean moveBy(float dX, float dY) {
        return moveTo(dX + getPosX(), dY + getPosY());
    }

    public boolean moveTo(float posX, float posY) {
        panDispatcher.onPanBegin();
        if (internalMove(posX, posY, true)) {
            panDispatcher.onPan();
        }
        panDispatcher.onPanEnd();
        return true;
    }

    private boolean internalMoveBy(float dx, float dy, boolean clamp) {
        float tdx = dx;
        float tdy = dy;
        if (clamp) {
            RectF bounds = getTranslateDeltaBounds();
            tdx = NumberUtils.clamp(bounds.left, dx, bounds.right);
            tdy = NumberUtils.clamp(bounds.top, dy, bounds.bottom);
        }
//        L.d(TAG, String.format(Locale.US, "clamp: x[ %.2f -> %.2f ], y[ %.2f -> %.2f ]", dx, tdx, dy, tdy));
        float posX = tdx + getPosX();
        float posY = tdy + getPosY();
        if (!NumberUtils.isEqual(posX, getPosX()) ||
                !NumberUtils.isEqual(posY, getPosY())) {
            translateMatrix.setTranslate(-posX, -posY);
            matrixUpdated();
            invalidate();
            return true;
        }
        return false;
    }

    private boolean internalMove(float posX, float posY, boolean clamp) {
//        L.d(TAG, String.format(Locale.US, "internalMove: x[ %.2f -> %.2f ], y[ %.2f -> %.2f ]", getPosX(), posX, getPosY(), posY));
        return internalMoveBy(posX - getPosX(), posY - getPosY(), clamp);
    }

    private RectF getTranslateDeltaBounds() {
        RectF r = new RectF();
        float maxDeltaX = drawRect.width() - viewPortRect.width();
        if (maxDeltaX < 0) {
            float leftEdge = Math.round((viewPortRect.width() - drawRect.width()) / 2);
            if (leftEdge > drawRect.left) {
                r.left = 0;
                r.right = leftEdge - drawRect.left;
            } else {
                r.left = leftEdge - drawRect.left;
                r.right = 0;
            }
        } else {
            r.left = drawRect.left - viewPortRect.left;
            r.right = r.left + maxDeltaX;
        }

        float maxDeltaY = drawRect.height() - viewPortRect.height();
        if (maxDeltaY < 0) {
            float topEdge = Math.round((viewPortRect.height() - drawRect.height()) / 2f);
            if (topEdge > drawRect.top) {
                r.top = drawRect.top - topEdge;
            } else {
                r.top = topEdge - drawRect.top;
            }
            r.bottom = 0;
        } else {
            r.top = drawRect.top - viewPortRect.top;
            r.bottom = r.top + maxDeltaY;
        }

        return r;
    }

    /**
     * Gets the closest valid translation point, to the current {@link #getPosX() x} and
     * {@link #getPosY() y} coordinates.
     * @return the closest point
     */
    private PointF getClosestValidTranslationPoint() {
        PointF p = new PointF(getPosX(), getPosY());
        if (drawRect.width() < viewPortRect.width()) {
            p.x += drawRect.centerX() - viewPortRect.centerX();
        } else if (drawRect.right < viewPortRect.right) {
            p.x += drawRect.right - viewPortRect.right;
        } else if (drawRect.left > viewPortRect.left) {
            p.x += drawRect.left - viewPortRect.left;
        }
        if (drawRect.height() < viewPortRect.height()) {
            p.y += drawRect.centerY() - viewPortRect.centerY();
        } else if (drawRect.bottom < viewPortRect.bottom) {
            p.y += drawRect.bottom - viewPortRect.bottom;
        } else if (drawRect.top > viewPortRect.top) {
            p.y += drawRect.top - viewPortRect.top;
        }
        return p;
    }

    private void internalScale(float scale, float focusX, float focusY) {
        this.focusX = focusX;
        this.focusY = focusY;
        scaleMatrix.setScale(scale, scale, this.focusX, this.focusY);
        matrixUpdated();
        requestLayout();
        invalidate();
    }

    /**
     * Update all variables that rely on the Matrices.
     */
    private void matrixUpdated() {
        // First inverse matrices
        scaleMatrix.invert(scaleMatrixInverse);
        translateMatrix.invert(translateMatrixInverse);
        // Update DrawRect - maybe this should be viewPort.left instead of 0?
        ZoomUtils.setRect(viewPortRect, 0, 0, getWidth(), getHeight());

        final View child = getChildAt(0);
        if (child != null) {
            ZoomUtils.setRect(drawRect, child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
            scaledPointsToScreenPoints(drawRect);
        } else {
            // If no child is added, then center the drawrect, and let it be empty
            float x = viewPortRect.centerX();
            float y = viewPortRect.centerY();
            drawRect.set(x, y, x, y);
        }
    }

    /**
     * Get the current x-translation
     */
    public float getPosX() {
        return -getMatrixValue(translateMatrix, Matrix.MTRANS_X);
    }

    /**
     * Get the current y-translation
     */
    public float getPosY() {
        return -getMatrixValue(translateMatrix, Matrix.MTRANS_Y);
    }

    /**
     * Read a specific value from a given matrix
     * @param matrix The Matrix to read a value from
     * @param value The value-position to read
     * @return The value at a given position
     */
    private float getMatrixValue(Matrix matrix, int value) {
        matrix.getValues(matrixValues);
        return matrixValues[value];
    }

    private class AnimatedZoomRunnable implements Runnable {

        boolean mCancelled = false;
        boolean mFinished = false;

        private final long mStartTime;
        private float mZoomStart, mZoomEnd, mFocalX, mFocalY;
        private float mStartX, mStartY, mTargetX, mTargetY;

        AnimatedZoomRunnable() {
            mStartTime = System.currentTimeMillis();
        }

        boolean doScale() {
            return !NumberUtils.isEqual(mZoomStart, mZoomEnd);
        }

        boolean doTranslate() {
            return !NumberUtils.isEqual(mStartX, mTargetX) || !NumberUtils.isEqual(mStartY, mTargetY);
        }

        boolean runValidation() {
            float scale = getScale();
            float newScale = NumberUtils.clamp(minScale, scale, maxScale);
            scale(scale, newScale, focusX, focusY, true);
            if (animatedZoomRunnable.doScale() || animatedZoomRunnable.doTranslate()) {
                ViewCompat.postOnAnimation(ZoomLayout.this, animatedZoomRunnable);
                return true;
            }
            return false;
        }

        void scale(float currentZoom, float targetZoom, float focalX, float focalY, @SuppressWarnings("SameParameterValue") boolean ensureTranslations) {
            mFocalX = focalX;
            mFocalY = focalY;
            mZoomStart = currentZoom;
            mZoomEnd = targetZoom;
            if (doScale()) {
                zoomDispatcher.onZoomBegin(getScale());
            }
            if (ensureTranslations) {
                mStartX = getPosX();
                mStartY = getPosY();
                boolean scale = doScale();
                if (scale) {
                    scaleMatrix.setScale(mZoomEnd, mZoomEnd, mFocalX, mFocalY);
                    matrixUpdated();
                }
                PointF p = getClosestValidTranslationPoint();
                mTargetX = p.x;
                mTargetY = p.y;
                if (scale) {
                    scaleMatrix.setScale(mZoomStart, mZoomStart, ZoomLayout.this.focusX, ZoomLayout.this.focusY);
                    matrixUpdated();
                }
                if (doTranslate()) {
                    panDispatcher.onPanBegin();
                }
            }
        }

        void cancel() {
            mCancelled = true;
            finish();
        }

        private void finish() {
            if (!mFinished) {
                if (doScale()) {
                    zoomDispatcher.onZoomEnd(getScale());
                }
                if (doTranslate()) {
                    panDispatcher.onPanEnd();
                }
            }
            mFinished = true;
        }

        @Override
        public void run() {

            if (mCancelled || (!doScale() && !doTranslate())) {
                return;
            }

            float t = interpolate();
            if (doScale()) {
                float newScale = mZoomStart + t * (mZoomEnd - mZoomStart);
//                log(String.format(Locale.US, "AnimatedZoomRunnable.run.scale %.2f", newScale));
                internalScale(newScale, mFocalX, mFocalY);
                zoomDispatcher.onZoom(newScale);
            }
            if (doTranslate()) {
                float x = mStartX + t * (mTargetX - mStartX);
                float y = mStartY + t * (mTargetY - mStartY);
//                log(String.format(Locale.US, "AnimatedZoomRunnable.run.translate x:%.0f, y:%.0f", x, y));
                internalMove(x, y, false);
                panDispatcher.onPan();
            }

            // We haven't hit our target scale yet, so post ourselves again
            if (t < 1f) {
                ViewCompat.postOnAnimation(ZoomLayout.this, this);
            } else {
                finish();
            }
        }

        private float interpolate() {
            float t = 1f * (System.currentTimeMillis() - mStartTime) / zoomDuration;
            t = Math.min(1f, t);
            return animationInterpolator.getInterpolation(t);
        }

    }

    private class FlingRunnable implements Runnable {

        private final OverScroller mScroller;
        private int mCurrentX, mCurrentY;
        private boolean mFinished = false;

        FlingRunnable(Context context) {
            mScroller = new OverScroller(context);
        }

        void fling(int velocityX, int velocityY) {

            final int startX = Math.round(viewPortRect.left);
            final int minX, maxX;
            if (viewPortRect.width() < drawRect.width()) {
                minX = Math.round(drawRect.left);
                maxX = Math.round(drawRect.width() - viewPortRect.width());
            } else {
                minX = maxX = startX;
            }

            final int startY = Math.round(viewPortRect.top);
            final int minY, maxY;
            if (viewPortRect.height() < drawRect.height()) {
                minY = Math.round(drawRect.top);
                maxY = Math.round(drawRect.bottom - viewPortRect.bottom);
            } else {
                minY = maxY = startY;
            }

            mCurrentX = startX;
            mCurrentY = startY;

//            log(String.format("fling. x[ %s - %s ], y[ %s - %s ]", minX, maxX, minY, maxY));
            // If we actually can move, fling the scroller
            if (startX != maxX || startY != maxY) {
                mScroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY, 0, 0);
                panDispatcher.onPanBegin();
            } else {
                mFinished = true;
            }

        }

        void cancelFling() {
            mScroller.forceFinished(true);
            finish();
        }

        private void finish() {
            if (!mFinished) {
                panDispatcher.onPanEnd();
            }
            mFinished = true;
        }

        public boolean isFinished() {
            return mScroller.isFinished();
        }

        @Override
        public void run() {
            if (!mScroller.isFinished() && mScroller.computeScrollOffset()) {

                final int newX = mScroller.getCurrX();
                final int newY = mScroller.getCurrY();

//                log(String.format("mCurrentX:%s, newX:%s, mCurrentY:%s, newY:%s", mCurrentX, newX, mCurrentY, newY));
                if (internalMoveBy(mCurrentX - newX, mCurrentY - newY, true)) {
                    panDispatcher.onPan();
                }

                mCurrentX = newX;
                mCurrentY = newY;

                // Post On animation
                ViewCompat.postOnAnimation(ZoomLayout.this, FlingRunnable.this);
            } else {
                finish();
            }
        }
    }

    public void addOnTouchListener(ZoomLayoutInterface.OnZoomTouchListener l) {
        if (onTouchListeners == null) {
            onTouchListeners = new ArrayList<>();
        }
        onTouchListeners.add(l);
    }

    public void removeOnTouchListeners(ZoomLayoutInterface.OnZoomTouchListener listener) {
        if (onTouchListeners != null) {
            onTouchListeners.remove(listener);
        }
    }

    public void clearOnTouchListener() {
        if (onTouchListeners != null) {
            onTouchListeners.clear();
        }
    }

    private void dispatchOnTouch(int action, MotionEvent ev) {
        if (onTouchListeners != null) {
            for (int i = 0, z = onTouchListeners.size(); i < z; i++) {
                ZoomLayoutInterface.OnZoomTouchListener listener = onTouchListeners.get(i);
                if (listener != null) {
                    listener.onTouch(ZoomLayout.this, action, new TapInfo(ZoomLayout.this, ev));
                }
            }
        }
    }

    public void addOnTapListener(ZoomLayoutInterface.OnTapListener l) {
        if (onTapListeners == null) {
            onTapListeners = new ArrayList<>();
        }
        onTapListeners.add(l);
    }

    public void removeOnTouchListener(ZoomLayoutInterface.OnTapListener listener) {
        if (onTapListeners != null) {
            onTapListeners.remove(listener);
        }
    }

    public void clearOnTabListeners() {
        if (onTapListeners != null) {
            onTapListeners.clear();
        }
    }

    private void dispatchOnTab(MotionEvent ev) {
        if (onTapListeners != null) {
            for (int i = 0, z = onTapListeners.size(); i < z; i++) {
                ZoomLayoutInterface.OnTapListener listener = onTapListeners.get(i);
                if (listener != null) {
                    listener.onTap(ZoomLayout.this, new TapInfo(ZoomLayout.this, ev));
                }
            }
        }
    }

    public void addOnDoubleTapListener(ZoomLayoutInterface.OnDoubleTapListener l) {
        if (onDoubleTapListeners == null) {
            onDoubleTapListeners = new ArrayList<>();
        }
        onDoubleTapListeners.add(l);
    }

    public void removeOnDoubleTapListener(ZoomLayoutInterface.OnDoubleTapListener listener) {
        if (onDoubleTapListeners != null) {
            onDoubleTapListeners.remove(listener);
        }
    }

    public void clearOnDoubleTapListeners() {
        if (onDoubleTapListeners != null) {
            onDoubleTapListeners.clear();
        }
    }

    private void dispatchOnDoubleTap(MotionEvent ev) {
        if (onDoubleTapListeners != null) {
            for (int i = 0, z = onDoubleTapListeners.size(); i < z; i++) {
                ZoomLayoutInterface.OnDoubleTapListener listener = onDoubleTapListeners.get(i);
                if (listener != null) {
                    listener.onDoubleTap(ZoomLayout.this, new TapInfo(ZoomLayout.this, ev));
                }
            }
        }
    }

    public void addOnLongTapListener(ZoomLayoutInterface.OnLongTapListener l) {
        if (onLongTapListeners == null) {
            onLongTapListeners = new ArrayList<>();
        }
        onLongTapListeners.add(l);
    }

    public void removeOnLongTapListener(ZoomLayoutInterface.OnLongTapListener listener) {
        if (onLongTapListeners != null) {
            onLongTapListeners.remove(listener);
        }
    }

    public void clearOnLongTapListeners() {
        if (onLongTapListeners != null) {
            onLongTapListeners.clear();
        }
    }

    private void dispatchOnLongTap(MotionEvent ev) {
        if (onLongTapListeners != null) {
            for (int i = 0, z = onLongTapListeners.size(); i < z; i++) {
                ZoomLayoutInterface.OnLongTapListener listener = onLongTapListeners.get(i);
                if (listener != null) {
                    listener.onLongTap(ZoomLayout.this, new TapInfo(ZoomLayout.this, ev));
                }
            }
        }
    }

    public void addOnZoomListener(ZoomLayoutInterface.OnZoomListener l) {
        if (onZoomListeners == null) {
            onZoomListeners = new ArrayList<>();
        }
        onZoomListeners.add(l);
    }

    public void removeOnZoomListener(ZoomLayoutInterface.OnZoomListener listener) {
        if (onZoomListeners != null) {
            onZoomListeners.remove(listener);
        }
    }

    public void clearOnZoomListeners() {
        if (onZoomListeners != null) {
            onZoomListeners.clear();
        }
    }

    public void addOnPanListener(ZoomLayoutInterface.OnPanListener l) {
        if (onPanListeners == null) {
            onPanListeners = new ArrayList<>();
        }
        onPanListeners.add(l);
    }

    public void removeOnPanListener(ZoomLayoutInterface.OnPanListener listener) {
        if (onPanListeners != null) {
            onPanListeners.remove(listener);
        }
    }

    public void clearOnPanListeners() {
        if (onPanListeners != null) {
            onPanListeners.clear();
        }
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        throw new IllegalStateException("Cannot set OnClickListener, please use OnTapListener.");
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        throw new IllegalStateException("Cannot set OnLongClickListener, please use OnLongTabListener.");
    }

    @Override
    public void setOnTouchListener(View.OnTouchListener l) {
        throw new IllegalStateException("Cannot set OnTouchListener.");
    }

    private class ZoomDispatcher {

        int mCount = 0;

        void onZoomBegin(float scale) {
            if (mCount++ == 0) {
                if (onZoomListeners != null) {
                    for (int i = 0, z = onZoomListeners.size(); i < z; i++) {
                        ZoomLayoutInterface.OnZoomListener listener = onZoomListeners.get(i);
                        if (listener != null) {
                            listener.onZoomBegin(ZoomLayout.this, scale);
                        }
                    }
                }
            }
        }

        void onZoom(float scale) {
            if (onZoomListeners != null) {
                for (int i = 0, z = onZoomListeners.size(); i < z; i++) {
                    ZoomLayoutInterface.OnZoomListener listener = onZoomListeners.get(i);
                    if (listener != null) {
                        listener.onZoom(ZoomLayout.this, scale);
                    }
                }
            }
        }

        void onZoomEnd(float scale) {
            if (--mCount == 0) {
                if (onZoomListeners != null) {
                    for (int i = 0, z = onZoomListeners.size(); i < z; i++) {
                        ZoomLayoutInterface.OnZoomListener listener = onZoomListeners.get(i);
                        if (listener != null) {
                            listener.onZoomEnd(ZoomLayout.this, scale);
                        }
                    }
                }
            }
        }
    }

    private class PanDispatcher {

        int mCount = 0;

        void onPanBegin() {
            if (mCount++ == 0) {
                if (onPanListeners != null) {
                    for (int i = 0, z = onPanListeners.size(); i < z; i++) {
                        ZoomLayoutInterface.OnPanListener listener = onPanListeners.get(i);
                        if (listener != null) {
                            listener.onPanBegin(ZoomLayout.this);
                        }
                    }
                }
            }
        }

        void onPan() {
            if (onPanListeners != null) {
                for (int i = 0, z = onPanListeners.size(); i < z; i++) {
                    ZoomLayoutInterface.OnPanListener listener = onPanListeners.get(i);
                    if (listener != null) {
                        listener.onPan(ZoomLayout.this);
                    }
                }
            }
        }

        void onPanEnd() {
            if (--mCount == 0) {
                if (onPanListeners != null) {
                    for (int i = 0, z = onPanListeners.size(); i < z; i++) {
                        ZoomLayoutInterface.OnPanListener listener = onPanListeners.get(i);
                        if (listener != null) {
                            listener.onPanEnd(ZoomLayout.this);
                        }
                    }
                }
            }
        }
    }

    private class SimpleOnGlobalLayoutChangedListener implements ViewTreeObserver.OnGlobalLayoutListener {

        private int mLeft, mTop, mRight, mBottom;

        @Override
        public void onGlobalLayout() {
            int oldL = mLeft;
            int oldT = mTop;
            int oldR = mRight;
            int oldB = mBottom;
            mLeft = getLeft();
            mTop = getTop();
            mRight = getRight();
            mBottom = getBottom();
            boolean changed = oldL != mLeft || oldT != mTop || oldR != mRight || oldB != mBottom;
            if (changed) {
                matrixUpdated();
                PointF p = getClosestValidTranslationPoint();
                internalMove(p.x, p.y, false);
            }
        }

    }

    private void log(String msg) {
        if (DEBUG) {
            TjekLogCat.INSTANCE.d(String.format(Locale.ENGLISH, "%s: %s", TAG, msg));
        }
    }

}
