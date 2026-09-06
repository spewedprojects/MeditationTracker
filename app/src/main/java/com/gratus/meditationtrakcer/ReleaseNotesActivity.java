package com.gratus.meditationtrakcer;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gratus.meditationtrakcer.adapters.ReleaseNotesAdapter;
import com.gratus.meditationtrakcer.utils.TextFormatUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.Markwon;
import io.noties.markwon.core.MarkwonTheme;
import io.noties.markwon.ext.tasklist.TaskListPlugin;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.html.HtmlPlugin;

public class ReleaseNotesActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private ReleaseNotesAdapter adapter;
    private List<String> releaseNotesList = new ArrayList<>();
    private List<String> latestReleaseNotes = new ArrayList<>();
    private TextView currentVersionDetails;

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
        setupToolbar(R.id.toolbar2, R.id.menubutton, R.string.title_activity_release_notes);

//        TextView formattedCurrentVersionDetails = findViewById(R.id.current_version_details);
//        // Get raw text from resources
//        String rawText = getString(R.string.current_version_details);
//
//        // Format it using your utility
//        CharSequence formattedText = TextFormatUtils.formatNotesForDisplay(rawText);
//
//        // Apply to TextView
//        formattedCurrentVersionDetails.setText(formattedText);

        // Initialize Markdown processor & ✅ Enable Table Plugin
        Markwon markwon = Markwon.builder(this)
                .usePlugin(TaskListPlugin.create(this))
                .usePlugin(TablePlugin.create(this))
                .usePlugin(HtmlPlugin.create())
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(new AbstractMarkwonPlugin() {
                    @Override
                    public void configureTheme(@NonNull MarkwonTheme.Builder builder) {
                        float density = ReleaseNotesActivity.this.getResources().getDisplayMetrics().density;
                        // Set a custom block margin (e.g., 18dp)
                        builder.blockMargin((int) (24 * density + 0.5f));
                        // 🔹 Define the size of the bullet point (diameter)
                        // Default is usually ~8dp, reducing to 4dp for a cleaner look
                        builder.bulletWidth((int) (4 * density + 0.5f));
                    }
                })
                .build();

        readLatestReleaseNotes();
        currentVersionDetails = findViewById(R.id.current_version_details);
        if (!latestReleaseNotes.isEmpty()) {
            // Apply line break fix for consistency
            String rawText = latestReleaseNotes.get(0).replace("\\n", "\n");
            markwon.setMarkdown(currentVersionDetails, rawText);
        }

        recyclerView = findViewById(R.id.RN_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        readReleaseNotes();
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
            Log.e("ReleaseNotesActivity", "Error reading release notes: " + e.getMessage(), e);
        }
    }

    private void readLatestReleaseNotes() {
        try {
            InputStream inputStream = getResources().openRawResource(R.raw.latest_release);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n"); // Keep new lines
            }
            reader.close();
            latestReleaseNotes.add(stringBuilder.toString()); // Store entire content
        } catch (Exception e) {
            Log.e("ReleaseNotesActivity", "Error reading latest release notes: " + e.getMessage(), e);
        }
    }

}
