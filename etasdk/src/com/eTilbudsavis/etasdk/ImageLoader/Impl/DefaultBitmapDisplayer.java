package com.eTilbudsavis.etasdk.ImageLoader.Impl;

import com.eTilbudsavis.etasdk.Eta;
import com.eTilbudsavis.etasdk.ImageLoader.BitmapDisplayer;
import com.eTilbudsavis.etasdk.ImageLoader.ImageRequest;

public class DefaultBitmapDisplayer implements BitmapDisplayer {
	
	public static final String TAG = Eta.TAG_PREFIX + DefaultBitmapDisplayer.class.getSimpleName();
	
	public void display(ImageRequest ir) {
		
		if(ir.getBitmap() != null) {
			ir.getImageView().setImageBitmap(ir.getBitmap());
		} else if (ir.getPlaceholderError() != 0) {
			ir.getImageView().setImageResource(ir.getPlaceholderError());
		}
		
	}

}
