package com.example.taylorq.roasted;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int IMAGE_CAPTURE = 0;
    private static final int IMAGE_CROP = 1;

    ImageView image;
    TextView colorView;
    Button capture, crop;

    Bitmap bitmap;
    Uri tempUri, photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestAllPower();

        image = findViewById(R.id.imageView);
        colorView = findViewById(R.id.colorView);

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch(view.getId()){
                    case R.id.capture:
                        image_capture();
                        break;
                    case R.id.crop:
                        image_crop();
                        break;
                }
            }
        };

        capture = findViewById(R.id.capture);
        capture.setOnClickListener(onClickListener);

        crop = findViewById(R.id.crop);
        crop.setOnClickListener(onClickListener);
        crop.setEnabled(false);
    }

    public void requestAllPower() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                Toast.makeText(this, "no need to ask", Toast.LENGTH_SHORT).show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PERMISSION_GRANTED) {
                    Toast.makeText(this, "" + "权限" + permissions[i] + "申请成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "" + "权限" + permissions[i] + "申请失败", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void image_capture(){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        File dir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath()+"/roasted");
        if(!dir.exists()){
            dir.mkdirs();
        }
        File image = null;
        try {
            image = File.createTempFile("roasted_"+System.currentTimeMillis(), ".jpg", dir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (Build.VERSION.SDK_INT >= 24) {
            tempUri = FileProvider.getUriForFile(getApplicationContext(), "com.example.taylorq.roasted.FileProvider", image);
        } else {
            tempUri = Uri.fromFile(image);
        }
        intent.putExtra(MediaStore.EXTRA_OUTPUT, tempUri);
        startActivityForResult(intent, IMAGE_CAPTURE);
    }

    private void image_crop() {
        Intent intent = new Intent("com.android.camera.action.CROP");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        intent.setDataAndType(photoUri, "image/*");
        //是否可裁剪
        intent.putExtra("corp", "true");
        //裁剪器高宽比
        intent.putExtra("aspectY", 1);
        intent.putExtra("aspectX", 1);
        //设置裁剪框高宽
        intent.putExtra("outputX", 150);
        intent.putExtra("outputY", 150);
        //返回数据
        intent.putExtra("return-data", true);
        startActivityForResult(intent, IMAGE_CROP);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode){
            case IMAGE_CAPTURE:
                if (resultCode == Activity.RESULT_OK){
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), tempUri);
                        image.setImageBitmap(bitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    crop.setEnabled(true);
                    photoUri = tempUri;
                    extractColor();
                }
                break;
            case IMAGE_CROP:
                if (resultCode == Activity.RESULT_OK){
                    Bundle bundle = data.getExtras();
                    if (bundle != null){
                        bitmap = bundle.getParcelable("data");
                        image.setImageBitmap(bitmap);
                    }
                    extractColor();
                }
                break;
        }
    }

    private void extractColor(){
        int xblock = bitmap.getWidth()/51;
        int yblock = bitmap.getHeight()/51;
        int a = 0, r = 0, g = 0, b = 0;
        for (int i = 0;i < 50;i++){
            for (int j = 0;j < 50;j++){
                int color = bitmap.getPixel(i*xblock, j*yblock);
                a += Color.alpha(color);
                r += Color.red(color);
                g += Color.green(color);
                b += Color.blue(color);
            }
        }
        a /= 250;
        r /= 250;
        g /= 250;
        b /= 250;
        int color = Color.argb(a, r, g, b);
        colorView.setBackgroundColor(color);
    }
}
