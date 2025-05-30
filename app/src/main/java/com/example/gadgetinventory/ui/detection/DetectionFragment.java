package com.example.gadgetinventory.ui.detection;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.gadgetinventory.R;
import com.example.gadgetinventory.ui.detectionv2.Detector;
import com.example.gadgetinventory.ui.detectionv2.BoundingBox;
import com.example.gadgetinventory.ui.detectionv2.Constants;
import com.example.gadgetinventory.ui.detectionv2.OverlayView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import androidx.navigation.Navigation;
import androidx.core.content.FileProvider;
import com.google.android.material.button.MaterialButton;

public class DetectionFragment extends Fragment implements Detector.DetectorListener {
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    private PreviewView viewFinder;
    private OverlayView overlayView;
    private MaterialButton selectGadgetButton;
    private ExecutorService cameraExecutor;
    private Detector detector;
    private final boolean isFrontCamera = false;
    private Bitmap currentBitmap;
    private List<BoundingBox> currentDetections;
    private ProcessCameraProvider cameraProvider;
    private boolean isDetecting = true;
    private ImageAnalysis imageAnalysis;
    private volatile boolean isProcessingFrame = false;
    private final Object lock = new Object();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewFinder = view.findViewById(R.id.viewFinder);
        overlayView = view.findViewById(R.id.overlay);
        selectGadgetButton = view.findViewById(R.id.selectGadgetButton);
        
        // Set up select button
        selectGadgetButton.setOnClickListener(v -> selectBestDetection());
        
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Initialize detector on background thread
        cameraExecutor.execute(() -> {
            detector = new Detector(
                requireContext(),
                Constants.MODEL_PATH,
                Constants.LABELS_PATH,
                this
            );
        });

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(requireActivity(), 
                    REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void startCamera() {
        ProcessCameraProvider.getInstance(requireContext()).addListener(() -> {
            try {
                cameraProvider = ProcessCameraProvider.getInstance(requireContext()).get();

                Preview preview = new Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .build();

                imageAnalysis = new ImageAnalysis.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    if (isProcessingFrame) {
                        imageProxy.close();
                        return;
                    }
                    isProcessingFrame = true;

                    try {
                        synchronized (lock) {
                            if (currentBitmap != null) {
                                currentBitmap.recycle();
                            }

                            Bitmap bitmapBuffer = Bitmap.createBitmap(
                                imageProxy.getWidth(),
                                imageProxy.getHeight(),
                                Bitmap.Config.ARGB_8888
                            );
                            bitmapBuffer.copyPixelsFromBuffer(imageProxy.getPlanes()[0].getBuffer());

                            Matrix matrix = new Matrix();
                            matrix.postRotate(imageProxy.getImageInfo().getRotationDegrees());

                            currentBitmap = Bitmap.createBitmap(
                                bitmapBuffer,
                                0,
                                0,
                                bitmapBuffer.getWidth(),
                                bitmapBuffer.getHeight(),
                                matrix,
                                true
                            );

                            bitmapBuffer.recycle();
                            detector.detect(currentBitmap);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        imageProxy.close();
                        isProcessingFrame = false;
                    }
                });

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
                );

                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    @Override
    public void onEmptyDetect() {
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> overlayView.clear());
        }
    }

    @Override
    public void onDetect(List<BoundingBox> boundingBoxes, long inferenceTime) {
        if (isAdded()) {
            currentDetections = boundingBoxes;
            requireActivity().runOnUiThread(() -> {
                overlayView.setResults(boundingBoxes);
                overlayView.invalidate();
                selectGadgetButton.setEnabled(!boundingBoxes.isEmpty());
            });
        }
    }

    @Override
    public void onDestroyView() {
        synchronized (lock) {
            if (cameraProvider != null) {
                cameraProvider.unbindAll();
            }
            if (currentBitmap != null) {
                currentBitmap.recycle();
                currentBitmap = null;
            }
            if (detector != null) {
                detector.close();
                detector = null;
            }
        }
        super.onDestroyView();
        cameraExecutor.shutdown();
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                // Handle permission denied
            }
        }
    }

    private void selectBestDetection() {
        if (currentDetections == null || currentDetections.isEmpty() || currentBitmap == null) {
            return;
        }
        BoundingBox selectedDetection = null;
        try {
            synchronized (lock) {
                // Stop camera and image analysis first
                if (cameraProvider != null) {
                    cameraProvider.unbindAll();
                }
                imageAnalysis = null;
                
                // Find detection with highest confidence
                BoundingBox bestDetection = currentDetections.get(0);
                for (BoundingBox box : currentDetections) {
                    if (box.getCnf() > bestDetection.getCnf()) {
                        bestDetection = box;
                    }
                }
                selectedDetection = bestDetection;
                final BoundingBox detection = selectedDetection;
                // Create a copy of the bitmap
                Bitmap bitmapCopy = Bitmap.createBitmap(currentBitmap);

                // Process the image and navigate in a background thread
                cameraExecutor.execute(() -> {
                    try {
                        // Create a cropped bitmap of the detection
                        int startX = (int) (detection.getX1() * bitmapCopy.getWidth());
                        int startY = (int) (detection.getY1() * bitmapCopy.getHeight());
                        int width = (int) ((detection.getX2() - detection.getX1()) * bitmapCopy.getWidth());
                        int height = (int) ((detection.getY2() - detection.getY1()) * bitmapCopy.getHeight());

                        // Ensure coordinates are valid
                        startX = Math.max(0, Math.min(startX, bitmapCopy.getWidth() - 1));
                        startY = Math.max(0, Math.min(startY, bitmapCopy.getHeight() - 1));
                        width = Math.min(width, bitmapCopy.getWidth() - startX);
                        height = Math.min(height, bitmapCopy.getHeight() - startY);

                        final Bitmap croppedBitmap = Bitmap.createBitmap(
                            bitmapCopy,
                            startX,
                            startY,
                            width,
                            height
                        );

                        // Save to file
                        File outputDir = requireContext().getCacheDir();
                        File outputFile = File.createTempFile("detected_gadget", ".jpg", outputDir);
                        FileOutputStream fos = new FileOutputStream(outputFile);
                        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                        fos.close();

                        // Clean up bitmaps
                        croppedBitmap.recycle();
                        bitmapCopy.recycle();

                        Uri imageUri = FileProvider.getUriForFile(
                            requireContext(),
                            requireContext().getPackageName() + ".fileprovider",
                            outputFile
                        );

                        // Navigate on main thread
                        requireActivity().runOnUiThread(() -> {
                            Bundle args = new Bundle();
                            args.putString("detected_gadget_image", imageUri.toString());
                            args.putString("detected_gadget_model", detection.getClsName());
                            Navigation.findNavController(requireView())
                                .navigate(R.id.navigation_add, args);
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}