package com.eveningoutpost.dexdrip;

import android.content.*;
import android.os.*;
import android.preference.*;
import android.util.*;
import android.widget.*;

import com.eveningoutpost.dexdrip.models.*;
import com.eveningoutpost.dexdrip.utils.*;

import java.util.*;

class Location {
    Location(String location, int location_id) {
        this.location = location;
        this.location_id = location_id;
    }
    public String location;
    public int location_id;
}


public class NewSensorLocation extends ActivityWithMenu {
    public static String menu_name = "New sensor location";
    private Button button;
    private Button buttonCancel;
    private RadioGroup radioGroup;
    private EditText sensor_location_other;
    CheckBox DontAskAgain;
    List<Location> locations;

    final int PRIVATE_ID = 200;
    final int OTHER_ID = 201;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_sensor_location);
        button = (Button)findViewById(R.id.saveSensorLocation);
        buttonCancel = (Button)findViewById(R.id.saveSensorLocationCancel);
        sensor_location_other = (EditText) findViewById(R.id.edit_sensor_location);
        sensor_location_other.setEnabled(false);
        DontAskAgain = (CheckBox)findViewById(R.id.sensorLocationDontAskAgain);
        radioGroup = (RadioGroup) findViewById(R.id.myRadioGroup);
        addListenerOnButton();

        locations = new LinkedList<>();

        locations.add(new Location("I don't wish to share", PRIVATE_ID));
        locations.add(new Location("Upper arm", 1));
        locations.add(new Location("Thigh", 2));
        locations.add(new Location("Belly (abdomen)", 3));
        locations.add(new Location("Lower back", 4));
        locations.add(new Location("Buttocks", 5));
        locations.add(new Location("Other", OTHER_ID));

        for(Location location : locations) {
            AddButton(location.location, location.location_id);
        }
        radioGroup.check(PRIVATE_ID);

    }


    private void AddButton(String text, int id) {
        RadioButton newRadioButton = new RadioButton(this);
        newRadioButton.setText(text);
        newRadioButton.setId(id);
        LinearLayout.LayoutParams layoutParams = new RadioGroup.LayoutParams(
                RadioGroup.LayoutParams.WRAP_CONTENT,
                RadioGroup.LayoutParams.WRAP_CONTENT);
        radioGroup.addView(newRadioButton);

    }

    @Override
    public String getMenuName() {
        return menu_name;
    }

    public void addListenerOnButton() {

        button.setOnClickListener(v -> {


            int selectedId = radioGroup.getCheckedRadioButtonId();
            String location = "";

            if (selectedId == OTHER_ID) {
                location = sensor_location_other.getText().toString();
            } else {
                for(Location it : locations) {
                    if(selectedId == it.location_id) {
                        location = it.location;
                        break;
                    }
                }
            }
            Toast.makeText(getApplicationContext(), "Sensor locaton is " + location, Toast.LENGTH_LONG).show();


            Log.d("NEW SENSOR", "Sensor location is " + location);
            Sensor.updateSensorLocation(location);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

            prefs.edit().putBoolean("store_sensor_location", !DontAskAgain.isChecked()).apply();

            Intent intent = new Intent(getApplicationContext(), Home.class);
            startActivity(intent);
            finish();
        });

        buttonCancel.setOnClickListener(v -> {

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

            prefs.edit().putBoolean("store_sensor_location", !DontAskAgain.isChecked()).apply();

            Intent intent = new Intent(getApplicationContext(), Home.class);
            startActivity(intent);
            finish();
        });

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if(checkedId == OTHER_ID) {
                sensor_location_other.setEnabled(true);
                sensor_location_other.requestFocus();
            } else {
                sensor_location_other.setEnabled(false);
            }
        });


    }
}
