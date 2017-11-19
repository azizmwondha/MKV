/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mkv.filters.post;

import java.io.OutputStream;
import java.util.HashMap;
import mkv.types.MKI;
import mkv.types.MKR;
import mkv.types.MKV;
import mkv.types.PostChainFilter;
import mkv.types.exceptions.InvalidInput;
import mkv.types.exceptions.MKE;

/**
 *
 * @author aziz
 */
public class Predict
        extends PostChainFilter
{

    private MKR mkr = null;

    @Override
    public void apply(MKV m,
                      MKR r,
                      HashMap<String, String> options,
                      OutputStream o)
            throws MKE
    {

        int time;

        try
        {
            time = Integer.parseInt(options.get(MKI.FilterKeys.TIME.key()) + "");
        }
        catch (NumberFormatException nfe)
        {
            time = 0;
            throw new InvalidInput("Invalid time(" + options.get(MKI.FilterKeys.TIME.key()) + "), expected integer");
        }

        String startState = options.get(MKI.FilterKeys.STARTVECTOR.key());

        double[][] product = r.matrix();

        boolean hasStartVector = ((null != startState) && (!startState.trim().isEmpty()));

        if (hasStartVector)
        {
            String startStates[] = startState.split("[,]", 0);

            double[][] startVector = new double[1][startStates.length];
            for (int x = 0; x < startStates.length; x++)
            {
                try
                {
                    startVector[0][x] = Double.parseDouble(startStates[x]);
                }
                catch (NumberFormatException nfe)
                {
                    throw new InvalidInput("Invalid starting vector (" + startStates[x] + " in " + startState + ")");
                }
            }
            product = startVector;
            d(startVector, "Starting vector");
        }

        for (int power = 1; power <= time; power++)
        {
            product = product(product, r.matrix());
        }
        d(product, "Predict matrix (time=" + time + ")");

        final double[][] prediction = product;

        mkr = new MKR()
        {
            @Override
            public double[][] matrix()
            {
                return prediction;
            }
        };
    }

    /**
     * Multiply matrices s and m.
     *
     * @param s
     * @param m
     * @return
     */
    private double[][] product(double[][] s,
                               double[][] m)
            throws MKE
    {
        double[][] p = new double[s.length][m[0].length];

        if (s[0].length != m.length)
        {
            throw new InvalidInput("Start vector length (" + s[0].length + ") not compatible with transition matrix (" + m.length + ")");
        }
        for (int sy = 0; sy < s.length; sy++)
        {
            for (int sx = 0; sx < s[0].length; sx++)
            {
                double pd = p[sy][sx];
                for (int my = 0; my < m.length; my++)
                {
                    pd += s[sy][my] * m[my][sx];
                }
                p[sy][sx] = pd;
            }
        }
        return p;
    }

    @Override
    public MKR result()
    {

        return mkr;
    }

    private void d(double[][] t,
                   String s)
    {
        p(s);
        p("-----------------");
        p("[");

        for (int y = 0; y < t.length; y++)
        {
            System.out.print("[");
            for (int x = 0; x < t[0].length; x++)
            {
                System.out.print(" " + t[y][x] + (((x + 1) < t[0].length) ? "," : " "));
            }
            p("]" + (((y + 1) < t.length) ? "," : ""));
        }
        p("]");
        p("Size: " + t[0].length + " x " + t.length + "\n");
    }
}
