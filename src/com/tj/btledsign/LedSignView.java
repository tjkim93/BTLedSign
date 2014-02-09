package com.tj.btledsign;


import java.util.Timer;
import java.util.TimerTask;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.EdgeEffectCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.OverScroller;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class LedSignView extends View {

	private static final String TAG = "TEST";
    /**
     * The scaling factor for a single zoom 'step'.
     *
     * @see #zoomIn()
     * @see #zoomOut()
     */
    private static final float ZOOM_AMOUNT = 0.25f;	
    private static final float AXIS_X_MIN = -1f;
    private static final float AXIS_X_MAX = 1f;
    private static final float AXIS_Y_MIN = -1f;
    private static final float AXIS_Y_MAX = 1f;
    private static final int   MATRIX_Y_OFF      = 30;
    private static final int   CTRL_COLOR_Y_OFF  = 30;
    private static final int   CTRL_COLOR_Y_SIZE = 60;

	//
	private int mIntZoomScale = 4;
	private int mIntDispPos   = 14;
	private int mPrevTouched  = -1;
	
	// Color & Scroll
	private int mIntSelColorIdx  = 0;
	private int mIntSelColor     = Color.BLACK;
	private boolean mVScrollLock = false;
	
	// animating
	private boolean mPlaying = false;
	private int     mPlayCnt = 0;

	// Rect area
    private RectF mCurrentViewport = new RectF(AXIS_X_MIN, AXIS_Y_MIN, AXIS_X_MAX, AXIS_Y_MAX);
    private Rect mContentRect      = new Rect();
    private Rect mViewRect         = new Rect();
    private Rect mTouchableRect    = new Rect();
    private Rect mColorCtrlRect    = new Rect();

    // State objects and values related to gesture tracking.
    private ScaleGestureDetector mScaleGestureDetector = null;
    private GestureDetectorCompat mGestureDetector = null;
    private OverScroller mScroller = null;
    private Zoomer mZoomer = null;
    private PointF mZoomFocalPoint = new PointF();
    private RectF mScrollerStartViewport = new RectF(); // Used only for zooms and flings.	
	
    // Edge effect / overscroll tracking objects.
    private EdgeEffectCompat mEdgeEffectTop;
    private EdgeEffectCompat mEdgeEffectBottom;
    private EdgeEffectCompat mEdgeEffectLeft;
    private EdgeEffectCompat mEdgeEffectRight;

    private boolean mEdgeEffectTopActive;
    private boolean mEdgeEffectBottomActive;
    private boolean mEdgeEffectLeftActive;
    private boolean mEdgeEffectRightActive;
    private Point mSurfaceSizeBuffer = new Point();
    private LedSignBitmap mLedSign = new LedSignBitmap(16 * 5, 16, 16 * 5, 16, 2);
    
	// CONSTRUCTOR
    public void setup(Context context) {
        // Sets up interactions
		if (mScaleGestureDetector == null)
			mScaleGestureDetector = new ScaleGestureDetector(context, mScaleGestureListener);
		
		if (mGestureDetector == null)
			mGestureDetector = new GestureDetectorCompat(context, mGestureListener);

		if (mScroller == null)
			mScroller = new OverScroller(context);
		
		if (mZoomer == null)
			mZoomer = new Zoomer(context);    	
		
        // Sets up edge effects
		if (mEdgeEffectLeft == null)
			mEdgeEffectLeft = new EdgeEffectCompat(context);
		
		if (mEdgeEffectTop == null)
			mEdgeEffectTop = new EdgeEffectCompat(context);
		
		if (mEdgeEffectRight == null)
			mEdgeEffectRight = new EdgeEffectCompat(context);
		
		if (mEdgeEffectBottom == null)
			mEdgeEffectBottom = new EdgeEffectCompat(context);
		
		updateViewInfo();
    }
   
	public LedSignView(Context context) {
		super(context);
		setFocusable(true);
		setup(context);
	}

	public LedSignView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setup(context);
	}
	
	public LedSignView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setup(context);
	}
	
	public void setApplication(BTLedSignApp app) {
		mLedSign = app.getLedSignBitmap();
		updateViewInfo();
	}
	
	public void updateViewInfo() {
		mContentRect.set(0, 0, mLedSign.getXResVirtual() * 20, mLedSign.getYResVirtual() * 40);
		float ratio = (float)mLedSign.getXRes() / (float)mLedSign.getXResVirtual();
		mCurrentViewport.set(-1.0f, -0.5f, ratio, 0.5f);		
	}
	
	public void setSelColor(int pos, int color) {
		mIntSelColorIdx = pos;
		if (pos < mLedSign.getColorCount())
			mIntSelColor = color;
	}
	
	public int getSelColor() {
		return mIntSelColor;
	}
	
	public int getSelColorIdx() {
		return mIntSelColorIdx;
	}
	
	public void lockVScroll(boolean lock) {
		mVScrollLock = lock;
	}
	
	public boolean getVScroll() {
		return mVScrollLock;
	}
	
	public void saveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putBoolean("vscr_lock", mVScrollLock);
	}

	public void restoreInstanceState(Bundle savedInstanceState) {
	  	mVScrollLock  = savedInstanceState.getBoolean("vscr_lock");
	}
	
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean retVal = mScaleGestureDetector.onTouchEvent(event);
        retVal = mGestureDetector.onTouchEvent(event) || retVal;
        return retVal || super.onTouchEvent(event);
    }	
	
    /**
     * The scale listener, used for handling multi-finger scale gestures.
     */
    private final ScaleGestureDetector.OnScaleGestureListener mScaleGestureListener
            = new ScaleGestureDetector.SimpleOnScaleGestureListener() {
        /**
         * This is the active focal point in terms of the viewport. Could be a local
         * variable but kept here to minimize per-frame allocations.
         */
    	
/*
        private PointF viewportFocus = new PointF();
        private float lastSpanX;
        private float lastSpanY;

        @Override
        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
            lastSpanX = ScaleGestureDetectorCompat.getCurrentSpanX(scaleGestureDetector);
            lastSpanY = ScaleGestureDetectorCompat.getCurrentSpanY(scaleGestureDetector);
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            float spanX = ScaleGestureDetectorCompat.getCurrentSpanX(scaleGestureDetector);
            float spanY = ScaleGestureDetectorCompat.getCurrentSpanY(scaleGestureDetector);

            float newWidth = lastSpanX / spanX * mCurrentViewport.width();
            float newHeight = lastSpanY / spanY * mCurrentViewport.height();

            float focusX = scaleGestureDetector.getFocusX();
            float focusY = scaleGestureDetector.getFocusY();
            hitTest(focusX, focusY, viewportFocus);

            mCurrentViewport.set(
                    viewportFocus.x
                            - newWidth * (focusX - mContentRect.left)
                            / mContentRect.width(),
                    viewportFocus.y
                            - newHeight * (mContentRect.bottom - focusY)
                            / mContentRect.height(),
                    0,
                    0);
            mCurrentViewport.right = mCurrentViewport.left + newWidth;
            mCurrentViewport.bottom = mCurrentViewport.top + newHeight;
            constrainViewport();
            ViewCompat.postInvalidateOnAnimation(DotMatrixView.this);

            lastSpanX = spanX;
            lastSpanY = spanY;
            return true;
        }
*/
    };	
	
    /**
     * The gesture listener, used for handling simple gestures such as double touches, scrolls,
     * and flings.
     */
    private final GestureDetector.SimpleOnGestureListener mGestureListener
            = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
        	int xpos = (int)e.getX();
        	int ypos = (int)e.getY();

        	if (!mTouchableRect.contains(xpos, ypos) &&
        		!mColorCtrlRect.contains(xpos, ypos)) {
        		return false;
        	}
        	
        	if (mColorCtrlRect.contains(xpos, ypos)) {
        		onColorCtrlTouched(xpos, ypos);
        	}
        	
            releaseEdgeEffects();
            mScrollerStartViewport.set(mCurrentViewport);
            mScroller.forceFinished(true);
            ViewCompat.postInvalidateOnAnimation(LedSignView.this);
            return true;
        }
        
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
        	mPrevTouched = -1;
			return false;
        }

/*
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            mZoomer.forceFinished(true);
            if (hitTest(e.getX(), e.getY(), mZoomFocalPoint)) {
                mZoomer.startZoom(ZOOM_AMOUNT);
            }
            ViewCompat.postInvalidateOnAnimation(DotMatrixView.this);
            Log.d(TAG,  "onDoubleTap");
            return true;
        }
*/
        
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        	if (mColorCtrlRect.contains((int)e2.getX(), (int)e2.getY())) {
        		onColorCtrlTouched((int)e2.getX(), (int)e2.getY());
        	}
        	
        	if (!mTouchableRect.contains((int)e1.getX(), (int)e1.getY())) {
        		return false;
        	}
        	
            // Scrolling uses math based on the viewport (as opposed to math using pixels).
            /**
             * Pixel offset is the offset in screen pixels, while viewport offset is the
             * offset within the current viewport. For additional information on surface sizes
             * and pixel offsets, see the docs for {@link computeScrollSurfaceSize()}. For
             * additional information about the viewport, see the comments for
             * {@link mCurrentViewport}.
             */
            float viewportOffsetX = distanceX * mCurrentViewport.width() / mContentRect.width();
            float viewportOffsetY = -distanceY * mCurrentViewport.height() / mContentRect.height();
            computeScrollSurfaceSize(mSurfaceSizeBuffer);
            int scrolledX = (int) (mSurfaceSizeBuffer.x
                    * (mCurrentViewport.left + viewportOffsetX - AXIS_X_MIN)
                    / (AXIS_X_MAX - AXIS_X_MIN));
            int scrolledY = (int) (mSurfaceSizeBuffer.y
                    * (AXIS_Y_MAX - mCurrentViewport.bottom - viewportOffsetY)
                    / (AXIS_Y_MAX - AXIS_Y_MIN));
            boolean canScrollX = mCurrentViewport.left > AXIS_X_MIN
                    || mCurrentViewport.right < AXIS_X_MAX;
            boolean canScrollY = mCurrentViewport.top > AXIS_Y_MIN
                    || mCurrentViewport.bottom < AXIS_Y_MAX;
            setViewportBottomLeft(
                    mCurrentViewport.left + viewportOffsetX,
                    mCurrentViewport.bottom + viewportOffsetY);

            if (canScrollX && scrolledX < 0) {
                mEdgeEffectLeft.onPull(scrolledX / (float) mContentRect.width());
                mEdgeEffectLeftActive = true;
            }
            if (canScrollY && scrolledY < 0) {
                mEdgeEffectTop.onPull(scrolledY / (float) mContentRect.height());
                mEdgeEffectTopActive = true;
            }
            if (canScrollX && scrolledX > mSurfaceSizeBuffer.x - mContentRect.width()) {
                mEdgeEffectRight.onPull((scrolledX - mSurfaceSizeBuffer.x + mContentRect.width())
                        / (float) mContentRect.width());
                mEdgeEffectRightActive = true;
            }
            if (canScrollY && scrolledY > mSurfaceSizeBuffer.y - mContentRect.height()) {
                mEdgeEffectBottom.onPull((scrolledY - mSurfaceSizeBuffer.y + mContentRect.height())
                        / (float) mContentRect.height());
                mEdgeEffectBottomActive = true;
            }
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            fling((int) -velocityX, (int) -velocityY);
            return true;
        }
    };    

    private void releaseEdgeEffects() {
        mEdgeEffectLeftActive
                = mEdgeEffectTopActive
                = mEdgeEffectRightActive
                = mEdgeEffectBottomActive
                = false;
        mEdgeEffectLeft.onRelease();
        mEdgeEffectTop.onRelease();
        mEdgeEffectRight.onRelease();
        mEdgeEffectBottom.onRelease();
    }

    private void fling(int velocityX, int velocityY) {
        releaseEdgeEffects();
        // Flings use math in pixels (as opposed to math based on the viewport).
        computeScrollSurfaceSize(mSurfaceSizeBuffer);
        mScrollerStartViewport.set(mCurrentViewport);
        int startX = (int) (mSurfaceSizeBuffer.x * (mScrollerStartViewport.left - AXIS_X_MIN) / (
                AXIS_X_MAX - AXIS_X_MIN));
        int startY = (int) (mSurfaceSizeBuffer.y * (AXIS_Y_MAX - mScrollerStartViewport.bottom) / (
                AXIS_Y_MAX - AXIS_Y_MIN));
        mScroller.forceFinished(true);
        mScroller.fling(
                startX,
                startY,
                velocityX,
                velocityY,
                0, mSurfaceSizeBuffer.x - mContentRect.width(),
                0, mSurfaceSizeBuffer.y - mContentRect.height(),
                mContentRect.width() / 2,
                mContentRect.height() / 2);
        ViewCompat.postInvalidateOnAnimation(this);
    }
    
    /**
     * Computes the current scrollable surface size, in pixels. For example, if the entire chart
     * area is visible, this is simply the current size of {@link #mContentRect}. If the chart
     * is zoomed in 200% in both directions, the returned size will be twice as large horizontally
     * and vertically.
     */
    private void computeScrollSurfaceSize(Point out) {
        out.set(
                (int) (mContentRect.width() * (AXIS_X_MAX - AXIS_X_MIN)
                        / mCurrentViewport.width()),
                (int) (mContentRect.height() * (AXIS_Y_MAX - AXIS_Y_MIN)
                        / mCurrentViewport.height()));
    }

    @Override
    public void computeScroll() {
        super.computeScroll();

        boolean needsInvalidate = false;

        if (mScroller.computeScrollOffset()) {
            // The scroller isn't finished, meaning a fling or programmatic pan operation is
            // currently active.

            computeScrollSurfaceSize(mSurfaceSizeBuffer);
            int currX = mScroller.getCurrX();
            int currY = mScroller.getCurrY();

            boolean canScrollX = (mCurrentViewport.left > AXIS_X_MIN
                    || mCurrentViewport.right < AXIS_X_MAX);
            boolean canScrollY = (mCurrentViewport.top > AXIS_Y_MIN
                    || mCurrentViewport.bottom < AXIS_Y_MAX);

            if (canScrollX
                    && currX < 0
                    && mEdgeEffectLeft.isFinished()
                    && !mEdgeEffectLeftActive) {
                mEdgeEffectLeft.onAbsorb((int) OverScrollerCompat.getCurrVelocity(mScroller));
                mEdgeEffectLeftActive = true;
                needsInvalidate = true;
            } else if (canScrollX
                    && currX > (mSurfaceSizeBuffer.x - mContentRect.width())
                    && mEdgeEffectRight.isFinished()
                    && !mEdgeEffectRightActive) {
                mEdgeEffectRight.onAbsorb((int) OverScrollerCompat.getCurrVelocity(mScroller));
                mEdgeEffectRightActive = true;
                needsInvalidate = true;
            }

            if (canScrollY
                    && currY < 0
                    && mEdgeEffectTop.isFinished()
                    && !mEdgeEffectTopActive) {
                mEdgeEffectTop.onAbsorb((int) OverScrollerCompat.getCurrVelocity(mScroller));
                mEdgeEffectTopActive = true;
                needsInvalidate = true;
            } else if (canScrollY
                    && currY > (mSurfaceSizeBuffer.y - mContentRect.height())
                    && mEdgeEffectBottom.isFinished()
                    && !mEdgeEffectBottomActive) {
                mEdgeEffectBottom.onAbsorb((int) OverScrollerCompat.getCurrVelocity(mScroller));
                mEdgeEffectBottomActive = true;
                needsInvalidate = true;
            }

            float currXRange = AXIS_X_MIN + (AXIS_X_MAX - AXIS_X_MIN)
                    * currX / mSurfaceSizeBuffer.x;
            float currYRange = AXIS_Y_MAX - (AXIS_Y_MAX - AXIS_Y_MIN)
                    * currY / mSurfaceSizeBuffer.y;
            setViewportBottomLeft(currXRange, currYRange);
        }

        if (mZoomer.computeZoom()) {
            // Performs the zoom since a zoom is in progress (either programmatically or via
            // double-touch).
            float newWidth = (1f - mZoomer.getCurrZoom()) * mScrollerStartViewport.width();
            float newHeight = (1f - mZoomer.getCurrZoom()) * mScrollerStartViewport.height();
            float pointWithinViewportX = (mZoomFocalPoint.x - mScrollerStartViewport.left)
                    / mScrollerStartViewport.width();
            float pointWithinViewportY = (mZoomFocalPoint.y - mScrollerStartViewport.top)
                    / mScrollerStartViewport.height();
            mCurrentViewport.set(
                    mZoomFocalPoint.x - newWidth * pointWithinViewportX,
                    mZoomFocalPoint.y - newHeight * pointWithinViewportY,
                    mZoomFocalPoint.x + newWidth * (1 - pointWithinViewportX),
                    mZoomFocalPoint.y + newHeight * (1 - pointWithinViewportY));
            constrainViewport();
            needsInvalidate = true;
        }

        if (needsInvalidate) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }
    
    /**
     * Sets the current viewport (defined by {@link #mCurrentViewport}) to the given
     * X and Y positions. Note that the Y value represents the topmost pixel position, and thus
     * the bottom of the {@link #mCurrentViewport} rectangle. For more details on why top and
     * bottom are flipped, see {@link #mCurrentViewport}.
     */
    private void setViewportBottomLeft(float x, float y) {
        /**
         * Constrains within the scroll range. The scroll range is simply the viewport extremes
         * (AXIS_X_MAX, etc.) minus the viewport size. For example, if the extrema were 0 and 10,
         * and the viewport size was 2, the scroll range would be 0 to 8.
         */

        float curWidth = mCurrentViewport.width();
        float curHeight = mCurrentViewport.height();
        x = Math.max(AXIS_X_MIN, Math.min(x, AXIS_X_MAX - curWidth));
        y = Math.max(AXIS_Y_MIN + curHeight, Math.min(y, AXIS_Y_MAX));

        mCurrentViewport.set(x, y - curHeight, x + curWidth, y);
        ViewCompat.postInvalidateOnAnimation(this);
    }    
    
    
    /**
     * Finds the chart point (i.e. within the chart's domain and range) represented by the
     * given pixel coordinates, if that pixel is within the chart region described by
     * {@link #mContentRect}. If the point is found, the "dest" argument is set to the point and
     * this function returns true. Otherwise, this function returns false and "dest" is unchanged.
     */
/*
    private boolean hitTest(float x, float y, PointF dest) {
        if (!mContentRect.contains((int) x, (int) y)) {
            return false;
        }

        dest.set(
                mCurrentViewport.left
                        + mCurrentViewport.width()
                        * (x - mContentRect.left) / mContentRect.width(),
                mCurrentViewport.top
                        + mCurrentViewport.height()
                        * (y - mContentRect.bottom) / -mContentRect.height());
        return true;
     }
*/
    
    /**
     * Ensures that current viewport is inside the viewport extremes defined by {@link #AXIS_X_MIN},
     * {@link #AXIS_X_MAX}, {@link #AXIS_Y_MIN} and {@link #AXIS_Y_MAX}.
     */
    private void constrainViewport() {
        mCurrentViewport.left = Math.max(AXIS_X_MIN, mCurrentViewport.left);
        mCurrentViewport.top = Math.max(AXIS_Y_MIN, mCurrentViewport.top);
        mCurrentViewport.bottom = Math.max(Math.nextUp(mCurrentViewport.top),
                Math.min(AXIS_Y_MAX, mCurrentViewport.bottom));
        mCurrentViewport.right = Math.max(Math.nextUp(mCurrentViewport.left),
                Math.min(AXIS_X_MAX, mCurrentViewport.right));
    }    
    
    private void onColorCtrlTouched(int x, int y) {
		int nTotWidth  = (int)((mLedSign.getXRes()) * mIntZoomScale + 2);
		int nXPos   = (mViewRect.width() - nTotWidth) / 2;
		
		int nX = (int)((mCurrentViewport.left + 1.0f) *  mLedSign.getXResVirtual());
		nX += ((x - nXPos) / mIntZoomScale);
		
		if (nX == mPrevTouched)
			return;
		
		mPrevTouched = nX;
		
		if (nX >= mLedSign.getBitmap().getWidth())
			return;
		
		int nUsedCol = Color.BLACK; //LedSignBitmap.COLOR_OFF_LED;
		for (int j = 0; j < mLedSign.getYRes(); j++) {
			int pixel = mLedSign.getBitmap().getPixel(nX, j);
			
			if (pixel != Color.BLACK) {
				nUsedCol = pixel;
			}
		}
		
		for (int j = 0; j < mLedSign.getYRes(); j++) {
			int pixel = mLedSign.getBitmap().getPixel(nX, j);
			if (mIntSelColorIdx == mLedSign.getInverseIndex()) {		//inverse
				if (pixel == Color.BLACK && nUsedCol != Color.BLACK) {
					mLedSign.getBitmap().setPixel(nX, j, nUsedCol);
				}
				else
					mLedSign.getBitmap().setPixel(nX, j, Color.BLACK);
			} 	else if (mIntSelColorIdx == mLedSign.getFillIndex()) {	//Fill
				if (pixel == Color.BLACK) {
					mLedSign.getBitmap().setPixel(nX, j, mIntSelColor);
				}
			} else if (mIntSelColorIdx == mLedSign.getFlashIndex()) {	// flash
				if (pixel != Color.BLACK) {
					int pix = mLedSign.getBitmap().getPixel(nX, j);
					mLedSign.getBitmap().setPixel(nX, j, Color.argb(128, Color.red(pix), Color.green(pix), Color.blue(pix)));
				}
			}
			else {
				if (pixel != Color.BLACK)
					mLedSign.getBitmap().setPixel(nX, j, mIntSelColor);
			}
		}
		invalidate();
    }
    
    private void drawBitmap(Canvas canvas) {

		// Matrix
		Paint paint = new Paint();
		int nYPos = MATRIX_Y_OFF;
		int nTotWidth  = (int)((mLedSign.getXRes()) * mIntZoomScale + 2);
		int nTotHeight = (int)((mLedSign.getYRes()) * mIntZoomScale + 2);
		int nXPos = (mViewRect.width() - nTotWidth) / 2;

		// draw dark circle
		canvas.drawColor(Color.WHITE);
		paint.setColor(Color.BLACK);
		canvas.drawRect(nXPos, nYPos, nXPos + nTotWidth, nYPos + nTotHeight, paint);
		paint.setColor(LedSignBitmap.COLOR_OFF_LED);
		int rad = mIntZoomScale / 2 - 1;
		for (int j = 0; j < mLedSign.getYRes(); j++) {
			int cy =  nYPos + (j * mIntZoomScale) + (mIntZoomScale / 2);
			for (int i = 0; i < mLedSign.getXRes(); i++) {
				int cx = nXPos + (i * mIntZoomScale) + (mIntZoomScale / 2);
				canvas.drawCircle(cx, cy, rad, paint);
			}
		}
		mTouchableRect.set(nXPos, nYPos, nXPos + nTotWidth, nYPos + nTotHeight);
		
		int nColY =  nYPos + (mLedSign.getYRes() * mIntZoomScale) + 
				(mIntZoomScale / 2) + CTRL_COLOR_Y_OFF;
		mColorCtrlRect.set(nXPos, nColY, nXPos + nTotWidth, nColY + CTRL_COLOR_Y_SIZE);

		// draw bitmap circle
		if (mLedSign.getBitmap() == null) {
			return;
		}

		int nX = (int)((mCurrentViewport.left + 1.0f) *  mLedSign.getXResVirtual());
		if (nX >= mLedSign.getXResVirtual() - mLedSign.getXRes())
			nX = mLedSign.getXResVirtual() - mLedSign.getXRes();
		
		int nY  = 14 + (int)((mCurrentViewport.top + 0.5f) *  mLedSign.getYResVirtual());
		if (mVScrollLock == false && nY != mIntDispPos) {
			mIntDispPos = nY;
			mLedSign.setFontYPos(nY);
		}

		if (mPlaying == true)
			mPlayCnt++;
		
		for (int i = 0; i < mLedSign.getXRes(); i++) {
			int cx = nXPos + (i * mIntZoomScale) + (mIntZoomScale / 2);
			int nUsedCol = Color.BLACK;
			
			for (int j = 0; j < mLedSign.getYRes(); j++) {
				int cy =  nYPos + (j * mIntZoomScale) + (mIntZoomScale / 2);
				int pixel = mLedSign.getBitmap().getPixel(nX + i, j);
				
				if (Color.alpha(pixel) == 128) {
					if (mPlaying == true) {
						if ((mPlayCnt % 10) < 5) {
							paint.setARGB(255, Color.red(pixel), Color.green(pixel), Color.blue(pixel));
						} else {
							paint.setColor(LedSignBitmap.COLOR_OFF_LED);
						}
						canvas.drawCircle(cx, cy, rad, paint);
						
					} else {
						RectF rect = new RectF(nXPos + (i * mIntZoomScale), nYPos + (j * mIntZoomScale), 
								nXPos + (i * mIntZoomScale) + mIntZoomScale, nYPos + (j * mIntZoomScale) + mIntZoomScale);
	
						paint.setARGB(255, Color.red(pixel), Color.green(pixel), Color.blue(pixel));
						canvas.drawArc(rect, 270, 180, true, paint);
						
						paint.setColor(LedSignBitmap.COLOR_OFF_LED);
						canvas.drawArc(rect, 90, 180, true, paint);
					}
					nUsedCol = pixel;
				} else if (pixel != Color.BLACK) {
					paint.setColor(pixel);
					canvas.drawCircle(cx, cy, rad, paint);
					nUsedCol = pixel;
				}
			}
			
			if (mPlaying == false) {
				//if (nUsedCol != Color.BLACK)
				//	paint.setColor(LedSignBitmap.COLOR_OFF_LED);
				//else
					paint.setColor(nUsedCol);
				cx -= (mIntZoomScale / 2);
				nColY =  nYPos + (mLedSign.getYRes() * mIntZoomScale) + 
						(mIntZoomScale / 2) + CTRL_COLOR_Y_OFF;
				canvas.drawRect(cx + 1, nColY, 
						cx + mIntZoomScale - 1, nColY + CTRL_COLOR_Y_SIZE, paint);
			}
		}
		paint.setColor(Color.BLACK);
		int xpos = (mViewRect.width()) / 2 - (mLedSign.getBitmap().getWidth() / 2);
		canvas.drawBitmap(mLedSign.getBitmap(), xpos, 5, paint);
    }
    
    private Timer mRefreshTimer;
    private RefreshTask mRefreshTask;
    private static Handler mTimerHandler;
    private int mIntPlaySpeed = 0;
    
    private final class HandlerExtension extends Handler {
		@Override
		public void handleMessage(Message msg) {
		    super.handleMessage(msg);
		    if (mPlaying == true) {
		    	mLedSign.shiftBitmap2Left();
		    	invalidate();
		    }
		}
	}

	class RefreshTask extends TimerTask {
        private int count = 0;
        public void run() {
        	int mod = 9 - mIntPlaySpeed;
            count++;
            if (count % mod == 0)
            	mTimerHandler.sendEmptyMessage(count);
        }
    }

    public void startPlay() {
    	mLedSign.saveBackup();
    	mTimerHandler = new HandlerExtension();
        mRefreshTimer = new Timer();
        mRefreshTask = new RefreshTask();
        
        mRefreshTimer.schedule(mRefreshTask, 0, 20);
        mPlaying = true;
    }

    public void stopPlay() {
        if (mRefreshTimer != null) {
            mRefreshTimer.cancel();
            mRefreshTimer.purge();
            mRefreshTimer = null;
        }
        mRefreshTask = null;
        mPlaying = false;
        mLedSign.restoreBackup();
        invalidate();
    }
    
    public void setPlaySpeed(int speed) {
    	mIntPlaySpeed = speed;
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mViewRect.set(
                getPaddingLeft(),
                getPaddingTop(),
                getWidth() - getPaddingRight(),
                getHeight() - getPaddingBottom());
        int wr = mViewRect.width() / mLedSign.getXRes();
        int hr = (mViewRect.height() - (CTRL_COLOR_Y_SIZE + CTRL_COLOR_Y_OFF + MATRIX_Y_OFF)) / mLedSign.getYRes();
        mIntZoomScale = Math.min(wr, hr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int minChartSize = 0;
        setMeasuredDimension(
                Math.max(getSuggestedMinimumWidth(),
                        resolveSize(minChartSize + getPaddingLeft() + getPaddingRight(),
                                widthMeasureSpec)),
                Math.max(getSuggestedMinimumHeight(),
                        resolveSize(minChartSize + getPaddingTop() + getPaddingBottom(),
                                heightMeasureSpec)));
    }    
    
	@Override
	protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Clips the next few drawing operations to the content area
        //int clipRestoreCount = canvas.save();
        canvas.clipRect(mViewRect);
        drawBitmap(canvas);
	}

		
}