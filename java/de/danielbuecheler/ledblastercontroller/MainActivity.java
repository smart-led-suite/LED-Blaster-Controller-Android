package de.danielbuecheler.ledblastercontroller;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    final String TAG = "MainActivity";
    private static final int TIME_CHANGE_FACTOR = 50;

    ArrayList<Boolean> cb_states = new ArrayList<>();
    ArrayList<Integer> sb_states = new ArrayList<>();

    SharedPreferences prefs;
    LEDItemAdapter adapter;

    boolean twoFloors;
    int fadetime;
    private LinearLayout ll_steady_lighting;
    private int adapterCount;
    private LEDItem ledItems[];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Context context = getApplicationContext();
        Resources res = getResources();

        Intent intent = new Intent(context, FadeBackgroundService.class);

        intent.putExtra(FadeBackgroundService.EXTRA_ACTION, "OFF");
        intent.putExtra(FadeBackgroundService.EXTRA_COLOR, "G");
        startService(intent);

        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        // Check for network connection, if not connected show toast
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (!(networkInfo != null && networkInfo.isConnected())) {
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, res.getText(R.string.toast_no_network), duration);
            toast.show();
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // ##### SET ON CLICK LISTENERS #####
        // initialize buttons
        // apply button
        Button btn;
        btn = (Button) findViewById(R.id.btn_apply);
        btn.setOnClickListener(this);
        // continuous fade buttons
        btn = (Button) findViewById(R.id.btn_mode1);
        btn.setOnClickListener(this);
        btn = (Button) findViewById(R.id.btn_modes_off);
        btn.setOnClickListener(this);
        btn = (Button) findViewById(R.id.btn_all_off);
        btn.setOnClickListener(this);

        // increment / decrement buttons for time
        ImageButton i_btn;
        i_btn = (ImageButton) findViewById(R.id.btn_inc_time);
        i_btn.setOnClickListener(this);
        i_btn = (ImageButton) findViewById(R.id.btn_dec_time);
        i_btn.setOnClickListener(this);

        adapter = new LEDItemAdapter(this);

        ll_steady_lighting = (LinearLayout) findViewById(R.id.linear_layout_steady_lighting);

        adapterCount = adapter.getCount();
        ledItems = new LEDItem[adapterCount];

        for(int position = 0; position < adapterCount; position++) {
            View item = adapter.getView(position, null, null);

            ledItems[position] = (LEDItem) adapter.getItem(position);

            // initialize slider
            SeekBar sb = (SeekBar) item.findViewById(R.id.sb_value);
            sb.setOnSeekBarChangeListener(this);

            item.setId(position);

            // initialize checkbox
            CheckBox cb = (CheckBox) item.findViewById(R.id.cb_change);
            cb.setActivated(true);

            // add to Linear Layout view
            ll_steady_lighting.addView(item, position + 1);
            ll_steady_lighting.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        }



}

    @Override
    protected void onStart() {
        super.onStart();
        twoFloors = prefs.getBoolean("pref_two_floors", false);

        fadetime = Integer.valueOf(prefs.getString("pref_fadetime", "500"));

        // initialize edittext for fade time
        EditText et = (EditText) findViewById(R.id.et_time);
        et.setText(String.valueOf(fadetime));
        et.setSelection(et.getText().toString().length());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }
        Log.d(TAG, "optionsItemSelected");

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        EditText et;
        int button = 2; // which fademode button is pressed
        SteadyFadeTask task;
        LEDTarget targets[] = new LEDTarget[adapterCount];
        switch(v.getId()) {
            case R.id.btn_apply: // if the button pressed is the apply button
                // deactivate the Button while fading...
                Button btn = (Button) v; // we have to cast it to a button so we can call setActivated()
                btn.setActivated(false);

                CheckBox cb; // var for the checkboxes i have to check in a second
                SeekBar sb;
                // read value from all checkboxes
                for(int item = 0; item < adapterCount; item++) {
                    ViewGroup itemLayout = (ViewGroup) ll_steady_lighting.getChildAt(item + 1);
                    cb = (CheckBox) itemLayout.findViewById(R.id.cb_change);
                    sb = (SeekBar) itemLayout.findViewById(R.id.sb_value);
                    if(cb.isChecked()) { // if checked, set to the value of the slider
                        sb_states.add(sb.getProgress());
                    } else {
                        sb_states.add(-1);
                    }
                }

                Log.d(TAG, "LED_states:");
                for(int val : sb_states) {
                    Log.d(TAG, String.valueOf(val));
                }

                SteadyFadeTask fade_task = new SteadyFadeTask();



                for(int item = 0; item < adapterCount; item++) {
                    targets[item] = new LEDTarget(ledItems[item].getShortName(), sb_states.get(item));
                }
                fade_task.execute(targets);

//                try {
//                    Log.d(TAG, String.valueOf(fade_task.get()));
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                } catch (ExecutionException e) {
//                    e.printStackTrace();
//                }

                resetUI();
                break;

            case R.id.btn_mode1: // button pressed == btn for mode 1
                button = 1;

                et = (EditText) findViewById(R.id.et_time);
                int fadetime_continuous = Integer.valueOf(et.getText().toString());

                // take focus from et
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(et.getWindowToken(), 0);

                SetModeTask set_mode_task = new SetModeTask();
                set_mode_task.execute(1, button, fadetime_continuous); // start fade mode 1 ([0]=1 for turning fade mode on
                break;
            case R.id.btn_modes_off:
                set_mode_task = new SetModeTask();
                set_mode_task.execute(0); // 2500 is placeholder, not needed
                break;
            case R.id.btn_all_off:
                task = new SteadyFadeTask();
                for(int item = 0; item < adapterCount; item++) {
                    targets[item] = new LEDTarget(ledItems[item].getShortName(), 0);
                }
                task.execute(targets);
                break;
            case R.id.btn_inc_time:
                et = (EditText) findViewById(R.id.et_time);
                et.setText(String.valueOf(Integer.valueOf(
                                et.getText().toString()) + TIME_CHANGE_FACTOR));
                break;
            case R.id.btn_dec_time:
                et = (EditText) findViewById(R.id.et_time);
                et.setText(String.valueOf(Integer.valueOf(
                        et.getText().toString()) - TIME_CHANGE_FACTOR));
                break;
        }
    }

    private void resetUI() {
        sb_states = new ArrayList<>(); // reset sb_states to have all 0s

        for(int item = 0; item < adapterCount; item++) {
            ViewGroup itemLayout = (ViewGroup) ll_steady_lighting.getChildAt(item + 1);
            // reset seekbar
            SeekBar sb = (SeekBar) itemLayout.findViewById(R.id.sb_value);
            sb.setProgress(0);
            // reset checkbox
            CheckBox cb = (CheckBox) itemLayout.findViewById(R.id.cb_change);
            cb.setChecked(false);
        }


    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        Log.v(TAG, "seekbar changed: " + progress + " sb: " + ((RelativeLayout) seekBar.getParent()).getId());
        ViewGroup parent = (ViewGroup) seekBar.getParent();
        int id = parent.getId();
        LEDItem ledItem = (LEDItem) adapter.getItem(id);
        String shortName = ledItem.getShortName();

        TextView tv = (TextView) parent.findViewById(R.id.tv_name);
        CheckBox cb = (CheckBox) parent.findViewById(R.id.cb_change);

        // update text
        String text = String.format(getResources().getString(R.string.tv_name), ledItem.getName(), progress);
        tv.setText(text);

        // check checkbox if input is coming from user
        if(fromUser) {
            cb.setChecked(true);
        }

        //Log.v(TAG, String.valueOf(sb_states.get(1)));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    private class SteadyFadeTask extends AsyncTask<LEDTarget,Void,Integer> {
        @Override
        protected Integer doInBackground(LEDTarget... params) {
            int response = 0;
            try {
                String url_http = prefs.getString("pref_url", "").trim(); // remove whitespaces at beginning and end
                String url_get;
                String url_complete;

                URL url;
                HttpURLConnection con;

                if(!url_http.endsWith("?")) // add "?" to begin GET if not already existing
                    url_http = url_http.concat("?");
                if(!url_http.startsWith("http://"))
                    url_http = "http://".concat(url_http); // add "http://" to the beginning

//                // TURN MODE 0 ON
//                url_get = String.format(getResources().getString(R.string.url_get_fademode), 0, 1); // set to mode 0 in 1 ms
//                url_complete = url_http.concat(url_get);
////                Log.d(TAG, url_complete);
//                url = new URL(url_complete);
//                // send request
//                con = (HttpURLConnection) url.openConnection();
//                con.setReadTimeout(10000 /* millis */);
//                con.setConnectTimeout(15000 /* millis */);
//                con.connect();
//                response = con.getResponseCode();
//                Log.d(TAG, "Response code: " + response);

                url_get = "time=" + fadetime;

                // FADE LEDs
                // prepare network request
                //url_get = String.format(getResources().getString(R.string.url_get_steady), led_vals[0], led_vals[1], led_vals[2], led_vals[3], fadetime);
                for(LEDTarget target : params) {
                    url_get = url_get.concat(String.format("&%s=%d", target.getShortName(), target.getTargetValue()));
                }
//                if(url_get.endsWith("&")) {
//                    url_get = url_get.substring(url_get.lastIndexOf("&"), url_get.length());
//                }
                Log.d(TAG, url_get);
                url_complete = url_http.concat(url_get);

                Log.d(TAG, url_complete);

                url = new URL(url_complete);

                // send request
                con = (HttpURLConnection) url.openConnection();
                con.setReadTimeout(10000 /* millis */);
                con.setConnectTimeout(15000 /* millis */);
                con.connect();

                response = con.getResponseCode();
                Log.d(TAG, "Response code: " + response);
            } catch(Exception e) {
                Log.d(TAG, e.toString());
            }
            return response;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if(result >= 200 && result < 300) { // if http status code is 2xx (success)
                Toast toast = Toast.makeText(getApplicationContext(), getResources().getString(R.string.toast_fading_leds), Toast.LENGTH_SHORT); // show toast that the LEDs are fading
                toast.show();
            } else { // if error occurred
                String message = String.format(getResources().getString(R.string.toast_error_http), result);
                Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG); // show toast that an error occured, including error / status code
                toast.show();
            }

            // reactivate button
            findViewById(R.id.btn_apply).setActivated(true);

        }
    }

    private class SetModeTask extends AsyncTask<Integer, Void, Integer> {
        @Override
        protected Integer doInBackground(Integer... params) {
            int response = 404; // if it fails we have to assume 404 not found
            try {
                // prepare network request
                String url_http = prefs.getString("pref_url", "").trim(); // remove whitespaces at beginning and end

                if(!url_http.endsWith("?")) // add "?" to begin GET if not already existing
                    url_http = url_http.concat("?");
                if(!url_http.startsWith("http://"))
                    url_http = "http://".concat(url_http); // add "http://" to the beginning


                String url_get;
                if(params[0] != 0) {
                    url_get = String.format(getResources().getString(R.string.url_get_fademode), params[1], params[2]);
                } else {
                    url_get = String.format(getResources().getString(R.string.url_get_fademode), 0, 0);
                }
                String url_complete = url_http.concat(url_get);

                Log.d(TAG, url_complete);

                URL url = new URL(url_complete);

                // connect to fade.php
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setReadTimeout(10000 /* millis */);
                con.setConnectTimeout(15000 /* millis */);
                con.connect();

                response = con.getResponseCode();
                Log.d(TAG, "Response code: " + response);
            } catch(Exception e) {
                Log.d(TAG, e.toString());
            }
            return response;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if(result >= 200 && result < 300) { // if http status code is 2xx (success)
                Toast toast = Toast.makeText(getApplicationContext(), getResources().getString(R.string.toast_fademode_started), Toast.LENGTH_SHORT); // show toast that the LEDs are fading
                toast.show();
            } else { // if error occurred
                String message = String.format(getResources().getString(R.string.toast_error_http), result);

                Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG); // show toast that an error occured, including error / status code
                toast.show();
            }
        }
    }
}
