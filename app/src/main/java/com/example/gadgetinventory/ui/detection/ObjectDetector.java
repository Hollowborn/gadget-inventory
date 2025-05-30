package com.example.gadgetinventory.ui.detection;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ObjectDetector {
    private static final String MODEL_PATH = "best_float32.tflite";
    private static final String LABELS_PATH = "labels.txt";
    private static final float INPUT_MEAN = 0f;
    private static final float INPUT_STD = 255f;
    private static final float CONFIDENCE_THRESHOLD = 0.3f;
    private static final float IOU_THRESHOLD = 0.5f;

    private final Context context;
    private final DetectorListener listener;
    private Interpreter interpreter;
    private final List<String> labels = new ArrayList<>();
    private int tensorWidth, tensorHeight, numChannels, numElements;

    private final ImageProcessor imageProcessor = new ImageProcessor.Builder()
            .add(new NormalizeOp(INPUT_MEAN, INPUT_STD))
            .build();

    public interface DetectorListener {
        void onEmptyDetect();
        void onDetect(List<BoundingBox> boundingBoxes, long inferenceTime);
    }

    public ObjectDetector(Context context, DetectorListener listener) {
        this.context = context;
        this.listener = listener;
        
        try {
            // Print model info
            MappedByteBuffer model = FileUtil.loadMappedFile(context, MODEL_PATH);
            System.out.println("Model file size: " + model.capacity() + " bytes");
            
            // Print labels
            List<String> labels = FileUtil.loadLabels(context, LABELS_PATH);
            System.out.println("Loaded " + labels.size() + " labels: " + labels);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        initializeDetector(true);
    }

    private void initializeDetector(boolean useGpu) {
        try {
            Interpreter.Options options = new Interpreter.Options();
            if (useGpu) {
                CompatibilityList compatList = new CompatibilityList();
                if (compatList.isDelegateSupportedOnThisDevice()) {
                    options.addDelegate(new GpuDelegate(compatList.getBestOptionsForThisDevice()));
                }
            }

            MappedByteBuffer model = FileUtil.loadMappedFile(context, MODEL_PATH);
            interpreter = new Interpreter(model, options);

            // Get input and output shapes
            int[] inputShape = interpreter.getInputTensor(0).shape();
            int[] outputShape = interpreter.getOutputTensor(0).shape();

            tensorWidth = inputShape[1];
            tensorHeight = inputShape[2];
            numChannels = outputShape[1];
            numElements = outputShape[2];

            // Load labels
            labels.clear();
            labels.addAll(FileUtil.loadLabels(context, LABELS_PATH));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void detect(Bitmap bitmap) {
        if (tensorWidth == 0 || tensorHeight == 0) return;

        long startTime = SystemClock.uptimeMillis();

        try {
            // Calculate aspect ratio preserving resize
            float scale = Math.min(
                (float) tensorWidth / bitmap.getWidth(),
                (float) tensorHeight / bitmap.getHeight()
            );
            
            int scaledWidth = Math.round(bitmap.getWidth() * scale);
            int scaledHeight = Math.round(bitmap.getHeight() * scale);
            
            // Resize while maintaining aspect ratio
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(
                bitmap, scaledWidth, scaledHeight, true);
            
            // Create padding if needed
            Bitmap paddedBitmap = Bitmap.createBitmap(
                tensorWidth, tensorHeight, Bitmap.Config.ARGB_8888);
            
            // Calculate padding
            int dx = (tensorWidth - scaledWidth) / 2;
            int dy = (tensorHeight - scaledHeight) / 2;
            
            // Draw resized bitmap centered on padded bitmap
            android.graphics.Canvas canvas = new android.graphics.Canvas(paddedBitmap);
            canvas.drawBitmap(resizedBitmap, dx, dy, null);

            // Convert to TensorImage
            TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
            tensorImage.load(paddedBitmap);
            tensorImage = imageProcessor.process(tensorImage);

            // Prepare output buffer
            TensorBuffer outputBuffer = TensorBuffer.createFixedSize(
                    new int[]{1, numChannels, numElements}, DataType.FLOAT32);

            // Run inference
            interpreter.run(tensorImage.getBuffer(), outputBuffer.getBuffer());

            // Process results
            List<BoundingBox> detections = processDetections(outputBuffer.getFloatArray());

            long inferenceTime = SystemClock.uptimeMillis() - startTime;

            if (detections.isEmpty()) {
                listener.onEmptyDetect();
            } else {
                listener.onDetect(detections, inferenceTime);
            }

            // Clean up
            resizedBitmap.recycle();
            paddedBitmap.recycle();

        } catch (Exception e) {
            e.printStackTrace();
            listener.onEmptyDetect();
        }
    }

    private List<BoundingBox> processDetections(float[] outputs) {
        List<BoundingBox> boundingBoxes = new ArrayList<>();
        
        // Print output shape for debugging
        System.out.println("Output shape: numChannels=" + numChannels + ", numElements=" + numElements);
        System.out.println("First few outputs: " + outputs[0] + ", " + outputs[1] + ", " + outputs[2] + ", " + outputs[3]);

        // Process each detection
        for (int i = 0; i < numElements; i++) {
            float confidence = outputs[i + 4 * numElements];
            if (confidence > CONFIDENCE_THRESHOLD) {
                // Get normalized coordinates (0-1)
                float cx = outputs[i];
                float cy = outputs[i + numElements];
                float w = outputs[i + 2 * numElements];
                float h = outputs[i + 3 * numElements];

                // Keep coordinates normalized between 0 and 1
                float x1 = Math.max(0, Math.min(1, cx - w/2f));
                float y1 = Math.max(0, Math.min(1, cy - h/2f));
                float x2 = Math.max(0, Math.min(1, cx + w/2f));
                float y2 = Math.max(0, Math.min(1, cy + h/2f));

                // Get class with highest confidence
                int detectedClass = -1;
                float maxClass = 0;
                for (int j = 5; j < numChannels; j++) {
                    float score = outputs[i + j * numElements];
                    if (score > maxClass) {
                        maxClass = score;
                        detectedClass = j - 5;
                    }
                }

                if (detectedClass >= 0 && detectedClass < labels.size()) {
                    // Add detection only if box dimensions are reasonable
                    if (w > 0.01 && w < 0.99 && h > 0.01 && h < 0.99) {
                        boundingBoxes.add(new BoundingBox(
                                x1, y1, x2, y2, cx, cy, w, h,
                                confidence, detectedClass, labels.get(detectedClass)
                        ));
                        
                        // Debug output
                        System.out.println("Detection: " + labels.get(detectedClass) + 
                                         " conf=" + confidence + 
                                         " box=" + x1 + "," + y1 + "," + x2 + "," + y2);
                    }
                }
            }
        }

        // Apply Non-Maximum Suppression
        return applyNMS(boundingBoxes);
    }

    private List<BoundingBox> applyNMS(List<BoundingBox> boxes) {
        List<BoundingBox> selectedBoxes = new ArrayList<>();
        
        // Sort by confidence
        boxes.sort((b1, b2) -> Float.compare(b2.getCnf(), b1.getCnf()));
        
        boolean[] suppressed = new boolean[boxes.size()];
        
        for (int i = 0; i < boxes.size(); i++) {
            if (suppressed[i]) continue;
            
            selectedBoxes.add(boxes.get(i));
            
            for (int j = i + 1; j < boxes.size(); j++) {
                if (suppressed[j]) continue;
                
                float iou = calculateIoU(boxes.get(i), boxes.get(j));
                if (iou > IOU_THRESHOLD) {
                    suppressed[j] = true;
                }
            }
        }
        
        return selectedBoxes;
    }

    private float calculateIoU(BoundingBox box1, BoundingBox box2) {
        float intersectionX1 = Math.max(box1.getX1(), box2.getX1());
        float intersectionY1 = Math.max(box1.getY1(), box2.getY1());
        float intersectionX2 = Math.min(box1.getX2(), box2.getX2());
        float intersectionY2 = Math.min(box1.getY2(), box2.getY2());
        
        float intersectionArea = Math.max(0, intersectionX2 - intersectionX1) * 
                                Math.max(0, intersectionY2 - intersectionY1);
        
        float box1Area = (box1.getX2() - box1.getX1()) * (box1.getY2() - box1.getY1());
        float box2Area = (box2.getX2() - box2.getX1()) * (box2.getY2() - box2.getY1());
        
        return intersectionArea / (box1Area + box2Area - intersectionArea);
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
        }
    }
}