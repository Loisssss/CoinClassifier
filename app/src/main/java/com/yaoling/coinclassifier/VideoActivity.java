package com.yaoling.coinclassifier;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.IOException;
import java.util.Collection;

public class VideoActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

//    private EllipseDetector ellipseDetector;
    public static final String TAG = "VideoActivity";
    private JavaCameraView cameraView;
    private TextView textView;
    private TensorFlowImageClassifier classifier;
    Bitmap bitmap;
    private static final int PREVIEW_IMAGE_WIDTH = 640;
    private static final int PREVIEW_IMAGE_HEIGHT = 480;
    private static final int TF_INPUT_IMAGE_WIDTH = 224;
    private static final int TF_INPUT_IMAGE_HEIGHT = 224;
    ImagePreprocessor imagePreprocessor = new ImagePreprocessor(PREVIEW_IMAGE_WIDTH, PREVIEW_IMAGE_HEIGHT,
            TF_INPUT_IMAGE_WIDTH, TF_INPUT_IMAGE_HEIGHT);
    private static final String TAG2 = VideoActivity.class.getSimpleName();


    static {
        if (!OpenCVLoader.initDebug()) {
            Log.wtf(TAG, "static initializer: OpenCV failed to load!");
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        try {
            classifier = new TensorFlowImageClassifier(this,
                    224, 224);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        cameraView = findViewById(R.id.camera_view);
        cameraView.setCvCameraViewListener(this);
        textView = findViewById(R.id.textView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        BaseLoaderCallback callback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                    case SUCCESS:
                        Log.i(TAG, "onManagerConnected: OpenCV loaded successfully");
                        cameraView.enableView();
                        cameraView.enableFpsMeter();
                        break;
                    default:
                        super.onManagerConnected(status);
                        Log.e(TAG, "onManagerConnected: OpenCV load failed");
                }
            }
        };
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, callback);
    }
    @Override
    protected void onPause() {
        super.onPause();
        cameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

//        ellipseDetector = new EllipseDetector();
//        ellipseDetector.findEllipses(inputFrame.gray());
//        ellipseDetector.findCircles(inputFrame.gray());
        bitmap = imagePreprocessor.preprocessBitmap(bitmap);
        final Collection<Recognition> results = classifier.doRecognize(bitmap);
        textView.setText(results.toString());
        return inputFrame.rgba();
    }


}
