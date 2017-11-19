/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mkv.filters.post;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import mkv.types.MKR;
import mkv.types.MKV;
import mkv.types.State;
import mkv.types.PostChainFilter;
import mkv.types.exceptions.MKE;

/**
 *
 * @author aziz
 */
public class TransitionMatrix
        extends PostChainFilter
{

    private MKR mkr = null;

    // Transition matrix visualisation:
    // http://setosa.io/markov/playground.html
    @Override
    public void apply(MKV m,
                      MKR r,
                      HashMap<String, String> options,
                      OutputStream o)
            throws MKE
    {
        HashMap<String, State> h = m.states();

        Iterator<State> allStates = h.values().iterator();

        State[] states = new State[h.size()];
        int x = 0;
        p("Initial state");
        p("-------------");
        System.out.print("[");
        while (allStates.hasNext())
        {
            states[x] = allStates.next();
            System.out.print(" " + states[x].state().asString() + ((x < states.length) ? "," : ""));
            x++;
        }
        p(" ]\n");

        double[][] t = new double[h.size()][h.size()];
        int ty = -1;
        int tx;

        for (State from : states)
        {
            ty++;
            double sums = 0d;
            if (from.outCount() == 0)
            {
                // Absorbibg state
                t[ty][ty] = 1.0d;
            }
            else
            {
                tx = 0;
                for (State to : states)
                {
                    if (from.hasNext(to))
                    {
                        t[ty][tx] = from.weight(to);
                        sums += t[ty][tx];
                    }
                    else
                    {
                        t[ty][tx] = 0.0d;
                    }
                    tx++;
                }

                if (sums < 1.0d)
                {
                    t[ty][tx - 1] += (1.0d - sums);
                }
            }
        }

        d(t);

        mkr = new MKR()
        {
            @Override
            public double[][] matrix()
            {
                return t;
            }
        };
    }

    @Override
    public MKR result()
    {
        return mkr;
    }

    private void d(double[][] t)
    {
        p("Transition matrix");
        p("-----------------");
        if (t.length > 0)
        {
            p("[");
            for (int y = 0; y < t.length; y++)
            {
                System.out.print("[");
                for (int x = 0; x < t[0].length; x++)
                {
                    System.out.print(" " + t[y][x] + (((x + 1) < t[0].length) ? "," : ""));
                }
                p(" ]" + (((y + 1) < t.length) ? "," : ""));
            }
            p("]");
            p("Size: " + t.length + " x " + t[0].length + "\n");
        }
        else
        {
            p("[]");
        }
    }
}
