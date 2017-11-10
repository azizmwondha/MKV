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
public class GenerateTexts
        implements PostChainFilter
{

    private final Random r = new Random(System.currentTimeMillis());
    private OutputStream out;

    @Override
    public void apply(MKV m, OutputStream o)
    {
        m.origins().forEach((s) ->
        {
            try
            {
                compose(s, o);
            }
            catch (IOException ex)
            {
                Logger.getLogger(GenerateTexts.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
        });
    }

    private void compose(State s, OutputStream o) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        int maxTokens = 31;
        System.out.print("[ ");
        while (maxTokens > 0)
        {
            System.out.print(s.state() + " ");
            
            if (null != o)o.write(s.sequence().data());
            for (byte b : s.sequence().data()){
                sb.append(b).append(" ");
            }
            
            if (s.outCount() == 0)
            {
                break;
            }
            HashMap<State, Integer> next = s.next();
            int which = random(next.size());
            State[] ns = next.keySet().toArray(new State[0]);
            s = ns[which];
            maxTokens--;
        }
        System.out.println(" ]\n");
//        System.out.println(""+ sb.toString());
    }

    private int random(int oneOf)
    {
        return r.nextInt(oneOf);
    }

    private void d(double[][] t)
    {
    }
}
