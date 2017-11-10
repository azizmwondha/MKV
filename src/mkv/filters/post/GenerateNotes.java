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
import mkv.types.MKV;
import mkv.types.State;
import mkv.types.PostChainFilter;

/**
 *
 * @author aziz
 */
public class GenerateNotes
        implements PostChainFilter {

    private final Random r = new Random(System.currentTimeMillis());

    @Override
    public void apply(MKV m, OutputStream o) {
        m.origins().forEach((s)
                -> {
            compose(s, o);
        });
    }

    private void compose(State s, OutputStream o) {
        try {
            StringBuilder sb = new StringBuilder();
            int count = 0;
            while (count < 1024) {
                if (null != o) {
                    for (byte b : s.sequence().data()) {
                        o.write(b);
                        count++;
                    }
                }

                if (s.outCount() == 0) {
                    break;
                }
                HashMap<State, Integer> next = s.next();
                int which = random(next.size());
                State[] ns = next.keySet().toArray(new State[0]);
                s = ns[which];

            }
            System.out.println("Output, " + count + "bytes\n");
        }  catch (IOException ex) {
            Logger.getLogger(GenerateNotes.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private int random(int oneOf) {
        return r.nextInt(oneOf);
    }

    private void d(double[][] t) {
    }
}
