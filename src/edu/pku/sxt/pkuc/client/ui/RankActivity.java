package edu.pku.sxt.pkuc.client.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.pku.sxt.pkuc.client.Messages;
import edu.pku.sxt.pkuc.client.R;
import edu.pku.sxt.pkuc.client.R.layout;
import edu.pku.sxt.pkuc.client.R.menu;
import edu.pku.sxt.pkuc.client.util.HttpManager;
import edu.pku.sxt.pkuc.client.util.HttpManagerException;
import edu.pku.sxt.pkuc.client.util.MessageComparator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class RankActivity extends Activity {
	
	ListView rankList;
	ProgressBar progressBar;
		
	// data
	public static List<Messages> ranks = new ArrayList<Messages>();	// messages' information
	public static final String LOG_TAG = "rank_Activity";
	List<Map<String, String>> listData;
	SharedPreferences sp;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rank);
		rankList = (ListView)findViewById(R.id.rank_list);
		sp = getSharedPreferences(getString(R.string.shared_preferences_name), Activity.MODE_PRIVATE);
		getRankList();
	}
	private void getRankList(){
		final Map<String,String> reqParas = new HashMap();
		reqParas.put("t", "grl");
		if (!sp.getBoolean("isLogin", false)) {
			reqParas.put("isu", "0");
			reqParas.put("nam", sp.getString("imei", ""));
		} else {
			reqParas.put("isu", "1");
			reqParas.put("nam", sp.getString("uname", ""));
		}
		final String url = getString(R.string.server_url);
		Runnable runnable = new Runnable(){
			@Override
			public void run() {
				int status = -1;
				String json = "";
				// TODO Auto-generated method stub
				Map<String, String> resParas;
				try {
					resParas = HttpManager.postKV(url, reqParas);
					status = Integer.valueOf(resParas.get("stat"));
					json = resParas.get("json");
				} catch (HttpManagerException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Bundle data = new Bundle();
				Message msg = new Message();
				data.putInt("status", status);
				data.putString("json", json);
				msg.setData(data);
				getRankListHandler.sendMessage(msg);
			}
			
		};
		new Thread(runnable).start();
	}
	Handler getRankListHandler = new Handler(){
		public void handleMessage(Message msg){
			Bundle data = msg.getData();
			Map<String,String> map;
			listData = new ArrayList<Map<String, String>>();
			int status = data.getInt("status");
			String json = data.getString("json");
			switch(status) {
				case 1:
					try {
						JSONArray ja = new JSONArray(json);
						for (int i=0; i<ja.length(); i++) {
							
							JSONObject jo = ja.getJSONObject(i);
							if(i==10){
								map = new HashMap<String,String>();
								map.put("id","...");
								map.put("name","...");
								map.put("score","...");
								listData.add(map);
							}
							map = new HashMap<String,String>();
							map.put("id", String.valueOf(i)+".");
							map.put("name", jo.getString("name"));
							map.put("score", String.valueOf(jo.get("score")) + "points");
							listData.add(map);
						}
					} catch (JSONException e) {
						makeToast(getString(R.string.cannot_get_message_list));
						Log.e(LOG_TAG, e.getMessage());
					}
					SimpleAdapter adp = new SimpleAdapter(RankActivity.this,listData,R.layout.rank_list,
										new String[] {"id","name","score"},
										new int[] {R.id.rank_num,R.id.rank_user,R.id.rank_score});
					rankList.setAdapter(adp);
					break;
				case -1:
					makeToast(getString(R.string.cannot_get_message_list));
					Log.e(LOG_TAG, "Failed to get canteen list due to network error");
					break;
			}
		}
	};
	private void makeToast(String message) {
		Toast toast = Toast.makeText(RankActivity.this, message, Toast.LENGTH_LONG);
		toast.show();
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.rank, menu);
		return true;
	}

}
