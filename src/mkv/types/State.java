/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mkv.types;

import java.util.HashMap;

/**
 * Markov state
 *
 * @author aziz
 */
public class State
{

    private final Sequence state;
    private final Sequence sequence;

    private final HashMap<State, Integer> previous;
    private final HashMap<State, Integer> next;

    private double outWeight = 0.0f;

    public State(Sequence state, Sequence prequence)
    {
        this.sequence = prequence;
        this.state = state;
        previous = new HashMap<>();
        next = new HashMap<>();
    }
    
    public Sequence state()
    {
        return state;
    }

    public Sequence sequence()
    {
        return sequence;
    }

    public void previous(State s)
    {
        if (!previous.containsKey(s))
        {
            previous.put(s, 0);
        }
        previous.put(s, previous.get(s) + 1);
    }

    public void next(State s)
    {
        if (!next.containsKey(s))
        {
            next.put(s, 0);
        }
        next.put(s, next.get(s) + 1);
        outWeight++;
    }

    public HashMap<State, Integer> next()
    {
        return next;
    }

    public boolean hasPrevious(State s)
    {
        return previous.containsKey(s);
    }

    public boolean hasNext(State s)
    {
        return next.containsKey(s);
    }

    public double weight(State s)
    {
        if (hasNext(s))
        {
            return (next.get(s) / outWeight);
        }
        return 0.0f;
    }

    public int inCount()
    {
        return previous.size();
    }

    public int outCount()
    {
        return next.size();
    }

    @Override
    public boolean equals(Object o)
    {
        if ((null != o) && (o instanceof State))
        {
            return state.equals(((State) o).state());
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return state.hashCode();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
//        sb.append("[ \"").append(state).append("\" ]\n");
        sb.append("[ \"").append(state).append(" preq: ").append(sequence).append("\" ]\n");
        sb.append("\tin-count =").append(inCount()).append("\t");
        previous.keySet().forEach((s) ->
        {
            sb.append(" [\"").append(s.state()).append("\" (").append(previous.get(s)).append(")]");
        });

        sb.append("\n\tout-count=").append(outCount()).append("\t");
        next.keySet().forEach((s) ->
        {
            sb.append(" [\"").append(s.state()).append("\" (count=").append(next.get(s)).append(") (weight=").append(weight(s)).append(")]");
        });
        return sb.toString();
    }
}
