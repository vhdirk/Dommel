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
import java.util.Date;

import net.qvex.dommel.data.DommelDataService;
import net.qvex.dommel.R;
//import net.qvex.dommel_donate.R;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.example.android.actionbarcompat.ActionBarActivity;

public class MainActivity extends ActionBarActivity {
	private SharedPreferences prefs;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		if (prefs.getString("username", "").isEmpty()
				|| prefs.getString("password", "").isEmpty()) {
			// no preferences set yet, so show the settings activity
			showSettings();
		}
	}

	private void updateUsage() {
		// TODO: check username and password first?
		displayThrobber(true);
		Intent update = new Intent(this, DommelDataService.class);
		update.setAction(DommelDataService.UPDATE);
		WakefulIntentService.sendWakefulWork(this, update);
	}

	private void showSettings() {
		Intent settings = new Intent(this, SettingsActivity.class);
		startActivity(settings);
	}

	/** Called when an activity called by using startActivityForResult finishes. */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Toast toast = Toast.makeText(this, "The activity finnished",
				Toast.LENGTH_SHORT);
		toast.show();
	}

	public void updateOverview() {

		float volume_used = prefs.getFloat("volume_used", -1);
		@SuppressWarnings("unused")
		float volume_remaining = prefs.getFloat("volume_remaining", -1);
		float volume_total = prefs.getFloat("volume_total", -1);
		long reset_date = prefs.getLong("reset_date", -1);
		int days_left = prefs.getInt("days_left", -1);
		boolean unlimited = prefs.getBoolean("unlimited", false);
		boolean last_update_success = prefs.getBoolean("last_update_success",
				false);
		long last_update = prefs.getLong("last_update", -1);

		Resources res = getResources();

		TextView txt_volumeUsed = (TextView) findViewById(R.id.txt_volumeUsed);

		String str_volumeUsed = Float.toString(volume_used);
		if (!unlimited) {
			str_volumeUsed += " / " + Float.toString(volume_total)
					+ getString(R.string.megabyte);
		}
		txt_volumeUsed.setText(str_volumeUsed);

		TextView txt_resetDate = (TextView) findViewById(R.id.txt_resetDate);

		DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
		Date date_resetDate = new Date(reset_date);
		txt_resetDate.setText(df.format(date_resetDate));

		TextView txt_daysLeft = (TextView) findViewById(R.id.txt_daysLeft);
		txt_daysLeft.setText(res.getQuantityString(R.plurals.days_left,
				days_left, days_left));

		ProgressBar bar_volumeUsed = (ProgressBar) findViewById(R.id.bar_volumeUsed);

		if (!unlimited) {
			bar_volumeUsed.setMax(Math.round(volume_total));
			bar_volumeUsed.setProgress(Math.round(volume_used));
		} else {
			bar_volumeUsed.setMax(1);
			bar_volumeUsed.setProgress(1);
		}

		Date date_prevResetDate = (Date) date_resetDate.clone();

		int month = date_prevResetDate.getMonth() - 1;
		if (month < 0) {
			month = 12;
			date_prevResetDate.setYear(date_prevResetDate.getYear() - 1);
		}
		date_prevResetDate.setMonth(month);

		int totaldays = DommelDataService.daysBetween(date_prevResetDate,
				date_resetDate);

		ProgressBar bar_daysLeft = (ProgressBar) findViewById(R.id.bar_daysLeft);
		bar_daysLeft.setMax(totaldays);
		bar_daysLeft.setProgress(totaldays - days_left);

		TextView txt_lastUpdate = (TextView) findViewById(R.id.txt_lastUpdate);
		Date date_lastUpdate = new Date(last_update);
		txt_lastUpdate.setText(df.format(date_lastUpdate));

		int txtcolour = res.getColor(android.R.color.primary_text_light);
		if (!last_update_success) {
			txtcolour = res.getColor(android.R.color.holo_red_light);
		}

		txt_lastUpdate.setTextColor(txtcolour);
	}

	private void displayThrobber(boolean disp) {
		getActionBarHelper().setRefreshActionItemState(disp);
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(successReceiver);
		unregisterReceiver(exceptionReceiver);
	}

	/**
	 * Called when the activity is about to start interacting with the user.
	 */
	@Override
	protected void onResume() {
		super.onResume();
		this.setContentView(R.layout.activity_main);
		updateOverview();

		registerReceiver(successReceiver, new IntentFilter(
				DommelDataService.USAGE_UPDATED));
		registerReceiver(exceptionReceiver, new IntentFilter(
				DommelDataService.EXCEPTION));
	}

	/**
	 * Called when your activity's options menu needs to be created.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.main, menu);

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
		case android.R.id.home:
			Toast.makeText(this, "Tapped home", Toast.LENGTH_SHORT).show();
			break;

		case R.id.menu_refresh:
			updateUsage();
			break;

		case R.id.menu_settings:
			showSettings();
			break;

		}
		return super.onOptionsItemSelected(item);
	}

	private BroadcastReceiver successReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateOverview();
			MainActivity.this.displayThrobber(false);
			Toast.makeText(context, getString(R.string.success_message),
					Toast.LENGTH_SHORT).show();
		}
	};

	private BroadcastReceiver exceptionReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			Exception e = (Exception) intent
					.getSerializableExtra(DommelDataService.EXCEPTION);

			Toast.makeText(
					context,
					getString(R.string.exception_message, e == null ? "null"
							: e.getClass().getName()), Toast.LENGTH_LONG)
					.show();

			Log.e(DommelDataService.class.getSimpleName(),
					"Exception stackTrace: ");
			if (e != null)
				e.printStackTrace();

			MainActivity.this.displayThrobber(false);
		}

	};

}
