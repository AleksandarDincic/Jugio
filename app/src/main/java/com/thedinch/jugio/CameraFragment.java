package com.thedinch.jugio;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.tflite.client.TfLiteInitializationOptions;
import com.google.common.util.concurrent.ListenableFuture;
import com.thedinch.jugio.databinding.ActivityMainBinding;
import com.thedinch.jugio.databinding.FragmentCameraBinding;

import org.tensorflow.lite.task.gms.vision.TfLiteVision;
import org.tensorflow.lite.task.gms.vision.detector.Detection;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CameraFragment extends Fragment {

    public static final int CAMERA_PERMISSION_CODE = 100;

    MainActivity mainActivity;
    NavController navController;

    FragmentCameraBinding binding;
    ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    Camera camera;
    CardLocator locator;
    public Bitmap bitmapBuffer;

    ResultViewModel resultViewModel;

    Executor cameraExecutor;

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(binding.cameraView.getDisplay().getRotation())
                .build();

        ImageAnalysis imgAnalysis = new ImageAnalysis.Builder()
                .setTargetRotation(binding.cameraView.getDisplay().getRotation())
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build();

        imgAnalysis.setAnalyzer(cameraExecutor, image -> {
            if (bitmapBuffer == null) {
                bitmapBuffer = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
            }

            locateCards(image);
        });

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        cameraProvider.unbindAll();

        camera = cameraProvider.bindToLifecycle( this, cameraSelector, preview, imgAnalysis);

        preview.setSurfaceProvider(binding.cameraView.getSurfaceProvider());
    }

    void checkCameraPermission() {
        //if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        //}
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mainActivity = (MainActivity) requireActivity();

        checkCameraPermission();
        resultViewModel = new ViewModelProvider(mainActivity).get(ResultViewModel.class);

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCameraBinding.inflate(inflater, container, false);

        TfLiteInitializationOptions tfLiteOptions = TfLiteInitializationOptions.builder().setEnableGpuDelegateSupport(true).build();
        TfLiteVision.initialize(mainActivity, tfLiteOptions).addOnSuccessListener((v) -> {
            System.out.println("Succ");
            locator = new CardLocator(mainActivity, (detected, imgWidth, imgHeight) -> {
                binding.overlayView.setDetections(detected, imgWidth, imgHeight);
                binding.overlayView.invalidate();
            });
        }).addOnFailureListener((v) -> {
            v.printStackTrace();
            System.out.println("Fail");
        });

        cameraExecutor = Executors.newSingleThreadExecutor();

        cameraProviderFuture = ProcessCameraProvider.getInstance(mainActivity);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(mainActivity));


        binding.captureButton.setOnClickListener(v -> {
            if (locator != null) {
                captured = true;
            }
        });

        return binding.getRoot();
    }

    boolean captured = false;

    void locateCards(ImageProxy image) {
        bitmapBuffer.copyPixelsFromBuffer(image.getPlanes()[0].getBuffer());
        int rotationDegrees = image.getImageInfo().getRotationDegrees();
        image.close();


        if (locator != null) {
            List<Detection> detectionList = locator.locateCards(bitmapBuffer, rotationDegrees);
            if (captured) {
                if (!detectionList.isEmpty()) {
                    List<DetectionResult> newResults = new ArrayList<>();

                    Matrix matrix = new Matrix();

                    matrix.postRotate(rotationDegrees);

                    Bitmap rotatedImage = Bitmap.createBitmap(bitmapBuffer, 0, 0, bitmapBuffer.getWidth(), bitmapBuffer.getHeight(), matrix, true);


                    for (Detection d : detectionList) {
                        RectF bBox = d.getBoundingBox();

                        try {
                            Bitmap newResult = Bitmap.createBitmap(rotatedImage, (int) bBox.left, (int) bBox.top, (int) (bBox.right - bBox.left), (int) (bBox.bottom - bBox.top));

                            newResults.add(new DetectionResult(newResult));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    if (!newResults.isEmpty()) {
                        mainActivity.runOnUiThread(() -> {
                            resultViewModel.setResults(newResults);
                            NavDirections action = CameraFragmentDirections.actionCameraFragmentToResultsFragment();

                            navController.navigate(action);
                        });
                    }


                }
                captured = false;
            }
        }
    }

    public CameraFragment() {

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
    }

}