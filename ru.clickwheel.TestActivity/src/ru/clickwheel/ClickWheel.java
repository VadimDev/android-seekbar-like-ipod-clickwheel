package ru.clickwheel;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Shader.TileMode;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.OnGestureListener;

public class ClickWheel extends View implements WheelModel.Listener, OnGestureListener {

	private static final String TAG = ClickWheel.class.getSimpleName();
	
	private static boolean toolsInitialized = false;
	private static Rect bounds;
	private static Bitmap texture;
	private static Paint texturePaint;

	private static Paint createDefaultPaint() {
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setFilterBitmap(true);
		return paint;
	}
	
	private static void initDrawingToolsIfNecessary(Context context) {
		if (! toolsInitialized) {
			bounds = new Rect();
			
			// there's a subtle thing here. technically, different instances
			// of DialView might use different contexts. however, what we are
			// creating here is a Bitmap which is not bound to any context. 
			texture = BitmapFactory.decodeResource(context.getResources(),
												   R.drawable.dial_texture);
			texturePaint = createDefaultPaint();
			BitmapShader textureShader = new BitmapShader(texture, 
														  TileMode.MIRROR, 
														  TileMode.MIRROR);
			Matrix textureMatrix = new Matrix();
			textureMatrix.setScale(1.0f / texture.getWidth(), 1.0f / texture.getHeight());
			textureShader.setLocalMatrix(textureMatrix);
			texturePaint.setShader(textureShader);	
			
			toolsInitialized = true;
		}
	}
	
	private GestureDetector gestureDetector;
	private float dragStartDeg = Float.NaN;
	private float luftRotation = 0.0f;
	
	private WheelModel model;
	private Handler handler;
	
	private DrawLayer outerLayer;
	
	public ClickWheel(Context context) {
		super(context);
		init();
	}

	public ClickWheel(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public ClickWheel(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		initDrawingToolsIfNecessary(getContext());
		
		gestureDetector = new GestureDetector(getContext(), this);
		
		setModel(new WheelModel());
		handler = new Handler();
		
		outerLayer = new DrawLayer();
	}

	public final void setModel(WheelModel model) {
		if (this.model != null) {
			this.model.removeListener(this);
		}
		this.model = model;
		this.model.addListener(this);
		
		invalidate();
	}
	
	public final WheelModel getModel() {
		return model;
	}
	
	@Override
	protected final void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		SquareViewMeasurer measurer = new SquareViewMeasurer(100);
		measurer.onMeasure(widthMeasureSpec, heightMeasureSpec);
		setMeasuredDimension(measurer.getChosenDimension(), measurer.getChosenDimension());
	}

	private float getBaseRadius() {
		return 0.45f; // to avoid some aliasing issues ... 0.48
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		canvas.getClipBounds(bounds);

		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		{
			canvas.translate(bounds.left, bounds.top);

			float rotation = model.getRotationInDegrees() + luftRotation;
			float midX = bounds.width() / 2.0f;
			float midY = bounds.height() / 2.0f;

			canvas.rotate(rotation, midX, midY);
			outerLayer.drawOn(canvas, 0, 0);
			canvas.rotate(- rotation, midX, midY);
		}		
		canvas.restore();
	}

	private void drawOuterCircle(Canvas canvas, float baseRadius) {
		canvas.drawCircle(0.5f, 0.5f, baseRadius, texturePaint);
	}

	@Override
	public void onDialPositionChanged(WheelModel sender, int nicksChanged) {
	//	luftRotation = (float) (Math.random() * 1.0f - 0.5f);				
		invalidate();
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		Bundle bundle = (Bundle) state;
		super.onRestoreInstanceState(bundle.getParcelable("superState"));
		 
		setModel(WheelModel.restore(bundle));
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		Bundle bundle = new Bundle();
		bundle.putParcelable("superState", superState);

		model.save(bundle);
		
		return bundle;
	}
	
	private float xyToDegrees(float x, float y) {
		float distanceFromCenter = PointF.length((x - 0.5f), (y - 0.5f));
		if (distanceFromCenter < 0.1f
				|| distanceFromCenter > 0.5f) { // ignore center and out of bounds events
			return Float.NaN;
		} else {
			return (float) Math.toDegrees(Math.atan2(x - 0.5f, y - 0.5f));
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (gestureDetector.onTouchEvent(event)) {
			return true;
		} else {
			return super.onTouchEvent(event);
		}
	}
	
	@Override
	public boolean onDown(MotionEvent event) {
		float x = event.getX() / ((float) getWidth());
		float y = event.getY() / ((float) getHeight());
		
		dragStartDeg = xyToDegrees(x, y);
		Log.d(TAG, "deg = " + dragStartDeg);
		if (! Float.isNaN(dragStartDeg)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean onFling(MotionEvent eventA, MotionEvent eventB, float vx, float vy) {
		return false;
	}

	@Override
	public void onLongPress(MotionEvent event) {

	}

	@Override
	public boolean onScroll(MotionEvent eventA, MotionEvent eventB, float dx, float dy) {
		if (! Float.isNaN(dragStartDeg)) {
			float currentDeg = xyToDegrees(eventB.getX() / getWidth(), 
										   eventB.getY() / getHeight());
			
			if (! Float.isNaN(currentDeg)) {
				float degPerNick = 360.0f / model.getTotalNicks();
				float deltaDeg = dragStartDeg - currentDeg;
				
				final int nicks = (int) (Math.signum(deltaDeg) 
						* Math.floor(Math.abs(deltaDeg) / degPerNick));
				
				if (nicks != 0) {
					dragStartDeg = currentDeg;

					handler.post(new Runnable() {
						@Override
						public void run() {
							model.rotate(nicks);								
						}
					});
				} 
			} 
			
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void onShowPress(MotionEvent event) {
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent event) {
		return false;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		
		outerLayer.onSizeChange(w, h);
		
		regenerateLayers(w);
	}
	
	private void regenerateLayers(int size) {
		float baseRadius = getBaseRadius();
		
		float scale = (float) size;
		
		Canvas canvas = outerLayer.getCanvas();
		canvas.scale(scale, scale);
		drawOuterCircle(canvas, baseRadius);

	}
}
