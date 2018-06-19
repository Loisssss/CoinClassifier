package com.yaoling.coinclassifier;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.List;

/**
 * A classifier specialized to label images using TensorFlow.
 */
public class TensorFlowImageClassifier {

    private static final String TAG = "TFImageClassifier";

    private static final String LABELS_FILE = "labels.txt";
    private static final String MODEL_FILE = "model.tflite";

    /** Dimensions of inputs. */
    private static final int DIM_BATCH_SIZE = 1;
    private static final int DIM_PIXEL_SIZE = 3;

    /** Labels for categories that the TensorFlow model is trained for. */
    private List<String> labels;

    /** Cache to hold image data. */
    private ByteBuffer imgData = null;

    /** Inference results (Tensorflow Lite output). */
    private float[][] confidencePerLabel = null;

    /** Pre-allocated buffer for intermediate bitmap pixels */
    private int[] intValues;

    /** TensorFlow Lite engine */
    private Interpreter tfLite;

    /**
     * Initializes a TensorFlow Lite session for classifying images.
     */
    public TensorFlowImageClassifier(Context context, int inputImageWidth, int inputImageHeight)
            throws IOException {
        this.tfLite = new Interpreter(TensorFlowHelper.loadModelFile(context, MODEL_FILE));
        this.labels = TensorFlowHelper.readLabels(context, LABELS_FILE);

        // TODO: Use float per pixel representation now. Change model input in the future.
        int float32Size = 4;
        imgData =
                ByteBuffer.allocateDirect(
                        float32Size * DIM_BATCH_SIZE * inputImageWidth * inputImageHeight * DIM_PIXEL_SIZE);
        imgData.order(ByteOrder.nativeOrder());
        confidencePerLabel = new float[1][labels.size()];

        // Pre-allocate buffer for image pixels.
        intValues = new int[inputImageWidth * inputImageHeight];
    }

    /**
     * Clean up the resources used by the classifier.
     */
    public void destroyClassifier() {
        tfLite.close();
    }


    /**
     * @param image Bitmap containing the image to be classified. The image can be
     *              of any size, but preprocessing might occur to resize it to the
     *              format expected by the classification process, which can be time
     *              and power consuming.
     */
    public Collection<Recognition> doRecognize(Bitmap image) {
        TensorFlowHelper.convertBitmapToByteBuffer(image, intValues, imgData);

        long startTime = SystemClock.uptimeMillis();
        // Here's where the magic happens!!!
        tfLite.run(imgData,  confidencePerLabel);
        long endTime = SystemClock.uptimeMillis();
        Log.d(TAG, "Timecost to run model inference: " + Long.toString(endTime - startTime));

        // Get the results with the highest confidence and map them to their labels
        return TensorFlowHelper.getBestResults(confidencePerLabel, labels);
    }
}
