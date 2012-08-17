package ru.clickwheel;

import android.graphics.Bitmap;
import android.graphics.Canvas;

public final class DrawLayer {
	private Bitmap bitmap;
	
	public DrawLayer() {
		
	}
	
	public void reset() {
		bitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
	}
	
	public void onSizeChange(int width, int height) {
		if (bitmap != null) {
			bitmap.recycle();
		}
		bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
	}
	
	public Canvas getCanvas() {
		return new Canvas(bitmap);
	}
	
	public void drawOn(Canvas canvas, float x, float y) {
		canvas.drawBitmap(bitmap, x, y, null);
	}
	
	public void release() {
		if (bitmap != null) {
			bitmap.recycle();
		}
	}
}
