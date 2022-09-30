package com.thedinch.jugio;

import org.tensorflow.lite.task.gms.vision.detector.Detection;

import java.util.List;

public interface LocatorCallback {
    public void callback(
            List<Detection> detected,
            int imgWidth,
            int imgHeight
    );
}
