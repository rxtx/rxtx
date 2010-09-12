/*-------------------------------------------------------------------------
|   RXTX License v 2.1 - LGPL v 2.1 + Linking Over Controlled Interface.
|   RXTX is a native interface to serial ports in java.
|   Copyright 1997-2007 by Trent Jarvi tjarvi@qbang.org and others who
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
package gnu.io;

import java.util.TooManyListenersException;

/**
 * An RS-232 serial communications port. <CODE>SerialPort</CODE> describes the low-level
 * interface to a serial communications port made available by the underlying system.
 * <CODE>SerialPort</CODE> defines the minimum required functionality for serial
 * communications ports.
 * 
 * @author <a href="http://www.rxtx.org">The RXTX Project</a>
 */
public abstract class SerialPort extends CommPort {
    /**
     * 5 data bit format.
     */
    public static final int DATABITS_5 = 5;
    /**
     * 6 data bit format.
     */
    public static final int DATABITS_6 = 6;
    /**
     * 7 data bit format.
     */
    public static final int DATABITS_7 = 7;
    /**
     * 8 data bit format.
     */
    public static final int DATABITS_8 = 8;
    /**
     * No parity bit.
     */
    public static final int PARITY_NONE = 0;
    /**
     * Odd parity scheme. The parity bit is added so there are an odd number of TRUE bits.
     */
    public static final int PARITY_ODD = 1;
    /**
     * Even parity scheme. The parity bit is added so there are an even number of TRUE bits.
     */
    public static final int PARITY_EVEN = 2;
    /**
     * Mark parity scheme.
     */
    public static final int PARITY_MARK = 3;
    /**
     * Space parity scheme.
     */
    public static final int PARITY_SPACE = 4;
    /**
     * One stop bit.
     */
    public static final int STOPBITS_1 = 1;
    /**
     * Two stop bits.
     */
    public static final int STOPBITS_2 = 2;
    /**
     * One and 1/2 stop bits. Some UARTs permit 1-1/2 stop bits only with 5 data bit
     * format, but permit 1 or 2 stop bits with any format.
     */
    public static final int STOPBITS_1_5 = 3;
    /**
     * Flow control off.
     */
    public static final int FLOWCONTROL_NONE = 0;
    /**
     * RTS/CTS flow control on input.
     */
    public static final int FLOWCONTROL_RTSCTS_IN = 1;
    /**
     * RTS/CTS flow control on output.
     */
    public static final int FLOWCONTROL_RTSCTS_OUT = 2;
    /**
     * XON/XOFF flow control on input.
     */
    public static final int FLOWCONTROL_XONXOFF_IN = 4;
    /**
     * XON/XOFF flow control on output.
     */
    public static final int FLOWCONTROL_XONXOFF_OUT = 8;

    protected SerialPort(CommPortIdentifier cpi, int portHandle) {
        super(cpi, portHandle);
    }

    /**
     * Registers a <CODE>SerialPortEventListener</CODE> object to listen for
     * <CODE>SerialEvent</CODE>s. Interest in specific events may be expressed using the
     * <CODE>notifyOn<I>XXX</I></CODE> calls. The <CODE>serialEvent</CODE> method of
     * <CODE>SerialPortEventListener</CODE> will be called with a <CODE>SerialEvent</CODE>
     * object describing the event.
     * <P>
     * The current implementation only allows one listener per <CODE>SerialPort</CODE>.
     * Once a listener is registered, subsequent call attempts to
     * <CODE>addEventListener</CODE> will throw a <code>TooManyListenersException</code>
     * without affecting the listener already registered.
     * <P>
     * All the events received by this listener are generated by one dedicated thread that
     * belongs to the <code>SerialPort</code> object. After the port is closed, no more
     * events will be generated. Another call to <CODE>open()</CODE> of the port's
     * <CODE>CommPortIdentifier</CODE> object will return a new <CODE>CommPort</CODE>
     * object, and the listener has to be added again to the new <CODE>CommPort</CODE>
     * object to receive events from this port.
     * 
     * @param listener
     * @throws TooManyListenersException
     */
    public abstract void addEventListener(SerialPortEventListener listener) throws TooManyListenersException;

    /**
     * Returns the currently configured baud rate.
     * @return The currently configured baud rate.
     */
    public abstract int getBaudRate();

    /**
     * Returns the currently configured number of data bits.
     * @return The currently configured number of data bits.
     */
    public abstract int getDataBits();

    /**
     * Returns the currently configured flow control mode.
     * @return The currently configured flow control mode.
     */
    public abstract int getFlowControlMode();

    /**
     * Returns the currently configured parity setting.
     * @return The currently configured parity setting.
     */
    public abstract int getParity();

    /**
     * Returns the currently defined stop bits.
     * @return The currently defined stop bits.
     */
    public abstract int getStopBits();

    /**
     * Returns the state of the CD (Carrier Detect) bit in the UART, if supported by the
     * underlying implementation.
     * 
     * @return The state of the CD (Carrier Detect) bit.
     */
    public abstract boolean isCD();

    /**
     * Returns the state of the CTS (Clear To Send) bit in the UART, if supported by the
     * underlying implementation.
     * 
     * @return The state of the CTS (Clear To Send) bit.
     */
    public abstract boolean isCTS();

    /**
     * Returns the state of the DSR (Data Set Ready) bit in the UART, if supported by the
     * underlying implementation.
     * 
     * @return The state of the DSR (Data Set Ready) bit.
     */
    public abstract boolean isDSR();

    /**
     * Returns the state of the DTR (Data Terminal Ready) bit in the UART, if supported by the
     * underlying implementation.
     * 
     * @return The state of the DTR (Data Terminal Ready) bit.
     */
    public abstract boolean isDTR();

    /**
     * Returns the state of the RI (Ring Indicator) bit in the UART, if supported by the
     * underlying implementation.
     * 
     * @return The state of the RI (Ring Indicator) bit.
     */
    public abstract boolean isRI();

    /**
     * Returns the state of the RTS (Request To Send) bit in the UART, if supported by the
     * underlying implementation.
     * 
     * @return The state of the RTS (Request To Send) bit.
     */
    public abstract boolean isRTS();

    /**
     * Expresses interest in receiving notification when there is a break interrupt on the
     * line.
     * <P>
     * This notification is hardware dependent and may not be supported by all
     * implementations.
     * 
     * @param enable
     */
    public abstract void notifyOnBreakInterrupt(boolean enable);

    /**
     * Expresses interest in receiving notification when the CD (Carrier Detect) bit
     * changes.
     * <P>
     * This notification is hardware dependent and may not be supported by all
     * implementations.
     * 
     * @param enable
     */
    public abstract void notifyOnCarrierDetect(boolean enable);

    /**
     * Expresses interest in receiving notification when the CTS (Clear To Send) bit
     * changes.
     * <P>
     * This notification is hardware dependent and may not be supported by all
     * implementations.
     * 
     * @param enable
     */
    public abstract void notifyOnCTS(boolean enable);

    /**
     * Expresses interest in receiving notification when input data is available. This may
     * be used to drive asynchronous input. When data is available in the input buffer,
     * this event is propagated to the listener registered using
     * <CODE>addEventListener</CODE>.
     * <P>
     * The event will be generated once when new data arrive at the serial port. Even if
     * the user doesn't read the data, it won't be generated again until next time new
     * data arrive.
     * 
     * @param enable
     */
    public abstract void notifyOnDataAvailable(boolean enable);

    /**
     * Expresses interest in receiving notification when the DSR (Data Set Ready) bit
     * changes.
     * <P>
     * This notification is hardware dependent and may not be supported by all
     * implementations.
     * 
     * @param enable
     */
    public abstract void notifyOnDSR(boolean enable);

    /**
     * Expresses interest in receiving notification when there is a framing error.
     * <P>
     * This notification is hardware dependent and may not be supported by all
     * implementations.
     * 
     * @param enable
     */
    public abstract void notifyOnFramingError(boolean enable);

    /**
     * Expresses interest in receiving notification when the output buffer is empty. This
     * may be used to drive asynchronous output. When the output buffer becomes empty,
     * this event is propagated to the listener registered using
     * <CODE>addEventListener</CODE>. The event will be generated after a write is
     * completed, when the system buffer becomes empty again.
     * <P>
     * This notification is hardware dependent and may not be supported by all
     * implementations.
     * 
     * @param enable
     */
    public abstract void notifyOnOutputEmpty(boolean enable);

    /**
     * Expresses interest in receiving notification when there is an overrun error.
     * <P>
     * This notification is hardware dependent and may not be supported by all
     * implementations.
     * 
     * @param enable
     */
    public abstract void notifyOnOverrunError(boolean enable);

    /**
     * Expresses interest in receiving notification when there is a parity error.
     * <P>
     * This notification is hardware dependent and may not be supported by all
     * implementations.
     * 
     * @param enable
     */
    public abstract void notifyOnParityError(boolean enable);

    /**
     * Expresses interest in receiving notification when the RI (Ring Indicator) bit
     * changes.
     * <P>
     * This notification is hardware dependent and may not be supported by all
     * implementations.
     * 
     * @param enable
     */
    public abstract void notifyOnRingIndicator(boolean enable);

    /**
     * Deregisters event listener registered using <CODE>addEventListener</CODE>.
     * <P>
     * This is done automatically when the port is closed.
     */
    public abstract void removeEventListener();

    /**
     * Sends a break of <CODE>duration</CODE> milliseconds duration. Note that it may not
     * be possible to time the duration of the break under certain Operating Systems.
     * Hence this parameter is advisory.
     * 
     * @param duration The break duration in milliseconds.
     */
    public abstract void sendBreak(int duration);

    /**
     * Sets or clears the DTR (Data Terminal Ready) bit in the UART, if supported by the
     * underlying implementation.
     * 
     * @param state
     */
    public abstract void setDTR(boolean state);

    /**
     * Sets the flow control mode.
     * <P>
     * The <CODE>flowcontrol</CODE> argument can be a bitmask (bitwise AND)
     * combination of:
     * <UL>
     * <LI><code>FLOWCONTROL_NONE</code>: no flow control.</li>
     * <LI><code>FLOWCONTROL_RTSCTS_IN</code>: RTS/CTS (hardware) flow control for input.</li>
     * <LI><code>FLOWCONTROL_RTSCTS_OUT</code>: RTS/CTS (hardware) flow control for output.</li>
     * <LI><code>FLOWCONTROL_XONXOFF_IN</code>: XON/XOFF (software) flow control for input.</li>
     * <LI><code>FLOWCONTROL_XONXOFF_OUT</code>: XON/XOFF (software) flow control for output.</li>
     * </UL>
     * 
     * @param flowcontrol
     * @throws UnsupportedCommOperationException
     */
    public abstract void setFlowControlMode(int flowcontrol) throws UnsupportedCommOperationException;

    /**
     * Sets or clears the RTS (Request To Send) bit in the UART, if supported by the
     * underlying implementation.
     * 
     * @param state
     */
    public abstract void setRTS(boolean state);

    /**
     * Sets the serial port parameters.
     * <P>
     * Default: 9600 baud, 8 data bits, 1 stop bit, no parity.
     * 
     * @param baudRate
     * @param dataBits
     * @param stopBits
     * @param parity
     * @throws UnsupportedCommOperationException
     */
    public abstract void setSerialPortParams(int baudRate, int dataBits, int stopBits, int parity) throws UnsupportedCommOperationException;
}
