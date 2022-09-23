package org.fairdatasociety.fairos;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import fairos.Fairos;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        SharedPreferences sharedPreferences = getSharedPreferences("FairOS", MODE_PRIVATE);

        Button clickButton = (Button) findViewById(R.id.login_button);
        CircularProgressIndicator progress = findViewById(R.id.progress);
        Context self = this;
        progress.hide();
        clickButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText user = findViewById(R.id.username);
                String username = user.getText().toString();

                EditText pass = findViewById(R.id.password);
                String password = pass.getText().toString();

                progress.show();

                Observable.create((Observable.OnSubscribe<String>) emitter -> {
                    try {
                        if (!Fairos.isConnected()) {
                            Utils.init("", self);
                        }
                        Fairos.loginUser(username, password);
                    } catch (Exception e) {
                        emitter.onError(e);
                    }
                    emitter.onNext("login successful");
                    SharedPreferences.Editor fairosEditor = sharedPreferences.edit();
                    fairosEditor.putString("username", username);
                    fairosEditor.putString("password", password);
                    fairosEditor.apply();
                    emitter.onCompleted();
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer() {
                    @Override
                    public void onNext(Object o) { }

                    @Override
                    public void onError(Throwable e) {
                        Snackbar.make(findViewById(android.R.id.content), "Login failed: " + e.getMessage(), Snackbar.LENGTH_SHORT)
                                .setAnchorView(findViewById(R.id.message))
                                .show();
                        progress.hide();
                    }

                    @Override
                    public void onCompleted() {
                        String username = sharedPreferences.getString("username", "");
                        String password = sharedPreferences.getString("password", "");
                        assert username != null;
                        assert password != null;

                        Snackbar.make(findViewById(android.R.id.content), "Login Successful", Snackbar.LENGTH_SHORT)
                                .setAnchorView(findViewById(R.id.message))
                                .show();
                        progress.hide();
                        Intent myIntent = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(myIntent);
                    }
                });
            }
        });
    }
}