/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mkv;

import java.io.File;

/**
 *
 * @author aziz
 */
public class Bin2Midi
{
    public static void main(String[] args)
    {
                        String fname = args[0];
                File pitchFile = new File(fname + "-pitch.bin");
                File velocityFile = new File(fname + "-velocity.bin");
                File deltaFile = new File(fname + "-delta.bin");
    }
}
