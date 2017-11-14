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

        int max;

        try
        {
            max = Integer.parseInt(options.get(MKI.FilterKeys.TIME.key()) + "");
        }
        catch (NumberFormatException nfe)
        {
            max = 31;
        }

        final int maxTokens = max;

        System.out.println("time=" + max);

//        double[][] s = new double[4][1];
        double[][] s =
        {
            {
                0d
            },
            {
                2d
            },
            {
                4d
            },
            {
                6d
            }
        };
        
   
        System.out.println("SxM");
        d(product(s, r.matrix()));

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
        System.out.println("PROD:");
        double[][] p = new double[m.length][m.length];;
        for (int mx = 0; mx < m.length; mx++)
        {
            for (int my = 0; my < m[0].length; my++)
            {
                p[mx][my] = 1.0d;
            }
        }
        
        
        System.out.println("S:");
        d(s);
        
        
        System.out.println("P:");
        d(p);

        
        System.out.println("M:");
        d(m);
        for (int sy = 0; sy < s[0].length; sy++)
        {
            for (int mx = 0; mx < m.length; mx++)
            {
                for (int sx = 0; sx < s.length; sx++)
        {
                System.out.print(mx+","+sy+" ");
                p[mx][sy] += s[sx][sy] * m[mx][sy];
            }}
            System.out.println("");
        }

        return p;
    }

    public MKR result()
    {

        return mkr;
    }

    private void d(double[][] t)
    {
        System.out.println("Predict matrix");
        System.out.println("-----------------");
        System.out.println("[");
        
            for (int y = 0; y < t[0].length; y++)
        {
            System.out.print("[");
            for (int x = 0; x < t.length; x++)
            {
                System.out.print(t[x][y] + (((x + 1) < t[0].length) ? "," : ""));
            }
            System.out.println("]" + (((y + 1) < t.length) ? "," : ""));
        }
        System.out.println("]");
        System.out.println("Size: " + t.length + " x " + t[0].length);
    }
}
