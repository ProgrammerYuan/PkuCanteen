package edu.pku.sxt.pkuc.client.ui;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import edu.pku.sxt.pkuc.client.R;
import edu.pku.sxt.pkuc.client.util.HttpManager;
import edu.pku.sxt.pkuc.client.util.MD5;
import edu.pku.sxt.pkuc.client.util.MD5Exception;

/**
 * Activity for user to login.
 * @author songxintong
 *
 */
public class LoginActivity extends ActionBarActivity {
	
	private static final String LOG_TAG = "LoginActivity";
	
	// shared preferences
	SharedPreferences sp;
	
	// UI Views
	Button loginButton;
	Button returnButton;
	Button registerButton;
	EditText unameEText;
	EditText pwordEText;
	CheckBox remNamCB;
	CheckBox remPwdCB;
	
	// data
	String uname;
	String pword;
	boolean useNewPword = true; // user entered new password
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		
		sp = getSharedPreferences(getString(R.string.shared_preferences_name), Activity.MODE_PRIVATE);
	
		// custom action bar
		ActionBar bar = getSupportActionBar();
		bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#5CACEE")));
		bar.setDisplayShowTitleEnabled(false);
		bar.setDisplayShowCustomEnabled(true);
		bar.setDisplayShowHomeEnabled(false);
		View v = getLayoutInflater().inflate(R.layout.login_actionbar, null);
		bar.setCustomView(v);
		
		// get views
		unameEText = (EditText) findViewById(R.id.txt_uname);
		pwordEText = (EditText) findViewById(R.id.txt_pword);
		remNamCB = (CheckBox) findViewById(R.id.cb_runame);
		remPwdCB = (CheckBox) findViewById(R.id.cb_rpword);
		loginButton = (Button)findViewById(R.id.but_login);
		returnButton = (Button)findViewById(R.id.but_return);
		
		// restore username and password
        remNamCB.setChecked(sp.getBoolean("remUname", false));
        remPwdCB.setChecked(sp.getBoolean("remPword", false));
        if (remNamCB.isChecked()) {
        	unameEText.setText(sp.getString("uname", ""));
        } else {
        	remPwdCB.setClickable(false);
        }
        if (remPwdCB.isChecked()) {
        	pword = sp.getString("pword", "");
        	pwordEText.setText("password");
        	useNewPword = false;
        }
        
        // remember username checkbox
        remNamCB.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (remNamCB.isChecked()) {
					remPwdCB.setClickable(true);
				} else {
					remPwdCB.setClickable(false);
					remPwdCB.setChecked(false);
				}
			}
        });
		
        // password edit text
        pwordEText.setOnFocusChangeListener(new EditText.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus && !useNewPword) {
					useNewPword = true;
					pwordEText.setText("");
				}
			}
        });
        
        // buttons
		loginButton.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				login();
			}
		});
		returnButton.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		registerButton = (Button)findViewById(R.id.but_register);
		registerButton.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
				startActivityForResult(intent, 0);
			}
		});	
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RegisterActivity.RESULT_CODE_REGISTER_SUCCESS) { finish(); }
	}
	
	/**
	 * Display a long-time toast.
	 * @param message Message of toast.
	 */
    private void makeToast(String message){
    	Toast toast = Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG);
    	toast.show();
    }
	
    /** 
     * Make a login request.
     */
    private void login() {
    	// local check
    	String regex = getString(R.string.regex_alpha_digit);
		uname = unameEText.getText().toString();
		if (uname.length() == 0) {
			makeToast(getString(R.string.please_input_username));
			return;
		}
		if (!uname.matches(regex)) {
			makeToast(getString(R.string.username_password_format));
			return;
		}
		if (useNewPword) {
			String password = pwordEText.getText().toString();
			if (password.length() == 0) {
				makeToast(getString(R.string.please_input_password));
				return;
			}
			if (!password.matches(regex)) {
				makeToast(getString(R.string.username_password_format));
				return;
			}
			try {
				pword = MD5.md5(password.getBytes());
			} catch (MD5Exception e) {
				makeToast(getString(R.string.cannot_login));
				Log.e(LOG_TAG, "Login Failed: md5 error");
				Log.e(LOG_TAG, e.getMessage());
				return;
			}
		}
		
		// set http post parameters
		final Map<String, String> reqParas = new HashMap<String, String>();
		reqParas.put("t", "log");
		reqParas.put("nam", uname);
		reqParas.put("pwd", pword);
		final String url = getString(R.string.server_url);
		
		// do post in new thread
	    Runnable runnable = new Runnable(){
	    	String nam, pwd;
	    	boolean remNam, remPwd;
			@Override
			public void run() {
				nam = uname;
				pwd = pword;
				remNam = remNamCB.isChecked();
				remPwd =  remPwdCB.isChecked();
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
				data.putString("uname", nam);
				data.putString("pword", pwd);
				data.putBoolean("remNam", remNam);
				data.putBoolean("remPwd", remPwd);
				msg.setData(data);
				handler.sendMessage(msg);
			}
	    };
	    new Thread(runnable).start();
	}

    /**
     * Handle response of login request.
     */
    Handler handler = new Handler() {
    	@Override
    	public void handleMessage(Message msg) {
    		super.handleMessage(msg);
    		Bundle data = msg.getData();
    		int status = data.getInt("status");
    		/*
			 * 0 - success
			 * 1 - user doesn't existed
			 * 2 - wrong password
			 * -1 - server error
			 * -2 - network error
			 */
			switch (status) {
			  case 0: 
				makeToast(getString(R.string.login_success));
				sp.edit()
				.putBoolean("isLogin", true)
				.putBoolean("remUname", data.getBoolean("remNam"))
				.putBoolean("remPword", data.getBoolean("remPwd"))
				.putString("uname", data.getString("uname"))
				.putString("pword", data.getString("pword"))
				.commit();
				Intent intent = new Intent(LoginActivity.this, UserInfoActivity.class);
				startActivity(intent);
				finish();
				break;
			case 1: 
				makeToast(getString(R.string.login_user_not_exist));
				Log.e(LOG_TAG, "Login Failed: user not exist");
				break;
			case 2:
				makeToast(getString(R.string.login_wrong_password));
				Log.e(LOG_TAG, "Login Failed: password error");
				break;
			case -1:
				makeToast(getString(R.string.cannot_login));
				Log.e(LOG_TAG, "Login Failed: server error");
				break;
			case -2: 
				makeToast(getString(R.string.cannot_login));
				Log.e(LOG_TAG, "Login Failed: network error");
				break;
			}
    	}
    };
}
