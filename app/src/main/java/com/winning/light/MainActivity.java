package com.winning.light;

import android.Manifest;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.winning.light_core.Light;
import com.winning.light_core.LightConfig;
import com.winning.light_core.lightprotocol.TLVDecoder;
import com.winning.light_core.lightprotocol.TLVManager;
import com.winning.light_core.lightprotocol.TLVObject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks,
        EasyPermissions.RationaleCallbacks{
    private static final String FILE_NAME = "light_v1";
    private Button btnWriteToFile;
    private Button btnGetFileContent;
    private Button btnGetAllFile;
    private static final int RC_STORAGE_PERM = 123;
    private static final String[] WRITE_AND_READ_STORAGE =
            {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String path = getApplicationContext().getExternalFilesDir(null).getAbsolutePath()
                + File.separator + FILE_NAME;
        File file = new File(path);
        if (!file.exists()){
            file.mkdirs();
        }
        LightConfig config = new LightConfig.Builder()
                .setCachePath(getApplicationContext().getFilesDir().getAbsolutePath())
                .setPath(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()
                        + File.separator + FILE_NAME)
                .build();

        Light.init(config);

        btnWriteToFile = findViewById(R.id.btnWriteToFile);
        btnGetFileContent = findViewById(R.id.btnGetFileContent);
        btnGetAllFile = findViewById(R.id.btnGetAllFile);
        btnWriteToFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                write2File();
            }
        });

        btnGetFileContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte[] tlvByteArrays = Light.g("2018-10-22", TLVManager.BATTERY);

                List<List<TLVObject>> tlvObjects = TLVManager.convertSumTagValue(tlvByteArrays, new ArrayList<List<TLVObject>>());
                for (List<TLVObject> tlvObjects2 : tlvObjects){
                    for (TLVObject tlvObject : tlvObjects2) {
                        System.out.println("\n");
                        System.out.println(TLVDecoder.encodeHexStr(tlvObject.getTagValue(), true));

                        String sssString = null;
                        try {
                            sssString = new String(tlvObject.getTagValue(),"utf-8");
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        System.out.println(sssString);
                    }
                }
            }
        });

        btnGetAllFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Map<String, Long> allFile = Light.getAllFilesInfo();

                for (Map.Entry<String, Long> entry : allFile.entrySet()) {
                    System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
                }
            }
        });
    }

    @AfterPermissionGranted(RC_STORAGE_PERM)
    private void write2File() {
        if (hasWriteAndReadPermissions()) {
            // Have permissions, do the thing!
            Notification notification = new Notification();
            notification.setPkgName("军哥军军军军军军军军军军军军军军军军军军军军军军军军军军军军军军军军军军军军军军军军军军军军军军军军军军军军军军人军军军军人军军军军人军军军军人军军军军人军军军军人军军军军人军军军军人军军军军人军军军军人军军军军人军军军军人军军军军人军军军军人军军军军人军军军军人军军军军人军军军军人军军军军人军军军军人军军军军人军军军军人军军");
            notification.setUserName("君君");
            notification.setAlias("junge");
            Light.w(notification, TLVManager.BATTERY);
        } else {
            // Ask for both permissions
            EasyPermissions.requestPermissions(
                    MainActivity.this,
                    "申请存储卡选项用于存储",
                    RC_STORAGE_PERM,
                    WRITE_AND_READ_STORAGE);
        }
    }

    private boolean hasWriteAndReadPermissions() {
        return EasyPermissions.hasPermissions(this, WRITE_AND_READ_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // EasyPermissions handles the request result.
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }


    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onRationaleAccepted(int requestCode) {

    }

    @Override
    public void onRationaleDenied(int requestCode) {

    }
}
