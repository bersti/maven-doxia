/*
 * Copyright (c) 2001 Pixware. 
 *
 * Author: Jean-Yves Belmonte (john@codehaus.org)
 *
 * This file is part of the Pixware doxia package.
 * For conditions of use and distribution, see the attached legal.txt file.
 */

package org.codehaus.doxia.module.rtf;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

public class PBMReader
{

    public static final int TYPE_PBM = 1;
    public static final int TYPE_PGM = 2;
    public static final int TYPE_PPM = 3;

    private static final boolean TRACE = false;

    private static final String BAD_FILE_FORMAT = "bad file format";

    private static final String UNSUPPORTED_TYPE = "unsupported file type";
    private static final String UNSUPPORTED_FORMAT = "unsupported data format";
    private static final String UNSUPPORTED_DEPTH = "unsupported color depth";

    private int type;
    private boolean binary;
    private int width;
    private int height;
    private int maxValue;
    private int bytesPerLine;

    private InputStream stream;

    public PBMReader( String fileName ) throws Exception
    {
        HeaderReader header = new HeaderReader();

        int length = header.read( fileName );
        if ( TRACE ) System.out.println( length );

        if ( type != TYPE_PPM )
            throw new Exception( UNSUPPORTED_TYPE );

        if ( !binary )
            throw new Exception( UNSUPPORTED_FORMAT );

        if ( maxValue > 255 )
            throw new Exception( UNSUPPORTED_DEPTH );

        switch ( type )
        {
            case TYPE_PBM:
                bytesPerLine = ( width + 7 ) / 8;
                break;
            case TYPE_PGM:
                bytesPerLine = width;
                break;
            case TYPE_PPM:
                bytesPerLine = 3 * width;
                break;
        }

        stream = new BufferedInputStream( new FileInputStream( fileName ) );

        skip( length );
    }

    public int type()
    {
        return type;
    }

    public int width()
    {
        return width;
    }

    public int height()
    {
        return height;
    }

    public int maxValue()
    {
        return maxValue;
    }

    public int bytesPerLine()
    {
        return bytesPerLine;
    }

    public long skip( long count ) throws IOException
    {
        long skipped = stream.skip( count );

        if ( skipped < count )
        {
            byte[] b = new byte[512];
            while ( skipped < count )
            {
                int len = (int) Math.min( b.length, ( count - skipped ) );
                int n = stream.read( b, 0, len );
                if ( n < 0 ) break; // end of file
                skipped += n;
            }
        }

        return skipped;
    }

    public int read( byte[] b, int off, int len ) throws IOException
    {
        int count = 0;
        while ( count < len )
        {
            int n = stream.read( b, off + count, len - count );
            if ( n < 0 ) break; // end of file
            count += n;
        }
        return count;
    }

    public static void main( String[] args ) throws Exception
    {
        PBMReader pbm = new PBMReader( args[0] );
    }

    // -----------------------------------------------------------------------

    private class HeaderReader
    {

        private Reader reader;
        private int offset;

        int read( String fileName ) throws Exception
        {
            String field;

            reader = new BufferedReader( new FileReader( fileName ) );
            offset = 0;

            field = getField();
            if ( field.length() != 2 || field.charAt( 0 ) != 'P' )
            {
                reader.close();
                throw new Exception( BAD_FILE_FORMAT );
            }
            switch ( field.charAt( 1 ) )
            {
                case '1':
                case '4':
                    type = TYPE_PBM;
                    break;
                case '2':
                case '5':
                    type = TYPE_PGM;
                    break;
                case '3':
                case '6':
                    type = TYPE_PPM;
                    break;
                default:
                    reader.close();
                    throw new Exception( BAD_FILE_FORMAT );
            }
            if ( field.charAt( 1 ) > '3' )
                binary = true;
            else
                binary = false;

            try
            {
                width = Integer.parseInt( getField() );
                height = Integer.parseInt( getField() );
                if ( type == TYPE_PBM )
                    maxValue = 1;
                else
                    maxValue = Integer.parseInt( getField() );
            }
            catch ( NumberFormatException e )
            {
                reader.close();
                throw new Exception( BAD_FILE_FORMAT );
            }

            reader.close();

            return offset;
        }

        private String getField() throws IOException
        {
            char c;
            StringBuffer field = new StringBuffer();

            try
            {
                do
                {
                    while ( ( c = getChar() ) == '#' )
                        skipComment();
                }
                while ( Character.isWhitespace( c ) );

                field.append( c );

                while ( !Character.isWhitespace( c = getChar() ) )
                {
                    if ( c == '#' )
                    {
                        skipComment();
                        break;
                    }
                    field.append( c );
                }
            }
            catch ( EOFException ignore )
            {
            }

            if ( TRACE ) System.out.println( "\"" + field + "\"" );

            return field.toString();
        }

        private char getChar() throws IOException, EOFException
        {
            int c = reader.read();
            if ( c < 0 ) throw new EOFException();
            offset += 1;
            return (char) c;
        }

        private void skipComment() throws IOException
        {
            try
            {
                while ( getChar() != '\n' ) ;
            }
            catch ( EOFException ignore )
            {
            }
        }

    }

}