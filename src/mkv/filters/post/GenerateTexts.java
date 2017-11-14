/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mkv.filters.post;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import mkv.types.MKI;
import mkv.types.MKR;
import mkv.types.MKV;
import mkv.types.State;
import mkv.types.PostChainFilter;

/**
 *
 * @author aziz
 */
public class GenerateTexts
        implements PostChainFilter {

    private final Random r = new Random(System.currentTimeMillis());
//    HashMap<String, String> options = null;

    @Override
    public void apply(MKV m, MKR r, HashMap<String, String> options,
            OutputStream o) {

        int max;

        try {
            max = Integer.parseInt(options.get(MKI.FilterKeys.MAXTOKENS.key()) + "");
        } catch (NumberFormatException nfe) {
            max = 31;
        }

        final int maxTokens = max;
        m.origins().forEach((s)
                -> {
            try {
                compose(s, maxTokens, o);
            } catch (IOException ex) {
                Logger.getLogger(GenerateTexts.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
        });
    }

    private void compose(State s, final int maxTokens,
            OutputStream o)
            throws IOException {
        StringBuilder sb = new StringBuilder();
        System.out.print("[ ");
        for (int i = 0; i <maxTokens;i++) {
            System.out.print(s.state() + " ");

//            if (null != o)
//            {
//                o.write(s.state().data());
//            }
            if (s.outCount() == 0) {
                break;
            }
            HashMap<State, Integer> next = s.next();
            int which = random(next.size());
            State[] ns = next.keySet().toArray(new State[0]);
            s = ns[which];
        }
        System.out.println(" ]\n");
    }

    private int random(int oneOf) {
        return r.nextInt(oneOf);
    }

    public MKR result() {
        return null;
    }

    private void d(double[][] t) {
    }
}
