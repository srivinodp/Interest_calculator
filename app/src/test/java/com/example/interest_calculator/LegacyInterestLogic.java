package com.example.interest_calculator;

/**
 * Verbatim copy of v1's {@code MainActivity.calculateInterest()} (Java, commit on master),
 * used as the oracle in {@code InterestCalculatorParityTest}.
 *
 * The only edits: view reads are replaced by parameters, {@code output.setText(x); return;}
 * becomes {@code return x;}, and the radio button id is passed in. Every expression is untouched.
 */
final class LegacyInterestLogic {
    static final int NONE = -1;
    static final int YES = 1; // R.id.Yes  -> "Tirageta"
    static final int NO = 2;  // R.id.No   -> "Plain"

    private LegacyInterestLogic() {}

    static String calculateInterest(
            String d1, String m1, String y1,
            String d2, String m2, String y2,
            float sliderAmountValue, float sliderInterestValue,
            int selectedId) {
        if (d1.isEmpty() || m1.isEmpty() || y1.isEmpty() ||
                    d2.isEmpty() || m2.isEmpty() || y2.isEmpty()) {
                     return "Missing dates!";
                }

                //starting and ending dates
                int dd1 = Integer.parseInt(d1);
                int dd2 = Integer.parseInt(d2);
                int mm1 = Integer.parseInt(m1);
                int mm2 = Integer.parseInt(m2);
                int yy1 = Integer.parseInt(y1);
                int yy2 = Integer.parseInt(y2);

                if(selectedId==-1){
                    return "Tirageta / No ???";
                }

                if(yy1>yy2||yy1==yy2&&mm1>mm2||yy1==yy2&&mm1==mm2&&dd1>dd2){
                    return "Incorrect dates???";
                }

                int aamount = Math.round(sliderAmountValue);
                double intt_val = sliderInterestValue;

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
                if (selectedId == YES) {
                    if(diff_y<1){
                        total = 1.0*aamount*(1+intt_val*0.01*(12*diff_y+diff_m+diff_d*1.0/30));
                        return "Plain  "+ String.valueOf(Math.round(total*100.0)/100.0);
                    }
                    if(diff_m<6){
                        total = 1.0*aamount*Math.pow((1+(0.12*intt_val)),diff_y-1)*(1+intt_val*0.01*(12+diff_m+diff_d*1.0/30));
                        if(diff_y<2) return "Plain  "+String.valueOf(Math.round(total*100.0)/100.0);
                        else return "Tirageta  "+String.valueOf(Math.round(total*100.0)/100.0);
                    }
                    total = 1.0*aamount*Math.pow((1+(0.12*intt_val)),diff_y)*(1+intt_val*0.01*(diff_m+diff_d*1.0/30));
                    return "Tirageta  "+ String.valueOf(Math.round(total*100.0)/100.0);
                } else if (selectedId == NO) {
                    total = 1.0*aamount*(1+intt_val*0.01*(12*diff_y+diff_m+diff_d*1.0/30));
                    return "Plain  "+ String.valueOf(Math.round(total*100.0)/100.0);
                }
                throw new IllegalStateException("unreachable");
    }
}
