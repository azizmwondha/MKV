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
import mkv.filters.chains.MKV_byte;
import mkv.filters.chains.MKV_word;
import mkv.filters.pre.MidiTrackReader;
import mkv.filters.pre.URL2LocalFile;
import mkv.types.MKV;
import mkv.types.PostChainFilter;

/**
 *
 * @author amwon
 */
public class MRunner
    {

        public MKV mkv;
        private final List<String> history;
        private final Map<String, String> postFilters;

        public MRunner()
        {
            this.history = new ArrayList<>();
            this.mkv = new MKV_word();
            this.postFilters = new HashMap<>();
            postFilters.put("transition", "mkv.filters.post.TransitionMatrix");
            postFilters.put("texts", "mkv.filters.post.GenerateTexts");
            postFilters.put("notes", "mkv.filters.post.GenerateNotes");
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
                System.out.println("order set to " + o);
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
            for (int i = 1; i < s.length; i++)
            {
                if (postFilters.containsKey(s[i]))
                {
                    try
                    {
                        Class<?> filter = Class.forName(postFilters.get(s[i]));
                        PostChainFilter pcf = (PostChainFilter) filter.newInstance();
                        pcf.apply(mkv, o);
                    }
                    catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex)
                    {
                        System.out.println("Filter not found (" + postFilters.get(s[i]) + ")");
                        System.out.println(ex.getMessage());
                    }
                }
                else
                {
                    System.out.println("Unknown filter (" + s[i] + ")");
                }
            }
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
