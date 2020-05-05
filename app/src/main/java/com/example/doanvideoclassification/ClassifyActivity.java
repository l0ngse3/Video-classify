package com.example.doanvideoclassification;

import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.doanvideoclassification.tflite.Classifier.Device;
import com.example.doanvideoclassification.tflite.Classifier.Model;
import com.example.doanvideoclassification.tflite.Classifier.Recognition;

import com.example.doanvideoclassification.tflite.Classifier;
import com.example.doanvideoclassification.utils.ImageUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ClassifyActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private ImageView imgView;
    private TextView txtTitle, txtResult;

    Classifier classifier;
    private Model model = Model.FLOAT;
    private Device device = Device.CPU;
    private int numThreads = 3;

    Bitmap rgbBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classify);

        imgView = findViewById(R.id.imgView);
        txtTitle = findViewById(R.id.txtTitle);
        txtResult = findViewById(R.id.txtResult);
        btnBack = findViewById(R.id.btnBack);

        getLastestImage();
        classfyImage();

        eventProcessing();
    }

    private void classfyImage() {
        if(classifier == null){
            recreateClassifier(device, numThreads);
        }

        new Runnable() {
            @Override
            public void run() {
                if (classifier != null) {
                    final List<Recognition> results =
                            classifier.recognizeImage(rgbBitmap, 0);
                    Log.d("Classifier", "results: "+results);
                    runOnUiThread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    Recognition recognition = results.get(0);
                                    //Toast.makeText(ClassifyActivity.this, results.get(0).getTitle()+" "+results.get(0).getConfidence(), Toast.LENGTH_SHORT).show();
                                    String result = "";
                                    Float value = new Float(0);
                                    if (recognition != null) {
                                        if (recognition.getTitle() != null)
                                            result = recognition.getTitle();
                                        if (recognition.getConfidence() != null)
                                            value = recognition.getConfidence();

                                        if (value > 0.75) {
                                            txtResult.setText(result);
                                        } else {
                                            txtResult.setText("Can not classify");
                                        }
                                    } else {
                                        txtResult.setText("Can not classify");
                                    }
                                }
                            });
                }
            }
        }.run();
    }

    private void eventProcessing() {
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClassifyActivity.this.finish();
            }
        });


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
                rgbBitmap = bm;
                ImageUtils.loadRectangleImageInto(this, bm, imgView);
            }
        }
    }

    private void recreateClassifier(Classifier.Device device, int numThreads) {
        if (classifier != null) {
            classifier.close();
            classifier = null;
        }
        try {
            classifier = Classifier.create(this, model,device, numThreads);
        } catch (IOException e) {
            Log.e("RecreateClassifier Fail", "Failed to create classifier: " + e);
        }
    }

}
