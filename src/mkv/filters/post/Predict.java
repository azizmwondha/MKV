/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mkv.filters.post;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import mkv.types.MKI;
import mkv.types.MKR;
import mkv.types.MKV;
import mkv.types.State;
import mkv.types.PostChainFilter;

/**
 *
 * @author aziz
 */
public class Predict
        implements PostChainFilter {

    private MKR mkr = null;

    // Transition matrix visualisation:
    // http://setosa.io/markov/playground.html
    @Override
    public void apply(MKV m, HashMap<String, String> options, OutputStream o) {
        
                int max;

        try {
            max = Integer.parseInt(options.get(MKI.FilterKeys.MAX_TOKENS.key()) + "");
        } catch (NumberFormatException nfe) {
            max = 31;
        }

        final int maxTokens = max;
        
        HashMap<String, State> h = m.states();

        Iterator<State> allStates = h.values().iterator();

        State[] states = new State[h.size()];
        int x = 0;
        while (allStates.hasNext()) {
            states[x++] = allStates.next();
        }

        double[][] t = new double[h.size()][h.size()];
        int ic = -1;
        int jc = 0;

        for (State from : states) {
            ic++;
            double sums = 0d;
            if (from.outCount() == 0) {
                t[ic][ic] = 1.0d;
            } else {
                jc = 0;
                for (State to : states) {
                    if (from.hasNext(to)) {
                        t[ic][jc] = from.weight(to);
                        sums += t[ic][jc];

                        if (sums > 1.0d) {
//                            System.out.println("ofenda= from:" + from.state() + " to:" + to.state());
                        }
                    } else {
                        t[ic][jc] = 0.0d;
                    }
                    jc++;
                }

                if (sums < 1.0d) {
                    t[ic][jc - 1] += (1.0d - sums);
                }
            }
        }

        d(t);

        mkr = new MKR() {
            @Override
            public double[][] matrix() {
                return t; //To change body of generated methods, choose Tools | Templates.
            }
        };
    }

    public MKR result() {
        return mkr;
    }

    private void d(double[][] t) {
        System.out.println("Transition matrix");
        System.out.println("-----------------");
        System.out.println("[");
        for (int x = 0; x < t.length; x++) {
            System.out.print("[");
            for (int y = 0; y < t.length; y++) {
                System.out.print(t[x][y] + (((y + 1) < t.length) ? "," : ""));
            }
            System.out.println("]" + (((x + 1) < t.length) ? "," : ""));
        }
        System.out.println("]");
        System.out.println("Size: " + t.length + " x " + t.length);
    }
}
