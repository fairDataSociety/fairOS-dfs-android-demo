package org.fairdatasociety.fairos;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import fairos.Fairos;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class ListActivity extends AppCompatActivity implements ListAdaptor.ItemClickListener {
    private int EXTERNAL_STORAGE_PERMISSION_CODE = 23;
    ProgressDialog progressBar;

    static final String POD = "consents";
    String PATH = "/";

    ListAdaptor adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                EXTERNAL_STORAGE_PERMISSION_CODE);;
        Context self = this;

        progressBar = new ProgressDialog(self);
        progressBar.setCancelable(false);
        progressBar.setIndeterminate(true);
        progressBar.setMessage("Loading content...");
        progressBar.show();

        Observable.create((Observable.OnSubscribe<JSONObject>) emitter -> {
            try {
                if (!Fairos.isConnected()) {
                    Utils.init(self);
                }
                SharedPreferences sharedPreferences = getSharedPreferences("FairOS", MODE_PRIVATE);
                String username = sharedPreferences.getString("username", "");
                String password = sharedPreferences.getString("password", "");
                assert username != null;
                assert password != null;
                if (username.equals("") || password.equals("")) {
                    startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                    return;
                }
                if (!Fairos.isUserLoggedIn()) {
                    Fairos.loginUser(username, password);
                }
                if (Fairos.isPodPresent(POD)) {
                    Fairos.podOpen(POD);
                } else {
                    Fairos.newPod(POD);
                }

                String list = Fairos.dirList(POD, PATH);
                JSONObject data = new JSONObject(list);

                emitter.onNext(data);
                emitter.onCompleted();
            } catch (Exception e) {
                if (!e.getMessage().equals("pod already open")) {
                    emitter.onError(e);
                }
            }
            emitter.onCompleted();
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Observer() {
            @Override
            public void onNext(Object o) {
                JSONObject list = (JSONObject) o;
                RecyclerView recyclerView = findViewById(R.id.files);
                recyclerView.setLayoutManager(new LinearLayoutManager(self));

                try {
                    JSONArray files = list.getJSONArray("files");
                    JSONArray dirs = list.getJSONArray("dirs");

                    ArrayList<String> filesList = new ArrayList<String>();
                    ArrayList<String> dirsList = new ArrayList<String>();
                    if (files != null) {
                        for (int i = 0; i < files.length(); i++) {
                            filesList.add(files.getString(i));
                        }
                    }
                    if (dirs != null) {
                        for (int i = 0; i < dirs.length(); i++) {
                            dirsList.add(dirs.getString(i));
                        }
                    }
                    adapter = new ListAdaptor(
                            self,
                            dirsList,
                            filesList
                    );
                    adapter.setClickListener((ListAdaptor.ItemClickListener) self);
                    recyclerView.setAdapter(adapter);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
                Snackbar.make(findViewById(android.R.id.content), "ls failed: " + e.getMessage(), Snackbar.LENGTH_SHORT)
                        .show();
                progressBar.hide();
            }

            @Override
            public void onCompleted() {
                String error = getIntent().getStringExtra("error");
                if (error != null && !error.equals("")) {
                    Snackbar.make(findViewById(android.R.id.content), "download failed: " + error, Snackbar.LENGTH_SHORT)
                            .show();
                }
                progressBar.hide();
            }
        });
    }

    @Override
    public void onItemClick(View view, int position) {
        Item item = adapter.getItem(position);
        if (item.type.equals("file")) {
            Context self = this;
            progressBar = new ProgressDialog(self);
            progressBar.setCancelable(false);
            progressBar.setIndeterminate(true);
            progressBar.setMessage("Loading content...");
            progressBar.show();
            Observable.create((Observable.OnSubscribe<JSONObject>) emitter -> {
                try {
                    byte[] fileBytes = Fairos.fileDownload(POD, "/"+item.name);
                    String jsonString = new String(fileBytes, StandardCharsets.UTF_8);
                    JSONObject jsonObject = new JSONObject(jsonString);
                    emitter.onNext(jsonObject);
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
                    JSONObject consent = (JSONObject) o;
                    Intent myIntent = new Intent(getApplicationContext(), JSONActivity.class);
                    try {
                        myIntent.putExtra("data", consent.toString(4));
                        myIntent.putExtra("name", item.name);
                        startActivity(myIntent);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    progressBar.hide();
                }

                @Override
                public void onError(Throwable e) {
                    Snackbar.make(findViewById(android.R.id.content), "download failed: " + e.getMessage(), Snackbar.LENGTH_SHORT)
                            .show();
                    progressBar.hide();
                }

                @Override
                public void onCompleted() { }
            });
        }
//        Item item = adapter.getItem(position);
//        CircularProgressIndicator progress = findViewById(R.id.progress);
//        progress.show();
//        if (item.type.equals("dir")) {
//            PATH = PATH.equals("/") ? "/"+item.name: PATH+"/"+item.name;
//            Context self = this;
//            Observable.create((Observable.OnSubscribe<JSONObject>) emitter -> {
//                try {
//                    String list = Fairos.dirList(POD, PATH);
//                    JSONObject data = new JSONObject(list);
//                    emitter.onNext(data);
//                    emitter.onCompleted();
//                } catch (Exception e) {
//                    emitter.onError(e);
//                }
//                emitter.onCompleted();
//            })
//            .subscribeOn(Schedulers.io())
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribe(new Observer() {
//                @Override
//                public void onNext(Object o) {
//                    JSONObject list = (JSONObject) o;
//                    RecyclerView recyclerView = findViewById(R.id.files);
//                    recyclerView.setLayoutManager(new LinearLayoutManager(self));
//                    try {
//                        JSONArray files = list.getJSONArray("files");
//                        JSONArray dirs = list.getJSONArray("dirs");
//
//                        ArrayList<String> filesList = new ArrayList<String>();
//                        ArrayList<String> dirsList = new ArrayList<String>();
//                        if (files != null) {
//                            for (int i = 0; i < files.length(); i++) {
//                                filesList.add(files.getString(i));
//                            }
//                        }
//                        if (dirs != null) {
//                            for (int i = 0; i < dirs.length(); i++) {
//                                dirsList.add(dirs.getString(i));
//                            }
//                        }
//                        adapter = new ListAdaptor(
//                                self,
//                                dirsList,
//                                filesList
//                        );
//                        adapter.setClickListener((ListAdaptor.ItemClickListener) self);
//                        recyclerView.setAdapter(adapter);
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                }
//
//                @Override
//                public void onError(Throwable e) {
//                    Snackbar.make(findViewById(android.R.id.content), "ls failed: " + e.getMessage(), Snackbar.LENGTH_SHORT)
//                            .show();
//                    progress.hide();
//                }
//
//                @Override
//                public void onCompleted() {
//                    progress.hide();
//                }
//            });
//        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}