package com.snu.msl.sensys;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class SignUpActivity extends Activity {
    String id;
    InputStream is=null;
    String result=null;
    String line=null;
    int code;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up_screen);
        Button b = (Button) findViewById(R.id.btnSingUpmain);
        final TextView t = (TextView) findViewById( R.id.etEmail);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                id  = ""+t.getText();
                new SendData().execute("http://mobisense.webapps.snu.edu.in/ITRAWebsite/upload/mail.php");
            }
        });
    }
    class SendData extends AsyncTask<String, Void, Integer> {
        private Exception exception;
        protected Integer doInBackground(String... urls) {
            ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("id",id));
            try
            {
                HttpClient httpclient = new DefaultHttpClient();
                HttpPost httppost = new HttpPost(urls[0]);
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                HttpResponse response = httpclient.execute(httppost);
                HttpEntity entity = response.getEntity();
                is = entity.getContent();
                Log.e("pass 1", "connection success ");
            }
            catch(Exception e)
            {
                Log.e("Fail 1", e.toString());
                return 0;
            }
            try
            {
                BufferedReader reader = new BufferedReader
                        (new InputStreamReader(is,"iso-8859-1"),8);
                StringBuilder sb = new StringBuilder();
                while ((line = reader.readLine()) != null)
                {
                    sb.append(line + "\n");
                }
                is.close();
                result = sb.toString();
                Log.e("pass 2", result);
            }
            catch(Exception e)
            {
                Log.e("Fail 2", e.toString());
                return 0;
            }
            if(result.contains("success"))
                return 1;
            else
                return 0;
        }
        protected void onPostExecute(Integer result) {
            if(result==1)
                Toast.makeText(getApplicationContext(), "Successfully Requested", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(getApplicationContext(),"Failed",Toast.LENGTH_SHORT).show();
        }
    }


}
