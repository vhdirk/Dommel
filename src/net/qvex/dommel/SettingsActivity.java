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
 * I got some inspiration here:
 * http://stackoverflow.com/a/4325239/343006
 * 
 */
package net.qvex.dommel;

import net.qvex.dommel.data.DommelDataService;
import net.qvex.dommel.R;
//import net.qvex.dommel_donate.R;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

public class SettingsActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener, OnPreferenceClickListener {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.app_preferences);

		PreferenceManager.setDefaultValues(SettingsActivity.this,
				R.xml.app_preferences, false);

		for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
			initSummary(getPreferenceScreen().getPreference(i));
		}

		// hide donate stuff if donated
		if (getResources().getBoolean(R.bool.is_donate_version)) {
			hideDonatePref();
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);

	}

	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);

	}

	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		if (key.equals("update_frequency")) {
			long delay = Long.parseLong(prefs.getString("update_frequency",
					"-1"));

			Intent intent = new Intent(this, DommelDataService.class);

			if (delay < 0) {
				intent.setAction(DommelDataService.STOP_SERVICE);
			} else {
				intent.setAction(DommelDataService.SCHEDULE_SERVICE);
			}
			WakefulIntentService.sendWakefulWork(this, intent);
		}
		updatePrefSummary(findPreference(key));

	}

	public boolean onPreferenceClick(Preference p) {
		// TODO Auto-generated method stub
		return false;
	}

	private void initSummary(Preference p) {
		if (p instanceof PreferenceCategory) {
			PreferenceCategory pCat = (PreferenceCategory) p;
			for (int i = 0; i < pCat.getPreferenceCount(); i++) {
				initSummary(pCat.getPreference(i));
			}
		} else {
			updatePrefSummary(p);
		}

	}

	private void updatePrefSummary(Preference p) {

		// if (p.getKey().equals("username") ||
		// p.getKey().equals("password"))
		// return;
		//

		if (p instanceof ListPreference) {
			ListPreference listPref = (ListPreference) p;
			p.setSummary(listPref.getEntry());
		}
		// if (p instanceof EditTextPreference) {
		// EditTextPreference editTextPref = (EditTextPreference) p;
		// p.setSummary(editTextPref.getText());
		// }

	}

	private void hideDonatePref() {

		Preference donate_pref = findPreference("donate_action");

		// http://stackoverflow.com/questions/4081533/how-to-remove-android-preferences-from-the-screen
		if (Build.VERSION.SDK_INT > 7) {
			PreferenceCategory about_prefcat = (PreferenceCategory) findPreference("pref_category_about");
			about_prefcat.removePreference(donate_pref);

		} else {
			getPreferenceScreen().removePreference(donate_pref);
		}

	}

	// private void setUpdateFrequencySummary(){
	// ListPreference update_pref = (ListPreference)
	// getPreferenceScreen().findPreference("update_frequency");
	// update_pref.setSummary(update_pref.getEntry());
	// }
	//
	// private void setWidgetActionSummary(){
	// ListPreference widgetaction_pref = (ListPreference)
	// getPreferenceScreen().findPreference("widget_action");
	// widgetaction_pref.setSummary(widgetaction_pref.getEntry());
	// }

}
