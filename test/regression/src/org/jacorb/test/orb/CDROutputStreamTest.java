/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1999-2004 Gerald Brose
 *
 *   This library is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU Library General Public
 *   License as published by the Free Software Foundation; either
 *   version 2 of the License, or (at your option) any later version.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *   Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this library; if not, write to the Free
 *   Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package org.jacorb.test.orb;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.jacorb.orb.CDROutputStream;
import org.jacorb.orb.giop.CodeSet;
import org.jacorb.test.common.ORBTestCase;
import org.jacorb.test.common.TestUtils;
import org.omg.CORBA.BAD_PARAM;

import java.util.Arrays;

public class CDROutputStreamTest extends ORBTestCase
{
    private CDROutputStream objectUnderTest;

    protected void doSetUp() throws Exception
    {
        objectUnderTest = new CDROutputStream(orb);
    }

    protected void doTearDown() throws Exception
    {
        objectUnderTest.close();
        objectUnderTest = null;
    }

    public void testIncreaseSize()
    {
        byte[] buffer = new byte[4];
        objectUnderTest.setBuffer(buffer);

        objectUnderTest.increaseSize(8);

        assertTrue(objectUnderTest.size() >= 8);
    }


    /**
     * Verifies that the default encoding (ISO8859_1) works for char, char arrays, and strings. Reading the string
     * forces alignment of the 4-byte length, and ignores any null terminator.
     */
    public void testDefaultEncodingChar() throws Exception {
        objectUnderTest.write_char( 'a' );
        objectUnderTest.write_char( 's' );
        objectUnderTest.write_char( 'd' );
        objectUnderTest.write_char( 'f' );

        objectUnderTest.write_char_array( "wxyz".toCharArray(), 1, 3 );
        objectUnderTest.write_string( "CAFE" );

        byte[] codedText = { 'a', 's', 'd', 'f', 'x', 'y', 'z', 0,
                             0, 0, 0, 5, 'C', 'A', 'F', 'E', 0 };
        assertEquals( "generated bytes", codedText, objectUnderTest.getBufferCopy() );
    }


    /**
     * Verifies that the default encoding (UTF-16) works for wchar, wchar arrays, and wstrings
     * with no byte-order-marker. Reading the wstring
     * forces alignment of the 4-byte length.
     */
    public void testDefaultEncodingWChar() throws Exception {
        byte[] codedText = { 2, 0x5, (byte) (0xD0 & 0xff),  // Hebrew letter aleph
                             2, 0x30, 0x51,                 // Hiragana syllable ha
                             2, 0x30, 0x74,                 // Hiragana syllable pi
                             2, 0x5, (byte) (0xD1 & 0xff),  // Hebrew letter beis
                             2, 0x5, (byte) (0xD2 & 0xff),  // Hebrew letter gimmel
                             2, 0x5, (byte) (0xD3 & 0xff),  // Hebrew letter dalet
                             0, 0,                          // bytes ignored by 'long' alignment
                             0, 0, 0, 8,                    // string length in bytes, not chars
                             0x30, (byte) (0xDF & 0xff),    // Mitsubishi, in Katakana
                             0x30, (byte) (0xC4 & 0xff),
                             0x30, (byte) (0xFA & 0xff),
                             0x30, (byte) (0xB7 & 0xff),
                           };
        objectUnderTest.write_wchar( '\u05D0' );
        objectUnderTest.write_wchar( '\u3051' );
        objectUnderTest.write_wchar( '\u3074' );

        objectUnderTest.write_wchar_array( "\u05D0\u05D1\u05D2\u05D3".toCharArray(), 1, 3 );
        objectUnderTest.write_wstring( "\u30DF\u30C4\u30FA\u30B7" );

        assertEquals( "generated bytes", codedText, objectUnderTest.getBufferCopy() );
    }




    /**
     * Verifies that the UCS-2 encoding works for wchar, wchar arrays, and wstrings. Unlike UTF-16,
     * the byte-order-marker is not optional.
     */
    public void testUCS2EncodingWChar() throws Exception {
        byte[] bom = { (byte) (0xFE & 0xff), (byte) (0xFF & 0xff) };
        byte[] codedText = { 2, bom[0], bom[1], 0x5, (byte) (0xD0 & 0xff), // Hebrew letter aleph
                             2, bom[0], bom[1], 0x30, 0x51,                // Hiragana syllable ha
                             2, bom[0], bom[1], 0x30, 0x74,                // Hiragana syllable pi
                             2, bom[0], bom[1], 0x5, (byte) (0xD1 & 0xff), // Hebrew letter beis
                             2, bom[0], bom[1], 0x5, (byte) (0xD2 & 0xff), // Hebrew letter gimmel
                             2, bom[0], bom[1], 0x5, (byte) (0xD3 & 0xff), // Hebrew letter dalet
                             0, 0,                                         // bytes ignored by 'long' alignment
                             0, 0, 0, 0xa,                                 // string length in bytes, not chars (includes BOM)
                             bom[0], bom[1],                               // byte-order-marker for string
                             0x30, (byte) (0xDF & 0xff),                   // Mitsubishi, in Katakana
                             0x30, (byte) (0xC4 & 0xff),
                             0x30, (byte) (0xFA & 0xff),
                             0x30, (byte) (0xB7 & 0xff),
                           };
        selectCodeSets( "UTF8", "UCS2" );
        objectUnderTest.write_wchar( '\u05D0' );
        objectUnderTest.write_wchar( '\u3051' );
        objectUnderTest.write_wchar( '\u3074' );

        objectUnderTest.write_wchar_array( "\u05D0\u05D1\u05D2\u05D3".toCharArray(), 1, 3 );
        objectUnderTest.write_wstring( "\u30DF\u30C4\u30FA\u30B7" );

        assertEquals( "generated bytes", codedText, objectUnderTest.getBufferCopy() );
    }


    /**
     * Verifies that the default encoding (UTF-16) works for wstrings in giop 1.1, which uses the length
     * indicator to specify the number of characters rather than bytes and require a two-byte null terminator.
     * Wide characters in 1.1 do not take width bytes
     */
    public void testDefaultEncodingWCharGiop1_1() throws Exception {
        byte[] codedText = { 0, 0, 0, 5,                    // string length in bytes, not chars
                             0x30, (byte) (0xDF & 0xff),    // Mitsubishi, in Katakana
                             0x30, (byte) (0xC4 & 0xff),
                             0x30, (byte) (0xFA & 0xff),
                             0x30, (byte) (0xB7 & 0xff),
                             0, 0,                       // two-byte null terminator
                             0x5, (byte) (0xD1 & 0xff),  // Hebrew letter beis
                           };
        objectUnderTest.setGIOPMinor( 1 );
        objectUnderTest.write_wstring( "\u30DF\u30C4\u30FA\u30B7" );
        objectUnderTest.write_wchar( '\u05D1' );

        assertEquals( "generated bytes", codedText, objectUnderTest.getBufferCopy() );
    }


    /**
     * Verifies that the UTF-8 encoding works for strings in giop 1.1.
     */
    public void testUTF8EncodingCharGiop1_1() throws Exception {
        byte[] codedText = { 0, 0, 0, 5,                    // string length in bytes, including null pointer
                             'a', 's', 'd', 'f', 0,         // one-byte null terminator
                             'x'
                           };
        selectCodeSets( "UTF8", "UTF8" );
        objectUnderTest.setGIOPMinor( 1 );
        objectUnderTest.write_string( "asdf" );
        objectUnderTest.write_char( 'x' );

        assertEquals( "generated bytes", codedText, objectUnderTest.getBufferCopy() );
    }


    /**
     * Verifies that the UTF-8 encoding works for strings in giop 1.1. Have to check - this may not be required.
     */
    public void testUTF8EncodingWCharGiop1_1() throws Exception {
        byte[] codedText = { 0, 0, 0, 13,                    // string length in bytes, including null terminator
                             (byte) (0xE3 & 0xff), (byte) (0x83 & 0xff), (byte) (0x9F & 0xff),    // Mitsubishi, in Katakana
                             (byte) (0xE3 & 0xff), (byte) (0x83 & 0xff), (byte) (0x84 & 0xff),
                             (byte) (0xE3 & 0xff), (byte) (0x83 & 0xff), (byte) (0xBA & 0xff),
                             (byte) (0xE3 & 0xff), (byte) (0x82 & 0xff), (byte) (0xB7 & 0xff),
                             0,                                           // one-byte null terminator
                             (byte) (0xD7 & 0xff), (byte) (0x91 & 0xff),  // Hebrew letter beis (w/o length byte)
                           };
        selectCodeSets( "ISO8859_1", "UTF8" );
        objectUnderTest.setGIOPMinor( 1 );
        objectUnderTest.write_wstring( "\u30DF\u30C4\u30FA\u30B7" );
        objectUnderTest.write_wchar( '\u05D1' );

        assertEquals( "generated bytes", codedText, objectUnderTest.getBufferCopy() );
    }


    /**
     * Verifies that the UTF-8 works for wchar, wchar arrays, and wstrings. Reading the wstring
     * forces alignment of the 4-byte length. Note that byte-ordering is fixed by the encoding.
     */
    public void testUTF8EncodingWChar() throws Exception {
        byte[] codedText = { 1, 'x',                                                              // Latin-l lowercase x
                             2, (byte) (0xD7 & 0xff), (byte) (0x90 & 0xff),                       // Hebrew letter aleph
                             3, (byte) (0xE3 & 0xff), (byte) (0x81 & 0xff), (byte) (0x91 & 0xff), // Hiragana syllable ha
                             3, (byte) (0xE3 & 0xff), (byte) (0x81 & 0xff), (byte) (0xB4 & 0xff), // Hiragana syllable pi
                             2, (byte) (0xD7 & 0xff), (byte) (0x91 & 0xff),                       // Hebrew letter beis
                             2, (byte) (0xD7 & 0xff), (byte) (0x92 & 0xff),                       // Hebrew letter gimmel
                             2, (byte) (0xD7 & 0xff), (byte) (0x93 & 0xff),                       // Hebrew letter dalet
                             0, 0,                                                                // bytes ignored by 'long' alignment
                             0, 0, 0, 12,                                                         // string length in bytes, not chars
                             (byte) (0xE3 & 0xff), (byte) (0x83 & 0xff), (byte) (0x9F & 0xff),    // Mitsubishi, in Katakana
                             (byte) (0xE3 & 0xff), (byte) (0x83 & 0xff), (byte) (0x84 & 0xff),
                             (byte) (0xE3 & 0xff), (byte) (0x83 & 0xff), (byte) (0xBA & 0xff),
                             (byte) (0xE3 & 0xff), (byte) (0x82 & 0xff), (byte) (0xB7 & 0xff),
                           };
        selectCodeSets( "ISO8859_1", "UTF8" );
        objectUnderTest.write_wchar( 'x' );
        objectUnderTest.write_wchar( '\u05D0' );
        objectUnderTest.write_wchar( '\u3051' );
        objectUnderTest.write_wchar( '\u3074' );

        objectUnderTest.write_wchar_array( "\u05D0\u05D1\u05D2\u05D3".toCharArray(), 1, 3 );
        objectUnderTest.write_wstring( "\u30DF\u30C4\u30FA\u30B7" );

        assertEquals( "generated bytes", codedText, objectUnderTest.getBufferCopy() );
    }


    private void selectCodeSets( String charCodeSet, String wideCharCodeSet )
    {
        objectUnderTest.setCodeSets( CodeSet.getCodeSet( charCodeSet ), CodeSet.getCodeSet( wideCharCodeSet ) );
    }


    private static void assertEquals( String comment, byte[] expected, byte[] actual ) {
        if (Arrays.equals( expected, actual )) return;

        StringBuffer sb = new StringBuffer( comment );
        append( sb, " expected: ", expected );
        append( sb, "but was: ", actual );
        fail( sb.toString() );
    }

    private static final int BYTES_PER_GROUP = 8;
    private static final int GROUPS_PER_LINE = 8;
    private static final int BYTES_PER_LINE = BYTES_PER_GROUP * GROUPS_PER_LINE;
    private static final int LAST_IN_GROUP = BYTES_PER_GROUP - 1;

    private static void append( StringBuffer sb, String comment, byte[] bytes ) {
        sb.append( comment ).append( '\n' );
        int pos = 0;
        while (pos < bytes.length ) {
            for (int i = 0; i < BYTES_PER_LINE && i < bytes.length - pos; i++) {
                formatOneByte( sb, bytes[i] );
                if ((i % BYTES_PER_GROUP) == LAST_IN_GROUP) sb.append( ' ' );
            }
            sb.append( '\n' );
            pos += BYTES_PER_LINE;
        }
    }


    private static void formatOneByte(StringBuffer sb,  byte aByte ) {
        int value = (int) aByte & 0x0ff;
        if (value < 0x10) sb.append( '0' );
        sb.append( Integer.toHexString( value ) );
    }


    public void testDoesNotLikeNonJacORB()
    {
        org.omg.CORBA.ORB sunORB = org.omg.CORBA.ORB.init(new String[0], TestUtils.newSunORBProperties());

        try
        {
            try
            {
                new CDROutputStream(sunORB);
            }
            catch(BAD_PARAM e)
            {

            }
        }
        finally
        {
            orb.shutdown(true);
        }
    }
}
