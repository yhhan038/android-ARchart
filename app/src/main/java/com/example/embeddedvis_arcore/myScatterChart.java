package com.example.embeddedvis_arcore;

import android.content.Context;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;

public class myScatterChart extends ScatterChart implements SeekBar.OnSeekBarChangeListener,
        OnChartValueSelectedListener {

    private ScatterChart chart;
    private SeekBar seekBarX, seekBarY;
    private TextView tvX, tvY;
    private Context mContext;

    public myScatterChart(Context context) {
        super(context);
        mContext = context;
    }

    public void onCreateView(Context context, RelativeLayout layout) {

        tvX = layout.findViewById(R.id.tvXMax);
        tvY = layout.findViewById(R.id.tvYMax);

        seekBarX = layout.findViewById(R.id.seekBar1);
        seekBarX.setOnSeekBarChangeListener(this);

        seekBarY = layout.findViewById(R.id.seekBar2);
        seekBarY.setOnSeekBarChangeListener(this);

        chart = layout.findViewById(R.id.chart1);
        chart.getDescription().setEnabled(false);
        chart.setOnChartValueSelectedListener(this);

        chart.setDrawGridBackground(false);
        chart.setTouchEnabled(true);
        chart.setMaxHighlightDistance(50f);

        // enable scaling and dragging
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);

        chart.setMaxVisibleValueCount(200);
        chart.setPinchZoom(true);

        seekBarX.setProgress(45);
        seekBarY.setProgress(100);

        Legend l = chart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        l.setOrientation(Legend.LegendOrientation.VERTICAL);
        l.setDrawInside(false);
        //l.setTypeface(tfLight);
        l.setXOffset(5f);

        YAxis yl = chart.getAxisLeft();
        //yl.setTypeface(tfLight);
        yl.setAxisMinimum(0f); // this replaces setStartAtZero(true)

        chart.getAxisRight().setEnabled(false);

        XAxis xl = chart.getXAxis();
        //xl.setTypeface(tfLight);
        xl.setDrawGridLines(false);

    }


    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        tvX.setText(String.valueOf(seekBarX.getProgress()));
        tvY.setText(String.valueOf(seekBarY.getProgress()));

        ArrayList<Entry> values1 = new ArrayList<>();
        ArrayList<Entry> values2 = new ArrayList<>();
        ArrayList<Entry> values3 = new ArrayList<>();

        for (int i = 0; i < seekBarX.getProgress(); i++) {
            float val = (float) (Math.random() * seekBarY.getProgress()) + 3;
            values1.add(new Entry(i, val));
        }

        for (int i = 0; i < seekBarX.getProgress(); i++) {
            float val = (float) (Math.random() * seekBarY.getProgress()) + 3;
            values2.add(new Entry(i+0.33f, val));
        }

        for (int i = 0; i < seekBarX.getProgress(); i++) {
            float val = (float) (Math.random() * seekBarY.getProgress()) + 3;
            values3.add(new Entry(i+0.66f, val));
        }

        // create a dataset and give it a type
        ScatterDataSet set1 = new ScatterDataSet(values1, "DS 1");
        set1.setScatterShape(ScatterChart.ScatterShape.SQUARE);
        set1.setColor(ColorTemplate.COLORFUL_COLORS[0]);
        ScatterDataSet set2 = new ScatterDataSet(values2, "DS 2");
        set2.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        set2.setScatterShapeHoleColor(ColorTemplate.COLORFUL_COLORS[3]);
        set2.setScatterShapeHoleRadius(3f);
        set2.setColor(ColorTemplate.COLORFUL_COLORS[1]);
        ScatterDataSet set3 = new ScatterDataSet(values3, "DS 3");
        set3.setShapeRenderer(new CustomScatterShapeRenderer());
        set3.setColor(ColorTemplate.COLORFUL_COLORS[2]);

        set1.setScatterShapeSize(8f);
        set2.setScatterShapeSize(8f);
        set3.setScatterShapeSize(8f);

        ArrayList<IScatterDataSet> dataSets = new ArrayList<>();
        dataSets.add(set1); // add the data sets
        dataSets.add(set2);
        dataSets.add(set3);

        // create a data object with the data sets
        ScatterData data = new ScatterData(dataSets);
        //data.setValueTypeface(tfLight);

        chart.setData(data);
        chart.invalidate();
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        Log.i("VAL SELECTED",
                "Value: " + e.getY() + ", xIndex: " + e.getX()
                        + ", DataSet index: " + h.getDataSetIndex());
    }

    @Override
    public void onNothingSelected() {}

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}
}


