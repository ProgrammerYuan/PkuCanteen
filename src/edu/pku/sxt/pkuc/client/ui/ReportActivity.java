package edu.pku.sxt.pkuc.client.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import edu.pku.sxt.pkuc.client.Canteen;
import edu.pku.sxt.pkuc.client.R;
import edu.pku.sxt.pkuc.client.util.CanteenDistanceComparator;
import edu.pku.sxt.pkuc.client.util.HttpManager;
import edu.pku.sxt.pkuc.client.util.ImageModification;

public class ReportActivity extends ActionBarActivity {
	
	/****** SETTINGS ******/
	
	// upload picture
	private final static int UPLOAD_PIC_WIDTH = 400;
	private final static int UPLOAD_PIC_HEIGHT = 300;
	private final static int UPLOAD_PIC_QUALITY = 30;
	
	// upload retry delay
	private final static long REPORT_RETRY_DELAY = 30 * 1000;
	private final static long PICTURE_RETRY_DELAY = 30 * 1000;
	// GPS upload delay
	private final static long GPS_interval = 10 * 60 * 1000;
	
	
	/****** END OF SETTINGS ******/
	
	private final static String LOG_TAG = "ReportActivity";
	
	// broadcast identifier
	public static final String UPLOAD_SUCCESS_ACTION = "pkuc.upload_success";
	
	// UI Views
	Button returnButton;
	Spinner titleSpinner;
	RatingBar levelRate;
	ImageButton pictureIB;
	EditText commentET;
	Button confirmButton;
	TextView rewardText;

	// shared preferences
	SharedPreferences sp;
	
	// data
	int reward; // current reward for reporting
	boolean hasPicture = false; // user added a picture
	List<Canteen> canteens;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_report);
		
		// get shared preferences
		sp = getSharedPreferences("PKUC", Activity.MODE_PRIVATE);
		
		// initialize data
		int cat_id = getIntent().getExtras().getInt("cat_id");
		reward = getIntent().getExtras().getInt("reward");
		
		// custom action bar
		ActionBar bar = getSupportActionBar();
		bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#5CACEE")));
		bar.setDisplayShowTitleEnabled(false);
		bar.setDisplayShowCustomEnabled(true);
		bar.setDisplayShowHomeEnabled(false);
		View v = getLayoutInflater().inflate(R.layout.report_actionbar, null);
		bar.setCustomView(v);
		
		// set UI Views
		returnButton = (Button)findViewById(R.id.but_return);
		titleSpinner = (Spinner)findViewById(R.id.ctitle_spin);
		levelRate = (RatingBar)findViewById(R.id.level_rate);
		pictureIB = (ImageButton)findViewById(R.id.ib_picture);
		commentET = (EditText)findViewById(R.id.et_comment);
		confirmButton = (Button)findViewById(R.id.but_confirm);
		rewardText = (TextView)findViewById(R.id.txt_reward);
		// rating bar
		levelRate.setRating((float) 2.5);
		// reward
		if(reward != -1)
			rewardText.setText("ÉÏ´«½±Àø"+reward+"·Ö");
		// spinner
		canteens = new ArrayList<Canteen>(MainActivity.canteens);
		int selection = 0;
		if (canteens.size() != 0) {
			titleSpinner.setClickable(true);
			if (MainActivity.location != null && 
					MainActivity.location.getTime() + 5 * 60 * 1000 > new Date().getTime()) {
				Collections.sort(canteens, new CanteenDistanceComparator(MainActivity.location));
				selection = 0;
			} else {
				if(cat_id != -1){
					for(int i=0; i<canteens.size(); i++){
						if(canteens.get(i).id == cat_id){
							selection = i;
							break;
						}
					}
				}
			}
		} else {
			titleSpinner.setClickable(false);
		}
		String[] titles = new String[canteens.size()];
		for (int i = 0; i < canteens.size(); i++) {
			titles[i] = canteens.get(i).title;
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, 
				android.R.layout.simple_spinner_item, titles);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		titleSpinner.setAdapter(adapter);
		if (canteens.size() != 0)
			titleSpinner.setSelection(selection);
		// picture image button
		pictureIB.setOnClickListener(new ImageButton.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				File f = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "tmp.jpg");
				intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
				startActivityForResult(intent, ImageModification.REQUEST_CODE_CAPTURE);
			}
		});
		// return button
		returnButton.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View v) { finish(); }
		});
		// confirm button
		confirmButton.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View v) { report(); }
		});
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		if (resultCode != RESULT_OK) {
			Log.e(LOG_TAG, "Activity Result Code: "+resultCode);
			return;
		}
		File imgDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
		Uri orgUri, uploadTmpUri;
		File imgFile = new File(imgDir, "upload_tmp.jpg");
		uploadTmpUri = Uri.fromFile(imgFile);
		ContentResolver cr = getContentResolver();;
		
		switch (requestCode) {
		  case ImageModification.REQUEST_CODE_CAPTURE:	// capture
			orgUri = Uri.fromFile(new File(imgDir, "tmp.jpg"));
			break;
		  case ImageModification.REQUEST_CODE_IMPORT:	// picture
			orgUri = data.getData();
			break;
		  default:
			Log.e(LOG_TAG, "Unknown request code: "+requestCode);
			makeToast(getString(R.string.cannot_import_picture));
			return;
		}
		
		try {
			InputStream i = cr.openInputStream(orgUri);
			OutputStream o = cr.openOutputStream(uploadTmpUri);
			byte buffer[] = new byte[1024];
			int count;
			while ((count = i.read(buffer)) > 0) {
				o.write(buffer, 0, count);
			}
			i.close();
			o.close();
			
			File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES)
					, "upload_tmp.jpg");
			String filePath = file.getPath();
			
			int orientation = 0;
			try{
				ExifInterface exif = new ExifInterface(filePath);
				orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
			} catch(Exception e){
				e.printStackTrace();
			}
			
			byte[] b = ImageModification.compress(filePath, UPLOAD_PIC_WIDTH, UPLOAD_PIC_HEIGHT, UPLOAD_PIC_QUALITY);
			
			file = new File(file.getParentFile(), "upload.jpg");
			filePath = file.getPath();
			try {
				FileOutputStream fos = new FileOutputStream(filePath);
				fos.write(b);
				fos.close();
			} catch (IOException e) {
				Log.e(LOG_TAG, e.getMessage());
				finish();
				return;
			}
			
			try {
				ExifInterface exif = new ExifInterface(filePath);
				exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(orientation));
				exif.saveAttributes();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			float rotate = 0;
			switch (orientation) {
			  case 6: rotate = 90; break;
			  case 3: rotate = 180; break;
			  case 8: rotate = 270; break;
			}
			
			Bitmap bitmap = BitmapFactory.decodeFile(filePath);
			
			Matrix matrix = new Matrix();
			matrix.postRotate(rotate);
			bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), 
					bitmap.getHeight(), matrix, true);
			
			hasPicture = true;
			pictureIB.setImageResource(R.drawable.photo);
			pictureIB.setImageBitmap(bitmap);
		} catch (Exception e) {
			Log.e(LOG_TAG, e.getMessage());
			makeToast(getString(R.string.cannot_import_picture));
			return;
		} 
	}
	
	/**
	 * Display a long-time toast.
	 * @param message Message of toast.
	 */
	private void makeToast(String message){
		Toast toast = Toast.makeText(ReportActivity.this, message, Toast.LENGTH_LONG);
		toast.show();
	}

	/**
	 * Make a report request.
	 */
	private void report() {
		String nam, pwd, isu;
		final int cid = canteens.get(titleSpinner.getSelectedItemPosition()).id;
		if (sp.getBoolean("isLogin", false)) {
			nam = sp.getString("uname", "");
			pwd = sp.getString("pword", "");
			isu = "1";
		} else {
			nam = sp.getString("imei", "");
			pwd = "";
			isu = "0";
		}
		double lv = levelRate.getRating();
		String com = commentET.getText().toString();
		SimpleDateFormat   sDateFormat   =   new   SimpleDateFormat("yyyy-MM-dd   hh:mm:ss");     
	    String   date   =   sDateFormat.format(new   java.util.Date()); 
		// set http post parameters
		final Map<String, String> reqParas = new HashMap<String, String>();
		reqParas.put("t", "rep");
		reqParas.put("isu", isu);
		reqParas.put("nam", nam);
		reqParas.put("pwd", pwd);
		reqParas.put("cid", String.valueOf(cid));
		reqParas.put("lv", String.valueOf(lv));
		reqParas.put("com", com);
		reqParas.put("reward", String.valueOf(reward));
		reqParas.put("time",date);
		final String url = getString(R.string.server_url); 			
		
		// do post in new thread
	    Runnable runnable = new Runnable() {
			@Override
			public void run() {
				int rid = -1;
				int status = -2;
				for (int i = 0; i < 3; i++) {
					try {
						Map<String, String> resParas = HttpManager.postKV(url, reqParas);
						status = Integer.parseInt(resParas.get("stat"));
						rid = Integer.parseInt(resParas.get("rid"));
						if(status == 0)
							break;
					} catch (Exception e) {
						status = -2; // network error
						Log.e(LOG_TAG, e.getMessage());
					}				
					try {
						Thread.sleep(REPORT_RETRY_DELAY);
					} catch (Exception e) {
						Log.e(LOG_TAG, e.getMessage());
					}
				}
				Message msg = new Message();
				Bundle data = new Bundle();
				data.putInt("status", status);
				data.putInt("rid", rid);
				data.putInt("cid", cid);
				data.putInt("reward", reward);
				msg.setData(data);
				reportHandler.sendMessage(msg);
			}
	    };
	    new Thread(runnable).start();
	    finish();
	}
	
	/**
	 * Handle response of report request.
	 */
	Handler reportHandler = new Handler(){
		@Override
    	public void handleMessage(Message msg){
    		super.handleMessage(msg);
    		Bundle data = msg.getData();
    		int status = data.getInt("status");
    		/*
			 * 0 - success
			 * 1 - account error
			 * -1 - server error
			 * -2 - network error
			 */
			switch (status) {
			  case 0: 
				int cid = data.getInt("cid");
				if (hasPicture) {
					int rid = data.getInt("rid");
					uploadPicture(rid, cid);
				} else {
					makeToast(getString(R.string.upload_success));
					Intent intent = new Intent();
					intent.setAction(UPLOAD_SUCCESS_ACTION);
					intent.putExtra("cid", cid);
					sendBroadcast(intent);
					finish();
				}
				break;
			case 1:
				makeToast(getString(R.string.upload_failed));
				Log.e(LOG_TAG, "Upload Failed: account error");
				break;
			case -1:
				makeToast(getString(R.string.upload_failed));
				Log.e(LOG_TAG, "Upload Failed: server error");
				break;
			case -2: 
				makeToast(getString(R.string.upload_failed));
				Log.e(LOG_TAG, "Upload Failed: network error");
				break;
			}
    	}
	};

	/**
	 * Make a report-picture request
	 * @param rid report id
	 * @param cid canteen id
	 */
	private void uploadPicture(int rid, final int cid) {
		File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES)
				, "upload.jpg");
		final File f = file;
		
		// set http post parameters
		final Map<String, String> reqParas = new HashMap<String, String>();
		reqParas.put("t", "rpc");
		reqParas.put("rid", String.valueOf(rid));
		final String url = getString(R.string.server_url); 	
		
		// do post in new thread
	    Runnable runnable = new Runnable(){
			@Override
			public void run() {
				Map<String, File> files = new HashMap<String, File>();
				files.put("upload.jpg", f);
				int status = -2;
				for (int i = 0; i < 5; i++) {
					try {
						Map<String, String> resParas = HttpManager.postFile(url, reqParas, files);
						status = Integer.parseInt(resParas.get("stat"));
						if(status == 0)
							break;
					} catch (Exception e) {
						status = -2; // network error
						Log.e(LOG_TAG, e.getMessage());
					}
					try {
						Thread.sleep(PICTURE_RETRY_DELAY);
					} catch (Exception e){
						Log.e(LOG_TAG, e.getMessage());
					}
				}
				Message msg = new Message();
				Bundle data = new Bundle();
				data.putInt("status", status);
				data.putInt("cid", cid);
				msg.setData(data);
				uploadPictureHandler.sendMessage(msg);
			}
	    };
	    new Thread(runnable).start();
	}

	/**
	 * Handle response of report-picture request.
	 */
	Handler uploadPictureHandler = new Handler() {
		@Override
    	public void handleMessage(Message msg) {
    		super.handleMessage(msg);
    		Bundle data = msg.getData();
    		int status = data.getInt("status");
    		/*
			 * 0 - success
			 * 1 - account error
			 * -1 - server error
			 * -2 - network error
			 */
			switch (status) {
			  case 0: 
				makeToast(getString(R.string.upload_success));
				Intent intent = new Intent();
				intent.setAction(UPLOAD_SUCCESS_ACTION);
				intent.putExtra("cid", data.getInt("cid"));
				sendBroadcast(intent);
				finish();
				break;
			  case 1:
				makeToast(getString(R.string.upload_picture_failed));
				Log.e(LOG_TAG, "Upload Picture Failed: rid error");
				break;
			  case -1:
				makeToast(getString(R.string.upload_picture_failed));
				Log.e(LOG_TAG, "Upload Picture Failed: server error");
				break;
			  case -2: 
				makeToast(getString(R.string.upload_picture_failed));
				Log.e(LOG_TAG, "Upload Picture Failed: network error");
				break;
			}
    	}
	};
}
