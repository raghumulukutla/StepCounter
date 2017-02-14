package com.raghu.stepcounter;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.opencsv.CSVWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private CSVWriter csvWriter;
    private File path;
    private float[] prev = {0f,0f,0f};
    private File file;
    private Menu menu;
    private TextView stepView;
    private int stepCount = 0;
    private static final int ABOVE = 1;
    private static final int BELOW = 0;
    private static int CURRENT_STATE = 0;
    private static int PREVIOUS_STATE = BELOW;
    private LineGraphSeries<DataPoint> rawData;
    private LineGraphSeries<DataPoint> lpData;
    private GraphView graphView;
    private GraphView graphView1;
    private GraphView combView;
    private int rawPoints = 0;
    private int sampleCount = 0;
    private long startTime;
    boolean SAMPLING_ACTIVE = true;
    private long streakStartTime;
    private long streakPrevTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        stepView = (TextView) findViewById(R.id.count);
        path =  this.getExternalFilesDir(null);
        file = new File(path, "raghu1.csv");
        try {
            csvWriter = new CSVWriter(new FileWriter(file));
        } catch (IOException e) {
            e.printStackTrace();
        }

        rawData = new LineGraphSeries<>();
        rawData.setTitle("Raw Data");
        rawData.setColor(Color.RED);
        lpData = new LineGraphSeries<>();
        lpData.setTitle("Smooth Data");
        lpData.setColor(Color.BLUE);
        graphView = (GraphView) findViewById(R.id.rawGraph);
        graphView1 = (GraphView) findViewById(R.id.lpGraph);
        graphView.getViewport().setYAxisBoundsManual(true);
        graphView.getViewport().setMinY(-40);
        graphView.getViewport().setMaxY(30);
        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setMinX(4);
        graphView.getViewport().setMaxX(80);
        // enable scaling and scrolling
//        graphView.getViewport().setScalable(true);
//        graphView.getViewport().setScalableY(true);
//        graphView.getViewport().setScrollable(true); // enables horizontal scrolling
//        graphView.getViewport().setScrollableY(true); // enables vertical scrolling
//        graphView.getViewport().setScalable(true); // enables horizontal zooming and scrolling
//        graphView.getViewport().setScalableY(true); // enables vertical zooming and scrolling
        graphView.addSeries(rawData);



        // set manual X bounds
        graphView1.getViewport().setYAxisBoundsManual(true);
        graphView1.getViewport().setMinY(-30);
        graphView1.getViewport().setMaxY(30);
        graphView1.getViewport().setXAxisBoundsManual(true);
        graphView1.getViewport().setMinX(4);
        graphView1.getViewport().setMaxX(80);
        // enable scaling and scrolling
//        graphView1.getViewport().setScalable(true);
//        graphView1.getViewport().setScalableY(true);
//        graphView1.getViewport().setScrollable(true); // enables horizontal scrolling
//        graphView1.getViewport().setScrollableY(true); // enables vertical scrolling
//        graphView1.getViewport().setScalable(true); // enables horizontal zooming and scrolling
//        graphView1.getViewport().setScalableY(true); // enables vertical zooming and scrolling
        graphView1.addSeries(lpData);

        combView = (GraphView) findViewById(R.id.combGraph);

        combView.getViewport().setYAxisBoundsManual(true);
        combView.getViewport().setMinY(-70);
        combView.getViewport().setMaxY(70);
        combView.getViewport().setXAxisBoundsManual(true);
        combView.getViewport().setMinX(4);
        combView.getViewport().setMaxX(80);

        combView.addSeries(rawData);
        combView.addSeries(lpData);
        streakPrevTime = System.currentTimeMillis() - 500;
    }


    @Override
    protected void onResume() {
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        super.onResume();
        startTime = System.currentTimeMillis();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.instrumentation) {
            SAMPLING_ACTIVE = true;
            sampleCount = 0;
            startTime = System.currentTimeMillis();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleEvent(SensorEvent event) {
        prev = lowPassFilter(event.values,prev);
        Accelerometer raw = new Accelerometer(event.values);
        Accelerometer data = new Accelerometer(prev);
        StringBuilder text = new StringBuilder();
        text.append("X: " + data.X);
        text.append("Y: " + data.Y);
        text.append("Z: " + data.Z);
        text.append("R: " + data.R);
        rawData.appendData(new DataPoint(rawPoints++,raw.R), true,1000);
        lpData.appendData(new DataPoint(rawPoints, data.R), true, 1000);
        if(data.R > 10.5f){
            CURRENT_STATE = ABOVE;
            if(PREVIOUS_STATE != CURRENT_STATE) {
                streakStartTime = System.currentTimeMillis();
                if ((streakStartTime - streakPrevTime) <= 250f) {
                    streakPrevTime = System.currentTimeMillis();
                    return;
                }
                streakPrevTime = streakStartTime;
                Log.d("STATES:", "" + streakPrevTime + " " + streakStartTime);
                stepCount++;
            }
            PREVIOUS_STATE = CURRENT_STATE;
        }
        else if(data.R < 10.5f) {
            CURRENT_STATE = BELOW;
            PREVIOUS_STATE = CURRENT_STATE;
        }
        stepView.setText(""+(stepCount));;
//        String[] text1 = new String[4];
//        text1[0] = Long.toString(System.currentTimeMillis());
//        text1[1] = Float.toString(data.X);
//        text1[2] = Float.toString(data.Y);
//        text1[3] = Float.toString(data.Z);
//        csvWriter.writeNext(text1);
    }

    private float[] lowPassFilter(float[] input, float[] prev) {
        float ALPHA = 0.1f;
        if(input == null || prev == null) {
            return null;
        }
        for (int i=0; i< input.length; i++) {
            prev[i] = prev[i] + ALPHA * (input[i] - prev[i]);
        }
        return prev;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            handleEvent(event);
            if(SAMPLING_ACTIVE) {
                sampleCount++;
                long now = System.currentTimeMillis();
                if (now >= startTime + 5000) {
                    double samplingRate = sampleCount / ((now - startTime) / 1000.0);
                    SAMPLING_ACTIVE = false;
                    Toast.makeText(getApplicationContext(), "Sampling rate of your device is " + samplingRate + "Hz", Toast.LENGTH_LONG).show();
                    MenuItem rate = menu.findItem(R.id.instrumentation);
                    rate.setTitle("Sampling Rate : " + samplingRate + "hz");
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
