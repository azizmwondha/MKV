/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mkv.filters.chains;

import java.io.InputStream;
import java.util.Scanner;
import java.util.regex.Pattern;
import mkv.types.MKV;
import mkv.types.StringSequence;

/**
 *
 * @author amwon
 */
public class MKV_word
        extends MKV
{

    private final Pattern pattern;

    public MKV_word()
    {
        pattern = Pattern.compile(".*", Pattern.CASE_INSENSITIVE);
    }

    @Override
    public void scan(InputStream input)
    {
        try (Scanner scanner = new Scanner(input))
        {
            scanner.useDelimiter("[\\s\n]");

            while (scanner.hasNext())
            {
                String token = scanner.next();//pattern);
                // Sanitise token
                // if has period ".", clear tokenBuffer after scan
                // remove period from data then scan.
                scan(new StringSequence(token));
            }
            scanner.close();
        }
    }
}
