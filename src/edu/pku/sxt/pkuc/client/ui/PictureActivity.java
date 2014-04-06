package edu.pku.sxt.pkuc.client.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.ImageView;
import edu.pku.sxt.pkuc.client.R;
import edu.pku.sxt.pkuc.client.util.HttpManager;

/**
 * Display a full screen picture.
 * @author songxintong
 *
 */
public class PictureActivity extends ActionBarActivity {
	
	static final String LOG_TAG = "PictureActivity";
	
	ImageView iv;	// UI ImageView
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_picture);
		
		getSupportActionBar().hide();
		
		Bundle data = getIntent().getExtras();
		int pos = data.getInt("pos");
		final int rid =  (Integer)(DetailActivity.pictures.get(pos).get("rid"));
		
		Bitmap thumb = DetailActivity.thumbs.get(rid);
		iv = (ImageView)findViewById(R.id.image);
		iv.setImageBitmap(thumb);
		
		getPicture(rid);
	}
	
	/**
	 * Make a get-picture request.
	 * @param rid report id of picture
	 */
	private void getPicture(final int rid) {
		// set http post parameters
		final Map<String, String> reqParas = new HashMap<String, String>();
		reqParas.put("t", "gpc");
		reqParas.put("id", String.valueOf(rid));
		final String url = getString(R.string.server_url); 
				
    	// do post in new thread
	    Runnable runnable = new Runnable() {
			@Override
			public void run() {
				int status;
				byte[] picture = new byte[1];
				try {
					picture = HttpManager.postForFile(url, reqParas);
					status = 0;
				} catch (Exception e) {
					status = -2; // network error
					Log.e(LOG_TAG, e.getMessage());
				}				
				Message msg = new Message();
				Bundle data = new Bundle();
				data.putInt("status", status);
				data.putByteArray("picture", picture);
				data.putInt("rid", rid);
				msg.setData(data);
				getPictureHandler.sendMessage(msg);
			}
	    };
	    new Thread(runnable).start();
	}
	
	/**
	 * Handle response of get-picture request.
	 */
	Handler getPictureHandler = new Handler( ){
		@Override
    	public void handleMessage(Message msg) {
    		super.handleMessage(msg);
    		Bundle data = msg.getData();
    		int status = data.getInt("status");
    		/*
			 * 0 - success
			 * -1 - server error
			 * -2 - network error
			 */
			switch (status) {
			  case 0: 
				byte[] picture = data.getByteArray("picture");
				int rid = data.getInt("rid");
				int orientation = 0;
				try{
					File f = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), rid+"_pic.jpg");
					FileOutputStream fos = new FileOutputStream(f);
					fos.write(picture);
					fos.close();
					ExifInterface exif = new ExifInterface(f.getPath());
					orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
				} catch (Exception e){
					Log.e(LOG_TAG, e.getMessage());
				}
				float rotate = 0;
				switch (orientation) {
				  case 6: rotate = 90; break;
				  case 3: rotate = 180; break;
				  case 8: rotate = 270; break;
				}
				Bitmap bitmap = BitmapFactory.decodeByteArray(picture, 0, picture.length);
				
				Matrix matrix = new Matrix();
				matrix.postRotate(rotate);
				bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), 
						bitmap.getHeight(), matrix, true);
				
				iv.setImageBitmap(bitmap);
				break;
			  case -1:
				Log.e(LOG_TAG, "Cannot get picture, server error.");
				break;
			  case -2: 
				Log.e(LOG_TAG, "Cannot get picture, network error.");
				break;
			}
    	}
	};
}
