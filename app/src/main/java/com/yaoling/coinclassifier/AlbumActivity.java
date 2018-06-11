package com.yaoling.coinclassifier;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.Collection;

public class AlbumActivity extends AppCompatActivity {

    private ImageView imageView;
    private TextView textView;
    private Button button;
    private static final int IMAGE = 1;
    TensorFlowImageClassifier classifier;
    private static final int PREVIEW_IMAGE_WIDTH = 640;
    private static final int PREVIEW_IMAGE_HEIGHT = 480;
    private static final int TF_INPUT_IMAGE_WIDTH = 224;
    private static final int TF_INPUT_IMAGE_HEIGHT = 224;
    ImagePreprocessor imagePreprocessor = new ImagePreprocessor(PREVIEW_IMAGE_WIDTH, PREVIEW_IMAGE_HEIGHT,
                                               TF_INPUT_IMAGE_WIDTH, TF_INPUT_IMAGE_HEIGHT);

    private static final String TAG = AlbumActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);

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
        if (requestCode == IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            String[] projection = { MediaStore.Images.Media.DATA };
            Cursor cursor = getContentResolver().query(imageUri, projection, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(projection[0]);
            String imagePath = cursor.getString(columnIndex);
            cursor.close();

            Bitmap bitmap = showImage(imagePath);
            bitmap = imagePreprocessor.preprocessBitmap(bitmap);
            final Collection<Recognition> results = classifier.doRecognize(bitmap);
            textView.setText(results.toString());
        }
    }


    public void PickImage(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, IMAGE);
    }

    private Bitmap showImage(String imagePath){
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        imageView.setImageBitmap(bitmap);
        return bitmap;
    }
}
