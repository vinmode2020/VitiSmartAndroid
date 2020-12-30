package com.example.vinmod;

import android.app.Activity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Toast;


public class SpinnerActivity extends Activity implements AdapterView.OnItemSelectedListener {

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Spinner spinner = (Spinner) findViewById(R.id.startMonthSpinner);
        Spinner spinner2 = (Spinner) findViewById(R.id.startYearSpinner);
        Spinner spinner3 = (Spinner) findViewById(R.id.endMonthSpinner);
        Spinner spinner4 = (Spinner) findViewById(R.id.endYearSpinner);

        spinner.setOnItemSelectedListener(this);
        spinner2.setOnItemSelectedListener(this);
        spinner3.setOnItemSelectedListener(this);
        spinner4.setOnItemSelectedListener(this);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
