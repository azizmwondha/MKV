/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mkv;

import mkv.filters.chains.MKV_byte;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import mkv.filters.chains.MKV_word;
import mkv.filters.post.TransitionMatrix;
import mkv.filters.pre.URL2LocalFile;
import mkv.types.MKV;
import mkv.types.exceptions.MKE;

/**
 *
 * @author amwon
 */
public class Runit
{

    public static void main(String[] args)
    {
        if (args.length < 2)
        {
            args = new String[]
            {
                "2", "C:\\aziz\\workspaces\\netbeans\\MKV\\data\\text\\anthem_kenya_en.txt"
            };

//            args = new String[]
//            {
//                "2", "http://vknight.org/unpeudemath/code/2015/11/15/Visualising-markov-chains.html"
//            };
//
//            args = new String[]
//            {
//                "4", "C:\\aziz\\workspaces\\netbeans\\MKV\\data\\midi\\Scales_and_Arpeggios_-_C_SCALES.mid"
////                "2", "C:\\aziz\\workspaces\\netbeans\\MKV\\data\\midi\\samba.mid"
//            };
        }
        
         
        int order = 1;
        try
        {
            order = Integer.parseInt(args[0]);
        }
        catch (NumberFormatException nfe)
        {
            order = 1;
            System.out.println("Cannot parse first argument (order), default to 1");
        }
        MKV t = new MKV_word();
//        MKV t = new MKV_byte(order);
t.order(order);

        InputStream is;
        if (args[1].startsWith("http"))
        {
            URL2LocalFile urL2LocalFile = new URL2LocalFile();
            is = urL2LocalFile.scan(url(args[1]));
        }
        else
        {
            is = file(args[1]);
        }

        try
        {
            t.scan(is);
        }
        catch (IOException ex)
        {
            Logger.getLogger(Runit.class.getName()).log(Level.SEVERE, null, ex);
        }
        t.d();

        TransitionMatrix tm = new TransitionMatrix();

        try
        {
            // Transition matrix visualisation:
            // http://setosa.io/markov/playground.html
            tm.apply(t, null, null, System.out);
            
//        new Main().go(args);
        }
        catch (MKE ex)
        {
            Logger.getLogger(Runit.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void go(String[] args)
    {
//        MKV t = new MKV(3);
//        t.scan("This is the thing");
//        t.scan("You shouldnt throw stones if you live in a glass house");
//        t.scan("You got a glass face better watch your mouth");

//        t.scan("a b c b a c c a ");
        String fif = ("You shouldnt throw stones if you live in a glass house ");
        fif += ("You got a glass face better watch your mouth ");
        fif += ("Cos Ill break your face");

//        t.scan("This is the thing, if");
//        t.scan("You shouldnt throw stones if you live in a glass house, you");
//        t.scan("You got a glass face better watch your mouth, this");
        String TX = "Abstract class is a special class in Java, it can not be instantiated and that's why can not be used directly. At first concept of abstraction, abstract class and interface all look useless to many developers, because you can not implement any method in an interface, you can not create an object of the abstract class, so why do you need them. Once they face biggest constant of software development, yes that is CHANGE, they understand how abstraction at the top level can help in writing flexible software. A key challenge while writing software (Java Programs, C++ programs) is not just to cater today's requirement but also to ensure that nurture requirement can be handled without any architectural or design change in your code. In short, your software must be flexible enough to support future changes.\n"
                + "\n"
                + "The abstract class and inheritance collectively ensures that most of the code are written using abstract and higher level classes, so that it can leverage Inheritance and Polymorphism to support future changes.\n"
                + "\n"
                + "This is actually one of the most useful design principle, also known as \"Programming for interfaces rather than implementation\".  Abstract class and abstract method are two ways through which Java assist you on coding at a certain level of abstraction.\n";

        String RIRIKS = "If I should stay, I would only be in your way "
                + "So I'll go, but I know. "
                + "I'll think of you every step of the way. "
                + "And I will always love you "
                + "I will always love you "
                + "You, my darling you, hm "
                + "Bittersweet memories "
                + "That is all I'm taking with me "
                + "So, goodbye "
                + "Please, don't cry ";
        String RIRIKS2 = "We both know I'm not what you, you need "
                + "And I will always love you "
                + "I will always love you "
                + "I hope life treats you kind "
                + "And I hope you have all you've dreamed of "
                + "And I wish to you joy and happiness "
                + "But above all this, I wish you love "
                + "And I will always love you "
                + "I will always love you ."
                + "I will always love you ."
                + "I will always love you ."
                + "I will always love you ."
                + "I, I will always love you "
                + "You, darling, I love you "
                + "Oh, I'll always, I'll always love you";

        String MIMI_MTU = "I am not a number! I am a free man!";

        String FOX = "quick brown fox jumped over the lazy dog";

//        t.scan(RIRIKS);
//        t.scan(RIRIKS2);
//        MKV t = new MKV_word();
        MKV t = new MKV_byte();
        t.order(3);
        InputStream is_whitney = new ByteArrayInputStream((RIRIKS + RIRIKS2).getBytes());
        InputStream is_fifty = new ByteArrayInputStream((fif).getBytes());
        InputStream is_mtu = new ByteArrayInputStream((MIMI_MTU).getBytes());
        InputStream is_fox = new ByteArrayInputStream((FOX).getBytes());
        InputStream is_alice = new ByteArrayInputStream(("Alice was beginning to get very tired of sitting by her sister on the bank, and of having nothing to do").getBytes());

        try
        {
            //t.scan(is_whitney);
//t.scan(is_fifty);
//t.scan(is_mtu);
//t.scan(is_fox);
//t.scan(is_alice);
            t.scan(file("C:\\aziz\\workspaces\\netbeans\\MKV\\data\\alice29.txt"));
        }
        catch (IOException ex)
        {
            Logger.getLogger(Runit.class.getName()).log(Level.SEVERE, null, ex);
        }

//InputStream is_url = url("http://vknight.org/unpeudemath/code/2015/11/15/Visualising-markov-chains.html");
//            t.scan(is_url);
        t.d();

        TransitionMatrix tm = new TransitionMatrix();

        try
        {
            // Transition matrix visualisation:
            // http://setosa.io/markov/playground.html
            tm.apply(t, null, null, System.out);
        }
        catch (MKE ex)
        {
            Logger.getLogger(Runit.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static InputStream file(String path)
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

    private static InputStream url(String url)
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
