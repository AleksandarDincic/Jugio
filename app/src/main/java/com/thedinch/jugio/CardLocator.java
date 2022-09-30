package com.thedinch.jugio;

import android.content.Context;
import android.graphics.Bitmap;

import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.Rot90Op;
import org.tensorflow.lite.task.core.BaseOptions;
import org.tensorflow.lite.task.gms.vision.detector.Detection;
import org.tensorflow.lite.task.gms.vision.detector.ObjectDetector;

import java.util.List;

public class CardLocator {

    public static final float DETECTION_THRESHOLD = 0.5f;
    public static final int DETECTION_THREADS = 2;
    public static final int DETECTION_MAX_RESULTS = 10;

    public static final String DETECTION_MODEL_NAME = "localization.tflite";

    Context context;
    ObjectDetector objDetector = null;
    LocatorCallback callback = null;

    public CardLocator(Context context, LocatorCallback callback){
        this.context = context;
        this.callback = callback;
        initObjDetector();
    }

    public void initObjDetector(){
        ObjectDetector.ObjectDetectorOptions.Builder objOptionsBuilder = ObjectDetector.ObjectDetectorOptions.builder()
                .setScoreThreshold(DETECTION_THRESHOLD).setMaxResults(DETECTION_MAX_RESULTS);

        BaseOptions.Builder baseOptionsBuilder = BaseOptions.builder().setNumThreads(DETECTION_THREADS);

        try{
            baseOptionsBuilder.useGpu();
        }catch (Exception e){

        }

        objOptionsBuilder.setBaseOptions(baseOptionsBuilder.build());

        try{
            objDetector = ObjectDetector.createFromFileAndOptions(context, DETECTION_MODEL_NAME, objOptionsBuilder.build());
        } catch (Exception e){

        }
    }

    public List<Detection> locateCards(Bitmap img, int rotation){
        if(objDetector == null){
            initObjDetector();
        }

        //double time = System.nanoTime();
        ImageProcessor imgProcessor = new ImageProcessor.Builder().add(new Rot90Op(-rotation / 90)).build();

        TensorImage tensorImg = imgProcessor.process(TensorImage.fromBitmap(img));

        List<Detection> detections = objDetector.detect(tensorImg);
        //time = (System.nanoTime() - time) / 1_000_000_000;

        //System.out.println("Time for location: " + time + "s");

        if(callback != null){
            callback.callback(detections, tensorImg.getWidth(), tensorImg.getHeight());
        }

        return detections;
    }
}
