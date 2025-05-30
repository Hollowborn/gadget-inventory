//package com.example.gadgetinventory.ui.detectionv2;
//
//// MainActivity.java
//
//
//import android.Manifest;
//import android.content.pm.PackageManager;
//import android.graphics.Bitmap;
//import android.graphics.Matrix;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.View;
//
//import androidx.activity.result.ActivityResultLauncher;
//import androidx.activity.result.contract.ActivityResultContracts;
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.camera.core.AspectRatio;
//import androidx.camera.core.Camera;
//import androidx.camera.core.CameraSelector;
//import androidx.camera.core.ImageAnalysis;
//import androidx.camera.core.Preview;
//import androidx.camera.lifecycle.ProcessCameraProvider;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//
//import com.example.myapplication.databinding.ActivityMainBinding;
//
//import java.util.List;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//public class MainActivity extends AppCompatActivity implements Detector.DetectorListener {
//
//    private ActivityMainBinding binding;
//    private final boolean isFrontCamera = false;
//
//    private Preview preview;
//    private ImageAnalysis imageAnalyzer;
//    private Camera camera;
//    private ProcessCameraProvider cameraProvider;
//    private Detector detector;
//
//    private ExecutorService cameraExecutor;
//
//    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
//            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
//                if (Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA))) {
//                    startCamera();
//                }
//            });
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        binding = ActivityMainBinding.inflate(getLayoutInflater());
//        setContentView(binding.getRoot());
//
//        cameraExecutor = Executors.newSingleThreadExecutor();
//
//        cameraExecutor.execute(() ->
//                detector = new Detector(getBaseContext(), Constants.MODEL_PATH, Constants.LABELS_PATH, this)
//        );
//
//        if (allPermissionsGranted()) {
//            startCamera();
//        } else {
//            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
//        }
//
//        bindListeners();
//    }
//
//    private void bindListeners() {
//        binding.isGpu.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            cameraExecutor.submit(() -> detector.restart(isChecked));
//
//            int color = ContextCompat.getColor(getBaseContext(), isChecked ? R.color.orange : R.color.gray);
//            buttonView.setBackgroundColor(color);
//        });
//    }
//
//    private void startCamera() {
//        ProcessCameraProvider.getInstance(this).addListener(() -> {
//            try {
//                cameraProvider = ProcessCameraProvider.getInstance(this).get();
//                bindCameraUseCases();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }, ContextCompat.getMainExecutor(this));
//    }
//
//    private void bindCameraUseCases() {
//        if (cameraProvider == null) throw new IllegalStateException("Camera initialization failed.");
//
//        int rotation = binding.viewFinder.getDisplay().getRotation();
//
//        CameraSelector cameraSelector = new CameraSelector.Builder()
//                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
//                .build();
//
//        preview = new Preview.Builder()
//                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
//                .setTargetRotation(rotation)
//                .build();
//
//        imageAnalyzer = new ImageAnalysis.Builder()
//                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
//                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                .setTargetRotation(rotation)
//                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
//                .build();
//
//        imageAnalyzer.setAnalyzer(cameraExecutor, imageProxy -> {
//            Bitmap bitmapBuffer = Bitmap.createBitmap(imageProxy.getWidth(), imageProxy.getHeight(), Bitmap.Config.ARGB_8888);
//            bitmapBuffer.copyPixelsFromBuffer(imageProxy.getPlanes()[0].getBuffer());
//            imageProxy.close();
//
//
//            Matrix matrix = new Matrix();
//            matrix.postRotate(imageProxy.getImageInfo().getRotationDegrees());
//
//            if (isFrontCamera) {
//                matrix.postScale(-1f, 1f, imageProxy.getWidth(), imageProxy.getHeight());
//            }
//
//            Bitmap rotatedBitmap = Bitmap.createBitmap(
//                    bitmapBuffer, 0, 0, bitmapBuffer.getWidth(), bitmapBuffer.getHeight(),
//                    matrix, true
//            );
//
//            detector.detect(rotatedBitmap);
//        });
//
//        cameraProvider.unbindAll();
//
//        try {
//            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer);
//            preview.setSurfaceProvider(binding.viewFinder.getSurfaceProvider());
//        } catch (Exception e) {
//            Log.e(TAG, "Use case binding failed", e);
//        }
//    }
//
//    private boolean allPermissionsGranted() {
//        for (String permission : REQUIRED_PERMISSIONS) {
//            if (ContextCompat.checkSelfPermission(getBaseContext(), permission) != PackageManager.PERMISSION_GRANTED) {
//                return false;
//            }
//        }
//        return true;
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        if (detector != null) detector.close();
//        cameraExecutor.shutdown();
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        if (allPermissionsGranted()) {
//            startCamera();
//        } else {
//            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS);
//        }
//    }
//
//    @Override
//    public void onEmptyDetect() {
//        runOnUiThread(() -> binding.overlay.clear());
//    }
//
//    @Override
//    public void onDetect(List<BoundingBox> boundingBoxes, long inferenceTime) {
//        runOnUiThread(() -> {
//            binding.inferenceTime.setText(inferenceTime + "ms");
//            binding.overlay.setResults(boundingBoxes);
//            binding.overlay.invalidate();
//        });
//    }
//
//    private static final String TAG = "Camera";
//    private static final int REQUEST_CODE_PERMISSIONS = 10;
//    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};
//}
