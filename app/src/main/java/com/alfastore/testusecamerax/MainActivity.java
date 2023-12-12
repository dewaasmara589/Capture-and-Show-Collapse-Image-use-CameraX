package com.alfastore.testusecamerax;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import static android.Manifest.permission.CAMERA;

public class MainActivity extends AppCompatActivity {
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private TextView tvWarning, tvSysDate;
    private PreviewView[] PVS = new PreviewView[6];

    private Button btnCapture, btnSave;
    private ConstraintLayout clContent;
    private ImageView[] IVS = new ImageView[6];
    private ImageCapture imageCapture;

    private static final int CAMERA_PERMISSION_CODE = 1;

    private int indexCamera = 0;
    private int indexImage = 0;

    private ImageView ibFlash;
    private boolean flash = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvWarning = findViewById(R.id.tvWarning);
        tvSysDate = findViewById(R.id.tvSysDate);

        btnCapture = findViewById(R.id.btnCapture);
        btnSave = findViewById(R.id.btnSave);

        ibFlash = findViewById(R.id.ibFlash);
        ibFlash.setImageResource(R.drawable.ic_flash_off);

        clContent = findViewById(R.id.clContent);

        PVS[0] = findViewById(R.id.previewView1);
        PVS[1] = findViewById(R.id.previewView2);
        PVS[2] = findViewById(R.id.previewView3);
        PVS[3] = findViewById(R.id.previewView4);
        PVS[4] = findViewById(R.id.previewView5);
        PVS[5] = findViewById(R.id.previewView6);

        IVS[0] = findViewById(R.id.ivCapture1);
        IVS[1] = findViewById(R.id.ivCapture2);
        IVS[2] = findViewById(R.id.ivCapture3);
        IVS[3] = findViewById(R.id.ivCapture4);
        IVS[4] = findViewById(R.id.ivCapture5);
        IVS[5] = findViewById(R.id.ivCapture6);

        btnCapture.setOnClickListener(view -> {
            capturePhoto();
        });

        ibFlash.setOnClickListener(view -> {
            flash = !flash;

            if (flash){
                ibFlash.setImageResource(R.drawable.ic_flash_on);
            }else {
                ibFlash.setImageResource(R.drawable.ic_flash_off);
            }

            cameraProviderFuture = ProcessCameraProvider.getInstance(this);
            cameraProviderFuture.addListener(() -> {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    startCameraX(cameraProvider);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }

            }, getExecutor());
        });

        btnSave.setOnClickListener(view -> {
            setSaveBtnClick();
        });

        checkPermissionCamera();
    }

    private void checkPermissionCamera(){
        if (ActivityCompat.checkSelfPermission(MainActivity.this, CAMERA) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{CAMERA}, CAMERA_PERMISSION_CODE);
        }else {
            btnCapture.setVisibility(View.VISIBLE);
            ibFlash.setVisibility(View.VISIBLE);
            cameraProviderFuture = ProcessCameraProvider.getInstance(this);
            cameraProviderFuture.addListener(() -> {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    startCameraX(cameraProvider);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }

            }, getExecutor());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case CAMERA_PERMISSION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    btnCapture.setVisibility(View.VISIBLE);
                    ibFlash.setVisibility(View.VISIBLE);
                    cameraProviderFuture = ProcessCameraProvider.getInstance(this);
                    cameraProviderFuture.addListener(() -> {
                        try {
                            ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                            startCameraX(cameraProvider);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }

                    }, getExecutor());
                } else {
                    tvWarning.setText("Permission Denied, application cannot access CAMERA.");
                }
                break;
        }
    }

    @SuppressLint("RestrictedApi")
    private void startCameraX(ProcessCameraProvider cameraProvider) {

        cameraProvider.unbindAll();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        Preview preview = new Preview.Builder().build();

        preview.setSurfaceProvider(PVS[indexCamera].getSurfaceProvider());

        if (flash){
            imageCapture = new ImageCapture.Builder()
                    .setFlashMode(ImageCapture.FLASH_MODE_ON)
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build();
        }else {
            imageCapture = new ImageCapture.Builder()
                    .setFlashMode(ImageCapture.FLASH_MODE_OFF)
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build();
        }

        Preview previewConfig = new Preview.Builder().build();

        Camera cam = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, previewConfig);
        if ( cam.getCameraInfo().hasFlashUnit() ) {
            cam.getCameraControl().enableTorch(flash);
        }
    }

    private Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
    }

    private void capturePhoto() {
        Bitmap bitmap = PVS[indexCamera].getBitmap();

        BitmapDrawable ob = new BitmapDrawable(getResources(), bitmap);

        IVS[indexImage].setBackground(ob);
        PVS[indexCamera].setVisibility(View.GONE);
        IVS[indexImage].setVisibility(View.VISIBLE);

        if (indexCamera == 5 && indexImage == 5){
            btnCapture.setVisibility(View.INVISIBLE);
            ibFlash.setVisibility(View.INVISIBLE);

            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");
            String date = dateFormat.format(calendar.getTime());
            tvSysDate.setText(date);

            btnSave.setVisibility(View.VISIBLE);
        }

        indexCamera++;
        indexImage++;

        // Start Camera
        if (indexCamera < 6 && indexImage < 6){
            cameraProviderFuture = ProcessCameraProvider.getInstance(this);
            cameraProviderFuture.addListener(() -> {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    startCameraX(cameraProvider);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }

            }, getExecutor());
        }
    }

    private void setSaveBtnClick() {
        clContent = (ConstraintLayout) findViewById(R.id.clContent);

        Bitmap returnedBitmap = Bitmap.createBitmap(clContent.getWidth(), clContent.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        clContent.draw(canvas);

        // Make Images to String
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        returnedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        byte[] imageBytes = stream.toByteArray();
        String imageString = Base64.encodeToString(imageBytes, Base64.DEFAULT);

        Log.e("TESTES", "Images String : " + imageString);

        saveImage(returnedBitmap);
    }

    private void saveImage(Bitmap getBitmap) {
        FileOutputStream outStream;
        try {
            String fileName = "Capture_" + System.currentTimeMillis() + ".jpg";
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), fileName);
            outStream = new FileOutputStream(file);
            getBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outStream);
            outStream.close();
            Toast.makeText(MainActivity.this, "Picture Saved: " + fileName, Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}