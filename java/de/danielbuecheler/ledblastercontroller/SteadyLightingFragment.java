package de.danielbuecheler.ledblastercontroller;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

public class SteadyLightingFragment extends Fragment implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    final String TAG = "SteadyLightingFragment";

    ArrayList<Integer> sb_states = new ArrayList<>();

    HashMap<String, Integer> codeToNumber;

    SharedPreferences prefs;
    LEDItemAdapter adapter;

    boolean twoFloors;
    int fadetime;
    private LinearLayout ll_steady_lighting;
    private int adapterCount;
    private LEDItem ledItems[];

    boolean inHomeNetwork = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Context context = getActivity().getApplicationContext();
        Resources res = getResources();

        prefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());

        View returnView = inflater.inflate(R.layout.fragment_steady_lighting, container, false);

        ConnectivityManager connMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        WifiManager wifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        String homeSSID = prefs.getString("pref_home_network", "");

        // Check for network connection, if not connected show toast
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (!(networkInfo != null && networkInfo.isConnected())) {
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, res.getText(R.string.toast_no_network), duration);
            toast.show();
        }

        // check if we are in home wifi
        String actualSSID = wifiInfo.getSSID();
        inHomeNetwork = actualSSID.equals("\"" + homeSSID + "\""); // getSSID delivers it with quotation marks
        Log.d(TAG, "in home network: " + inHomeNetwork);






        // ##### SET ON CLICK LISTENERS #####
        // initialize buttons
        // apply button
        Button btn;
        btn = (Button) returnView.findViewById(R.id.btn_apply);
        btn.setOnClickListener(this);
        btn = (Button) returnView.findViewById(R.id.btn_all_off);
        btn.setOnClickListener(this);



        adapter = new LEDItemAdapter(this.getActivity());

        ll_steady_lighting = (LinearLayout) returnView.findViewById(R.id.linear_layout_steady_lighting);

        adapterCount = adapter.getCount();
        if(adapterCount <= 0) {
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, res.getText(R.string.toast_no_led_data), duration);
            toast.show();
            adapter.update(this.getActivity());
            adapterCount = adapter.getCount();
        }
        ledItems = new LEDItem[adapterCount];
        codeToNumber = new HashMap<>();


        // initialize each LED
        for(int position = 0; position < adapterCount; position++) {
            View item = adapter.getView(position, null, null);

            ledItems[position] = (LEDItem) adapter.getItem(position);

            codeToNumber.put(ledItems[position].getShortName(), position);

            // initialize slider
            SeekBar sb = (SeekBar) item.findViewById(R.id.sb_value);
            sb.setOnSeekBarChangeListener(this);

            item.setId(position);

            // add to Linear Layout view
            ll_steady_lighting.addView(item, position + 1);
            ll_steady_lighting.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        }

        Log.v(TAG, "finished creating");

        // update LEDs if in home network
        if(inHomeNetwork) {
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, res.getText(R.string.toast_updating_leds), duration);
            toast.show();
            adapter.update(this.getActivity());
        } else { // show warning if not in home network
            Toast toast = Toast.makeText(context, res.getText(R.string.toast_not_home_network), Toast.LENGTH_LONG);
            toast.show();
        }

        return returnView;
    }


    @Override
    public void onStart() {
        super.onStart();
        twoFloors = prefs.getBoolean("pref_two_floors", false);

        fadetime = Integer.valueOf(prefs.getString("pref_fadetime", "500"));
        try {
            if(inHomeNetwork) {
                updateSeekBars();
            }
        } catch(NullPointerException e) {
            Log.e(TAG, "toasting");

            Toast.makeText(getActivity().getApplicationContext(), R.string.toast_error_parsing, Toast.LENGTH_LONG).show();
        }
    }

    public void resetSeekBars() {
        for(int i = 0; i < ledItems.length; i++) {
            Log.v(TAG, "updating sb");
            SeekBar sb = (SeekBar) ll_steady_lighting.getChildAt(i + 1).findViewById(R.id.sb_value);
            sb.setProgress(0);
        }
    }

    public void updateSeekBars() throws NullPointerException {
        Log.v(TAG, "started updating..");
        String url_current_brightnesses = prefs.getString("pref_brightness_url", "").trim();
        Log.v(TAG, url_current_brightnesses);
        String reply = "";
        try {
            reply = new DownloadWebpageTask().execute(url_current_brightnesses).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        // process csv
        String[] tupels = reply.split("\n");
        for(int i = 0; i < tupels.length; i++) {
            String[] data = tupels[i].split(";");
            if(data.length != 2) // break if data doesn't fit our scheme
                break;
            Log.v(TAG, "updating sb");
            int numberOfLEDItem = codeToNumber.get(data[0]);
            SeekBar sb = (SeekBar) ll_steady_lighting.getChildAt(numberOfLEDItem + 1).findViewById(R.id.sb_value);
            sb.setProgress(Integer.parseInt(data[1]));
        }
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
                    sb = (SeekBar) itemLayout.findViewById(R.id.sb_value);
                    sb_states.add(sb.getProgress());
                }

                Log.v(TAG, "LED_states:" + sb_states);
                for(int val : sb_states) {
                    Log.v(TAG, String.valueOf(val));
                }

                SteadyFadeTask fade_task = new SteadyFadeTask();



                for(int item = 0; item < adapterCount; item++) {
                    targets[item] = new LEDTarget(ledItems[item].getShortName(), sb_states.get(item));
                }
                fade_task.execute(targets);

//                updateSeekBars();
                // reset sb states
                sb_states = new ArrayList<>();
                break;


            case R.id.btn_all_off:
                task = new SteadyFadeTask();
                for(int item = 0; item < adapterCount; item++) {
                    targets[item] = new LEDTarget(ledItems[item].getShortName(), 0);
                }
                task.execute(targets);
                
                resetSeekBars(); // set all sb to zero
//                updateSeekBars();
                break;

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

        // update text
        String text = String.format(getResources().getString(R.string.tv_name), ledItem.getName(), progress);
        tv.setText(text);
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
////                Log.v(TAG, url_complete);
//                url = new URL(url_complete);
//                // send request
//                con = (HttpURLConnection) url.openConnection();
//                con.setReadTimeout(10000 /* millis */);
//                con.setConnectTimeout(15000 /* millis */);
//                con.connect();
//                response = con.getResponseCode();
//                Log.v(TAG, "Response code: " + response);

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
                Log.v(TAG, url_get);
                url_complete = url_http.concat(url_get);

                Log.v(TAG, url_complete);

                url = new URL(url_complete);

                // send request
                con = (HttpURLConnection) url.openConnection();
                con.setReadTimeout(10000 /* millis */);
                con.setConnectTimeout(15000 /* millis */);
                con.connect();

                response = con.getResponseCode();
                Log.v(TAG, "Response code: " + response);
            } catch(Exception e) {
                Log.v(TAG, e.toString());
            }
            return response;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if(result >= 200 && result < 300) { // if http status code is 2xx (success)
                Toast toast = Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.toast_fading_leds), Toast.LENGTH_SHORT); // show toast that the LEDs are fading
                toast.show();
            } else { // if error occurred
                String message = String.format(getResources().getString(R.string.toast_error_http), result);
                Toast toast = Toast.makeText(getActivity().getApplicationContext(), message, Toast.LENGTH_LONG); // show toast that an error occured, including error / status code
                toast.show();
            }
//            // sleep for 1 second to ensure led-blaster has written to file
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
            // reactivate button
            getView().findViewById(R.id.btn_apply).setActivated(true);

        }
    }

    public String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
        Reader reader = null;
        reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[len];
        reader.read(buffer);
        return new String(buffer);
    }

    // Given a URL, establishes an HttpUrlConnection and retrieves
// the web page content as a InputStream, which it returns as
// a string.
    private String downloadUrl(String myurl) throws IOException {
        InputStream is = null;
        // Only display the first 500 characters of the retrieved
        // web page content.
        int len = 500;

        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            Log.v(TAG, "The response is: " + response);
            is = conn.getInputStream();

            // Convert the InputStream into a string
            String contentAsString = readIt(is, len);

            return contentAsString;

            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    // Uses AsyncTask to create a task away from the main UI thread. This task takes a
    // URL string and uses it to create an HttpUrlConnection. Once the connection
    // has been established, the AsyncTask downloads the contents of the webpage as
    // an InputStream. Finally, the InputStream is converted into a string, which is
    // displayed in the UI by the AsyncTask's onPostExecute method.
    private class DownloadWebpageTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            // params comes from the execute() call: params[0] is the url.
            try {
                return downloadUrl(urls[0]);
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
        }
    }
}
