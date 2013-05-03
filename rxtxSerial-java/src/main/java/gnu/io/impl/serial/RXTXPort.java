/*-------------------------------------------------------------------------
 |   RXTX License v 2.1 - LGPL v 2.1 + Linking Over Controlled Interface.
 |   RXTX is a native interface to serial ports in java.
 |   Copyright 1997-2012 by Trent Jarvi tjarvi@qbang.org and others who
 |   actually wrote it.  See individual source files for more information.
 |
 |   A copy of the LGPL v 2.1 may be found at
 |   http://www.gnu.org/licenses/lgpl.txt on March 4th 2007.  A copy is
 |   here for your convenience.
 |
 |   This library is free software; you can redistribute it and/or
 |   modify it under the terms of the GNU Lesser General Public
 |   License as published by the Free Software Foundation; either
 |   version 2.1 of the License, or (at your option) any later version.
 |
 |   This library is distributed in the hope that it will be useful,
 |   but WITHOUT ANY WARRANTY; without even the implied warranty of
 |   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 |   Lesser General Public License for more details.
 |
 |   An executable that contains no derivative of any portion of RXTX, but
 |   is designed to work with RXTX by being dynamically linked with it,
 |   is considered a "work that uses the Library" subject to the terms and
 |   conditions of the GNU Lesser General Public License.
 |
 |   The following has been added to the RXTX License to remove
 |   any confusion about linking to RXTX.   We want to allow in part what
 |   section 5, paragraph 2 of the LGPL does not permit in the special
 |   case of linking over a controlled interface.  The intent is to add a
 |   Java Specification Request or standards body defined interface in the
 |   future as another exception but one is not currently available.
 |
 |   http://www.fsf.org/licenses/gpl-faq.html#LinkingOverControlledInterface
 |
 |   As a special exception, the copyright holders of RXTX give you
 |   permission to link RXTX with independent modules that communicate with
 |   RXTX solely through the Sun Microsytems CommAPI interface version 2,
 |   regardless of the license terms of these independent modules, and to copy
 |   and distribute the resulting combined work under terms of your choice,
 |   provided that every copy of the combined work is accompanied by a complete
 |   copy of the source code of RXTX (the version of RXTX used to produce the
 |   combined work), being distributed under the terms of the GNU Lesser General
 |   Public License plus this exception.  An independent module is a
 |   module which is not derived from or based on RXTX.
 |
 |   Note that people who make modified versions of RXTX are not obligated
 |   to grant this special exception for their modified versions; it is
 |   their choice whether to do so.  The GNU Lesser General Public License
 |   gives permission to release a modified version without this exception; this
 |   exception also makes it possible to release a modified version which
 |   carries forward this exception.
 |
 |   You should have received a copy of the GNU Lesser General Public
 |   License along with this library; if not, write to the Free
 |   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 |   All trademarks belong to their respective owners.
 --------------------------------------------------------------------------*/
package gnu.io.impl.serial;

import gnu.io.DriverContext;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.TooManyListenersException;

final class RXTXPort extends SerialPort {

    protected static final boolean DEBUG = false;
    protected static final boolean DEBUG_READ = false;
    protected static final boolean DEBUG_READ_RESULTS = false;
    protected static final boolean DEBUG_WRITE = false;
    protected static final boolean DEBUG_EVENTS = false;
    protected static final boolean DEBUG_VERBOSE = false;
    private static Zystem sys;
    private final DriverContext context;

    static {
        try {
            sys = new Zystem();
        } catch (Exception e) {
            throw new Error(e.toString());
        }

        if (DEBUG) {
            sys.reportln("RXTXPort {}");
        }
        RXTXVersion.loadLibrary("rxtxSerial");
        Initialize();
    }

    /**
     * Initialize the native library
     */
    private native static void Initialize();
    boolean monitorThreadAlive = false;

    /**
     * Open the named port
     *
     * @param name the name of the device to open
     * @throws PortInUseException
     * @see gnu.io.SerialPort
     */
    public RXTXPort(DriverContext context, String name) throws PortInUseException {
        super(name);
        if (DEBUG) {
            sys.reportln("RXTXPort:RXTXPort(" + name + ") called");
        }
        this.context = context;
        /*
         * commapi/javadocs/API_users_guide.html specifies that whenever an
         * application tries to open a port in use by another application the
         * PortInUseException will be thrown
         *
         * I know some didnt like it this way but I'm not sure how to avoid it.
         * We will just be writing to a bogus fd if we catch the exeption
         *
         * Trent
         */
        //	try {
        fd = open(name);

        MonitorThreadLock = true;
        monThread = new MonitorThread();
        monThread.start();
        waitForTheNativeCodeSilly();
        monitorThreadAlive = true;
        //	} catch ( PortInUseException e ){}
        timeout = -1;	/*
         * default disabled timeout
         */
        if (DEBUG) {
            sys.reportln("RXTXPort:RXTXPort(" + name + ") returns with fd = "
                    + fd);
        }
    }

    private synchronized native int open(String name)
            throws PortInUseException;
    /*
     * dont close the file while accessing the fd
     */
    int IOLocked = 0;
    Object IOLockedMutex = new Object();
    /**
     * File descriptor
     */
    private int fd = 0;
    /**
     * a pointer to the event info structure used to share information between
     * threads so write threads can send output buffer empty from a pthread if
     * need be.
     *
     * long for 64 bit pointers.
     */
    long eis = 0;
    /**
     * pid for lock files
     */
    int pid = 0;
    /**
     * DSR flag *
     */
    static boolean dsrFlag = false;
    /**
     * Output stream
     */
    private final SerialOutputStream out = new SerialOutputStream();

    public OutputStream getOutputStream() {
        if (DEBUG) {
            sys.reportln("RXTXPort:getOutputStream() called and returning");
        }
        return out;
    }
    /**
     * Input stream
     */
    private final SerialInputStream in = new SerialInputStream();

    public InputStream getInputStream() {
        if (DEBUG) {
            sys.reportln("RXTXPort:getInputStream() called and returning");
        }
        return in;
    }

    /**
     * Set the SerialPort parameters 1.5 stop bits requires 5 databits
     *
     * @param b baudrate
     * @param d databits
     * @param s stopbits
     * @param p parity
     * @throws UnsupportedCommOperationException
     * @see gnu.io.UnsupportedCommOperationException
     *
     * If speed is not a predifined speed it is assumed to be the actual speed
     * desired.
     */
    private native int nativeGetParity(int fd);

    private native int nativeGetFlowControlMode(int fd);

    public synchronized void setSerialPortParams(int b, int d, int s,
            int p)
            throws UnsupportedCommOperationException {
        if (DEBUG) {
            sys.reportln("RXTXPort:setSerialPortParams("
                    + b + " " + d + " " + s + " " + p + ") called");
        }
        if (nativeSetSerialPortParams(b, d, s, p)) {
            throw new UnsupportedCommOperationException(
                    "Invalid Parameter");
        }
        speed = b;
        if (s == STOPBITS_1_5) {
            dataBits = DATABITS_5;
        } else {
            dataBits = d;
        }
        stopBits = s;
        parity = p;
        sys.reportln("RXTXPort:setSerialPortParams("
                + b + " " + d + " " + s + " " + p
                + ") returning");
    }

    /**
     * Set the native serial port parameters If speed is not a predifined speed
     * it is assumed to be the actual speed desired.
     */
    private native boolean nativeSetSerialPortParams(int speed,
            int dataBits, int stopBits, int parity)
            throws UnsupportedCommOperationException;
    /**
     * Line speed in bits-per-second
     */
    private int speed = 9600;

    public int getBaudRate() {
        if (DEBUG) {
            sys.reportln("RXTXPort:getBaudRate() called and returning " + speed);
        }
        return speed;
    }
    /**
     * Data bits port parameter
     */
    private int dataBits = DATABITS_8;

    public int getDataBits() {
        if (DEBUG) {
            sys.reportln("RXTXPort:getDataBits() called and returning " + dataBits);
        }
        return dataBits;
    }
    /**
     * Stop bits port parameter
     */
    private int stopBits = SerialPort.STOPBITS_1;

    public int getStopBits() {
        if (DEBUG) {
            sys.reportln("RXTXPort:getStopBits() called and returning " + stopBits);
        }
        return stopBits;
    }
    /**
     * Parity port parameter
     */
    private int parity = SerialPort.PARITY_NONE;

    public int getParity() {
        if (DEBUG) {
            sys.reportln("RXTXPort:getParity() called and returning " + parity);
        }
        return parity;
    }
    /**
     * Flow control
     */
    private int flowmode = SerialPort.FLOWCONTROL_NONE;

    public void setFlowControlMode(int flowcontrol) {
        if (DEBUG) {
            sys.reportln("RXTXPort:setFlowControlMode( " + flowcontrol + " ) called");
        }
        if (monThreadisInterrupted) {
            if (DEBUG_EVENTS) {
                sys.reportln("RXTXPort:setFlowControlMode MonThread is Interrupeted returning");
            }
            return;
        }
        try {
            setflowcontrol(flowcontrol);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        flowmode = flowcontrol;
        if (DEBUG) {
            sys.reportln("RXTXPort:setFlowControlMode( " + flowcontrol + " ) returning");
        }
    }

    public int getFlowControlMode() {
        if (DEBUG) {
            sys.reportln("RXTXPort:getFlowControlMode() returning " + flowmode);
        }
        return flowmode;
    }

    native void setflowcontrol(int flowcontrol) throws IOException;

    /*
     * linux/drivers/char/n_hdlc.c? FIXME taj@www.linux.org.uk
     */
    public void enableReceiveFraming(int f)
            throws UnsupportedCommOperationException {
        if (DEBUG) {
            sys.reportln("RXTXPort:enableReceiveFramming() throwing exception");
        }
        throw new UnsupportedCommOperationException("Not supported");
    }

    public void disableReceiveFraming() {
        if (DEBUG) {
            sys.reportln("RXTXPort:disableReceiveFramming() called and returning (noop)");
        }
    }

    public boolean isReceiveFramingEnabled() {
        if (DEBUG) {
            sys.reportln("RXTXPort:isReceiveFrammingEnabled() called and returning " + false);
        }
        return false;
    }

    public int getReceiveFramingByte() {
        if (DEBUG) {
            sys.reportln("RXTXPort:getReceiveFrammingByte() called and returning " + 0);
        }
        return 0;
    }
    /**
     * Receive timeout control
     */
    private int timeout;

    /**
     * @return int the timeout
     */
    public native int NativegetReceiveTimeout();

    /**
     * @return bloolean true if recieve timeout is enabled
     */
    private native boolean NativeisReceiveTimeoutEnabled();

    /**
     * @param time
     * @param threshold
     * @param InputBuffer
     */
    private native void NativeEnableReceiveTimeoutThreshold(int time,
            int threshold, int InputBuffer);

    public void disableReceiveTimeout() {
        if (DEBUG) {
            sys.reportln("RXTXPort:disableReceiveTimeout() called");
        }
        timeout = -1;
        NativeEnableReceiveTimeoutThreshold(timeout, threshold, InputBuffer);
        if (DEBUG) {
            sys.reportln("RXTXPort:disableReceiveTimeout() returning");
        }
    }

    public void enableReceiveTimeout(int time) {
        if (DEBUG) {
            sys.reportln("RXTXPort:enableReceiveTimeout() called");
        }
        if (time >= 0) {
            timeout = time;
            NativeEnableReceiveTimeoutThreshold(time, threshold,
                    InputBuffer);
        } else {
            throw new IllegalArgumentException(
                    "Unexpected negative timeout value");
        }
        if (DEBUG) {
            sys.reportln("RXTXPort:enableReceiveTimeout() returning");
        }
    }

    public boolean isReceiveTimeoutEnabled() {
        if (DEBUG) {
            sys.reportln("RXTXPort:isReceiveTimeoutEnabled() called and returning " + NativeisReceiveTimeoutEnabled());
        }
        return NativeisReceiveTimeoutEnabled();
    }

    public int getReceiveTimeout() {
        if (DEBUG) {
            sys.reportln("RXTXPort:getReceiveTimeout() called and returning " + NativegetReceiveTimeout());
        }
        return NativegetReceiveTimeout();
    }
    /**
     * Receive threshold control
     */
    private int threshold = 0;

    public void enableReceiveThreshold(int thresh) {
        if (DEBUG) {
            sys.reportln("RXTXPort:enableReceiveThreshold( " + thresh + " ) called");
        }
        if (thresh >= 0) {
            threshold = thresh;
            NativeEnableReceiveTimeoutThreshold(timeout, threshold,
                    InputBuffer);
        } else {
            throw new IllegalArgumentException(
                    "Unexpected negative threshold value");
        }
        if (DEBUG) {
            sys.reportln("RXTXPort:enableReceiveThreshold( " + thresh + " ) returned");
        }
    }

    public void disableReceiveThreshold() {
        if (DEBUG) {
            sys.reportln("RXTXPort:disableReceiveThreshold() called and returning");
        }
        enableReceiveThreshold(0);
    }

    public int getReceiveThreshold() {
        if (DEBUG) {
            sys.reportln("RXTXPort:getReceiveThreshold() called and returning " + threshold);
        }
        return threshold;
    }

    /**
     * @return boolean true if receive threshold is enabled
     */
    public boolean isReceiveThresholdEnabled() {
        if (DEBUG) {
            sys.reportln("RXTXPort:isReceiveThresholdEnable() called and returning" + (threshold > 0));
        }
        return (threshold > 0);
    }
    /**
     * FIXME I think this refers to FOPEN(3)/SETBUF(3)/FREAD(3)/FCLOSE(3)
     * taj@www.linux.org.uk
     *
     * These are native stubs...
     */
    private int InputBuffer = 0;
    private int OutputBuffer = 0;

    public void setInputBufferSize(int size) {
        if (DEBUG) {
            sys.reportln("RXTXPort:setInputBufferSize( "
                    + size + ") called");
        }
        if (size < 0) {
            throw new IllegalArgumentException(
                    "Unexpected negative buffer size value");
        } else {
            InputBuffer = size;
        }
        if (DEBUG) {
            sys.reportln("RXTXPort:setInputBufferSize( "
                    + size + ") returning");
        }
    }

    public int getInputBufferSize() {
        if (DEBUG) {
            sys.reportln("RXTXPort:getInputBufferSize() called and returning " + InputBuffer);
        }
        return InputBuffer;
    }

    public void setOutputBufferSize(int size) {
        if (DEBUG) {
            sys.reportln("RXTXPort:setOutputBufferSize( "
                    + size + ") called");
        }
        if (size < 0) {
            throw new IllegalArgumentException(
                    "Unexpected negative buffer size value");
        } else {
            OutputBuffer = size;
        }
        if (DEBUG) {
            sys.reportln("RXTXPort:setOutputBufferSize( "
                    + size + ") returned");
        }

    }

    public int getOutputBufferSize() {
        if (DEBUG) {
            sys.reportln("RXTXPort:getOutputBufferSize() called and returning " + OutputBuffer);
        }
        return OutputBuffer;
    }

    /**
     * @return true if DTR is set
     */
    public native boolean isDTR();

    /**
     * @param state
     */
    public native void setDTR(boolean state);

    /**
     * @param state
     */
    public native void setRTS(boolean state);

    private native void setDSR(boolean state);

    /**
     * @return boolean true if CTS is set
     */
    public native boolean isCTS();

    /**
     * @return boolean true if DSR is set
     */
    public native boolean isDSR();

    /**
     * @return boolean true if CD is set
     */
    public native boolean isCD();

    /**
     * @return boolean true if RI is set
     */
    public native boolean isRI();

    /**
     * @return boolean true if RTS is set
     */
    public native boolean isRTS();

    /**
     * Write to the port
     *
     * @param duration
     */
    public native void sendBreak(int duration);

    protected native void writeByte(int b, boolean i) throws IOException;

    protected native void writeArray(byte b[], int off, int len, boolean i)
            throws IOException;

    protected native boolean nativeDrain(boolean i) throws IOException;

    protected native int nativeavailable() throws IOException;

    protected native int readByte() throws IOException;

    protected native int readArray(byte b[], int off, int len)
            throws IOException;

    protected native int readTerminatedArray(byte b[], int off, int len, byte t[])
            throws IOException;
    /**
     * Serial Port Event listener
     */
    private SerialPortEventListener SPEventListener;
    /**
     * Thread to monitor data
     */
    private MonitorThread monThread;

    /**
     * Process SerialPortEvents
     */
    native void eventLoop();
    /**
     * true if monitor thread is interrupted
     */
    boolean monThreadisInterrupted = true;

    private native void interruptEventLoop();

    public boolean checkMonitorThread() {
        if (DEBUG) {
            sys.reportln("RXTXPort:checkMonitorThread()");
        }
        if (monThread != null) {
            if (DEBUG) {
                sys.reportln(
                        "monThreadisInterrupted = "
                        + monThreadisInterrupted);
            }
            return monThreadisInterrupted;
        }
        if (DEBUG) {
            sys.reportln("monThread is null ");
        }
        return true;
    }

    /**
     * @param event
     * @param state
     * @return boolean true if the port is closing
     */
    public boolean sendEvent(int event, boolean state) {
        if (DEBUG_EVENTS) {
            sys.report("RXTXPort:sendEvent(");
        }
        /*
         * Let the native side know its time to die
         */

        if (fd == 0 || SPEventListener == null || monThread == null) {
            return true;
        }

        switch (event) {
            case SerialPortEvent.DATA_AVAILABLE:
                if (DEBUG_EVENTS) {
                    sys.reportln("DATA_AVAILABLE "
                            + monThread.Data + ")");
                }
                break;
            case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
                if (DEBUG_EVENTS) {
                    sys.reportln(
                            "OUTPUT_BUFFER_EMPTY "
                            + monThread.Output + ")");
                }
                break;
            case SerialPortEvent.CTS:
                if (DEBUG_EVENTS) {
                    sys.reportln("CTS "
                            + monThread.CTS + ")");
                }
                break;
            case SerialPortEvent.DSR:
                if (DEBUG_EVENTS) {
                    sys.reportln("DSR "
                            + monThread.Output + ")");
                }
                break;
            case SerialPortEvent.RI:
                if (DEBUG_EVENTS) {
                    sys.reportln("RI "
                            + monThread.RI + ")");
                }
                break;
            case SerialPortEvent.CD:
                if (DEBUG_EVENTS) {
                    sys.reportln("CD "
                            + monThread.CD + ")");
                }
                break;
            case SerialPortEvent.OE:
                if (DEBUG_EVENTS) {
                    sys.reportln("OE "
                            + monThread.OE + ")");
                }
                break;
            case SerialPortEvent.PE:
                if (DEBUG_EVENTS) {
                    sys.reportln("PE "
                            + monThread.PE + ")");
                }
                break;
            case SerialPortEvent.FE:
                if (DEBUG_EVENTS) {
                    sys.reportln("FE "
                            + monThread.FE + ")");
                }
                break;
            case SerialPortEvent.BI:
                if (DEBUG_EVENTS) {
                    sys.reportln("BI "
                            + monThread.BI + ")");
                }
                break;
            default:
                if (DEBUG_EVENTS) {
                    sys.reportln("XXXXXXXXXXXXXX "
                            + event + ")");
                }
                break;
        }
        if (DEBUG_EVENTS && DEBUG_VERBOSE) {
            sys.reportln("	checking flags ");
        }

        switch (event) {
            case SerialPortEvent.DATA_AVAILABLE:
                if (monThread.Data) {
                    break;
                }
                return false;
            case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
                if (monThread.Output) {
                    break;
                }
                return false;
            case SerialPortEvent.CTS:
                if (monThread.CTS) {
                    break;
                }
                return false;
            case SerialPortEvent.DSR:
                if (monThread.DSR) {
                    break;
                }
                return false;
            case SerialPortEvent.RI:
                if (monThread.RI) {
                    break;
                }
                return false;
            case SerialPortEvent.CD:
                if (monThread.CD) {
                    break;
                }
                return false;
            case SerialPortEvent.OE:
                if (monThread.OE) {
                    break;
                }
                return false;
            case SerialPortEvent.PE:
                if (monThread.PE) {
                    break;
                }
                return false;
            case SerialPortEvent.FE:
                if (monThread.FE) {
                    break;
                }
                return false;
            case SerialPortEvent.BI:
                if (monThread.BI) {
                    break;
                }
                return false;
            default:
                System.err.println("unknown event: " + event);
                return false;
        }
        if (DEBUG_EVENTS && DEBUG_VERBOSE) {
            sys.reportln("	getting event");
        }
        SerialPortEvent e = context.getEventFactory()
                .createSerialPortEvent(this, event, !state, state);
        if (DEBUG_EVENTS && DEBUG_VERBOSE) {
            sys.reportln("	sending event");
        }
        if (monThreadisInterrupted) {
            if (DEBUG_EVENTS) {
                sys.reportln("	sendEvent return");
            }
            return true;
        }
        if (SPEventListener != null) {
            SPEventListener.serialEvent(e);
        }

        if (DEBUG_EVENTS && DEBUG_VERBOSE) {
            sys.reportln("	sendEvent return");
        }

        if (fd == 0 || SPEventListener == null || monThread == null) {
            return true;
        } else {
            return false;
        }
    }
    boolean MonitorThreadLock = true;

    public void addEventListener(
            SerialPortEventListener lsnr) throws TooManyListenersException {
        /*
         * Don't let and notification requests happen until the Eventloop is
         * ready
         */

        if (DEBUG) {
            sys.reportln("RXTXPort:addEventListener()");
        }
        if (SPEventListener != null) {
            throw new TooManyListenersException();
        }
        SPEventListener = lsnr;
        if (!monitorThreadAlive) {
            MonitorThreadLock = true;
            monThread = new MonitorThread();
            monThread.setDaemon(true);
            monThread.start();
            waitForTheNativeCodeSilly();
            monitorThreadAlive = true;
        }
        if (DEBUG) {
            sys.reportln("RXTXPort:Interrupt=false");
        }
    }

    public void removeEventListener() {
        if (DEBUG) {
            sys.reportln("RXTXPort:removeEventListener() called");
        }
        waitForTheNativeCodeSilly();
        //if( monThread != null && monThread.isAlive() )
        if (monThreadisInterrupted == true) {
            sys.reportln("	RXTXPort:removeEventListener() already interrupted");
            monThread = null;
            SPEventListener = null;
            return;
        } else if (monThread != null && monThread.isAlive()) {
            if (DEBUG) {
                sys.reportln("	RXTXPort:Interrupt=true");
            }
            monThreadisInterrupted = true;
            /*
             * Notify all threads in this PID that something is up They will
             * call back to see if its their thread using isInterrupted().
             */
            if (DEBUG) {
                sys.reportln("	RXTXPort:calling interruptEventLoop");
            }
            interruptEventLoop();

            if (DEBUG) {
                sys.reportln("	RXTXPort:calling monThread.join()");
            }
            try {

                // wait a reasonable moment for the death of the monitor thread
                monThread.join(3000);
            } catch (InterruptedException ex) {
                // somebody called interrupt() on us (ie wants us to abort)
                // we dont propagate InterruptedExceptions so lets re-set the flag 
                Thread.currentThread().interrupt();
                return;
            }

            if (DEBUG && monThread.isAlive()) {
                sys.reportln("	MonThread is still alive!");

            }

        }
        monThread = null;
        SPEventListener = null;
        MonitorThreadLock = false;
        monitorThreadAlive = false;
        monThreadisInterrupted = true;
        sys.reportln("RXTXPort:removeEventListener() returning");
    }

    /**
     * Give the native code a chance to start listening to the hardware or
     * should we say give the native code control of the issue.
     *
     * This is important for applications that flicker the Monitor thread while
     * keeping the port open. In worst case test cases this loops once or twice
     * every time.
     */
    protected void waitForTheNativeCodeSilly() {
        while (MonitorThreadLock) {
            try {
                Thread.sleep(5);
            } catch (Exception e) {
            }
        }
    }

    private native void nativeSetEventFlag(int fd, int event,
            boolean flag);

    public void notifyOnDataAvailable(boolean enable) {
        if (DEBUG) {
            sys.reportln("RXTXPort:notifyOnDataAvailable( "
                    + enable + " )");
        }

        waitForTheNativeCodeSilly();

        MonitorThreadLock = true;
        nativeSetEventFlag(fd, SerialPortEvent.DATA_AVAILABLE,
                enable);
        monThread.Data = enable;
        MonitorThreadLock = false;
    }

    public void notifyOnOutputEmpty(boolean enable) {
        if (DEBUG) {
            sys.reportln("RXTXPort:notifyOnOutputEmpty( "
                    + enable + " )");
        }
        waitForTheNativeCodeSilly();
        MonitorThreadLock = true;
        nativeSetEventFlag(fd, SerialPortEvent.OUTPUT_BUFFER_EMPTY,
                enable);
        monThread.Output = enable;
        MonitorThreadLock = false;
    }

    public void notifyOnCTS(boolean enable) {
        if (DEBUG) {
            sys.reportln("RXTXPort:notifyOnCTS( "
                    + enable + " )");
        }
        waitForTheNativeCodeSilly();
        MonitorThreadLock = true;
        nativeSetEventFlag(fd, SerialPortEvent.CTS, enable);
        monThread.CTS = enable;
        MonitorThreadLock = false;
    }

    public void notifyOnDSR(boolean enable) {
        if (DEBUG) {
            sys.reportln("RXTXPort:notifyOnDSR( "
                    + enable + " )");
        }
        waitForTheNativeCodeSilly();
        MonitorThreadLock = true;
        nativeSetEventFlag(fd, SerialPortEvent.DSR, enable);
        monThread.DSR = enable;
        MonitorThreadLock = false;
    }

    public void notifyOnRingIndicator(boolean enable) {
        if (DEBUG) {
            sys.reportln("RXTXPort:notifyOnRingIndicator( "
                    + enable + " )");
        }
        waitForTheNativeCodeSilly();
        MonitorThreadLock = true;
        nativeSetEventFlag(fd, SerialPortEvent.RI, enable);
        monThread.RI = enable;
        MonitorThreadLock = false;
    }

    public void notifyOnCarrierDetect(boolean enable) {
        if (DEBUG) {
            sys.reportln("RXTXPort:notifyOnCarrierDetect( "
                    + enable + " )");
        }
        waitForTheNativeCodeSilly();
        MonitorThreadLock = true;
        nativeSetEventFlag(fd, SerialPortEvent.CD, enable);
        monThread.CD = enable;
        MonitorThreadLock = false;
    }

    public void notifyOnOverrunError(boolean enable) {
        if (DEBUG) {
            sys.reportln("RXTXPort:notifyOnOverrunError( "
                    + enable + " )");
        }
        waitForTheNativeCodeSilly();
        MonitorThreadLock = true;
        nativeSetEventFlag(fd, SerialPortEvent.OE, enable);
        monThread.OE = enable;
        MonitorThreadLock = false;
    }

    public void notifyOnParityError(boolean enable) {
        if (DEBUG) {
            sys.reportln("RXTXPort:notifyOnParityError( "
                    + enable + " )");
        }
        waitForTheNativeCodeSilly();
        MonitorThreadLock = true;
        nativeSetEventFlag(fd, SerialPortEvent.PE, enable);
        monThread.PE = enable;
        MonitorThreadLock = false;
    }

    public void notifyOnFramingError(boolean enable) {
        if (DEBUG) {
            sys.reportln("RXTXPort:notifyOnFramingError( "
                    + enable + " )");
        }
        waitForTheNativeCodeSilly();
        MonitorThreadLock = true;
        nativeSetEventFlag(fd, SerialPortEvent.FE, enable);
        monThread.FE = enable;
        MonitorThreadLock = false;
    }

    public void notifyOnBreakInterrupt(boolean enable) {
        if (DEBUG) {
            sys.reportln("RXTXPort:notifyOnBreakInterrupt( "
                    + enable + " )");
        }
        waitForTheNativeCodeSilly();
        MonitorThreadLock = true;
        nativeSetEventFlag(fd, SerialPortEvent.BI, enable);
        monThread.BI = enable;
        MonitorThreadLock = false;
    }

    /**
     * Close the port
     */
    private native void nativeClose(String name);
    boolean closeLock = false;

    public void close() {
        synchronized (this) {
            if (DEBUG) {
                sys.reportln("RXTXPort:close( " + this.name + " )");
            }

            while (IOLocked > 0) {
                if (DEBUG) {
                    sys.reportln("IO is locked " + IOLocked);
                }
                try {
                    this.wait(500);
                } catch (InterruptedException ie) {
                    // somebody called interrupt() on us
                    // we obbey and return without without closing the socket
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            // we set the closeLock after the above check because we might
            // have returned without proceeding
            if (closeLock) {
                return;
            }
            closeLock = true;
        }

        if (fd <= 0) {
            sys.reportln("RXTXPort:close detected bad File Descriptor");
            return;
        }
        setDTR(false);
        setDSR(false);
        if (DEBUG) {
            sys.reportln("RXTXPort:close( " + this.name + " ) setting monThreadisInterrupted");
        }
        if (!monThreadisInterrupted) {
            removeEventListener();
        }
        if (DEBUG) {
            sys.reportln("RXTXPort:close( " + this.name + " ) calling nativeClose");
        }
        nativeClose(this.name);
        if (DEBUG) {
            sys.reportln("RXTXPort:close( " + this.name + " ) calling super.close");
        }
        super.close();
        fd = 0;
        closeLock = false;
        if (DEBUG) {
            sys.reportln("RXTXPort:close( " + this.name + " ) leaving");
        }
    }

    protected void finalize() {
        if (DEBUG) {
            sys.reportln("RXTXPort:finalize()");
        }
        if (fd > 0) {
            if (DEBUG) {
                sys.reportln("RXTXPort:calling close()");
            }
            close();
        }
        sys.finalize();
    }

    /**
     * Inner class for SerialOutputStream.
     */
    class SerialOutputStream extends OutputStream {

        public void write(int b) throws IOException {
            if (DEBUG_WRITE) {
                sys.reportln("RXTXPort:SerialOutputStream:write(int)");
            }
            if (speed == 0) {
                return;
            }
            if (monThreadisInterrupted == true) {
                return;
            }
            synchronized (IOLockedMutex) {
                IOLocked++;
            }
            try {
                waitForTheNativeCodeSilly();
                if (fd == 0) {
                    throw new IOException();
                }
                writeByte(b, monThreadisInterrupted);
                if (DEBUG_WRITE) {
                    sys.reportln("Leaving RXTXPort:SerialOutputStream:write( int )");
                }
            } finally {
                synchronized (IOLockedMutex) {
                    IOLocked--;
                }
            }
        }

        public void write(byte b[]) throws IOException {
            if (DEBUG_WRITE) {
                sys.reportln("Entering RXTXPort:SerialOutputStream:write(" + b.length + ") "/*
                         * + new String(b)
                         */);
            }
            if (speed == 0) {
                return;
            }
            if (monThreadisInterrupted == true) {
                return;
            }
            if (fd == 0) {
                throw new IOException();
            }
            synchronized (IOLockedMutex) {
                IOLocked++;
            }
            try {
                waitForTheNativeCodeSilly();
                writeArray(b, 0, b.length, monThreadisInterrupted);
                if (DEBUG_WRITE) {
                    sys.reportln("Leaving RXTXPort:SerialOutputStream:write(" + b.length + ")");
                }
            } finally {
                synchronized (IOLockedMutex) {
                    IOLocked--;
                }
            }

        }

        public void write(byte b[], int off, int len)
                throws IOException {
            if (speed == 0) {
                return;
            }
            if (off + len > b.length) {
                throw new IndexOutOfBoundsException(
                        "Invalid offset/length passed to read");
            }

            byte send[] = new byte[len];
            System.arraycopy(b, off, send, 0, len);
            if (DEBUG_WRITE) {
                sys.reportln("Entering RXTXPort:SerialOutputStream:write(" + send.length + " " + off + " " + len + " " + ") " /*
                         * + new String(send)
                         */);
            }
            if (fd == 0) {
                throw new IOException();
            }
            if (monThreadisInterrupted == true) {
                return;
            }
            synchronized (IOLockedMutex) {
                IOLocked++;
            }
            try {
                waitForTheNativeCodeSilly();
                writeArray(send, 0, len, monThreadisInterrupted);
                if (DEBUG_WRITE) {
                    sys.reportln("Leaving RXTXPort:SerialOutputStream:write(" + send.length + " " + off + " " + len + " " + ") " /*
                             * + new String(send)
                             */);
                }
            } finally {
                synchronized (IOLockedMutex) {
                    IOLocked--;
                }
            }
        }

        public void flush() throws IOException {
            if (DEBUG) {
                sys.reportln("RXTXPort:SerialOutputStream:flush() enter");
            }
            if (speed == 0) {
                return;
            }
            if (fd == 0) {
                throw new IOException();
            }
            if (monThreadisInterrupted == true) {
                if (DEBUG) {
                    sys.reportln("RXTXPort:SerialOutputStream:flush() Leaving Interrupted");
                }
                return;
            }
            synchronized (IOLockedMutex) {
                IOLocked++;
            }
            try {
                waitForTheNativeCodeSilly();
                /*
                 * this is probably good on all OS's but for now just sendEvent
                 * from java on Sol
                 */
                if (nativeDrain(monThreadisInterrupted)) {
                    sendEvent(SerialPortEvent.OUTPUT_BUFFER_EMPTY, true);
                }
                if (DEBUG) {
                    sys.reportln("RXTXPort:SerialOutputStream:flush() leave");
                }
            } finally {
                synchronized (IOLockedMutex) {
                    IOLocked--;
                }
            }
        }
    }

    /**
     * Inner class for SerialInputStream
     */
    class SerialInputStream extends InputStream {

        /*        
         * timeout threshold Behavior:
         * 0 0 blocks until 1 byte is available timeout > 0, threshold = 0,
         * blocks until timeout occurs, returns -1 on timeout >0 >0 blocks until
         * timeout, returns - 1 on timeout, magnitude of threshold doesn't play
         * a role. 0 >0 Blocks until 1 byte, magnitude of threshold doesn't play
         * a role
         */
        public synchronized int read() throws IOException {
            if (DEBUG_READ) {
                sys.reportln("RXTXPort:SerialInputStream:read() called");
            }
            if (fd == 0) {
                throw new IOException();
            }
            if (monThreadisInterrupted) {
                sys.reportln("+++++++++ read() monThreadisInterrupted");
            }
            synchronized (IOLockedMutex) {
                IOLocked++;
            }
            try {
                if (DEBUG_READ_RESULTS) {
                    sys.reportln("RXTXPort:SerialInputStream:read() L");
                }
                waitForTheNativeCodeSilly();
                if (DEBUG_READ_RESULTS) {
                    sys.reportln("RXTXPort:SerialInputStream:read() N");
                }
                int result = readByte();
                if (DEBUG_READ_RESULTS) //z.reportln(  "RXTXPort:SerialInputStream:read() returns byte = " + result );
                {
                    sys.reportln("RXTXPort:SerialInputStream:read() returns");
                }
                return result;
            } finally {
                synchronized (IOLockedMutex) {
                    IOLocked--;
                }
            }
        }

        /*       
         * timeout threshold Behavior:
         * 0 0 blocks until 1 byte is available >0 0 blocks until timeout
         * occurs, returns 0 on timeout >0 >0 blocks until timeout or reads
         * threshold bytes, returns 0 on timeout 0 >0 blocks until reads
         * threshold bytes
         */
        public synchronized int read(byte b[]) throws IOException {
            int result;
            if (DEBUG_READ) {
                sys.reportln("RXTXPort:SerialInputStream:read(" + b.length + ") called");
            }
            if (monThreadisInterrupted == true) {
                return 0;
            }
            synchronized (IOLockedMutex) {
                IOLocked++;
            }
            try {
                waitForTheNativeCodeSilly();
                result = read(b, 0, b.length);
                if (DEBUG_READ_RESULTS) {
                    sys.reportln("RXTXPort:SerialInputStream:read() returned " + result + " bytes");
                }
                return result;
            } finally {
                synchronized (IOLockedMutex) {
                    IOLocked--;
                }
            }
        }


        /*
         * timeout threshold Behavior:
         * 0 0 blocks until 1 byte is available >0 0 blocks until timeout
         * occurs, returns 0 on timeout >0 >0 blocks until timeout or reads
         * threshold bytes, returns 0 on timeout 0 >0 blocks until either
         * threshold # of bytes or len bytes, whichever was lower.
         */
        public synchronized int read(byte b[], int off, int len)
                throws IOException {
            if (DEBUG_READ) {
                sys.reportln("RXTXPort:SerialInputStream:read(" + b.length + " " + off + " " + len + ") called" /*
                         * + new String(b)
                         */);
            }
            int result;
            /*
             * Some sanity checks
             */
            if (fd == 0) {
                if (DEBUG_READ) {
                    sys.reportln("RXTXPort:SerialInputStream:read() fd == 0");
                }
                sys.reportln("+++++++ IOException()\n");
                throw new IOException();
            }

            if (b == null) {
                sys.reportln("+++++++ NullPointerException()\n");
                if (DEBUG_READ) {
                    sys.reportln("RXTXPort:SerialInputStream:read() b == 0");
                }
                throw new NullPointerException();
            }

            if ((off < 0) || (len < 0) || (off + len > b.length)) {
                sys.reportln("+++++++ IndexOutOfBoundsException()\n");
                if (DEBUG_READ) {
                    sys.reportln("RXTXPort:SerialInputStream:read() off < 0 ..");
                }
                throw new IndexOutOfBoundsException();
            }

            /*
             * Return immediately if len==0
             */
            if (len == 0) {
                if (DEBUG_READ) {
                    sys.reportln("RXTXPort:SerialInputStream:read() off < 0 ..");
                }
                return 0;
            }
            /*
             * See how many bytes we should read
             */
            int Minimum = len;

            if (threshold == 0) {
                /*
                 * If threshold is disabled, read should return as soon as data
                 * are available (up to the amount of available bytes in order
                 * to avoid blocking) Read may return earlier depending of the
                 * receive time out.
                 */
                int a = nativeavailable();
                if (a == 0) {
                    Minimum = 1;
                } else {
                    Minimum = Math.min(Minimum, a);
                }
            } else {
                /*
                 * Threshold is enabled. Read should return when 'threshold'
                 * bytes have been received (or when the receive timeout
                 * expired)
                 */
                Minimum = Math.min(Minimum, threshold);
            }
            if (monThreadisInterrupted == true) {
                if (DEBUG_READ) {
                    sys.reportln("RXTXPort:SerialInputStream:read() Interrupted");
                }
                return 0;
            }
            synchronized (IOLockedMutex) {
                IOLocked++;
            }
            try {
                waitForTheNativeCodeSilly();
                result = readArray(b, off, Minimum);
                if (DEBUG_READ_RESULTS) {
                    sys.reportln("RXTXPort:SerialInputStream:read(" + b.length + " " + off + " " + len + ") returned " + result + " bytes" /*
                             * + new String(b)
                             */);
                }
                return result;
            } finally {
                synchronized (IOLockedMutex) {
                    IOLocked--;
                }
            }
        }

        /*
         * We are trying to catch the terminator in the native code Right now it
         * is assumed that t[] is an array of 2 bytes.
         *
         * if the read encounters the two bytes, it will return and the array
         * will contain the terminator. Otherwise read behavior should be the
         * same as read( b[], off, len ). Timeouts have not been well tested.
         */
        public synchronized int read(byte b[], int off, int len, byte t[])
                throws IOException {
            if (DEBUG_READ) {
                sys.reportln("RXTXPort:SerialInputStream:read(" + b.length + " " + off + " " + len + ") called" /*
                         * + new String(b)
                         */);
            }
            int result;
            /*
             * Some sanity checks
             */
            if (fd == 0) {
                if (DEBUG_READ) {
                    sys.reportln("RXTXPort:SerialInputStream:read() fd == 0");
                }
                sys.reportln("+++++++ IOException()\n");
                throw new IOException();
            }

            if (b == null) {
                sys.reportln("+++++++ NullPointerException()\n");
                if (DEBUG_READ) {
                    sys.reportln("RXTXPort:SerialInputStream:read() b == 0");
                }
                throw new NullPointerException();
            }

            if ((off < 0) || (len < 0) || (off + len > b.length)) {
                sys.reportln("+++++++ IndexOutOfBoundsException()\n");
                if (DEBUG_READ) {
                    sys.reportln("RXTXPort:SerialInputStream:read() off < 0 ..");
                }
                throw new IndexOutOfBoundsException();
            }

            /*
             * Return immediately if len==0
             */
            if (len == 0) {
                if (DEBUG_READ) {
                    sys.reportln("RXTXPort:SerialInputStream:read() off < 0 ..");
                }
                return 0;
            }
            /*
             * See how many bytes we should read
             */
            int Minimum = len;

            if (threshold == 0) {
                /*
                 * If threshold is disabled, read should return as soon as data
                 * are available (up to the amount of available bytes in order
                 * to avoid blocking) Read may return earlier depending of the
                 * receive time out.
                 */
                int a = nativeavailable();
                if (a == 0) {
                    Minimum = 1;
                } else {
                    Minimum = Math.min(Minimum, a);
                }
            } else {
                /*
                 * Threshold is enabled. Read should return when 'threshold'
                 * bytes have been received (or when the receive timeout
                 * expired)
                 */
                Minimum = Math.min(Minimum, threshold);
            }
            if (monThreadisInterrupted == true) {
                if (DEBUG_READ) {
                    sys.reportln("RXTXPort:SerialInputStream:read() Interrupted");
                }
                return 0;
            }
            synchronized (IOLockedMutex) {
                IOLocked++;
            }
            try {
                waitForTheNativeCodeSilly();
                result = readTerminatedArray(b, off, Minimum, t);
                if (DEBUG_READ_RESULTS) {
                    sys.reportln("RXTXPort:SerialInputStream:read(" + b.length + " " + off + " " + len + ") returned " + result + " bytes" /*
                             * + new String(b)
                             */);
                }
                return result;
            } finally {
                synchronized (IOLockedMutex) {
                    IOLocked--;
                }
            }
        }

        public synchronized int available() throws IOException {
            if (monThreadisInterrupted == true) {
                return 0;
            }
            if (DEBUG_VERBOSE) {
                sys.reportln("RXTXPort:available() called");
            }
            synchronized (IOLockedMutex) {
                IOLocked++;
            }
            try {
                int r = nativeavailable();
                if (DEBUG_VERBOSE) {
                    sys.reportln("RXTXPort:available() returning "
                            + r);
                }
                return r;
            } finally {
                synchronized (IOLockedMutex) {
                    IOLocked--;
                }
            }
        }
    }

    class MonitorThread extends Thread {

        /**
         * Note: these have to be separate boolean flags because the
         * SerialPortEvent constants are NOT bit-flags, they are just defined as
         * integers from 1 to 10 -DPL
         */
        private volatile boolean CTS = false;
        private volatile boolean DSR = false;
        private volatile boolean RI = false;
        private volatile boolean CD = false;
        private volatile boolean OE = false;
        private volatile boolean PE = false;
        private volatile boolean FE = false;
        private volatile boolean BI = false;
        private volatile boolean Data = false;
        private volatile boolean Output = false;

        MonitorThread() {
            setDaemon(true);
            if (DEBUG) {
                sys.reportln("RXTXPort:MontitorThread:MonitorThread()");
            }
        }

        /**
         * run the thread and call the event loop.
         */
        public void run() {
            if (DEBUG) {
                sys.reportln("RXTXPort:MontitorThread:run()");
            }
            monThreadisInterrupted = false;
            eventLoop();
            if (DEBUG) {
                sys.reportln("eventLoop() returned");
            }
        }

        protected void finalize() throws Throwable {
            if (DEBUG) {
                sys.reportln("RXTXPort:MonitorThread exiting");
            }
        }
    }

    /**
     * A dummy method added so RXTX compiles on Kaffee
     *
     * @deprecated deprecated but used in Kaffe
     */
    public void setRcvFifoTrigger(int trigger) {
    }

    private native static void nativeStaticSetSerialPortParams(String f,
            int b, int d, int s, int p)
            throws UnsupportedCommOperationException;

    private native static boolean nativeStaticSetDSR(String port,
            boolean flag)
            throws UnsupportedCommOperationException;

    private native static boolean nativeStaticSetDTR(String port,
            boolean flag)
            throws UnsupportedCommOperationException;

    private native static boolean nativeStaticSetRTS(String port,
            boolean flag)
            throws UnsupportedCommOperationException;

    private native static boolean nativeStaticIsDSR(String port)
            throws UnsupportedCommOperationException;

    private native static boolean nativeStaticIsDTR(String port)
            throws UnsupportedCommOperationException;

    private native static boolean nativeStaticIsRTS(String port)
            throws UnsupportedCommOperationException;

    private native static boolean nativeStaticIsCTS(String port)
            throws UnsupportedCommOperationException;

    private native static boolean nativeStaticIsCD(String port)
            throws UnsupportedCommOperationException;

    private native static boolean nativeStaticIsRI(String port)
            throws UnsupportedCommOperationException;

    private native static int nativeStaticGetBaudRate(String port)
            throws UnsupportedCommOperationException;

    private native static int nativeStaticGetDataBits(String port)
            throws UnsupportedCommOperationException;

    private native static int nativeStaticGetParity(String port)
            throws UnsupportedCommOperationException;

    private native static int nativeStaticGetStopBits(String port)
            throws UnsupportedCommOperationException;

    private native byte nativeGetParityErrorChar()
            throws UnsupportedCommOperationException;

    private native boolean nativeSetParityErrorChar(byte b)
            throws UnsupportedCommOperationException;

    private native byte nativeGetEndOfInputChar()
            throws UnsupportedCommOperationException;

    private native boolean nativeSetEndOfInputChar(byte b)
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

    private native boolean nativeClearCommInput()
            throws UnsupportedCommOperationException;

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be
     * supported on all operating systems.
     *
     * This is only accurate up to 38600 baud currently.
     *
     * @param port the name of the port thats been preopened
     * @return BaudRate on success
     * @throws UnsupportedCommOperationException; This will not behave as
     * expected with custom speeds
     *
     */
    public static int staticGetBaudRate(String port)
            throws UnsupportedCommOperationException {
        if (DEBUG) {
            sys.reportln(
                    "RXTXPort:staticGetBaudRate( " + port + " )");
        }
        return nativeStaticGetBaudRate(port);
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be
     * supported on all operating systems.
     *
     * @param port the name of the port thats been preopened
     * @return DataBits on success
     * @throws UnsupportedCommOperationException;
     *
     */
    public static int staticGetDataBits(String port)
            throws UnsupportedCommOperationException {
        if (DEBUG) {
            sys.reportln(
                    "RXTXPort:staticGetDataBits( " + port + " )");
        }
        return nativeStaticGetDataBits(port);
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be
     * supported on all operating systems.
     *
     * @param port the name of the port thats been preopened
     * @return Parity on success
     * @throws UnsupportedCommOperationException;
     *
     */
    public static int staticGetParity(String port)
            throws UnsupportedCommOperationException {
        if (DEBUG) {
            sys.reportln(
                    "RXTXPort:staticGetParity( " + port + " )");
        }
        return nativeStaticGetParity(port);
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be
     * supported on all operating systems.
     *
     * @param port the name of the port thats been preopened
     * @return StopBits on success
     * @throws UnsupportedCommOperationException;
     *
     */
    public static int staticGetStopBits(String port)
            throws UnsupportedCommOperationException {
        if (DEBUG) {
            sys.reportln(
                    "RXTXPort:staticGetStopBits( " + port + " )");
        }
        return nativeStaticGetStopBits(port);
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be
     * supported on all operating systems.
     *
     * Set the SerialPort parameters 1.5 stop bits requires 5 databits
     *
     * @param f filename
     * @param b baudrate
     * @param d databits
     * @param s stopbits
     * @param p parity
     *
     * @throws UnsupportedCommOperationException
     * @see gnu.io.UnsupportedCommOperationException
     */
    public static void staticSetSerialPortParams(String f, int b, int d,
            int s, int p)
            throws UnsupportedCommOperationException {
        if (DEBUG) {
            sys.reportln(
                    "RXTXPort:staticSetSerialPortParams( "
                    + f + " " + b + " " + d + " " + s + " " + p);
        }
        nativeStaticSetSerialPortParams(f, b, d, s, p);
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be
     * supported on all operating systems.
     *
     * Open the port and set DSR. remove lockfile and do not close This is so
     * some software can appear to set the DSR before 'opening' the port a
     * second time later on.
     *
     * @return true on success
     * @throws UnsupportedCommOperationException;
     *
     */
    public static boolean staticSetDSR(String port, boolean flag)
            throws UnsupportedCommOperationException {
        if (DEBUG) {
            sys.reportln("RXTXPort:staticSetDSR( " + port
                    + " " + flag);
        }
        return nativeStaticSetDSR(port, flag);
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be
     * supported on all operating systems.
     *
     * Open the port and set DTR. remove lockfile and do not close This is so
     * some software can appear to set the DTR before 'opening' the port a
     * second time later on.
     *
     * @return true on success
     * @throws UnsupportedCommOperationException;
     *
     */
    public static boolean staticSetDTR(String port, boolean flag)
            throws UnsupportedCommOperationException {
        if (DEBUG) {
            sys.reportln("RXTXPort:staticSetDTR( " + port
                    + " " + flag);
        }
        return nativeStaticSetDTR(port, flag);
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be
     * supported on all operating systems.
     *
     * Open the port and set RTS. remove lockfile and do not close This is so
     * some software can appear to set the RTS before 'opening' the port a
     * second time later on.
     *
     * @return none
     * @throws UnsupportedCommOperationException;
     *
     */
    public static boolean staticSetRTS(String port, boolean flag)
            throws UnsupportedCommOperationException {
        if (DEBUG) {
            sys.reportln("RXTXPort:staticSetRTS( " + port
                    + " " + flag);
        }
        return nativeStaticSetRTS(port, flag);
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be
     * supported on all operating systems.
     *
     * find the fd and return RTS without using a Java open() call
     *
     * @param port
     * @return true if asserted
     * @throws UnsupportedCommOperationException;
     *
     */
    public static boolean staticIsRTS(String port)
            throws UnsupportedCommOperationException {
        if (DEBUG) {
            sys.reportln("RXTXPort:staticIsRTS( " + port + " )");
        }
        return nativeStaticIsRTS(port);
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be
     * supported on all operating systems.
     *
     * find the fd and return CD without using a Java open() call
     *
     * @param port
     * @return true if asserted
     * @throws UnsupportedCommOperationException;
     *
     */
    public static boolean staticIsCD(String port)
            throws UnsupportedCommOperationException {
        if (DEBUG) {
            sys.reportln("RXTXPort:staticIsCD( " + port + " )");
        }
        return nativeStaticIsCD(port);
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be
     * supported on all operating systems.
     *
     * find the fd and return CTS without using a Java open() call
     *
     * @param port
     * @return true if asserted
     * @throws UnsupportedCommOperationException;
     *
     */
    public static boolean staticIsCTS(String port)
            throws UnsupportedCommOperationException {
        if (DEBUG) {
            sys.reportln("RXTXPort:staticIsCTS( " + port + " )");
        }
        return nativeStaticIsCTS(port);
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be
     * supported on all operating systems.
     *
     * find the fd and return DSR without using a Java open() call
     *
     * @param port
     * @return true if asserted
     * @throws UnsupportedCommOperationException;
     *
     */
    public static boolean staticIsDSR(String port)
            throws UnsupportedCommOperationException {
        if (DEBUG) {
            sys.reportln("RXTXPort:staticIsDSR( " + port + " )");
        }
        return nativeStaticIsDSR(port);
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be
     * supported on all operating systems.
     *
     * find the fd and return DTR without using a Java open() call
     *
     * @param port
     * @return true if asserted
     * @throws UnsupportedCommOperationException;
     *
     */
    public static boolean staticIsDTR(String port)
            throws UnsupportedCommOperationException {
        if (DEBUG) {
            sys.reportln("RXTXPort:staticIsDTR( " + port + " )");
        }
        return nativeStaticIsDTR(port);
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be
     * supported on all operating systems.
     *
     * find the fd and return RI without using a Java open() call
     *
     * @param port
     * @return true if asserted
     * @throws UnsupportedCommOperationException;
     *
     */
    public static boolean staticIsRI(String port)
            throws UnsupportedCommOperationException {
        if (DEBUG) {
            sys.reportln("RXTXPort:staticIsRI( " + port + " )");
        }
        return nativeStaticIsRI(port);
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be
     * supported on all operating systems.
     *
     * @return int the Parity Error Character
     * @throws UnsupportedCommOperationException;
     *
     * Anyone know how to do this in Unix?
     */
    public byte getParityErrorChar()
            throws UnsupportedCommOperationException {
        byte ret;
        if (DEBUG) {
            sys.reportln("getParityErrorChar()");
        }
        ret = nativeGetParityErrorChar();
        if (DEBUG) {
            sys.reportln("getParityErrorChar() returns "
                    + ret);
        }
        return ret;
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be
     * supported on all operating systems.
     *
     * @param b Parity Error Character
     * @return boolean true on success
     * @throws UnsupportedCommOperationException;
     *
     * Anyone know how to do this in Unix?
     */
    public boolean setParityErrorChar(byte b)
            throws UnsupportedCommOperationException {
        if (DEBUG) {
            sys.reportln("setParityErrorChar(" + b + ")");
        }
        return nativeSetParityErrorChar(b);
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be
     * supported on all operating systems.
     *
     * @return int the End of Input Character
     * @throws UnsupportedCommOperationException;
     *
     * Anyone know how to do this in Unix?
     */
    public byte getEndOfInputChar()
            throws UnsupportedCommOperationException {
        byte ret;
        if (DEBUG) {
            sys.reportln("getEndOfInputChar()");
        }
        ret = nativeGetEndOfInputChar();
        if (DEBUG) {
            sys.reportln("getEndOfInputChar() returns "
                    + ret);
        }
        return ret;
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be
     * supported on all operating systems.
     *
     * @param b End Of Input Character
     * @return boolean true on success
     * @throws UnsupportedCommOperationException;
     */
    public boolean setEndOfInputChar(byte b)
            throws UnsupportedCommOperationException {
        if (DEBUG) {
            sys.reportln("setEndOfInputChar(" + b + ")");
        }
        return nativeSetEndOfInputChar(b);
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be
     * supported on all operating systems.
     *
     * @param type String representation of the UART type which mayb be "none",
     * "8250", "16450", "16550", "16550A", "16650", "16550V2" or "16750".
     * @param test boolean flag to determin if the UART should be tested.
     * @return boolean true on success
     * @throws UnsupportedCommOperationException;
     */
    public boolean setUARTType(String type, boolean test)
            throws UnsupportedCommOperationException {
        if (DEBUG) {
            sys.reportln("RXTXPort:setUARTType()");
        }
        return nativeSetUartType(type, test);
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be
     * supported on all operating systems.
     *
     * @return type String representation of the UART type which mayb be "none",
     * "8250", "16450", "16550", "16550A", "16650", "16550V2" or "16750".
     * @throws UnsupportedCommOperationException;
     */
    public String getUARTType() throws UnsupportedCommOperationException {
        return nativeGetUartType();
    }

    /**
     * Extension to CommAPI. Set Baud Base to 38600 on Linux and W32 before
     * using.
     *
     * @param BaudBase The clock frequency divided by 16. Default BaudBase is
     * 115200.
     * @return true on success
     * @throws UnsupportedCommOperationException, IOException
     */
    public boolean setBaudBase(int BaudBase)
            throws UnsupportedCommOperationException,
            IOException {
        if (DEBUG) {
            sys.reportln("RXTXPort:setBaudBase()");
        }
        return nativeSetBaudBase(BaudBase);
    }

    /**
     * Extension to CommAPI
     *
     * @return BaudBase
     * @throws UnsupportedCommOperationException, IOException
     */
    public int getBaudBase() throws UnsupportedCommOperationException,
            IOException {
        if (DEBUG) {
            sys.reportln("RXTXPort:getBaudBase()");
        }
        return nativeGetBaudBase();
    }

    /**
     * Extension to CommAPI. Set Baud Base to 38600 on Linux and W32 before
     * using.
     *
     * @param Divisor
     * @throws UnsupportedCommOperationException, IOException
     */
    public boolean setDivisor(int Divisor)
            throws UnsupportedCommOperationException, IOException {
        if (DEBUG) {
            sys.reportln("RXTXPort:setDivisor()");
        }
        return nativeSetDivisor(Divisor);
    }

    /**
     * Extension to CommAPI
     *
     * @return Divisor;
     * @throws UnsupportedCommOperationException, IOException
     */
    public int getDivisor() throws UnsupportedCommOperationException,
            IOException {
        if (DEBUG) {
            sys.reportln("RXTXPort:getDivisor()");
        }
        return nativeGetDivisor();
    }

    /**
     * Extension to CommAPI returns boolean true on success
     *
     * @throws UnsupportedCommOperationException
     */
    public boolean setLowLatency() throws UnsupportedCommOperationException {
        if (DEBUG) {
            sys.reportln("RXTXPort:setLowLatency()");
        }
        return nativeSetLowLatency();
    }

    /**
     * Extension to CommAPI returns boolean true on success
     *
     * @throws UnsupportedCommOperationException
     */
    public boolean getLowLatency() throws UnsupportedCommOperationException {
        if (DEBUG) {
            sys.reportln("RXTXPort:getLowLatency()");
        }
        return nativeGetLowLatency();
    }

    /**
     * Extension to CommAPI returns boolean true on success
     *
     * @throws UnsupportedCommOperationException
     */
    public boolean setCallOutHangup(boolean NoHup)
            throws UnsupportedCommOperationException {
        if (DEBUG) {
            sys.reportln("RXTXPort:setCallOutHangup()");
        }
        return nativeSetCallOutHangup(NoHup);
    }

    /**
     * Extension to CommAPI returns boolean true on success
     *
     * @throws UnsupportedCommOperationException
     */
    public boolean getCallOutHangup()
            throws UnsupportedCommOperationException {
        if (DEBUG) {
            sys.reportln("RXTXPort:getCallOutHangup()");
        }
        return nativeGetCallOutHangup();
    }

    /**
     * Extension to CommAPI returns boolean true on success
     *
     * @throws UnsupportedCommOperationException
     */
    public boolean clearCommInput()
            throws UnsupportedCommOperationException {
        if (DEBUG) {
            sys.reportln("RXTXPort:clearCommInput()");
        }
        return nativeClearCommInput();
    }
}
