package com.snu.msl.sensys;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class SignInActivity extends Activity {
    public SharedPreferences.Editor editor;
    public TextView t;
    Button btnLogin;
    Button btnLinkToRegister;
    EditText inputEmail;
    EditText inputPassword;
    TextView loginErrorMsg;

    // JSON Response node names
    private static String KEY_SUCCESS = "success";
    private static String KEY_ERROR = "error";
    private static String KEY_ERROR_MSG = "error_msg";
    private static String KEY_UID = "uid";
    private static String KEY_NAME = "name";
    private static String KEY_NAME2 = "age";
    private static String KEY_EMAIL = "email";
    private static String KEY_CREATED_AT = "created_at";
    private String re;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in_screen);
        SharedPreferences pref = getApplicationContext().getSharedPreferences("User", 0);
        editor = pref.edit();
        inputEmail = (EditText) findViewById(R.id.etUserName1);
        inputPassword = (EditText) findViewById(R.id.etPass1);
        btnLogin = (Button) findViewById(R.id.btnSingIn1);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ProgressDialog progressDialog = new ProgressDialog(SignInActivity.this);
                progressDialog.setMessage("Logging in...");
                LoginTask loginTask = new LoginTask(SignInActivity.this, progressDialog);
                loginTask.execute();


            }
        });
    }

    public void showLoginError(int responseCode) {
        int duration = Toast.LENGTH_LONG;
        Context context = getApplicationContext();
        Toast toast = Toast.makeText(context, "Login Error", duration);
        toast.show();
    }
    
}
