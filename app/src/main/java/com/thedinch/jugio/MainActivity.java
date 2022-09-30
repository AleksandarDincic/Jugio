package com.thedinch.jugio;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.camera2.internal.annotation.CameraExecutor;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tflite.client.TfLiteInitializationOptions;
import com.google.android.gms.tflite.java.TfLite;
import com.google.common.util.concurrent.ListenableFuture;
import com.thedinch.jugio.databinding.ActivityMainBinding;

import org.tensorflow.lite.task.gms.vision.TfLiteVision;
import org.tensorflow.lite.task.gms.vision.detector.Detection;
import org.tensorflow.lite.task.gms.vision.detector.ObjectDetector;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    private NavController navController;
    private FragmentManager fragmentManager;

    private ResultViewModel resultViewModel;

    private Executor jsonExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        resultViewModel = new ViewModelProvider(this).get(ResultViewModel.class);

        resultViewModel.getResults().observe(this, l -> {});

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        jsonExecutor = Executors.newSingleThreadExecutor();

        jsonExecutor.execute(() -> CardIdentifier.initJSON(this));

        fragmentManager = getSupportFragmentManager();

        NavHostFragment navHost = (NavHostFragment) fragmentManager
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHost.getNavController();
    }
}