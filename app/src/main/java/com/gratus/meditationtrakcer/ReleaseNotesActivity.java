package com.gratus.meditationtrakcer;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gratus.meditationtrakcer.adapters.ReleaseNotesAdapter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.html.HtmlPlugin;

public class ReleaseNotesActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private ReleaseNotesAdapter adapter;
    private List<String> releaseNotesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_releasenotes);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize the toolbar and menu button
        setupToolbar(R.id.toolbar2, R.id.menubutton);

        recyclerView = findViewById(R.id.RN_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        readReleaseNotes();

        // Initialize Markdown processor & ✅ Enable Table Plugin
        Markwon markwon = Markwon.builder(this)
                .usePlugin(TablePlugin.create(this)) // 📌 Add table support
                //.usePlugin(HtmlPlugin.create()) // 👈 Add this for HTML table support
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

}
