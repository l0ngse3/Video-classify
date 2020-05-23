package com.example.doanvideoclassification;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import com.example.doanvideoclassification.tflite.Classifier;

import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VideoClassifyActivity extends AppCompatActivity {

    VideoView videoView;
    TextView txtResult;
    String videoPath;
    MediaController mediaController;

    ArrayList<Bitmap> listBitmap = new ArrayList<>();

    StringBuffer result;

    Classifier classifier;
    private Classifier.Model model = Classifier.Model.FLOAT;
    private Classifier.Device device = Classifier.Device.CPU;
    private int numThreads = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_classify);

        videoView = findViewById(R.id.videoView);
        txtResult = findViewById(R.id.txtResult);

        mediaController= new
                MediaController(this);
        mediaController.setAnchorView(videoView);

        loadVideo();
//        classifyVideo();
    }

    private void classifyVideo() {
        FrameGrabber frameGrabber = new FFmpegFrameGrabber(videoPath);
        Frame vFrame = null;
        try {
            frameGrabber.setFormat("mp4");
            frameGrabber.start();

            do{
                vFrame = frameGrabber.grabFrame();
                if(vFrame != null){
                    AndroidFrameConverter frameConverter = new AndroidFrameConverter();
                    Bitmap bm = frameConverter.convert(vFrame);
                    if(bm != null)
                        classfyImage(bm);
                }
            }
            while (vFrame != null);
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
    }

        

    private void loadVideo() {
        videoPath = getIntent().getStringExtra("data");
        videoView.setVideoPath(videoPath);
        videoView.setMediaController(mediaController);
        videoView.start();
    }

    private void classfyImage(final Bitmap rgbBitmap) {
        if(classifier == null){
            recreateClassifier(device, numThreads);
        }

        new Runnable() {
            @Override
            public void run() {
                if (classifier != null) {
                    final List<Classifier.Recognition> results =
                            classifier.recognizeImage(rgbBitmap, 0);
                    Log.d("Classifier", "results: "+results);
                    runOnUiThread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    Classifier.Recognition recognition = results.get(0);
                                    //Toast.makeText(ClassifyActivity.this, results.get(0).getTitle()+" "+results.get(0).getConfidence(), Toast.LENGTH_SHORT).show();

                                    Float value = new Float(0);
                                    if (recognition != null) {
                                        if (recognition.getConfidence() != null)
                                            value = recognition.getConfidence();

                                        if (value > 0.80) {
//                                            listResult.add(value);
                                            if (recognition.getTitle() != null)
                                                result.append(recognition.getTitle());
                                            txtResult.setText(result.toString());
                                        }
                                    }
                                }
                            });
                }
            }
        }.run();
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
