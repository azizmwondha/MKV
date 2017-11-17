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

    private HashMap<String, State> h;
    private List<State> o;
    private final List<Sequence> sequences;
    protected int order;
    private boolean isOrigin;

    public MKV()
    {
        this.h = new HashMap<>();
        this.o = new ArrayList<>();
        this.sequences = new ArrayList<>();
        this.order = 1;
        this.isOrigin = true;
    }

    public void clear()
    {
        h.clear();
        o.clear();

        h = null;
        o = null;

        h = new HashMap<>();
        o = new ArrayList<>();
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
     * Build MArkov chain for the data supplied by the InputStream. Implementing
     * classes must parse the input and tokenise it into single state tokens
     * that make up the finite and discrete states of the chain.
     *
     * @param input
     * @throws IOException
     */
    public abstract void scan(InputStream input)
            throws IOException;

    /**
     * Build the Markov chain, one token at a time
     *
     * @param data
     */
    protected final void scan(Sequence data)
    {
        String stringValue = data.asString();
        boolean isEndOfLine = false;

        if (data instanceof StringSequence)
        {
            stringValue = stringValue.trim();
            
            // Period "." is predefined EOL token
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
                    // A lone standing period
                    isPunctuation = true;
                }
                isEndOfLine = true;
            }
            if (stringValue.isEmpty() || isPunctuation)
            {
                return;
            }
        }

        String prevStateKey = "";
        String thisStateKey = "";

        // Add incoming state to current sequence
        sequences.add(data);

        // Calculate new reference frame for the current chain
        int thisKeyStart = (sequences.size() <= order) ? 0 : (sequences.size() - order);
        int prevKeyStart = thisKeyStart - 1;
        int thisKeyEnd = (sequences.size() - 1);
        int prevKeyEnd = (thisKeyEnd - 1);

        if ((prevKeyStart < 0) && (thisKeyStart > 0))
        {
            prevKeyStart = 0;
        }

        for (int x = 0; x <= thisKeyEnd; x++)
        {
            if (x >= thisKeyStart)
            {
                thisStateKey += sequences.get(x).asString();
            }
            if ((x >= prevKeyStart) && (x <= prevKeyEnd))
            {
                prevStateKey += sequences.get(x).asString();
            }
        }

        State state = h.get(thisStateKey);
        if (null == state)
        {
            state = new State(data, merge(sequences, prevKeyStart));
            h.put(thisStateKey, state);
        }

        if (h.containsKey(prevStateKey))
        {
            State prevState = h.get(prevStateKey);
            state.previous(prevState);
            prevState.next(state);
        }
        else
        {
            if ((prevKeyStart >= order) && (thisKeyStart >= order))
            {
                State prevState = new State(sequences.get(thisKeyStart - 1), merge(sequences, prevKeyStart));
                h.put(prevStateKey, prevState);
            }
        }

        if (isOrigin)
        {
            if (!o.contains(state))
            {
                o.add(state);
            }
            isOrigin = false;
        }

        if (sequences.size() > (order + 1))
        {
            sequences.remove(0);
        }
        if (isEndOfLine)
        {
            sequences.clear();
            isOrigin = true;
        }
    }

    private synchronized Sequence merge(List<Sequence> prequences,
                                        int startIndex)
    {
        boolean isString = false;
        if (!prequences.isEmpty())
        {
            isString = (prequences.get(0) instanceof StringSequence);
        }
        List<Integer> s = new ArrayList<>();
        if (startIndex >= 0)
        {
            int endIndex = startIndex + order;
            if (endIndex >= prequences.size())
            {
                endIndex = prequences.size();
            }
            
            for (int i = startIndex; i < endIndex; i++)
            {
                Sequence seq = prequences.get(i);
                for (byte b : seq.data())
                {
                    s.add((int) b);
                }
                if (seq instanceof StringSequence)
                {
                    s.add(0x20);
                }
            }
        }
        if (!s.isEmpty())
        {
            byte[] ba = new byte[s.size() - (isString ? 1 : 0)];
            for (int i = 0; i < ba.length; i++)
            {
                ba[i] = s.get(i).byteValue();
            }
            return new ByteSequence(ba);
        }
        else
        {
            return null;
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
