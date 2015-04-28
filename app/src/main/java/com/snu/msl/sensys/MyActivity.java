package com.snu.msl.sensys;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.sensorcon.sensordrone.DroneEventListener;
import com.sensorcon.sensordrone.DroneEventObject;
import com.sensorcon.sensordrone.DroneStatusListener;
import com.sensorcon.sensordrone.android.tools.DroneConnectionHelper;
import com.sensorcon.sensordrone.android.tools.DroneQSStreamer;
import com.sensorcon.sensordrone.android.tools.DroneStreamer;
import com.snu.msl.sensys.Cards.SensorCard;
import com.snu.msl.sensys.SyncAdapter.Provider.Provider;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import it.gmariotti.cardslib.library.view.CardView;


public class MyActivity extends Activity {
    private SharedPreferences sdcPreferences;
   public static CardView cardView;
    public static SensorCard card;
    public static int fTime=0;
    public static String firstTime="";
    public static ProgressDialog progressDialog;
    public static EditText indoorLocation;
    public static String indoors[];
    /*
     * We put our Drone object in a class that extends Application so it can be
     * accessed in multiple activities.
     */
    private DroneApplication droneApp;
    /*
         * We will use some stuff from our Sensordrone Helper library
         */
    public DroneConnectionHelper myHelper = new DroneConnectionHelper();
    // A ConnectionBLinker from the SDHelper Library
    private DroneStreamer myBlinker;

    // Our Listeners
    private DroneEventListener deListener;
    private DroneStatusListener dsListener;

    // An int[] that will hold the QS_TYPEs for our sensors of interest
    private int[] qsSensors;

    // Text to display
    private static final String[] SENSOR_NAMES = { "Temperature (Ambient)",
            "Humidity", "Pressure", "Object Temperature (IR)",
            "Illuminance (calculated)", "Precision Gas (CO equivalent)",
            "Proximity Capacitance", "External Voltage (0-3V)",
            "Altitude (calculated)", "Oxidizing Gas", "Reducing Gas"};


    // Figure out how many sensors we have based on the length of our labels
    private int numberOfSensors = SENSOR_NAMES.length;
    SimpleDateFormat s = new SimpleDateFormat("ddMMyyyyHHmmss");
    String sensordroneprecisionGas="";
    String timeStamp="";
    String sensordroneMAC="";
    String sensordroneTemperature="-";
    String sensordroneHumidity="-";
    String sensordronePressure="-";
    String sensordroneIRTemperature="-";
    String sensordroneIlluminance="-";
    String sensordroneCapacitance="-";
    String sensordroneExternalVoltage="-";
    String sensordroneBatteryVoltage="-";
    String sensordroneOxidizingGas="-";
    String sensordroneReducingGas="-";
    String sensordroneCO2="-";

    // GUI Stuff
    private ImageButton btnStartLogging;
    private ImageButton btnStopLogging;
    private Switch gpsSwitch;
    private TextView tvStatus;
    private TextView tvSampling;
    // Another object from the SDHelper library. It helps us set up our
    // pseudo streaming
    private DroneQSStreamer[] streamerArray = new DroneQSStreamer[numberOfSensors];

    // We only want to notify of a low battery once,
    // but the event might be triggered multiple times.
    // We use this to try and show it only once
    private boolean lowbatNotify;
     DroneStreamer bvStreamer=null;
    // Toggle our LED
    private boolean ledToggle = true;
    public static final String SCHEME = "content://";
    // Content provider authority
    // Path for the content provider table
    public static final String TABLE_PATH = "temperature";

    public static final String AUTHORITY = "com.snu.msl.sensys.provider";
    // An account type, in the form of a domain name
    public static final String ACCOUNT_TYPE = "com.snu.msl.sensys.SyncAdapter.accounts.GenericAccountService";
    // The account name
    public static  String ACCOUNT = "";
    // Instance fields
    private boolean gpsStatus = true;
    private boolean isSampling = false;
    Account mAccount;
    Uri mUri;
    // A content resolver for accessing the provider
    ContentResolver mResolver;
    public static Handler UIHandler;
    boolean newFile = false;
    FileWriter writer;
    boolean firstLog=true;

    static
    {
        UIHandler = new Handler(Looper.getMainLooper());
    }
    public static void runOnUI(Runnable runnable) {
        UIHandler.post(runnable);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        File root = Environment.getExternalStorageDirectory();
        File dir = new File(root,"sensordrone data");
        File csvFile  = new File(dir,"sensordrone.csv");

        if(!dir.exists()){
            try{
                if(dir.mkdir()) {

                } else {
                    Toast.makeText(getApplicationContext(), "Directory Not Created", Toast.LENGTH_LONG).show();
                }
            }catch(Exception e){
                e.printStackTrace();
            }
            newFile = true;

        }
        try {
             writer = new FileWriter(csvFile,true);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // Get out Application so we have access to our Drone
        ActionBar ab = getActionBar();
        ColorDrawable colorDrawable = new ColorDrawable(Color.parseColor("#81a3d0"));
        ab.setBackgroundDrawable(colorDrawable);
        ab.setSplitBackgroundDrawable(colorDrawable);
        SharedPreferences pref = getApplicationContext().getSharedPreferences("User", 0);
        ACCOUNT=pref.getString("username",null);
        cardView = (CardView) findViewById(R.id.carddemo_weathercard);
        droneApp = (DroneApplication) getApplication();
// Initialize SharedPreferences
        sdcPreferences = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        mAccount = CreateSyncAccount(this);

        mUri = new Uri.Builder()
                .scheme(SCHEME)
                .authority(AUTHORITY)
                .path(TABLE_PATH)
                .build();
        initCards();
        Handler h=new Handler();
        TableObserver observer = new TableObserver(null);
        mResolver = getContentResolver();
         mResolver.registerContentObserver(mUri, true, observer);
        //mResolver.requestSync(mAccount,AUTHORITY,Bundle.EMPTY);
        // mResolver.registerContentObserver(mUri, true, observer);
        mResolver.setSyncAutomatically(mAccount, AUTHORITY, true);

        qsSensors = new int[] { droneApp.myDrone.QS_TYPE_TEMPERATURE,
                droneApp.myDrone.QS_TYPE_HUMIDITY,
                droneApp.myDrone.QS_TYPE_PRESSURE,
                droneApp.myDrone.QS_TYPE_IR_TEMPERATURE,
                droneApp.myDrone.QS_TYPE_RGBC,
                droneApp.myDrone.QS_TYPE_PRECISION_GAS,
                droneApp.myDrone.QS_TYPE_CAPACITANCE,
                droneApp.myDrone.QS_TYPE_ADC,
                droneApp.myDrone.QS_TYPE_ALTITUDE,
                droneApp.myDrone.QS_TYPE_OXIDIZING_GAS,
                droneApp.myDrone.QS_TYPE_REDUCING_GAS };


        // This will Blink our Drone, once a second, Blue
        myBlinker = new DroneStreamer(droneApp.myDrone, 1000) {
            @Override
            public void repeatableTask() {
                if (ledToggle) {
                    droneApp.myDrone.setLEDs(0, 0, 126);
                } else {
                    droneApp.myDrone.setLEDs(0,0,0);
                }
                ledToggle = !ledToggle;
            }
        };
        for (int i = 0; i < numberOfSensors; i++) {

            // The clickListener will need a final type of i
            final int counter = i;

            streamerArray[i] = new DroneQSStreamer(droneApp.myDrone, qsSensors[i]);
        }
        bvStreamer = new DroneStreamer(droneApp.myDrone, droneApp.defaultRate) {
            @Override
            public void repeatableTask() {
                droneApp.myDrone.measureBatteryVoltage();
            }
        };
        gpsSwitch = (Switch) findViewById(R.id.gpsSwitch);
        indoorLocation = (EditText)findViewById(R.id.indoorLocation);
        //set the switch to ON
        gpsSwitch.setChecked(true);
        indoorLocation.setEnabled(false);
        //attach a listener to check for changes in state
        gpsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                if(isSampling)
                    gpsSwitch.setChecked(!isChecked);
                else {
                    if (isChecked) {
                        gpsStatus=true;
                        indoorLocation.setEnabled(false);

                    } else {
                        gpsStatus=false;
                        indoorLocation.setEnabled(true);

                    }
                }

            }
        });
        tvStatus = (TextView)findViewById(R.id.tvStatus);
        tvSampling = (TextView)findViewById(R.id.tvSampling);


        deListener = new DroneEventListener() {

            @Override
            public void adcMeasured(DroneEventObject arg0) {
                // This is triggered the the external ADC pin is measured

                // Update our display with the measured value
                // Ask for another measurement
                // (droneApp.streamingRate has been set to 1 second, so
                // every time the ADC is measured
                // it will measure again in one second)
               sensordroneExternalVoltage= String.format("%.3f",
                        droneApp.myDrone.externalADC_Volts);

                streamerArray[7].streamHandler.postDelayed(streamerArray[7],
                        droneApp.streamingRate);
                updateDatabase();
            }

            @Override
            public void altitudeMeasured(DroneEventObject arg0) {
                int pref = sdcPreferences.getInt(SDPreferences.ALTITUDE_UNIT,
                        SDPreferences.FEET);



                streamerArray[8].streamHandler.postDelayed(streamerArray[8],
                        droneApp.streamingRate);

            }

            @Override
            public void capacitanceMeasured(DroneEventObject arg0) {
                sensordroneCapacitance=String.format("%.0f",
                        droneApp.myDrone.capacitance_femtoFarad);
                streamerArray[6].streamHandler.postDelayed(streamerArray[6],
                        droneApp.streamingRate);
                updateDatabase();

            }

            @Override
            public void connectEvent(DroneEventObject arg0) {

                // Since we are adding SharedPreferences to store unit
                // preferences,
                // we might as well store the last MAC there. Now we can
                // press re-connect
                // to always try and connect to the last Drone (not just the
                // last one per
                // app instance)

                SharedPreferences.Editor prefEditor = sdcPreferences.edit();
                prefEditor.putString(SDPreferences.LAST_MAC,
                        droneApp.myDrone.lastMAC);
                prefEditor.commit();

                // Things to do when we connect to a Sensordrone
                quickMessage("Connected!");
                tvUpdate(tvStatus, "Connected to: "
                        + droneApp.myDrone.lastMAC);
                sensordroneMAC=droneApp.myDrone.lastMAC;
                // Turn on our blinker
                myBlinker.start();
                // People don't need to know how to connect if they are
                // already connected
                // Notify if there is a low battery
                lowbatNotify = true;
            }

            @Override
            public void connectionLostEvent(DroneEventObject arg0) {

                // Things to do if we think the connection has been lost.

                // Turn off the blinker
                myBlinker.stop();

                // notify the user
                tvUpdate(tvStatus, "Connection Lost!");
                quickMessage("Connection lost! Trying to re-connect!");

                // Try to reconnect once, automatically
                if (droneApp.myDrone.btConnect(droneApp.myDrone.lastMAC)) {
                    // A brief pause
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    quickMessage("Re-connect failed");
                    doOnDisconnect();
                }
            }

            @Override
            public void customEvent(DroneEventObject arg0) {

            }

            @Override
            public void disconnectEvent(DroneEventObject arg0) {
                // notify the user
                quickMessage("Disconnected!");
                tvUpdate(tvStatus, "Disconnected");
            }

            @Override
            public void oxidizingGasMeasured(DroneEventObject arg0) {
                sensordroneOxidizingGas=String.format("%.0f", droneApp.myDrone.oxidizingGas_Ohm);
                streamerArray[9].streamHandler.postDelayed(streamerArray[9],
                        droneApp.streamingRate);
                updateDatabase();
            }

            @Override
            public void reducingGasMeasured(DroneEventObject arg0) {
                sensordroneReducingGas=String.format("%.0f", droneApp.myDrone.reducingGas_Ohm);
                streamerArray[10].streamHandler.postDelayed(streamerArray[10],
                        droneApp.streamingRate);
                updateDatabase();
            }

            @Override
            public void humidityMeasured(DroneEventObject arg0) {
                 sensordroneHumidity=String.format("%.1f", droneApp.myDrone.humidity_Percent);
                  streamerArray[1].streamHandler.postDelayed(streamerArray[1],
                        droneApp.streamingRate);
                updateDatabase();

            }

            @Override
            public void i2cRead(DroneEventObject arg0) {

            }

            @Override
            public void irTemperatureMeasured(DroneEventObject arg0) {
                sensordroneIRTemperature=String.format("%.1f",
                        droneApp.myDrone.irTemperature_Celsius);
                streamerArray[3].streamHandler.postDelayed(streamerArray[3],
                        droneApp.streamingRate);
                updateDatabase();

            }

            @Override
            public void precisionGasMeasured(DroneEventObject arg0) {
                  sensordroneprecisionGas=String.format("%.1f",
                          droneApp.myDrone.precisionGas_ppmCarbonMonoxide);

                   streamerArray[5].streamHandler.postDelayed(streamerArray[5],
                        droneApp.streamingRate);
                droneApp.myDrone.uartWrite("Z\r\n".getBytes());
                droneApp.myDrone.uartRead();
                    updateDatabase();
            }

            @Override
            public void pressureMeasured(DroneEventObject arg0) {

                sensordronePressure=String.format("%.2f",
                        droneApp.myDrone.pressure_Pascals / 100);
                streamerArray[2].streamHandler.postDelayed(streamerArray[2],
                        droneApp.streamingRate);
                updateDatabase();

            }

            @Override
            public void rgbcMeasured(DroneEventObject arg0) {
                // The Lux value is calibrated for a (mostly) broadband
                // light source.
                // Pointing it at a narrow band light source (like and LED)
                // will bias the color channels, and provide a "wonky"
                // number.
                // Just for a nice look, we won't show a negative number.
                String msg = "";
                if (droneApp.myDrone.rgbcLux >= 0) {
                    msg = String.format("%.0f", droneApp.myDrone.rgbcLux);
                } else {
                    msg = String.format("%.0f", 0.0) ;
                }
                sensordroneIlluminance=msg;
                streamerArray[4].streamHandler.postDelayed(streamerArray[4],
                        droneApp.streamingRate);
                updateDatabase();

            }

            @Override
            public void temperatureMeasured(DroneEventObject arg0) {

                sensordroneTemperature=String.format("%.1f",
                        droneApp.myDrone.temperature_Celsius);
                streamerArray[0].streamHandler.postDelayed(streamerArray[0],
                        droneApp.streamingRate);
                updateDatabase();

            }

            @Override
            public void uartRead(DroneEventObject arg0) {

                String result="-1";
                try {
                    int avail = droneApp.myDrone.uartInputStream.available();
                    boolean needData = true;
                    for (int i=0; i < avail; i++) {

                        if ((byte)droneApp.myDrone.uartInputStream.read() == 0x5a && i < avail-7 && needData) {
                            droneApp.myDrone.uartInputStream.read();
                            byte[] value = new byte[5];
                            droneApp.myDrone.uartInputStream.read(value);
                            result = new String(value);
                            i +=6;
                            needData = false;
                        }
                    }
                } catch (IOException e1) {

                }
                Long tempCO2 = Long.parseLong(result);
                if(tempCO2==-1)
                    sensordroneCO2 ="-";
                else
                    sensordroneCO2 = "" + tempCO2;

            }

            @Override
            public void unknown(DroneEventObject arg0) {

            }

            @Override
            public void usbUartRead(DroneEventObject arg0) {

            }
        };

		/*
		 * Set up our status listener
		 *
		 * see adcStatus for the general flow for sensors.
		 */
        dsListener = new DroneStatusListener() {

            @Override
            public void adcStatus(DroneEventObject arg0) {
                // This is triggered when the status of the external ADC has
                // been
                // enable, disabled, or checked.

                // If status has been triggered to true (on)
                if (droneApp.myDrone.adcStatus) {
                    // then start the streaming by taking the first
                    // measurement
                    streamerArray[7].run();
                }
                // Don't do anything if false (off)
            }

            @Override
            public void altitudeStatus(DroneEventObject arg0) {
                if (droneApp.myDrone.altitudeStatus) {
                    streamerArray[8].run();
                }

            }

            @Override
            public void batteryVoltageStatus(DroneEventObject arg0) {
                // This is triggered when the battery voltage has been
                // measured.
                String bVoltage = String.format("%.2f",
                        droneApp.myDrone.batteryVoltage_Volts) + " V";
                sensordroneBatteryVoltage=String.format("%.2f",
                        droneApp.myDrone.batteryVoltage_Volts);

                // We might need to update the rate due to graphing
                bvStreamer.setRate(droneApp.streamingRate);
            }

            @Override
            public void capacitanceStatus(DroneEventObject arg0) {
                if (droneApp.myDrone.capacitanceStatus) {
                    streamerArray[6].run();
                }
            }

            @Override
            public void chargingStatus(DroneEventObject arg0) {

            }

            @Override
            public void customStatus(DroneEventObject arg0) {

            }

            @Override
            public void humidityStatus(DroneEventObject arg0) {
                if (droneApp.myDrone.humidityStatus) {
                    streamerArray[1].run();
                }

            }

            @Override
            public void irStatus(DroneEventObject arg0) {
                if (droneApp.myDrone.irTemperatureStatus) {
                    streamerArray[3].run();
                }

            }

            @Override
            public void lowBatteryStatus(DroneEventObject arg0) {
                // If we get a low battery, notify the user
                // and disconnect

                // This might trigger a lot (making a call the the LEDS will
                // trigger it,
                // so the myBlinker will trigger this once a second.
                // calling myBlinker.disable() even sets LEDS off, which
                // will trigger it...

                // We wil also add in a voltage check, to allow users to use their
                // Sensordrone a little more
                if (lowbatNotify && droneApp.myDrone.batteryVoltage_Volts < 3.1) {
                    lowbatNotify = false; // Set true again in connectEvent
                    myBlinker.stop();
                    isSampling=false;
                    droneApp.myDrone.uartWrite("K 0\r\n".getBytes());

                    doOnDisconnect(); // run our disconnect routine
                    // Notify the user
                    quickMessage("Low Battery: Disconnecting..");
                    tvUpdate(tvStatus, "Low Battery: Disconnected!");
                    AlertInfo.lowBattery(MyActivity.this);
                }

            }

            @Override
            public void oxidizingGasStatus(DroneEventObject arg0) {
                if (droneApp.myDrone.oxidizingGasStatus) {
                    streamerArray[9].run();
                }
            }

            @Override
            public void precisionGasStatus(DroneEventObject arg0) {

                if (droneApp.myDrone.precisionGasStatus) {
                    streamerArray[5].run();
                }

            }

            @Override
            public void pressureStatus(DroneEventObject arg0) {
                if (droneApp.myDrone.pressureStatus) {
                    streamerArray[2].run();
                }

            }

            @Override
            public void reducingGasStatus(DroneEventObject arg0) {
                if (droneApp.myDrone.reducingGasStatus) {
                    streamerArray[10].run();
                }
            }

            @Override
            public void rgbcStatus(DroneEventObject arg0) {
                if (droneApp.myDrone.rgbcStatus) {
                    streamerArray[4].run();
                }

            }

            @Override
            public void temperatureStatus(DroneEventObject arg0) {
                if (droneApp.myDrone.temperatureStatus) {
                    streamerArray[0].run();
                }

            }

            @Override
            public void unknownStatus(DroneEventObject arg0) {

            }
        };
        droneApp.myDrone.registerDroneListener(deListener);
        droneApp.myDrone.registerDroneListener(dsListener);
        connectSensorDrone();
    }
    @Override
      public void onBackPressed() {
        if(!isSampling){
            super.onBackPressed();
        }
    }
    public static void refreshDisplay(final String[] values)
    {
        runOnUI(new Runnable() {
            public void run() {
                card.refresh( values);

                cardView.refreshCard(card);
            }
        });

    }
    private void initCards() {

        //Create a Card
         card = new SensorCard(this);
        card.init();
        // card.mObjects.get(0).
        //Set card in the cardView

        cardView.setCard(card);

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main_actions, menu);


        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.btConnect:
                connectSensorDrone();
                break;
            case R.id.action_start:
                // location found
                progressDialog = new ProgressDialog(getApplicationContext());
                progressDialog.setMessage("Initializing");
                if((indoorLocation.isEnabled()&&indoorLocation.getText().toString().contains(","))) {
                    indoors = indoorLocation.getText().toString().split(",");
                    if (indoors.length == 3 ) {
                        quickMessage("Starting...");
                        start();
                    }else{
                        quickMessage("Indoor Format Wrong");
                    }
                }
                else if(!indoorLocation.isEnabled())
                    start();
                else
               quickMessage("Indoor Location Missing");


                return true;
            case R.id.action_stop:
                stop();
                return true;
            case R.id.action_refresh:
                if(isSampling)
                    quickMessage("Cannot refresh while Sampling");
                else
                    refreshDisplay(new String[] {"--", "--.","--","--", "--.","--","--", "--.","--","--", "--.","--","--", "--.","--"});

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    // Measure the battery at the default rate (once a second)
    public void start(){

                if (droneApp.myDrone.isConnected) {
                    if (!isSampling) {
                        droneApp.myDrone.measureBatteryVoltage();
                        // droneApp.myDrone.measurePrecisionGas();
                        isSampling = true;
                        droneApp.myDrone.uartWrite("K 1\r\n".getBytes());
                        bvStreamer.start();
                        for (int i = 0; i < numberOfSensors; i++) {
                            streamerArray[i].enable();
                            // Enable the sensor
                            droneApp.myDrone
                                    .quickEnable(qsSensors[i]);
                        }
                        if (gpsStatus) {
                            startService(new Intent(getApplicationContext(), GPSService.class));
                            Toast.makeText(getApplicationContext(), GPSService.GPS_STATUS, Toast.LENGTH_SHORT).show();
                        }
                        bvStreamer.start();
                        droneApp.myDrone.enableTemperature();

                        for (int i = 0; i < numberOfSensors; i++) {
                            streamerArray[i].enable();
                            // Enable the sensor
                            droneApp.myDrone
                                    .quickEnable(qsSensors[i]);

                        }
                        tvUpdate(tvSampling, "Logging Data");

                    } else {
                        quickMessage("Already Logging");
                    }
                } else {
                    quickMessage("SensorDrone not connected. Please connect and try again.");
                }

    }
    public void stop()
    {
        isSampling=false;
        if(gpsStatus)
            stopService(new Intent(getApplicationContext(), GPSService.class));
        bvStreamer.stop();
        fTime=0;
        firstTime="";
        disconnectSensorDrone();
    }
    public class TableObserver extends ContentObserver {


        public TableObserver(Handler handler) {
            super(handler);
        }

        /*
                         * Define a method that's called when data in the
                         * observed content provider changes.
                         * This method signature is provided for compatibility with
                         * older platforms.
                         */
        @Override
        public void onChange(boolean selfChange) {
            /*
             * Invoke the method signature available as of
             * Android platform version 4.1, with a null URI.
             */
            onChange(selfChange, null);
        }
        /*
         * Define a method that's called when data in the
         * observed content provider changes.
         */
        @Override
        public void onChange(boolean selfChange, Uri changeUri) {
            ContentResolver.requestSync(mAccount, AUTHORITY, null);
        }
    }
    public static Account CreateSyncAccount(Context context) {
        // Create the account type and default account
        Account newAccount = new Account(
                ACCOUNT, ACCOUNT_TYPE);
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(
                        ACCOUNT_SERVICE);
        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
        if (accountManager.addAccountExplicitly(newAccount, null, null)) {
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call
             * context.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */

        } else {
            /*
             * The account exists or some other error occurred. Log this, report it,
             * or handle it internally.
             */
        }
        return newAccount;
    }



     public  void connectSensorDrone() {
         if (!droneApp.myDrone.isConnected) {

                     myHelper.connectFromPairedDevices(droneApp.myDrone, MyActivity.this);

             // We now just use paired devices instead of scanning every time
//				myHelper.scanToConnect(droneApp.myDrone,
//                        SensordroneControl.this, this, false);
             // Enable our steamer

             // Measure the voltage once to trigger streaming

         } else {
             quickMessage("Please disconnect first");
         }
     }

    public void disconnectSensorDrone() {
        if (droneApp.myDrone.isConnected) {

            // Stop taking measurements

            // Run our routine of things to do on disconnect
            isSampling=false;
            for(int i=0;i<numberOfSensors;i++) {
                streamerArray[i].disable();

                // Disable the sensor
                droneApp.myDrone
                        .quickDisable(qsSensors[i]);
            }
            doOnDisconnect();
        } else {
            quickMessage("Not currently connected..");
        }
    }
    public void doOnDisconnect() {
        // Shut off any sensors that are on
        MyActivity.this.runOnUiThread(new Runnable() {

            @Override
            public void run() {

                // Turn off myBlinker
                myBlinker.stop();

                // Make sure the LEDs go off
                if (droneApp.myDrone.isConnected) {
                    droneApp.myDrone.setLEDs(0, 0, 0);
                }


                // Only try and disconnect if already connected
                if (droneApp.myDrone.isConnected) {
                    droneApp.myDrone.disconnect();
                }

                // Remind people how to connect
                tvStatus.setText("Disconnected");
                tvSampling.setText("Not Sampling");
            }
        });

    }
    public void quickMessage(final String msg) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT)
                        .show();
            }
        });

    }
    public void tvUpdate(final TextView tv, final String msg) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                tv.setText(msg);
            }
        });
    }
    public void updateDatabase() {
        if(firstLog==true) {
            isSampling = true;
            quickMessage("Started Logging");
            firstLog=false;
        }
        String lat="",lon="",alt="";
        if(!gpsStatus) {
            refreshDisplay(new String[] {"n.a.", "n.a.","n.a.",sensordroneTemperature, sensordroneHumidity,sensordronePressure,sensordroneIRTemperature,sensordroneIlluminance,sensordroneprecisionGas,sensordroneCO2,sensordroneCapacitance,sensordroneOxidizingGas,sensordroneReducingGas,sensordroneExternalVoltage,sensordroneBatteryVoltage});

            timeStamp = s.format(new Date());
            if(fTime==0)
            {
                progressDialog.dismiss();
                firstTime=timeStamp;
                fTime=1;
            }
            ContentValues values = new ContentValues();
            values.put(Provider.timestamp, timeStamp);
            values.put(Provider.firstTime,firstTime);
            values.put(Provider.sensordronePrecisionGas, sensordroneprecisionGas);
            values.put(Provider.sensordroneBatteryVoltage, sensordroneBatteryVoltage);
            values.put(Provider.sensordroneCapacitance, sensordroneCapacitance);
            values.put(Provider.sensordroneExternalVoltage, sensordroneExternalVoltage);
            values.put(Provider.sensordroneHumidity, sensordroneHumidity);
            values.put(Provider.sensordroneIlluminance, sensordroneIlluminance);
            values.put(Provider.sensordroneIRTemperature, sensordroneIRTemperature);
            values.put(Provider.sensordroneMAC, sensordroneMAC);
            values.put(Provider.sensordronePressure, sensordronePressure);
            values.put(Provider.sensordroneTemperature, sensordroneTemperature);
            values.put(Provider.sensordroneOxidizingGas,sensordroneOxidizingGas);
            values.put(Provider.sensordroneReducingGas,sensordroneReducingGas);
            values.put(Provider.sensordroneCO2,sensordroneCO2);
            values.put(Provider.gpsLatitude, indoors[0]);
            values.put(Provider.gpsLongitude, indoors[1]);
            values.put(Provider.gpsAltitude, indoors[2]);
            lat=indoors[0];
            lon= indoors[1];
            alt=indoors[2];
            Uri uri = getContentResolver().insert(com.snu.msl.sensys.SyncAdapter.Provider.Provider.CONTENT_URI, values);
        }
        if(gpsStatus)
        {
            if(GPSService.isGPSFix)
            { refreshDisplay(new String[] {""+(float)GPSService.mLastLocation.getLatitude(), ""+(float)GPSService.mLastLocation.getLongitude(),""+(float)GPSService.mLastLocation.getAltitude(),sensordroneTemperature, sensordroneHumidity,sensordronePressure,sensordroneIRTemperature,sensordroneIlluminance,sensordroneprecisionGas,sensordroneCO2,sensordroneCapacitance,sensordroneOxidizingGas,sensordroneReducingGas,sensordroneExternalVoltage,sensordroneBatteryVoltage});

                timeStamp = s.format(new Date());
                if(fTime==0)
                {
                    firstTime=timeStamp;
                    fTime=1;
                }
                ContentValues values = new ContentValues();
                values.put(Provider.timestamp, timeStamp);
                values.put(Provider.firstTime,firstTime);
                values.put(Provider.sensordronePrecisionGas, sensordroneprecisionGas);
                values.put(Provider.sensordroneBatteryVoltage, sensordroneBatteryVoltage);
                values.put(Provider.sensordroneCapacitance, sensordroneCapacitance);
                values.put(Provider.sensordroneExternalVoltage, sensordroneExternalVoltage);
                values.put(Provider.sensordroneHumidity, sensordroneHumidity);
                values.put(Provider.sensordroneIlluminance, sensordroneIlluminance);
                values.put(Provider.sensordroneIRTemperature, sensordroneIRTemperature);
                values.put(Provider.sensordroneMAC, sensordroneMAC);
                values.put(Provider.sensordronePressure, sensordronePressure);
                values.put(Provider.sensordroneTemperature, sensordroneTemperature);
                values.put(Provider.sensordroneOxidizingGas,sensordroneOxidizingGas);
                values.put(Provider.sensordroneReducingGas,sensordroneReducingGas);
                values.put(Provider.sensordroneCO2,sensordroneCO2);
                values.put(Provider.gpsLatitude, ""+GPSService.mLastLocation.getLatitude());
                values.put(Provider.gpsLongitude, ""+GPSService.mLastLocation.getLongitude());
                values.put(Provider.gpsAltitude, ""+GPSService.mLastLocation.getAltitude());
                lat=""+GPSService.mLastLocation.getLatitude();
                lon=""+GPSService.mLastLocation.getLongitude();
                alt=""+GPSService.mLastLocation.getAltitude();

                Uri uri = getContentResolver().insert(com.snu.msl.sensys.SyncAdapter.Provider.Provider.CONTENT_URI, values);
            }
        }
        try {

            if (newFile)
            {
                String line = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n", "Time","Lat","Lon","Alt","MAC","SD Temp","SD Pressure","SD Humidity","SD IR Temperature","SD Illuminance","SD Precision Gas","SD Capacitance","SD External Voltage","Battery Voltage","SD Oxidizing Gas","SD Reducing Gas","SD CO2");
                writer.write(line);
                newFile=false;
            }

            String line = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n", timeStamp, lat,lon,alt,sensordroneMAC,sensordroneTemperature,sensordronePressure,sensordroneHumidity,sensordroneIRTemperature,sensordroneIlluminance,sensordroneprecisionGas,sensordroneCapacitance,sensordroneExternalVoltage,sensordroneBatteryVoltage,sensordroneOxidizingGas,sensordroneReducingGas,sensordroneCO2);
            writer.write(line);
        }catch (IOException e) {
            e.printStackTrace();
        }
        try {
            writer.flush();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
