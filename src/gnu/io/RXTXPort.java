/*-------------------------------------------------------------------------
|   rxtx is a native interface to serial ports in java.
|   Copyright 1997-2001 by Trent Jarvi trentjarvi@yahoo.com.
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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.TooManyListenersException;
import java.lang.Math;

/**
* An extension of gnu.io.SerialPort
* @see gnu.io.SerialPort
*/

final class RXTXPort extends SerialPort
{

	private static boolean debug = false;
	static
	{
		if(debug ) 
			System.out.println("RXTXPort {}");
		System.loadLibrary( "Serial" );
		Initialize();
	}


	/** Initialize the native library */
	private native static void Initialize();

	/** 
	*  Open the named port
	*  @param name the name of the device to open
	*  @throws  PortInUseException
	*  @see gnu.io.SerialPort
	*/
	public RXTXPort( String name ) throws gnu.io.PortInUseException
	{
		if (debug)
			System.out.println("RXTXPort:RXTXPort("+name+")");
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
		throws gnu.io.PortInUseException;

	/** File descriptor */
	private int fd = 0;

	/** DSR flag **/
	static boolean dsrFlag = false;

	/** Output stream */
	private final SerialOutputStream out = new SerialOutputStream();
	/** 
	*  get the OutputStream
	*  @return OutputStream
	*/
	public OutputStream getOutputStream() { return out; }

	/** Input stream */
	private final SerialInputStream in = new SerialInputStream();
	/** 
	*  get the InputStream
	*  @return InputStream
	*  @see java.io.InputStream
	*/
	public InputStream getInputStream() { return in; }

	/** 
	*  Set the SerialPort parameters
	*  1.5 stop bits requires 5 databits
	*  @param  b baudrate
	*  @param  d databits
	*  @param  s stopbits
	*  @param  p parity
	*  @throws UnsupportedCommOperationException
	*  @see gnu.io.UnsupportedCommOperationException
	*/
	public void setSerialPortParams( int b, int d, int s, int p )
		throws UnsupportedCommOperationException
	{
		if (debug)
			System.out.println("RXTXPort:setSerialPortParams()");
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
	/** 
	*  @return  int representing the baudrate
	*/
	public int getBaudRate() { return speed; }

	/** Data bits port parameter */
	private int dataBits=DATABITS_8;
	/** 
	*  @return int representing the databits
	*/
	public int getDataBits() { return dataBits; }

	/** Stop bits port parameter */
	private int stopBits=SerialPort.STOPBITS_1;
	/** 
	*  @return int representing the stopbits
	*/
	public int getStopBits() { return stopBits; }

	/** Parity port parameter */
	private int parity= SerialPort.PARITY_NONE;
	/** 
	*  @return int representing the parity
	*/
	public int getParity() { return parity; }


	/** Flow control */
	private int flowmode = SerialPort.FLOWCONTROL_NONE;
	/** 
	*  @param  flowcontrol FLOWCONTROL_NONE is default
	*  @see gnu.io.SerialPort#FLOWCONTROL_NONE
	*/
	public void setFlowControlMode( int flowcontrol )
	{
		if (debug)
			System.out.println("RXTXPort:setFlowControlMode()");
		try { setflowcontrol( flowcontrol ); }
		catch( IOException e )
		{
			e.printStackTrace();
			return;
		}
		flowmode=flowcontrol;
	}
	/** 
	*  @return  int representing the flowmode
	*/
	public int getFlowControlMode() { return flowmode; }
	native void setflowcontrol( int flowcontrol ) throws IOException;


	/*
	linux/drivers/char/n_hdlc.c? FIXME
		trentjarvi@yahoo.com
	*/
	/**
	*  Receive framing control
	*  @param  f framming
	*  @throws UnsupportedCommOperationException
	*/
	public void enableReceiveFraming( int f )
		throws UnsupportedCommOperationException
	{
		if (debug)
			System.out.println("RXTXPort:enableReceiveFramming()");
		throw new UnsupportedCommOperationException( "Not supported" );
	}
	/** 
	*/
	public void disableReceiveFraming()
	{
		if (debug)
			System.out.println("RXTXPort:disableReceiveFramming()");
	}
	/** 
	*  @returns true if framing is enabled
	*/
	public boolean isReceiveFramingEnabled()
	{
		if (debug)
			System.out.println("RXTXPort:isReceiveFrammingEnabled()");
		return false;
	}
	/** 
	*  @return  int representing the framing byte
	*/
	public int getReceiveFramingByte()
	{
		if (debug)
			System.out.println("RXTXPort:getReceiveFrammingByte()");
		return 0;
	}


	/** Receive timeout control */
	private int timeout = 0;

	/** 
	*  @return  int the timeout
	*/
	public native int NativegetReceiveTimeout();
	/** 
	*  @return  bloolean true if recieve timeout is enabled
	*/
	public native boolean NativeisReceiveTimeoutEnabled();
	/** 
	*  @param  time
	*  @param  threshold
	*  @param  InputBuffer
	*/
	public native void NativeEnableReceiveTimeoutThreshold(int time,
		int threshold,int InputBuffer);
	/** 
	*/
	public void disableReceiveTimeout()
	{
		if (debug)
			System.out.println("RXTXPort:disableReceiveTimeout()");
		enableReceiveTimeout(0);
	}
	/** 
	*  @param time
	*/
	public void enableReceiveTimeout( int time )
	{
		if (debug)
			System.out.println("RXTXPort:enableReceiveTimeout()");
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
	/** 
	*  @return  boolean true if recieve timeout is enabled
	*/
	public boolean isReceiveTimeoutEnabled()
	{
		if (debug)
			System.out.println("RXTXPort:isReceiveTimeoutEnabled()");
		return(NativeisReceiveTimeoutEnabled());
	}
	/** 
	*  @return  int the timeout
	*/
	public int getReceiveTimeout()
	{
		if (debug)
			System.out.println("RXTXPort:getReceiveTimeout()");
		return(NativegetReceiveTimeout( ));
	}

	/** Receive threshold control */

	private int threshold = 0;

	/** 
	*  @param thresh threshold
	*/
	public void enableReceiveThreshold( int thresh )
	{
		if (debug)
			System.out.println("RXTXPort:enableReceiveThreshold()");
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
	/** 
	*/
	public void disableReceiveThreshold()
	{
		if (debug)
			System.out.println("RXTXPort:disableReceiveThreshold()");
		enableReceiveThreshold(0);
	}
	/** 
	*  @return  int the recieve threshold
	*/
	public int getReceiveThreshold()
	{
		if (debug)
			System.out.println("RXTXPort:getReceiveThreshold()");
		return threshold;
	}
	/** 
	*  @return  boolean true if receive threshold is enabled
	*/
	public boolean isReceiveThresholdEnabled()
	{
		if (debug)
			System.out.println("RXTXPort:isReceiveThresholdEnable()");
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
	/** 
	*  @param size
	*/
	public void setInputBufferSize( int size )
	{
		if (debug)
			System.out.println("RXTXPort:setInputBufferSize( " +
					size + ")");
		if( size < 0 )
			throw new IllegalArgumentException
			(
				"Unexpected negative buffer size value"
			);
		else InputBuffer=size;
	}
	/** 
	*/
	public int getInputBufferSize()
	{
		if (debug)
			System.out.println("RXTXPort:getInputBufferSize()");
		return(InputBuffer);
	}
	/** 
	*  @param size
	*/
	public void setOutputBufferSize( int size )
	{
		if (debug)
			System.out.println("RXTXPort:setOutputBufferSize( " +
					size + ")");
		if( size < 0 )
			throw new IllegalArgumentException
			(
				"Unexpected negative buffer size value"
			);
		else OutputBuffer=size;
	}
	/** 
	*  @return  in the output buffer size
	*/
	public int getOutputBufferSize()
	{
		if (debug)
			System.out.println("RXTXPort:getOutputBufferSize()");
		return(OutputBuffer);
	}

	/**
	*  Line status methods
	*/
	/**
	*  @returns true if DTR is set
	*/
	public native boolean isDTR();
	/** 
	*  @param state
	*/
	public native void setDTR( boolean state );
	/** 
	*  @param state
	*/
	public native void setRTS( boolean state );
	private native void setDSR( boolean state );
	/** 
	*  @return boolean true if CTS is set
	*/
	public native boolean isCTS();
	/** 
	*  @return boolean true if DSR is set
	*/
	public native boolean isDSR();
	/** 
	*  @return boolean true if CD is set
	*/
	public native boolean isCD();
	/** 
	*  @return boolean true if RI is set
	*/
	public native boolean isRI();
	/** 
	*  @return boolean true if RTS is set
	*/
	public native boolean isRTS();


	/**
	*  Write to the port
	*  @param duration
	*/
	public native void sendBreak( int duration );
	private native void writeByte( int b ) throws IOException;
	private native void writeArray( byte b[], int off, int len )
		throws IOException;
	private native void nativeDrain() throws IOException;

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

	/** 
	*  @return boolean  true if monitor thread is interrupted
	*/
	boolean monThreadisInterrupted=true;
	public boolean checkMonitorThread()
	{
		if (debug)
			System.out.println("RXTXPort:checkMonitorThread()");
		if(monThread != null)
		{
			if ( monThreadisInterrupted )
				System.out.println(
					"monThreadisInterrupted = " +
					monThreadisInterrupted );
			return monThreadisInterrupted;
		}
		if ( debug )
			System.out.println( "monThread is null " );
		return(true);
	}

	/** 
	*  @param event
	*  @param state
	*  @return boolean true if the port is closing
	*/
	public synchronized boolean sendEvent( int event, boolean state )
	{

		if (debug)
			System.out.print("RXTXPort:sendEvent(");
		/* Let the native side know its time to die */

		if ( fd == 0 || SPEventListener == null || monThread == null)
		{
			return(true);
		}

		switch( event )
		{
			case SerialPortEvent.DATA_AVAILABLE:
				if( debug )
					System.out.println( "DATA_AVAILABLE " +
						monThread.Data + ")" );
				break;
			case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
				if( debug )
					System.out.println( "OUTPUT_BUF_EMPTY " +
						monThread.Output + ")" );
				break;
			case SerialPortEvent.CTS:
				if( debug )
					System.out.println( "CTS " +
						monThread.Output + ")" );
				break;
			case SerialPortEvent.DSR:
				if( debug )
					System.out.println( "DSR " +
						monThread.Output + ")" );
				break;
			case SerialPortEvent.RI:
				if( debug )
					System.out.println( "RI " +
						monThread.Output + ")" );
				break;
			case SerialPortEvent.CD:
				if( debug )
					System.out.println( "CD " +
						monThread.Output + ")" );
				break;
			case SerialPortEvent.OE:
				if( debug )
					System.out.println( "OE " +
						monThread.Output + ")" );
				break;
			case SerialPortEvent.PE:
				if( debug )
					System.out.println( "PE " +
						monThread.Output + ")" );
				break;
			case SerialPortEvent.FE:
				if( debug )
					System.out.println( "FE " +
						monThread.Output + ")" );
				break;
			case SerialPortEvent.BI:
				if( debug )
					System.out.println( "BI " +
						monThread.Output + ")" );
				break;
			default:
				if( debug )
					System.out.println( "XXXXXXXXXXXXXXXX " +
						event + ")" );
				break;
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
			try{
				Thread.sleep(50);
			} catch(Exception exc){}
			return(false);  
		}
	}

	/**
	*  Add an event listener
	*  @param lsnr SerialPortEventListener
	*  @throws TooManyListenersException
	*/
	public synchronized void addEventListener(
		SerialPortEventListener lsnr ) throws TooManyListenersException
	{
		if (debug)
			System.out.println("RXTXPort:addEventListener()");
		if( SPEventListener != null )
			throw new TooManyListenersException();
		SPEventListener = lsnr;
		if (debug)
			System.out.println("RXTXPort:Interrupt=true");
		monThreadisInterrupted=false;
		monThread = new MonitorThread();
		monThread.setDaemon(true);
		monThread.start();
	}
	/**
	*  Remove the serial port event listener
	*/
	
	public void removeEventListener()
	{
		if (debug)
			System.out.println("RXTXPort:removeEventListener()");
		if( monThread != null && monThread.isAlive() )
		{
			if (debug)
				System.out.println("RXTXPort:Interrupt=true");
			monThreadisInterrupted=true;
			try {
				monThread.join(1000);
			} catch (Exception ex) {
				/* yikes */
				ex.printStackTrace();
			}
		}
		monThread = null;
		SPEventListener = null;
		Runtime.getRuntime().gc();
	}

	/**
	*  @param enable
	*/
	public synchronized void notifyOnDataAvailable( boolean enable )
	{
		if (debug)
			System.out.println("RXTXPort:notifyOnDataAvailable()");
		monThread.Data = enable;
	}

	/**
	*  @param enable
	*/
	public synchronized void notifyOnOutputEmpty( boolean enable )
	{
		if (debug)
			System.out.println("RXTXPort:notifyOnOutputEmpty()");
		monThread.Output = enable;
	}

	/**
	*  @param enable
	*/
	public synchronized void notifyOnCTS( boolean enable )
	{
		if (debug)
			System.out.println("RXTXPort:notifyOnCTS()");
		monThread.CTS = enable;
	}
	/**
	*  @param enable
	*/
	public synchronized void notifyOnDSR( boolean enable )
	{
		if (debug)
			System.out.println("RXTXPort:notifyOnDSR()");
		monThread.DSR = enable;
	}
	/**
	*  @param enable
	*/
	public synchronized void notifyOnRingIndicator( boolean enable )
	{
		if (debug)
			System.out.println("RXTXPort:notifyOnRingIndicator()");
		monThread.RI = enable;
	}
	/**
	*  @param enable
	*/
	public synchronized void notifyOnCarrierDetect( boolean enable )
	{
		if (debug)
			System.out.println("RXTXPort:notifyOnCarrierDetect()");
		monThread.CD = enable;
	}
	/**
	*  @param enable
	*/
	public synchronized void notifyOnOverrunError( boolean enable )
	{
		if (debug)
			System.out.println("RXTXPort:notifyOnOverrunError()");
		monThread.OE = enable;
	}
	/**
	*  @param enable
	*/
	public synchronized void notifyOnParityError( boolean enable )
	{
		if (debug)
			System.out.println("RXTXPort:notifyOnParityError()");
		monThread.PE = enable;
	}
	/**
	*  @param enable
	*/
	public synchronized void notifyOnFramingError( boolean enable )
	{
		if (debug)
			System.out.println("RXTXPort:notifyOnFramingError()");
		monThread.FE = enable;
	}
	/**
	*  @param enable
	*/
	public synchronized void notifyOnBreakInterrupt( boolean enable )
	{
		if (debug)
			System.out.println("RXTXPort:notifyOnBreakInterrupt()");
		monThread.BI = enable;
	}


	/** Close the port */
	private native void nativeClose( String name );
	/**
	*/
	public synchronized void close()
	{
		if (debug)
			System.out.println("RXTXPort:close(" + this.name + " )"); 
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
		if (debug)
			System.out.println("RXTXPort:finalize()");
		if( fd > 0 ) close();
	}

	/** Inner class for SerialOutputStream */
	class SerialOutputStream extends OutputStream
	{
	/**
	*  @param b
	*  @throws IOException
	*/
                public synchronized void write( int b ) throws IOException
		{
			if (debug)
				System.out.println("RXTXPort:SerialOutputStream:write(int)");
			if ( fd == 0 ) throw new IOException();
                        writeByte( b );
                }
	/**
	*  @param b[]
	*  @throws IOException
	*/
                public synchronized void write( byte b[] ) throws IOException
		{
			if (debug)
			{
				System.out.println("::::: Entering RXTXPort:SerialOutputStream:write(" +b.length +")" );
				System.out.println("RXTXPort:SerialOutputStream:write() data = " + new String(b) );
			}
			if ( fd == 0 ) throw new IOException();
                        writeArray( b, 0, b.length );
			if (debug)
				System.out.println("::::: Leaving RXTXPort:SerialOutputStream:write(" +b.length +")");
                }
	/**
	*  @param b[]
	*  @param off
	*  @param len
	*  @throws IOException
	*/
                public synchronized void write( byte b[], int off, int len )
			throws IOException
		{
			if( off + len  > b.length )
			{
				throw new IndexOutOfBoundsException(
					"Invalid offset/length passed to read"
				);
			}
 
			byte send[] = new byte[len];
			System.arraycopy( b, off, send, 0, len );
			if (debug)
			{
				System.out.println("::::: Entering RXTXPort:SerialOutputStream:write(" + send.length + " " + off + " " + len + " " +")" +  new String(send) );
				System.out.println("RXTXPort:SerialOutputStream:write() data = " +  new String(send) );
			}
			if ( fd == 0 ) throw new IOException();
                        writeArray( send, 0, len );
			if( debug )
				System.out.println("::::: Leaving RXTXPort:SerialOutputStream:write(" + send.length + " " + off + " " + len + " " +")" +  new String(send) );
                }
	/**
	*/
                //public synchronized void flush() throws IOException
                public void flush() throws IOException
		{
			if (debug)
				System.out.println("RXTXPort:SerialOutputStream:flush() enter");
			if ( fd == 0 ) throw new IOException();
                        nativeDrain();
			Thread.yield();
			if (debug)
				System.out.println("RXTXPort:SerialOutputStream:flush() leave");
		}
	}

	/** Inner class for SerialInputStream */
	class SerialInputStream extends InputStream
	{
	/**
	*  @return int the int read
	*  @throws IOException
	*  @see java.io.InputStream
	*/
		public synchronized int read() throws IOException
		{
			if (debug)
				System.out.println("RXTXPort:SerialInputStream:read()");
			if ( fd == 0 ) throw new IOException();
			return readByte();
		}
	/**
	*  @param b[]
	*  @return int  number of bytes read
	*  @throws IOException
	*/
		public synchronized int read( byte b[] ) throws IOException
		{
			if (debug)
				System.out.println("RXTXPort:SerialInputStream:read(" + b.length + ")");
			return read ( b, 0, b.length);
		}
/*
read(byte b[], int, int)
Documentation is at http://java.sun.com/products/jdk/1.2/docs/api/java/io/InputStream.html#read(byte[], int, int)
*/
	/**
	*  @param b[]
	*  @param off
	*  @param len
	*  @return int  number of bytes read
	*  @throws IOException
	*/
		public synchronized int read( byte b[], int off, int len )
			throws IOException
		{
			if (debug)
				System.out.println("RXTXPort:SerialInputStream:read(" + b.length + " " + off + " " + len + ")");
			int result;
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
			result = readArray( b, off, Minimum);
			if (debug)
				System.out.println("RXTXPort:SerialInputStream:read(" + b.length + " " + off + " " + len + ") = " + result + " bytes containing "  + new String(b) );
			return( result );
		}
	/**
	*  @return int bytes available
	*  @throws IOException
	*/
		public int available() throws IOException
		{
			int r = nativeavailable();
			if ( debug && r > 0 )
				System.out.println("available() returning " +
					r );
			return r;
		}
	}
	/**
	*/
	class MonitorThread extends Thread
	{
	/** Note: these have to be separate boolean flags because the
	   SerialPortEvent constants are NOT bit-flags, they are just
	   defined as integers from 1 to 10  -DPL */
		private volatile boolean CTS=false;
		private volatile boolean DSR = false;
		private volatile boolean RI = false;
		private volatile boolean CD=false;
		private volatile boolean OE=false;
		private volatile boolean PE=false;
		private volatile boolean FE=false;
		private volatile boolean BI=false;
		private volatile boolean Data=false;
		private volatile boolean Output=false;

		MonitorThread() 
		{
			if (debug)
				System.out.println("RXTXPort:MontitorThread:MonitorThread()"); 
		}
	/**
	*  run the thread and call the event loop.
	*/
		public void run()
		{
			if (debug)
				System.out.println("RXTXPort:MontitorThread:run()"); 
			if( monThreadisInterrupted )
			{
				System.out.println("eventLoop is interrupted?");
			}
			eventLoop();
			if (debug)
				System.out.println("eventLoop() returned"); 
		}
		protected void finalize() throws Throwable 
		{ 
			if (debug)
				System.out.println("MonitorThread finalizing"); 
		}
	}
	/**
	*  A dummy method added so RXTX compiles on Kaffee
	*  @deprecated deprecated but used in Kaffe 
	*/
	public void setRcvFifoTrigger(int trigger){};  

/*------------------------  END OF CommAPI -----------------------------*/

	private native boolean nativeSetUartType(String type, boolean test)
		throws UnsupportedCommOperationException;
	native String nativeGetUartType()
		throws UnsupportedCommOperationException;
	private native boolean nativeSetBaudBase(int BaudBase) 
		throws UnsupportedCommOperationException;
	private native int nativeGetBaudBase() 
		throws UnsupportedCommOperationException;
	private native boolean nativeSetDivisor(int Divisor)
		throws UnsupportedCommOperationException;
	private native int nativeGetDivisor()
		throws UnsupportedCommOperationException;
	private native boolean nativeSetLowLatency()
		throws UnsupportedCommOperationException;
	private native boolean nativeGetLowLatency()
		throws UnsupportedCommOperationException;
	private native boolean nativeSetCallOutHangup(boolean NoHup)
		throws UnsupportedCommOperationException;
	private native boolean nativeGetCallOutHangup()
		throws UnsupportedCommOperationException;
	/**
	*  Extension to CommAPI
	*  This is an extension to CommAPI.  It may not be supported on
	*  all operating systems.
	*  @param type String representation of the UART type which mayb
	*  be "none", "8250", "16450", "16550", "16550A", "16650", "16550V2"
	*  or "16750".
	*  @param test boolean flag to determin if the UART should be tested.
	*  @return boolean true on success
	*  @throws UnsupportedCommOperationException;
	*/
	public boolean setUARTType(String type, boolean test)
		throws UnsupportedCommOperationException
	{
		return nativeSetUartType(type, test);
	}
	/**
	*  Extension to CommAPI
	*  This is an extension to CommAPI.  It may not be supported on
	*  all operating systems.
	*  @return type String representation of the UART type which mayb
	*  be "none", "8250", "16450", "16550", "16550A", "16650", "16550V2"
	*  or "16750".
	*  @throws UnsupportedCommOperationException;
	*/
	public String getUARTType() throws UnsupportedCommOperationException
	{
		return nativeGetUartType();
	}

	/**
	*  Extension to CommAPI
	*  @param int BaudBase The clock frequency divided by 16.  Default
	*  BaudBase is 115200.
	*  @return boolean true on success
	*  @throws UnsupportedCommOperationException
	*/

	public boolean setBaudBase(int BaudBase)
		throws UnsupportedCommOperationException
	{
		return nativeSetBaudBase(BaudBase);
	}

	/**
	*  Extension to CommAPI
	*  @return int BaudBase
	*  @throws UnsupportedCommOperationException
	*/

	public int getBaudBase() throws UnsupportedCommOperationException
	{
		return nativeGetBaudBase();
	}

	/**
	*  Extension to CommAPI
	*  @param int Divisor;
	*  @throws UnsupportedCommOperationException
	*/

	public boolean setDivisor(int Divisor)
		throws UnsupportedCommOperationException
	{
		return nativeSetDivisor(Divisor);
	}

	/**
	*  Extension to CommAPI
	*  @returns int Divisor;
	*  @throws UnsupportedCommOperationException
	*/

	public int getDivisor() throws UnsupportedCommOperationException
	{
		return nativeGetDivisor();
	}
	
	/**
	*  Extension to CommAPI
	*  returns boolean true on success
	*  @throws UnsupportedCommOperationException
	*/

	public boolean setLowLatency() throws UnsupportedCommOperationException
	{
		return nativeSetLowLatency();
	}

	/**
	*  Extension to CommAPI
	*  returns boolean true on success
	*  @throws UnsupportedCommOperationException
	*/

	public boolean getLowLatency() throws UnsupportedCommOperationException
	{
		return nativeGetLowLatency();
	}

	/**
	*  Extension to CommAPI
	*  returns boolean true on success
	*  @throws UnsupportedCommOperationException
	*/

	public boolean setCallOutHangup(boolean NoHup)
		throws UnsupportedCommOperationException
	{
		return nativeSetCallOutHangup(NoHup);
	}

	/**
	*  Extension to CommAPI
	*  returns boolean true on success
	*  @throws UnsupportedCommOperationException
	*/

	public boolean getCallOutHangup()
		throws UnsupportedCommOperationException
	{
		return nativeGetCallOutHangup();
	}

/*------------------------  END OF CommAPI Extensions -----------------------*/
}
