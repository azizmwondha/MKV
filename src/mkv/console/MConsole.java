/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mkv.console;

import java.util.Scanner;

/**
 *
 * @author amwon
 */
public class MConsole
            implements Runnable
    {

        private final MRunner runner = new MRunner();
        private boolean isRunning = true;

        public MConsole()
        {

        }

        @Override
        public void run()
        {
            help();

            while (isRunning())
            {
                String s = readString();

                if (s.equalsIgnoreCase("info"))
                {
                    info();
                }
                if (s.equalsIgnoreCase("help"))
                {
                    help();
                }
                if (s.equalsIgnoreCase("exit"))
                {
                    exit();
                    continue;
                }
                runner.run(s, System.out);//null);
            }
        }

        private boolean isRunning()
        {
            return isRunning;
        }

        private void exit()
        {
            isRunning = false;
        }

        private synchronized void info()
        {
            p("----");
            p("parser  " + runner.mkv().getClass().getSimpleName());
            p("order   " + runner.mkv().order());
            p("states  " + runner.mkv().states().size());
            p("origins " + runner.mkv().origins().size());
            p("total   " + Runtime.getRuntime().totalMemory());
            p("free    " + Runtime.getRuntime().freeMemory());
            p("used    " + (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()));
            p("");
            
        }

        private synchronized void help()
        {
            p("----");
            p("parser (word | byte)");
            p("order  (1+)");
            p("scan   (file path | URL | plain text)");
            p("map    ");
            p("eval   (matrix | texts | predict)");
            p("       Post-chain filter name");
            p("       Params: max-tokens, start-vector, time");
            p("info   Session info");
            p("clear  ");
            p("");
        }

        private synchronized String readString()
        {
            Scanner scanner = new Scanner(System.in);
            scanner.useDelimiter("[\n]");
            if (scanner.hasNext())
            {
                return scanner.next().trim();
            }
            return "exit";
        }

        private void p(String s)
        {
            System.out.println(s);
        }
    }
