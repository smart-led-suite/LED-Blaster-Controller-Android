package de.danielbuecheler.ledblastercontroller;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class FadeModesFragment extends Fragment implements View.OnClickListener {
    final String TAG = "FadeModesFragment";

    SharedPreferences prefs;

    int fadetime;
    private static final int TIME_CHANGE_FACTOR = 50;

    public FadeModesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Context context = getActivity().getApplicationContext();
        Resources res = getResources();

        // Inflate the layout for this fragment
        View returnView = inflater.inflate(R.layout.fragment_fade_modes, container, false);

        ConnectivityManager connMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        // Check for network connection, if not connected show toast
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (!(networkInfo != null && networkInfo.isConnected())) {
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, res.getText(R.string.toast_no_network), duration);
            toast.show();
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());

        Button btn;

        // continuous fade buttons
        btn = (Button) returnView.findViewById(R.id.btn_mode1);
        btn.setOnClickListener(this);
        btn = (Button) returnView.findViewById(R.id.btn_modes_off);
        btn.setOnClickListener(this);
        // increment / decrement buttons for time
        ImageButton i_btn;
        i_btn = (ImageButton) returnView.findViewById(R.id.btn_inc_time);
        i_btn.setOnClickListener(this);
        i_btn = (ImageButton) returnView.findViewById(R.id.btn_dec_time);
        i_btn.setOnClickListener(this);

        return returnView;
    }

    @Override
    public void onStart() {
        super.onStart();

        fadetime = Integer.valueOf(prefs.getString("pref_fadetime", "500"));

        // initialize edittext for fade time
        EditText et = (EditText) getView().findViewById(R.id.et_time);
        et.setText(String.valueOf(fadetime));
        et.setSelection(et.getText().toString().length());
    }


    @Override
    public void onClick(View v) {
        EditText et;

        switch(v.getId()) {
            case R.id.btn_mode1: // button pressed == btn for mode 1

                et = (EditText) getView().findViewById(R.id.et_time);
                int fadetime_continuous = Integer.valueOf(et.getText().toString());

                // take focus from et
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(et.getWindowToken(), 0);

                SetModeTask set_mode_task = new SetModeTask();
                set_mode_task.execute(1, 1, fadetime_continuous); // start fade mode 1 ([0]=1 for turning fade mode on
                break;
            case R.id.btn_modes_off:
                set_mode_task = new SetModeTask();
                set_mode_task.execute(0); // 2500 is placeholder, not needed
                break;
            case R.id.btn_inc_time:
                et = (EditText) getView().findViewById(R.id.et_time);
                et.setText(String.valueOf(Integer.valueOf(
                        et.getText().toString()) + TIME_CHANGE_FACTOR));
                break;
            case R.id.btn_dec_time:
                et = (EditText) getView().findViewById(R.id.et_time);
                et.setText(String.valueOf(Integer.valueOf(
                        et.getText().toString()) - TIME_CHANGE_FACTOR));
                break;

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
                Toast toast = Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.toast_fademode_started), Toast.LENGTH_SHORT); // show toast that the LEDs are fading
                toast.show();
            } else { // if error occurred
                String message = String.format(getResources().getString(R.string.toast_error_http), result);

                Toast toast = Toast.makeText(getActivity().getApplicationContext(), message, Toast.LENGTH_LONG); // show toast that an error occured, including error / status code
                toast.show();
            }
        }
    }
}
