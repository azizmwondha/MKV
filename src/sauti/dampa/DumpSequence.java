/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sauti.dampa;

import java.io.File;
import java.io.IOException;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;

public class DumpSequence
{
	private static String[]	sm_astrKeyNames = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

	private static Receiver		sm_receiver = new DumpReceiver(System.out, true);


	public static void main(String[] args)
	{
//		/*
//		 *	We check that there is exactely one command-line
//		 *	argument. If not, we display the usage message and
//		 *	exit.
//		 */
//		if (args.length != 1)
//		{
//			out("DumpSequence: usage:");
//			out("\tjava DumpSequence <midifile>");
//			System.exit(1);
//		}
//		/*
//		 *	Now, that we're shure there is an argument, we take it as
//		 *	the filename of the soundfile we want to play.
//		 */
//		String	strFilename = args[0];
//		File	midiFile = new File(strFilename);

//		String	strFilename = "C:\\aziz\\workspaces\\netbeans\\MKV\\data\\midi\\MIDI_sample.mid";
		String	strFilename = "C:\\aziz\\workspaces\\netbeans\\MKV\\data\\midi\\morse-code-a.mid";
		File	midiFile = new File(strFilename);
		/*
		 *	We try to get a Sequence object, which the content
		 *	of the MIDI file.
		 */
		Sequence	sequence = null;
		try
		{
			sequence = MidiSystem.getSequence(midiFile);
		}
		catch (InvalidMidiDataException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.exit(1);
		}

		/*
		 *	And now, we output the data.
		 */
		if (sequence == null)
		{
			out("Cannot retrieve Sequence.");
		}
		else
		{
			out("---------------------------------------------------------------------------");
			out("File: " + strFilename);
			out("---------------------------------------------------------------------------");
			out("Length: " + sequence.getTickLength() + " ticks");
			out("Duration: " + sequence.getMicrosecondLength() + " microseconds");
			out("---------------------------------------------------------------------------");
			float	fDivisionType = sequence.getDivisionType();
			String	strDivisionType = null;
			if (fDivisionType == Sequence.PPQ)
			{
				strDivisionType = "PPQ";
			}
			else if (fDivisionType == Sequence.SMPTE_24)
			{
				strDivisionType = "SMPTE, 24 frames per second";
			}
			else if (fDivisionType == Sequence.SMPTE_25)
			{
				strDivisionType = "SMPTE, 25 frames per second";
			}
			else if (fDivisionType == Sequence.SMPTE_30DROP)
			{
				strDivisionType = "SMPTE, 29.97 frames per second";
			}
			else if (fDivisionType == Sequence.SMPTE_30)
			{
				strDivisionType = "SMPTE, 30 frames per second";
			}

			out("DivisionType: " + strDivisionType);

			String	strResolutionType = null;
			if (sequence.getDivisionType() == Sequence.PPQ)
			{
				strResolutionType = " ticks per beat";
			}
			else
			{
				strResolutionType = " ticks per frame";
			}
			out("Resolution: " + sequence.getResolution() + strResolutionType);
			out("---------------------------------------------------------------------------");
			Track[]	tracks = sequence.getTracks();
			for (int nTrack = 0; nTrack < tracks.length; nTrack++)
			{
				out("Track " + nTrack + ":");
				out("-----------------------");
				Track	track = tracks[nTrack];
				for (int nEvent = 0; nEvent < track.size(); nEvent++)
				{
					MidiEvent	event = track.get(nEvent);
					output(event);
				}
				out("---------------------------------------------------------------------------");
			}
			// TODO: getPatchList()
		}
	}


	public static void output(MidiEvent event)
	{
		MidiMessage	message = event.getMessage();
		long		lTicks = event.getTick();
		sm_receiver.send(message, lTicks);
	}



	private static void out(String strMessage)
	{
		System.out.println(strMessage);
	}
}

