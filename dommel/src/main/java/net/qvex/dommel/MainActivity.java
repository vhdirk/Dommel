/*
 * This file is part of Dommel Monitor.
 *
 * Dommel Monitor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Dommel Monitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Dommel Monitor. If not, see <http://www.gnu.org/licenses/>.
 * 
 * @author Dirk Van Haerenborgh (vhdirk)
 * 
 * Note:
 *   Big parts of this file originate from Mobile Vikings for Android by
 *   Lorenzo Bernardi and Ben Van Daele (https://redmine.djzio.be/projects/mvfa)
 * 
 */

package net.qvex.dommel;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import net.qvex.dommel.data.DataServiceResultReceiver;
import net.qvex.dommel.data.DommelDataService;
import net.qvex.dommel.R;
//import net.qvex.dommel_donate.R;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity
{
    private SharedPreferences mPreferences;
    private Menu mOptionsMenu;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //request a spinner in the actionbar
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        //load shared preferences
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // if no username/password set, show the settings activity
        if (mPreferences.getString("username", "").isEmpty() || mPreferences.getString("password", "").isEmpty())
        {
            showSettings();
        }
    }

    private void updateUsage()
    {
        //user name and password are checked in the service
        final Intent update = new Intent(this, DommelDataService.class);
        update.putExtra(DommelDataService.FIELD_MANUAL, true);
        startService(update);
    }

    private void showSettings()
    {
        Intent settings = new Intent(this, SettingsActivity.class);
        startActivity(settings);
    }

    /** Called when an activity called by using startActivityForResult finishes. */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Toast toast = Toast.makeText(this, "The activity finished", Toast.LENGTH_SHORT);
        toast.show();
    }

    public void updateOverview()
    {
        float volume_used = mPreferences.getFloat("volume_used", -1);
        @SuppressWarnings("unused")
        float volume_remaining = mPreferences.getFloat("volume_remaining", -1);
        float volume_total = mPreferences.getFloat("volume_total", -1);
        long reset_date = mPreferences.getLong("reset_date", -1);
        int days_left = mPreferences.getInt("days_left", -1);
        boolean unlimited = mPreferences.getBoolean("unlimited", false);
        boolean last_update_success = mPreferences.getBoolean("last_update_success",
                false);
        long last_update = mPreferences.getLong("last_update", -1);

        Resources res = getResources();

        TextView txt_volumeUsed = (TextView) findViewById(R.id.txt_volumeUsed);
        TextView txt_resetDate = (TextView) findViewById(R.id.txt_resetDate);
        TextView txt_lastUpdate = (TextView) findViewById(R.id.txt_lastUpdate);
        TextView txt_daysLeft = (TextView) findViewById(R.id.txt_daysLeft);
        ProgressBar bar_daysLeft = (ProgressBar) findViewById(R.id.bar_daysLeft);
        ProgressBar bar_volumeUsed = (ProgressBar) findViewById(R.id.bar_volumeUsed);


        String str_volumeUsed = Float.toString(volume_used);
        if (!unlimited) {
            str_volumeUsed += " / " + Float.toString(volume_total)
                    + getString(R.string.megabyte);
        }
        txt_volumeUsed.setText(str_volumeUsed);

        DateFormat df = SimpleDateFormat.getDateInstance();

        Calendar date_resetDate = Calendar.getInstance();
        date_resetDate.setTimeInMillis(reset_date);

        txt_daysLeft.setText(res.getQuantityString(R.plurals.days_left,
                days_left, days_left));


        if (!unlimited) {
            bar_volumeUsed.setMax(Math.round(volume_total));
            bar_volumeUsed.setProgress(Math.round(volume_used));
        } else {
            bar_volumeUsed.setMax(1);
            bar_volumeUsed.setProgress(1);
        }

        //calculate the previous reset date. (just 1 month earlier)
        Calendar date_prevResetDate = (Calendar) date_resetDate.clone();
        date_prevResetDate.add(Calendar.MONTH, -1);

        int totaldays = DommelDataService.daysBetween(date_prevResetDate, date_resetDate);

        bar_daysLeft.setMax(totaldays);
        bar_daysLeft.setProgress(totaldays - days_left);

        Date date_lastUpdate = new Date(last_update);
        txt_lastUpdate.setText(df.format(date_lastUpdate));

        txt_resetDate.setText(df.format(date_resetDate.getTime()));

        int txtcolour = res.getColor(android.R.color.primary_text_light);
        if (!last_update_success) {
            txtcolour = res.getColor(android.R.color.holo_red_light);
        }

        txt_lastUpdate.setTextColor(txtcolour);
    }

    void setRefreshActionButtonState(boolean refreshing) {
        if (mOptionsMenu == null) {
            return;
        }

        final MenuItem refreshItem = mOptionsMenu.findItem(R.id.menu_refresh);
        if (refreshItem != null) {
            if (refreshing) {
                refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
            } else {
                refreshItem.setActionView(null);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // clear receiver so no leaks.
        this.unregisterReceiver(this.mReceiver);
    }

    /**
     * Called when the activity is about to start interacting with the user.
     */
    @Override
    protected void onResume() {
        super.onResume();
        this.setContentView(R.layout.activity_main);
        this.registerReceiver(this.mReceiver, new IntentFilter(DommelDataService.MESSAGE_STATUS));


        updateOverview();

        //TODO: maybe always disable the throtter

    }

    /**
     * Called when your activity's options menu needs to be created.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main, menu);

        mOptionsMenu = menu;

        // Calling super after populating the menu is necessary here to ensure
        // that the
        // action bar helpers have a chance to handle this event.
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Called right before your activity's option menu is displayed.
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // Before showing the menu, we need to decide whether the clear
        // item is enabled depending on whether there is text to clear.
        // menu.findItem(CLEAR_ID).setVisible(mEditor.getText().length() > 0);

        return true;
    }

    /**
     * Called when a menu item is selected.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_refresh:
                updateUsage();
                break;

            case R.id.menu_settings:
                showSettings();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {

            String action = intent.getAction();
            if(action.equalsIgnoreCase(DommelDataService.MESSAGE_STATUS))
            {
                Bundle extra = intent.getExtras();
                int resultCode = intent.getIntExtra(DommelDataService.FIELD_STATUS, -1);

                switch (resultCode) {
                    case DommelDataService.STATUS_RUNNING:
                        setRefreshActionButtonState(true);
                        break;
                    case DommelDataService.STATUS_FINISHED:
                        MainActivity.this.setRefreshActionButtonState(false);
                        Toast.makeText(MainActivity.this, getString(R.string.success_message),
                                Toast.LENGTH_SHORT).show();
                        break;
                    case DommelDataService.STATUS_ERROR:
                        MainActivity.this.setRefreshActionButtonState(false);

                        Exception e = (Exception) extra.getSerializable(DommelDataService.FIELD_EXCEPTION);

                        if (e != null){
                            Toast.makeText(
                                    MainActivity.this,
                                    getString(R.string.exception_message, e == null ? "null"
                                            : e.getClass().getName()), Toast.LENGTH_LONG)
                                    .show();

                            Log.e(DommelDataService.class.getSimpleName(),
                                    "Exception stackTrace: ");
                            if (e != null)
                                e.printStackTrace();
                        }
                        break;

                }
                MainActivity.this.updateOverview();
            }

        }
    };
}
