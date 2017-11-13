/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mkv;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Scanner;
import mkv.console.MConsole;
import mkv.console.MRunner;

/**
 *
 * @author aziz
 */
public class Main
{

    public static void main(String[] args)
    {
        if (args.length == 0)
        {
            new Main().konsol();
        }
        else
        {
            new Main().skript(args);
        }
    }

    private void konsol()
    {
        Thread t = new Thread(new MConsole());
        t.start();
    }

    private void skript(String[] args)
    {
        File file = new File(args[0]);

        if (file.canRead())
        {
            OutputStream out = System.out;
            System.out.println(file.getAbsolutePath());
            // Read script from file
            MRunner k = new MRunner();
            try (Scanner scanner = new Scanner(file))
            {
                if (args.length > 1)
                {
                    out = new FileOutputStream(args[1]);
                }
                scanner.useDelimiter("\\n");
                while (scanner.hasNext())
                {
                    String l = scanner.next();
                    System.out.println("> " + l);
                    k.run(l, out);
                }
            }
            catch (FileNotFoundException ex)
            {
                ex.printStackTrace();
            }
            finally
            {
                try
                {
                    if (null != out)
                    {
                        out.flush();
                        if (args.length > 1)
                        {
                            out.close();
                        }
                    }
                }
                catch (IOException ex)
                {
                    ex.printStackTrace();
                }
            }
        }
        else
        {
            // 
        }
    }
}
