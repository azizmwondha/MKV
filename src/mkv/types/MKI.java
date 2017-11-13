/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mkv.types;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author amwon
 */
public interface MKI {

    public void scan(InputStream input);

    public HashMap<String, State> states();

    public List<State> origins();

    public void d();

    public enum FilterKeys {
        FILTER_NAME("filter-name"),
        MAX_TOKENS("max-tokens"),
        TIME("time");

        final String key;

        private FilterKeys(String k) {
            key = k;
        }
        
        public String key(){
            return key;
        }
    }

}
