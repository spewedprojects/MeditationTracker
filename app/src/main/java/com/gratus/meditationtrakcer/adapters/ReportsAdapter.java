package com.gratus.meditationtrakcer.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gratus.meditationtrakcer.R;
import com.gratus.meditationtrakcer.models.MeditationReportData;
import com.gratus.meditationtrakcer.utils.ReportJsonHelper;

import java.util.List;
import java.util.Locale;

public class ReportsAdapter extends RecyclerView.Adapter<ReportsAdapter.ReportViewHolder> {

    public interface OnReportClickListener {
        void onReportClick(MeditationReportData data, View sharedView);
    }

    private Context context;
    private List<MeditationReportData> reports;
    private OnReportClickListener listener;

    private int spanCount = 1;

    public void setSpanCount(int spanCount) {
        this.spanCount = spanCount;
    }

    public ReportsAdapter(Context context, List<MeditationReportData> reports, OnReportClickListener listener) {
        this.context = context;
        this.reports = reports;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_minireport_card, parent, false);
        ViewGroup.LayoutParams layoutParams = v.getLayoutParams();
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT; // Set to wrap content SOLVES THE HUGE GAP EVERY
                                                                   // SINGLE TIME!
        v.setLayoutParams(layoutParams);

        // Catch all text inside the newly created list item

        return new ReportViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        MeditationReportData data = reports.get(position);

        holder.title.setText(data.title);
        holder.totalHours.setText(String.format(Locale.getDefault(), "%.1f", data.totalHours));
        holder.bestStreak.setText(String.valueOf(data.bestStreak));
        holder.avgSession.setText(String.valueOf(data.weightedAvgSessionLength));
        holder.consistency.setText(String.valueOf(data.consistencyScore));
        holder.daysWithout.setText(String.valueOf(data.daysNotMeditated));

        // Delete Logic
        holder.btnDelete.setOnClickListener(v -> {
            ReportJsonHelper.deleteReport(context, data.reportId);
            reports.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, reports.size());
        });

        // Click to Expand
        holder.itemView.setOnClickListener(v -> {
            listener.onReportClick(data, holder.cardView);
        });

        ViewGroup.MarginLayoutParams lp =
                (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();

        int outer = dpToPx(12);
        int inner = dpToPx(1);

        if (spanCount == 1) {
            lp.setMarginStart(outer);
            lp.setMarginEnd(outer);
        } else {
            if (position % spanCount == 0) {
                // Left column
                lp.setMarginStart(inner);
                lp.setMarginEnd(inner);
            } else {
                // Right column
                lp.setMarginStart(inner);
                lp.setMarginEnd(outer);
            }
        }

        holder.itemView.setLayoutParams(lp);
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    public static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView title, totalHours, bestStreak, avgSession, consistency, daysWithout;
        ImageButton btnDelete;
        View cardView;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView_reports_list);
            title = itemView.findViewById(R.id.report_title);
            totalHours = itemView.findViewById(R.id.tv_total_hours_value);
            bestStreak = itemView.findViewById(R.id.tv_best_streak_value);
            avgSession = itemView.findViewById(R.id.tv_avg_session_value);
            consistency = itemView.findViewById(R.id.tv_consistency_value);
            daysWithout = itemView.findViewById(R.id.tv_days_without_value);
            btnDelete = itemView.findViewById(R.id.delete_report_button);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }
}