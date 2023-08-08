package com.yusuf.detectingandcorrectingblurredimages;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CalibrationActivity extends AppCompatActivity implements SurfaceHolder.Callback {
        private Camera camera;
        private SurfaceView surfaceView;
        private SurfaceHolder surfaceHolder;
        private Button captureButton;
        private Button retakeButton;
        private ImageView imageView;
        private boolean isBlurry;
        private Paint paint;
        private TextView textView;
        private Button okButton;
        private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_calibration);

                surfaceView = findViewById(R.id.surfaceView);
                captureButton = findViewById(R.id.captureButton);
                imageView = findViewById(R.id.imageView);
                retakeButton = findViewById(R.id.retakeButton);
                textView = findViewById(R.id.textViewCalibration);
                surfaceHolder = surfaceView.getHolder();
                surfaceHolder.addCallback(this);
                okButton = findViewById(R.id.okButton);

                okButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                                        Intent intent = new Intent(CalibrationActivity.this, MainActivity.class);
                                        finish();
                                        startActivity(intent);

                        }
                });



                retakeButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                                textView.setText("---");
                                textView.setTextColor(Color.BLACK);
                                captureButton.setVisibility(View.VISIBLE);
                                retakeButton.setVisibility(View.INVISIBLE);
                                imageView.setVisibility(View.INVISIBLE);
                        }
                });
                captureButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                                if (camera != null) {
                                        captureButton.setVisibility(View.INVISIBLE);
                                        retakeButton.setVisibility(View.VISIBLE);
                                        camera.takePicture(null, null, pictureCallback);


                                }
                        }
                });
        }

        @Override
        public void surfaceCreated(@NonNull SurfaceHolder holder) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        startCameraPreview();
                } else {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
                }
        }

        @Override
        public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                if (holder.getSurface() == null) {
                        return;
                }

                try {
                        camera.stopPreview();
                } catch (Exception e) {
                        Log.e("MainActivity", "Error stopping camera preview: " + e.getMessage());
                }

                try {
                        camera.setPreviewDisplay(holder);
                        camera.startPreview();
                } catch (IOException e) {
                        Log.e("MainActivity", "Error starting camera preview: " + e.getMessage());
                }
        }

        @Override
        public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                releaseCamera();
        }

        private void startCameraPreview() {
                try {
                        camera = Camera.open();
                        camera.setDisplayOrientation(90);
                        camera.setPreviewDisplay(surfaceHolder);
                        camera.startPreview();
                } catch (Exception e) {
                        Log.e("MainActivity", "Error starting camera preview: " + e.getMessage());
                }
        }

        private void releaseCamera() {
                if (camera != null) {
                        camera.stopPreview();
                        camera.release();
                        camera = null;
                }
        }

        private Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                        if (bitmap != null) {
                                isBlurry = checkImageBlur(bitmap);

                                Bitmap resultBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                                Canvas canvas = new Canvas(resultBitmap);
                                Paint paint = new Paint();
                                paint.setStyle(Paint.Style.STROKE);

                                if (isBlurry) {
                                        paint.setColor(Color.RED);
                                } else {
                                        paint.setColor(Color.GREEN);
                                }

                                int strokeWidth = 10;
                                paint.setStrokeWidth(strokeWidth);

                                int padding = strokeWidth / 2;
                                int width = imageView.getWidth();
                                int height = imageView.getHeight();
                                canvas.drawRect(padding, padding, width - padding, height - padding, paint);

                                imageView.setImageBitmap(resultBitmap);
                                Matrix matrix = new Matrix();
                                matrix.postRotate(90); // 90 derece döndürme işlemi
                                Bitmap bitmap2 = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
                                Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                                imageView.setImageBitmap(rotatedBitmap);
                                imageView.setVisibility(View.VISIBLE);
                        }

                        // Kamera önizlemesini yeniden başlat
                        camera.startPreview();
                }
        };

        private boolean checkImageBlur(Bitmap bitmap) {
                Mat mat2 = new Mat();
                Utils.bitmapToMat(bitmap, mat2);
                Mat laplacian = new Mat();
                Imgproc.Laplacian(mat2, laplacian, CvType.CV_64F);
                MatOfDouble median = new MatOfDouble();
                MatOfDouble stdDeviation = new MatOfDouble();
                Core.meanStdDev(laplacian, median, stdDeviation);
                double variance = Math.pow(stdDeviation.get(0, 0)[0], 2);

                double threshold = calculateSharpnessThreshold(bitmap);
                String result;


                if(variance<0.5*threshold){
                        result ="Calibration is very bad\nPlease do the calibration setting again";
                        textView.setTextColor(Color.RED);
                }

                else if(variance<0.6*threshold && variance>0.5*threshold){
                        result ="Calibration is bad\nPlease do the calibration setting again";
                        textView.setTextColor(Color.RED);

                }

                else if(variance>0.6*threshold && variance<0.7*threshold){
                        textView.setTextColor(Color.rgb(255, 165, 0)); // RGB değerlerine göre turuncu
                        result = "Semi-Clear Calibration\nPlease do the calibration setting again";
                }

                else if(variance>0.7*threshold && variance<0.8*threshold){
                        textView.setTextColor(Color.GREEN);
                        result = "Good Calibration\nCalibration Completed.";
                }

                else{
                        textView.setTextColor(Color.GREEN);
                        result = "High Definition Calibration\nCalibration Completed.";
                }

                textView.setText(result);

                // Set the image view background color based on the result
                int backgroundColor = isBlurry ? Color.RED : Color.GREEN;
                imageView.setBackgroundColor(backgroundColor);
                return variance < threshold;
        }
        private static double calculateSharpnessThreshold(Bitmap bitmap) {
                Mat mat = new Mat();
                Utils.bitmapToMat(bitmap, mat);
                Mat grayMat = new Mat();
                Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY);

                MatOfDouble mean = new MatOfDouble();
                MatOfDouble stdDeviation = new MatOfDouble();
                Core.meanStdDev(grayMat, mean, stdDeviation);

                //meanValue: Gri tonlamalı görüntünün piksel değerlerinin ortalaması.
                double meanValue = mean.get(0, 0)[0];
                //stdDevValue: Gri tonlamalı görüntünün piksel değerlerinin standart sapması.
                double stdDevValue = stdDeviation.get(0, 0)[0];


                double threshold = (meanValue + 2*stdDevValue)/8;

                return threshold;
        }

        private static double calculateImageVariance(Bitmap bitmap) {
                Mat mat = new Mat();
                Utils.bitmapToMat(bitmap, mat);
                Mat laplacian = new Mat();
                Imgproc.Laplacian(mat, laplacian, CvType.CV_64F);
                MatOfDouble median = new MatOfDouble();
                MatOfDouble stdDeviation = new MatOfDouble();
                Core.meanStdDev(laplacian, median, stdDeviation);
                double variance = Math.pow(stdDeviation.get(0, 0)[0], 2);
                return variance;
        }
/*
        private void checkImageBlur(Bitmap bitmap) {
                // Calculate the Laplacian variance of the image
                Mat mat = new Mat();
                Utils.bitmapToMat(bitmap, mat);
                Mat laplacian = new Mat();
                Imgproc.Laplacian(mat, laplacian, CvType.CV_64F);
                MatOfDouble median = new MatOfDouble();
                MatOfDouble stdDeviation = new MatOfDouble();
                Core.meanStdDev(laplacian, median, stdDeviation);
                double variance = Math.pow(stdDeviation.get(0, 0)[0], 2);

                // Set the blur threshold
                double threshold = 120.0;

                // Check if the image is blurry or not
                boolean isBlurry = variance < threshold;

                // Display the result in the TextView
                String result = isBlurry ? "CLARITY SCORE : " + (int) variance : " CLARITY SCORE : " + (int) variance;
                textView.setText(result);

                // Set the image view background color based on the result
                int backgroundColor = isBlurry ? Color.RED : Color.GREEN;
                imageView.setBackgroundColor(backgroundColor);
        }*/
}
