package de.danielbuecheler.ledblastercontroller;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Created by daniel on 29.11.15.
 */
public class LEDItemAdapter  extends BaseAdapter {

    private final String TAG = "LEDItemAdapter";

    private List<LEDItem> ledItems;

    private final LayoutInflater inflator;

    public LEDItemAdapter(Context context)  {
        inflator = LayoutInflater.from(context);

        ledItems = new ArrayList<>();

        // try to read file containing led items
        Log.d(TAG, "reading...");

        try {
            FileInputStream fis = context.openFileInput("leditem");
            ObjectInputStream is = new ObjectInputStream(fis);
            ledItems = (ArrayList<LEDItem>) is.readObject();
            Log.d(TAG, "read leditems " + ledItems.toString());
            is.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }

        Log.d(TAG, String.valueOf(this.getCount()));
    }

    public void update(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String url_setupfile = prefs.getString("pref_setup_url", "").trim(); // get setup url from sharedPrefs

        String reply = "";
        try {
            // get file from webpage and convert response to string we then can work with
            reply = new DownloadWebpageTask().execute(url_setupfile).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        } catch (ExecutionException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }

        // if no reply
        if(reply == null || reply.isEmpty()) {
            // exit
            return;
        }

        // delete current LED list
        ledItems = new ArrayList<>();

        // split csv so it's usable for me
        String[] tupels = reply.split("\n");
        for(int i = 0; i < tupels.length; i++) {
            String[] data = tupels[i].split(";");
            if(data.length != 5)
                break; // this is only reached when the relevant data has ended (EOF is not possible)
            // add the LED Item
            ledItems.add(new LEDItem(/* short name / code */ data[0], /* long name / label */ data[4], /* hex color */ data[3], /* hex color for font */ data[2]));
        }

        // save LED Items to file
        try {
            Log.d(TAG, "saving...");
            FileOutputStream fos = new FileOutputStream(new File(context.getFilesDir(), "leditem"));

            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(ledItems);
            os.close();
            fos.close();
            Log.d(TAG, "saved successfully");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getCount() {
        return ledItems.size();
    }

    @Override
    public Object getItem(int position) {
        return ledItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        // if not recycling
        if(convertView == null) {
            convertView = inflator.inflate(R.layout.list_element, parent, false); // inflate from xml

            holder = new ViewHolder(); // prepare holder to hold our view elements
            // add each view element
            holder.layout = (RelativeLayout) convertView.findViewById(R.id.surrounding_layout);
            holder.name = (TextView) convertView.findViewById(R.id.tv_name);
            holder.seekBar = (SeekBar) convertView.findViewById(R.id.sb_value);

            if(holder.layout == null) {
                Log.d(TAG, "fail");
            }
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // get LEDItem from list
        LEDItem ledItem = (LEDItem) getItem(position);

        // apply the changes that are necessary
        holder.layout.setBackgroundColor(Color.parseColor(ledItem.getColorHex())); // apply background color
        String tv_name = String.format(convertView.getResources().getString(R.string.tv_name), ledItem.getName(), 0);
        holder.name.setText(tv_name);
        holder.name.setTextColor(Color.parseColor(ledItem.getColorFontHex()));
        return convertView;
    }

    static class ViewHolder{
        RelativeLayout layout;
        TextView name;
        CheckBox checkBox;
        SeekBar seekBar;
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
            Log.d(TAG, "The response is: " + response);
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

