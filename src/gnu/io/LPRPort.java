/*-------------------------------------------------------------------------
|   rxtx is a native interface to communication ports in java.
|   Copyright 1997-2000 by Trent Jarvi trentjarvi@yahoo.com
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
package javax.comm;

import java.io.*;
import java.util.*;


/**
  * LPRPort
  */
final class LPRPort extends ParallelPort {

	static {
		System.loadLibrary( "Parallel" );
		Initialize();
	}

	/** Initialize the native library */
	private native static void Initialize();

	/** Open the named port */
	public LPRPort( String name ) throws IOException {
		fd = open( name );
	}
	private native int open( String name ) throws IOException;

	/** File descriptor */
	private int fd;

	/** Output stream */
	private final ParallelOutputStream out = new ParallelOutputStream();
	public OutputStream getOutputStream() { return out; }

	/** Input stream */
	private final ParallelInputStream in = new ParallelInputStream();
	public InputStream getInputStream() { return in; }

	/** return current mode LPT_MODE_SPP, LPT_MODE_PS2, LPT_MODE_EPP, 
	    or LPT_MODE_ECP */
	private int lprmode=LPT_MODE_ANY;
	public int getMode() { return lprmode; }
	public int setMode(int mode) {
		try {
			setLPRMode(mode);
		} catch(UnsupportedCommOperationException e) {
			e.printStackTrace();
                        return -1;
		}
		lprmode = mode;
		return(0);
	}
	public void restart(){System.out.println("restart() is not implemented");};
	public void suspend(){System.out.println("suspend() is not implemented");};
	
	public native boolean setLPRMode(int mode) throws UnsupportedCommOperationException;
	public native boolean isPaperOut();
	public native boolean isPrinterBusy();
	public native boolean isPrinterError();
	public native boolean isPrinterSelected();
	public native boolean isPrinterTimedOut();
	
        /** Line speed in bits-per-second */
        private int speed;
        public int getBaudRate() { return speed; }

        /** Data bits port parameter */
        private int dataBits;
        public int getDataBits() { return dataBits; }

	/** Stop bits port parameter */
	private int stopBits;
	public int getStopBits() { return stopBits; }

	/** Parity port parameter */
	private int parity;
	public int getParity() { return parity; }

	/** Close the port */
	private native void nativeClose();
	public void close(){
		nativeClose();
		super.close();
		fd = 0;
	}
	/** Receive framing control */
	public void enableReceiveFraming( int f )
		throws UnsupportedCommOperationException
	{
		throw new UnsupportedCommOperationException( "Not supported" );
	}
	public void disableReceiveFraming() {}
	public boolean isReceiveFramingEnabled() { return false; }
	public int getReceiveFramingByte() { return 0; }

	/** Receive timeout control */
	private int timeout = 0;
	public void enableReceiveTimeout( int t ) {
		if( t > 0 ) timeout = t;
		else timeout = 0;
	}
	public void disableReceiveTimeout() { timeout = 0; }
	public boolean isReceiveTimeoutEnabled() { return timeout > 0; }
	public int getReceiveTimeout() { return timeout; }

	/** Receive threshold control */
	private int threshold = 1;
	public void enableReceiveThreshold( int t ) {
		if( t > 1 ) threshold = t;
		else threshold = 1;
	}
	public void disableReceiveThreshold() { threshold = 1; }
	public int getReceiveThreshold() { return threshold; }
	public boolean isReceiveThresholdEnabled() { return threshold > 1; };

	/** 
		Input/output buffers
		These are native stubs...
	*/

	public native void setInputBufferSize( int size );
	public native int getInputBufferSize(); 
	public native void setOutputBufferSize( int size );
	public native int getOutputBufferSize();

	public native int getOutputBufferFree();
	/** Write to the port */
	private native void writeByte( int b ) throws IOException;
	private native void writeArray( byte b[], int off, int len )
		throws IOException;
	private native void drain() throws IOException;

	/** LPRPort read methods */
	private native int nativeavailable() throws IOException;
	private native int readByte() throws IOException;
	private native int readArray( byte b[], int off, int len ) 
		throws IOException;

	/** Parallel Port Event listener */
	private ParallelPortEventListener PPEventListener;

	/** Thread to monitor data */
	private MonitorThread monThread;

	/** Process ParallelPortEvents */
	native void eventLoop();
	void sendEvent( int event, boolean state ) {
		switch( event ) {
			case ParallelPortEvent.PAR_EV_BUFFER:
				if(  monThread.monBuffer ) break;
				return;
			case ParallelPortEvent.PAR_EV_ERROR:
				if(  monThread.monError ) break;
				return;
			default:
		}
		ParallelPortEvent e = new ParallelPortEvent(this, event, !state, state );
		if( PPEventListener != null ) PPEventListener.parallelEvent( e );
	}

	/** Add an event listener */
	public void addEventListener( ParallelPortEventListener lsnr )
		throws TooManyListenersException
	{
		if( PPEventListener != null ) throw new TooManyListenersException();
		PPEventListener = lsnr;
		monThread = new MonitorThread();
		monThread.start();
	}

	/** Remove the parallel port event listener */
	public void removeEventListener() {
		PPEventListener = null;
		if( monThread != null ) {
			monThread.interrupt();
			monThread = null;
		}
	}

	/** Note: these have to be separate boolean flags because the
	   ParallelPortEvent constants are NOT bit-flags, they are just
	   defined as integers from 1 to 10  -DPL */
	public void notifyOnError( boolean enable ) { 
		System.out.println("notifyOnError is not implemented yet");
		monThread.monError = enable; 
	}
	public void notifyOnBuffer( boolean enable ) { 
		System.out.println("notifyOnBuffer is not implemented yet");
		monThread.monBuffer = enable; 
	}


	/** Finalize the port */
	protected void finalize() {
		if ( fd > 0 ) close();
	}

        /** Inner class for ParallelOutputStream */
        class ParallelOutputStream extends OutputStream {
                public void write( int b ) throws IOException {
                        writeByte( b );
                }
                public void write( byte b[] ) throws IOException {
                        writeArray( b, 0, b.length );
                }
                public void write( byte b[], int off, int len ) throws IOException {
                        writeArray( b, off, len );
                }
                public void flush() throws IOException {
                        //drain();
                }
        }

	/** Inner class for ParallelInputStream */
	class ParallelInputStream extends InputStream {
		public int read() throws IOException {
			return readByte();
		}
		public int read( byte b[] ) throws IOException {
			return readArray( b, 0, b.length );
		}
		public int read( byte b[], int off, int len ) throws IOException {
			return readArray( b, off, len );
		}
		public int available() throws IOException {
			return nativeavailable();
		}
	}
class MonitorThread extends Thread {
	private boolean monError = false;
	private boolean monBuffer = false; 
                MonitorThread() { }
                public void run() {
                        eventLoop();
                }
	}
}
