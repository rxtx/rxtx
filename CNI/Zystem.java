/*------------------------------------------------------------------------
|   Zystem is a native interface for message reporting in java.
|   Copyright 2003-2004 by Trent Jarvi taj@www.linux.org.uk.
|
|   This library is free software; you can redistribute it and/or
|   modify it under the terms of the GNU Library General Public
|   License as published by the Free Software Foundation; either
|   version 2 of the License, or (at your option) any later version.
|
|   This library is distributed in the hope that it will be useful,
|   but WITHOUT ANY WARRANTY; without even the implied warranty of
|   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
|   Library General Public License for more details.
|
|   You should have received a copy of the GNU Library General Public
|   License along with this library; if not, write to the Free
|   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
--------------------------------------------------------------------------*/
package gnu.io;

import java.io.RandomAccessFile;

public class Zystem
{
	public static final int SILENT_MODE	= 0;
	public static final int FILE_MODE	= 1;
	public static final int NET_MODE	= 2;
	public static final int MEX_MODE	= 3;
	public static final int PRINT_MODE	= 4;
	public static final int J2EE_MSG_MODE   = 5;

	static int mode;

	static
	{
		/* 
		The rxtxZystem library uses Python code and is not
		included with RXTX.  A seperate library will be released
		to avoid potential license conflicts.

		Trent Jarvi taj@www.linux.org.uk
		*/

		//System.loadLibrary( "rxtxZystem" );
		mode = SILENT_MODE;
	}

	private static String target;

	public Zystem( int m ) throws UnSupportedLoggerException
	{
		mode = m;
		startLogger( "asdf" );
	}


	public void startLogger( ) throws UnSupportedLoggerException
	{
		if ( mode == SILENT_MODE || mode == PRINT_MODE )
		{
			//nativeNetInit( );
			return;
		}
		throw new UnSupportedLoggerException( "Target Not Allowed" );
	}

	/*  accept the host or file to log to. */

	public void startLogger( String t ) throws UnSupportedLoggerException
	{
		target = t;
	/*
		if ( mode == NET_MODE )
		{
			nativeNetInit( );
		}
		if ( nativeInit( ) )
		{
			throw new UnSupportedLoggerException(
				"Port initializion failed" );
		}
	*/
		return;
	}

	public void finalize()
	{
	/*
		if ( mode == NET_MODE )
		{
			nativeNetFinalize( );
		}
		nativeFinalize();
	*/
		mode = SILENT_MODE;
		target = null;
	}

	public void filewrite( String s )
	{
		try {
			RandomAccessFile w =
				new RandomAccessFile( target, "rw" );;
			w.seek( w.length() );
			w.writeBytes( s );
			w.close();
		} catch ( Exception e ) {
			System.out.println("Debug output file write failed");
		}
	}

	public boolean report( String s)
	{
		if ( mode == NET_MODE )
		{
		//	return( nativeNetReportln( s ) );
		}
		else if ( mode == PRINT_MODE )
		{
			System.out.println( s );
			return( true );
		}
		else if ( mode == MEX_MODE )
		{
		//	return( nativeMexReport( s ) );
		}
		else if ( mode == SILENT_MODE )
		{
			return( true );
		}
		else if ( mode == FILE_MODE )
		{
			filewrite( s );
		}
		else if ( mode == J2EE_MSG_MODE )
		{
			return( false );
		}
		return( false );
	}

	public boolean reportln( )
	{
		boolean b;
		if ( mode == NET_MODE )
		{
		//	b= nativeNetReportln( "\n" );
		//	return(b);
		}
		else if ( mode == PRINT_MODE )
		{
			System.out.println( );
			return( true );
		}
		else if ( mode == MEX_MODE )
		{
		//	b = nativeMexReportln( "\n" );
		//	return(b);
		}
		else if ( mode == SILENT_MODE )
		{
			return( true );
		}
		else if ( mode == FILE_MODE )
		{
			filewrite( "\n" );
		}
		else if ( mode == J2EE_MSG_MODE )
		{
			return( false );
		}
		return( false );
	}

	public boolean reportln( String s)
	{
		boolean b;
		if ( mode == NET_MODE )
		{
		//	b= nativeNetReportln( s + "\n" );
		//	return(b);
		}
		else if ( mode == PRINT_MODE )
		{
			System.out.println( s );
			return( true );
		}
		else if ( mode == MEX_MODE )
		{
		//	b = nativeMexReportln( s + "\n" );
		//	return(b);
		}
		else if ( mode == SILENT_MODE )
		{
			return( true );
		}
		else if ( mode == FILE_MODE )
		{
			filewrite( s + "\n" );
		}
		else if ( mode == J2EE_MSG_MODE )
		{
			return( false );
		}
		return( false );
	}

/*
	private native boolean nativeInit( );
	private native void nativeFinalize();
*/

	/* open and close the socket */
/*
	private native boolean nativeNetInit( );
	private native void nativeNetFinalize();
*/

	/* dumping to a remote machine */
/*
	public native boolean nativeNetReport( String s );
	public native boolean nativeNetReportln( String s );
*/

	/*  specific to Matlab */
/*
	public native boolean nativeMexReport( String s );
	public native boolean nativeMexReportln( String s );
*/
}
