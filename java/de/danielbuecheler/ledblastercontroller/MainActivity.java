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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    final String TAG = "MainActivity";
    private static final int TIME_CHANGE_FACTOR = 50;

    boolean[] cb_states = new boolean[4];
    int[] sb_states = new int[4];

    SharedPreferences prefs;

    boolean twoFloors;
    int fadetime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Context context = getApplicationContext();
        Resources res = getResources();

        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        // Check for network connection, if not connected show toast
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (!(networkInfo != null && networkInfo.isConnected())) {
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, res.getText(R.string.toast_no_network), duration);
            toast.show();
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // change the text for white to add "upstairs" if ticked in the settings
        twoFloors = prefs.getBoolean("pref_two_floors", false);
        if(twoFloors) {
            TextView tv_white = (TextView) findViewById(R.id.tv_white);
            tv_white.setText(res.getString(R.string.white_desc_upstairs));
            Log.d(TAG, "twoFloors == true");
        }


        // ##### SET ON CLICK LISTENERS #####
        // initialize buttons
        // apply button
        Button btn;
        btn = (Button) findViewById(R.id.btn_apply);
        btn.setOnClickListener(this);
        // continuous fade buttons
        btn = (Button) findViewById(R.id.btn_mode1);
        btn.setOnClickListener(this);
        btn = (Button) findViewById(R.id.btn_mode2);
        btn.setOnClickListener(this);
        btn = (Button) findViewById(R.id.btn_stop_fademode);
        btn.setOnClickListener(this);

        // increment / decrement buttons for time
        ImageButton i_btn;
        i_btn = (ImageButton) findViewById(R.id.btn_inc_time);
        i_btn.setOnClickListener(this);
        i_btn = (ImageButton) findViewById(R.id.btn_dec_time);
        i_btn.setOnClickListener(this);

        // initialize all CheckBoxes
        CheckBox cb;

        cb = (CheckBox) findViewById(R.id.cb_white);
        cb.setActivated(true);

        // initialize all seekbars
        SeekBar sb;

        sb = (SeekBar) findViewById(R.id.sb_white);
        sb.setOnSeekBarChangeListener(this);
        onProgressChanged(sb, 0, false);
        sb = (SeekBar) findViewById(R.id.sb_red);
        sb.setOnSeekBarChangeListener(this);
        onProgressChanged(sb, 0, false);
        sb = (SeekBar) findViewById(R.id.sb_green);
        sb.setOnSeekBarChangeListener(this);
        onProgressChanged(sb, 0, false);
        sb = (SeekBar) findViewById(R.id.sb_blue);
        sb.setOnSeekBarChangeListener(this);
        onProgressChanged(sb, 0, false);



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
        switch(v.getId()) {
            case R.id.btn_apply: // if the button pressed is the apply button
                // deactivate the Button while fading...
                Button btn = (Button) v; // we have to cast it to a button so we can call setActivated()
                btn.setActivated(false);

                CheckBox cb; // var for the checkboxes i have to check in a second

                // set the val to -1 if checkbox is NOT checked
                cb = (CheckBox) findViewById(R.id.cb_white);
                if(!cb.isChecked())
                    sb_states[0] = -1;
                cb = (CheckBox) findViewById(R.id.cb_red);
                if(!cb.isChecked())
                    sb_states[1] = -1;
                cb = (CheckBox) findViewById(R.id.cb_green);
                if(!cb.isChecked())
                    sb_states[2] = -1;
                cb = (CheckBox) findViewById(R.id.cb_blue);
                if(!cb.isChecked())
                    sb_states[3] = -1;

                Log.d(TAG, "LED_states:");
                for(int val : sb_states) {
                    Log.d(TAG, String.valueOf(val));
                }

                SteadyFadeTask fade_task = new SteadyFadeTask();
                fade_task.execute(String.valueOf(sb_states[0]), String.valueOf(sb_states[1]), String.valueOf(sb_states[2]), String.valueOf(sb_states[3]));

                try {
                    Log.d(TAG, String.valueOf(fade_task.get()));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }

                resetUI();
                break;

            case R.id.btn_mode1: // button pressed == btn for mode 1
                button = 1;
            case R.id.btn_mode2:
                et = (EditText) findViewById(R.id.et_time);
                int fadetime_continuous = Integer.valueOf(et.getText().toString());

                // take focus from et
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(et.getWindowToken(), 0);

                SetModeTask set_mode_task = new SetModeTask();
                set_mode_task.execute(button, fadetime_continuous); // start fade mode 1
                break;
            case R.id.btn_stop_fademode:
                SteadyFadeTask task = new SteadyFadeTask();
                task.execute("0", "0", "0", "0");
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
        sb_states = new int[4]; // reset sb_states to have all 0s

        // reset seekbars before checkboxes, as the seekbar Listener checks the boxes
        SeekBar sb;
        sb = (SeekBar) findViewById(R.id.sb_white);
        sb.setProgress(0);
        sb = (SeekBar) findViewById(R.id.sb_red);
        sb.setProgress(0);
        sb = (SeekBar) findViewById(R.id.sb_green);
        sb.setProgress(0);
        sb = (SeekBar) findViewById(R.id.sb_blue);
        sb.setProgress(0);

        // Reset all Checkboxes
        CheckBox cb;
        cb = (CheckBox) findViewById(R.id.cb_white);
        cb.setChecked(false);
        cb = (CheckBox) findViewById(R.id.cb_red);
        cb.setChecked(false);
        cb = (CheckBox) findViewById(R.id.cb_green);
        cb.setChecked(false);
        cb = (CheckBox) findViewById(R.id.cb_blue);
        cb.setChecked(false);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        Log.v(TAG, "seekbar changed");

        int str_id = 0; // string id
        int tv_id = 0; // textview id
        int cb_id = 0; // checkbox id

        TextView tv;
        CheckBox cb;
        switch(seekBar.getId()) {
            case R.id.sb_white:
                sb_states[0] = progress;
                // set IDs
                if(!twoFloors) {
                    str_id = R.string.white_desc;
                } else {
                    str_id = R.string.white_desc_upstairs;
                }
                cb_id = R.id.cb_white;
                tv_id = R.id.tv_white;
                break;
            case R.id.sb_red:
                sb_states[1] = progress;
                // set IDs
                str_id = R.string.red_desc;
                cb_id = R.id.cb_red;
                tv_id = R.id.tv_red;
                break;
            case R.id.sb_green:
                sb_states[2] = progress;
                // set IDs
                str_id = R.string.green_desc;
                cb_id = R.id.cb_green;
                tv_id = R.id.tv_green;
                break;
            case R.id.sb_blue:
                sb_states[3] = progress;
                // set IDs
                str_id = R.string.blue_desc;
                cb_id = R.id.cb_blue;
                tv_id = R.id.tv_blue;
                break;
            default:
                return; // if none of the checkboxes -> abort
        }
        // update text
        String text = String.format(getResources().getString(str_id), progress);
        tv = (TextView) findViewById(tv_id);
        tv.setText(text);

        // check checkbox if input is coming from user
        if(fromUser) {
            cb = (CheckBox) findViewById(cb_id);
            cb.setChecked(true);
        }

        Log.v(TAG, String.valueOf(sb_states[1]));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    private class SteadyFadeTask extends AsyncTask<String, Void, Integer> {
        @Override
        protected Integer doInBackground(String... led_vals) {
            int response = 404;
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

                // TURN MODE 0 ON
                url_get = String.format(getResources().getString(R.string.url_get_fademode), 0, 1); // set to mode 0 in 1 ms
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

                // FADE LEDs
                // prepare network request
                url_get = String.format(getResources().getString(R.string.url_get_steady), led_vals[0], led_vals[1], led_vals[2], led_vals[3], fadetime);
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

                String url_get = String.format(getResources().getString(R.string.url_get_fademode), params[0], params[1]);
                String url_complete = url_http.concat(url_get);

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
