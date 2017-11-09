package mkv.filters.pre.midi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import static javax.sound.midi.MidiSystem.getSequence;

public class Parser
{

    private int tempo;
    private Sequence sequence;
    private Map<Integer, Collection<Note>> map = new HashMap<>();
    private Map<Integer, Boolean> isIncluded = new HashMap<>();
    private double num, denom;

    public Parser(InputStream f)
    {
        try
        {
            this.sequence = getSequence(f);
        }
        catch (InvalidMidiDataException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        try
        {
            this.setMap();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    Parser(File f)
    {
        try
        {
            this.sequence = getSequence(f);
        }
        catch (InvalidMidiDataException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        try
        {
            this.setMap();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private static int toTempo(double bpm,
                               double timeSig)
    {
        bpm /= timeSig;
        bpm = 1 / bpm;
        return (int) (10000 * 60 * bpm);
    }

    private void setTempo()
    {
        this.num = 4;
        this.denom = 4;
        for (Track track : this.sequence.getTracks())
        {
            for (int i = 0; i < track.size(); i++)
            {
                MidiEvent event = track.get(i);
                MidiMessage message = event.getMessage();
                if (message instanceof MetaMessage)
                {
                    MetaMessage m = (MetaMessage) message;
                    if (m.getType() == 0x58)
                    {
                        switch (m.getData()[0])
                        {
                            case 4:
                                break;
                            case 3:
                                num = 4;
                                break;
                            case 6:
                                num = 6;
                                denom = 8;
                                break;
                            default:
                                throw new IllegalArgumentException("Invalid time signature");
                        }
                    }
                }
            }
        }
        this.tempo = toTempo(160, denom);
    }

    public Set<Integer> channels()
    {
        return this.map.keySet();
    }

    public void setMap()
            throws Exception
    {
        for (Track track : this.sequence.getTracks())
        {
            List<Note> unfinished = new ArrayList();
            for (int i = 0; i < track.size(); i++)
            {
                MidiEvent event = track.get(i);
                MidiMessage message = event.getMessage();
                if (message instanceof ShortMessage)
                {
                    ShortMessage sm = (ShortMessage) message;
                    int channel = sm.getChannel() + 1;
                    if (!this.map.containsKey(channel))
                    {
                        this.map.put(channel, new LinkedList());
                        this.isIncluded.put(channel, true);
                    }
                    int key = sm.getData1();
                    int velocity = sm.getData2();
                    int beat = (int) Math.floor(denom * event.getTick() / (this.sequence.getResolution() / num));
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
                                this.map.get(channel).add(n);
                            }
                        }
                    }
                }
            }
        }
    }

    public String toString()
    {
        StringBuilder str = new StringBuilder();
        str.append("tempo " + this.tempo + "\n");
        for (int i : this.map.keySet())
        {
            if (this.isIncluded.get(i))
            {
                for (Note n : this.map.get(i))
                {
                    str.append(n.toString() + "\n");
                }
            }
        }
        return str.toString();
    }

    public void flip(int i)
    {
        this.isIncluded.put(i, !this.isIncluded.get(i));
    }
}
