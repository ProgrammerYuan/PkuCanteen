package edu.pku.sxt.pkuc.client.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.media.ExifInterface;
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
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.HeaderViewListAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import edu.pku.sxt.pkuc.client.Canteen;
import edu.pku.sxt.pkuc.client.R;
import edu.pku.sxt.pkuc.client.util.HttpManager;
import edu.pku.sxt.pkuc.client.util.HttpManagerException;

/**
 * Display detail information of a certain canteen.
 * @author songxintong
 *
 */
public class DetailActivity extends ActionBarActivity {
	
	private static final String LOG_TAG = "DetailActivity";
	
	// Application Scope Variables
	public static boolean isInBackground;
	
	// UI Views
	Button accountButton;
	ImageButton avatarButton;
	ImageButton reportButton;
	ProgressBar progressCom, progressPic;
	ListView commentList;
	GridView pictureGrid;
	DetailListHeader dlh;
	ImageButton helpButton;
	TextView rewardText;
	
	// commentList adapter parameters
	final String[] commentFrom = new String[]{"comment_uimg", "comment_uname", "comment_ulv",
			"comment_time", "comment_level", "comment_lv_rate", "comment_comment"};
	final int[] commentTo = new int[]{R.id.comment_uimg, R.id.comment_uname, R.id.comment_ulv,
			R.id.comment_time, R.id.comment_level, R.id.comment_lv_rate, R.id.comment_comment};
	
	// pictureGrid adapter parameters
	final String[] pictureFrom = new String[]{"picture_img", "picture_uname", "picture_ulv", "picture_time"};
	final int[] pictureTo = new int[]{R.id.picture_img, R.id.picture_uname, R.id.picture_ulv, R.id.picture_time};
	
	final int[] lv_img = UserInfoActivity.lv_img; // user level image id
	final int[] lv_score =  UserInfoActivity.lv_score; // user level score

	// shared preferences
	SharedPreferences sp;
	
	// data
	Canteen c; // displayed canteen
	int id; // displayed canteen's id
	Bitmap bitmapCP; // displayed canteen's image
	
	List<Map<String, Object>> comments; // data of comments list
	public static List<Map<String, Object>> pictures; // data of pictures grid
	public static Map<Integer, Bitmap> thumbs; // thumbnail of pictures
	Date earliestCommentDate = new Date(0); // date of the earliest comment displayed
	Date earliestPictureDate = new Date(0); // date of the earliest picture displayed
	Map<String, Bitmap> avatars; // user avatars
	
	static int reward = -1; // current reward for reporting

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_detail);
		
		// set UI
		progressCom = (ProgressBar)findViewById(R.id.progressBar_comment);
		progressPic = (ProgressBar)findViewById(R.id.progressBar_picture);
		TabHost tabhost = (TabHost)findViewById(android.R.id.tabhost);
		tabhost.setup();
		tabhost.addTab(tabhost.newTabSpec("comments").setIndicator(getString(R.string.comments)).setContent(R.id.tab_comments));
		tabhost.addTab(tabhost.newTabSpec("pictures").setIndicator(getString(R.string.pictures)).setContent(R.id.tab_pictures));
		commentList = (ListView)findViewById(R.id.list_comment);
		pictureGrid = (GridView)findViewById(R.id.grid_pictures);
		dlh = new DetailListHeader(this);
		commentList.addHeaderView(dlh, null, false);
		commentList.setOnScrollListener(new ListView.OnScrollListener() {
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {}
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				if (scrollState == SCROLL_STATE_IDLE) {
					if (view.getLastVisiblePosition() == (view.getCount()-1)) {
						getComments();
					}
				}
			}
		});
		pictureGrid.setOnScrollListener(new GridView.OnScrollListener() {
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {}
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				if (scrollState == SCROLL_STATE_IDLE){
					if (view.getLastVisiblePosition() == (view.getCount()-1)) {
						getPicturesList();
					}
				}
			}
		});
		pictureGrid.setOnItemClickListener(new GridView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				Intent intent = new Intent(DetailActivity.this, PictureActivity.class);
				int rid =  (Integer)pictures.get(position).get("rid");
				intent.putExtra("pos", position);
				intent.putExtra("rid", rid);
				startActivity(intent);
			}
		});
		
		// get shared preferences
		sp = getSharedPreferences(getString(R.string.shared_preferences_name), Activity.MODE_PRIVATE);
		
		// get displayed canteen and set related UI
		Bundle b = getIntent().getExtras();
		id = b.getInt("id"); 
		readSummary(id);
		List<Canteen> canteens = MainActivity.canteens;
		for(int i=0; i<canteens.size(); i++) {
			if(canteens.get(i).id == id) {
				c = canteens.get(i);
				break;
			}
		}
		bitmapCP = c.bitmap;
		dlh.canteenImg.setImageBitmap(bitmapCP);
		dlh.canteenTitle.setText(c.title);
		if (c.level < 0) {
			dlh.canteenLvRate.setRating(0);
			dlh.canteenLevel.setText(getString(R.string.canteen_level_unknown));
		} else {
			dlh.canteenLvRate.setRating((float)c.level);
			switch ((int)c.level) {
			  case 5:
				dlh.canteenLevel.setText(getString(R.string.canteen_level_5)); break;
			  case 4:
				dlh.canteenLevel.setText(getString(R.string.canteen_level_4)); break;
			  case 3:
				dlh.canteenLevel.setText(getString(R.string.canteen_level_3)); break;
			  case 2:
				dlh.canteenLevel.setText(getString(R.string.canteen_level_2)); break;
			  case 1:
				dlh.canteenLevel.setText(getString(R.string.canteen_level_1)); break;
			  case 0:
				dlh.canteenLevel.setText(getString(R.string.canteen_level_0)); break;
			}
		}
		
		// set user feedback button
		int gimg = 0, bimg = 0;	// feedback button image id
		boolean gen = true, ben = true; // feedback button enabled
		switch (c.fb) {
		  case 0:
			gimg = R.drawable.thumb_up_white;
			bimg = R.drawable.thumb_down_white;
			gen = true;
			ben = true;
			break;
		  case 1:
			gimg = R.drawable.thumb_up_blue;
			bimg = R.drawable.thumb_down_white;
			gen = false;
			ben = true;
			break;
		  case -1:
			gimg = R.drawable.thumb_up_white;
			bimg = R.drawable.thumb_down_blue;
			gen = true;
			ben = false;
			break;
		}
		if (c.iid == -1) {
			gen = false;
			ben = false;
		}
		ImageButton.OnClickListener listener = new ImageButton.OnClickListener() {
			@Override
			public void onClick(View v) {
				View p = (View)v.getParent();
				ImageButton ibg = (ImageButton)p.findViewById(R.id.canteen_ib_good);
				ImageButton ibb = (ImageButton)p.findViewById(R.id.canteen_ib_bad);
				switch (v.getId()) {
				  case R.id.canteen_ib_good:
					c.fb = 1;
					ibg.setImageResource(R.drawable.thumb_up_blue);
					ibg.setEnabled(false);
					ibb.setImageResource(R.drawable.thumb_down_white);
					ibb.setEnabled(true);
					sendFeedback(c.iid, c.fb);
					break;
				  case R.id.canteen_ib_bad:
					c.fb = -1;
					ibg.setImageResource(R.drawable.thumb_up_white);
					ibg.setEnabled(true);
					ibb.setImageResource(R.drawable.thumb_down_blue);
					ibb.setEnabled(false);
					sendFeedback(c.iid, c.fb);
					break;
				}
			}
		};
		dlh.ibb.setImageResource(bimg);
		dlh.ibg.setImageResource(gimg);
		dlh.ibb.setEnabled(ben);
		dlh.ibg.setEnabled(gen);
		dlh.ibb.setOnClickListener(listener);
		dlh.ibg.setOnClickListener(listener);
		
		// initialize data
		avatars = new HashMap<String, Bitmap>();
		comments = new ArrayList<Map<String, Object>>();
		pictures = new ArrayList<Map<String, Object>>();
		thumbs = new HashMap<Integer, Bitmap>();
		
		// set comments list and picture grid
		MySimpleAdapter adapterC = new MySimpleAdapter(DetailActivity.this, 
				comments, R.layout.comment_list, commentFrom, commentTo){
			@Override public boolean isEnabled(int position){
				return false;
			}
		};
		commentList.setAdapter(adapterC);
		MySimpleAdapter adapterP = new MySimpleAdapter(DetailActivity.this, 
				pictures, R.layout.picture_grid, pictureFrom, pictureTo);
		pictureGrid.setAdapter(adapterP);
		
		// start progress bar
		progressCom.setVisibility(View.VISIBLE);
		progressPic.setVisibility(View.VISIBLE);
		
		// get data from server
		getReward();
		getComments();
		getPicturesList();
	}

	protected void onStart() {
		super.onStart();
		isInBackground = false;
	}
	
	protected void onStop() {
		super.onStop();
		isInBackground = true;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		// custom action bar
		ActionBar bar = getSupportActionBar();
		bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#5CACEE")));
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
					Intent intent = new Intent(DetailActivity.this, UserInfoActivity.class);
					startActivity(intent);
				}
			});
			avatarButton.setOnClickListener(new ImageButton.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(DetailActivity.this, UserInfoActivity.class);
					startActivity(intent);
				}
			});
			
			// set user avatar
			if (sp.getString("avatarOf", "").equals(uname)) {
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
					Intent intent = new Intent(DetailActivity.this, UserInfoActivity.class);
					startActivity(intent);
				}
			});
			avatarButton.setOnClickListener(new ImageButton.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(DetailActivity.this, UserInfoActivity.class);
					startActivity(intent);
				}
			});

			// get IMEI
			// TODO can't get IMEI on some devices, should use other identifier
			String imei = ((TelephonyManager)getSystemService(TELEPHONY_SERVICE)).getDeviceId();
			sp.edit().putString("uname", imei);
		}
		
		// set common UI
		reportButton = (ImageButton)findViewById(R.id.but_new_report);
		reportButton.setOnClickListener(new ImageButton.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(DetailActivity.this, ReportActivity.class);
				intent.putExtra("cat_id", id);
				intent.putExtra("reward", reward);
				startActivityForResult(intent, 0);
			}
		});
		rewardText = (TextView)findViewById(R.id.txt_reward);
		helpButton = (ImageButton)findViewById(R.id.but_help);
		helpButton.setOnClickListener(new ImageButton.OnClickListener(){
			@Override
			public void onClick(View v) {
				makeToast(getString(R.string.help_about_reward));
			}
		});
		
		// get current reward for reporting from server
		getReward();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) { finish(); }
	
	/**
	 * Display a long-time toast.
	 * @param message Message of toast.
	 */
    private void makeToast(String message) {
    	Toast toast = Toast.makeText(DetailActivity.this, message, Toast.LENGTH_LONG);
    	toast.show();
    }

	/**
	 * Make a get-comments request.
	 */
    private void getComments() {
    	if (earliestCommentDate == null) // no earlier comments on server
    		return;
    	// set http post parameters
		final Map<String, String> reqParas = new HashMap<String, String>();
		reqParas.put("t", "gcm");
		reqParas.put("id", String.valueOf(id));
		reqParas.put("time", String.valueOf(earliestCommentDate.getTime()));
		boolean isu = sp.getBoolean("isLogin", false);
		if (isu) {
			reqParas.put("isu", "1");
			reqParas.put("nam", sp.getString("uname", ""));
		} else {
			reqParas.put("isu", "0");
			reqParas.put("nam", sp.getString("imei", ""));
		}
		final String url = getString(R.string.server_url);
		
		// do post in new thread
	    Runnable runnable = new Runnable() {
	    	@Override
			public void run() {
				int status;
				String json = "";
				long time = 0;
				try {
					Map<String, String> resParas = HttpManager.postKV(url, reqParas);
					status = Integer.parseInt(resParas.get("stat"));
					json = resParas.get("json");
					time = Long.parseLong(resParas.get("time"));
				} catch (Exception e) {
					status = -2; // network error
					Log.e(LOG_TAG, e.getMessage());
				}				
				Message msg = new Message();
				Bundle data = new Bundle();
				data.putInt("status", status);
				data.putString("json", json);
				data.putLong("time", time);
				msg.setData(data);
				getCommentsHandler.sendMessage(msg);
			}
	    };
	    new Thread(runnable).start();
    }
    
    /**
	 * Handle response of get-comments request.
	 */
    Handler getCommentsHandler = new Handler() {
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
				String json = data.getString("json");
				long time = data.getLong("time");
				Date now = new Date();
				try {
					JSONArray ja = new JSONArray(json);
					for (int i = 0; i < ja.length(); i++) {
						JSONObject jo = ja.getJSONObject(i);
						String uname = jo.getString("nam");
						long t = (now.getTime() - jo.getLong("tim")) / (60 * 1000);
						String tstr;
						if (t == 0) {
							tstr = "刚刚更新";
						} else if (0 < t && t < 60) {
							tstr = t + "分钟前";
						} else if (60 <= t && t < 24 * 60) {
							tstr = t/60 + "小时前";
						} else {
							tstr = "很久之前";
						}
						String comment = jo.getString("com");
						double level = jo.getDouble("lv");
						int score = jo.getInt("uct") + jo.getInt("ufb");
						boolean isu = jo.getBoolean("isu");
						
						Map<String, Object> map = new HashMap<String, Object>();
						int x;
						for (x = 0; x < lv_score.length; x++) {
							if(score < lv_score[x])
								break;
						}
						if (x == lv_score.length)
							x -= 1;
						map.put("comment_ulv", lv_img[x]);
						
						if (!isu) { 
							if (!sp.getBoolean("isLogin", false) && uname.equals(sp.getString("imei", ""))) {
								map.put("comment_uimg", R.drawable.avatar);
								uname = getString(R.string.username_me);
							} else {
								map.put("comment_uimg", R.drawable.avatar);
								uname = getString(R.string.username_unknown);
							}
						} else if (!avatars.containsKey(uname)) {
							map.put("comment_uimg", R.drawable.avatar);
							getAvatar(uname);
						} else if (avatars.get(uname) == null) {
							map.put("comment_uimg", R.drawable.avatar);
						} else {
							map.put("comment_uimg", avatars.get(uname));
						}
						map.put("comment_uname", uname);
						map.put("comment_time", tstr);
						map.put("comment_comment", comment);
						map.put("comment_lv_rate", (float)level);
						String comment_level = "";
						switch((int)level) {
						  case 5:
							dlh.canteenLevel.setText(getString(R.string.canteen_level_5)); break;
						  case 4:
							dlh.canteenLevel.setText(getString(R.string.canteen_level_4)); break;
						  case 3:
							dlh.canteenLevel.setText(getString(R.string.canteen_level_3)); break;
						  case 2:
							dlh.canteenLevel.setText(getString(R.string.canteen_level_2)); break;
						  case 1:
							dlh.canteenLevel.setText(getString(R.string.canteen_level_1)); break;
						  case 0:
							dlh.canteenLevel.setText(getString(R.string.canteen_level_0)); break;
						}
						map.put("comment_level", comment_level);
						
						comments.add(map);
					}
					earliestCommentDate = new Date(time);
					((MySimpleAdapter)((HeaderViewListAdapter)commentList.getAdapter()).getWrappedAdapter()).notifyDataSetChanged();
				} catch (JSONException e) {
					makeToast(getString(R.string.cannot_get_comments));
					Log.e(LOG_TAG, "Cannot get comments, json error.");
					Log.e(LOG_TAG, e.getMessage());
				}
				break;
			  case -1:
				  makeToast(getString(R.string.cannot_get_comments));
				  Log.e(LOG_TAG, "Cannot get comments, server error.");
				break;
			  case -2: 
				  makeToast(getString(R.string.cannot_get_comments));
				  Log.e(LOG_TAG, "Cannot get comments, network error.");
				break;
			}
			progressCom.setVisibility(View.GONE);
    	}
	};

	/**
	 * Make a get-picture-list request.
	 */
	private void getPicturesList() {
		if (earliestPictureDate == null) // no earlier pictures on server
    		return;
		// set http post parameters
		final Map<String, String> reqParas = new HashMap<String, String>();
		reqParas.put("t", "gpl");
		reqParas.put("id", String.valueOf(id));
		reqParas.put("time", String.valueOf(earliestPictureDate.getTime()));
		boolean isu = sp.getBoolean("isLogin", false);
		if (isu) {
			reqParas.put("isu", "1");
			reqParas.put("nam", sp.getString("uname", ""));
		} else {
			reqParas.put("isu", "0");
			reqParas.put("nam", sp.getString("imei", ""));
		}
		final String url = getString(R.string.server_url);
		
		// do post in new thread
	    Runnable runnable = new Runnable() {
	    	@Override
			public void run() {
				int status;
				String json = "";
				long time = 0;
				try {
					Map<String, String> resParas = HttpManager.postKV(url, reqParas);
					status = Integer.parseInt(resParas.get("stat"));
					json = resParas.get("json");
					time = Long.parseLong(resParas.get("time"));
				} catch (Exception e) {
					status = -2; // network error
					Log.e(LOG_TAG, e.getMessage());
				}				
				Message msg = new Message();
				Bundle data = new Bundle();
				data.putInt("status", status);
				data.putString("json", json);
				data.putLong("time", time);
				msg.setData(data);
				getPictureListHandler.sendMessage(msg);
			}
	    };
	    new Thread(runnable).start();
	}

	/**
	 * Handle response of get-picture-list request.
	 */
	Handler getPictureListHandler = new Handler() {
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
				String json = data.getString("json");
				long time = data.getLong("time");
				Date now = new Date();
				try {
					JSONArray ja = new JSONArray(json);
					for (int i = 0; i < ja.length(); i++) {
						JSONObject jo = ja.getJSONObject(i);
						String uname = jo.getString("nam");
						long t = (now.getTime() - jo.getLong("tim")) / (60 * 1000);
						String tstr;
						if (t == 0) {
							tstr = "刚刚更新";
						} else if (0 < t && t < 60) {
							tstr = t + "分钟前";
						} else if (60 <= t && t < 24 * 60) {
							tstr = t/60 + "小时前";
						} else {
							tstr = "很久之前";
						}
						int rid = jo.getInt("rid");
						boolean isu = jo.getBoolean("isu");
						
						Map<String, Object> map = new HashMap<String, Object>();
						if (!isu) {
							if(!sp.getBoolean("isLogin", false) && uname.equals(sp.getString("imei", ""))) {
								uname = getString(R.string.username_me);
							} else {
								uname = getString(R.string.username_unknown);
							}
						} else {
							int score = jo.getInt("uct") + jo.getInt("ufb");
							int x;
							for (x = 0; x < lv_score.length; x++) {
								if (score < lv_score[x])
									break;
							}
							if (x == lv_score.length)
								x -= 1;
							map.put("picture_ulv", lv_img[x]);
						}
						
						map.put("picture_img", R.drawable.photo);
						map.put("picture_uname", uname);
						map.put("picture_time", tstr);
						map.put("rid", rid);
						
						getThumb(rid, pictures.size());
						pictures.add(map);
					}
					earliestPictureDate = new Date(time);
					((MySimpleAdapter)pictureGrid.getAdapter()).notifyDataSetChanged();
				} catch (JSONException e) {
					makeToast(getString(R.string.cannot_get_pictures));
					Log.e(LOG_TAG, "Cannot get pictures, json error.");
					Log.e(LOG_TAG, e.getMessage());
				}
				break;
			  case -1:
				 makeToast(getString(R.string.cannot_get_pictures));
				 Log.e(LOG_TAG, "Cannot get pictures, server error.");
				break;
			  case -2: 
				makeToast(getString(R.string.cannot_get_pictures));
				Log.e(LOG_TAG, "Cannot get pictures, network error.");
				break;
			}
			progressPic.setVisibility(View.GONE);
		}
	};

	/**
	 * Make a send-feedback request.
	 * @param iid information id
	 * @param feedback
	 */
	private void sendFeedback(int iid, int feedback) {
		// set http post parameters
		final Map<String, String> reqParas = new HashMap<String, String>();
		reqParas.put("t", "sfb");
		reqParas.put("iid", String.valueOf(iid));
		reqParas.put("fb", String.valueOf(feedback));
		reqParas.put("reward", String.valueOf(sp.getInt("fbReward", 20)));
		boolean isu = sp.getBoolean("isLogin", false);
		if (isu) {
			reqParas.put("isu", "1");
			reqParas.put("nam", sp.getString("uname", ""));
		} else {
			reqParas.put("isu", "0");
			reqParas.put("nam", sp.getString("imei", ""));
		}
		final String url = getString(R.string.server_url);
		
		// do post in new thread
		Runnable runnable = new Runnable() {
	    	@Override
			public void run() {
				try {
					HttpManager.postKV(url, reqParas);
				} catch (Exception e) {
					Log.e(LOG_TAG, e.getMessage());
				}				
			}
	    };
	    new Thread(runnable).start();
	}
	
	/**
	 * Make a get-avatar request
	 * @param uname username
	 */
	private void getAvatar(String uname) {
		avatars.put(uname, null);
		
		// set http post parameters
		final Map<String, String> reqParas = new HashMap<String, String>();
		reqParas.put("t", "ava");
		reqParas.put("nam", uname);
    	final String name = uname;
		final String url = getString(R.string.server_url); 
		
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
				data.putString("uname", name);
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
			 * -1 - server error
			 * -2 - network error
			 */
			switch (status) {
			  case 0: 
				byte[] avatar = data.getByteArray("avatar");
				if(avatar.length<=0) return;
				String name = data.getString("uname");
				File f = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), name+"_ava.jpg");
				FileOutputStream fos;
				try {
					fos = new FileOutputStream(f);
					fos.write(avatar);
					fos.close();
				} catch (IOException e) {
					Log.e(LOG_TAG, e.getMessage());
					return;
				}
				int orientation = 0;
				try {
					ExifInterface exif = new ExifInterface(f.getPath());
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
				Bitmap bitmap = BitmapFactory.decodeFile(f.getPath());
				
				Matrix matrix = new Matrix();
				matrix.postRotate(rotate);
				bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), 
						bitmap.getHeight(), matrix, true);
				avatars.put(name, bitmap);
				for (int i = 0; i < comments.size(); i++) {
					Map<String, Object> map = comments.get(i);
					if (((String)map.get("comment_uname")).equals(name)) {
						map.put("comment_uimg", bitmap);
					}
				}
				((MySimpleAdapter)((HeaderViewListAdapter)commentList.getAdapter()).getWrappedAdapter()).notifyDataSetChanged();
				break;
			  case -1:
				Log.e(LOG_TAG, "Cannot get avatar, server error.");
				break;
			  case -2: 
				Log.e(LOG_TAG, "Cannot get avatar, network error.");
				break;
			}
    	}
	};
	private void readSummary(int id){
		final Map<String,String> reqParas = new HashMap<String,String>();
		reqParas.put("t", "rds");
		reqParas.put("id",String.valueOf(id));
		final String url = getString(R.string.server_url);
		Runnable runnable = new Runnable() {
			public void run(){
				int status;
				try {
					Map<String,String> resParas = HttpManager.postKV(url, reqParas);
					status = Integer.valueOf(resParas.get("stat"));
					System.err.println(status);
				} catch (HttpManagerException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		};
		new Thread(runnable).start();
	}
	/**
	 * Make a get thumbnail request.
	 * @param rid report id of picture
	 * @param pos position in picture grid
	 */
	private void getThumb(int rid, int pos) {
		// set http post parameters
		final Map<String, String> reqParas = new HashMap<String, String>();
		reqParas.put("t", "gth");
		reqParas.put("id", String.valueOf(rid));
		final int p = pos;
		final String url = getString(R.string.server_url); 
		
    	// do post in new thread
	    Runnable runnable = new Runnable() {
			@Override
			public void run() {
				int status;
				byte[] thumb = new byte[1];
				try {
					thumb = HttpManager.postForFile(url, reqParas);
					status = 0;
				} catch (Exception e) {
					status = -2; // network error
					Log.e(LOG_TAG, e.getMessage());
				}				
				Message msg = new Message();
				Bundle data = new Bundle();
				data.putInt("status", status);
				data.putByteArray("thumb", thumb);
				data.putInt("pos", p);
				msg.setData(data);
				getThumbHandler.sendMessage(msg);
			}
	    };
	    new Thread(runnable).start();
	}
	
	/**
	 * Handle response of get-thumbnail request.
	 */
	Handler getThumbHandler = new Handler() {
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
				byte[] thumb = data.getByteArray("thumb");
				int pos = data.getInt("pos");
				int rid = (Integer)pictures.get(pos).get("rid");
				
				int orientation = 0;
				try {
					File f = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), rid+"_thumb.jpg");
					FileOutputStream fos = new FileOutputStream(f);
					fos.write(thumb);
					fos.close();
					ExifInterface exif = new ExifInterface(f.getPath());
					orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
				} catch (Exception e) {
					Log.e(LOG_TAG, e.getMessage());
				}
				float rotate = 0;
				switch (orientation){
				  case 6: rotate = 90; break;
				  case 3: rotate = 180; break;
				  case 8: rotate = 270; break;
				}
				Bitmap bitmap = BitmapFactory.decodeByteArray(thumb, 0, thumb.length);
				
				Matrix matrix = new Matrix();
				matrix.postRotate(rotate);
				bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), 
						bitmap.getHeight(), matrix, true);
				
				pictures.get(pos).put("picture_img", bitmap);
				thumbs.put((Integer)pictures.get(pos).get("rid"), bitmap);
				((MySimpleAdapter)pictureGrid.getAdapter()).notifyDataSetChanged();
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
		reqParas.put("t", "gcr");
		reqParas.put("cid", String.valueOf(id));
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
						rewardText.setText("test");
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
}
