/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sauti.dampa;

/**
 *
 * @author aziz
 */
public class Yutil
{

    public static final char hexDigits[] =
    {
        '0', '1', '2', '3',
        '4', '5', '6', '7',
        '8', '9', 'A', 'B',
        'C', 'D', 'E', 'F'
    };

    static String toHex(int i)
    {
        return "" + hexDigits[(i & 240) >> 4] + hexDigits[i & 15];
    }

    static String toHex(long l)
    {
        int i = (int) l;
        return "" + hexDigits[(i & 61440) >> 12] + hexDigits[(i & 3840) >> 8] + hexDigits[(i & 240) >> 4] + hexDigits[i & 15];
    }

}
