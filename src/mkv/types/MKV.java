/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mkv.types;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author amwon
 */
public abstract class MKV
{

    private HashMap<String, State> h = new HashMap<>();
    private List<State> o = new ArrayList<>();
    private List<State> history = new ArrayList<>();
    private final List<Sequence> sequences = new ArrayList<>();
    protected int order = 1;

    public void clear()
    {
        h.clear();
        o.clear();
        history.clear();
        
        h=null;
        o=null;
        history=null;
        
        h = new HashMap<>();
        o = new ArrayList<>();
        history = new ArrayList<>();
    }

    /**
     * 
     * @param order 
     */
    public void order(int order)
    {
        if (order > 0)
        {
            this.order = order;
        }
    }

    public int order()
    {
        return order;
    }

    /**
     * Build MArkov chain for the data supplied 
     * by the InputStream.
     * Implementing classes must parse the input 
     * and tokenise it into single state tokens
     * that make up the finite and discrete states
     * of the chain.
     * 
     * @param input
     * @throws IOException 
     */
    public abstract void scan(InputStream input)
            throws IOException;

    /**
     * Build the Markov chain, one token at a time
     * @param data 
     */
    protected final void scan(Sequence data)
    {
        String stringValue = data.asString();
        boolean isString = (data instanceof StringSequence);
        boolean isEndOfLine = false;

        if (isString)
        {
            stringValue = stringValue.trim();
            boolean hasPeriod = stringValue.endsWith(".");
            boolean isPunctuation = false;
            if (hasPeriod)
            {
                if (stringValue.lastIndexOf(".") > 0)
                {
                    stringValue = stringValue.substring(0, stringValue.lastIndexOf("."));
                    data = new StringSequence(stringValue);
                }
                else
                {
                    isPunctuation = true;
                }
                isEndOfLine = true;
            }
            if (stringValue.isEmpty() || isPunctuation)
            {
                return;
            }
        }

        String key = "";

        sequences.add(data);

        for (int x = (sequences.size() < order) ? 0 : (sequences.size() - order); x < sequences.size(); x++)
        {
            key += sequences.get(x).asString();
        }

        State state = h.get(key);
        if (null == state)
        {
            state = new State(sequences);
        }

        if (!history.isEmpty())
        {
            // We have a previous state
            State previous = history.get(0);
            state.previous(previous);
            previous.next(state);
        }
        else
        {
            if (!o.contains(state))
            {
                o.add(state);
            }
        }
        h.put(key, state);
        history.add(0, state);
        if (sequences.size() >= order)
        {
            sequences.remove(0);
        }
        if (history.size() > order)
        {
            history.remove(order);
        }
        if (isEndOfLine)
        {
            history.clear();
            sequences.clear();
        }
    }

    public HashMap<String, State> states()
    {
        return h;
    }

    public List<State> origins()
    {
        return o;
    }

    public void d()
    {
        System.out.println("origins:");
        o.forEach((state) ->
        {
            System.out.println(state.toString());
        });
        System.out.println("states:");
        h.values().
                forEach((state) ->
                {
                    System.out.println(state.toString());
                });
        System.out.flush();
        System.out.println("Origin count=" + o.size());
        System.out.println("State  count=" + h.size());
    }
}
