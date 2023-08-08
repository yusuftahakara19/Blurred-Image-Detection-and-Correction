package com.yusuf.detectingandcorrectingblurredimages;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.w3c.dom.Text;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    static {
        System.loadLibrary("opencv_java4");
    }

    ImageView imageView;
    Uri imageUri;
    Button btnGallery;
    Button btnTakePhoto;
    Button btnCorrect;
    Button btnSave;
    Button btnCalibration;
    TextView textView;
    private static final int REQUEST_CAMERA_PERMISSION = 3;
    private static final int REQUEST_STORAGE_PERMISSION = 4;

    // private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private String currentPhotoPath;

    private Uri photoUri;

    private CameraBridgeViewBase mOpenCvCameraView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions();
        imageView = (ImageView) findViewById(R.id.imageView2);
        btnGallery = (Button) findViewById(R.id.btnGallery);
        btnTakePhoto = (Button) findViewById(R.id.btnTakePhoto);
        btnCorrect = (Button) findViewById(R.id.btnCorrect);
        btnSave = (Button) findViewById(R.id.btnSave);
        btnCalibration = (Button) findViewById(R.id.btnCalibration);
        textView = (TextView) findViewById(R.id.textView);
        // Görüntüyü alma
        Bundle extras = getIntent().getExtras();
        if (extras != null) {

            if (getIntent().hasExtra("imageBitmap")) {
                byte[] byteArray = getIntent().getByteArrayExtra("imageBitmap");
                Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                // Bitmap'i kullanarak yapmak istediğiniz işlemleri gerçekleştirin
            }
        }
        btnCalibration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, CalibrationActivity.class);// MainActivity.this yerine getApplicationContext() şeklinde de yazabilirsin.
                finish();

                startActivity(intent);
            }
        });
        btnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, 1);
            }
        });

        btnTakePhoto.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                //    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                //    startActivityForResult(intent,101);
                String fileName = "photo";
                File storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                try {
                    File imageFile = File.createTempFile(fileName, "jpg", storageDirectory);
                    currentPhotoPath = imageFile.getAbsolutePath();
                    Uri imageUri = FileProvider.getUriForFile(MainActivity.this, "com.yusuf.detectingandcorrectingblurredimages.fileprovider", imageFile);
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    startActivityForResult(intent, 2);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        btnCorrect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bitmap originalBitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

                // Gürültü azaltma işlemi
                Mat mat = new Mat();
                Utils.bitmapToMat(originalBitmap, mat);
                Imgproc.GaussianBlur(mat, mat, new Size(3, 3), 0);

                // Keskinleştirme işlemi
                Mat kernel = new Mat(3, 3, CvType.CV_32F);
                kernel.put(0, 0, 0, -1, 0, -1, 5, -1, 0, -1, 0);
                Imgproc.filter2D(mat, mat, -1, kernel);

                // Düzeltme sonucunu ImageView'e yükle
                Bitmap resultBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(mat, resultBitmap);
                imageView.setImageBitmap(resultBitmap);

                checkImageBlur(imageView, textView, originalBitmap);
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Bitmap bitmap = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
                String fileName = "image_" + System.currentTimeMillis() + ".jpeg";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentResolver resolver = getContentResolver();
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                    contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MyImages/");
                    Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                    try (OutputStream fos = resolver.openOutputStream(imageUri)) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                        Toast.makeText(getApplicationContext(), "Image saved to gallery!", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Toast.makeText(getApplicationContext(), "Error saving image to gallery!", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                } else {
                    File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/MyImages/");
                    if (!directory.exists()) {
                        directory.mkdirs();
                    }
                    File file = new File(directory, fileName);
                    try (OutputStream fos = new FileOutputStream(file)) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                        MediaScannerConnection.scanFile(getApplicationContext(), new String[]{file.getAbsolutePath()}, new String[]{"image/jpeg"}, null);
                        Toast.makeText(getApplicationContext(), "Image saved to gallery!", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Toast.makeText(getApplicationContext(), "Error saving image to gallery!", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }

            }
        });
    }

    // Kamera izni kontrolü
    private boolean isCameraPermissionGranted() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    // Depolama izni kontrolü
    private boolean isStoragePermissionGranted() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    // İzin isteği
    private void requestPermissions() {
        if (!isCameraPermissionGranted()) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }

        if (!isStoragePermissionGranted()) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

        }
    }

    // İzin sonucu kontrolü
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Kamera izni verildi
                } else {
                    Toast.makeText(this, "Kamera izni reddedildi!", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_STORAGE_PERMISSION:
                if (grantResults.length > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    // Depolama izni verildi
                } else {
                    //  Toast.makeText(this, "Depolama izni reddedildi!", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {

            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));
            btnCorrect.performClick();
            btnCorrect.performClick();

            Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

            checkImageBlur(imageView, textView, bitmap);

            // Yeni kareler için bulanıklık kontrolü yapmak için buraya ek kod ekleyin
            Bitmap yeniKare = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
            checkImageBlur(imageView, textView, yeniKare);
        }


        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
            imageView.setImageBitmap(bitmap);

            checkImageBlur(imageView, textView, bitmap);

            Matrix matrix = new Matrix();
            matrix.postRotate(90); // 90 derece döndürme işlemi
            Bitmap bitmap2 = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            imageView.setImageBitmap(rotatedBitmap);
            btnCorrect.performClick();


        }
    }

    private void checkImageBlur(ImageView imageView, TextView textView, Bitmap bitmap) {
        String result="";
        // Calculate the Laplacian variance of the image
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);
        Mat laplacian = new Mat();
        Imgproc.Laplacian(mat, laplacian, CvType.CV_64F);
        MatOfDouble median = new MatOfDouble();
        MatOfDouble stdDeviation = new MatOfDouble();
        Core.meanStdDev(laplacian, median, stdDeviation);
        double variance = Math.pow(stdDeviation.get(0, 0)[0], 2);
        double threshold = calculateSharpnessThreshold(bitmap);

        if(variance<1.5*threshold){
            result ="Very blurry image";
            textView.setTextColor(Color.RED);
        }

        else if(variance<2*threshold && variance>1.5*threshold){
            result ="Blurry image";
            textView.setTextColor(Color.RED);

        }

        else if(variance>2*threshold && variance<4*threshold){
            textView.setTextColor(Color.rgb(255, 165, 0)); // RGB değerlerine göre turuncu
            result = "Semi-Clear Image";
        }

        else if(variance>4*threshold && variance<5*threshold){
            textView.setTextColor(Color.GREEN);
            result = "Clearly Photo";
        }

        else{
            textView.setTextColor(Color.GREEN);
            result = "Very Clear Photo";
        }

        textView.setText(result);
        // Set the image view background color based on the result


    }

        private Boolean checkImageBlurCamera(Bitmap bitmap) {
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
     int threshold =20;

        // Check if the image is blurry or not
        boolean isBlurry = variance < threshold;

        return isBlurry;
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


        double threshold = (meanValue + 2*stdDevValue)/4;

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

}

