package com.example.customwheelview;

import android.graphics.Color;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.customwheelview.widget.CustomSlideTabView;

import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SlipTabActivity extends AppCompatActivity {

    private CustomSlideTabView mSlideTapeView;
    private TextView mTextView;

    private static final int INDICATOR_COLOR = Color.rgb(77, 166, 104);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slip_tab);

        mTextView = (TextView) findViewById(R.id.text);
        mTextView.setTextColor(INDICATOR_COLOR);

        mTextView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });

        mSlideTapeView = (CustomSlideTabView) findViewById(R.id.slideTape);
        mSlideTapeView.setValue(30, 150);
        mSlideTapeView.setLongUnix(10);
        mSlideTapeView.setShortPointCount(10);
        mSlideTapeView.setmCallBack(new CustomSlideTabView.CallBack() {
            @Override
            public void onSlide(float current) {
                mTextView.setText(String.format(Locale.getDefault(),"%.1f",current));
            }
        });

    }
}
