/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mkv.filters.pre;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import mkv.types.PreChainFilter;

/**
 *
 * @author aziz
 */
public class URL2LocalFile
        implements PreChainFilter
{

    @Override
    public InputStream scan(InputStream input)
    {
        File file = new File(getClass().getName());
        try (FileOutputStream fos = new FileOutputStream(file))
        {
            byte[] bs = new byte[64];
            int i = 0;
            while (i > -1)
            {
                try
                {
                    i = input.read(bs);
                    if(i > -1)fos.write(bs, 0, i-1);
                }
                catch (IOException ex)
                {
                    ex.printStackTrace();
                }
            }
            fos.flush();
            fos.close();
            
            try
            {
                input.close();
            }
            catch (IOException ex)
            {
                // Fail silently
                Logger.getLogger(URL2LocalFile.class.getName()).log(Level.SEVERE, null, ex);
            }
            return new FileInputStream(file);
        }
        catch (FileNotFoundException ex)
        {
            Logger.getLogger(URL2LocalFile.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (IOException ex)
        {
            Logger.getLogger(URL2LocalFile.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }
}
