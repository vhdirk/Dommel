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

package net.qvex.dommel.widget;

import net.qvex.dommel.MainActivity;
import net.qvex.dommel.data.DommelDataService;

import net.qvex.dommel.R;
//import net.qvex.dommel_donate.R;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * 
 */
public class WidgetProvider extends AppWidgetProvider {
	// log tag
	public static final String ON_WIDGET_CLICK = "net.qvex.dommel.widget.onWidgetClick";

	private static final String LOG_TAG = "DommelWidgetProvider";
	public static final String ACTION_OPEN_APP = "0";
	public static final String ACTION_UPDATE_DATA = "1";
	public static final int WIDGET_BG_BLACK = 0;
	public static final int WIDGET_BG_TRANSPARENT = 1;

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {

		final int N = appWidgetIds.length;
		for (int i = 0; i < N; i++) {
			int appWidgetId = appWidgetIds[i];
			appWidgetManager.updateAppWidget(appWidgetId, getViews(context));
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {

		super.onReceive(context, intent);

		if (intent.getAction().equals(ON_WIDGET_CLICK)) {
			String widgetAction = getWidgetActionPreference(context);
			if (widgetAction.equals(ACTION_OPEN_APP)) {
				openApp(context);
			} else if (widgetAction.equals(ACTION_UPDATE_DATA)) {
				updateData(context);
			}
		}

		else if (intent.getAction().equals(DommelDataService.USAGE_UPDATED)) {
			// Some magic to obtain a reference to the AppWidgetManager
			ComponentName thisWidget = new ComponentName(context,
					WidgetProvider.class);
			AppWidgetManager manager = AppWidgetManager.getInstance(context);
			manager.updateAppWidget(thisWidget, getViews(context));
		} else if (intent.getAction().equals(DommelDataService.EXCEPTION)) {
			// TODO: update was not successful; display this
		}

	}

	private void openApp(Context context) {
		Intent intent = new Intent(context, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}

	private void updateData(Context context) {
		Intent update = new Intent(context, DommelDataService.class);
		update.setAction(DommelDataService.UPDATE);
		WakefulIntentService.sendWakefulWork(context, update);
	}

	private String getWidgetActionPreference(Context context) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		return prefs.getString("widget_action", "0");

	}

	private RemoteViews getViews(Context context) {
		RemoteViews views = null;

		// int bg_color = Integer
		// .decode(prefs.getString("widget_background", "0"));
		//
		int layout = 0;
		// Log.v(LOG_TAG, "color:" + bg_color);
		//
		// switch (bg_color) {
		//
		// case SettingsActivity.WIDGET_BG_BLACK:
		//
		layout = R.layout.widget;
		//
		// break;
		//
		// case SettingsActivity.WIDGET_BG_TRANSPARENT:
		//
		// layout = R.layout.widget1x1_transparent;
		//
		// break;
		//
		// }

		views = new RemoteViews(context.getPackageName(), layout);
		Intent intent = new Intent(ON_WIDGET_CLICK);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
				intent, 0);

		views.setOnClickPendingIntent(R.id.widget, pendingIntent);
		updateViewContent(context, views);
		return views;

	}

	public void updateViewContent(Context context, RemoteViews views) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);

		float volume_used = prefs.getFloat("volume_used", -1);
		float volume_remaining = prefs.getFloat("volume_remaining", -1);
		float volume_total = prefs.getFloat("volume_total", -1);
		@SuppressWarnings("unused")
		long reset_date = prefs.getLong("reset_date", -1);
		int days_left = prefs.getInt("days_left", -1);
		boolean unlimited = prefs.getBoolean("unlimited", false);

		boolean last_update_success = prefs.getBoolean("last_update_success",
				false);

		int txtcolour = context.getResources()
				.getColor(android.R.color.primary_text_dark);
		
		if (!last_update_success){
			txtcolour = context.getResources()
					.getColor(android.R.color.holo_red_dark);			
		}
		
		views.setTextColor(R.id.widget_txtUsage, txtcolour);
		views.setTextColor(R.id.widget_txtDays, txtcolour);

		views.setTextViewText(R.id.widget_txtDays, Integer.toString(days_left));

		if (!unlimited) {
			views.setProgressBar(R.id.widget_volumeUsed,
					Math.round(volume_total), Math.round(volume_used), false);
			views.setTextViewText(R.id.widget_txtUsage,
					Integer.toString((int) Math.floor(volume_remaining / 1024)));

		} else {
			views.setProgressBar(R.id.widget_volumeUsed, 1, 1, false);
			views.setTextViewText(R.id.widget_txtUsage, "inf");

		}
	}

}
