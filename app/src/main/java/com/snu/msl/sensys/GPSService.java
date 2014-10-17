package com.snu.msl.sensys;

/**
 * Created by varun on 14/10/14.
 */
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Context;
import android.content.Intent;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager.WakeLock;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

public class GPSService extends Service implements LocationListener, GpsStatus.Listener {

    public static final String TAG = GPSService.class.getName();
    public static final int SCREEN_OFF_RECEIVER_DELAY = 500;
    protected LocationManager locationManager;
    public static String latitude,longitude;
    public static String GPS_STATUS="Waiting For Location";
    private int gpsStatus=1;
    public static boolean isGPSFix=false;
    private WakeLock mWakeLock = null;
    public  long mLastLocationMillis;
    public static Location mLastLocation;
    public BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive("+intent+")");
            if (!intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                return;
            }
            Runnable runnable = new Runnable() {
                public void run() {
                    Log.i(TAG, "Runnable executing.");
                }
            };
            new Handler().postDelayed(runnable, SCREEN_OFF_RECEIVER_DELAY);
        }
    };
    @Override
    public void onCreate() {
        super.onCreate();
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            locationManager.addGpsStatusListener(this);
        PowerManager manager =
                (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }

    @Override
    public void onDestroy() {
            locationManager.removeUpdates(this);
        unregisterReceiver(mReceiver);
        GPS_STATUS="IDLE";
        mWakeLock.release();
        stopForeground(true);
    }
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        mWakeLock.acquire();
        return START_STICKY;
    }
    @Override
    public void onLocationChanged(Location location) {
// TODO Auto-generated method stub
        if (location == null) return;

        mLastLocationMillis = SystemClock.elapsedRealtime();

        // Do something.

        mLastLocation = location;

        }
    @Override
    public void onProviderDisabled(String provider) {
        Log.d("Latitude","disable");
        GPS_STATUS="Location Not Available";

        stopSelf();
    }
    @Override
    public void onProviderEnabled(String provider) {
        Log.d("Latitude","enable");
    }
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("Latitude",""+status);
        gpsStatus=status;
        if (gpsStatus==1)
            GPS_STATUS="Location Not Available";
    }

    @Override
    public void onGpsStatusChanged(int event) {
        switch (event) {
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                if (mLastLocation != null)
                    isGPSFix = (SystemClock.elapsedRealtime() - mLastLocationMillis) < 4000;
                mLastLocationMillis=SystemClock.elapsedRealtime();
                if (isGPSFix) { // A fix has been acquired.
                    // Do something.

                } else { // The fix has been lost.
                    // Do something.
                }

                break;
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                // Do something.
                isGPSFix = true;
                 break;

        }
    }
}