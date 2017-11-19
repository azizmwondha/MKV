/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mkv.types;

import java.io.OutputStream;
import java.util.HashMap;
import mkv.types.exceptions.MKE;

/**
 * Markov state filter
 * @author aziz
 */
public abstract class PostChainFilter {
    public abstract void apply(MKV m, MKR r, HashMap<String, String> options, OutputStream o) throws MKE;
    public abstract MKR result();
    
    protected void p(String s)
    {
        System.out.println(s);
    }
}
