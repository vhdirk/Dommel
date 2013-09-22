/*
 * Copyright (C) 2012 Dirk Van Haerenborgh (vhdirk@gmail.com)
 * 2011 Lorenzo Bernardi (fastlorenzo@gmail.com)
 * 2010 Ben Van Daele (vandaeleben@gmail.com)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * @author Dirk Van Haerenborgh (vhdirk)
 *
 */


package net.qvex.dommel.data;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class OnAlarmReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent i = new Intent(context.getApplicationContext(), DommelDataService.class);
		WakefulIntentService.sendWakefulWork(context, i);
	}

}
