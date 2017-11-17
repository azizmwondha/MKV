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

/**
 *
 * @author aziz
 */
public class Predict
        implements PostChainFilter
{

    private MKR mkr = null;

    // Transition matrix visualisation:
    // http://setosa.io/markov/playground.html
    @Override
    public void apply(MKV m,
                      MKR r,
                      HashMap<String, String> options,
                      OutputStream o)
    {

        int time;

        try
        {
            time = Integer.parseInt(options.get(MKI.FilterKeys.TIME.key()) + "");
        }
        catch (NumberFormatException nfe)
        {
            time = 1;
        }

        String startState = options.containsKey(MKI.FilterKeys.STARTVECTOR.key()) ? options.get(MKI.FilterKeys.STARTVECTOR.key()) : "";

        System.out.println("time=" + time);
        System.out.println("start=" + startState);
        
        String startStates[] = startState.split("[, ]");
        
        double[][] s = new double[1][startStates.length];
        for (int x = 0; x < startStates.length; x++){
            try
        {
            s[0][x] = Double.parseDouble(startStates[x]);
        }
        catch (NumberFormatException nfe)
        {
        }
        }
        d(s, "Starting vector");

        double[][] product = r.matrix();
        for (int power = 0; power < time; power++)
        {
            product = product(product, r.matrix());
        }
//        d(product);
        d(product(s, product), "Predict matrix");

        mkr = new MKR()
        {
            @Override
            public double[][] matrix()
            {
                return product(s, r.matrix()); //To change body of generated methods, choose Tools | Templates.
            }
        };
    }

    private double[][] product(double[][] s,
                               double[][] m)
    {
        double[][] p = new double[s.length][m[0].length];

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

    public MKR result()
    {

        return mkr;
    }

    private void d(double[][] t, String s)
    {
        System.out.println(s);
        System.out.println("-----------------");
        System.out.println("[");

        for (int y = 0; y < t.length; y++)
        {
            System.out.print("[");
            for (int x = 0; x < t[0].length; x++)
            {
                System.out.print(" " + t[y][x] + (((x + 1) < t[0].length) ? "," : " "));
            }
            System.out.println("]" + (((y + 1) < t.length) ? "," : ""));
        }
        System.out.println("]");
        System.out.println("Size: " + t[0].length + " x " + t.length + "\n");
    }
}
