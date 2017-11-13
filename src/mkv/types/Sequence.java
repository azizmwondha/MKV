/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mkv.types;

import java.util.Arrays;

/**
 *
 * @author aziz
 */
public abstract class Sequence
{

    public abstract String asString();

    public abstract byte[] data();

    @Override
    public String toString()
    {
        return asString();
    }

    @Override
    public boolean equals(Object o)
    {
        if ((null != o) && (o instanceof Sequence))
        {
            return Arrays.equals(data(), ((Sequence) o).data());
        }
        return false;
    }
}
