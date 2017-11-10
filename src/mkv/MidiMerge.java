/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mkv;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Track;

/**
 *
 * @author aziz
 */
public class MidiMerge {

    public static void main(String[] args) {
        String source = "C:\\aziz\\workspace\\lab\\MKV-Copy\\midi\\music";
        try {
            new MidiMerge().mergem(new File(source));
        } catch (InvalidMidiDataException ex) {
            Logger.getLogger(MidiMerge.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MidiMerge.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void mergem(File source) throws InvalidMidiDataException, IOException {
        if (null == source) {
            return;
        }

        if (!source.canRead()) {
            return;
        }

        File[] midis = source.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return (name.endsWith(".mid"));
            }
        });

        javax.sound.midi.Sequence s = new javax.sound.midi.Sequence(javax.sound.midi.Sequence.SMPTE_25, 6);
        Track t = s.createTrack();

        for (File midi : midis) {
            System.out.println("File in -> " + midi.getAbsolutePath());
            append(midi, t);
        }
        //****  set end of track (meta event) ****
        MetaMessage mt = new MetaMessage();
        byte[] bet = {}; // empty array
        mt.setMessage(0x2F, bet, 0);
        MidiEvent me = new MidiEvent(mt, (long) 0);
        t.add(me);

        //****  write the MIDI sequence to a MIDI file  ****
        File f = new File("midifile.mid");
        MidiSystem.write(s, 1, f);
        System.out.println("File out -> " + f.getAbsolutePath());
    }

    public void append(File file, Track t) {
        try {
            javax.sound.midi.Sequence sequence = MidiSystem.getSequence(file);

            for (Track track : sequence.getTracks()) {
                for (int i = 0; i < track.size(); i++) {
//                    if (track.size() > 1) {
//                        continue;
//                    }
                    t.add(track.get(i));
                }
            }

        } catch (InvalidMidiDataException | IOException e) {
            e.printStackTrace();
        }
    }

//    public void Learn(String midiName, Track t) {
//        try {
//            javax.sound.midi.Sequence sequence = MidiSystem.getSequence(new File(midiName));
//
//            int id[] = {0, 0, 0};
//            int nArr[][] = new int[2][2];
//
//            for (Track track : sequence.getTracks()) {
//                for (int i = 0; i < track.size(); i++) {
//                    MidiEvent event = track.get(i);
//                    MidiMessage message = event.getMessage();
//                    if (message instanceof ShortMessage) {
//                        ShortMessage sm = (ShortMessage) message;
//
//                        if (sm.getCommand() == ShortMessage.NOTE_ON) {
//                            int key = sm.getData1();
//
//                            for (int j = 0; j < 2; j++) {
//                                if (id[j] == 2) {
//                                    id[j] = 0;
//                                    Score.updateWeight(nArr[j][0], nArr[j][1], key);
//                                } else {
//                                    nArr[j][id[j]++] = key;
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//
//        } catch (InvalidMidiDataException | IOException e) {
//            e.printStackTrace();
//        }
//    }
}
