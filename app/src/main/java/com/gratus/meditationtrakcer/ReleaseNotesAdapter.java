package com.gratus.meditationtrakcer;

import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.tables.TablePlugin; // 📌 Import Table Plugin

public class ReleaseNotesAdapter extends RecyclerView.Adapter<ReleaseNotesAdapter.ViewHolder> {
    private List<String> releaseNotes;
    private Markwon markwon; // Markdown renderer

    public ReleaseNotesAdapter(List<String> releaseNotes, Markwon markwon) {
        this.releaseNotes = releaseNotes;
        this.markwon = markwon;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.release_items, parent, false);
        /*
        This will completely remove teh abnormal spacing beneath each inflated item.
         */
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT; // Set to wrap content
        view.setLayoutParams(layoutParams);
        /* Until here */
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        markwon.setMarkdown(holder.releaseNote, releaseNotes.get(position));
        // 🔹 Replace `\n` with real line breaks before rendering
        String formattedText = releaseNotes.get(position).replace("\\n", "\n");
        markwon.setMarkdown(holder.releaseNote, formattedText);
    }

    @Override
    public int getItemCount() {
        return releaseNotes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView releaseNote;

        public ViewHolder(View itemView) {
            super(itemView);
            releaseNote = itemView.findViewById(R.id.release_notes_text);
        }
    }
}
