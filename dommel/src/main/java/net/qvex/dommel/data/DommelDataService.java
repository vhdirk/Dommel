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
 *   Lorenzo Bernardi and Ben Van Daele (redmine.djzio.be/projects/mvfa)
 * 
 *   Parsing the Dommel webpage is inspired by phptemeler by Jan De Luyck
 *   (phptelemeter.kcore.org)
 * 
 */

package net.qvex.dommel.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

public class DommelDataService extends IntentService {
    // Android Intent actions

    public static final String MESSAGE_STATUS   = "net.qvex.dommel.data.Message.Status";

    public static final String FIELD_EXCEPTION  = "net.qvex.dommel.data.Field.Exception";
    public static final String FIELD_STATUS     = "net.qvex.dommel.data.Field.Status";
    public static final String FIELD_MANUAL     = "net.qvex.dommel.data.Field.Update";


    public static final int STATUS_RUNNING  =  1;
    public static final int STATUS_FINISHED =  2;
    public static final int STATUS_ERROR    = -1;

    // Dommel provider info
    private static final String URL_LOGIN      = "https://crm.schedom-europe.net/index.php";
    private static final String URL_PACKAGES   = "https://crm.schedom-europe.net/user.php?op=view&tile=mypackages";
    private static final String URL_STATS_INIT = "https://crm.schedom-europe.net/include/scripts/linked/dslinfo/dslinfo.php";
    private static final String URL_LOGOUT     = "https://crm.schedom-europe.net/index.php?op=logout";
    private static final String USER_AGENT     = "Mozilla/5.0 (Linux; U; Android 2.3; en-gb) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1";
    private DefaultHttpClient httpclient;


    //
    private float volume_used;
    private float volume_remaining;
    private float volume_total;
    private boolean unlimited;
    private int days_left;
    private Calendar reset_date;
    private SharedPreferences prefs;

    /**
     * A constructor is required, and must call the super IntentService(String)
     * constructor with a name for the worker thread.
     */
    public DommelDataService() {
        super("DommelDataService");

        this.volume_used = -1;
        this.volume_remaining = -1;
        this.unlimited = false;
        this.days_left = -1;
        this.reset_date = Calendar.getInstance();
    }

    /**
     * Does the actual work.
     */

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        final boolean manualUpdate = intent.getBooleanExtra(FIELD_MANUAL, false);

        // send broadcast containing error message
        Intent i = new Intent(MESSAGE_STATUS);
        i.putExtra(FIELD_MANUAL, manualUpdate);

        try {
            i.putExtra(FIELD_STATUS, STATUS_RUNNING);
            sendBroadcast(i);

            update();

            i.putExtra(FIELD_STATUS, STATUS_FINISHED);
            sendBroadcast(i);


        }
        catch (Exception e) {

        }

    }

    /**
     * The IntentService calls this method from the default worker thread with
     * the intent that started the service. When this method returns,
     * IntentService stops the service, as appropriate.
     */
    protected void update() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String username = prefs.getString("username", null);
        String password = prefs.getString("password", null);

        this.httpclient = new DefaultHttpClient();

        // store the result in the sharedpreferences

        try {
            // first check for internet connectivity

            @SuppressWarnings("unused")
            boolean success = getData(username, password);
            // TODO: what if this is not successful

            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            this.httpclient.getConnectionManager().shutdown();

            // send broadcast that everything succeeded
//            sendBroadcast(usageBroadcast);

        } catch (ClientProtocolException e) {
            this.handleError(e);
            //System.out.println(e.toString());
        } catch (IOException e) {
            this.handleError(e);
            //System.out.println(e.toString());
        } catch (Exception e)
        {
            this.handleError(e);
            //System.out.println(e.toString());
        }

    }

    public boolean getData(String username, String password)
            throws ClientProtocolException, IOException {


        // adapted from phptelemeter
        unlimited = false;

		/* login */
        Map<String, String> urlParameters = new HashMap<String, String>();
        urlParameters.put("op", "login");
        urlParameters.put("new_language", "english");
        urlParameters.put("submit", "login");
        urlParameters.put("username", username);
        urlParameters.put("password", password);

        String res;

//        try{
            res = this.httpPost(URL_LOGIN, urlParameters);
//
//        }catch(Exception e)
//        {
//        // TODO: check for errors in res. possibly just catch nullpointer
//        // exception..
//            throw new Exception(e);
//        }

		/* go to the packages page, and get the serv_id and client_id */
        res = this.httpGet(URL_PACKAGES);
        // TODO: check for errors in res

        String[] lines = res.split("\n");
        String log = null;
        int pos = 0;
        /* figure out the stats exact url */
        for (int i = 0; i < lines.length; i++) {
            pos = lines[i].indexOf(URL_STATS_INIT);
            if (pos >= 0) {
                log = lines[i].substring(pos);
                break;
            }
        }

        String url_stats = log.substring(0, log.indexOf("'"));

		/* and get the data */
        String data = this.httpGet(url_stats);

		/* logout */
        res = this.httpGet(URL_LOGOUT);

        lines = data.split("/n");
        String data2 = null;
        pos = 0;

		/* find the entry position */
        for (int i = 0; i < lines.length; i++) {
            pos = lines[i].indexOf("total traffic downloaded in broadband");
            if (pos >= 0) {
                data2 = lines[i].substring(pos);
                break;
            }
        }

        lines = data2.split("<br>");

		/* set some default positions */
        int pos_remaining = -1;
        int pos_traffic = -1;
        int pos_reset_date = -1;
        int pos_total = -1;
        @SuppressWarnings("unused")
        int strpos_total = -1;

		/* position finding & data cleanup */
        for (int i = 0; i < lines.length; i++) {
            lines[i] = stripTags(lines[i]);

            //System.out.println(lines[i]);

            if (lines[i].contains("total traffic downloaded")) {
                pos_traffic = i;
            } else if (lines[i].contains("next counter reset")) {
                pos_reset_date = i;
            } else if (lines[i].contains("remaining")) {
                pos_remaining = i;
                if (lines[i].contains("unlimited")) {
                    unlimited = true;
                }
            } else if (lines[i].contains("maximum datatransfer")) {
                pos_total = i;
				/* data cleanup */
                int test_ind = lines[i].indexOf("maximum datatransfer:");
                if (test_ind >= 0 && test_ind + 1 < lines[i].length()) {
                    lines[i] = lines[i].substring(test_ind + 21);
                }
            }

			/* data cleanup */
            int test_ind = lines[i].indexOf(":");
            if (test_ind >= 0 && test_ind + 1 < lines[i].length()) {
                lines[i] = lines[i].substring(test_ind + 2);
            }

        }

		/* stats */
		/* total used */
        volume_used = Float.parseFloat(lines[pos_traffic].substring(0,
                lines[pos_traffic].length() - 3)) * 1024;

        volume_remaining = 0;

		/* remaining, if exists? */
        if (pos_remaining >= 0) {
            if (!unlimited) {
                volume_remaining = Float.parseFloat(lines[pos_remaining]
                        .substring(0, lines[pos_remaining].length() - 3)) * 1024;

                if (pos_total >= 0) {
                    volume_total = Float.parseFloat(lines[pos_total].substring(
                            0, 4)) * 1024;
                }
            } else {
                // Unlimited account
                volume_remaining = Float.POSITIVE_INFINITY;
            }
        }

		/* reset date */
        String reset_date_str = lines[pos_reset_date].substring(0, 10);
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        try {
            reset_date.setTime(df.parse(reset_date_str));
        } catch (ParseException e) {
            e.printStackTrace();
            // TODO: handle error
            return false;
        }

        days_left = calculateDaysLeft(reset_date);


        Date now = new Date();

        Editor edit = prefs.edit();
        edit.putFloat("volume_used", volume_used);
        edit.putFloat("volume_remaining", volume_remaining);
        edit.putFloat("volume_total", volume_total);
        edit.putLong("reset_date", reset_date.getTimeInMillis());
        edit.putInt("days_left", days_left);
        edit.putBoolean("unlimited", unlimited);
        edit.putLong("last_update", now.getTime());
        edit.putBoolean("last_update_success", true);


        return edit.commit();

    }

    private String httpGet(String targetURL) throws ClientProtocolException,
            IOException {
        return executeHttp(targetURL, new HashMap<String, String>(), 0);
    }

    private String httpPost(String targetURL, Map<String, String> urlParameters)
            throws ClientProtocolException, IOException {
        return executeHttp(targetURL, urlParameters, 1);
    }

    private String executeHttp(String targetURL,
                               Map<String, String> urlParameters, int method)
            throws ClientProtocolException, IOException {

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        Iterator<Map.Entry<String, String>> it = urlParameters.entrySet()
                .iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> pairs = (Map.Entry<String, String>) it
                    .next();
            nvps.add(new BasicNameValuePair((String) pairs.getKey(),
                    (String) pairs.getValue()));
        }
        HttpResponse response = null;
        if (method == 1) {

            HttpPost httpost = new HttpPost(targetURL);
            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            response = httpclient.execute(httpost);

        } else {

            HttpGet httpget = new HttpGet(targetURL);
            response = httpclient.execute(httpget);

        }

        HttpEntity entity = response.getEntity();

        InputStream is = entity.getContent();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        String line;
        StringBuilder resp = new StringBuilder();
        while ((line = rd.readLine()) != null) {
            resp.append(line);
            resp.append('\n');
        }
        rd.close();

        is.close();

        return resp.toString();
    }

    public void handleError(Exception e) {
        Date now = new Date();

        Editor edit = prefs.edit();
        edit.putLong("last_update", now.getTime());
        edit.putBoolean("last_update_success", false);
        edit.commit();

        // send broadcast containing error message
        Intent i = new Intent(MESSAGE_STATUS);
        i.putExtra(FIELD_STATUS, STATUS_ERROR);
        i.putExtra(FIELD_EXCEPTION, e);
        sendBroadcast(i);
    }

    public static String stripTags(String text) {
        return text.replaceAll("\\<.*?\\>", "");
    }

    public static int calculateDaysLeft(Calendar reset_date) {
        Calendar today = Calendar.getInstance();
        return daysBetween(today, reset_date);
    }

    public static int daysBetween(Calendar startCal, Calendar endCal) {

        Calendar date = (Calendar) startCal.clone();
        int daysBetween = 0;
        while (date.before(endCal)) {
            date.add(Calendar.DAY_OF_MONTH, 1);
            daysBetween++;
        }
        return daysBetween;
    }

    //
    // public boolean isOnline() {
    // ConnectivityManager cm = (ConnectivityManager)
    // getSystemService(Context.CONNECTIVITY_SERVICE);
    // NetworkInfo netInfo = cm.getActiveNetworkInfo();
    // if (netInfo != null && netInfo.isConnectedOrConnecting()) {
    // return true;
    // }
    // return false;
    // }

    public boolean getDebug() {
        return prefs.getBoolean("set_debug", false);
    }

}

//public class InvalidCredentialsException extends java.lang.Exception
//{
//    public InvalidCredentialsException(){
//    }
//
//}