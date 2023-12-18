package com.as.healthcaredeu;

import org.json.JSONObject;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.telecom.Call;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class GraphActivity extends AppCompatActivity implements SensorEventListener{

    private static String getUserInfoUrl = "http://20.125.193.10:80/api/get_user_info";
    private static String getbmiUrl = "http://20.125.193.10:80/api/bmi";
    private static String getWeightUrl = "http://20.125.193.10:80/api/get_weight";
    // Graph for showing weights
    private GraphView weightGraph;

    int stepCount = 0;
    int targetStepCount = 0; // Declare it at the class level
    ImageView statisticsButton;
    ImageView notificationButton;
    ImageView settingsButton;
    // Graph for showing steps
    BarChart stepsBar;
    TextView displayWeight;
    TextView displayName;

    TextView stepCounter;
    SensorManager sensorManager;
    Sensor mStepCounter;
    boolean isCounterSensorPresent;
    TextView bmi;

    private LineChart lineChart;
    private RequestQueue requestQueue;
    private LineGraphSeries<DataPoint> weights;
    private HashMap<Date, Double> weightMap;
    TextView alertView;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);

        weightMap = new HashMap<>();



        setContentView(R.layout.statistics);

        alertView = findViewById(R.id.alert);
        // getting graph
        weightGraph = (GraphView) findViewById(R.id.graph);
        weightGraph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    // Convert the value back to a date
                    Date date = new Date((long) value);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM");
                    return sdf.format(date);
                } else {
                    // Let the Y-values be normal decimal values
                    return super.formatLabel(value, isValueX);
                }
            }
        });


        weights = new LineGraphSeries<>();

        statisticsButton = findViewById(R.id.statisticsButton);
        notificationButton = findViewById(R.id.notificationButton);
        settingsButton = findViewById(R.id.settingsButton);
        bmi = findViewById(R.id.textViewBmi);

        // Retrieve the username from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String username = sharedPreferences.getString("username", "");
        displayName = findViewById(R.id.textViewName);
        displayName.setText("Welcome, " + username);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        stepCounter = findViewById(R.id.currentStepCount);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Start the StepResetService when your app starts
        Intent serviceIntent = new Intent(this, StepResetService.class);
        startService(serviceIntent);

        fetchUserInfo(username);

        if(sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null) {
            mStepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            isCounterSensorPresent = true;
        } else {
            stepCounter.setText("Step Sensor is not present");
            isCounterSensorPresent = false;
        }

        getWeight(username);
        bmiCalc(username);
        detectDangerousWeightLoss();
        statisticsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(GraphActivity.this,GraphActivity.class);
                startActivity(intent);
            }
        });

        notificationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(GraphActivity.this, NotificationActivity.class);
                startActivity(intent);
            }
        });


        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(GraphActivity.this,SettingsActivity.class);
                startActivity(intent);
            }
        });
    }

    private void detectDangerousWeightLoss(){

        Iterator<Date> dateIterator = weightMap.keySet().iterator();
        while (dateIterator.hasNext()){
            Date date = dateIterator.next();
            Double weight = weightMap.get(date);
            Double upperLimit = weight * 1.05;
            Double bottomLimit = weight * 0.95;
            // plus one month in milliseconds
            Date oneMonthAfter = new Date((long) (date.getTime() + 2629800000.0));
            Iterator<Date> dateIteratorToCompare = weightMap.keySet().iterator();
            while (dateIteratorToCompare.hasNext()){
                Date dateToCompare = dateIterator.next();
                if(dateToCompare.after(date) && dateToCompare.before(oneMonthAfter) || dateToCompare.equals(date)){
                    Double weightToCompare = weightMap.get(dateToCompare);
                    if(weightToCompare <= bottomLimit || weightToCompare >= upperLimit){
                        alertView.setText("Between " + date.toString() + " and " + dateToCompare.toString() + " dangerous weight loss or gain." );
                    }
                }
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor == mStepCounter) {
            stepCount = (int) sensorEvent.values[0];
            stepCounter.setText(String.valueOf(stepCount));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onResume() {
        super.onResume();
        if (sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)!=null){
            sensorManager.registerListener(this, mStepCounter, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }
    @Override
    public void onPause() {
        super.onPause();
        if (sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)!=null){
            sensorManager.unregisterListener(this, mStepCounter);
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    private void fetchUserInfo(final String username) {
        JSONObject requestData = new JSONObject();
        try {
            requestData.put("username", username);
        } catch (JSONException e) {
            e.printStackTrace();
        }

/*        MyVolleyRequest.postRequest(getApplicationContext(), getUserInfoUrl, requestData, new MyVolleyRequest.VolleyCallback() {
            @Override
            public void onSuccess(String result) {
                try {
                    JSONObject response = new JSONObject(result);
                    //String targetWeight = response.getString("targetweight");
                    String targetSteps = response.getString("targetsteps");

                    targetStepCount = Integer.parseInt(targetSteps);
                    int progress = (int) stepCount / targetStepCount;
                    CircularProgressBar circularProgressBar = findViewById(R.id.progress_circular);

                    // Set the progress to the CircularProgressBar
                    circularProgressBar.setProgress(progress * 100); // Progress should be in the range 0-100

                    //TextView targetWeightTextView = findViewById(R.id.weightGoal);
                    TextView targetStepsTextView = findViewById(R.id.stepCountGoal);

                    //targetWeightTextView.setText(targetWeight + " kg");
                    targetStepsTextView.setText(targetSteps + " steps");

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onError(String error) {
                // Handle error when fetching user information
                Toast.makeText(GraphActivity.this, "Error fetching user information", Toast.LENGTH_SHORT).show();
            }
        });*/
    }


    private void bmiCalc(String username){
        JSONObject requestData = new JSONObject();
        try {
            requestData.put("username", username);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        MyVolleyRequest.postRequest(getApplicationContext(), getbmiUrl, requestData, new MyVolleyRequest.VolleyCallback() {
            @Override
            public void onSuccess(String result)  {

                try {
                    JSONObject response = new JSONObject(result);
                    String bmitext = response.getString("message");

                    bmi.setText(bmitext);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onError(String error) {
                // Handle error when fetching user information
                bmi.setText("null");
            }
        });

    }

    private void getWeight(String username){
        JSONObject requestData = new JSONObject();
        try {
            requestData.put("username", username);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        MyVolleyRequest.postRequest(getApplicationContext(), getWeightUrl, requestData, new MyVolleyRequest.VolleyCallback() {
            @Override
            public void onSuccess(String result)  {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                try {
                    JSONObject response = new JSONObject(result);
                    if(result.equals(null)) return;
                    JSONArray jArray = response.getJSONArray("message");
                    for (int i = 0; i < jArray.length(); i++) {
                        String dateString = jArray.getJSONObject(i).getString("DATE");
                        Double weight = jArray.getJSONObject(i).getDouble("Weight");
                        Date date = formatter.parse(dateString);
                        weightMap.put(date, weight);
                        DataPoint dp = new DataPoint(date, weight);
                        weights.appendData(dp, true, 50, false);
                    }
                    weightGraph.addSeries(weights);
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
            @Override
            public void onError(String error) {
                // Handle error when fetching user information
                displayWeight.setText("null");
            }
        });
    }
}