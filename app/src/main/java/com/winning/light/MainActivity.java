package com.winning.light;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.winning.light_core.Light;
import com.winning.light_core.LightConfig;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static final String FILE_NAME = "light_v1";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LightConfig config = new LightConfig.Builder()
                .setCachePath(getApplicationContext().getFilesDir().getAbsolutePath())
                .setPath(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()
                        + File.separator + FILE_NAME)
                .build();

        Light.init(config);
    }
}
