/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.snu.msl.sensys.SyncAdapter;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Define a sync adapter for the app.
 *
 * <p>This class is instantiated in {@link SyncService}, which also binds SyncAdapter to the system.
 * SyncAdapter should only be initialized in SyncService, never anywhere else.
 *
 * <p>The system calls onPerformSync() via an RPC call through the IBinder object supplied by
 * SyncService.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {
    ContentResolver mContentResolver;

    /**
     * Set up the sync adapter
     */
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
        mContentResolver = context.getContentResolver();
    }


    /**
     * Set up the sync adapter. This form of the
     * constructor maintains compatibility with Android 3.0
     * and later platform versions
     */
    public SyncAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
        mContentResolver = context.getContentResolver();

    }

    @Override
    public void onPerformSync(Account account, Bundle bundle, String s, ContentProviderClient contentProviderClient, SyncResult syncResult) {
       Log.d("UPUP", "Reached here");
        try {
            Cursor cursor = contentProviderClient.query(Uri.parse("content://com.snu.msl.sensys.provider/datacollected"),null, null, null, null);
            cursor.moveToFirst();
            StringBuilder res=new StringBuilder();
                while (!cursor.isAfterLast()) {
                     String timestamp = cursor.getString(cursor.getColumnIndex("timestamp"));
                    String firstTime = cursor.getString(cursor.getColumnIndex("firsttime"));
                    String gpsLatitude = cursor.getString(cursor.getColumnIndex("gpslatitude"));
                     String gpsLongitude = cursor.getString(cursor.getColumnIndex("gpslongitude"));
                     String sensordroneMAC = cursor.getString(cursor.getColumnIndex("sensordronemac"));
                     String sensordroneTemperature = cursor.getString(cursor.getColumnIndex("sensordronetemperature"));
                     String sensordroneHumidity = cursor.getString(cursor.getColumnIndex("sensordronehumidity"));
                    String sensordronePressure = cursor.getString(cursor.getColumnIndex("sensordronepressure"));
                     String sensordroneIRTemperature = cursor.getString(cursor.getColumnIndex("sensordroneirtemperature"));
                     String sensordroneIlluminance = cursor.getString(cursor.getColumnIndex("sensordroneilluminance"));
                     String sensordronePrecisionGas = cursor.getString(cursor.getColumnIndex("sensordroneprecisiongas"));
                     String sensordroneCapacitance = cursor.getString(cursor.getColumnIndex("sensordronecapacitance"));
                     String sensordroneExternalVoltage = cursor.getString(cursor.getColumnIndex("sensordroneexternalvoltage"));
                     String sensordroneBatteryVoltage = cursor.getString(cursor.getColumnIndex("sensordronebatteryvoltage"));
                    String sensordroneOxidizingGas = cursor.getString(cursor.getColumnIndex("sensordroneoxidizinggas"));
                    String sensordroneReducingGas = cursor.getString(cursor.getColumnIndex("sensordronereducinggas"));
                    String gpsAltitude = cursor.getString(cursor.getColumnIndex("gpsaltitude"));
                    String sensordroneCO2 = cursor.getString(cursor.getColumnIndex("sensordroneco2"));


                    String responseString ="";
                    HttpClient httpclient = new DefaultHttpClient();
                    HttpPost httppost = new HttpPost("http://mobisense.webapps.snu.edu.in/ITRAWebsite/upload/sensordroneupload.php");
                    try {
                        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                        nameValuePairs.add(new BasicNameValuePair("user", account.name));
                        nameValuePairs.add(new BasicNameValuePair("timestamp", timestamp));
                        nameValuePairs.add(new BasicNameValuePair("firsttime", firstTime));
                        nameValuePairs.add(new BasicNameValuePair("gpslatitude", gpsLatitude));
                        nameValuePairs.add(new BasicNameValuePair("gpslongitude", gpsLongitude));
                        nameValuePairs.add(new BasicNameValuePair("sensordronemac", sensordroneMAC));
                        nameValuePairs.add(new BasicNameValuePair("sensordronetemperature", sensordroneTemperature));
                        nameValuePairs.add(new BasicNameValuePair("sensordronehumidity", sensordroneHumidity));
                        nameValuePairs.add(new BasicNameValuePair("sensordronepressure", sensordronePressure));
                        nameValuePairs.add(new BasicNameValuePair("sensordroneirtemperature", sensordroneIRTemperature));
                        nameValuePairs.add(new BasicNameValuePair("sensordroneilluminance", sensordroneIlluminance));
                        nameValuePairs.add(new BasicNameValuePair("sensordroneprecisiongas", sensordronePrecisionGas));
                        nameValuePairs.add(new BasicNameValuePair("sensordronecapacitance", sensordroneCapacitance));
                        nameValuePairs.add(new BasicNameValuePair("sensordroneexternalvoltage", sensordroneExternalVoltage));
                        nameValuePairs.add(new BasicNameValuePair("sensordronebatteryvoltage", sensordroneBatteryVoltage));
                        nameValuePairs.add(new BasicNameValuePair("sensordroneoxidizinggas", sensordroneOxidizingGas));
                        nameValuePairs.add(new BasicNameValuePair("sensordronereducinggas", sensordroneReducingGas));
                        nameValuePairs.add(new BasicNameValuePair("sensordroneco2", sensordroneCO2));
                        nameValuePairs.add(new BasicNameValuePair("gpsaltitude", gpsAltitude));

                        Log.d("TAG",sensordroneOxidizingGas+", " + sensordroneReducingGas + ", " + sensordroneTemperature);

                        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                        HttpResponse response=  httpclient.execute(httppost);
                        HttpEntity entity = response.getEntity();
                        responseString = EntityUtils.toString(entity, "UTF-8");
                        Log.d("TAGFFFF", responseString);
                    } catch (ClientProtocolException e) {
                        // TODO Auto-generated catch block
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                    }
                    if(responseString.equalsIgnoreCase("Success"))
                        contentProviderClient.delete(Uri.parse("content://com.snu.msl.sensys.provider/datacollected"), "timestamp=?", new String[]{timestamp});



                        cursor = contentProviderClient.query(Uri.parse("content://com.snu.msl.sensys.provider/datacollected"), null, null, null, null);
                        cursor.moveToFirst();

                }
            } catch (RemoteException e) {
            syncResult.hasHardError();
            e.printStackTrace();
        }


    }

}