/*-------------------------------------------------------------------------
|   rxtx is a native interface to serial ports in java.
|   Copyright 1997-2002 by Trent Jarvi taj@www.linux.org.uk.
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

final public class RXTXPort extends SerialPort
{
	/* I had a report that some JRE's complain when MonitorThread
	   tries to access private variables
	*/

	protected final static boolean debug = false;
	protected final static boolean debug_read = false;
	protected final static boolean debug_write = false;
	protected final static boolean debug_events = false;
	protected final static boolean debug_verbose = false;
	static
	{
		if(debug ) 
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort {}");
		System.loadLibrary( "rxtxSerial" );
		Initialize();
	}


	/** Initialize the native library */
	private native static void Initialize();
	boolean MonitorThreadAlive=false;

	/** 
	*  Open the named port
	*  @param name the name of the device to open
	*  @throws  PortInUseException
	*  @see gnu.io.SerialPort
	*/
	public RXTXPort( String name ) throws PortInUseException
	{
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:RXTXPort("+name+")");
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

			MonitorThreadLock = true;
			monThread = new MonitorThread();
			monThread.start();
			waitForTheNativeCodeSilly();
			MonitorThreadAlive=true;
	//	} catch ( PortInUseException e ){}
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:RXTXPort("+name+") fd = " +
				fd);
		timeout = -1;	/* default disabled timeout */
	}
	private native synchronized int open( String name )
		throws PortInUseException;

	/** File descriptor */
	private int fd = 0;
	/** a pointer to the event info structure used to share information
	    between threads so write threads can send output buffer empty
	    from a pthread if need be.
	*/
	int eis = 0;
	/** pid for lock files */
	int pid = 0;

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

	*  If speed is not a predifined speed it is assumed to be
	*  the actual speed desired.
	*/
	private native int nativeGetParity( int fd );
	private native int nativeGetFlowControlMode( int fd );
	public synchronized void setSerialPortParams( int b, int d, int s,
		int p )
		throws UnsupportedCommOperationException
	{
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:setSerialPortParams(" +
					b + " " + d + " " + s + " " + p + ")");
		if ( nativeSetSerialPortParams( b, d, s, p ) )
		 	throw new UnsupportedCommOperationException(
				"Invalid Parameter" );
		speed = b;
		if( s== STOPBITS_1_5 ) dataBits = DATABITS_5;
		else dataBits = d;
		stopBits = s;
		parity = p;
	}

	/**
	*  Set the native serial port parameters
	*  If speed is not a predifined speed it is assumed to be
	*  the actual speed desired.
	*/
	private native boolean nativeSetSerialPortParams( int speed,
		int dataBits, int stopBits, int parity )
		throws UnsupportedCommOperationException;

	/** Line speed in bits-per-second */
	private int speed=9600;
	/** 
	*  @return  int representing the baudrate
	*  This will not behave as expected with custom speeds
	*/
	public int getBaudRate()
	{
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:getBaudRate()");
		return speed;
	}

	/** Data bits port parameter */
	private int dataBits=DATABITS_8;
	/** 
	*  @return int representing the databits
	*/
	public int getDataBits()
	{
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:getDataBits()");
		return dataBits;
	}

	/** Stop bits port parameter */
	private int stopBits=SerialPort.STOPBITS_1;
	/** 
	*  @return int representing the stopbits
	*/
	public int getStopBits()
	{
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:getStopBits()");
		return stopBits;
	}

	/** Parity port parameter */
	private int parity= SerialPort.PARITY_NONE;
	/** 
	*  @return int representing the parity
	*/
	public int getParity()
	{
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:getParity()");
		return parity;
	}


	/** Flow control */
	private int flowmode = SerialPort.FLOWCONTROL_NONE;
	/** 
	*  @param  flowcontrol FLOWCONTROL_NONE is default
	*  @see gnu.io.SerialPort#FLOWCONTROL_NONE
	*/
	public void setFlowControlMode( int flowcontrol )
	{
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:setFlowControlMode()");
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
		taj@www.linux.org.uk
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
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:enableReceiveFramming()");
		throw new UnsupportedCommOperationException( "Not supported" );
	}
	/** 
	*/
	public void disableReceiveFraming()
	{
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:disableReceiveFramming()");
	}
	/** 
	*  @returns true if framing is enabled
	*/
	public boolean isReceiveFramingEnabled()
	{
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:isReceiveFrammingEnabled()");
		return false;
	}
	/** 
	*  @return  int representing the framing byte
	*/
	public int getReceiveFramingByte()
	{
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:getReceiveFrammingByte()");
		return 0;
	}


	/** Receive timeout control */
	private int timeout;

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
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:disableReceiveTimeout()");
		timeout = -1;
		NativeEnableReceiveTimeoutThreshold( timeout , threshold, InputBuffer );
	}
	/** 
	*  @param time
	*/
	public void enableReceiveTimeout( int time )
	{
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:enableReceiveTimeout()");
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
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:isReceiveTimeoutEnabled()");
		return(NativeisReceiveTimeoutEnabled());
	}
	/** 
	*  @return  int the timeout
	*/
	public int getReceiveTimeout()
	{
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:getReceiveTimeout()");
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
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:enableReceiveThreshold()");
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
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:disableReceiveThreshold()");
		enableReceiveThreshold(0);
	}
	/** 
	*  @return  int the recieve threshold
	*/
	public int getReceiveThreshold()
	{
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:getReceiveThreshold()");
		return threshold;
	}
	/** 
	*  @return  boolean true if receive threshold is enabled
	*/
	public boolean isReceiveThresholdEnabled()
	{
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:isReceiveThresholdEnable()");
		return(threshold>0);
	}

	/** Input/output buffers */
	/** FIXME I think this refers to
		FOPEN(3)/SETBUF(3)/FREAD(3)/FCLOSE(3)
		taj@www.linux.org.uk

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
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:setInputBufferSize( " +
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
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:getInputBufferSize()");
		return(InputBuffer);
	}
	/** 
	*  @param size
	*/
	public void setOutputBufferSize( int size )
	{
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:setOutputBufferSize( " +
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
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:getOutputBufferSize()");
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
	protected native void writeByte( int b, boolean i ) throws IOException;
	protected native void writeArray( byte b[], int off, int len, boolean i )
		throws IOException;
	protected native boolean nativeDrain( boolean i ) throws IOException;

	/** RXTXPort read methods */
	protected native int nativeavailable() throws IOException;
	protected native int readByte() throws IOException;
	protected native int readArray( byte b[], int off, int len )
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
	private native void interruptEventLoop( );
	public boolean checkMonitorThread()
	{
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:checkMonitorThread()");
		if(monThread != null)
		{
			if ( debug )
				System.out.println(System.currentTimeMillis() + ": " + 
					"monThreadisInterrupted = " +
					monThreadisInterrupted );
			return monThreadisInterrupted;
		}
		if ( debug )
			System.out.println(System.currentTimeMillis() + ": " +  "monThread is null " );
		return(true);
	}

	/** 
	*  @param event
	*  @param state
	*  @return boolean true if the port is closing
	*/
	public boolean sendEvent( int event, boolean state )
	{
		if (debug_events)
			System.out.print(System.currentTimeMillis() + ": " + "RXTXPort:sendEvent(");
		/* Let the native side know its time to die */

		if ( fd == 0 || SPEventListener == null || monThread == null)
		{
			return(true);
		}

		switch( event )
		{
			case SerialPortEvent.DATA_AVAILABLE:
				if( debug_events )
					System.out.println( "DATA_AVAILABLE " +
						monThread.Data + ")" );
				break;
			case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
				if( debug_events )
					System.out.println( 
						"OUTPUT_BUFFER_EMPTY " +
						monThread.Output + ")" );
				break;
			case SerialPortEvent.CTS:
				if( debug_events )
					System.out.println( "CTS " +
						monThread.CTS + ")" );
				break;
			case SerialPortEvent.DSR:
				if( debug_events )
					System.out.println( "DSR " +
						monThread.Output + ")" );
				break;
			case SerialPortEvent.RI:
				if( debug_events )
					System.out.println( "RI " +
						monThread.RI + ")" );
				break;
			case SerialPortEvent.CD:
				if( debug_events )
					System.out.println( "CD " +
						monThread.CD + ")" );
				break;
			case SerialPortEvent.OE:
				if( debug_events )
					System.out.println( "OE " +
						monThread.OE + ")" );
				break;
			case SerialPortEvent.PE:
				if( debug_events )
					System.out.println( "PE " +
						monThread.PE + ")" );
				break;
			case SerialPortEvent.FE:
				if( debug_events )
					System.out.println( "FE " +
						monThread.FE + ")" );
				break;
			case SerialPortEvent.BI:
				if( debug_events )
					System.out.println( "BI " +
						monThread.BI + ")" );
				break;
			default:
				if( debug_events )
					System.out.println( "XXXXXXXXXXXXXX " +
						event + ")" );
				break;
		}
		if( debug_events && debug_verbose )
			System.out.println(System.currentTimeMillis() + ": " +  "checking flags " );

		switch( event )
		{
			case SerialPortEvent.DATA_AVAILABLE:
				if( monThread.Data ) break;
				return(false);
			case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
				if( monThread.Output ) break;
				return(false);
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
				System.err.println(System.currentTimeMillis() + ": " + "unknown event: " + event);
				return(false);
		}
		if( debug_events && debug_verbose )
			System.out.println(System.currentTimeMillis() + ": " +  "getting event" );
		SerialPortEvent e = new SerialPortEvent(this, event, !state,
			state );
		if( debug_events && debug_verbose )
			System.out.println(System.currentTimeMillis() + ": " +  "sending event" );
		if(monThreadisInterrupted) 
		{
			if( debug_events )
				System.out.println(System.currentTimeMillis() + ": " +  "sendEvent return" );
			return(true);
		}
		if( SPEventListener != null )
		{
			SPEventListener.serialEvent( e );
		}

		if( debug_events && debug_verbose )
			System.out.println(System.currentTimeMillis() + ": " +  "sendEvent return" );

		if (fd == 0 ||  SPEventListener == null || monThread == null) 
		{
			return(true);
		}
		else 
		{
			return(false);  
		}
	}

	/**
	*  Add an event listener
	*  @param lsnr SerialPortEventListener
	*  @throws TooManyListenersException
	*/

	boolean MonitorThreadLock = true;
	boolean MonitorThreadCloseLock = true;

	public void addEventListener(
		SerialPortEventListener lsnr ) throws TooManyListenersException
	{
		/*  Don't let and notification requests happen until the
		    Eventloop is ready
		*/

		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:addEventListener()");
		if( SPEventListener != null )
			throw new TooManyListenersException();
		SPEventListener = lsnr;
		if( !MonitorThreadAlive )
		{
			MonitorThreadLock = true;
			monThread = new MonitorThread();
			monThread.start();
			waitForTheNativeCodeSilly();
			MonitorThreadAlive=true;
		}
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:Interrupt=false");
	}
	/**
	*  Remove the serial port event listener
	*/
	public void removeEventListener()
	{
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:removeEventListener()");
		waitForTheNativeCodeSilly();
		//if( monThread != null && monThread.isAlive() )
		if( monThreadisInterrupted == true )
		{
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:removeEventListener() already interrupted");
			monThread = null;
			SPEventListener = null;
			Runtime.getRuntime().gc();
			return;
		}
		else if( monThread != null && monThread.isAlive() )
		{
			if (debug)
				System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:Interrupt=true");
			monThreadisInterrupted=true;
			/*
			   Notify all threads in this PID that something is up
			   They will call back to see if its their thread
			   using isInterrupted().
			*/
			MonitorThreadCloseLock = true;
			if (debug)
				System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:calling interruptEventLoop");
			interruptEventLoop( );
			if (debug)
				System.out.print("RXTXPort:waiting on closelock");
			while( MonitorThreadCloseLock )
			{
				if (debug)
					System.out.print(".");
				try {
					Thread.sleep(100);
				} catch( Exception e ) {}
			}
			if (debug)
				System.out.println();
			if (debug)
				System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:calling monThread.join()");
			try {
				monThread.join(1000);
			} catch (Exception ex) {
				/* yikes */
				ex.printStackTrace();
			}
			if (debug)
				System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:waiting on isAlive()");
			while( monThread.isAlive() )
			{
				if ( debug )
					System.out.println(System.currentTimeMillis() + ": " + "MonThread is still alive!");
				try {
					monThread.join(1000);
					Thread.sleep( 1000 );
				} catch( Exception e ){} 
				monThread.stop();
			}
			
		}
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:calling gc()");
		monThread = null;
		SPEventListener = null;
		Runtime.getRuntime().gc();
		MonitorThreadLock = false;
		MonitorThreadAlive=false;
	}
	/**
	 *	Give the native code a chance to start listening to the hardware
	 *	or should we say give the native code control of the issue.
	 *
	 *	This is important for applications that flicker the Monitor
	 *	thread while keeping the port open.
	 *	In worst case test cases this loops once or twice every time.
	 */

	protected void waitForTheNativeCodeSilly()
	{
		while( MonitorThreadLock )
		{
			try {
				Thread.sleep(100);
			} catch( Exception e ) {}
		}
	}
	/**
	*  @param enable
	*/
	private native void nativeSetEventFlag( int fd, int event,
						boolean flag );
	public void notifyOnDataAvailable( boolean enable )
	{
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:notifyOnDataAvailable( " +
				enable+" )");
		
		waitForTheNativeCodeSilly();

		MonitorThreadLock = true;
		nativeSetEventFlag( fd, SerialPortEvent.DATA_AVAILABLE,
					enable );
		monThread.Data = enable;
		MonitorThreadLock = false;
	}

	/**
	*  @param enable
	*/
	public void notifyOnOutputEmpty( boolean enable )
	{
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:notifyOnOutputEmpty( " +
				enable+" )");
		waitForTheNativeCodeSilly();
		MonitorThreadLock = true;
		nativeSetEventFlag( fd, SerialPortEvent.OUTPUT_BUFFER_EMPTY,
					enable );
		monThread.Output = enable;
		MonitorThreadLock = false;
	}

	/**
	*  @param enable
	*/
	public void notifyOnCTS( boolean enable )
	{
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:notifyOnCTS( " +
				enable+" )");
		waitForTheNativeCodeSilly();
		MonitorThreadLock = true;
		nativeSetEventFlag( fd, SerialPortEvent.CTS, enable );
		monThread.CTS = enable;
		MonitorThreadLock = false;
	}
	/**
	*  @param enable
	*/
	public void notifyOnDSR( boolean enable )
	{
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:notifyOnDSR( " +
				enable+" )");
		waitForTheNativeCodeSilly();
		MonitorThreadLock = true;
		nativeSetEventFlag( fd, SerialPortEvent.DSR, enable );
		monThread.DSR = enable;
		MonitorThreadLock = false;
	}
	/**
	*  @param enable
	*/
	public void notifyOnRingIndicator( boolean enable )
	{
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:notifyOnRingIndicator( " +
				enable+" )");
		waitForTheNativeCodeSilly();
		MonitorThreadLock = true;
		nativeSetEventFlag( fd, SerialPortEvent.RI, enable );
		monThread.RI = enable;
		MonitorThreadLock = false;
	}
	/**
	*  @param enable
	*/
	public void notifyOnCarrierDetect( boolean enable )
	{
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:notifyOnCarrierDetect( " +
				enable+" )");
		waitForTheNativeCodeSilly();
		MonitorThreadLock = true;
		nativeSetEventFlag( fd, SerialPortEvent.CD, enable );
		monThread.CD = enable;
		MonitorThreadLock = false;
	}
	/**
	*  @param enable
	*/
	public void notifyOnOverrunError( boolean enable )
	{
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:notifyOnOverrunError( " +
				enable+" )");
		waitForTheNativeCodeSilly();
		MonitorThreadLock = true;
		nativeSetEventFlag( fd, SerialPortEvent.OE, enable );
		monThread.OE = enable;
		MonitorThreadLock = false;
	}
	/**
	*  @param enable
	*/
	public void notifyOnParityError( boolean enable )
	{
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:notifyOnParityError( " +
				enable+" )");
		waitForTheNativeCodeSilly();
		MonitorThreadLock = true;
		nativeSetEventFlag( fd, SerialPortEvent.PE, enable );
		monThread.PE = enable;
		MonitorThreadLock = false;
	}
	/**
	*  @param enable
	*/
	public void notifyOnFramingError( boolean enable )
	{
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:notifyOnFramingError( " +
				enable+" )");
		waitForTheNativeCodeSilly();
		MonitorThreadLock = true;
		nativeSetEventFlag( fd, SerialPortEvent.FE, enable );
		monThread.FE = enable;
		MonitorThreadLock = false;
	}
	/**
	*  @param enable
	*/
	public void notifyOnBreakInterrupt( boolean enable )
	{
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:notifyOnBreakInterrupt( " +
				enable+" )");
		waitForTheNativeCodeSilly();
		MonitorThreadLock = true;
		nativeSetEventFlag( fd, SerialPortEvent.BI, enable );
		monThread.BI = enable;
		MonitorThreadLock = false;
	}

	/** Close the port */
	private native void nativeClose( String name );
	/**
	*/
	boolean closeLock = false;
	public synchronized void close()
	{
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:close( " + this.name + " )"); 
		if( closeLock ) return;
		closeLock = true;
		if ( fd <= 0 )
		{
			System.out.println( System.currentTimeMillis() + ": " + "RXTXPort:close detected bad File Descriptor" );
			return;
		}
		setDTR(false);
		setDSR(false);
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:close( " + this.name + " ) setting monThreadisInterrupted"); 
		if ( ! monThreadisInterrupted )
		{
			removeEventListener();
		}
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:close( " + this.name + " ) calling nativeClose"); 
		nativeClose( this.name );
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:close( " + this.name + " ) calling super.close"); 
		super.close();
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:close( " + this.name + " ) calling System.gc"); 

		fd = 0;
		Runtime.getRuntime().gc();
		closeLock = false;
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:close( " + this.name + " ) leaving"); 
	}


	/** Finalize the port */
	protected void finalize()
	{
		if (debug)
			System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:finalize()");
		if( fd > 0 ) close();
	}

	/** Inner class for SerialOutputStream */
	class SerialOutputStream extends OutputStream
	{
	/**
	*  @param b
	*  @throws IOException
	*/
		public void write( int b ) throws IOException
		{
			if (debug_write)
				System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:SerialOutputStream:write(int)");
			if( speed == 0 ) return;
	/* hmm this turns out to be a very bad idea
			if ( monThreadisInterrupted == true )
			{
				throw new IOException( "Port has been Closed" );
			}
	*/
			waitForTheNativeCodeSilly();
			if ( fd == 0 ) throw new IOException();
			writeByte( b, monThreadisInterrupted );
			if (debug_write)
				System.out.println(System.currentTimeMillis() + ": " + "Leaving RXTXPort:SerialOutputStream:write( int )");
		}
	/**
	*  @param b[]
	*  @throws IOException
	*/
		public void write( byte b[] ) throws IOException
		{
			if (debug_write)
			{
				System.out.println(System.currentTimeMillis() + ": " + "Entering RXTXPort:SerialOutputStream:write(" + b.length + ") "/* + new String(b)*/ );
			}
			if( speed == 0 ) return;
	/* hmm this turns out to be a very bad idea
			if ( monThreadisInterrupted == true )
			{
				throw new IOException( "Port has been Closed" );
			}
	*/
			if ( fd == 0 ) throw new IOException();
			waitForTheNativeCodeSilly();
			writeArray( b, 0, b.length, monThreadisInterrupted );
			if (debug_write)
				System.out.println(System.currentTimeMillis() + ": " + "Leaving RXTXPort:SerialOutputStream:write(" +b.length  +")");
		}
	/**
	*  @param b[]
	*  @param off
	*  @param len
	*  @throws IOException
	*/
		public void write( byte b[], int off, int len )
			throws IOException
		{
			if( speed == 0 ) return;
			if( off + len  > b.length )
			{
				throw new IndexOutOfBoundsException(
					"Invalid offset/length passed to read"
				);
			}
 
			byte send[] = new byte[len];
			System.arraycopy( b, off, send, 0, len );
			if (debug_write)
			{
				System.out.println(System.currentTimeMillis() + ": " + "Entering RXTXPort:SerialOutputStream:write(" + send.length + " " + off + " " + len + " " +") " /*+  new String(send) */ );
			}
			if ( fd == 0 ) throw new IOException();
	/* hmm this turns out to be a very bad idea
			if ( monThreadisInterrupted == true )
			{
				throw new IOException( "Port has been Closed" );
			}
	*/
			waitForTheNativeCodeSilly();
			writeArray( send, 0, len, monThreadisInterrupted );
			if( debug_write )
				System.out.println(System.currentTimeMillis() + ": " + "Leaving RXTXPort:SerialOutputStream:write(" + send.length + " " + off + " " + len + " " +") "  /*+ new String(send)*/ );
		}
	/**
	*/
		public void flush() throws IOException
		{
			if (debug)
				System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:SerialOutputStream:flush() enter");
			if( speed == 0 ) return;
			if ( fd == 0 ) throw new IOException();
	/* hmm this turns out to be a very bad idea
			if ( monThreadisInterrupted == true )
			{
				return;
				// FIXME Trent this breaks
				//throw new IOException( "flush() Port has been Closed" );
			}
	*/
			waitForTheNativeCodeSilly();
			/* 
			   this is probably good on all OS's but for now
			   just sendEvent from java on Sol
			*/
			if ( nativeDrain( monThreadisInterrupted ) )
				sendEvent( SerialPortEvent.OUTPUT_BUFFER_EMPTY, true );
			if (debug)
				System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:SerialOutputStream:flush() leave");
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
		public int read() throws IOException
		{
			if (debug_read)
				System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:SerialInputStream:read()");
	/* hmm this turns out to be a very bad idea
			if ( monThreadisInterrupted ) return( -1 ) ;
	*/
			if ( fd == 0 ) throw new IOException();
	/* hmm this turns out to be a very bad idea
			if ( monThreadisInterrupted == true )
			{
				throw new IOException( "Port has been Closed" );
			}
	*/
			waitForTheNativeCodeSilly();
			int result = readByte();
			if (debug_read)
				System.out.println(System.currentTimeMillis() + ": " +  "readByte= " + result );
			return( result );
		}
	/**
	*  @param b[]
	*  @return int  number of bytes read
	*  @throws IOException
	*/
		public int read( byte b[] ) throws IOException
		{
			int result;
			if (debug_read)
				System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:SerialInputStream:read(" + b.length + ")");
	/* hmm this turns out to be a very bad idea
			if ( monThreadisInterrupted == true )
			{
				throw new IOException( "Port has been Closed" );
			}
	*/
			waitForTheNativeCodeSilly();
			result = read( b, 0, b.length);
			if (debug_read)
				System.out.println(System.currentTimeMillis() + ": " +  "read = " + result );
			return( result );
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
		public int read( byte b[], int off, int len )
			throws IOException
		{
			if (debug_read)
				System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:SerialInputStream:read(" + b.length + " " + off + " " + len + ") " /*+ new String(b) */ );
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
	/* hmm this turns out to be a very bad idea
			if ( monThreadisInterrupted == true )
			{
				throw new IOException( "Port has been Closed" );
			}
	*/
			waitForTheNativeCodeSilly();
			result = readArray( b, off, Minimum);
			if (debug_read)
				System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:SerialInputStream:read(" + b.length + " " + off + " " + len + ") = " + result + " bytes containing "  /*+ new String(b) */);
			return( result );
		}
	/**
	*  @return int bytes available
	*  @throws IOException
	*/
		public int available() throws IOException
		{
	/* hmm this turns out to be a very bad idea
			if ( monThreadisInterrupted == true )
			{
				throw new IOException( "Port has been Closed" );
			}
	*/
			int r = nativeavailable();
			if ( debug_verbose && r > 0 )
				System.out.println(System.currentTimeMillis() + ": " + "available() returning " +
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
		private volatile boolean DSR=false;
		private volatile boolean RI=false;
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
				System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:MontitorThread:MonitorThread()"); 
		}
	/**
	*  run the thread and call the event loop.
	*/
		public void run()
		{
			if (debug)
				System.out.println(System.currentTimeMillis() + ": " + "RXTXPort:MontitorThread:run()"); 
			monThreadisInterrupted=false;
			eventLoop();

			if (debug)
				System.out.println(System.currentTimeMillis() + ": " + "eventLoop() returned"); 
		}
		protected void finalize() throws Throwable 
		{ 
			if (debug)
				System.out.println(System.currentTimeMillis() + ": " + "MonitorThread finalizing"); 
		}
	}
	/**
	*  A dummy method added so RXTX compiles on Kaffee
	*  @deprecated deprecated but used in Kaffe 
	*/
	public void setRcvFifoTrigger(int trigger){};  

/*------------------------  END OF CommAPI -----------------------------*/

	private native static void nativeStaticSetSerialPortParams( String f,
		int b, int d, int s, int p )
		throws UnsupportedCommOperationException;
	private native static boolean nativeStaticSetDSR( String port,
							boolean flag )
		throws UnsupportedCommOperationException;
	private native static boolean nativeStaticSetDTR( String port,
							boolean flag )
		throws UnsupportedCommOperationException;
	private native static boolean nativeStaticSetRTS( String port,
							boolean flag )
		throws UnsupportedCommOperationException;

	private native static boolean nativeStaticIsDSR( String port )
		throws UnsupportedCommOperationException;
	private native static boolean nativeStaticIsDTR( String port )
		throws UnsupportedCommOperationException;
	private native static boolean nativeStaticIsRTS( String port )
		throws UnsupportedCommOperationException;
	private native static boolean nativeStaticIsCTS( String port )
		throws UnsupportedCommOperationException;
	private native static boolean nativeStaticIsCD( String port )
		throws UnsupportedCommOperationException;
	private native static boolean nativeStaticIsRI( String port )
		throws UnsupportedCommOperationException;

	private native static int nativeStaticGetBaudRate( String port )
		throws UnsupportedCommOperationException;
	private native static int nativeStaticGetDataBits( String port )
		throws UnsupportedCommOperationException;
	private native static int nativeStaticGetParity( String port )
		throws UnsupportedCommOperationException;
	private native static int nativeStaticGetStopBits( String port )
		throws UnsupportedCommOperationException;


	private native byte nativeGetParityErrorChar( )
		throws UnsupportedCommOperationException;
	private native boolean nativeSetParityErrorChar( byte b )
		throws UnsupportedCommOperationException;
	private native byte nativeGetEndOfInputChar( )
		throws UnsupportedCommOperationException;
	private native boolean nativeSetEndOfInputChar( byte b )
		throws UnsupportedCommOperationException;
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
	*
	*  This is only accurate up to 38600 baud currently.
	*
	*  @param  port the name of the port thats been preopened
	*  @return BaudRate on success
	*  @throws UnsupportedCommOperationException;
	*  This will not behave as expected with custom speeds
	*
	*/
	public static int staticGetBaudRate( String port )
		throws UnsupportedCommOperationException
	{
		if ( debug )
			System.out.println(System.currentTimeMillis() + ": " + 
				"RXTXPort:staticGetBaudRate( " + port + " )");
		return(nativeStaticGetBaudRate( port ));
	}
	/**
	*  Extension to CommAPI
	*  This is an extension to CommAPI.  It may not be supported on
	*  all operating systems.
	*
	*  @param  port the name of the port thats been preopened
	*  @return DataBits on success
	*  @throws UnsupportedCommOperationException;
	*
	*/
	public static int staticGetDataBits( String port )
		throws UnsupportedCommOperationException
	{
		if ( debug )
			System.out.println(System.currentTimeMillis() + ": " + 
				"RXTXPort:staticGetDataBits( " + port + " )");
		return(nativeStaticGetDataBits( port ) );
	}

	/**
	*  Extension to CommAPI
	*  This is an extension to CommAPI.  It may not be supported on
	*  all operating systems.
	*
	*  @param  port the name of the port thats been preopened
	*  @return Parity on success
	*  @throws UnsupportedCommOperationException;
	*
	*/
	public static int staticGetParity( String port )
		throws UnsupportedCommOperationException
	{
		if ( debug )
			System.out.println(System.currentTimeMillis() + ": " + 
				"RXTXPort:staticGetParity( " + port + " )");
		return( nativeStaticGetParity( port ) );
	}

	/**
	*  Extension to CommAPI
	*  This is an extension to CommAPI.  It may not be supported on
	*  all operating systems.
	*
	*  @param  port the name of the port thats been preopened
	*  @return StopBits on success
	*  @throws UnsupportedCommOperationException;
	*
	*/
	public static int staticGetStopBits( String port )
		throws UnsupportedCommOperationException
	{
		if ( debug )
			System.out.println(System.currentTimeMillis() + ": " + 
				"RXTXPort:staticGetStopBits( " + port + " )");
			return(nativeStaticGetStopBits( port ) );
	}

	/** 
	*  Extension to CommAPI
	*  This is an extension to CommAPI.  It may not be supported on
	*  all operating systems.
	*
	*  Set the SerialPort parameters
	*  1.5 stop bits requires 5 databits
	*  @param  f filename
	*  @param  b baudrate
	*  @param  d databits
	*  @param  s stopbits
	*  @param  p parity
	*
	*  @throws UnsupportedCommOperationException
	*  @see gnu.io.UnsupportedCommOperationException
	*/

	public static void staticSetSerialPortParams( String f, int b, int d,
		int s, int p )
		throws UnsupportedCommOperationException
	{
		if ( debug )
			System.out.println(System.currentTimeMillis() + ": " + 
				"RXTXPort:staticSetSerialPortParams( " +
				f + " " + b + " " + d + " " + s + " " + p );
		nativeStaticSetSerialPortParams( f, b, d, s, p );
	}

	/**
	*  Extension to CommAPI
	*  This is an extension to CommAPI.  It may not be supported on
	*  all operating systems.
	*
	*  Open the port and set DSR.  remove lockfile and do not close
	*  This is so some software can appear to set the DSR before 'opening'
	*  the port a second time later on.
	*
	*  @return true on success
	*  @throws UnsupportedCommOperationException;
	*
	*/

	public static boolean staticSetDSR( String port, boolean flag )
		throws UnsupportedCommOperationException
	{
		if ( debug )
			System.out.println(System.currentTimeMillis() + ": " +  "RXTXPort:staticSetDSR( " + port +
						" " + flag );
		return( nativeStaticSetDSR( port, flag ) );
	}

	/**
	*  Extension to CommAPI
	*  This is an extension to CommAPI.  It may not be supported on
	*  all operating systems.
	*
	*  Open the port and set DTR.  remove lockfile and do not close
	*  This is so some software can appear to set the DTR before 'opening'
	*  the port a second time later on.
	*
	*  @return true on success
	*  @throws UnsupportedCommOperationException;
	*
	*/

	public static boolean staticSetDTR( String port, boolean flag )
		throws UnsupportedCommOperationException
	{
		if ( debug )
			System.out.println(System.currentTimeMillis() + ": " +  "RXTXPort:staticSetDTR( " + port +
						" " + flag );
		return( nativeStaticSetDTR( port, flag ) );
	}

	/**
	*  Extension to CommAPI
	*  This is an extension to CommAPI.  It may not be supported on
	*  all operating systems.
	*
	*  Open the port and set RTS.  remove lockfile and do not close
	*  This is so some software can appear to set the RTS before 'opening'
	*  the port a second time later on.
	*
	*  @return none
	*  @throws UnsupportedCommOperationException;
	*
	*/

	public static boolean staticSetRTS( String port, boolean flag )
		throws UnsupportedCommOperationException
	{
		if ( debug )
			System.out.println(System.currentTimeMillis() + ": " +  "RXTXPort:staticSetRTS( " + port +
						" " + flag );
		return( nativeStaticSetRTS( port, flag ) );
	}

	/**
	*  Extension to CommAPI
	*  This is an extension to CommAPI.  It may not be supported on
	*  all operating systems.
	*
	*  find the fd and return RTS without using a Java open() call
	*
	*  @param String port
	*  @return boolean true if asserted
	*  @throws UnsupportedCommOperationException;
	*
	*/

	public static boolean staticIsRTS( String port )
		throws UnsupportedCommOperationException
	{
		if ( debug )
			System.out.println(System.currentTimeMillis() + ": " +  "RXTXPort:staticIsRTS( " + port + " )" );
		return( nativeStaticIsRTS( port ) );
	}
	/**
	*  Extension to CommAPI
	*  This is an extension to CommAPI.  It may not be supported on
	*  all operating systems.
	*
	*  find the fd and return CD without using a Java open() call
	*
	*  @param String port
	*  @return boolean true if asserted
	*  @throws UnsupportedCommOperationException;
	*
	*/

	public static boolean staticIsCD( String port )
		throws UnsupportedCommOperationException
	{
		if ( debug )
			System.out.println( "RXTXPort:staticIsCD( " + port + " )" );
		return( nativeStaticIsCD( port ) );
	}
	/**
	*  Extension to CommAPI
	*  This is an extension to CommAPI.  It may not be supported on
	*  all operating systems.
	*
	*  find the fd and return CTS without using a Java open() call
	*
	*  @param String port
	*  @return boolean true if asserted
	*  @throws UnsupportedCommOperationException;
	*
	*/

	public static boolean staticIsCTS( String port )
		throws UnsupportedCommOperationException
	{
		if ( debug )
			System.out.println(System.currentTimeMillis() + ": " +  "RXTXPort:staticIsCTS( " + port + " )" );
		return( nativeStaticIsCTS( port ) );
	}
	/**
	*  Extension to CommAPI
	*  This is an extension to CommAPI.  It may not be supported on
	*  all operating systems.
	*
	*  find the fd and return DSR without using a Java open() call
	*
	*  @param String port
	*  @return boolean true if asserted
	*  @throws UnsupportedCommOperationException;
	*
	*/

	public static boolean staticIsDSR( String port )
		throws UnsupportedCommOperationException
	{
		if ( debug )
			System.out.println(System.currentTimeMillis() + ": " +  "RXTXPort:staticIsDSR( " + port + " )" );
		return( nativeStaticIsDSR( port ) );
	}
	/**
	*  Extension to CommAPI
	*  This is an extension to CommAPI.  It may not be supported on
	*  all operating systems.
	*
	*  find the fd and return DTR without using a Java open() call
	*
	*  @param String port
	*  @return boolean true if asserted
	*  @throws UnsupportedCommOperationException;
	*
	*/

	public static boolean staticIsDTR( String port )
		throws UnsupportedCommOperationException
	{
		if ( debug )
			System.out.println(System.currentTimeMillis() + ": " +  "RXTXPort:staticIsDTR( " + port + " )" );
		return( nativeStaticIsDTR( port ) );
	}
	/**
	*  Extension to CommAPI
	*  This is an extension to CommAPI.  It may not be supported on
	*  all operating systems.
	*
	*  find the fd and return RI without using a Java open() call
	*
	*  @param String port
	*  @return boolean true if asserted
	*  @throws UnsupportedCommOperationException;
	*
	*/

	public static boolean staticIsRI( String port )
		throws UnsupportedCommOperationException
	{
		if ( debug )
			System.out.println(System.currentTimeMillis() + ": " +  "RXTXPort:staticIsRI( " + port + " )" );
		return( nativeStaticIsRI( port ) );
	}


	/**
	*  Extension to CommAPI
	*  This is an extension to CommAPI.  It may not be supported on
	*  all operating systems.
	*  @return int the Parity Error Character
	*  @throws UnsupportedCommOperationException;
	*
	*  Anyone know how to do this in Unix?
	*/

	public byte getParityErrorChar( )
		throws UnsupportedCommOperationException
	{
		byte ret;
		if ( debug )
			System.out.println(System.currentTimeMillis() + ": " +  "getParityErrorChar()" );
		ret = nativeGetParityErrorChar();
		if ( debug )
			System.out.println(System.currentTimeMillis() + ": " +  "getParityErrorChar() returns " +
						ret );
		return( ret );
	}

	/**
	*  Extension to CommAPI
	*  This is an extension to CommAPI.  It may not be supported on
	*  all operating systems.
	*  @param b Parity Error Character
	*  @return boolean true on success
	*  @throws UnsupportedCommOperationException;
	*
	*  Anyone know how to do this in Unix?
	*/

	public boolean setParityErrorChar( byte b )
		throws UnsupportedCommOperationException
	{
		if ( debug )
			System.out.println(System.currentTimeMillis() + ": " +  "setParityErrorChar(" + b + ")" );
		return( nativeSetParityErrorChar( b ) );
	}

	/**
	*  Extension to CommAPI
	*  This is an extension to CommAPI.  It may not be supported on
	*  all operating systems.
	*  @return int the End of Input Character
	*  @throws UnsupportedCommOperationException;
	*
	*  Anyone know how to do this in Unix?
	*/

	public byte getEndOfInputChar( )
		throws UnsupportedCommOperationException
	{
		byte ret;
		if ( debug )
			System.out.println(System.currentTimeMillis() + ": " +  "getEndOfInputChar()" );
		ret = nativeGetEndOfInputChar();
		if ( debug )
			System.out.println(System.currentTimeMillis() + ": " +  "getEndOfInputChar() returns " +
						ret );
		return( ret );
	}

	/**
	*  Extension to CommAPI
	*  This is an extension to CommAPI.  It may not be supported on
	*  all operating systems.
	*  @param b End Of Input Character
	*  @return boolean true on success
	*  @throws UnsupportedCommOperationException;
	*/

	public boolean setEndOfInputChar( byte b )
		throws UnsupportedCommOperationException
	{
		if ( debug )
			System.out.println(System.currentTimeMillis() + ": " +  "setEndOfInputChar(" + b + ")" );
		return( nativeSetEndOfInputChar( b ) );
	}

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
		if ( debug )
			System.out.println(System.currentTimeMillis() + ": " +  "RXTXPort:setUARTType()");
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
		if ( debug )
			System.out.println(System.currentTimeMillis() + ": " +  "RXTXPort:setBaudBase()");
		return nativeSetBaudBase(BaudBase);
	}

	/**
	*  Extension to CommAPI
	*  @return int BaudBase
	*  @throws UnsupportedCommOperationException
	*/

	public int getBaudBase() throws UnsupportedCommOperationException
	{
		if ( debug )
			System.out.println(System.currentTimeMillis() + ": " +  "RXTXPort:getBaudBase()");
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
		if ( debug )
			System.out.println(System.currentTimeMillis() + ": " +  "RXTXPort:setDivisor()");
		return nativeSetDivisor(Divisor);
	}

	/**
	*  Extension to CommAPI
	*  @returns int Divisor;
	*  @throws UnsupportedCommOperationException
	*/

	public int getDivisor() throws UnsupportedCommOperationException
	{
		if ( debug )
			System.out.println(System.currentTimeMillis() + ": " +  "RXTXPort:getDivisor()");
		return nativeGetDivisor();
	}

	/**
	*  Extension to CommAPI
	*  returns boolean true on success
	*  @throws UnsupportedCommOperationException
	*/

	public boolean setLowLatency() throws UnsupportedCommOperationException
	{
		if ( debug )
			System.out.println(System.currentTimeMillis() + ": " +  "RXTXPort:setLowLatency()");
		return nativeSetLowLatency();
	}

	/**
	*  Extension to CommAPI
	*  returns boolean true on success
	*  @throws UnsupportedCommOperationException
	*/

	public boolean getLowLatency() throws UnsupportedCommOperationException
	{
		if ( debug )
			System.out.println(System.currentTimeMillis() + ": " +  "RXTXPort:getLowLatency()");
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
		if ( debug )
			System.out.println(System.currentTimeMillis() + ": " +  "RXTXPort:setCallOutHangup()");
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
		if ( debug )
			System.out.println(System.currentTimeMillis() + ": " +  "RXTXPort:getCallOutHangup()");
		return nativeGetCallOutHangup();
	}

/*------------------------  END OF CommAPI Extensions -----------------------*/
}
