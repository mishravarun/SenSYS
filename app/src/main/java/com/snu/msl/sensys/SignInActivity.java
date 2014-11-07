package com.snu.msl.sensys;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class SignInActivity extends Activity {
    public SharedPreferences.Editor editor;
    public  TextView t;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in_screen);
        SharedPreferences pref = getApplicationContext().getSharedPreferences("User", 0);
        editor = pref.edit();
        t= (TextView) findViewById(R.id.etUserName);
        Button b = (Button) findViewById(R.id.btnSingIn);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor.putString("username",""+t.getText());
                editor.commit();
                Intent i = new Intent(getApplicationContext(),MyActivity.class);

                startActivity(i);
                finish();
            }
        });
    }

    
}
