package org.fairdatasociety.fairos;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.loader.content.CursorLoader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import fairos.Fairos;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    PodsAdaptor adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("MainActivity", "****************************MainActivity");
        super.onCreate(savedInstanceState);
        // check if credentials are saved in sharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("FairOS", MODE_PRIVATE);
        String username = sharedPreferences.getString("username", "");
        String password = sharedPreferences.getString("password", "");
        assert username != null;
        assert password != null;
        if (username.equals("") || password.equals("")) {
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            return;
        }
        setContentView(R.layout.activity_main);
        CircularProgressIndicator progress = findViewById(R.id.progress);
        String dataDir = getApplicationContext().getFilesDir() + "/fairos";
        Context self = this;
        Observable.create((Observable.OnSubscribe<String>) emitter -> {
            try {
                if (!Fairos.isConnected()) {
                    Utils.init(dataDir, self);
                }
                if (!Fairos.isUserLoggedIn(username)) {
                    Fairos.loginUser(username, password);
                }
                emitter.onNext("User logged in");
                emitter.onCompleted();
            } catch (Exception e) {
                emitter.onError(e);
            }
            emitter.onCompleted();
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Observer() {
            @Override
            public void onNext(Object o) {
                Intent i = new Intent(getApplicationContext(), ListActivity.class);
                startActivity(i);
            }

            @Override
            public void onError(Throwable e) {
                Snackbar.make(findViewById(android.R.id.content), "init failed: " + e.getMessage(), Snackbar.LENGTH_SHORT)
                        .setAnchorView(findViewById(R.id.message))
                        .show();
                progress.hide();
            }

            @Override
            public void onCompleted() {
                progress.hide();
            }
        });
    }
}