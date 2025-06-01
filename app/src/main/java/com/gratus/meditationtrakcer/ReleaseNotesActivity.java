package com.gratus.meditationtrakcer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.tables.TablePlugin;

public class ReleaseNotesActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private ReleaseNotesAdapter adapter;
    private List<String> releaseNotesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_releasenotes);

        // Initialize the toolbar and menu button
        setupToolbar(R.id.toolbar2, R.id.menubutton);

        recyclerView = findViewById(R.id.RN_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        readReleaseNotes();

        // Initialize Markdown processor & ✅ Enable Table Plugin
        Markwon markwon = Markwon.builder(this)
                .usePlugin(TablePlugin.create(this)) // 📌 Add table support
                .build();

        // Pass Markwon to the adapter
        adapter = new ReleaseNotesAdapter(releaseNotesList, markwon);
        recyclerView.setAdapter(adapter);
    }

    private void readReleaseNotes() {
        try {
            InputStream inputStream = getResources().openRawResource(R.raw.release_notes);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n"); // Keep new lines
            }

            reader.close();
            releaseNotesList.add(stringBuilder.toString()); // Store entire content
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    @Override
//    public void onBackPressed() {
//        super.onBackPressed();
//        Intent intent = new Intent(ReleaseNotesActivity.this, MainActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(intent);
//        finish();
//    }
}
