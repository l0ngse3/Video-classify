package com.example.doanvideoclassification;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
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

import org.tensorflow.lite.support.common.FileUtil;

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

        getImagePath();
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

                                        if (value > 0.80) {
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
                onBackPressed();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(ClassifyActivity.this, MainActivity.class);
        startActivity(intent);
        ClassifyActivity.this.finish();
    }

    public void getImagePath(){
        String imageLocation = getIntent().getStringExtra("data");
        Log.d("image ", "getLastestImage: "+ imageLocation);
        File imageFile = new File(imageLocation);
        int rotation = Integer.parseInt(getIntent().getStringExtra("rotation"));
        if (imageFile.exists()) {
            Bitmap bm = BitmapFactory.decodeFile(imageLocation);
            rgbBitmap = bm;
            ImageUtils.loadRectangleImageInto(this, bm, imgView, rotation);
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
