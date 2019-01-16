// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** SensorsPlugin */
public class SensorsPlugin implements EventChannel.StreamHandler {
  private static final String ACCELEROMETER_CHANNEL_NAME =
      "plugins.flutter.io/sensors/accelerometer";
  private static final String GYROSCOPE_CHANNEL_NAME = "plugins.flutter.io/sensors/gyroscope";
  private static final String USER_ACCELEROMETER_GRAVITY_CHANNEL_NAME =
          "plugins.flutter.io/sensors/user_accel_gravity";

  /** Plugin registration. */
  public static void registerWith(Registrar registrar) {
    final EventChannel accelerometerChannel =
        new EventChannel(registrar.messenger(), ACCELEROMETER_CHANNEL_NAME);
    accelerometerChannel.setStreamHandler(
        new SensorsPlugin(registrar.context(), Sensor.TYPE_ACCELEROMETER));

    final EventChannel userAccelGravityChannel =
        new EventChannel(registrar.messenger(), USER_ACCELEROMETER_GRAVITY_CHANNEL_NAME);
    userAccelGravityChannel.setStreamHandler(
        new SensorsPlugin(registrar.context(), Sensor.TYPE_LINEAR_ACCELERATION, Sensor.TYPE_GRAVITY));

    final EventChannel gyroscopeChannel =
            new EventChannel(registrar.messenger(), GYROSCOPE_CHANNEL_NAME);
    gyroscopeChannel.setStreamHandler(
            new SensorsPlugin(registrar.context(), Sensor.TYPE_GYROSCOPE));
  }

  private SensorEventListener sensorEventListener;
  private final SensorManager sensorManager;
  private final List<Sensor> sensors;
  private final double[] values;

  private SensorsPlugin(Context context, int ... sensorTypes) {
    sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    sensors = new ArrayList<>(sensorTypes.length);
    for (int sensorType : sensorTypes) {
      sensors.add(sensorManager.getDefaultSensor(sensorType));
    }
    values = new double[sensorTypes.length * 3];
  }

  @Override
  public void onListen(Object arguments, EventChannel.EventSink events) {
    sensorEventListener = createSensorEventListener(events);
    for (Sensor sensor : sensors) {
      sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_GAME);
    }
  }

  @Override
  public void onCancel(Object arguments) {
    sensorManager.unregisterListener(sensorEventListener);
  }

  private SensorEventListener createSensorEventListener(final EventChannel.EventSink events) {
    return new SensorEventListener() {
      @Override
      public void onAccuracyChanged(Sensor sensor, int accuracy) {}

      @Override
      public void onSensorChanged(SensorEvent event) {
        if (event.values.length >=3) {
          int offset = sensors.indexOf(event.sensor) * 3;
          if (offset >= 0) {
            for (int i = 0; i < 3; i++) {
              values[offset + i] = event.values[i];
            }
            events.success(Arrays.copyOf(values, values.length));
          }
        }
      }
    };
  }
}
