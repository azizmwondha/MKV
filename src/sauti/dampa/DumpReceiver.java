package sauti.dampa;

/*
 *	DumpReceiver.java
 *
 *	This file is part of jsresources.org
 */

 /*
 * Copyright (c) 1999 - 2001 by Matthias Pfisterer
 * Copyright (c) 2003 by Florian Bomers
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
import java.io.PrintStream;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Receiver;

/**
 * Displays the file format information of a MIDI file.
 */
public class DumpReceiver
        implements Receiver
{

    public static long seByteCount = 0;
    public static long smByteCount = 0;
    public static long seCount = 0;
    public static long smCount = 0;

    private static final String[] sm_astrKeyNames =
    {
        "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    };

    private static final String[] sm_astrKeySignatures =
    {
        "Cb", "Gb", "Db", "Ab", "Eb", "Bb", "F", "C", "G", "D", "A", "E", "B", "F#", "C#"
    };
    private static final String[] SYSTEM_MESSAGE_TEXT =
    {
        "System Exclusive (should not be in ShortMessage!)",
        "MTC Quarter Frame: ",
        "Song Position: ",
        "Song Select: ",
        "Undefined",
        "Undefined",
        "Tune Request",
        "End of SysEx (should not be in ShortMessage!)",
        "Timing clock",
        "Undefined",
        "Start",
        "Continue",
        "Stop",
        "Undefined",
        "Active Sensing",
        "System Reset"
    };

    private static final String[] QUARTER_FRAME_MESSAGE_TEXT =
    {
        "frame count LS: ",
        "frame count MS: ",
        "seconds count LS: ",
        "seconds count MS: ",
        "minutes count LS: ",
        "minutes count MS: ",
        "hours count LS: ",
        "hours count MS: "
    };

    private static final String[] FRAME_TYPE_TEXT =
    {
        "24 frames/second",
        "25 frames/second",
        "30 frames/second (drop)",
        "30 frames/second (non-drop)",
    };

    private PrintStream m_printStream;
    private boolean m_bDebug;
    private boolean m_bPrintTimeStampAsTicks;

    public DumpReceiver(PrintStream printStream)
    {
        this(printStream, false);
    }

    public DumpReceiver(PrintStream printStream,
                        boolean bPrintTimeStampAsTicks)
    {
        m_printStream = printStream;
        m_bDebug = false;
        m_bPrintTimeStampAsTicks = bPrintTimeStampAsTicks;
    }

    @Override
    public void close()
    {
    }

    @Override
    public void send(MidiMessage message,
                     long lTimeStamp)
    {
        String strMessage = null;
        if (message instanceof ShortMessage)
        {
            strMessage = DumpReceiver.this.decode((ShortMessage) message, lTimeStamp);
        }
        else if (message instanceof SysexMessage)
        {
            strMessage = DumpReceiver.this.decode((SysexMessage) message);
        }
        else if (message instanceof MetaMessage)
        {
            strMessage = decode((MetaMessage) message);
        }
        else
        {
            strMessage = "unknown message type";
        }
        String strTimeStamp = null;
        if (m_bPrintTimeStampAsTicks)
        {
            strTimeStamp = "tick " + lTimeStamp + ": ";
        }
        else
        {
            if (lTimeStamp == -1L)
            {
                strTimeStamp = "timestamp [unknown]: ";
            }
            else
            {
                strTimeStamp = "timestamp " + lTimeStamp + " us: ";
            }
        }
        m_printStream.println(strTimeStamp + strMessage);
    }

    public String decode(ShortMessage message,
                         long time)
    {
        String strMessage = null;
        switch (message.getCommand())
        {
            case 0x80:
                strMessage = "note Off " + getKeyName(message.getData1()) + " velocity: " + message.getData2();
                break;

            case 0x90:
                strMessage = "note On " + getKeyName(message.getData1()) + " velocity: " + message.getData2();
                break;

            case 0xa0:
                strMessage = "polyphonic key pressure " + getKeyName(message.getData1()) + " pressure: " + message.getData2();
                break;

            case 0xb0:
                strMessage = "control change " + message.getData1() + " value: " + message.getData2();
                break;

            case 0xc0:
                strMessage = "program change " + message.getData1();
                break;

            case 0xd0:
                strMessage = "key pressure " + getKeyName(message.getData1()) + " pressure: " + message.getData2();
                break;

            case 0xe0:
                strMessage = "pitch wheel change " + get14bitValue(message.getData1(), message.getData2());
                break;

            case 0xF0:
                strMessage = SYSTEM_MESSAGE_TEXT[message.getChannel()];
                switch (message.getChannel())
                {
                    case 0x1:
                        int nQType = (message.getData1() & 0x70) >> 4;
                        int nQData = message.getData1() & 0x0F;
                        if (nQType == 7)
                        {
                            nQData = nQData & 0x1;
                        }
                        strMessage += QUARTER_FRAME_MESSAGE_TEXT[nQType] + nQData;
                        if (nQType == 7)
                        {
                            int nFrameType = (message.getData1() & 0x06) >> 1;
                            strMessage += ", frame type: " + FRAME_TYPE_TEXT[nFrameType];
                        }
                        break;

                    case 0x2:
                        strMessage += get14bitValue(message.getData1(), message.getData2());
                        break;

                    case 0x3:
                        strMessage += message.getData1();
                        break;
                }
                break;

            default:
                strMessage = "unknown message: status = " + message.getStatus() + ", byte1 = " + message.getData1() + ", byte2 = " + message.getData2();
                break;
        }
        if (message.getCommand() != 0xF0)
        {
            int nChannel = message.getChannel() + 1;
            String strChannel = "channel " + nChannel + ": ";
            strMessage = strChannel + strMessage;
        }
        smCount++;
        smByteCount += message.getLength();
        return "[" + Yutil.toHex(time) + "] [" + getHexString(message) + "] " + strMessage;
    }

    public String decode(SysexMessage message)
    {
        byte[] abData = message.getData();
        String strMessage = null;
        if (message.getStatus() == SysexMessage.SYSTEM_EXCLUSIVE)
        {
            strMessage = "Sysex message: F0" + getHexString(abData);
        }
        else if (message.getStatus() == SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE)
        {
            strMessage = "Continued Sysex message F7" + getHexString(abData);
            seByteCount--; // do not count the F7
        }
        seByteCount += abData.length + 1;
        seCount++; // for the status byte
        return strMessage;
    }

    public String decode(MetaMessage message)
    {
        byte[] data = message.getData();
        String strMessage = null;
        switch (message.getType())
        {
            case 0:
                int sequenceNumber = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
                strMessage = "Sequence Number: " + sequenceNumber;
                break;

            case 1:
                strMessage = "Text Event: " + new String(data);
                break;

            case 2:
                strMessage = "Copyright Notice: \"" + new String(data) + "\"";
                break;

            case 3:
                strMessage = "Sequence/Track Name: \"" + new String(data) + "\"";
                break;

            case 4:
                strMessage = "Instrument Name:  \"" + new String(data) + "\"";
                break;

            case 5:
                strMessage = "Lyric: " + new String(data);
                break;

            case 6:
                strMessage = "Marker: " + new String(data);
                break;

            case 7:
                strMessage = "Cue Point: " + new String(data);
                break;

            case 0x20:
                int nChannelPrefix = data[0] & 0xFF;
                strMessage = "MIDI Channel Prefix: " + nChannelPrefix;
                break;

            case 0x2F:
                strMessage = "End of Track";
                break;

            case 0x51:
                int nTempo = ((data[0] & 0xFF) << 16)
                        | ((data[1] & 0xFF) << 8)
                        | (data[2] & 0xFF);           // tempo in microseconds per beat
                float bpm = convertTempo(nTempo);
                // truncate it to 2 digits after dot
                bpm = (float) (Math.round(bpm * 100.0f) / 100.0f);
                strMessage = "Set Tempo: " + bpm + " bpm";
                break;

            case 0x54:
                strMessage = "SMTPE Offset: "
                        + (data[0] & 0xFF) + ":"
                        + (data[1] & 0xFF) + ":"
                        + (data[2] & 0xFF) + "."
                        + (data[3] & 0xFF) + "."
                        + (data[4] & 0xFF);
                break;

            case 0x58:
                strMessage = "Time Signature: "
                        + (data[0] & 0xFF) + "/" + (1 << (data[1] & 0xFF))
                        + ", MIDI clocks per metronome tick: " + (data[2] & 0xFF)
                        + ", 1/32 per 24 MIDI clocks: " + (data[3] & 0xFF);
                break;

            case 0x59:
                String strGender = (data[1] == 1) ? "minor" : "major";
                strMessage = "Key Signature: " + sm_astrKeySignatures[data[0] + 7] + " " + strGender;
                break;

            case 0x7F:
                // TODO: decode vendor code, dump data in rows
                String strDataDump = getHexString(data);
                strMessage = "Sequencer-Specific Meta event: " + strDataDump;
                break;

            default:
                String strUnknownDump = getHexString(data);
                strMessage = "unknown Meta event: " + strUnknownDump;
                break;

        }
        return "[Type=" + Yutil.toHex(message.getType()) + "] " + strMessage;
    }

    public static String getKeyName(int nKeyNumber)
    {
        if (nKeyNumber > 127)
        {
            return "illegal value";
        }
        else
        {
            int nNote = nKeyNumber % 12;
            int nOctave = nKeyNumber / 12;
            return sm_astrKeyNames[nNote] + (nOctave - 1);
        }
    }

    public static int get14bitValue(int nLowerPart,
                                    int nHigherPart)
    {
        return (nLowerPart & 0x7F) | ((nHigherPart & 0x7F) << 7);
    }

    private static int signedByteToUnsigned(byte b)
    {
        return b & 0xFF;
    }

    // convert from microseconds per quarter note to beats per minute and vice versa
    private static float convertTempo(float value)
    {
        if (value <= 0)
        {
            value = 0.1f;
        }
        return 60000000.0f / value;
    }

    public static String getHexString(byte[] aByte)
    {
        StringBuffer sbuf = new StringBuffer(aByte.length * 3 + 2);
        for (int i = 0; i < aByte.length; i++)
        {
            sbuf.append(' ');
            sbuf.append(Yutil.hexDigits[(aByte[i] & 0xF0) >> 4]);
            sbuf.append(Yutil.hexDigits[aByte[i] & 0x0F]);
            /*byte	bhigh = (byte) ((aByte[i] &  0xf0) >> 4);
			sbuf.append((char) (bhigh > 9 ? bhigh + 'A' - 10: bhigh + '0'));
			byte	blow = (byte) (aByte[i] & 0x0f);
			sbuf.append((char) (blow > 9 ? blow + 'A' - 10: blow + '0'));*/
        }
        return new String(sbuf);
    }

    public static String getHexString(ShortMessage sm)
    {
        // bug in J2SDK 1.4.1
        // return getHexString(sm.getMessage());
        int status = sm.getStatus();
        String res = Yutil.toHex(sm.getStatus());
        // if one-byte message, return
        switch (status)
        {
            case 0xF6:			// Tune Request
            case 0xF7:			// EOX
            // System real-time messages
            case 0xF8:			// Timing Clock
            case 0xF9:			// Undefined
            case 0xFA:			// Start
            case 0xFB:			// Continue
            case 0xFC:			// Stop
            case 0xFD:			// Undefined
            case 0xFE:			// Active Sensing
            case 0xFF:
                return res;
        }
        res += ' ' + Yutil.toHex(sm.getData1());
        // if 2-byte message, return
        switch (status)
        {
            case 0xF1:			// MTC Quarter Frame
            case 0xF3:			// Song Select
                return res;
        }
        switch (sm.getCommand())
        {
            case 0xC0:
            case 0xD0:
                return res;
        }
        // 3-byte messages left
        res += ' ' + Yutil.toHex(sm.getData2());
        return res;
    }
}

/**
 * * DumpReceiver.java **
 */