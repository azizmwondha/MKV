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
public class StringSequence
        extends Sequence
{

    private final String sequence;

    public StringSequence(String data)
    {
        sequence = data;
    }

    @Override
    public byte[] data()
    {
        return sequence.getBytes();
    }

    @Override
    public String asString()
    {
        return sequence;
    }
}
