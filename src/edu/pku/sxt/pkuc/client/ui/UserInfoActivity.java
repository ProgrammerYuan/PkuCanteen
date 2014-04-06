package edu.pku.sxt.pkuc.client.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import edu.pku.sxt.pkuc.client.R;
import edu.pku.sxt.pkuc.client.util.HttpManager;
import edu.pku.sxt.pkuc.client.util.ImageModification;
import edu.pku.sxt.pkuc.client.util.MD5;
import edu.pku.sxt.pkuc.client.util.MD5Exception;

public class UserInfoActivity extends ActionBarActivity {
	
	/****** SETTINGS ******/

	// upload avatar 
	private final static int PIC_WIDTH = 100;
	private final static int PIC_HEIGHT = 100;
	private final static int PIC_QUALITY = 30;
	
	// user level image id
	public static final int[] lv_img = {
		R.drawable.level11, R.drawable.level12, R.drawable.level13, R.drawable.level14, R.drawable.level15,
		R.drawable.level21, R.drawable.level22, R.drawable.level23, R.drawable.level24, R.drawable.level25,
		R.drawable.level31, R.drawable.level32, R.drawable.level33, R.drawable.level34, R.drawable.level35,
		R.drawable.level41, R.drawable.level42, R.drawable.level43, R.drawable.level44, R.drawable.level45,
		R.drawable.level51, R.drawable.level52, R.drawable.level53, R.drawable.level54, R.drawable.level55
	};
	
	// user level score
	public static final int[] lv_score = {
		100, 200, 300, 400, 500,
		700, 900, 1100, 1300, 1500,
		1800, 2100, 2400, 2700, 3000,
		3500, 4000, 4500, 5000, 5500, 
		6500, 7500, 8500, 9500, 10500
	};

	/****** END OF SETTINGS ******/
	
	private static final String LOG_TAG = "UserInfoActivity";
	
	// UI Views
	TextView unameText;
	TextView emailText;
	TextView mobileText;
	TextView accountText;
	ImageButton avatarIB;
	ImageButton editEmailIB;
	ImageButton editAccountIB;
	ImageButton editMobileIB;
	Button logoutButton;
	Button registerButton;
	Button loginButton;
	ImageView lvIV, nlvIV;
	TextView lvTV, nlvTV;
	Button messageButton;
	Button aboutLvButton;
	Button rankButton;
	ProgressBar progress;
	TextView cText, wrText, wcText, wpText, trText, tcText, tpText;
	
	// shared preferences
	SharedPreferences sp;
	
	// data
	int c=0; // continuous report days
	int wr=0; // reports in this week
	int wc=0; // comments in this week
	int wp=0; // pictures in this week
	int tr=0; // reports in total
	int tc=0; // comments in total
	int tp=0; // pictures in total
	boolean isUser;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// action bar
		getSupportActionBar().hide();
		
		// shared preferences
		sp = getSharedPreferences(getString(R.string.shared_preferences_name), Activity.MODE_PRIVATE);
		
		// set UI differently for logged in or not logged in
		
		// if already logged in
		if (sp.getBoolean("isLogin", false)) {
			isUser = true;
			setContentView(R.layout.activity_user_info);
			
			// UI Views
			unameText = (TextView)findViewById(R.id.txt_uname);
			accountText = (TextView)findViewById(R.id.txt_account);
			emailText = (TextView)findViewById(R.id.txt_email);
			mobileText = (TextView)findViewById(R.id.txt_mobile);
			avatarIB = (ImageButton)findViewById(R.id.ib_avatar);
			logoutButton = (Button)findViewById(R.id.but_logout);
			editEmailIB = (ImageButton)findViewById(R.id.ib_edit_email);
			editMobileIB = (ImageButton)findViewById(R.id.ib_edit_mobile);
			editAccountIB = (ImageButton)findViewById(R.id.ib_edit_account);
			lvIV = (ImageView)findViewById(R.id.img_lv);
			nlvIV = (ImageView)findViewById(R.id.img_next_lv);
			lvTV = (TextView)findViewById(R.id.txt_lv);
			nlvTV = (TextView)findViewById(R.id.txt_next_lv);
			messageButton = (Button)findViewById(R.id.but_message);
			if(sp.getBoolean("hasNewMessage", false)){
				messageButton.setText(R.string.new_message);
			} else {
				messageButton.setText(R.string.message);
			}
			messageButton.setOnClickListener(new Button.OnClickListener(){
				public void onClick(View arg0){
					Intent intent = new Intent(UserInfoActivity.this,MessageActivity.class);
					startActivity(intent);
				}
			});
			rankButton = (Button)findViewById(R.id.but_rank);
			rankButton.setOnClickListener(new Button.OnClickListener(){

				@Override
				public void onClick(View arg0) {
					Intent intent = new Intent(UserInfoActivity.this,RankActivity.class);
					startActivity(intent);
				}
			
			});
			// set user name
			unameText.setText(sp.getString("uname", ""));
			
			// avatar image button
			avatarIB.setOnClickListener(new ImageButton.OnClickListener() {
				@Override
				public void onClick(View v) {
					new GetPicDialogFragment().show(getSupportFragmentManager(), "pic");
				}
			});
			avatarIB.setImageResource(R.drawable.avatar);
			if (sp.getString("avatarOf", "").equals(sp.getString("uname", ""))) {
				File imgDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
				File imgFile = new File(imgDir, "avatar.jpg");
				int orientation = 0;
				try {
					ExifInterface exif = new ExifInterface(imgFile.getPath());
					orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
				} catch (Exception e) {
					Log.e(LOG_TAG, e.getMessage());
				}
				float rotate = 0;
				switch (orientation) {
				  case 6: rotate = 90; break;
				  case 3: rotate = 180; break;
				  case 8: rotate = 270; break;
				}
				Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getPath());
				
				Matrix matrix = new Matrix();
				matrix.postRotate(rotate);
				bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), 
						bitmap.getHeight(), matrix, true);
				
				avatarIB.setImageBitmap(bitmap);	
			}
			
			// edit email image button
			editEmailIB.setOnClickListener(new ImageButton.OnClickListener(){
				@Override
				public void onClick(View arg0) {
					EditEmailDialogFragment dialog = new EditEmailDialogFragment();
					dialog.setActivity(UserInfoActivity.this);
					dialog.show(getSupportFragmentManager(), "email");
				}
			});
			
			// edit mobile image button
			editMobileIB.setOnClickListener(new ImageButton.OnClickListener(){
				@Override
				public void onClick(View arg0) {
					EditMobileDialogFragment dialog = new EditMobileDialogFragment();
					dialog.setActivity(UserInfoActivity.this);
					dialog.show(getSupportFragmentManager(), "mobile");
				}
			});
			
			editAccountIB.setOnClickListener(new ImageButton.OnClickListener(){
				public void onClick(View arg0){
					EditAccountDialogFragment dialog = new EditAccountDialogFragment();
					dialog.setActivity(UserInfoActivity.this);
					dialog.show(getSupportFragmentManager(), "Account");
				}
			});
			// logout button
			logoutButton.setOnClickListener(new Button.OnClickListener(){
				@Override
				public void onClick(View v) {
					sp.edit().putBoolean("isLogin", false).commit();
					finish();
				}
			});
		}
		
		// if not logged in
		else {
			isUser = false;
			setContentView(R.layout.activity_user_info_not_user);
			
			// UI Views
			loginButton = (Button)findViewById(R.id.but_login);
			registerButton = (Button)findViewById(R.id.but_register);
			lvIV = (ImageView)findViewById(R.id.img_lv_will_be);
			lvTV = (TextView)findViewById(R.id.txt_lv_will_be);
			
			// login button
			loginButton.setOnClickListener(new Button.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(UserInfoActivity.this, LoginActivity.class);
					startActivity(intent);
					finish();
				}
			});
			
			// register button
			registerButton.setOnClickListener(new Button.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(UserInfoActivity.this, RegisterActivity.class);
					startActivity(intent);
					finish();
				}
			});
		}
	
		// set common UI
		cText = (TextView)findViewById(R.id.txt_continuous);
		wrText = (TextView)findViewById(R.id.txt_wr);
		wcText = (TextView)findViewById(R.id.txt_wc);
		wpText = (TextView)findViewById(R.id.txt_wp);
		trText = (TextView)findViewById(R.id.txt_tr);
		tcText = (TextView)findViewById(R.id.txt_tc);
		tpText = (TextView)findViewById(R.id.txt_tp);
		progress = (ProgressBar)findViewById(R.id.progressBar);
		aboutLvButton = (Button)findViewById(R.id.but_about_lv);
		aboutLvButton.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(UserInfoActivity.this);
				builder.setMessage(getString(R.string.level_help))
					.setTitle(getString(R.string.about_level_label));
				builder.create().show();
			}
		});
		
		// get user information from server
		getUserInfo();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK) {
			Log.e(LOG_TAG, "Activity Result Code: "+resultCode);
			return;
		}
		File imgDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
		Uri orgUri, avaTmpUri;
		File imgFile = new File(imgDir, "avatar_tmp.jpg");
		avaTmpUri = Uri.fromFile(imgFile);
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
			OutputStream o = cr.openOutputStream(avaTmpUri);
			byte buffer[] = new byte[1024];
			int count;
			while ((count = i.read(buffer)) > 0) {
				o.write(buffer, 0, count);
			}
			i.close();
			o.close();
			
			File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES)
					, "avatar_tmp.jpg");
			String filePath = file.getPath();
			
			int orientation = 0;
			try{
				ExifInterface exif = new ExifInterface(filePath);
				orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
			} catch(Exception e) {
				Log.e(LOG_TAG, e.getMessage());
			}
			
			byte[] b = ImageModification.compress(filePath, PIC_WIDTH, PIC_HEIGHT, PIC_QUALITY);
			
			file = new File(file.getParentFile(), "avatar_tmp2.jpg");
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
			} catch (Exception e){
				e.printStackTrace();
			}
			
		} catch (Exception e) {
			Log.e(LOG_TAG, e.getMessage());
			makeToast(getString(R.string.cannot_import_picture));
			return;
		} 
		updateAvatar();
	}
	
	/**
	 * Display a long-time toast.
	 * @param message Message of toast.
	 */
    private void makeToast(String message) {
    	Toast toast = Toast.makeText(UserInfoActivity.this, message, Toast.LENGTH_LONG);
    	toast.show();
    }
    
    /**
     * Make a get-user-information request.
     */
    private void getUserInfo() {
    	progress.setVisibility(View.VISIBLE);
    	// set http post parameters
		final Map<String, String> reqParas = new HashMap<String, String>();
		reqParas.put("t", "uif");
		boolean isLogin = sp.getBoolean("isLogin", false);
		if(isLogin)
			reqParas.put("nam", sp.getString("uname", ""));
		else
			reqParas.put("nam", sp.getString("imei", ""));
		reqParas.put("pwd", sp.getString("pword", ""));
		reqParas.put("isu", String.valueOf(isLogin ? 1:0));
		final String url = getString(R.string.server_url); 			
		
		// do post in new thread
	    Runnable runnable = new Runnable() {
			@Override
			public void run() {
				int status;
				String email = "";
				String mobile = ""; 
				String avamd5 = "";
				String c = "";
				String wr = "";
				String wc = "";
				String wp = "";
				String tr = "";
				String tc = "";
				String tp = "";
				String ct = "";
				String fb = "";
				try {
					Map<String, String> resParas = HttpManager.postKV(url, reqParas);
					status = Integer.parseInt(resParas.get("stat"));
					email = resParas.get("email");
					mobile = resParas.get("mobile");
					avamd5 = resParas.get("avamd5");
					c = resParas.get("c");
					wr = resParas.get("wr");
					wc = resParas.get("wc");
					wp = resParas.get("wp");
					tr = resParas.get("tr");
					tc = resParas.get("tc");
					tp = resParas.get("tp");
					ct = resParas.get("ct");
					fb = resParas.get("fb");
				} catch (Exception e) {
					status = -2; // network error
					Log.e(LOG_TAG, e.getMessage());
				}				
				Message msg = new Message();
				Bundle data = new Bundle();
				data.putInt("status", status);
				data.putString("email", email);
				data.putString("mobile", mobile);
				data.putString("avamd5", avamd5);
				data.putString("c", c);
				data.putString("wr", wr);
				data.putString("wc", wc);
				data.putString("wp", wp);
				data.putString("tr", tr);
				data.putString("tc", tc);
				data.putString("tp", tp);
				data.putString("ct", ct);
				data.putString("fb", fb);
				msg.setData(data);
				getUserInfoHandler.sendMessage(msg);
			}
	    };
	    new Thread(runnable).start();
    }
    
    /**
     * Handle response of get-user-information request.
     */
    Handler getUserInfoHandler = new Handler() {
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
				String email = data.getString("email");
				String mobile = data.getString("mobile");
				String avamd5 = data.getString("avamd5");
				
				if (!email.equals("null")) {
					emailText.setText(email);
				}
				if (!mobile.equals("null")) {
					mobileText.setText(mobile);
				}
				
				if (!avamd5.equals(null) && !avamd5.equals(sp.getString("avamd5", ""))) {
					getAvatar(avamd5);
				}
				
				cText.setText(data.getString("c") + "å¤");
				wrText.setText(data.getString("wr"));
				wcText.setText(data.getString("wc"));
				wpText.setText(data.getString("wp"));
				trText.setText(data.getString("tr"));
				tcText.setText(data.getString("tc"));
				tpText.setText(data.getString("tp"));
				
				c = Integer.valueOf(data.getString("c"));
				wr = Integer.valueOf(data.getString("wr"));
				wc = Integer.valueOf(data.getString("wc"));
				wp = Integer.valueOf(data.getString("wp"));
				tr = Integer.valueOf(data.getString("tr"));
				tc = Integer.valueOf(data.getString("tc"));
				tp = Integer.valueOf(data.getString("tp"));
				
				int ct = Integer.valueOf(data.getString("ct"));
				int fb = Integer.valueOf(data.getString("fb"));
				int score = ct + fb;
				
				if (isUser) {
					int i;
					for (i = 0; i < lv_score.length; i++) {
						if(score < lv_score[i])
							break;
					}
					if (i == lv_score.length)
						i--;
					lvIV.setImageResource(lv_img[i]);
					lvTV.setText("("+score+")");
					if (i < lv_score.length-1)
						i+=1;
					nlvIV.setImageResource(lv_img[i]);
					nlvTV.setText("("+lv_score[i]+")");
				} else {
					int i;
					for (i = 0; i < lv_score.length; i++) {
						if(score < lv_score[i])
							break;
					}
					if (i == lv_score.length)
						i--;
					lvIV.setImageResource(lv_img[i]);
					lvTV.setText(getString(R.string.non_user_level));
				}
				break;
			  case 1: 
				makeToast(getString(R.string.account_error_relogin));
				Log.e(LOG_TAG, "Get User Information Failed: account error");
				Intent intent = new Intent(UserInfoActivity.this, LoginActivity.class);
				startActivity(intent);
				finish();
				break;
			  case -1: 
				makeToast(getString(R.string.cannot_get_user_info));
				Log.e(LOG_TAG, "Get User Information Failed: server error");
				finish();
				break;
			  case -2: 
				makeToast(getString(R.string.cannot_get_user_info));
				Log.e(LOG_TAG, "Get User Information Failed: network error");
				finish();
				break;
			}
			progress.setVisibility(View.GONE);
		}
	};

	/**
	 * Make a get-avatar request.
	 * @param newAvaMd5 MD5 of new avatar. Get from user information.s
	 */
	private void getAvatar(String newAvaMd5) {
    	// set http post parameters
		final Map<String, String> reqParas = new HashMap<String, String>();
		reqParas.put("t", "ava");
		reqParas.put("nam", sp.getString("uname", ""));
		final String url = getString(R.string.server_url); 
		final String avamd5 = newAvaMd5;
		
		// do post in new thread
	    Runnable runnable = new Runnable() {
			@Override
			public void run() {
				int status;
				byte[] avatar = new byte[1];
				try {
					avatar = HttpManager.postForFile(url, reqParas);
					status = 0;
				} catch (Exception e) {
					status = -2; // network error
					Log.e(LOG_TAG, e.getMessage());
				}				
				Message msg = new Message();
				Bundle data = new Bundle();
				data.putInt("status", status);
				data.putByteArray("avatar", avatar);
				data.putString("avamd5", avamd5);
				msg.setData(data);
				getAvatarHandler.sendMessage(msg);
			}
	    };
	    new Thread(runnable).start();
    }
    
	/**
	 * Handle response of get-avatar request.
	 */
    Handler getAvatarHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			Bundle data = msg.getData();
			int status = data.getInt("status");
			/*
			 * 0 - success
			 * -2 - network error
			 */
			switch (status) {
			  case 0: 
				String avamd5 = data.getString("avamd5");
				byte[] avatar = data.getByteArray("avatar");
				File imgDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
				File imgFile = new File(imgDir, "avatar.jpg");
				try {
					FileOutputStream fos = new FileOutputStream(imgFile);
					fos.write(avatar);
					fos.close();
					sp.edit()
					.putString("avatarOf", sp.getString("uname", ""))
					.putString("avamd5", avamd5)
					.commit();
					
					int orientation = 0;
					try {
						ExifInterface exif = new ExifInterface(imgFile.getPath());
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
					Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getPath());
					
					Matrix matrix = new Matrix();
					matrix.postRotate(rotate);
					bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), 
							bitmap.getHeight(), matrix, true);
					
					avatarIB.setImageResource(R.drawable.avatar);
					avatarIB.setImageBitmap(bitmap);
				} catch (Exception e) {
					String emsg = e.getMessage();
					if (emsg == null)
						emsg = "exception has no message";
					Log.e(LOG_TAG, emsg);
					return;
				}
				break;
			  case -2: 
				Log.e(LOG_TAG, "Get avatar failed: network error");
				break;
			}
		}
	};

	/**
	 * Make a update avatar request.
	 */
	private void updateAvatar() {
		final File f = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES)
				, "avatar_tmp2.jpg");
		// update avatar
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				// set http post parameters
				final Map<String, String> reqParas = new HashMap<String, String>();
				reqParas.put("t", "uda");
				String uname = sp.getString("uname", "");
				reqParas.put("nam", uname);
				reqParas.put("pwd", sp.getString("pword", ""));
				String avamd5;
				try {
					avamd5 = MD5.md5(f);
				} catch (MD5Exception e) {
					Log.e(LOG_TAG, e.getMessage());
					Message msg = new Message();
					Bundle data = new Bundle();
					data.putInt("status", -3);
					msg.setData(data);
					updateAvatarHandler.sendMessage(msg);
					return;
				}
				reqParas.put("avamd5", avamd5);
				
				Map<String, File> files = new HashMap<String, File>();
				files.put("avatar.jpg", f);
				int status;
				try {
					Map<String, String> resParas = HttpManager.postFile(getString(R.string.server_url), reqParas, files);
					status = Integer.parseInt(resParas.get("stat"));
				} catch (Exception e) {
					status = -2;
					e.printStackTrace();
				}
				Message msg = new Message();
				Bundle data = new Bundle();
				data.putInt("status", status);
				data.putString("avatarOf", uname);
				data.putString("avamd5", avamd5);
				msg.setData(data);
				updateAvatarHandler.sendMessage(msg);
			}
		};
		new Thread(runnable).start();
	}

	/**
	 * Handle response of update-avatar request.
	 */
	Handler updateAvatarHandler = new Handler() {
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
			 * -3 - picture error
			 */
    		switch (status) {
			  case 0: 
				File imgDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
				File input = new File(imgDir, "avatar_tmp2.jpg");
				File output = new File(imgDir, "avatar.jpg");
				input.renameTo(output);
				
				String avatarOf = data.getString("avatarOf");
				String avamd5 = data.getString("avamd5");
				sp.edit()
				.putString("avatarOf", avatarOf)
				.putString("avamd5", avamd5)
				.commit();		
				
				int orientation = 0;
				try{
					ExifInterface exif = new ExifInterface(output.getPath());
					orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
				} catch (Exception e){
					e.printStackTrace();
				}
				float rotate = 0;
				switch (orientation) {
				  case 6: rotate = 90; break;
				  case 3: rotate = 180; break;
				  case 8: rotate = 270; break;
				}
				Bitmap bitmap = BitmapFactory.decodeFile(output.getPath());
				
				Matrix matrix = new Matrix();
				matrix.postRotate(rotate);
				bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), 
						bitmap.getHeight(), matrix, true);
				// reset image
				avatarIB.setImageResource(R.drawable.avatar);
				avatarIB.setImageBitmap(bitmap);
				break;
			  case 1: 
				makeToast(getString(R.string.account_error_relogin));
				Log.e(LOG_TAG, "Upload Avatar Failed: account error");
				Intent intent = new Intent(UserInfoActivity.this, LoginActivity.class);
				startActivity(intent);
				finish();
				break;
			  case -1: 
				makeToast(getString(R.string.upload_avatar_failed));
				Log.e(LOG_TAG, "Upload Avatar Failed: server error");
				break;
			  case -2: 
				makeToast(getString(R.string.upload_avatar_failed));
				Log.e(LOG_TAG, "Upload Avatar Failed: network error");
				break;
			  case -3: 
				makeToast(getString(R.string.cannot_import_picture));
				Log.e(LOG_TAG, "Upload Avatar Failed: import picture error");
				break;
			}
    	}
    };
}
