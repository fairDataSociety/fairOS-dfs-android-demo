package org.fairdatasociety.fairos;

import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.content.CursorLoader;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import fairos.Fairos;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class UploadActivity extends AppCompatActivity {
    ProgressDialog progressBar;
    static final String POD = "consents";
    String PATH = "/";
    String username;
    String password;
    Intent intent;
    EditText fileInput;
    byte[] dataBytes;
    String filename;
    int size;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = getSharedPreferences("FairOS", MODE_PRIVATE);
        username = sharedPreferences.getString("username", "");
        password = sharedPreferences.getString("password", "");
        assert username != null;
        assert password != null;
        if (username.equals("") || password.equals("")) {
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            return;
        }

        setContentView(R.layout.activity_upload);
        fileInput = findViewById(R.id.filename);
        intent = getIntent();
        try {
            handleIntent();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Button clickButton = findViewById(R.id.upload_button);
        Button cancelButton = findViewById(R.id.cancel_button);

        Context self = this;
        cancelButton.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               Intent i = new Intent(getApplicationContext(), ListActivity.class);
               startActivity(i);
           }
        });

        clickButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filename = fileInput.getText().toString();
                if (filename.equals("")) {
                    Snackbar.make(findViewById(android.R.id.content), "file name cannot be blank", Snackbar.LENGTH_SHORT)
                            .show();
                    return;
                }
                progressBar = new ProgressDialog(v.getContext());
                progressBar.setCancelable(false);
                progressBar.setIndeterminate(true);
                progressBar.setMessage("Uploading content...");
                progressBar.show();
                Observable.create((Observable.OnSubscribe<String>) emitter -> {
                    try {
                        if (!Fairos.isConnected()) {
                            Utils.init(self);
                        }
                        if (!Fairos.isUserLoggedIn()) {
                            Fairos.loginUser(username, password);
                        }
                        Fairos.podOpen(POD);
                        emitter.onNext("");
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
                        upload();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Snackbar.make(findViewById(android.R.id.content), "Upload failed: " + e.getMessage(), Snackbar.LENGTH_SHORT)
                                .show();
                        progressBar.hide();
                    }

                    @Override
                    public void onCompleted() { }
                });
            }
        });
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        intent = i;
    }

    void upload() {
        Observable.create((Observable.OnSubscribe<JSONObject>) emitter -> {
            try {
                Fairos.blobUpload(dataBytes, POD, filename, PATH, "", size, 8192000, true);
            } catch (Exception e) {
                emitter.onError(e);
            }
            emitter.onCompleted();
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Observer() {
            @Override
            public void onNext(Object o) { }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
                Snackbar.make(findViewById(android.R.id.content), "Upload failed failed: " + e.getMessage(), Snackbar.LENGTH_SHORT)
                        .show();
                progressBar.hide();
            }

            @Override
            public void onCompleted() {
                progressBar.hide();
                Intent i = new Intent(getApplicationContext(), ListActivity.class);
                startActivity(i);
                finish();
            }
        });
    }

    void handleIntent() throws JSONException {
        if (intent != null) {
            String action = intent.getAction();
            String type = intent.getType();
            if (action != null) {
                switch (action) {
                    case Intent.ACTION_SEND:
                        if (type != null) {
                            size = 0;
                            dataBytes = new byte[0];
                            if (type.equalsIgnoreCase("text/plain")) {
                                Log.d("text/plain", intent.getStringExtra(Intent.EXTRA_TEXT));
                                String data = intent.getStringExtra(Intent.EXTRA_TEXT);
                                dataBytes = data.getBytes();
                                size = dataBytes.length;
                            } else {
                                Uri selectedUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                                if (selectedUri != null) {
                                    String[] projection = {MediaStore.MediaColumns.DATA};
                                    CursorLoader cursorLoader = new CursorLoader(this, selectedUri, projection, null, null, null);
                                    Cursor cursor = cursorLoader.loadInBackground();
                                    assert cursor != null;
                                    int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                                    cursor.moveToFirst();
                                    File file = new File(cursor.getString(column_index));
                                    size = (int) file.length();
                                    dataBytes = new byte[size];
                                    filename = file.getName();
                                    try {
                                        BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
                                        buf.read(dataBytes, 0, dataBytes.length);
                                        buf.close();
                                    } catch (IOException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }
                                }
                            }
                            fileInput.setText(filename);
                        }
                        break;
                    case Intent.ACTION_VIEW:
                        Uri data = getIntent().getData();

                        assert data != null;
                        String path = data.getPath();
                        assert path != null;
                        String[] parts = path.split("/");
                        if(parts.length >= 3) {
                            String filename = parts[2];
                            fileInput.setText(filename);

                            String host = data.getHost();
                            String scheme = data.getScheme();

                            String queryParameter = data.getQueryParameter("data");
                            if (queryParameter != null && !Objects.equals(queryParameter, "")) {
                                dataBytes = Base64.decode(queryParameter, Base64.DEFAULT);
                                size = dataBytes.length;
                            } else {
                                Context self = this;
                                progressBar = new ProgressDialog(self);
                                progressBar.setIndeterminate(true);
                                progressBar.setMessage("Loading content...");
                                progressBar.show();
                                progressBar.setCancelable(false);

                                Observable.create((Observable.OnSubscribe<JSONObject>) emitter -> {
                                    try {
                                        if (!Fairos.isConnected()) {
                                            Utils.init(self);
                                        }
                                        if (!Fairos.isUserLoggedIn()) {
                                            Fairos.loginUser(username, password);
                                        }
                                        Fairos.podOpen(POD);
                                        byte[] fileBytes = Fairos.fileDownload(POD, "/"+filename);
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
                                            myIntent.putExtra("name", filename);
                                            startActivity(myIntent);
                                            finish();
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        progressBar.hide();
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        Snackbar.make(findViewById(android.R.id.content), "download failed: " + e.getMessage(), Snackbar.LENGTH_SHORT)
                                                .show();
                                        Intent i = new Intent(getApplicationContext(), ListActivity.class);
                                        i.putExtra("error", e.getMessage());
                                        startActivity(i);
                                        finish();
                                        progressBar.hide();
                                    }

                                    @Override
                                    public void onCompleted() { }
                                });
                            }
                        } else {
                            Intent i = new Intent(getApplicationContext(), ListActivity.class);
                            i.putExtra("error", "wrong url");
                            startActivity(i);
                            finish();
                        }
                        break;
                    default:
                        Log.d("handleIntent", "default");
                        // code to be executed if value is not "hi","hello" or "bonjour"
                }
            }
        }
    }
}