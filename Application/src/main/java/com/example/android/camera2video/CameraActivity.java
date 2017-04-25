/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.camera2video;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;

public class CameraActivity extends Activity implements SensorEventListener {

    private PowerManager powerManager;
    private PowerManager.WakeLock temporaryWakeLock;

    private SensorManager sensorManager;
    private Sensor proximitySensor;
    private Sensor lightSensor;

    private int lastLightValue = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, Camera2VideoFragment.newInstance())
                    .commit();
        }

        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "doorguard");
        wakeLock.acquire();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (proximitySensor == event.sensor) {
            if (event.values[0] < 5) {
                wake();
            } else {
                unwake();
            }
        } else if (lightSensor == event.sensor) {
            int newValue = (int) event.values[0];

            int valueChange = newValue - lastLightValue;
            if (lastLightValue > 0 && valueChange > 3) {
                wake();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        unwake();
                    }
                });
            }

            lastLightValue = newValue;
        }
    }

    private void wake() {
        if (temporaryWakeLock != null && temporaryWakeLock.isHeld()) {
            return;
        }

        temporaryWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                "temp-doorguard");
        temporaryWakeLock.acquire();
    }

    private void unwake() {
        if (temporaryWakeLock == null || !temporaryWakeLock.isHeld()) {
            return;
        }

        try {
            temporaryWakeLock.release();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
