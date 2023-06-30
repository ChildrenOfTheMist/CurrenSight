package com.example.cameratest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GestureDetectorCompat;
import androidx.lifecycle.LifecycleOwner;


import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.util.Size;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.cameratest.ml.UnquantModelEfficientnetv2B021kFt1k;
import com.google.common.util.concurrent.ListenableFuture;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity{
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    MediaPlayer bg, pm, p20, p50, p100, p200, p500, p1000, lang, dir;
    Camera cam;
    TextToSpeech textToSpeech;
    PreviewView previewView;
    Bitmap bitmap;
    RelativeLayout main_layout;
    int imageSize=224;
    private ImageCapture imageCapture;

    private final int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRE_PERMISSIONS = new String[]{"android.permission.CAMERA",  "android.permission.FLASHLIGHT"};

    private GestureDetectorCompat mGestureDetector;


    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String LANG = "lang";

    public String language = "en";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();
        mGestureDetector = new GestureDetectorCompat(this, new GestureListener());

        if(allPermissionGranted()){
            startInitializations();

        }
        else{
            ActivityCompat.requestPermissions(this, REQUIRE_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
            startInitializations();
        }


    }

    private Executor getExecutor(){
        return ContextCompat.getMainExecutor(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_PERMISSIONS:
                if (!(grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    onDenyPermission();
                }
                return;
        }
    }

    private void startInitializations() {
        loadLang();
        pm = MediaPlayer.create(this, R.raw.playmoney);
        p20 = MediaPlayer.create(this, R.raw.php20);
        p50 = MediaPlayer.create(this, R.raw.php50);
        p100 = MediaPlayer.create(this, R.raw.php100);
        p200 = MediaPlayer.create(this, R.raw.php200);
        p500 = MediaPlayer.create(this, R.raw.php500);
        p1000 = MediaPlayer.create(this, R.raw.php1000);
        bg = MediaPlayer.create(this, R.raw.background);
        lang = MediaPlayer.create(this, R.raw.language);
        dir = MediaPlayer.create(this, R.raw.direction);

        previewView = findViewById(R.id.previewView);
        main_layout = findViewById(R.id.layout_main);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(()-> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                startCameraX(cameraProvider);

            }catch (Exception e){
                e.printStackTrace();
            }
        }, getExecutor());
    }

    public void startCameraX(ProcessCameraProvider cameraProvider){

        cameraProvider.unbindAll();

        //Camera Provider
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        //Preview
        Preview preview = new Preview.Builder().build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        //Image Capture
        imageCapture = new ImageCapture.Builder()
                .setTargetResolution(new Size(2240, 2240))
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        cam = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageCapture);

        if ( cam.getCameraInfo().hasFlashUnit() ) {
            cam.getCameraControl().enableTorch(true); // or false
        }

    }

    private void capturePhoto(){
        imageCapture.takePicture(getExecutor(), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {

                try {
                    bitmap = getBitmap(imageProxy);
                    classifyImage(bitmap);

                }catch (Exception e){
                    e.printStackTrace();
                }

                imageProxy.close();
            }
            @Override
            public void onError(@NonNull ImageCaptureException exception){
                Toast.makeText(MainActivity.this, "Photo Failed to Capture", Toast.LENGTH_LONG).show();

            }
        });
    }

    private Bitmap getBitmap(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        buffer.rewind();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        byte[] clonedBytes = bytes.clone();
        return BitmapFactory.decodeByteArray(clonedBytes, 0, clonedBytes.length);
    }

    private boolean allPermissionGranted() {
        for(String permission:REQUIRE_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission)!= PackageManager.PERMISSION_GRANTED){
                return false;

            }
        }
        return true;
    }

    public void classifyImage(Bitmap image){
        if (image != null){
            try {
                image = Bitmap.createScaledBitmap(image, imageSize, imageSize, true);
                UnquantModelEfficientnetv2B021kFt1k model = UnquantModelEfficientnetv2B021kFt1k.newInstance(MainActivity.this);

                // Creates inputs for reference.

                TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
                ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
                byteBuffer.order(ByteOrder.nativeOrder());
                int[] intVal = new int[imageSize * imageSize];
                image.getPixels(intVal, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
                int pixel =0;
                for (int i=0; i<imageSize; i++){
                    for (int j=0; j<imageSize; j++){
                        int val = intVal[pixel++];//RGB
                        byteBuffer.putFloat(((val >> 16)& 0xFF)* (1.f/255));
                        byteBuffer.putFloat(((val >> 8)& 0xFF)* (1.f/255));
                        byteBuffer.putFloat((val & 0xFF)* (1.f/255));
                    }
                }

                inputFeature0.loadBuffer(byteBuffer);

                // Runs model inference and gets result.
                UnquantModelEfficientnetv2B021kFt1k.Outputs outputs = model.process(inputFeature0);
                TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

                float[] confidence = outputFeature0.getFloatArray();
                //find the index of the class with biggest confidence
                int maxPos=0;
                float maxConfidence=0;
                for(int i=0;i<confidence.length;i++){
                    if(confidence[i]>maxConfidence) {
                        maxConfidence = confidence[i];
                        maxPos = i;
                    }
                }
                speakDetectionResult(maxPos);
                // Releases model resources if no longer used.
                model.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else{
            Toast.makeText(MainActivity.this, image.getHeight() +" "+ image.getWidth(), Toast.LENGTH_LONG).show();
        }

    }

    private void speakResult(String word){
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech.speak(word, TextToSpeech.QUEUE_ADD, null);
                }
            }
        });


    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener{
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

            if (cam.getCameraInfo().hasFlashUnit() ) {
                cam.getCameraControl().enableTorch(true); // or false
            }
            capturePhoto();

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

                        }
                    });
                }
            }, 2000);
            return super.onSingleTapConfirmed(e);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

            loadLang();

            switch (language){
                case "en":
                    saveLang("ph");
                    lang.start();
                    break;
                case "ph":
                    saveLang("en");
                    speakResult("The language is set to English.");
                    break;


            }

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

                        }
                    });
                }
            }, 1000);
            super.onLongPress(e);
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    public void saveLang(String lang){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(LANG, lang);
        editor.apply();

    }

    public void loadLang(){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        language = sharedPreferences.getString(LANG, "en");


    }

    public void speakDetectionResult(int index){
        loadLang();
        if (language.equals("en")){
            String[] classes = {
                    "No peso banknote detected",
                    "No peso banknote detected",
                    "This is a 100 Peso Bill",
                    "This is a 1000 Peso Bill",
                    "This is a 20 Peso Bill",
                    "This is a 200 Peso Bill",
                    "This is a 50 Peso Bill",
                    "This is a 500 Peso Bill",
                    "This is a Play money"};

            speakResult(classes[index]);
        }
        else if(language.equals("ph")){
            switch (index){
                case 0:
                case 1:
                    bg.start();
                    break;
                case 2:
                    p100.start();
                    break;
                case 3:
                    p1000.start();
                    break;
                case 4:
                    p20.start();
                    break;
                case 5:
                    p200.start();
                    break;
                case 6:
                    p50.start();
                    break;
                case 7:
                    p500.start();
                    break;
                case 8:
                    pm.start();
                    break;
            }
        }
    }

    public void onDenyPermission() {
        // Create the object of AlertDialog Builder class
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        // Set the message show for the Alert time
        builder.setMessage("The application needs permission to use the camera in order to function correctly.");

        // Set Alert Title
        builder.setTitle("Permission Denied");

        // Set Cancelable false for when the user clicks on the outside the Dialog Box then it will remain show
        builder.setCancelable(false);

        builder.setNeutralButton("OK", (DialogInterface.OnClickListener) (dialog, which) -> {
            finish();
        });

        // Create the Alert dialog
        AlertDialog alertDialog = builder.create();
        // Show the Alert Dialog box
        alertDialog.show();
    }
}