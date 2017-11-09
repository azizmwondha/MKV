/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mkv.types;

import java.io.OutputStream;

/**
 * Markov state filter
 * @author aziz
 */
public interface PostChainFilter {
    public void apply(MKV m, OutputStream o);
}
