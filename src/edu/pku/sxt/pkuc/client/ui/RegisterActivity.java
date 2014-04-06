package edu.pku.sxt.pkuc.client.ui;
import java.util.HashMap;
import java.util.Map;

import edu.pku.sxt.pkuc.client.R;
import edu.pku.sxt.pkuc.client.R.id;
import edu.pku.sxt.pkuc.client.R.layout;
import edu.pku.sxt.pkuc.client.R.string;
import edu.pku.sxt.pkuc.client.util.HttpManager;
import edu.pku.sxt.pkuc.client.util.MD5;
import edu.pku.sxt.pkuc.client.util.MD5Exception;

import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Activity for user to register.
 * @author songxintong
 *
 */
public class RegisterActivity extends ActionBarActivity {
	
	private static final String LOG_TAG = "RegisterActivity";
	
	// these result codes are used for previous Activity to determine
	// whether the registration is success
	public static final int RESULT_CODE_REGISTER_SUCCESS = 0;
	public static final int RESULT_CODE_REGISTER_FAIL = -1;
	
	// shared preferences
	SharedPreferences sp;
	
	// UI Views
	Button registerButton;
	Button returnButton;
	EditText unameEText;
	EditText pword1EText;
	EditText pword2EText;
	
	// data
	String uname;
	String pword;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register);
		
		// get shared preferences
		sp = getSharedPreferences("PKUC", Activity.MODE_PRIVATE);
				
		// custom action bar
		ActionBar bar = getSupportActionBar();
		bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#5CACEE")));
		bar.setDisplayShowTitleEnabled(false);
		bar.setDisplayShowCustomEnabled(true);
		bar.setDisplayShowHomeEnabled(false);
		View v = getLayoutInflater().inflate(R.layout.register_actionbar, null);
		bar.setCustomView(v);
		
		// buttons
		registerButton = (Button)findViewById(R.id.but_register);
		registerButton.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				register();
			}
		});
		returnButton = (Button)findViewById(R.id.but_return);
		returnButton.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				setResult(RESULT_CODE_REGISTER_FAIL);
				finish();
			}
		});
		
		// edit text
		unameEText = (EditText) findViewById(R.id.txt_uname);
		pword1EText = (EditText) findViewById(R.id.txt_pword);
		pword2EText = (EditText) findViewById(R.id.txt_pword2);
		
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event){
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			setResult(RESULT_CODE_REGISTER_FAIL);
			finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	/**
	 * Display a long-time toast.
	 * @param message Message of toast.
	 */
    private void makeToast(String message){
    	Toast toast = Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG);
    	toast.show();
    }
	
    /**
     * Make a regiser request.
     */
	private void register() {
		// get input
		uname = unameEText.getText().toString();
		String pword1 = pword1EText.getText().toString();
		String pword2 = pword2EText.getText().toString();
		
		// local check
		if(uname.length()==0) {
			makeToast(getString(R.string.please_input_username));
			return;
		}
		if(pword1.length()==0){
			makeToast(getString(R.string.please_input_password));
			return;
		}
		if(pword1.compareTo(pword2)!=0){
			makeToast(getString(R.string.passwords_are_different));
			return;
		}
		String regex = getString(R.string.regex_alpha_digit);
		if(!uname.matches(regex)||!pword1.matches(regex)){
			makeToast(getString(R.string.username_password_format));
			return;
		}
		
		try {
			pword = MD5.md5(pword1.getBytes());
		} catch (MD5Exception e) {
			makeToast(getString(R.string.cannot_register));
			Log.e(LOG_TAG, "Register Failed: md5 error");
			Log.e(LOG_TAG, e.getMessage());
			return;
		}
		
		// set http post parameters
		final Map<String, String> reqParas = new HashMap<String, String>();
		reqParas.put("t", "reg");
		reqParas.put("nam", uname);
		reqParas.put("pwd", pword);
		reqParas.put("imei", sp.getString("imei", ""));
		final String url = getString(R.string.server_url);
		
		// do post in new thread
	    Runnable runnable = new Runnable() {
			@Override
			public void run() {
				int status;
				try {
					Map<String, String> resParas = HttpManager.postKV(url, reqParas);
					status = Integer.parseInt(resParas.get("stat"));
				} catch (Exception e) {
					status = -2; // network error
					Log.e(LOG_TAG, e.getMessage());
				}				
				Message msg = new Message();
				Bundle data = new Bundle();
				data.putInt("status", status);
				msg.setData(data);
				handler.sendMessage(msg);
			}
	    };
	    new Thread(runnable).start();
	}
	
    /**
     * Handle response of register request
     */
    Handler handler = new Handler() {
    	@Override
    	public void handleMessage(Message msg) {
    		super.handleMessage(msg);
    		Bundle data = msg.getData();
    		int status = data.getInt("status");
    		/*
			 * 0 - success
			 * 1 - user name existed
			 * -1 - server error
			 * -2 - network error
			 */
			switch (status) {
			  case 0: 
				makeToast(getString(R.string.register_success));
				finish();
				sp.edit()
				.putBoolean("isLogin", true)
				.putBoolean("remUname", true)
				.putBoolean("remPword", true)
				.putString("uname", uname)
				.putString("pword", pword)
				.commit();
				Intent intent = new Intent(RegisterActivity.this, UserInfoActivity.class);
				startActivity(intent);
				setResult(RESULT_CODE_REGISTER_SUCCESS);
				finish();
				break;
			  case 1:
				makeToast(getString(R.string.register_username_exist));
				Log.e(LOG_TAG, "Register Failed: username exist");
				break;
			  case -1:
				makeToast(getString(R.string.cannot_register));
				Log.e(LOG_TAG, "Register Failed: server error");
				break;
			  case -2: 
				makeToast(getString(R.string.cannot_register));
				Log.e(LOG_TAG, "Register Failed: network error");
				break;
			}
    	}
    };
}
