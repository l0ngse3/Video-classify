package com.example.doanvideoclassification;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.TextureView;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.doanvideoclassification.utils.CameraUtils;
import com.example.doanvideoclassification.utils.ImageUtils;
import com.example.doanvideoclassification.customview.AutoFitTextureView;

import java.io.File;

public class MainActivity extends AppCompatActivity  {

    private static final int REQUEST_PERMISSION_RESULT = 0;


    ImageView imgCapture, imgPreview, imgRealtime;
    AutoFitTextureView autoFitTextureView;
    CameraUtils camera;
    Chronometer mChronometer;
    TextureView.SurfaceTextureListener textureListener ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        autoFitTextureView = findViewById(R.id.autoFitTextureView);
        imgCapture = findViewById(R.id.imgCapture);
        imgPreview = findViewById(R.id.imgPreview);
        imgRealtime = findViewById(R.id.imgRealtime);
        mChronometer = findViewById(R.id.chronometer);


        grantPermission();
        camera = new CameraUtils(this, autoFitTextureView);
        camera.createVideoFolder();
        camera.createImageFolder();

        textureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
                camera.setupCamera(i, i1);
                camera.connectCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

            }
        };
        eventProcessing();
    }

    private void eventProcessing() {
        if(cameraGranted() && externalStorageGranted()){
            getLastestImage();
        }
        else{
            grantPermission();
        }

        ////
        imgPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "Preview", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, ClassifyActivity.class);
                startActivity(intent);
            }
        });
        
        imgCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "Capture", Toast.LENGTH_SHORT).show();
                if(!(camera.isIsTimelapse() || camera.isIsRecording())) {
                    camera.checkWriteStoragePermission();
                }
                camera.lockFocus();
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        getLastestImage();
                        Intent intent = new Intent(MainActivity.this, ClassifyActivity.class);
                        startActivity(intent);
                    }
                };
                Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(runnable, 1000);
            }
        });

        imgRealtime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ClassifyActivityRT.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        View decorView = getWindow().getDecorView();
        if (hasFocus) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_PERMISSION_RESULT && grantResults.length == 3){
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "The application will not run without camera services!", Toast.LENGTH_SHORT).show();
            }
            if(grantResults[1] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "The application will not run without read storage services!", Toast.LENGTH_SHORT).show();
            }
            if(grantResults[2] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "The application will not run without write storage services!", Toast.LENGTH_SHORT).show();
            }
            if(grantResults[0] == grantResults[1] && grantResults[1]==grantResults[2] && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                getLastestImage();
            }
        }
    }

    @Override
    protected void onPause() {
        camera.closeCamera();
        camera.stopBackgroundThread();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getLastestImage();
        camera.startBackgroundThread();
        if(autoFitTextureView.isAvailable()){
            camera.setupCamera(autoFitTextureView.getWidth(), autoFitTextureView.getHeight());
            camera.connectCamera();
        }
        else {
            autoFitTextureView.setSurfaceTextureListener(textureListener);
        }
    }

    public void getLastestImage(){
        String[] projection = new String[]{
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.ImageColumns.DATA,
                MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME, //the album it in
                MediaStore.Images.ImageColumns.DATE_TAKEN,
                MediaStore.Images.ImageColumns.MIME_TYPE
        };
        final Cursor cursor = this.getContentResolver()
                .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
                        null, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");

// Put it in the image view
        if (cursor.moveToFirst()) {
            String imageLocation = cursor.getString(1);
            File imageFile = new File(imageLocation);
            if (imageFile.exists()) {   // TODO: is there a better way to do this?
                Bitmap bm = BitmapFactory.decodeFile(imageLocation);
                ImageUtils.loadCircleImageInto(this, bm, imgPreview);
            }
        }
    }

    private boolean cameraGranted(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private boolean externalStorageGranted(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void grantPermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!cameraGranted() || !externalStorageGranted()) {
                requestPermissions(new String[]{Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                }, REQUEST_PERMISSION_RESULT);
            }
        }
    }

    public Chronometer getChronometer() {
        return mChronometer;
    }

}
