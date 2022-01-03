package com.example.interest_calculator;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    EditText d1,d2,y1,y2,m1,m2,amount,int_val;
    RadioGroup rad_group;
    TextView output;
    Button btn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        output = findViewById(R.id.output);
        amount = findViewById(R.id.amount);
        int_val =findViewById(R.id.intrst_val);
        d1 = findViewById(R.id.d1);
        d2 = findViewById(R.id.d2);
        y1 = findViewById(R.id.y1);
        y2 = findViewById(R.id.y2);
        m1 = findViewById(R.id.m1);
        m2 = findViewById(R.id.m2);
        rad_group = findViewById(R.id.radioGroup);
        btn = findViewById(R.id.button);
        output = findViewById(R.id.output);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                }
                if(yy1>yy2||yy1==yy2&&mm1>mm2||yy1==yy2&&mm1==mm2&&dd1>dd2){
                    output.setText("Incorrect dates???");return;
                }
                RadioButton radioButton = (RadioButton)rad_group.findViewById(selectedId);
                int aamount = Integer.parseInt(amount.getText().toString());
                double intt_val = Double.parseDouble(int_val.getText().toString());
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
                switch (radioButton.getId()){
                    case R.id.Yes:
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
                        break;
                    case R.id.No:
                        total = 1.0*aamount*(1+intt_val*0.01*(12*diff_y+diff_m+diff_d*1.0/30));
                        output.setText("Plain  "+ String.valueOf(Math.round(total*100.0)/100.0));
                        break;
                }
            }
        });
    }
}