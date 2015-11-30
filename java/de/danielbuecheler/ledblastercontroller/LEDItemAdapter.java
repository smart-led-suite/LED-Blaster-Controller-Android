package de.danielbuecheler.ledblastercontroller;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by daniel on 29.11.15.
 */
public class LEDItemAdapter  extends BaseAdapter {

    private final String TAG = "LEDItemAdapter";

    private List<LEDItem> ledItems;

    private final LayoutInflater inflator;

    public LEDItemAdapter(Context context) {
        inflator = LayoutInflater.from(context);

        ledItems = new ArrayList<>();

        // add some colors
        ledItems.add(new LEDItem("w", "White upstairs", context.getResources().getColor(R.color.white)));
        ledItems.add(new LEDItem("w1", "IKEA lamp", context.getResources().getColor(R.color.white)));
        ledItems.add(new LEDItem("r", "Red", context.getResources().getColor(R.color.holo_red_light)));
        ledItems.add(new LEDItem("g", "Green", context.getResources().getColor(R.color.holo_green_light)));
        ledItems.add(new LEDItem("b", "Blue", context.getResources().getColor(R.color.holo_blue_light)));
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
            holder.checkBox = (CheckBox) convertView.findViewById(R.id.cb_change);
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
        holder.layout.setBackgroundColor(ledItem.getColor()); // apply background color
        String tv_name = String.format(convertView.getResources().getString(R.string.tv_name), ledItem.getName(), 0);
        holder.name.setText(tv_name);
        return convertView;
    }

    static class ViewHolder{
        RelativeLayout layout;
        TextView name;
        CheckBox checkBox;
        SeekBar seekBar;
    }
}

