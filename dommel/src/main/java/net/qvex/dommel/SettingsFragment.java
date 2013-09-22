package net.qvex.dommel;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import net.qvex.dommel.data.DommelDataService;

import java.util.Calendar;


@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener
{
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.app_preferences);

        //set defaults
//        PreferenceManager.setDefaultValues(SettingsFragment.this,
//				R.xml.app_preferences, false);
//
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            initSummary(getPreferenceScreen().getPreference(i));
        }

        // hide donate stuff if donated
        if (getResources().getBoolean(R.bool.is_donate_version)) {
            hideDonatePref();
        }

    }


    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals("update_frequency")) {
			long delay = Long.parseLong(sharedPreferences.getString("update_frequency",
					"-1"));

            Calendar cal = Calendar.getInstance();

            Intent intent = new Intent(getActivity(), DommelDataService.class);
            PendingIntent pintent = PendingIntent.getService(getActivity(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager alarm = (AlarmManager)getActivity().getSystemService(Context.ALARM_SERVICE);

            if (delay < 0) {
                alarm.cancel(pintent);
            }
            else{
                alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), delay, pintent);
            }

        }
		updatePrefSummary(findPreference(key));
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);

    }

    private void initSummary(Preference pref) {
        if (pref instanceof PreferenceCategory) {
            PreferenceCategory pref_cat = (PreferenceCategory) pref;
            for (int i = 0; i < pref_cat.getPreferenceCount(); i++) {
                initSummary(pref_cat.getPreference(i));
            }
        } else {
            updatePrefSummary(pref);
        }

    }

    private void updatePrefSummary(Preference pref) {

        // if (p.getKey().equals("username") ||
        // p.getKey().equals("password"))
        // return;
        //

        if (pref instanceof ListPreference) {
            ListPreference list_pref = (ListPreference) pref;
            pref.setSummary(list_pref.getEntry());
        }

        // if (p instanceof EditTextPreference) {
        // EditTextPreference editTextPref = (EditTextPreference) p;
        // p.setSummary(editTextPref.getText());
        // }

    }

    private void hideDonatePref()
    {
        Preference donate_pref = findPreference("donate_action");

        // http://stackoverflow.com/questions/4081533/how-to-remove-android-preferences-from-the-screen
        if (Build.VERSION.SDK_INT > 7) {
            PreferenceCategory about_prefcat = (PreferenceCategory) findPreference("pref_category_about");
            about_prefcat.removePreference(donate_pref);
        } else {
            getPreferenceScreen().removePreference(donate_pref);
        }
    }

}