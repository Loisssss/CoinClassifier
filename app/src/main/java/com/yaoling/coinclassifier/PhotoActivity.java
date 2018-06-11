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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

public class PhotoActivity extends AppCompatActivity {

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        try {
            classifier = new TensorFlowImageClassifier(this,
                    224, 224);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);
        button = findViewById(R.id.button);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_TAKE_PHOTO && resultCode == RESULT_OK) {
            Bitmap bitmap = setPhoto();
            bitmap = imagePreprocessor.preprocessBitmap(bitmap);
            final Collection<Recognition> results = classifier.doRecognize(bitmap);
            textView.setText(results.toString());
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
        imageView.setImageBitmap(bitmap);

        return bitmap;
    }
}

    

