/*-------------------------------------------------------------------------
|   rxtx is a native interface to serial ports in java.
|   Copyright 1997-2000 by Trent Jarvi trentjarvi@yahoo.com.
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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.TooManyListenersException;
import java.lang.Math;

/**
  * RXTXPort
  */

final class RXTXPort extends SerialPort
{

	static
	{
		System.loadLibrary( "Serial" );
		Initialize();
	}


	/** Initialize the native library */
	private native static void Initialize();
	private static boolean debug = false;


	/** Actual SerialPort wrapper class */


	/** Open the named port */
	public RXTXPort( String name ) throws PortInUseException
	{
		if (debug) System.out.println("RXTXPort:RXTXPort("+name+")");
	/* 
	   commapi/javadocs/API_users_guide.html specifies that whenever
	   an application tries to open a port in use by another application
	   the PortInUseException will be thrown

	   I know some didnt like it this way but I'm not sure how to avoid
	   it.  We will just be writing to a bogus fd if we catch the 
	   exeption

	   Trent
	*/
	//	try {
			fd = open( name );
			this.name = name;
	//	} catch ( PortInUseException e ){}
		if (debug)
			System.out.println("RXTXPort:RXTXPort("+name+") fd = " +
				fd);
	}
	private native synchronized int open( String name )
		throws PortInUseException;

	/** File descriptor */
	private int fd = 0;

	/** DSR flag **/
	static boolean dsrFlag = false;

	/** Output stream */
	private final SerialOutputStream out = new SerialOutputStream();
	public OutputStream getOutputStream() { return out; }

	/** Input stream */
	private final SerialInputStream in = new SerialInputStream();
	public InputStream getInputStream() { return in; }

	/** Set the SerialPort parameters
	    1.5 stop bits requires 5 databits  */
	public void setSerialPortParams( int b, int d, int s, int p )
		throws UnsupportedCommOperationException
	{
		nativeSetSerialPortParams( b, d, s, p );
		speed = b;
		if( s== STOPBITS_1_5 ) dataBits = DATABITS_5;
		else dataBits = d;
		stopBits = s;
		parity = p;
	}

	/** Set the native serial port parameters */
	private native void nativeSetSerialPortParams( int speed, int dataBits,
		int stopBits, int parity )
		throws UnsupportedCommOperationException;

	/** Line speed in bits-per-second */
	private int speed=9600;
	public int getBaudRate() { return speed; }

	/** Data bits port parameter */
	private int dataBits=DATABITS_8;
	public int getDataBits() { return dataBits; }

	/** Stop bits port parameter */
	private int stopBits=SerialPort.STOPBITS_1;
	public int getStopBits() { return stopBits; }

	/** Parity port parameter */
	private int parity= SerialPort.PARITY_NONE;
	public int getParity() { return parity; }


	/** Flow control */
	private int flowmode = SerialPort.FLOWCONTROL_NONE;
	public void setFlowControlMode( int flowcontrol )
	{
		try { setflowcontrol( flowcontrol ); }
		catch( IOException e )
		{
			e.printStackTrace();
			return;
		}
		flowmode=flowcontrol;
	}
	public int getFlowControlMode() { return flowmode; }
	native void setflowcontrol( int flowcontrol ) throws IOException;


	/*
	linux/drivers/char/n_hdlc.c? FIXME
		trentjarvi@yahoo.com
	*/
	/** Receive framing control
	*/
	public void enableReceiveFraming( int f )
		throws UnsupportedCommOperationException
	{
		throw new UnsupportedCommOperationException( "Not supported" );
	}
	public void disableReceiveFraming() { }
	public boolean isReceiveFramingEnabled() { return false; }
	public int getReceiveFramingByte() { return 0; }


	/** Receive timeout control */
	private int timeout = 0;

	public native int NativegetReceiveTimeout();
	public native boolean NativeisReceiveTimeoutEnabled();
	public native void NativeEnableReceiveTimeoutThreshold(int time,
		int threshold,int InputBuffer);
	public void disableReceiveTimeout()
	{
		enableReceiveTimeout(0);
	}
	public void enableReceiveTimeout( int time )
	{
		if( time >= 0 )
		{
			timeout = time;
			NativeEnableReceiveTimeoutThreshold( time , threshold,
				InputBuffer );
		}
		else
		{
			throw new IllegalArgumentException
			(
				"Unexpected negative timeout value"
			);
		}
	}
	public boolean isReceiveTimeoutEnabled()
	{
		return(NativeisReceiveTimeoutEnabled());
	}
	public int getReceiveTimeout()
	{
		return(NativegetReceiveTimeout( ));
	}

	/** Receive threshold control */

	private int threshold = 0;

	public void enableReceiveThreshold( int thresh )
	{
		if(thresh >=0)
		{
			threshold=thresh;
			NativeEnableReceiveTimeoutThreshold(timeout, threshold,
				InputBuffer);
		}
		else /* invalid thresh */
		{
			throw new IllegalArgumentException
			(
				"Unexpected negative threshold value"
			);
		}
	}
	public void disableReceiveThreshold()
	{
		enableReceiveThreshold(0);
	}
	public int getReceiveThreshold()
	{
		return threshold;
	}
	public boolean isReceiveThresholdEnabled()
	{
		return(threshold>0);
	}

	/** Input/output buffers */
	/** FIXME I think this refers to
		FOPEN(3)/SETBUF(3)/FREAD(3)/FCLOSE(3)
		trentjarvi@yahoo.com

		These are native stubs...
	*/
	private int InputBuffer=0;
	private int OutputBuffer=0;
	public void setInputBufferSize( int size )
	{
		if( size < 0 )
			throw new IllegalArgumentException
			(
				"Unexpected negative buffer size value"
			);
		else InputBuffer=size;
	}
	public int getInputBufferSize()
	{
		return(InputBuffer);
	}
	public void setOutputBufferSize( int size )
	{
		if( size < 0 )
			throw new IllegalArgumentException
			(
				"Unexpected negative buffer size value"
			);
		else OutputBuffer=size;
	}
	public int getOutputBufferSize()
	{
		return(OutputBuffer);
	}

	/** Line status methods */
	public native boolean isDTR();
	public native void setDTR( boolean state );
	public native void setRTS( boolean state );
	private native void setDSR( boolean state );
	public native boolean isCTS();
	public native boolean isDSR();
	public native boolean isCD();
	public native boolean isRI();
	public native boolean isRTS();


	/** Write to the port */
	public native void sendBreak( int duration );
	private native void writeByte( int b ) throws IOException;
	private native void writeArray( byte b[], int off, int len )
		throws IOException;
	private native void drain() throws IOException;


	/** RXTXPort read methods */
	private native int nativeavailable() throws IOException;
	private native int readByte() throws IOException;
	private native int readArray( byte b[], int off, int len )
		throws IOException;


	/** Serial Port Event listener */
	private SerialPortEventListener SPEventListener;

	/** Thread to monitor data */
	private MonitorThread monThread;

	/** Process SerialPortEvents */
	native void eventLoop();

	public boolean checkMonitorThread() 
	{
		if(monThread != null)
			return monThread.isInterrupted();
		return(true);
	}

	public synchronized boolean sendEvent( int event, boolean state )
	{

		/* Let the native side know its time to die */

		if ( fd == 0 || SPEventListener == null || monThread == null)
		{
			return(true);
		}

		switch( event )
		{
			case SerialPortEvent.DATA_AVAILABLE:
				if( monThread.Data ) break;
				return(false);
			case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
				if( monThread.Output ) break;
				return(false);
/*
				if( monThread.DSR ) break;
				return(false);
				if (isDSR())
				{
					if (!dsrFlag)
					{
						dsrFlag = true;
						SerialPortEvent e = new SerialPortEvent(this, SerialPortEvent.DSR, !dsrFlag, dsrFlag );
					}
				}
				else if (dsrFlag)
				{
					dsrFlag = false;
					SerialPortEvent e = new SerialPortEvent(this, SerialPortEvent.DSR, !dsrFlag, dsrFlag );
				}
*/
			case SerialPortEvent.CTS:
				if( monThread.CTS ) break;
				return(false);
			case SerialPortEvent.DSR:
				if( monThread.DSR ) break;
				return(false);
			case SerialPortEvent.RI:
				if( monThread.RI ) break;
				return(false);
			case SerialPortEvent.CD:
				if( monThread.CD ) break;
				return(false);
			case SerialPortEvent.OE:
				if( monThread.OE ) break;
				return(false);
			case SerialPortEvent.PE:
				if( monThread.PE ) break;
				return(false);
			case SerialPortEvent.FE:
				if( monThread.FE ) break;
				return(false);
			case SerialPortEvent.BI:
				if( monThread.BI ) break;
				return(false);
			default:
				System.err.println("unknown event:"+event);
				return(false);
		}
		SerialPortEvent e = new SerialPortEvent(this, event, !state,
			state );
		if( SPEventListener != null ) SPEventListener.serialEvent( e );


		if (fd == 0 ||  SPEventListener == null || monThread == null) 
		{
			return(true);
		}
		else 
		{
			try{Thread.sleep(50);} catch(Exception exc){}
			return(false);  
		}
	}

	/** Add an event listener */
	public synchronized void addEventListener(
		SerialPortEventListener lsnr ) throws TooManyListenersException
	{
		if( SPEventListener != null )
			throw new TooManyListenersException();
		SPEventListener = lsnr;
		monThread = new MonitorThread();
		monThread.setDaemon(true);
		monThread.start();
	}
	/** Remove the serial port event listener */
	public synchronized void removeEventListener()
	{
		SPEventListener = null;
		if( monThread != null && monThread.isAlive() )
		{
			monThread.interrupt();
			try {
				monThread.join(1000);
			} catch (Exception ex) {
				/* yikes */
				ex.printStackTrace();
			}
		}
		monThread = null;
	}

	public synchronized void notifyOnDataAvailable( boolean enable )
	{
		monThread.Data = enable;
	}

	public synchronized void notifyOnOutputEmpty( boolean enable )
	{
		monThread.Output = enable;
	}

	public synchronized void notifyOnCTS( boolean enable )
	{
		monThread.CTS = enable;
	}
	public synchronized void notifyOnDSR( boolean enable )
	{
		monThread.DSR = enable;
	}
	public synchronized void notifyOnRingIndicator( boolean enable )
	{
		monThread.RI = enable;
	}
	public synchronized void notifyOnCarrierDetect( boolean enable )
	{
		monThread.CD = enable;
	}
	public synchronized void notifyOnOverrunError( boolean enable )
	{
		monThread.OE = enable;
	}
	public synchronized void notifyOnParityError( boolean enable )
	{
		monThread.PE = enable;
	}
	public synchronized void notifyOnFramingError( boolean enable )
	{
		monThread.FE = enable;
	}
	public synchronized void notifyOnBreakInterrupt( boolean enable )
	{
		monThread.BI = enable;
	}


	/** Close the port */
	private native void nativeClose( String name );
	public synchronized void close()
	{
		if ( fd <= 0 ) return;
		setDTR(false);
		setDSR(false);
		nativeClose( this.name );
		super.close();

		removeEventListener();

		fd = 0;
		Runtime.getRuntime().gc();
	}


	/** Finalize the port */
	protected void finalize()
	{
		if( fd > 0 ) close();
	}

	/** Inner class for SerialOutputStream */
	class SerialOutputStream extends OutputStream
	{
                public synchronized void write( int b ) throws IOException
		{
			if ( fd == 0 ) throw new IOException();
                        writeByte( b );
                }
                public synchronized void write( byte b[] ) throws IOException
		{
			if ( fd == 0 ) throw new IOException();
                        writeArray( b, 0, b.length );
                }
                public synchronized void write( byte b[], int off, int len )
			throws IOException
		{
			if ( fd == 0 ) throw new IOException();
                        writeArray( b, off, len );
                }
                public synchronized void flush() throws IOException
		{
			if ( fd == 0 ) throw new IOException();
                        drain();
		}
	}

	/** Inner class for SerialInputStream */
	class SerialInputStream extends InputStream
	{
		public synchronized int read() throws IOException
		{
			if ( fd == 0 ) throw new IOException();
			return readByte();
		}
		public synchronized int read( byte b[] ) throws IOException
		{
			return read ( b, 0, b.length);
		}
/*
read(byte b[], int, int)
Documentation is at http://java.sun.com/products/jdk/1.2/docs/api/java/io/InputStream.html#read(byte[], int, int)
*/
		public synchronized int read( byte b[], int off, int len )
			throws IOException
		{
			/*
			 * Some sanity checks
			 */
			if ( fd == 0 ) throw new IOException();

			if( b==null )
				throw new NullPointerException();

			if( (off < 0) || (len < 0) || (off+len > b.length))
				throw new IndexOutOfBoundsException();

			/*
			 * Return immediately if len==0
			 */
			if( len==0 ) return 0;

			/*
			 * See how many bytes we should read
			 */
			int Minimum = len;

			if( threshold==0 )
			{
			/*
			 * If threshold is disabled, read should return as soon
			 * as data are available (up to the amount of available
			 * bytes in order to avoid blocking)
			 * Read may return earlier depending of the receive time
			 * out.
			 */
				if( available()==0 )
					Minimum = 1;
				else
					Minimum = Math.min(Minimum,available());
			}
			else
			{
			/*
			 * Threshold is enabled. Read should return when
			 * 'threshold' bytes have been received (or when the
			 * receive timeout expired)
			 */
				Minimum = Math.min(Minimum, threshold);
			}
			return readArray( b, off, Minimum);
		}
		public int available() throws IOException
		{
			return nativeavailable();
		}
	}
	class MonitorThread extends Thread
	{
	/** Note: these have to be separate boolean flags because the
	   SerialPortEvent constants are NOT bit-flags, they are just
	   defined as integers from 1 to 10  -DPL */
		private volatile boolean CTS=false;
		private volatile boolean DSR=false;
		private volatile boolean RI=false;
		private volatile boolean CD=false;
		private volatile boolean OE=false;
		private volatile boolean PE=false;
		private volatile boolean FE=false;
		private volatile boolean BI=false;
		private volatile boolean Data=false;
		private volatile boolean Output=false;

		MonitorThread() { }
		public void run()
		{
			eventLoop();
			yield();
			//System.out.println("eventLoop() returned"); 

		}
		protected void finalize() throws Throwable 
		{ 
			//System.out.println("MonitorThread finalizing"); 
		}
	}
}
