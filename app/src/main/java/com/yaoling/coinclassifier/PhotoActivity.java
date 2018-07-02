package com.yaoling.coinclassifier;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PhotoActivity extends AppCompatActivity {

    private CircleActivity circleActivity;
    private ImageView imageView;
    private TextView textView;
    private Button button;
    private String tempPhotoPath;
    TensorFlowImageClassifier classifier;
    private static final int PREVIEW_IMAGE_WIDTH = 640;
    private static final int PREVIEW_IMAGE_HEIGHT = 480;
    private static final int TF_INPUT_IMAGE_WIDTH = 224;
    private static final int TF_INPUT_IMAGE_HEIGHT = 224;
    ImagePreprocessor imagePreprocessor = new ImagePreprocessor(PREVIEW_IMAGE_WIDTH, PREVIEW_IMAGE_HEIGHT,
            TF_INPUT_IMAGE_WIDTH, TF_INPUT_IMAGE_HEIGHT);

    private static final int REQUEST_CODE_TAKE_PHOTO = 1;
    private static final String TAG = PhotoActivity.class.getSimpleName();

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.wtf(TAG, "static initializer: OpenCV failed to load!");
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        try {
            classifier = new TensorFlowImageClassifier(this,
                    TF_INPUT_IMAGE_WIDTH, TF_INPUT_IMAGE_HEIGHT);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);
        button = findViewById(R.id.button);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_TAKE_PHOTO && resultCode == RESULT_OK) {
            setPhoto();

            String resultText = "";
            String inPath = getInnerSDCardPath();
            File directory = new File(inPath + "/ROI");
            File[] files = directory.listFiles();
            for (int i = 0; i < files.length; i++) {
                Bitmap bitmap = BitmapFactory.decodeFile(files[i].getPath());
                bitmap = imagePreprocessor.preprocessBitmap(bitmap);
                List<Recognition> results = classifier.doRecognize(bitmap);
                Map<Object,Object> map = new HashMap<Object,Object>();

                resultText += (results.get(0).toString()+",");
            }
            String[] array = resultText.split(",");
            Map<Object,Object> map = new HashMap<Object, Object>();
            for (int i = 0; i < array.length; i++) {
                if(map.containsKey(array[i])) {
                    Integer count = (Integer) map.get(array[i]);
                    count++;
                    map.put(array[i], count);
            } else {
                    map.put(array[i],1);
                }
            }
            textView.setText(map.toString().substring(1, map.toString().length() - 1));
        }
    }

    public void takePhoto(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) == null) {
            Log.w(TAG, "Unable to resolve activity");
            return;
        }

        try {
            File photoFile = createImageFile();
            Uri photoURI = FileProvider.getUriForFile(this, "com.yaoling.coinclassifier.fileprovider", photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(intent, REQUEST_CODE_TAKE_PHOTO);
        } catch (IOException ex){
            Log.d(TAG, "TakePhoto: failed to create the file");
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(timeStamp, ".jpg", storageDir);
        tempPhotoPath = image.getAbsolutePath();
        return image;
    }

    private Bitmap setPhoto() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(tempPhotoPath, options);

        int photoW = options.outWidth;
        int photoH = options.outHeight;
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        options.inJustDecodeBounds = false;
        options.inSampleSize = scaleFactor;

        Bitmap bitmap = BitmapFactory.decodeFile(tempPhotoPath, options);

        // change Bitmap to mat
        Mat srcImage = new Mat(bitmap.getWidth(), bitmap.getHeight(), CvType.CV_8UC1);
        Utils.bitmapToMat(bitmap, srcImage);
        Imgproc.cvtColor(srcImage, srcImage, Imgproc.COLOR_BGR2GRAY,0);

        circleActivity = new CircleActivity();
        circleActivity.findEllipses(srcImage);
        // change Mat to Bitmap
        Bitmap processedImage = Bitmap.createBitmap(srcImage.cols(), srcImage.rows(), Bitmap.Config.ARGB_8888);
        //System.out.print(processedImage.getWidth());
        Utils.matToBitmap(srcImage, processedImage);

        imageView.setImageBitmap(processedImage);

        return processedImage;
    }

    public String getInnerSDCardPath() {
        return Environment.getExternalStorageDirectory().getPath();
    }
}

    

