package org.fairdatasociety.fairos;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.loader.content.CursorLoader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

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
import java.util.List;

import fairos.Fairos;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class ListActivity extends AppCompatActivity implements ListAdaptor.ItemClickListener {
    private int EXTERNAL_STORAGE_PERMISSION_CODE = 23;

    static final String POD = "consents";
    String PATH = "/";

    ListAdaptor adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        CircularProgressIndicator progress = findViewById(R.id.progress);
        Context self = this;
        Observable.create((Observable.OnSubscribe<JSONObject>) emitter -> {
            try {
                Fairos.podOpen(POD);
            } catch (Exception e) {
                if (!e.getMessage().equals("pod already open")) {
                    emitter.onError(e);
                }
            }

            try {
                String list = Fairos.dirList(POD, PATH);
                JSONObject data = new JSONObject(list);

                emitter.onNext(data);
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
                Snackbar.make(findViewById(android.R.id.content), "ls failed: " + e.getMessage(), Snackbar.LENGTH_SHORT)
                        .show();
                progress.hide();
            }

            @Override
            public void onCompleted() {
                progress.hide();
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d("onNewIntent POD", POD);
        Log.d("onNewIntent path", PATH);
        super.onNewIntent(intent);
        if (intent!= null) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    EXTERNAL_STORAGE_PERMISSION_CODE);
            String action = intent.getAction();
            String type = intent.getType();
            if (Intent.ACTION_SEND.equals(action) && type != null) {
                CircularProgressIndicator progress = findViewById(R.id.progress);
                if (type.equalsIgnoreCase("text/plain")) {

                } else {
                    Uri selectedImageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                    if (selectedImageUri != null) {

                        String[] projection = {MediaStore.MediaColumns.DATA};
                        CursorLoader cursorLoader = new CursorLoader(this, selectedImageUri, projection, null, null, null);
                        Cursor cursor = cursorLoader.loadInBackground();
                        int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                        cursor.moveToFirst();
                        File file = new File(cursor.getString(column_index));
                        int size = (int) file.length();

                        byte[] bytes = new byte[size];
                        try {
                            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
                            buf.read(bytes, 0, bytes.length);
                            buf.close();

                            Context self = this;
                            Observable.create((Observable.OnSubscribe<JSONObject>) emitter -> {
                                try {
                                    Fairos.blobUpload(bytes, POD, file.getName(), PATH, "", file.length(), 2048000);
                                } catch (Exception e) {
                                    emitter.onError(e);
                                }
                                try {
                                    String list = Fairos.dirList(POD, PATH);
                                    JSONObject data = new JSONObject(list);
                                    emitter.onNext(data);
                                    emitter.onCompleted();
                                } catch (Exception e) {
                                    emitter.onError(e);
                                }
                                progress.hide();
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
                                    Snackbar.make(findViewById(android.R.id.content), "ls failed: " + e.getMessage(), Snackbar.LENGTH_SHORT)
                                            .show();
                                    progress.hide();
                                }

                                @Override
                                public void onCompleted() { progress.hide(); }
                            });
                        } catch (FileNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
                progress.show();
            }
        }
    }


    @Override
    public void onItemClick(View view, int position) {
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