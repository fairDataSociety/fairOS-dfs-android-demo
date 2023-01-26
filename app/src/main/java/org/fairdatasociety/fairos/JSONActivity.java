package org.fairdatasociety.fairos;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class JSONActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_json);
        TextView jsonTextView = findViewById(R.id.data);
        TextView nameTextView = findViewById(R.id.name);
        String data = getIntent().getStringExtra("data");
        String name = getIntent().getStringExtra("name");
        jsonTextView.setText(data);
        nameTextView.setText(name);
    }
}