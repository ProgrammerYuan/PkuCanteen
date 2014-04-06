package edu.pku.sxt.pkuc.client.ui;

import java.util.HashMap;
import java.util.Map;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import edu.pku.sxt.pkuc.client.R;
import edu.pku.sxt.pkuc.client.util.HttpManager;

/**
 * Dialog for editing email address.
 * @author songxintong
 *
 */
public class EditEmailDialogFragment extends DialogFragment {
	static final String LOG_TAG = "EditEmailDialogFragment";
	
	View view;
	UserInfoActivity activity;
	SharedPreferences sp;
	EditText emailET;
	Button confirmButton;
	
	public void setActivity(UserInfoActivity activity) { this.activity = activity; }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		getDialog().setTitle(R.string.email);
		view = inflater.inflate(R.layout.dialog_edit_email, container);
		sp = activity.sp;
		
		emailET = (EditText)view.findViewById(R.id.et_email);
		confirmButton = (Button)view.findViewById(R.id.but_confirm);
		
		confirmButton.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				String email = emailET.getText().toString();
				if (email.length() == 0) {
					makeToast(getString(R.string.please_input_email));
					return;
				}
				updateEmail(email);
			}
		});
		
		return view;
	}
	
	/**
	 * Make a update-email request
	 * @param email
	 */
	void updateEmail(String email) {
		final String e = email;
		
		// set http post parameters
		final Map<String, String> reqParas = new HashMap<String, String>();
		reqParas.put("t", "ude");
		reqParas.put("nam", sp.getString("uname", ""));
		reqParas.put("pwd", sp.getString("pword", ""));
		reqParas.put("email", e);
		final String url = getString(R.string.server_url);
		
		// do post in new thread
		Runnable runnable = new Runnable(){
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
				data.putString("email", e);
				msg.setData(data);
				handler.sendMessage(msg);
			}
	    };
	    new Thread(runnable).start();
	}

	/**
	 * Handle response of update-email request.
	 */
	Handler handler = new Handler() {
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
				String email = data.getString("email");
				activity.emailText.setText(email);
				dismiss();
				break;
			  case 1: 
				makeToast(getString(R.string.account_error_relogin));
				Log.e(LOG_TAG, "Update Email Failed: account error");
				Intent intent = new Intent(activity, LoginActivity.class);
				startActivity(intent);
				activity.finish();
				dismiss();
				break;
			  case -1: 
				makeToast(getString(R.string.upload_failed));
				Log.e(LOG_TAG, "Update Email Failed: server error");
				dismiss();
				break;
			  case -2: 
				makeToast(getString(R.string.upload_failed));
				Log.e(LOG_TAG, "Update Email Failed: network error");
				dismiss();
				break;
			}
		}
	};
	  
	/**
	 * Display a long-time toast.
	 * @param message Message of toast.
	 */
	private void makeToast(String message){
    	Toast toast = Toast.makeText(view.getContext(), message, Toast.LENGTH_LONG);
    	toast.show();
    }
}
