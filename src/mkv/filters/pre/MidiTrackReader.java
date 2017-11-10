/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mkv.filters.pre;

import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;
import mkv.filters.pre.midi.Note;
import mkv.types.PreChainFilter;

/**
 *
 * @author aziz
 */
public class MidiTrackReader
        implements PreChainFilter
{
    private final Map<Integer, Collection<Note>> map = new HashMap<>();
    private final Map<Integer, Boolean> isIncluded = new HashMap<>();
    private double num, denom;
    private Sequence sequence = null;

    @Override
    public InputStream scan(InputStream input)
    {
        try
        {
            //        Parser p = new Parser(input);
//        System.out.println(""+p.toString());

            return setMap(input);
        }
        catch (Exception ex)
        {
            Logger.getLogger(MidiTrackReader.class.getName()).log(Level.SEVERE, null, ex);
        }

//        return input;
        return null;

    }

    public InputStream setMap(InputStream input)
            throws Exception
    {
        try
        {
            sequence = MidiSystem.getSequence(input);
            input.close();
        }
        catch (InvalidMidiDataException | IOException e)
        {
            e.printStackTrace();
        }

        long t = System.currentTimeMillis();
        try
        {
            int trackIndex = 0;
            long prevTick = 0;
            for (Track track : sequence.getTracks())
            {

//                String fname = getClass().getCanonicalName() + "-" + t + "-track-" + (trackIndex + 1);
                String fname = "midi-track-" + (trackIndex + 1);
                File pitchFile = new File(fname + "-pitch.bin");
                File velocityFile = new File(fname + "-velocity.bin");
                File deltaFile = new File(fname + "-delta.bin");

                OutputStream fosP = new FileOutputStream(pitchFile);
                OutputStream fosV = new FileOutputStream(velocityFile);
                OutputStream fosD = new FileOutputStream(deltaFile);

                System.out.println("TRACK " + (trackIndex + 1));
                List<Note> unfinished = new ArrayList();
                for (int i = 0; i < track.size(); i++)
                {
                    MidiEvent event = track.get(i);
                    MidiMessage message = event.getMessage();

                    if (message instanceof MetaMessage)
                    {
                        MetaMessage mm = (MetaMessage) message;

                        System.out.println("META: " + HexBin.encode(mm.getMessage()) + " Typ=" + mm.getType() + " Data=" + HexBin.encode(mm.getMessage()));

                    }
                    if (message instanceof SysexMessage)
                    {
                        SysexMessage sm = (SysexMessage) message;

                        System.out.println("SYSX: " + HexBin.encode(sm.getMessage()) + " Data=" + HexBin.encode(sm.getMessage()));

                    }
                    if (message instanceof ShortMessage)
                    {
                        ShortMessage sm = (ShortMessage) message;

                        long d = event.getTick() - prevTick;
                        prevTick = event.getTick();

                        try
                        {
                            fosP.write(sm.getData1());
                            fosV.write(sm.getData2());
                            fosD.write((int) d);
                        }
                        catch (IOException ex)
                        {
                            ex.printStackTrace();
                        }

                        System.out.println("EVNT: deltaint=" + d + " deltahex=" + Long.toHexString(d) + "(" + Long.toHexString(event.getTick()) + ")" + "-" + HexBin.encode(sm.getMessage()) + " O=" + sm.getCommand() + " C=" + sm.getChannel() + " K=" + sm.getData1() + " V=" + sm.getData2());
                        int channel = sm.getChannel() + 1;
                        if (!map.containsKey(channel))
                        {
                            map.put(channel, new LinkedList());
                            isIncluded.put(channel, true);
                        }
                        int key = sm.getData1();
                        int velocity = sm.getData2();
                        int beat = (int) Math.floor(denom * event.getTick() / (sequence.getResolution() / num));
                        if (velocity != 0)
                        {
                            Note n = new Note(beat, channel, velocity, key);
                            unfinished.add(n);
                        }
                        else
                        {
                            Note temp = new Note(0, channel, velocity, key);
                            for (int j = 0; j < unfinished.size(); j++)
                            {
                                Note n = unfinished.get(j);
                                if (n.equals(temp))
                                {
                                    unfinished.remove(j);
                                    n.finish(beat);
                                    map.get(channel).add(n);
                                }
                            }
                        }
                    }
                }
                System.out.println("");
                trackIndex++;
                fosP.flush();
                fosV.flush();
                fosD.flush();
                fosP.close();
                fosV.close();
                fosD.close();
            }
            return null;
        }
        catch (FileNotFoundException ex)
        {
            Logger.getLogger(MidiTrackReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (IOException ex)
        {
            Logger.getLogger(MidiTrackReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
