package com.gratus.meditationtrakcer.utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class YearUtils {

    public static List<Integer> generatePastYears() {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        List<Integer> years = new ArrayList<>();
        for (int y = currentYear - 10; y <= currentYear; y++) {
            years.add(y);
        }
        return years;
    }
}
