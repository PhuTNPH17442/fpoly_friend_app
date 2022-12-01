package com.ltmt5.fpoly_friend_app.ui.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.gson.Gson;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.normal.TedPermission;
import com.ltmt5.fpoly_friend_app.BuildConfig;
import com.ltmt5.fpoly_friend_app.adapter.AddImageAdapter;
import com.ltmt5.fpoly_friend_app.databinding.ActivityQuestion6Binding;
import com.ltmt5.fpoly_friend_app.help.PublicData;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Question6Activity extends AppCompatActivity implements AddImageAdapter.ItemClick {

    public static int positionAdd = 0;
    public static List<Bitmap> bitmapList = new ArrayList<>();
    ActivityQuestion6Binding binding;
    AddImageAdapter addImageAdapter;
    File fileCamera;
    private Uri cameraUri;
    ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Bitmap bitmap = null;
                if (result.getData() == null) {
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(), cameraUri);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Uri imageUri = result.getData().getData();
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(), imageUri);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                bitmapList.set(positionAdd, bitmap);
                addImageAdapter.notifyDataSetChanged();
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuestion6Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initView();
        setClick();
    }

    private void initView() {
        bitmapList.clear();
        for (int i = 0; i < 6; i++) {
            bitmapList.add(null);

        }
        addImageAdapter = new AddImageAdapter(bitmapList, this, this);
        binding.recAddImage.setAdapter(addImageAdapter);
    }

    private void setClick() {
        ProgressDialog dialog= new ProgressDialog(this);
        dialog.setCancelable(false);
        dialog.setMessage("Loading...");


        binding.btnNext.setOnClickListener(v -> {
//            if (checkBitmap()) {
            dialog.show();
            List<String> list = new ArrayList<>();

            Executor executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());
            executor.execute(() -> {
                for (Bitmap bitmap:bitmapList){
                    if (bitmap!=null){
                        list.add(EncodeImage(bitmap));
                    }
                }
                handler.post(() -> {
                    dialog.dismiss();
                    PublicData.profileTemp.setImage(list);
                    startActivity(new Intent(this, WelcomeActivity.class));
                });
            });
//            }
        });
    }

    @Override
    public void clickItem(int position) {
        positionAdd = position;
        TedPermission.create().setPermissionListener(new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                launcher.launch(getIntentImage());
            }

            @Override
            public void onPermissionDenied(List<String> deniedPermissions) {

            }
        }).setDeniedMessage("If you reject permission,you can not use this service\n\nPlease turn on permissions at [Setting] > [Permission]").setPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE).check();

    }

    public String convertBitmapToArray(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos); //bm is the bitmap object
        byte[] b = baos.toByteArray();
        return Base64.encodeToString(b, Base64.DEFAULT);
    }

    private String EncodeImage(Bitmap bitmap){
        int previewWith = 150;
        int previewHeight = bitmap.getHeight() * previewWith / bitmap.getWidth();
        Bitmap previewBitmap  = Bitmap.createScaledBitmap(bitmap, previewWith, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG,50,byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    public Bitmap getBitmapFromArray(String encoded) {
        byte[] imageAsBytes = Base64.decode(encoded.getBytes(), Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length);
    }

    private Uri createCameraUri() {
        String nameTest = "camera_" + System.currentTimeMillis();
        try {
            fileCamera = File.createTempFile(nameTest, ".png");
        } catch (IOException e) {
            return null;
        }
        return FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", fileCamera);
    }

    public Intent getIntentImage() {
        cameraUri = createCameraUri();
        Intent pickIntent = new Intent();
        pickIntent.setType("image/*");
        pickIntent.setAction(Intent.ACTION_GET_CONTENT);
        pickIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        pickIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        pickIntent.putExtra(Intent.EXTRA_INDEX, 0);


        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
        String pickTitle = "Select or take a new Picture"; // Or get from strings.xml
        Intent chooserIntent = Intent.createChooser(pickIntent, pickTitle);
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takePhotoIntent});
        return chooserIntent;
    }

    @Override
    public void deleteItem(int position) {
        bitmapList.remove(position);
    }

    boolean checkBitmap() {
        boolean isDone = true;
        int count = 0;
        for (Bitmap bitmap : bitmapList) {
            if (bitmap != null) {
                count++;
            }
        }
        if (count < 2) {
            Toast.makeText(this, "Vui lòng chọn ít nhất 2 ảnh", Toast.LENGTH_SHORT).show();
            isDone = false;
        }
        return isDone;
    }
}