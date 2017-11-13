/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mkv.types;

/**
 *
 * @author aziz
 */
public class ByteSequence
        extends Sequence
{

    private final byte[] sequence;

    public ByteSequence(byte[] data)
    {
        sequence = data;
    }

    @Override
    public byte[] data()
    {
        return sequence;
    }

    @Override
    public String asString()
    {
        return new String(sequence);
    }
}
