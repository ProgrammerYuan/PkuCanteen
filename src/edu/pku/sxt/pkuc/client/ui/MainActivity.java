package edu.pku.sxt.pkuc.client.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import edu.pku.sxt.pkuc.client.Canteen;
import edu.pku.sxt.pkuc.client.R;
import edu.pku.sxt.pkuc.client.util.CanteenDistanceComparator;
import edu.pku.sxt.pkuc.client.util.HttpManager;
import edu.pku.sxt.pkuc.client.util.HttpManagerException;

/**
 * Main activity of the application, display a list
 * of all canteens, show basic information of each canteen.
 * @author songxintong
 *
 */
public class MainActivity extends ActionBarActivity {
	
	/****** SETTINGS ******/
	
	private static final int CLIENT_VERSION = 2; // client version number
	private static final int LOCATION_TRACKING_TIME = 1000 * 60 * 15; 
		// time in milliseconds to keep tracking location after go background
	
	/****** END OF SETTINGS ******/
	
	private static final String LOG_TAG = "MainActivity";
	
	// Application Scope Variables
	public static boolean isInBackground;
	public static Location location = null; // latest known user location
	public static boolean locListening;
	// UI views
	Button accountButton;
	ImageButton avatarButton;
	ImageButton reportButton;
	ListView canteenList;
	Button refreshButton;
	ProgressBar progressBar;
	TextView rewardText;
	ImageButton helpButton;
	
	// canteenList adapter parameters
	final static String[] listFrom = new String[] {"canteen_img", "canteen_title", 
			"canteen_lv_rate", "canteen_level", "canteen_comment","canteen_reward"};
	final static int[] listTo = new int[] {R.id.canteen_img, R.id.canteen_title, 
			R.id.canteen_lv_rate, R.id.canteen_level, R.id.canteen_comment,R.id.canteen_reward};
	//GPS collecting time period
	final static int GPSInterval = 60 * 1000;
	final static int[] noon = new int[] {0,130000};
	final static int[] night = new int[] {130000,240000};
	// shared preferences
	SharedPreferences sp;
	// data
	public static List<Canteen> canteens;	// canteens' information
	List<Map<String, Object>> listData;	// data of canteenLists
	int reward = -1; // current reward for reporting
	Date lastRefreshDate = null;
	// GPS file relevant
	final static String GPSFile = "GPS_Data.txt";
	final static SDCard sd = new SDCard();
	int count = 0;
	// location
	LocationManager lm;
	LocationListener locListener = new LocationListener() {
		@Override
		public void onLocationChanged(Location loc) {
			makeToast("locListener");
			if (loc != null){
				String msg = "Location Changed: " + loc.getProvider() + 
						" " + loc.getLatitude() + " " + loc.getLongitude();
				Log.d(LOG_TAG, msg);
				if (location == null) {
					location = loc;
				} else if (loc.getProvider().equals(LocationManager.GPS_PROVIDER)) {
					location = loc;
				} else if (loc.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
					if (location.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
						location = loc;
					} else if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
						if (loc.getTime() - 5 * 60 * 1000 > location.getTime()) {
							location = loc;
						} else {
							return;
						}
					}
				} else {
					return;
				}
				
				if (canteens != null) {
					Collections.sort(canteens, new CanteenDistanceComparator(location));
					resetCanteenList();
				}
			}
		}
		
		@Override
		public void onProviderDisabled(String provider) {
			Log.v(LOG_TAG, "Location Provider Disabled: " + provider);
		}
		@Override
		public void onProviderEnabled(String provider) {
			Log.v(LOG_TAG, "Location Provider Enabled: " + provider);
		}
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			Log.v(LOG_TAG, "Location Status Changed: " + provider + " status " + status);
		}
	};
	
	// receive broadcast of uploading-user-report-finished
	BroadcastReceiver reportFinishedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			getCanteenList();
		}
	};
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// initialize SDCard;
		sd.FileHelper(getApplicationContext());
		// set UI
		progressBar = (ProgressBar)findViewById(R.id.progressBar);
		canteenList = (ListView)findViewById(R.id.lv_canteen);
		canteenList.setOnItemClickListener(new ListView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				Intent intent = new Intent(MainActivity.this, DetailActivity.class);
				intent.putExtra("id", canteens.get(position).id);
				startActivity(intent);
			}
		});
		refreshButton = (Button)findViewById(R.id.but_refresh);
		refreshButton.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View arg0) { getCanteenList(); }
		});
		
		// get shared preferences
		sp = getSharedPreferences(getString(R.string.shared_preferences_name), Activity.MODE_PRIVATE);
		// initialize data
		canteens = new ArrayList<Canteen>();
		listData = new ArrayList<Map<String, Object>> ();
		
		// set upload success broadcast receiver
		IntentFilter filter = new IntentFilter();
		filter.addAction(ReportActivity.UPLOAD_SUCCESS_ACTION);
		registerReceiver(reportFinishedReceiver, filter);
		
		// check for new version
		checkVersion();
		
		// get canteen list from server
		getCanteenList();
	}
	
	protected void onStart() {
		
		super.onStart();
		
		isInBackground = false;
		locListening = false;
		// location
		lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		// get system last known location
		Location locN = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		Location locG = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		/*if(locG==null) makeToast("GPF");
		else {
			String msg = "Location Changed: " + locG.getProvider() + 
					" " + locG.getLatitude() + " " + locG.getLongitude();
			makeToast(msg);
		}*/
		if (locN != null) {
			if (location == null) {
				location = locN;
			} else if (location.getProvider().equals(LocationManager.NETWORK_PROVIDER) 
					&& location.getTime() < locN.getTime()) {
				location = locN;
			} else if (location.getProvider().equals(LocationManager.GPS_PROVIDER)
					&& location.getTime() + 5 * 60 * 1000 < locN.getTime()) {
				location = locN;
			}
		}
		if (locG != null) {
			if (location == null) {
				location = locG;
			} else if (location.getProvider().equals(LocationManager.GPS_PROVIDER)
					&& location.getTime() < locG.getTime()) {
				location = locG;
			} else if (location.getProvider().equals(LocationManager.NETWORK_PROVIDER)
					&& location.getTime() - 5 * 60 * 1000 < locG.getTime()) {
				location = locG;
			}
		}
		// remove all previous location update requests
		lm.removeUpdates(locListener);
		// request for location updates
		lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60 * 1000, 10, locListener);
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60 * 1000, 10, locListener);
		
		// refresh canteen list
		if (canteens != null) {
			Collections.sort(canteens, new CanteenDistanceComparator(location));
			resetCanteenList();
		}
		// periodically update text of refresh button
		final Handler locHandler = new Handler();
		final Runnable locRunnable = new Runnable(){
			public void run(){
				if(isInBackground||locListening) return;
				if(shouldGetGPS()){
					if(location==null){
						makeToast("1");
						Location locN = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
						Location locG = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
						makeToast("2");
						if(locN==null && locG==null) makeToast("Both Error");
						if (locN != null) {
							if (location == null) {
								location = locN;
							} else if (location.getProvider().equals(LocationManager.NETWORK_PROVIDER) 
									&& location.getTime() < locN.getTime()) {
								location = locN;
							} else if (location.getProvider().equals(LocationManager.GPS_PROVIDER)
									&& location.getTime() + 5 * 60 * 1000 < locN.getTime()) {
								location = locN;
							}
						}
						makeToast("3");
						if (locG != null) {
							if (location == null) {
								location = locG;
							} else if (location.getProvider().equals(LocationManager.GPS_PROVIDER)
									&& location.getTime() < locG.getTime()) {
								location = locG;
							} else if (location.getProvider().equals(LocationManager.NETWORK_PROVIDER)
									&& location.getTime() - 5 * 60 * 1000 < locG.getTime()) {
								location = locG;
							}
						}
						makeToast("4");
					}
					reportLoc();
				}
				locHandler.postDelayed(this, GPSInterval);
				return;
			}
		};
		locRunnable.run();
		final Handler mHandler = new Handler();
		final Runnable mRunnable = new Runnable() {
		   @Override
		   public void run() {
			   if (isInBackground) {
				   return;
			   }
			   if (lastRefreshDate != null) {
				   long t = (new Date().getTime() - lastRefreshDate.getTime()) / (60 * 1000);
				   String tstr;
					if (t == 0) {
						tstr = "刚刚更新";
					} else if (0 < t && t < 60) {
						tstr = t + "分钟前更新";
					} else if (60 <= t && t < 24 * 60) {
						tstr = t/60 + "小时前更新";
					} else {
						tstr = "很久之前更新";
					}
					refreshButton.setText("刷新("+tstr+")");
				}
			  mHandler.postDelayed(this, 60 * 1000);
		   }
		};
		mRunnable.run();
	}

	private boolean shouldGetGPS(){
		SimpleDateFormat sDateFormat = new SimpleDateFormat("HHmmss");     
	    String date = sDateFormat.format(new java.util.Date());
	    int time = Integer.valueOf(date);
	    if( (noon[0] < time && time < noon[1]) || night[0] < time && time < night[1])
	    	return true;
	    return false;
	}
	protected void onStop() { 
		super.onStop();
		
		isInBackground = true;
		
		// stop listening to location updates LOCATION_TRACKING_TIME after going background
		Looper myLooper = Looper.myLooper();
		final Handler myHandler = new Handler(myLooper);
		myHandler.postDelayed(new Runnable() {
			public void run() {
				lm.removeUpdates(locListener);
			}
		}, LOCATION_TRACKING_TIME);
	}
	
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(reportFinishedReceiver);
	}
	
	@Override
	protected void onResume() {
		
		super.onResume();
		
		// custom action bar
		ActionBar bar = getSupportActionBar();
		bar.setBackgroundDrawable(getResources().getDrawable(R.color.action_bar_background));
		bar.setDisplayShowTitleEnabled(false);
		bar.setDisplayShowCustomEnabled(true);
		bar.setDisplayShowHomeEnabled(false);
		
		// set UI differently for logged in or not logged in
		
		// if already logged in
		if (sp.getBoolean("isLogin", false)) {	
			// set action bar
			View v = getLayoutInflater().inflate(R.layout.log_actionbar, null);
			bar.setCustomView(v);
			
			// set buttons of action bar
			accountButton = (Button)findViewById(R.id.but_uname);
			avatarButton = (ImageButton)findViewById(R.id.but_avatar);
			String uname = sp.getString("uname", "");
			accountButton.setText(uname);
			accountButton.setOnClickListener(new Button.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(MainActivity.this, UserInfoActivity.class);
					startActivity(intent);
				}
			});
			avatarButton.setOnClickListener(new ImageButton.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(MainActivity.this, UserInfoActivity.class);
					startActivity(intent);
				}
			});
			
			// set user avatar
			if (sp.getString("avatarOf", "").equals(uname)) {
				File imgDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
				File imgFile = new File(imgDir, "avatar.jpg");
				
				int orientation = 0;
				try{
					ExifInterface exif = new ExifInterface(imgFile.getPath());
					orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
				} catch (Exception e){
					Log.e(LOG_TAG, e.getMessage());
				}
				float rotate = 0;
				switch (orientation){
				  case 6: rotate = 90; break;
				  case 3: rotate = 180; break;
				  case 8: rotate = 270; break;
				}
				Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getPath());
				
				Matrix matrix = new Matrix();
				matrix.postRotate(rotate);
				bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), 
						bitmap.getHeight(), matrix, true);
				avatarButton.setImageBitmap(bitmap);
			} else {
				avatarButton.setImageResource(R.drawable.avatar);
			}
		} 
		
		// if not logged in
		else {	
			// set action bar
			View v = getLayoutInflater().inflate(R.layout.not_log_actionbar, null);
			bar.setCustomView(v);
			
			// set buttons of action bar
			avatarButton = (ImageButton)findViewById(R.id.but_avatar);
			avatarButton.setImageResource(R.drawable.avatar);
			accountButton = (Button)findViewById(R.id.but_uname);
			accountButton.setOnClickListener(new Button.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(MainActivity.this, UserInfoActivity.class);
					startActivity(intent);
				}
			});
			avatarButton.setOnClickListener(new ImageButton.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(MainActivity.this, UserInfoActivity.class);
					startActivity(intent);
				}
			});
			
			// use IMEI as user identifier
			// TODO can't get IMEI on some devices, should use other identifier
			String imei = ((TelephonyManager)getSystemService(TELEPHONY_SERVICE)).getDeviceId();
			sp.edit().putString("imei", imei).commit();
		}
		
		// set common UI
		reportButton = (ImageButton)findViewById(R.id.but_new_report);
		reportButton.setOnClickListener(new ImageButton.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, ReportActivity.class);
				intent.putExtra("cat_id", -1);
				intent.putExtra("reward", reward);
				startActivity(intent);
			}
		});
		rewardText = (TextView)findViewById(R.id.txt_reward);
		helpButton = (ImageButton)findViewById(R.id.but_help);
		helpButton.setOnClickListener(new ImageButton.OnClickListener() {
			@Override
			public void onClick(View v) {
				makeToast(getString(R.string.help_about_reward));
			}
		});
		
		// get current reward for reporting from server
		getReward();
	}
	
	/**
	 * Display a long-time toast.
	 * @param message Message of toast.
	 */
	private void makeToast(String message) {
		Toast toast = Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG);
		toast.show();
	}
	private void reportLoc(){
		//if(location==null) makeToast("Location Error");
		SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");     
	    final String date = sDateFormat.format(new java.util.Date());
		final Map<String, String> reqParas = new HashMap<String, String>();
		reqParas.put("t", "rpl");
		if (!sp.getBoolean("isLogin", false)) {
			reqParas.put("isu", "0");
			reqParas.put("nam", sp.getString("imei", ""));
		} else {
			reqParas.put("isu", "1");
			reqParas.put("nam", sp.getString("uname", ""));
		}
		if(location!=null){
			reqParas.put("lng", String.valueOf(location.getLongitude()));
			reqParas.put("lat", String.valueOf(location.getLatitude()));
			reqParas.put("provider", location.getProvider());
		} else {
			reqParas.put("provider", "Error");
			ConnectivityManager cm = (ConnectivityManager)(getApplicationContext().getSystemService (Context.CONNECTIVITY_SERVICE));  
			NetworkInfo network = null;
			if(cm!=null) network = cm.getActiveNetworkInfo();  
			else System.err.println("dman");
	        if (network != null) reqParas.put("NetStat", String.valueOf(network.isAvailable()));
	        else reqParas.put("NetStat", "false");
	        reqParas.put("GPSProviderStat", String.valueOf(lm.isProviderEnabled(LocationManager.GPS_PROVIDER)));
	        reqParas.put("NETProviderStat", String.valueOf(lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)));
		}
		final String url = getString(R.string.server_url);
		Runnable runnable = new Runnable(){
			public void run() {
				int status;
				try {
					Map<String,String> resParas = HttpManager.postKV(url, reqParas);
					status = Integer.valueOf(resParas.get("stat"));
					makeToast("GPS+"+status);
				} catch (HttpManagerException e) {
					status = -2;
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				/*int GPSPostStatus = sp.getInt("GPSPosted", 0);
				if(status==-2){
					String msg = "";
					if(location!=null){
						msg = "Location Changed: " + location.getProvider() + " " + location.getLatitude() + " " + location.getLongitude();
					}
					if(GPSPostStatus==-2){
						try {
							sd.createSDFile(count+GPSFile);
							sd.writeSDFile(msg+" "+date, count+GPSFile);
							count++;
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else {
						//sd.deleteSDFile(GPSFile);
						FileOutputStream ops = null;
						try {
							sd.createSDFile(GPSFile);
							ops = openFileOutput(GPSFile,Context.MODE_APPEND);
							PrintStream ps = new PrintStream(ops);
							ps.println(msg+" "+date);
							ps.close();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} finally {
								try {
									if(ops!=null) ops.close();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
						}
					}
				}*/
				sp.edit().putInt("GPSPosted", status).commit();
			}
		};
		runnable.run();
	}
	/**
	 * Make a check-version request.
	 */
	private void checkVersion() {
		
		// set HTTP post parameters
		final Map<String, String> reqParas = new HashMap<String, String>();
		reqParas.put("t", "ckv");
		if (!sp.getBoolean("isLogin", false)) {
			reqParas.put("isu", "0");
			reqParas.put("nam", sp.getString("imei", ""));
		} else {
			reqParas.put("isu", "1");
			reqParas.put("nam", sp.getString("uname", ""));
		}
		reqParas.put("mid", String.valueOf(sp.getInt("mid", 0)));
		final String url = getString(R.string.server_url);
		
		// do POST in new thread
	    Runnable runnable = new Runnable() {
	    	@Override
			public void run() {
				int status;
				int version = -1;
				boolean hasNewMessage = false;
				String link = "";
				try {
					Map<String, String> resParas = HttpManager.postKV(url, reqParas);
					version = Integer.valueOf(resParas.get("version"));
					hasNewMessage = Boolean.valueOf(resParas.get("hasNewMessage"));
					link = resParas.get("link");
					status = 0; // success
				} catch (Exception e) {
					status = -2; // network error
					Log.e(LOG_TAG, e.getMessage());
				}				
				Message msg = new Message();
				Bundle data = new Bundle();
				data.putInt("status", status);
				data.putInt("version", version);
				data.putBoolean("hasNewMessage", hasNewMessage);
				data.putString("link", link);
				msg.setData(data);
				checkVersionHandler.sendMessage(msg);
			}
	    };
	    new Thread(runnable).start();   
	}
	
	/**
	 * Handle response of check-version request.
	 */
	Handler checkVersionHandler = new Handler() {
		
		@Override
    	public void handleMessage(Message msg) {
			
    		super.handleMessage(msg);
    		Bundle data = msg.getData();
    		
    		/*
    		 * check status
			 * 0 - success
			 * -2 - network error
			 */
    		int status = data.getInt("status");
			switch (status) {
			  case 0: 
				int version = data.getInt("version");
				boolean hasNewMessage = data.getBoolean("hasNewMessage");
				final String link = data.getString("link");
				// if there is a new version
				if (version > CLIENT_VERSION) {
					AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
					builder.setMessage(R.string.update_msg)
						.setTitle(getString(R.string.found_update));
					builder.setPositiveButton(R.string.update_now, new DialogInterface.OnClickListener() {	
							@Override
							public void onClick(DialogInterface dialog, int which) {
								Intent intent = new Intent("android.intent.action.VIEW", 
										Uri.parse(link));
								startActivity(intent);
							}
						});
					builder.setNegativeButton(R.string.update_later, null);
					AlertDialog dialog = builder.create();
					dialog.show();
				}
				if(hasNewMessage){
					sp.edit().putBoolean("hasNewMessage", true).commit();
				}else{
					sp.edit().putBoolean("hasNewMessage", false).commit();
				}
				break;
			  case -2: 
				Log.e(LOG_TAG, "Failed checking for update due to network error.");
				break;
			}
    	}
	};
	
	/**
	 * Make a get-reward request.
	 */
	private void getReward() {
		if (reward != -1) {
			rewardText.setText("上传奖励"+reward+"分");
			return;
		}
		
		// set HTTP post parameters
		final Map<String, String> reqParas = new HashMap<String, String>();
		reqParas.put("t", "gr");
		final String url = getString(R.string.server_url);
		
		// do POST in new thread
	    Runnable runnable = new Runnable(){
	    	@Override
			public void run() {
				int status;
				int reward = -1;
				int fbReward = 20;
				long timeout = -1;
				try {
					Map<String, String> resParas = HttpManager.postKV(url, reqParas);
					reward = Integer.valueOf(resParas.get("reward"));
					fbReward = Integer.valueOf(resParas.get("fbReward"));
					sp.edit().putInt("fbReward", fbReward).commit();
					timeout = Integer.valueOf(resParas.get("timeout"));
					status = 0; // success
				} catch (Exception e) {
					status = -2; // network error
					Log.e(LOG_TAG, e.getMessage());
				}				
				Message msg = new Message();
				Bundle data = new Bundle();
				data.putInt("status", status);
				data.putInt("reward", reward);
				data.putLong("timeout", timeout);
				msg.setData(data);
				getRewardHandler.sendMessage(msg);
			}
	    };
	    new Thread(runnable).start();
	}
	
	/**
	 * Handle response of get-reward request.
	 */
	Handler getRewardHandler = new Handler() {
		
		@Override
    	public void handleMessage(Message msg) {
			
    		super.handleMessage(msg);
    		Bundle data = msg.getData();
    		
    		/*
    		 * check status
			 * 0 - success
			 * -2 - network error
			 */
    		int status = data.getInt("status");
			switch (status) {
			  case 0: 
				reward = data.getInt("reward");
				rewardText.setText("上传奖励"+reward+"分");
				long timeout = data.getLong("timeout");
				// request again after timeout
				Looper myLooper = Looper.myLooper();
				final Handler myHandler = new Handler(myLooper);
				myHandler.postDelayed(new Runnable() {
					public void run() {
						reward = -1;
						rewardText.setText("");
						if (!isInBackground) {
							getReward();
						}
					}
				}, (timeout + 5) * 1000);
				break;
			  case -2: 
				Log.e(LOG_TAG, "Failed checking for update due to network error.");
				break;
			}
    	}
	};
	
	/**
	 * Make a get-canteen-list request.
	 */
	private void getCanteenList() {
		progressBar.setVisibility(View.VISIBLE);
		
		// set HTTP post parameters
		final Map<String, String> reqParas = new HashMap<String, String>();
		reqParas.put("t", "gcl");
		if (!sp.getBoolean("isLogin", false)) {
			reqParas.put("isu", "0");
			reqParas.put("nam", sp.getString("imei", ""));
		} else {
			reqParas.put("isu", "1");
			reqParas.put("nam", sp.getString("uname", ""));
		}
		final String url = getString(R.string.server_url);
		
		// do POST in new thread
	    Runnable runnable = new Runnable() {
	    	@Override
			public void run() {
				int status;
				String json = "";
				try {
					Map<String, String> resParas = HttpManager.postKV(url, reqParas);
					status = Integer.parseInt(resParas.get("stat"));
					json = resParas.get("json");
				} catch (Exception e) {
					status = -2; // network error
					Log.e(LOG_TAG, e.getMessage());
				}				
				Message msg = new Message();
				Bundle data = new Bundle();
				data.putInt("status", status);
				data.putString("json", json);
				msg.setData(data);
				getCanteenListHandler.sendMessage(msg);
			}
	    };
	    new Thread(runnable).start();
	}
	
	/**
	 * Handle response of get-canteen-list request.
	 */
	Handler getCanteenListHandler = new Handler() {
		@Override
    	public void handleMessage(Message msg){
			
    		super.handleMessage(msg);
    		Bundle data = msg.getData();

    		/*
    		 * check status
			 * 0 - success
			 * -1 - server error
			 * -2 - network error
			 */
    		int status = data.getInt("status");
			switch (status) {
			  case 0: 
				lastRefreshDate = new Date();
				refreshButton.setText("刷新(刚刚更新)");
				canteens = new ArrayList<Canteen>();
				String json = data.getString("json");
				try {
					JSONArray ja = new JSONArray(json);
					for (int i=0; i<ja.length(); i++) {
						JSONObject jo = ja.getJSONObject(i);
						Canteen c = new Canteen();
						c.id = jo.getInt("id");
						c.lat = jo.getDouble("lat");
						c.lng = jo.getDouble("lng");
						c.title = jo.getString("title");
						c.level = jo.getDouble("level");
						c.comment = jo.getString("comment");
						c.time = jo.getLong("time");
						c.iid = jo.getInt("iid");
						c.reward = jo.getInt("reward");
						c.fb = jo.getInt("fb");
						canteens.add(c);
					}
					
					// sort canteens by level and location
					Location loc = null;
					if (location != null && 
							location.getTime() + 5 * 60 * 1000> new Date().getTime()) {
						loc = location;
					}
					Collections.sort(canteens, new CanteenDistanceComparator(loc));
					resetCanteenList();
				} catch (JSONException e) {
					makeToast(getString(R.string.cannot_get_canteen_list));
					Log.e(LOG_TAG, e.getMessage());
				}
				break;
			  case -1:
				makeToast(getString(R.string.cannot_get_canteen_list));
				Log.e(LOG_TAG, "Failed to get canteen list due to server error");
				break;
			  case -2: 
				makeToast(getString(R.string.cannot_get_canteen_list)+ "2");
				Log.e(LOG_TAG, "Failed to get canteen list due to network error");
				break;
			}
			progressBar.setVisibility(View.GONE);
		}
	};
	
	/**
	 * Reset canteen list with canteens information
	 */
	private void resetCanteenList(){
		listData = new ArrayList<Map<String, Object>>();
		for (int i = 0; i < canteens.size(); i++) {
			Canteen c = canteens.get(i);
			Map<String, Object> map = new HashMap<String, Object>();
			
			map.put("canteen_title", c.title);
			
			if (c.level < 0) {
				map.put("canteen_lv_rate", 0);
				map.put("canteen_level", getString(R.string.canteen_level_unknown));
				
			} else {
				map.put("canteen_lv_rate", c.level);
				switch ((int)c.level) {
				  case 5:
					map.put("canteen_level", getString(R.string.canteen_level_5)); break;
				  case 4:
					map.put("canteen_level", getString(R.string.canteen_level_4)); break;
				  case 3:
					map.put("canteen_level", getString(R.string.canteen_level_3)); break;
				  case 2:
					map.put("canteen_level", getString(R.string.canteen_level_2)); break;
				  case 1:
					map.put("canteen_level", getString(R.string.canteen_level_1)); break;
				  case 0:
					map.put("canteen_level", getString(R.string.canteen_level_0)); break;
				}
			}
			map.put("canteen_comment", c.comment);
			String reward = "现在上传可获" + c.reward + "奖励分";
			map.put("canteen_reward", reward);
			listData.add(map);
			getCanteenImg(c.id);;
		}
		
		MySimpleAdapter adapter = new MySimpleAdapter(MainActivity.this, 
				listData, R.layout.canteen_list, listFrom, listTo);
		canteenList.setAdapter(adapter);
	}

	/**
	 * Make a get-canteen-image request.
	 * @param id Canteen id.
	 */
	private void getCanteenImg(int id) {
		
		// set HTTP post parameters
		final Map<String, String> reqParas = new HashMap<String, String>();
		reqParas.put("t", "gcp");
		reqParas.put("id", String.valueOf(id));
		final String url = getString(R.string.server_url);
		final int _id = id;
		
		// do POST in new thread
	    Runnable runnable = new Runnable() {
	    	@Override
			public void run() {
				int status;
				byte[] cp = new byte[1];
				try {
					cp = HttpManager.postForFile(url, reqParas);
					status = 0; // success
				} catch (Exception e) {
					status = -2; // network error
					Log.e(LOG_TAG, e.getMessage());
				}				
				Message msg = new Message();
				Bundle data = new Bundle();
				data.putInt("status", status);
				data.putInt("id", _id);
				data.putByteArray("cp", cp);
				msg.setData(data);
				getCanteenImgHandler.sendMessage(msg);
			}
	    };
	    new Thread(runnable).start();
	}
	
	/**
	 * Handle response of get-canteen-image request.
	 */
	Handler getCanteenImgHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			
			super.handleMessage(msg);
    		Bundle data = msg.getData();
    		
    		/*
    		 * check status
			 * 0 - success
			 * -2 - network error
			 */
    		int status = data.getInt("status");
			switch (status) {
			  case 0: 
				int id = data.getInt("id");
				byte[] cp = data.getByteArray("cp");
				
				File f = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "canteen_"+id+".jpg");
				try {
					FileOutputStream fos = new FileOutputStream(f);
					fos.write(cp);
					fos.close();
				} catch (IOException e) {
					Log.e(LOG_TAG, e.getMessage());
					return;
				}
				
				Bitmap bitmap = BitmapFactory.decodeFile(f.getPath());
				
				try {
					int orientation;
					ExifInterface exif = new ExifInterface(f.getPath());
					orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
					float rotate = 0;
					switch (orientation){
					case 6: rotate = 90; break;
					case 3: rotate = 180; break;
					case 8: rotate = 270; break;
					}
					Matrix matrix = new Matrix();
					matrix.postRotate(rotate);
					bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), 
							bitmap.getHeight(), matrix, true);
				} catch (Exception e) {
					Log.e(LOG_TAG, e.getMessage());
				}
				
				for (int i = 0; i < canteens.size(); i++) {
					if (canteens.get(i).id == id) {
						canteens.get(i).bitmap = bitmap;
						if (listData.get(i).containsKey("canteen_img")) {
							listData.get(i).remove("canteen_img");
						}
						listData.get(i).put("canteen_img", bitmap);
						((MySimpleAdapter)canteenList.getAdapter()).notifyDataSetChanged();
						break;
					}
				}
				break;
			  case -2: 
				Log.e(LOG_TAG, "Failed to get new avatar");
				break;
			}
		}
	};
	
}
