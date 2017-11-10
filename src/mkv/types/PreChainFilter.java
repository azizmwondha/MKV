/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mkv.types;

import java.io.InputStream;

/**
 *
 * @author aziz
 */
public interface PreChainFilter
{
    public abstract InputStream scan(InputStream input);
}
