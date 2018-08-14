package com.example.customwheelview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;

import com.example.customwheelview.widget.CustomWheelView;

import java.util.ArrayList;
import java.util.List;

public class Main2Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        final CustomWheelView wheelView=findViewById(R.id.wheelview);
        wheelView.setTextSize(80);
        wheelView.setVisibilityCount(7);
        wheelView.setTextGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        wheelView.setSelectedTextColor(R.color.colorAccent);
        final List<String> dataSources = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            dataSources.add("数据" + i);
        }
        wheelView.setDataSources(dataSources);
        //wheelView.setSelectPosition(2);

        wheelView.setmCallBack(new CustomWheelView.CallBack() {
            @Override
            public void onPositionSelect(int position) {
                //wheelView.isEqual(dataSources);
            }
        });
    }
}
