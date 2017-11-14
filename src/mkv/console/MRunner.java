/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mkv.console;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import mkv.filters.chains.MKV_byte;
import mkv.filters.chains.MKV_word;
import mkv.filters.pre.MidiTrackReader;
import mkv.filters.pre.URL2LocalFile;
import mkv.types.MKI;
import mkv.types.MKR;
import mkv.types.MKV;
import mkv.types.PostChainFilter;

/**
 *
 * @author amwon
 */
public class MRunner
{

    private MKV mkv;
    private final List<String> history;
    private final Map<String, String> postFilters;

    public MRunner()
    {
        this.history = new ArrayList<>();
        this.mkv = new MKV_word();
        this.postFilters = new HashMap<>();
        postFilters.put("matrix", "mkv.filters.post.TransitionMatrix");
        postFilters.put("texts", "mkv.filters.post.GenerateTexts");
        postFilters.put("notes", "mkv.filters.post.GenerateNotes");
        postFilters.put("predict", "mkv.filters.post.Predict");
    }

    public void run(String r,
                    OutputStream o)
    {
        String[] in = r.trim().split("\\s");
        if (in.length == 0)
        {
            return;
        }

        if (in[0].equalsIgnoreCase("parser"))
        {
            parser(in[1]);
            history.add(r);
        }

        if (in[0].equalsIgnoreCase("order"))
        {
            order(in[1]);
            history.add(r);
        }

        if (in[0].equalsIgnoreCase("scan"))
        {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < in.length; i++)
            {
                sb.append(in[i]).append(" ");
            }
            scan(sb.toString().trim());
            history.add(r);
        }

        if (in[0].equalsIgnoreCase("eval"))
        {
            eval(in, o);
            history.add(r);
        }

        if (in[0].equalsIgnoreCase("map"))
        {
            map();
            history.add(r);
        }

        if (in[0].equalsIgnoreCase("clear"))
        {
            mkv.clear();
        }

        if (in[0].equalsIgnoreCase("history"))
        {
            history.forEach((h) ->
            {
                System.out.println("> " + h);
            });
        }
        System.out.println("# ");

        if (history.size() > 17)
        {
            history.remove(0);
        }
    }

    private void parser(String s)
    {
        if (s.equalsIgnoreCase("word"))
        {
            mkv = new MKV_word();
        }
        else
        {
            mkv = new MKV_byte();
        }
        System.out.println("MKv_" + s + " loaded");
    }

    private void order(String s)
    {
        try
        {
            int o = Integer.parseInt(s);
            mkv.order(o);
            System.out.println("order set to " + mkv.order());
        }
        catch (NumberFormatException nfe)
        {
            System.out.println("Invalid input for order");
        }
    }

    private void scan(String s)
    {
        System.out.println("scan -> " + s);

        InputStream is = null;
        if (s.trim().toLowerCase().startsWith("http"))
        {
            URL2LocalFile urL2LocalFile = new URL2LocalFile();
            is = urL2LocalFile.scan(url(s));
        }
        else if (new File(s).exists())
        {
            is = file(s);
        }

        if ((null != is) && ((s.endsWith(".mid") || s.endsWith(".midi"))))
        {
            MidiTrackReader mtr = new MidiTrackReader();
            is = mtr.scan(is);
        }

        if (null == is)
        {
            is = new ByteArrayInputStream(s.getBytes());
        }
        try
        {
            if (null != is)
            {
                mkv.scan(is);
            }
            else
            {
                System.out.println("End of file");
            }
        }
        catch (IOException ex)
        {
            System.out.println("Scan failed with " + ex.getMessage());
        }
    }

    private void map()
    {
        mkv.d();
    }

    private void eval(String[] s,
                      OutputStream o)
    {
        MKR r = null;

        // [0] eval
        // [1..] filters
        // e.g.
        // eval filter
        // eval filter1 filter2 filter3
        // eval filter1;name1=value1;name2=value2
        // eval filter1;name1=value1;name2=value2 filter2 filter3;name1=value1;name2=value2
        // eval filter1;list1=value1,value2,value3
        for (int i = 1; i < s.length; i++)
        {
            HashMap<String, String> filterTokens = tokenise(s[i]);
            if (postFilters.containsKey(filterTokens.get(MKI.FilterKeys.FILTERNAME.key())))
            {
                String f = filterTokens.get(MKI.FilterKeys.FILTERNAME.key());
                try
                {
                    Class<?> filter = Class.forName(postFilters.get(f));
                    PostChainFilter pcf = (PostChainFilter) filter.newInstance();
                    pcf.apply(mkv, r, filterTokens, o);
                    r = pcf.result();
                }
                catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex)
                {
                    System.out.println("Filter not found (" + postFilters.get(f) + ")");
                    System.out.println(ex.getMessage());
                }
            }
            else
            {
                System.out.println("Unknown filter (" + filterTokens.get(MKI.FilterKeys.FILTERNAME.key()) + ")");
            }
        }
    }

    private HashMap<String, String> tokenise(String filter)
    {
        HashMap<String, String> tokens = new HashMap<>();
        tokens.put(MKI.FilterKeys.FILTERNAME.key(), "");

        StringTokenizer keys = new StringTokenizer(filter, MKI.FilterKeys.PARAMDELIMITER.key(), false);

        if (keys.hasMoreElements())
        {
            tokens.put(MKI.FilterKeys.FILTERNAME.key(), keys.nextToken().toLowerCase());

            while (keys.hasMoreTokens())
            {
                StringTokenizer params = new StringTokenizer(keys.nextToken(), MKI.FilterKeys.EQUALDELIMITER.key(), false);
                if (params.hasMoreTokens())
                {
                    if (params.countTokens() > 1)
                    {
                        tokens.put(params.nextToken().toLowerCase(), params.nextToken());
                    }
                    else
                    {
                        tokens.put(params.nextToken().toLowerCase(), "");
                    }
                }
            }
        }
        System.out.println("tokens->" + tokens);
        return tokens;
    }

    public MKV mkv()
    {
        return mkv;
    }

    public void clear()
    {
        mkv.clear();
    }

    private InputStream file(String path)
    {
        try
        {
            return new FileInputStream(path);
        }
        catch (FileNotFoundException ex)
        {
            ex.printStackTrace();
        }

        return null;
    }

    private InputStream url(String url)
    {

        try
        {
            return new URL(url).openStream();
        }
        catch (MalformedURLException ex)
        {
            ex.printStackTrace();
        }
        catch (IOException ex)
        {
            ex.printStackTrace();

        }
        return null;
    }
}
