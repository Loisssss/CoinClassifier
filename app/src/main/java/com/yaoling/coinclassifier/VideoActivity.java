package com.yaoling.coinclassifier;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

public class VideoActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

//    private EllipseDetector ellipseDetector;
    public static final String TAG = "VideoActivity";
    private JavaCameraView cameraView;

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.wtf(TAG, "static initializer: OpenCV failed to load!");
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        cameraView = findViewById(R.id.camera_view);
        cameraView.setCvCameraViewListener(this);
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
        return inputFrame.rgba();
    }


}
