package edu.pku.sxt.pkuc.client.ui;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.pku.sxt.pkuc.client.Messages;
import edu.pku.sxt.pkuc.client.Canteen;
import edu.pku.sxt.pkuc.client.R;
import edu.pku.sxt.pkuc.client.R.layout;
import edu.pku.sxt.pkuc.client.R.menu;
import edu.pku.sxt.pkuc.client.util.CanteenDistanceComparator;
import edu.pku.sxt.pkuc.client.util.HttpManager;
import edu.pku.sxt.pkuc.client.util.MessageComparator;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class MessageActivity extends Activity {
	
	// UI views
	ListView messageList;
	Button refreshButton;
	ProgressBar progressBar;
	
	// data
	public static List<Messages> messages = new ArrayList<Messages>();	// messages' information
	public static final String LOG_TAG = "Message_Activity";
	public static final String File_Name = "History_Message1.txt";
	public static int history_mid = -1;
	public static Context context;
	List<Map<String, Object>> listData;	// data of message
	int reward = -1; // current reward for reporting
	SharedPreferences sp;
	Date lastRefreshDate = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_message);
		
		refreshButton = (Button)findViewById(R.id.but_message_refresh);
		refreshButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				getMessages();
			}
			
		});
		sp = getSharedPreferences(getString(R.string.shared_preferences_name), Activity.MODE_PRIVATE);
		history_mid = sp.getInt("mid", 0);
		progressBar = (ProgressBar)findViewById(R.id.progressBar_message);
		progressBar.setVisibility(View.GONE);
		messageList = (ListView)findViewById(R.id.message_list);
		getMessages();
		/*messageList.setOnItemClickListener(new ListView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				Messages m = messages.get(position);
				Toast t = Toast.makeText(MessageActivity.this, m.content, Toast.LENGTH_LONG);
				t.show();
			}
		});*/
		
	}
	private void getMessages(){
		messages = new ArrayList<Messages>();
		progressBar.setVisibility(View.VISIBLE);
		// set HTTP post parameters
		final Map<String, String> reqParas = new HashMap<String, String>();
		reqParas.put("t", "gml");
		if (!sp.getBoolean("isLogin", false)) {
			reqParas.put("isu", "0");
			reqParas.put("nam", sp.getString("imei", ""));
		} else {
			reqParas.put("isu", "1");
			reqParas.put("nam", sp.getString("uname", ""));
		}
		history_mid = sp.getInt("mid", 0);
		reqParas.put("mid", String.valueOf(history_mid));
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
				getMessageHandler.sendMessage(msg);
			}
	    };
	    new Thread(runnable).start();
	}
	Handler getMessageHandler = new Handler(){
		public void handleMessage(Message msg){
			Bundle data = msg.getData();
			int status = data.getInt("status");
			String json = data.getString("json");
			readHistoryMessageList();
			switch(status){
				case 1:
					try {
						JSONArray ja = new JSONArray(json);
						for (int i=0; i<ja.length(); i++) {
							JSONObject jo = ja.getJSONObject(i);
							Messages m = new Messages();
							m.mid = jo.getInt("id");
							if(history_mid<m.mid)
								history_mid = m.mid;
							System.err.println("mid:"+m.mid+" hm:"+history_mid);
							m.title = jo.getString("title");
							m.content = jo.getString("content");
							m.time = jo.getInt("time");
							saveMessage(m);
							messages.add(m);
						}
						sp.edit()
						.putInt("mid", history_mid)
						.commit();
						/*/ sort canteens by level and location
						Location loc = null;
						if (location != null && 
								location.getTime() + 5 * 60 * 1000> new Date().getTime()) {
							loc = location;
						}*/
						Collections.sort(messages, new MessageComparator());
						resetMessageList();
					} catch (JSONException e) {
						makeToast(getString(R.string.cannot_get_message_list));
						Log.e(LOG_TAG, e.getMessage());
					}
					break;
				case -1:
					makeToast(getString(R.string.cannot_get_message_list));
					Log.e(LOG_TAG, "Failed to get canteen list due to network error");
					break;
				case -2:
					makeToast("Connected~!");
					Log.e(LOG_TAG, "Connected");
					break;
			}
			progressBar.setVisibility(View.GONE);
		}
	};
	public void readHistoryMessageList(){
		FileInputStream fis = null;
		try {
			fis = openFileInput(File_Name);
			byte[] buffer = new byte[1024];
			try {
				int len = 0;
				StringBuilder sb = new StringBuilder("");
				while((len = fis.read(buffer)) > 0){
					sb.append(new String(buffer,0,len));
				}
				//System.err.print(sb.toString());
				dealStrings(sb.toString());
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}  
		return;
	}
	public void resetMessageList(){
		listData = new ArrayList<Map<String,Object>>();
		for(int i = 0;i<messages.size();i++){
			Map<String, Object> map = new HashMap<String,Object>();
			Messages m = messages.get(i);
			map.put("message_title", m.title);
			map.put("message_content", m.content);
			map.put("message_time", m.time);
			listData.add(map);
		}
		SimpleAdapter adapter = new SimpleAdapter(MessageActivity.this,listData,R.layout.message_list,
								new String[] {"message_title","message_content","message_time"},
								new int[] {R.id.message_list_title,R.id.message_list_content,R.id.message_list_time});
		messageList.setAdapter(adapter);
	}
	public void saveMessage(Messages m){
		FileOutputStream ops = null;
		try {
			ops = openFileOutput(File_Name,Context.MODE_APPEND);
			PrintStream ps = new PrintStream(ops);
			ps.println(String.valueOf(m.mid));
			ps.println(m.title);
			ps.println(m.content);
			ps.println(String.valueOf(m.time));
			ps.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
				try {
					if(ops!=null) ops.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}
	private void dealStrings(String message){
		StringTokenizer st = new StringTokenizer(message,"\n");
		Messages m;
		String token;
		while(st.hasMoreTokens()){
			m = new Messages();
			token = st.nextToken();
			System.err.println(token);
			m.mid = Integer.valueOf(token);
			token = st.nextToken();
			System.err.println(token);
			m.title = token;
			token = st.nextToken();
			System.err.println(token);
			m.content = token;
			token = st.nextToken();
			System.err.println(token);
			m.time = Integer.valueOf(token);
			messages.add(m);
		}
		return;
	}
	private void makeToast(String message) {
		Toast toast = Toast.makeText(MessageActivity.this, message, Toast.LENGTH_LONG);
		toast.show();
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.message, menu);
		return true;
	}

}
