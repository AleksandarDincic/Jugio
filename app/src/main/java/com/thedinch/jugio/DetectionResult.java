package com.thedinch.jugio;

import android.graphics.Bitmap;

public class DetectionResult {

    Bitmap bitmap;

    String cardName = null;

    public DetectionResult(Bitmap bitmap) {
        this.bitmap = bitmap;
    }
}
