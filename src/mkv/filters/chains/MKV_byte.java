/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mkv.filters.chains;

import java.io.IOException;
import java.io.InputStream;
import mkv.types.ByteSequence;
import mkv.types.MKV;

/**
 *
 * @author amwon
 */
public class MKV_byte
        extends MKV
{

    public MKV_byte()
    {
    }

    /**
     *
     * @param input
     * @throws IOException
     */
    @Override
    public void scan(InputStream input) throws IOException
    {
        int i = 0;
        while (i > -1)
        {
            i = input.read();
            if (i > -1)
            {
                scan(new ByteSequence(new byte[]
                {
                    (byte) i
                }));
            }

        }
    }
    
        protected byte[] eolState(){
        return null;
    }
}
