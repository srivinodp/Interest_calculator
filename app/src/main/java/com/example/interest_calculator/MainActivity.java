package com.example.interest_calculator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.material.slider.Slider;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    EditText d1, d2, y1, y2, m1, m2;
    Slider sliderAmount, sliderInterest;
    TextView tvAmountValue, tvInterestValue;
    RadioGroup rad_group;
    TextView output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        output = findViewById(R.id.output);
        
        sliderAmount = findViewById(R.id.sliderAmount);
        sliderInterest = findViewById(R.id.sliderInterest);
        tvAmountValue = findViewById(R.id.tvAmountValue);
        tvInterestValue = findViewById(R.id.tvInterestValue);

        d1 = findViewById(R.id.d1);
        d2 = findViewById(R.id.d2);
        y1 = findViewById(R.id.y1);
        y2 = findViewById(R.id.y2);
        m1 = findViewById(R.id.m1);
        m2 = findViewById(R.id.m2);
        
        rad_group = findViewById(R.id.radioGroup);

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                calculateInterest();
            }
        };

        d1.addTextChangedListener(textWatcher);
        m1.addTextChangedListener(textWatcher);
        y1.addTextChangedListener(textWatcher);
        d2.addTextChangedListener(textWatcher);
        m2.addTextChangedListener(textWatcher);
        y2.addTextChangedListener(textWatcher);

        rad_group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                calculateInterest();
            }
        });

        // Setup Sliders change listeners
        sliderAmount.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                tvAmountValue.setText(String.format(Locale.getDefault(), "%.0f", value));
                calculateInterest();
            }
        });

        sliderInterest.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                tvInterestValue.setText(String.format(Locale.getDefault(), "%.2f", value));
                calculateInterest();
            }
        });
    }

    private void calculateInterest() {
        if (TextUtils.isEmpty(d1.getText()) || TextUtils.isEmpty(m1.getText()) || TextUtils.isEmpty(y1.getText()) ||
                    TextUtils.isEmpty(d2.getText()) || TextUtils.isEmpty(m2.getText()) || TextUtils.isEmpty(y2.getText())) {
                     output.setText("Missing dates!");
                     return;
                }

                //starting and ending dates
                int dd1 = Integer.parseInt(d1.getText().toString());
                int dd2 = Integer.parseInt(d2.getText().toString());
                int mm1 = Integer.parseInt(m1.getText().toString());
                int mm2 = Integer.parseInt(m2.getText().toString());
                int yy1 = Integer.parseInt(y1.getText().toString());
                int yy2 = Integer.parseInt(y2.getText().toString());
                
                int selectedId = rad_group.getCheckedRadioButtonId();
                if(selectedId==-1){
                    output.setText("Tirageta / No ???");
                    return;
                }
                
                if(yy1>yy2||yy1==yy2&&mm1>mm2||yy1==yy2&&mm1==mm2&&dd1>dd2){
                    output.setText("Incorrect dates???");
                    return;
                }
                
                RadioButton radioButton = (RadioButton)rad_group.findViewById(selectedId);
                int aamount = Math.round(sliderAmount.getValue());
                double intt_val = sliderInterest.getValue();
                
                int diff_d,diff_m,diff_y;
                if(dd1<=dd2) diff_d = dd2-dd1;
                else{
                   mm2-=1;diff_d = 30+dd2-dd1;
                }
                if(mm2>=mm1) diff_m = mm2-mm1;
                else{
                    yy2-=1; diff_m = 12+mm2-mm1;
                }
                diff_y = yy2-yy1;

                double total = 0;
                if (radioButton.getId() == R.id.Yes) {
                    if(diff_y<1){
                        total = 1.0*aamount*(1+intt_val*0.01*(12*diff_y+diff_m+diff_d*1.0/30));
                        output.setText("Plain  "+ String.valueOf(Math.round(total*100.0)/100.0));
                        return;
                    }
                    if(diff_m<6){
                        total = 1.0*aamount*Math.pow((1+(0.12*intt_val)),diff_y-1)*(1+intt_val*0.01*(12+diff_m+diff_d*1.0/30));
                        if(diff_y<2) output.setText("Plain  "+String.valueOf(Math.round(total*100.0)/100.0));
                        else output.setText("Tirageta  "+String.valueOf(Math.round(total*100.0)/100.0));
                        return;
                    }
                    total = 1.0*aamount*Math.pow((1+(0.12*intt_val)),diff_y)*(1+intt_val*0.01*(diff_m+diff_d*1.0/30));
                    output.setText("Tirageta  "+ String.valueOf(Math.round(total*100.0)/100.0));
                } else if (radioButton.getId() == R.id.No) {
                    total = 1.0*aamount*(1+intt_val*0.01*(12*diff_y+diff_m+diff_d*1.0/30));
                    output.setText("Plain  "+ String.valueOf(Math.round(total*100.0)/100.0));
                }
    }
}