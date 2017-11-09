/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mkv.types;

import java.util.ArrayList;
import java.util.List;

/**
 * Markov state group
 *
 * @author aziz
 */
public abstract class MSG_orig
{

    private final String[] stateItems;
    private final String state;

    private final List<State> previous;
    private final List<State> next;

    public MSG_orig(String... state)
    {
        this.stateItems = state;
        StringBuilder sb = new StringBuilder();
        for (String s : stateItems)
        {
            sb.append(s).append(" ");
        }
        this.state = sb.toString().trim();
        previous = new ArrayList<>();
        next = new ArrayList<>();
    }

    public String state()
    {
        return state;
    }

    public void previous(State s)
    {
        if (!previous.contains(s))
        {
            previous.add(s);
        }
    }

    public void next(State s)
    {
        if (!next.contains(s))
        {
            next.add(s);
        }
    }

    public boolean hasPrevious(State s)
    {
        return previous.contains(s);
    }

    public boolean hasNext(State s)
    {
        return next.contains(s);
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
        if ((null != o) && (o instanceof MSG_orig))
        {
            return state.equalsIgnoreCase(((MSG_orig) o).state());
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return state.hashCode();
    }

//    @Override
//    public String toString() {
//        return "(" + inCount() + ")" + state + "(" + outCount() + ")";
//    }
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ").append(state + " ]\n");
        sb.append("\tprev=").append(inCount());
        for (State s : previous)
        {
            sb.append(" [").append(s.state()).append("]");
        }
        
        sb.append("\n\tnext=").append(outCount());
        for (State s : next)
        {
            sb.append(" [").append(s.state()).append("]");
        }
        return sb.toString();
    }
}
