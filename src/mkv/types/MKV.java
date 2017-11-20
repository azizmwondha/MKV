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
public abstract class MKV {

    private HashMap<String, State> states;
    private HashMap<String, State> controls;
    private List<State> origins;
    private final List<Sequence> sequences;
    protected int order;
    private boolean isOrigin;
    private String EOL;

    public MKV() {
        this.states = new HashMap<>();
        this.controls = new HashMap<>();
        this.origins = new ArrayList<>();
        this.sequences = new ArrayList<>();
        this.order = 1;
        this.isOrigin = true;

        initControlStates();

    }

    private void initControlStates() {
        byte[] control = eolState();

        if (null != control) {
            EOL = new String(control);
            State eol = new State(new ByteSequence(control), null);
            controls.put(EOL, eol);
        } else {
            EOL = "";
        }
    }

    /**
     * Build Markov chain for the data supplied by the InputStream. Implementing
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
    protected final void scan(Sequence data) {
        String stringValue = data.asString();
        boolean isEndOfLine = false;

        if (data instanceof StringSequence) {
            stringValue = stringValue.trim();

            // Detect end of line sequence
            boolean hasEOL = stringValue.endsWith(EOL);
            boolean isPunctuation = false;
            if (hasEOL) {
                if (stringValue.lastIndexOf(EOL) > 0) {
                    stringValue = stringValue.substring(0, stringValue.lastIndexOf(EOL));
                    data = new StringSequence(stringValue);
                } else {
                    // A lone standing period
                    isPunctuation = true;
                }
                isEndOfLine = true;
            }
            if (stringValue.isEmpty() || isPunctuation) {
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

        if ((prevKeyStart < 0) && (thisKeyStart > 0)) {
            prevKeyStart = 0;
        }

        for (int x = 0; x <= thisKeyEnd; x++) {
            if (x >= thisKeyStart) {
                thisStateKey += sequences.get(x).asString();
            }
            if ((x >= prevKeyStart) && (x <= prevKeyEnd)) {
                prevStateKey += sequences.get(x).asString();
            }
        }

        State state = getState(thisStateKey);
        if (null == state) {
            state = new State(data, merge(sequences, prevKeyStart));
            addState(thisStateKey, state);
        }

        if (hasState(prevStateKey)) {
            State prevState = getState(prevStateKey);
            state.previous(prevState);
            prevState.next(state);
        } else {
            if ((prevKeyStart >= order) && (thisKeyStart >= order)) {
                State prevState = new State(sequences.get(thisKeyStart - 1), merge(sequences, prevKeyStart));
                addState(prevStateKey, prevState);
            }
        }

        if (isEndOfLine) {
            State eol = controls.get(EOL);
            if (null == eol) {
                eol = new State(new StringSequence(EOL), merge(sequences, thisKeyStart));
                controls.put(EOL, eol);
            }
            eol.previous(state);
            sequences.clear();
            isOrigin = true;
        }

        if (sequences.size() > (order + 1)) {
            sequences.remove(0);
        }

        if (isOrigin) {
            if (!origins.contains(state)) {
                origins.add(state);
            }
            isOrigin = false;
        }
    }

    private void addState(String key, State state) {
        states.put(key, state);
    }

    private State getState(String key) {
        return states.get(key);
    }

    private boolean hasState(String key) {
        return states.containsKey(key);
    }

    private synchronized Sequence merge(List<Sequence> prequences,
            int startIndex) {
        boolean isString = false;
        if (!prequences.isEmpty()) {
            isString = (prequences.get(0) instanceof StringSequence);
        }
        List<Integer> s = new ArrayList<>();
        if (startIndex >= 0) {
            int endIndex = startIndex + order;
            if (endIndex >= prequences.size()) {
                endIndex = prequences.size();
            }

            for (int i = startIndex; i < endIndex; i++) {
                Sequence seq = prequences.get(i);
                for (byte b : seq.data()) {
                    s.add((int) b);
                }
                if (seq instanceof StringSequence) {
                    s.add(0x20);
                }
            }
        }
        if (!s.isEmpty()) {
            byte[] ba = new byte[s.size() - (isString ? 1 : 0)];
            for (int i = 0; i < ba.length; i++) {
                ba[i] = s.get(i).byteValue();
            }
            return new ByteSequence(ba);
        } else {
            return null;
        }
    }

    public HashMap<String, State> states() {
        return states;
    }

    public List<State> origins() {
        return origins;
    }

    protected abstract byte[] eolState();

//    protected abstract String eolKeys();
    public void clear() {
        states.clear();
        origins.clear();

        states = null;
        origins = null;

        states = new HashMap<>();
        origins = new ArrayList<>();
    }

    /**
     *
     * @param order
     */
    public void order(int order) {
        if (order > 0) {
            this.order = order;
        }
    }

    public int order() {
        return order;
    }

    public void d() {
        System.out.println("origins:");
        origins.forEach((state)
                -> {
            System.out.println(state.toString());
        });
        System.out.println("states:");
        states.values().
                forEach((state)
                        -> {
                    System.out.println(state.toString());
                });
        System.out.flush();
        System.out.println("Origin count=" + origins.size());
        System.out.println("State  count=" + states.size());
    }
}
