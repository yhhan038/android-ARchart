package com.example.embeddedvis_arcore;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static android.location.LocationManager.GPS_PROVIDER;
public class SensorControl extends AppCompatActivity {

    public Context context;
    private SensorManager sensorManager;
    public LocationManager locationManager;
    private SensorEventListener sensorListener;
    private Sensor gyroSensor;
    public Location location;

    public double aX, aY, aZ;
    public String gpsAddr = "울산광역시";
    public String localAddress = "ulsan";
    public String displayedAddress = "울산광역시";

    public SensorControl(Context context) {
        this.context = context;
    }

    public void setSensorListener() {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        sensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                    aX = sensorEvent.values[0];
                    aY = sensorEvent.values[1];
                    aZ = sensorEvent.values[2];
                    ((MainActivity) context).gyroValue.setText("G(" + String.format("%.3f", aX) + ", " + String.format("%.3f", aY) + ", " + String.format("%.3f", aZ) + ")");
                }
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int i) { }
        };

        if (ContextCompat.checkSelfPermission( context, android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions( this, new String[] {  android.Manifest.permission.ACCESS_FINE_LOCATION  },
                    0 );
        } else {
            location = locationManager.getLastKnownLocation(GPS_PROVIDER);
        }

    }

    public void registerSensor() {
        sensorManager.registerListener(sensorListener, gyroSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    public void unregisterSensor() {
        sensorManager.unregisterListener(sensorListener);
    }

}
