package com.gratus.meditationtrakcer.utils;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.data.BarEntry;
import java.text.DecimalFormat;

public class HideZeroValueFormatter extends ValueFormatter {
    private final DecimalFormat df = new DecimalFormat("0.##");   // 0.63 → "0.63"

    @Override                    // label that sits above each bar
    public String getBarLabel(BarEntry e) {
        return e.getY() == 0f ? "" : df.format(e.getY());
    }

    @Override                    // fallback used by MPAndroidChart in some cases
    public String getFormattedValue(float v) {
        return v == 0f ? "" : df.format(v);
    }
}
