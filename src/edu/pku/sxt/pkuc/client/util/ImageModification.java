package edu.pku.sxt.pkuc.client.util;

import java.io.ByteArrayOutputStream;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;

/**
 * Image Modification Utility
 * @author songxintong
 *
 */
public class ImageModification {
	
	// These request codes are used when start a new Activity
	// doing some image modification task, to determine which
	// action has been taken on return.
	// Make sure they are UNIQUE in the application scope.
	public static final int REQUEST_CODE_CAPTURE = 100;
	public static final int REQUEST_CODE_IMPORT = 101;
	public static final int REQUEST_CODE_CROP = 102;
	
	/**
	 * Crop image using android crop service.
	 * @param inputUri
	 * @param outputUri
	 * @param width Width of output image.
	 * @param height Height of output image.
	 * @param context Context Activity.
	 */
	public static void crop(Uri inputUri, Uri outputUri,
			int width, int height, Activity context) {
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setDataAndType(inputUri, "image/*");
		intent.putExtra("crop", "true");
		intent.putExtra("aspectX", width);
		intent.putExtra("aspectY", height);
		intent.putExtra("outputX", width);
		intent.putExtra("outputY", height);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);
		context.startActivityForResult(intent, REQUEST_CODE_CROP);	
	}
	
	/**
	 * Compress image.
	 * @param filePath Path of image file to be compressed.
	 * @param reqWidth Width of compressed image.
	 * @param reqHeight Height of compressed image.
	 * @param quality Quality of compressed image.
	 * @return Compressed image as byte[].
	 */
	public static byte[] compress(String filePath,
			int reqWidth, int reqHeight, int quality) {
		
		// get image size from file
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filePath, options);
		
		// calculate in-sample-size
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
		
		// get image from file
		options.inJustDecodeBounds = false;
		Bitmap bm = BitmapFactory.decodeFile(filePath, options);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		// compress image and return as byte[]
		bm.compress(Bitmap.CompressFormat.JPEG, quality, baos);
		return baos.toByteArray();
		
	}
	
	/**
	 * Calculate in-sample-size of the given image under
	 * given request width and height. 
	 * Used in the method:
	 * 		compress(String, int, int, int)
	 * @param options Options of input image.
	 * @param reqWidth Request output width.
	 * @param reqHeight Request output height.
	 * @return In-sample-size.
	 */
	private static int calculateInSampleSize(BitmapFactory.Options options, 
			int reqWidth, int reqHeight) {
		
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;
		
		if (height > reqHeight || width > reqWidth) {
			final int heightRatio = Math.round((float)height / (float)reqHeight);
			final int widthRatio = Math.round((float)width / (float)reqWidth);
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}
		
		return inSampleSize;
	}
}
