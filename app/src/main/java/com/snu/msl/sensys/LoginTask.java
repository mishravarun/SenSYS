package com.snu.msl.sensys;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import library.JSONParser;
import library.UserFunctions;

public class LoginTask extends AsyncTask<String, Void, Integer> {

	private ProgressDialog progressDialog;
	private SignInActivity activity;
	private int id = -1;
	private JSONParser jsonParser;
	private static String loginURL = "http://varunmishra.com/login/";
	private static String registerURL = "http://varunmishra.com/login/";
	private static String KEY_SUCCESS = "success";
	private static String KEY_ERROR = "error";
	private static String KEY_ERROR_MSG = "error_msg";
	private static String KEY_UID = "uid";
	private static String KEY_NAME = "name";
	private static String KEY_NAME2 = "age";
	private static String KEY_EMAIL = "email";
	private static String KEY_CREATED_AT = "created_at";
	private int responseCode = 0;
	String username="";

	public LoginTask(SignInActivity activity, ProgressDialog progressDialog)
	{
		this.activity = activity;
		this.progressDialog = progressDialog;
	}

	@Override
	protected void onPreExecute()
	{
		progressDialog.show();
	}

	protected Integer doInBackground(String... arg0) {
		EditText userName = (EditText)activity.findViewById(R.id.etUserName1);
		EditText passwordEdit = (EditText)activity.findViewById(R.id.etPass1);
		String email = userName.getText().toString();
		String password = passwordEdit.getText().toString();
		UserFunctions userFunction = new UserFunctions();
		JSONObject json = userFunction.loginUser(email, password);
		
		// check for login response
		try {
			if (json.getString(KEY_SUCCESS) != null) {
				String res = json.getString(KEY_SUCCESS);

				if(Integer.parseInt(res) == 1){
					responseCode = 1;

				}else{
					responseCode = 0;
					// Error in login
				}
			}

		} catch (NullPointerException e) {
			e.printStackTrace();

		}
		catch (JSONException e) {
			e.printStackTrace();
		}
		
		return responseCode;
	}

	@Override
	protected void onPostExecute(Integer responseCode)
	{
		EditText userName = (EditText)activity.findViewById(R.id.etUserName1);
		EditText passwordEdit = (EditText)activity.findViewById(R.id.etPass1);
		

		if (responseCode == 1) {
			progressDialog.dismiss();

            activity.editor.putString("username", "" + userName.getText());
            activity.editor.commit();
            Intent i = new Intent(activity.getApplicationContext(),MyActivity.class);

            activity.startActivity(i);
            activity.finish();
		}
			//activity.loginReport(responseCode);	
		
		else {
			progressDialog.dismiss();
			activity.showLoginError(responseCode);
	
		}
		
	}
}