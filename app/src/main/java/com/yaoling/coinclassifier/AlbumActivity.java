package com.yaoling.coinclassifier;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class AlbumActivity extends AppCompatActivity {

    private ImageView imageView;
    private TextView textView;
    private Button button;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);

        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);
        button = findViewById(R.id.button);
    }
}
