package com.gastonlesbegueris.caretemplate.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.gastonlesbegueris.caretemplate.R;
import com.gastonlesbegueris.caretemplate.data.local.AppDb;
import com.gastonlesbegueris.caretemplate.data.local.EventDao;
import com.gastonlesbegueris.caretemplate.data.model.MonthTotal;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;


import java.util.*;

public class ExpensesCharActivity extends AppCompatActivity {

    private BarChart chart;
    private EventDao dao;
    private String appType; // "pets" | "cars" | "family" | "house"

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expenses_chart);

        appType = getString(R.string.app_type);
        dao = AppDb.get(this).eventDao();
        chart = findViewById(R.id.chart);

        loadAndRender();
    }

    private void loadAndRender() {
        new Thread(() -> {
            // rango: año actual
            Calendar c = Calendar.getInstance();
            c.set(Calendar.DAY_OF_MONTH, 1);
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);

            // inicio del año
            c.set(Calendar.MONTH, Calendar.JANUARY);
            long from = c.getTimeInMillis();

            // fin del año
            c.set(Calendar.MONTH, Calendar.DECEMBER);
            c.set(Calendar.DAY_OF_MONTH, 31);
            c.set(Calendar.HOUR_OF_DAY, 23);
            c.set(Calendar.MINUTE, 59);
            c.set(Calendar.SECOND, 59);
            c.set(Calendar.MILLISECOND, 999);
            long to = c.getTimeInMillis();

            List<MonthTotal> rows = dao.listMonthlyTotals(appType, from, to);

            // mapa ym -> total
            Map<String, Double> map = new HashMap<>();
            if (rows != null) {
                for (MonthTotal mt : rows) {
                    if (mt != null && mt.ym != null) {
                        map.put(mt.ym, mt.total == null ? 0d : mt.total);
                    }
                }
            }

            // generar 12 barras (ene..dic) aunque no haya datos
            List<BarEntry> entries = new ArrayList<>();
            final String[] labels = new String[]{"Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic"};

            Calendar mcal = Calendar.getInstance();
            mcal.setTimeInMillis(from);
            mcal.set(Calendar.MONTH, Calendar.JANUARY);

            int year = mcal.get(Calendar.YEAR);
            for (int i = 0; i < 12; i++) {
                String ym = String.format(Locale.US, "%04d-%02d", year, i+1);
                double val = map.getOrDefault(ym, 0d);
                entries.add(new BarEntry(i, (float) val));
            }

            runOnUiThread(() -> render(entries, labels));
        }).start();
    }

    private void render(List<BarEntry> entries, String[] labels) {
        BarDataSet dataSet = new BarDataSet(entries, "Gastos por mes");
        dataSet.setValueTextSize(10f);
        // 🟢 Colores por día
        List<Integer> colors = new ArrayList<>();
        for (DaySummary d : summaries) {
            if (d.mainColorHex != null) {
                try {
                    colors.add(android.graphics.Color.parseColor(d.mainColorHex));
                } catch(Exception ignore) {
                    colors.add(android.graphics.Color.LTGRAY);
                }
            } else {
                colors.add(android.graphics.Color.LTGRAY);
            }
        }

        set.setColors(colors);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);

        chart.setData(data);

        XAxis x = chart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setGranularity(1f);
        x.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int i = Math.round(value);
                return (i >= 0 && i < labels.length) ? labels[i] : "";
            }
        });


        chart.getAxisRight().setEnabled(false);

        Description d = new Description();
        d.setText(""); // sin descripción
        chart.setDescription(d);

        chart.getLegend().setEnabled(false);
        chart.setFitBars(true);
        chart.animateY(600);
        chart.invalidate();
    }
}
