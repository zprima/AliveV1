package com.sgt.primoz.alivev1;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.sgt.primoz.alivev1.Constants.Constants;
import com.sgt.primoz.alivev1.Constants.Mode;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;


public class MainActivity extends Activity {
    public DBSettingRepo dbSettingRepo;
    public DBSetting setting;
    public String token;
    public String previousState;
    public Date previousDate;
    public Constants.Modes Modes;
    public Map<String,Long> statustoday;
    public int txtSumId;
    public Calendar calendarSum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            //check for internet
            if(isNetworkAvailable()==false){
                new AlertDialog.Builder(this)
                        .setTitle("Error")
                        .setMessage("No internet connection")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                                finish();
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                return;
            }


            //load the modes
            Modes = new Constants.Modes();
            //get login username and password from db
            dbSettingRepo = new DBSettingRepo(this);
            try {
                //open repository
                dbSettingRepo.open();
                //get setting
                setting = dbSettingRepo.getSetting();
                //check is setting is null
                if(setting==null){
                    //create a new setting object
                    setting = new DBSetting();
                }
                //close repository
                dbSettingRepo.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            //default fragment is the main fragment
            Fragment fragment = new MainFragment();
            //if both username and password are empty, load the LoginFragment
            if(setting.Username=="" && setting.Password==""){
                fragment = new SettingFragment();
            }
            else{
                Bundle args = new Bundle();
                args.putBoolean("LOGIN_FLAG", true);
                fragment.setArguments(args);
            }
            //load the fragment
            getFragmentManager().beginTransaction()
                    .add(R.id.container,fragment)
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, new SettingFragment())
                    .addToBackStack(null)
                    .commit();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //Main fragment
    public static class MainFragment extends Fragment{
        public MainFragment(){}

        //load status http post call async
        private class HttpLoadStausAsync extends AsyncTask{

            @Override
            protected Object doInBackground(Object[] objects) {
                //get url from objects
                String url = objects[0].toString();
                //create client
                HttpClient c = ((MainActivity)getActivity()).createClient();
                //build json object
                JSONObject jsonObject = new JSONObject();
                JSONObject otherData = new JSONObject();
                try {
                    jsonObject.accumulate("actionName","ldWoSt");
                    otherData.accumulate("newD", TodayISO8601());
                    jsonObject.accumulate("otherData",otherData);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //crate post and give me the result
                JSONObject objResult = ((MainActivity)getActivity())
                        .createAndExecutePost(c, url + Constants.sAPI + "/s/PBBws_Action", jsonObject, true);
                //return json result
                return objResult;
            }

            @Override
            protected void onPostExecute(Object o) {
                if(o!=null){
                    JSONObject obj = (JSONObject)o;
                    JSONArray data = null;
                    try {
                        data = obj.getJSONArray("data");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    MainActivity activity = (MainActivity)getActivity();
                    activity.previousState = "";
                    activity.previousDate = ISO2Date(null);

                    if(data.length()>0){
                        try {
                            //first
                            JSONObject first = data.getJSONObject(0);
                            String newState = first.getString("NewState");
                            String newDat = first.getString("NewDat");
                            activity.previousState = newState;
                            activity.previousDate = ISO2Date(newDat);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        activity.statustoday = new HashMap<String,Long>();

                        Calendar todayCal = Calendar.getInstance();

                        //data contains a collection of records, group them by state and show only current date
                        for(int i=0;i<data.length();i++){
                            try{
                                JSONObject x = data.getJSONObject(i);
                                //OldState -> NewState
                                String oldState = x.getString("OldState");
                                if(oldState.equals("absent")){
                                    //skiping absent status
                                    continue;
                                }
                                //String newState = x.getString("NewState");
                                //kdaj je veljal oldstate pa od OldDat do NewDat
                                Date oldDate = ISO2Date(x.getString("OldDat"));
                                Date newDate = ISO2Date(x.getString("NewDat"));
                                Calendar newCal = Calendar.getInstance();
                                newCal.setTime(newDate);
                                Calendar oldCal = Calendar.getInstance();
                                oldCal.setTime(oldDate);

                                //is new cal today and old cal by day is not?
                                if((todayCal.get(Calendar.MONTH)==newCal.get(Calendar.MONTH))
                                    && (todayCal.get(Calendar.DAY_OF_MONTH)==newCal.get(Calendar.DAY_OF_MONTH))
                                    && (todayCal.get(Calendar.MONTH)==oldCal.get(Calendar.MONTH))
                                    && (todayCal.get(Calendar.DAY_OF_MONTH)!=oldCal.get(Calendar.DAY_OF_MONTH))
                                ){
                                    oldCal.set(Calendar.DAY_OF_MONTH,todayCal.get(Calendar.DAY_OF_MONTH));
                                    oldCal.set(Calendar.HOUR_OF_DAY,0);
                                    oldCal.set(Calendar.MINUTE,0);
                                    oldDate = oldCal.getTime();
                                }
                                //is new cal today and old cal by month is not
                                else if((todayCal.get(Calendar.MONTH)==newCal.get(Calendar.MONTH))
                                        && (todayCal.get(Calendar.DAY_OF_MONTH)==newCal.get(Calendar.DAY_OF_MONTH))
                                        && (todayCal.get(Calendar.MONTH)!=oldCal.get(Calendar.MONTH))
                                ){
                                    oldCal.set(Calendar.DAY_OF_MONTH,todayCal.get(Calendar.DAY_OF_MONTH));
                                    oldCal.set(Calendar.HOUR_OF_DAY,0);
                                    oldCal.set(Calendar.MINUTE,0);
                                    oldDate = oldCal.getTime();
                                }
                                //is everything on the same month and day
                                else if((todayCal.get(Calendar.MONTH)==newCal.get(Calendar.MONTH))
                                        && (todayCal.get(Calendar.DAY_OF_MONTH)==newCal.get(Calendar.DAY_OF_MONTH))
                                        && (todayCal.get(Calendar.MONTH)==oldCal.get(Calendar.MONTH))
                                        && (todayCal.get(Calendar.DAY_OF_MONTH)==oldCal.get(Calendar.DAY_OF_MONTH))
                                        ){
                                    //do nothing, for the sake on escaping the last else statement
                                }
                                else{
                                    continue;
                                }


                                //does statustoday contain state?
                                if(activity.statustoday.containsKey(oldState)){
                                    //add time to existing value for this key
                                    Long time = activity.statustoday.get(oldState);
                                    Calendar c = diffBetweenDates(newDate,oldDate);
                                    time = addToCalendar(time, c.getTimeInMillis()).getTimeInMillis();
                                    activity.statustoday.put(oldState, time);
                                }
                                else{
                                    //create new key-value pair
                                    Calendar c = diffBetweenDates(newDate,oldDate);
                                    activity.statustoday.put(oldState, c.getTimeInMillis() );
                                }
                            }
                            catch (JSONException e){
                                e.printStackTrace();
                            }
                        }

                    }
                    //go to commands fragment
                    getFragmentManager().beginTransaction()
                            .replace(R.id.container, new CommandFragment())
                            .commit();

                }
                else{
                    //display that something has gone wrong
                    //alert dialog for the error
                    new AlertDialog.Builder(getActivity())
                            .setTitle("Error")
                            .setMessage("Something went wrong :/")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();

                    ((ProgressBar)getActivity().findViewById(R.id.progressBar)).setIndeterminate(false);
                    ((ProgressBar)getActivity().findViewById(R.id.progressBar)).setProgress(100);
                    ((TextView)getActivity().findViewById(R.id.txtLoading)).setText("Not successful");
                }


            }
        }

        private class HttpLoginAsync extends AsyncTask {
            /*private final TaskListener listener;
            public HttpLoginAsync(TaskListener listener){
                this.listener = listener;
            }*/

            @Override
            protected Object doInBackground(Object[] objects) {
                String url = objects[0].toString();
                String username = objects[1].toString();
                String password = objects[2].toString();
                //make http client
                HttpClient c = ((MainActivity)getActivity()).createClient();
                //build json object
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.accumulate("username", username);
                    jsonObject.accumulate("password", password);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //get result from post
                JSONObject result = ((MainActivity)getActivity())
                        .createAndExecutePost(c,url + Constants.sAPI + "/login", jsonObject,false);
                //return result
                return result;
            }

            @Override
            protected void onPostExecute(Object o) {
                JSONObject json = (JSONObject)o;
                Boolean hasError = false;
                try{
                    String x = json.getString("error");
                    hasError = true;
                }
                catch (JSONException e){
                    e.printStackTrace();
                }
                //String result = "ok";
                if(hasError){
                    //result = "error";
                    String msg = "android fail get string from json";
                    try {
                        msg = json.getString("error");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    //alert dialog for the error
                    new AlertDialog.Builder(getActivity())
                            .setTitle("Error")
                            .setMessage(msg)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();

                    ((ProgressBar)getActivity().findViewById(R.id.progressBar)).setIndeterminate(false);
                    ((ProgressBar)getActivity().findViewById(R.id.progressBar)).setProgress(100);
                    ((TextView)getActivity().findViewById(R.id.txtLoading)).setText("Not successful");
                }
                else{
                    String token = null;
                    try {
                        token = json.getString("token");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    ((MainActivity)getActivity()).token = token;

                    //get current status
                    new HttpLoadStausAsync().execute(new Object[]{((MainActivity)getActivity()).setting.Url});
                }
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            MainActivity activity = (MainActivity)getActivity();
            activity.setTitle("Alive V1");
            return rootView;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            //get setting from activity
            DBSetting setting = ((MainActivity)getActivity()).setting;

            Bundle args = getArguments();
            Boolean login_flag = args.getBoolean("LOGIN_FLAG");
            if(login_flag) {
                //login
                new HttpLoginAsync().execute(new Object[]{setting.Url, setting.Username, setting.Password});
            }
            else{
                new HttpLoadStausAsync().execute(new Object[]{((MainActivity)getActivity()).setting.Url});
            }
        }
    }

    private HttpClient createClient(){
        HttpClient c = new DefaultHttpClient();
        HttpParams httpParameters = c.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, 5000);
        HttpConnectionParams.setSoTimeout(httpParameters, 10000);
        return c;
    }

    private JSONObject createAndExecutePost(HttpClient c, String url, JSONObject json, boolean addTokenHeader){
        //make post request
        HttpPost post = new HttpPost(url);
        if(addTokenHeader)
            post.setHeader("Authenticate","Bearer " + this.token);
        post.addHeader("content-type", "application/x-www-form-urlencoded");
        //convert json object to string
        String sjson = json.toString();
        //set json to StringEntity
        StringEntity se = null;
        try {
            se = new StringEntity(sjson);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        //set http post entity
        post.setEntity(se);
        //execute post call
        HttpResponse response = null;
        try {
            response = c.execute(post);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //get context as stream
        InputStream inputStream = null;
        try {
            inputStream = response.getEntity().getContent();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //convert input stream to string
        String result = "";
        if (inputStream != null) {
            try {
                result = convertResponse(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //create a json object
        JSONObject objResult = null;
        try {
            objResult = new JSONObject(result);
        } catch (JSONException e) {
            e.printStackTrace();

        }
        return objResult;
    }

    private static String convertResponse(InputStream inputStream) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
        String line="";
        String result = "";
        while((line = r.readLine())!=null){
            if(!line.equals(")]}',"))
                result += line;
        }
        inputStream.close();
        return result;
    }

    private static String TodayISO8601() {
        //TimeZone tz = TimeZone.getTimeZone("UTC");
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        df.setTimeZone(tz);
        String nowAsISO = df.format(new Date());
        return nowAsISO;
    }

    private static String DT2ISO8601(Date date) {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        df.setTimeZone(tz);
        String nowAsISO = df.format(date);
        return nowAsISO;
    }

    private static Date ISO2Date(String iso) {
        if(iso==null){
            return new Date();
        }
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        df.setTimeZone(tz);
        Date date = new Date();
        try {
            date = df.parse(iso);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    private static String HumanizeDate(Date date){
        DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();
        df.setTimeZone(tz);
        return df.format(date);
    }

    private static String HumanizeTime(Date date){
        DateFormat df = new SimpleDateFormat("HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();
        df.setTimeZone(tz);
        return df.format(date);
    }

    private static Calendar diffBetweenDates(Date d1, Date d2){
        //in milliseconds
        long diff = d1.getTime() - d2.getTime();
        long diffSeconds = diff / 1000 % 60;
        long diffMinutes = diff / (60 * 1000) % 60;
        long diffHours = diff / (60 * 60 * 1000) % 24;
        long diffDays = diff / (24 * 60 * 60 * 1000);

        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY,(int)diffHours);
        c.set(Calendar.MINUTE,(int)diffMinutes);
        c.set(Calendar.SECOND,(int)diffSeconds);

        return c;
    }

    private static Calendar addToCalendar(long t1, long t2){
        Calendar c1 = Calendar.getInstance();
        c1.setTimeInMillis(t1);

        Calendar c2 = Calendar.getInstance();
        c2.setTimeInMillis(t2);

        c1.add(Calendar.SECOND,c2.get(Calendar.SECOND));
        c1.add(Calendar.MINUTE,c2.get(Calendar.MINUTE));
        c1.add(Calendar.HOUR_OF_DAY,c2.get(Calendar.HOUR_OF_DAY));

        return c1;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    //Setting fragment
    public static class SettingFragment extends Fragment{
        public SettingFragment(){}
        public final String SETTING_SETTING = "setting_setting";

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_setting,container,false);
            ((MainActivity)getActivity()).setTitle("Settings");
            DBSetting setting = ((MainActivity)getActivity()).setting;
            final TextView txtUsername = ((TextView)rootView.findViewById(R.id.txtUsername));
            txtUsername.setText(setting.Username);
            final TextView txtPassword = ((TextView)rootView.findViewById(R.id.txtPassword));
            txtPassword.setText(setting.Password);
            final TextView txtUrl = ((TextView)rootView.findViewById(R.id.txtUrl));
            txtUrl.setText(setting.Url);

            ((Button)rootView.findViewById(R.id.btnSave)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //save inputs to DB
                    DBSettingRepo dbSettingRepo = ((MainActivity)getActivity()).dbSettingRepo;
                    try {
                        dbSettingRepo.open();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    //store in setting
                    DBSetting setting = ((MainActivity)getActivity()).setting;
                    setting.Username = txtUsername.getText().toString();
                    setting.Password = txtPassword.getText().toString();
                    setting.Url = txtUrl.getText().toString();
                    //save in db
                    try {
                        dbSettingRepo.save(setting);
                        ((MainActivity)getActivity()).setting = setting;
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    //close db
                    try {
                        dbSettingRepo.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    //redirect to main fragment
                    Fragment f = new MainFragment();
                    Bundle args = new Bundle();
                    args.putBoolean("LOGIN_FLAG", true);
                    f.setArguments(args);

                    getFragmentManager().beginTransaction()
                            .replace(R.id.container,f)
                            .addToBackStack(null)
                            .commit();
                }
            });

            return rootView;
        }

    }

    //Command fragment
    public static class CommandFragment extends Fragment{
        public CommandFragment(){}

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_commands,container,false);
            return rootView;
        }

        private class HttpChangeAsync extends AsyncTask {

            @Override
            protected void onPreExecute() {
                //before call, show progress dialog
            }

            @Override
            protected Object doInBackground(Object[] objects) {
                MainActivity activity = ((MainActivity)getActivity());

                String keyword = objects[0].toString();
                //create client
                HttpClient c = ((MainActivity)getActivity()).createClient();
                //build json
                JSONObject json = new JSONObject();
                try {
                    json.accumulate("actionName","wrWoSt");
                    JSONObject otherData = new JSONObject();
                    otherData.accumulate("newD",TodayISO8601());
                    otherData.accumulate("newS",keyword);
                    otherData.accumulate("oldD",DT2ISO8601(activity.previousDate));
                    otherData.accumulate("oldS",activity.previousState);
                    json.accumulate("otherData",otherData);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                //post and get result
                JSONObject result = ((MainActivity)getActivity()).createAndExecutePost(
                        c, activity.setting.Url + Constants.sAPI + "/s/PBBws_Action",json, true);
                return result;
            }

            @Override
            protected void onPostExecute(Object o) {
                if(o!=null){
                    JSONObject json = (JSONObject)o;
                    //after call, disable progress dialog
                    //after that, change fragment to loading, but without the login step
                    Fragment f = new MainFragment();
                    Bundle args = new Bundle();
                    args.putBoolean("LOGIN_FLAG", false);
                    f.setArguments(args);

                    getFragmentManager().beginTransaction()
                            .replace(R.id.container,f)
                            .commit();
                }
                else{
                    //display an alert dialog
                }
            }
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            //helper for activity
            final MainActivity activity = (MainActivity)getActivity();
            //get all modes for current mode
            Mode m = activity.Modes.modes.get(activity.previousState);
            //set text for mode name and date
            ((TextView)activity.findViewById(R.id.txtPreviousState)).setText(m.name);
            ((TextView)activity.findViewById(R.id.txtPreviousDate)).setText(HumanizeDate(activity.previousDate));

            //create items for every state group and display state name and duration
            //get linear layout
            LinearLayout ltimestatus = (LinearLayout)activity.findViewById(R.id.ltimestatus);
            //placeholder for summary
            Calendar calendarSum = null;
            //for every state
            for(Map.Entry<String,Long> kvp : activity.statustoday.entrySet()){
                //if sum was no previously defined, set it now
                if(calendarSum==null){
                    calendarSum = Calendar.getInstance();
                    calendarSum.setTimeInMillis(kvp.getValue());
                }
                else{
                    //add to the sum, current time for the current state
                    calendarSum = addToCalendar(calendarSum.getTimeInMillis(),kvp.getValue());
                }
                //create a new text view for this state
                TextView t = new TextView(activity);
                //set layout params
                t.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                //set text
                t.setText(activity.Modes.modes.get(kvp.getKey()).name + ": " + HumanizeTime(new Date(kvp.getValue())));
                //add to linear layout
                ltimestatus.addView(t);
            }
            //add a sum
            //get the calulated calendar sum
            activity.calendarSum = calendarSum;
            //create a new text view
            final TextView txtSum = new TextView(activity);
            //give it an id
            activity.txtSumId = View.generateViewId();
            txtSum.setId(activity.txtSumId);
            //set layout params
            txtSum.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            //check if calendarSum is null
            if(calendarSum==null){
                //set text
                txtSum.setText("Danes še ni blo nič");
            }
            else {
                //set text
                txtSum.setText("Skupaj: " + HumanizeTime(calendarSum.getTime()));
            }
            //add it to the linear layout
            ltimestatus.addView(txtSum);

            //get the chronometer control
            final Chronometer chrono = ((Chronometer)activity.findViewById(R.id.chronoCurrentDuration));
            //set ticker listener
            chrono.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
                @Override
                public void onChronometerTick(Chronometer chronometer) {
                    //if current state is absent, do not update anything
                    if(!activity.previousState.equals("absent")){
                        //on every tick, calc the diference in date/time
                        Calendar c = diffBetweenDates(new Date(System.currentTimeMillis()), activity.previousDate);
                        //display the new time in human friendly way
                        chrono.setText(HumanizeTime(c.getTime()));

                        //get the txt for sum
                        TextView txtSum = (TextView)activity.findViewById(activity.txtSumId);
                        //if no such txt view was found, do nothing
                        if(txtSum!=null){
                            //add to the calendar sum calculated earlier, the time from chronometer
                            Calendar calendarSum = addToCalendar(activity.calendarSum.getTimeInMillis(), c.getTimeInMillis());
                            //display the new time in human friendly way
                            txtSum.setText("Skupaj: " + HumanizeTime(calendarSum.getTime()));
                        }
                    }
                }
            });

            if(!activity.previousState.equals("absent")){
                //start the chronometer
                chrono.start();
            }

            //create linear layouts and buttons
            //how many buttons
            int btnCount = m.otherModes.length;
            int linearCount = (int)Math.ceil((double)btnCount/(double)2);

            LinearLayout master = (LinearLayout)activity.findViewById(R.id.btnLinearContainer);

            int currentBtnCounter = 0;
            for(int i=0;i<linearCount;i++){
                LinearLayout l = new LinearLayout(activity);
                l.setOrientation(LinearLayout.HORIZONTAL);
                l.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));

                //add up to 2 buttons
                for(int j=0;j<2;j++){
                    if(currentBtnCounter>=btnCount)
                        break;

                    //create a button
                    Button b = new Button(activity);
                    String key = m.otherModes[currentBtnCounter];
                    b.setText(activity.Modes.modes.get(key).name);
                    b.setTag(key);
                    TableRow.LayoutParams tr = new TableRow.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT,1f
                    );
                    int dp = Constants.dpToPx(5);
                    tr.setMargins(dp,dp,dp,dp);
                    b.setLayoutParams(tr);
                    dp = Constants.dpToPx(15);
                    b.setPadding(dp,dp,dp,dp);

                    //set click event
                    b.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //get tag - keyword
                            String keyword = ((Button)view).getTag().toString();
                            //call server for change
                            new HttpChangeAsync().execute(new Object[]{keyword});
                        }
                    });

                    l.addView(b);

                    currentBtnCounter++;
                }
                //add linear to master linear
                master.addView(l);
            }

        }
    }

}
