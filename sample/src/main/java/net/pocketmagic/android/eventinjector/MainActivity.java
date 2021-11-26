/*
 * Android Event Injector
 *
 * Copyright (c) 2013 by Radu Motisan , radu.motisan@gmail.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * For more information on the GPL, please go to:
 * http://www.gnu.org/copyleft/gpl.html
 *
 */

package net.pocketmagic.android.eventinjector;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import net.pocketmagic.android.eventinjector4.Events;
import net.pocketmagic.android.eventinjector4.Events.InputDevice;
import net.pocketmagic.android.utils.CustomAdapter;
import net.pocketmagic.android.utils.ListViewItem;
import net.pocketmagic.android.utils.MainThread;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements OnClickListener {
    final static String LT = "MainActivity";

    Events events = new Events();
    boolean m_bMonitorOn = false;                // used in the thread to poll for input event node  messages

    final static int idLVFirstItem = Menu.FIRST + 5000;
    // interface views
    TextView tvMonitor;                                // used to display monitored events, in the format code-type-value. See input.h in the NDK
    ListView lvDevices;                                // the listview showing devices found
    Spinner spinneDevices;                            // The spinner is used to select a target for the Key and Touch buttons
    int m_selectedDev = -1;                    // index of spinner selected device, or -1 is no selection

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LT, "App created.");

        Events.intEnableDebug(1);

        setContentView(R.layout.activity_main);


        Button btnScan = findViewById(R.id.btn_scan_devices);
        btnScan.setOnClickListener(v -> {
            Log.d(LT, "Scanning for input dev files.");
            // init event node files
            int res = events.Init();
            // debug results
            Log.d(LT, "Event files:" + res);
            // try opening all
            PopulateListView();
        });

        lvDevices = findViewById(R.id.lv_devices);

        spinneDevices = findViewById(R.id.spinner_devices);
        spinneDevices.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                int i = 0, j = 0;
                m_selectedDev = -1;
                for (InputDevice device : events.m_Devs) {
                    if (device.getOpen()) {
                        if (i == position) {
                            m_selectedDev = j;
                            break;
                        } else {
                            i++;
                        }
                    }
                    j++;
                }
                if (m_selectedDev != -1) {
                    String name = events.m_Devs.get(m_selectedDev).getName();
                    Log.d(LT, "spinner selected:" + position + " Name:" + name);
                    Toast.makeText(getApplicationContext(), "New device selected:" + name, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Invalid device selection!", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        findViewById(R.id.btn_inject_key).setOnClickListener(v -> injectKey());
        findViewById(R.id.btn_inject_touch).setOnClickListener(v -> injectTouch());


        tvMonitor = findViewById(R.id.tv_monitor);
        tvMonitor.setText("Event Monitor stopped.");
        findViewById(R.id.btn_monitor_start).setOnClickListener(v -> startMonitor());
        findViewById(R.id.btn_monitor_stop).setOnClickListener(v -> stopMonitor());
        findViewById(R.id.btn_test).setOnClickListener(v -> startTest());

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LT, "App destroyed.");
        StopEventMonitor();
        events.Release();
    }

    private void injectKey() {
        if (m_selectedDev != -1) {
            //see input.h in Android NDK, sequence represents the codes for pocketmagic.net
            final int keys[] = new int[]{25, 24, 46, 37, 18, 20, 50, 30, 34, 23, 46, 52, 49, 18, 20};
            // send all these keys with half a second delay
            Thread sender = new Thread(() -> {
                for (int key : keys) {
                    Log.d(LT, "Sending:" + key + " to:" + events.m_Devs.get(m_selectedDev).getName());
                    events.m_Devs.get(m_selectedDev).SendKey(key, true); //key down
                    events.m_Devs.get(m_selectedDev).SendKey(key, false); //key up
                    // a short delay before next character, just for testing purposes
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            });
            sender.start();
        } else
            Toast.makeText(this, "Select a valid device first, using the spinner to the left.", Toast.LENGTH_SHORT).show();

    }

    private void injectTouch() {
        //absolute coordinates, on my device they go up to 570x960
        if (m_selectedDev != -1) {
            events.m_Devs.get(m_selectedDev).SendTouchDownAbs(155, 183);
        } else {
            Toast.makeText(this, "Select a valid device first, using the spinner to the left.", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id >= idLVFirstItem) {
            int nLVIndexClick = id - idLVFirstItem;
            Log.d(LT, "LV Item Click:" + nLVIndexClick);
            for (InputDevice idev : events.m_Devs) {
                if (idev.getId() == nLVIndexClick) {
                    if (idev.Open(true)) {
                        // refresh listview
                        PopulateListView();
                        // inform user
                        Toast.makeText(this, "Device opened successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Device failed to open. Do you have root?", Toast.LENGTH_SHORT).show();
                    }
                    break;
                }
            }
        }

    }

    private void stopMonitor() {
        Toast.makeText(this, "Event monitor stopped.", Toast.LENGTH_SHORT).show();

        StopEventMonitor();
        tvMonitor.post(() -> tvMonitor.setText("Event Monitor stopped."));

    }

    private void startMonitor() {
        if (m_bMonitorOn) {
            Toast.makeText(this, "Event monitor already working. Consider opening more devices to monitor.", Toast.LENGTH_SHORT).show();
        } else {
            tvMonitor.post(() -> tvMonitor.setText("Event Monitor running, waiting for data."));
            StartEventMonitor();
        }

    }

    private void startTest() {
        Thread sender = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                SendHomeKeyToKeypad();
                //a short delay before next character, just for testing purposes
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        sender.start();
    }

    /**
     * Populated the listview with discovered devices, and the spinner with those that are open
     */
    private void PopulateListView() {
        lvDevices.post(() -> {
            lvDevices.setAdapter(null);
            ArrayList<ListViewItem> m_Devices = new ArrayList<>();
            for (InputDevice idev : events.m_Devs) {
                ListViewItem device = new ListViewItem(
                        idev.getName(),
                        idev.getPath(),
                        idev.getOpen(),
                        idLVFirstItem + idev.getId()
                );
                m_Devices.add(device);
            }
            CustomAdapter m_lvAdapter = new CustomAdapter(MainActivity.this, m_Devices);
            if (lvDevices != null) lvDevices.setAdapter(m_lvAdapter);
        });
        spinneDevices.post(() -> {
            ArrayList<String> openDevs = new ArrayList<>();

            for (InputDevice idev : events.m_Devs) {
                if (idev.getOpen())
                    openDevs.add(idev.getName());
            }
            // populate spinner
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    MainActivity.this, android.R.layout.simple_spinner_item,
                    openDevs);
            adapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
            // changes spin size and popup box size/color
            spinneDevices.setAdapter(adapter);

        });
    }

    /**
     * Stops our event monitor thread
     */
    public void StopEventMonitor() {
        m_bMonitorOn = false; //stop reading thread
    }


    /**
     * Starts our event monitor thread that does the data extraction via polling
     * all data is displayed in the textview, as type-code-value, see input.h in the Android NDK for more details
     * Monitor output is also sent to Logcat, so make sure you used that as well
     */
    public void StartEventMonitor() {
        m_bMonitorOn = true;
        Thread b = new Thread(() -> {
            while (m_bMonitorOn) {
                for (InputDevice idev : events.m_Devs) {
                    // Open more devices to see their messages
                    if (idev.getOpen() && (0 == idev.getPollingEvent())) {
                        final String line = idev.getName() +
                                ":" + idev.getSuccessfulPollingType() +
                                " " + idev.getSuccessfulPollingCode() +
                                " " + idev.getSuccessfulPollingValue();
                        Log.d(LT, "Event:" + line);
                        // update textview to show data
                        //if (idev.getSuccessfulPollingValue() != 0)
                        tvMonitor.post(() -> tvMonitor.setText(line));
                    }

                }
            }
        });
        b.start();
    }

    /**
     * Finds an open device that has a name containing keypad. This probably is the event node associated with the keypad
     * Its purpose is to handle all hardware Android buttons such as Back, Home, Volume, etc
     * Key codes are defined in input.h (see NDK) , or use the Event Monitor to see keypad messages
     * This function sends the Settings key
     */
    public void SendSettingsKeyToKeypad() {
        for (InputDevice idev : events.m_Devs) {
            //* Finds an open device that has a name containing keypad. This probably is the keypad associated event node
            if (idev.getOpen() && idev.getName().contains("keypad")) {
                idev.SendKey(139, true); // settings key down
                idev.SendKey(139, false); // settings key up
            }
        }
    }

    /**
     * Finds an open device that has a name containing keypad. This probably is the event node associated with the keypad
     * Its purpose is to handle all hardware Android buttons such as Back, Home, Volume, etc
     * Key codes are defined in input.h (see NDK) , or use the Event Monitor to see keypad messages
     * This function sends the HOME key
     */
    public void SendHomeKeyToKeypad() {
        boolean found = false;
        for (InputDevice idev : events.m_Devs) {
            //* Finds an open device that has a name containing keypad. This probably is the keypad associated event node
            if (idev.getOpen() && idev.getName().contains("keypad")) {
                idev.SendKey(102, true); // home key down
                idev.SendKey(102, false); // home key up
                found = true;
                break;
            }
        }
        //todo thread!
        if (!found) {
            MainThread.post(() -> {
                Toast.makeText(this, "Keypad not found.", Toast.LENGTH_SHORT).show();
            });
        }
    }

    /**
     * Finds an open device that has a name containing keypad. This probably is the event node associated with the keypad
     * Its purpose is to handle all hardware Android buttons such as Back, Home, Volume, etc
     * Key codes are defined in input.h (see NDK) , or use the Event Monitor to see keypad messages
     * This function sends the BACK key
     */
    public void SendBackKeyToKeypad() {
        for (InputDevice idev : events.m_Devs) {
            //* Finds an open device that has a name containing keypad. This probably is the keypad associated event node
            if (idev.getOpen() && idev.getName().contains("keypad")) {
                idev.SendKey(158, true); // Back key down
                idev.SendKey(158, false); // back key up
            }
        }
    }
}
