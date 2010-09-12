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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * A communications port. <code>CommPort</code> is an abstract class that describes a
 * communications port made available by the underlying system. It includes high-level
 * methods for controlling I/O that are common to different kinds of communications
 * ports. {@link gnu.io.SerialPort} and {@link gnu.io.ParallelPort} are subclasses of
 * <code>CommPort</code> that include additional methods for low-level control of
 * physical communications ports.
 * 
 * <p>
 * There are no public constructors for <code>CommPort</code>. Instead, an application
 * should use the static method {@link gnu.io.CommPortIdentifier#getPortIdentifiers()}
 * to generate a list of available ports. It then chooses a port from this list and
 * calls {@link gnu.io.CommPortIdentifier#open(String, int)} to create a
 * <code>CommPort</code> object. Finally, it casts the <code>CommPort</code> object
 * to a physical communications device class like <code>SerialPort</code> or
 * <code>ParallelPort</code>.
 * </p>
 * 
 * <p>
 * After a communications port has been identified and opened it can be configured with
 * the methods in the low-level classes like <code>SerialPort</code> and
 * <code>ParallelPort</code>. Then an I/O stream can be opened for reading and writing
 * data. Once the application is done with the port, it must call the {@link #close()}
 * method. Thereafter, the application must not call any methods in the port object -
 * doing so will throw <code>IllegalStateException</code>.
 * </p>
 * 
 * <dl><dt><b>Thread-safe:</b></dt>
 * <dd>This class may be used in multi-threaded applications.</dd></dl>
 * 
 * @author <a href="http://www.rxtx.org">The RXTX Project</a>
*/
public abstract class CommPort {
    private static final String source = CommPort.class.getName();
    private static final Dispatcher dispatch = Dispatcher.getInstance();
    private static final int PORT_STATUS_OPEN = 1;
    private static final int PORT_STATUS_CLOSED = 2;
    private static final int PORT_STATUS_ERROR = 3;

    protected final CommPortIdentifier cpi;
    protected InputStream inputStream = null;
    protected OutputStream outputStream = null;
    protected final int portHandle;
    private int portStatus = PORT_STATUS_OPEN;
    protected int receiveFramingByte = 0;
    protected boolean receiveFramingEnabled = false;
    protected int receiveThreshold = 0;
    protected boolean receiveThresholdEnabled = false;
    protected int receiveTimeout = 0;
    protected boolean receiveTimeoutEnabled = false;

    protected CommPort(CommPortIdentifier cpi, int portHandle) {
        this.cpi = cpi;
        this.portHandle = portHandle;
    }

    protected synchronized void abort() {
        // TODO: Possible thread deadlock
        if (this.portStatus == PORT_STATUS_OPEN) {
            this.portStatus = PORT_STATUS_ERROR;
            synchronized (Dispatcher.class) {
                // Port open and close are guarded by Dispatcher.class
                dispatch.abort(this.portHandle);
            }
            this.cpi.portClosed();
        }
    }

    protected synchronized void abort(Throwable t) {
        Log.log(t, "Port " + getName() + " aborted: ", source);
        abort();
    }

    protected final void checkStatus() {
        if (this.portStatus != PORT_STATUS_OPEN) {
            throw new IllegalStateException("Port is closed");
        }
    }

    /**
     * Closes the communications port. The application must call <CODE>close</CODE> when
     * it is done with the port. Notification of this ownership change will be propagated
     * to all classes registered using <CODE>addPortOwnershipListener</CODE>.
     */
    public synchronized void close() {
        // TODO: Possible thread deadlock
        if (this.portStatus == PORT_STATUS_OPEN) {
            try {
                if (this.inputStream != null) {
                    this.inputStream.close();
                }
                if (this.outputStream != null) {
                    this.outputStream.close();
                }
                synchronized (Dispatcher.class) {
                    // Port open and close are guarded by Dispatcher.class
                    dispatch.close(this.portHandle);
                }
                this.portStatus = PORT_STATUS_CLOSED;
            } catch (Throwable e) {
                this.portStatus = PORT_STATUS_ERROR;
            }
            this.cpi.portClosed();
        }
    }

    /**
     * Disables receive framing.
     */
    public abstract void disableReceiveFraming();

    /**
     * Disables receive threshold.
     */
    public abstract void disableReceiveThreshold();

    /**
     * Disables receive timeout.
     */
    public abstract void disableReceiveTimeout();

    /**
     * Enables receive framing. When the receive framing condition becomes true, a
     * <CODE>read</CODE> from the input stream for this port will return immediately.
     * <P>
     * By default, receive framing is not enabled.
     * <P>
     * Note: As implemented in this method, framing is <B>not</B> related to bit-level
     * framing at the hardware level, and is <B>not</B> associated with data errors.
     * <P>
     * 
     * @param framingByte
     * @throws UnsupportedCommOperationException
     */
    public abstract void enableReceiveFraming(int framingByte) throws UnsupportedCommOperationException;

    /**
     * Enables receive threshold. When the receive threshold condition becomes true, a
     * <CODE>read</CODE> from the input stream for this port will return immediately.
     * <P>
     * By default, receive threshold is not enabled.
     * <P>
     * See <CODE>getInputStream</CODE> for a description of the exact behavior.
     * <P>
     * 
     * @param threshold
     * @throws UnsupportedCommOperationException
     */
    public abstract void enableReceiveThreshold(int threshold) throws UnsupportedCommOperationException;

    /**
     * Enables receive timeout. When the receive timeout condition becomes true, a
     * <CODE>read</CODE> from the input stream for this port will return immediately.
     * <P>
     * See <CODE>getInputStream</CODE> for a description of the exact behavior.
     * <P>
     * @param rcvTimeout
     *            Timeout value in milliseconds
     * @throws UnsupportedCommOperationException
     */
    public abstract void enableReceiveTimeout(int rcvTimeout) throws UnsupportedCommOperationException;

    /**
     * Returns the input buffer size in bytes.
     * @return The input buffer size in bytes.
     */
    public abstract int getInputBufferSize();

    /**
     * Returns an input stream. This is the only way to receive data from the
     * communications port. If the port is unidirectional and doesn't support receiving
     * data, then <CODE>getInputStream</CODE> returns null.
     * <P>
     * The read behavior of the input stream returned by <CODE>getInputStream</CODE>
     * depends on combination of the threshold and timeout values. The possible behaviors
     * are described in the table below:
     * <P>
     * <table border="1">
     * <tr>
     * <th colspan=2>Threshold</th>
     * <th colspan=2>Timeout</th>
     * <th rowspan=2>Read Buffer Size</th>
     * <th rowspan=2>Read Behaviour</th>
     * </tr>
     * <tr>
     * <th>State</th>
     * <th>Value</th>
     * <th>State</th>
     * <th>Value</th>
     * </tr>
     * <tr>
     * <td>disabled</td>
     * <td>-</td>
     * <td>disabled</td>
     * <td>-</td>
     * <td>n bytes</td>
     * <td>block until any data is available</td>
     * </tr>
     * <tr>
     * <td>enabled</td>
     * <td>m bytes</td>
     * <td>disabled</td>
     * <td>-</td>
     * <td>n bytes</td>
     * <td>block until min(<I>m</I>,<I>n</I>) bytes are available</td>
     * </tr>
     * <tr>
     * <td>disabled</td>
     * <td>-</td>
     * <td>enabled</td>
     * <td>x ms</td>
     * <td>n bytes</td>
     * <td>block for <I>x</I> ms or until any data is available</td>
     * </tr>
     * <tr>
     * <td>enabled</td>
     * <td>m bytes</td>
     * <td>enabled</td>
     * <td>x ms</td>
     * <td>n bytes</td>
     * <td>block for <I>x</I> ms or until min(<I>m</I>,<I>n</I>) bytes are available</td>
     * </tr>
     * </table>
     * <P>
     * Note, however, that framing errors may cause the Timeout and Threshold values to
     * complete prematurely without raising an exception.
     * <P>
     * 
     * @return An input stream, or <code>null</code> if the port is unidirectional and
     * doesn't support receiving data.
     * @throws IOException
     */
    public abstract InputStream getInputStream() throws IOException;

    /**
     * Returns the port name.
     * @return The port name.
     */
    public final String getName() {
        checkStatus();
        return this.cpi.getName();
    }

    /**
     * Returns the output buffer size in bytes.
     * @return The output buffer size in bytes.
     */
    public abstract int getOutputBufferSize();

    /**
     * Returns an output stream. This is the only way to send data to the communications
     * port. If the port is unidirectional and doesn't support sending data, then
     * <CODE>getOutputStream</CODE> returns null.
     * <P>
     * 
     * @return An output stream, or <code>null</code> if the port is unidirectional and
     * doesn't support sending data.
     * @throws IOException
     */
    public abstract OutputStream getOutputStream() throws IOException;

    /**
     * Returns the current byte used for receive framing. If receive framing is disabled,
     * then the value returned is meaningless. The return value of
     * <CODE>getReceiveFramingByte</CODE> is an integer - the low 8 bits of which
     * represent the current byte used for receive framing.
     * <P>
     * Note: As implemented in this method, framing is <B>not</B> related to bit-level
     * framing at the hardware level, and it is <B>not</B> associated with data errors.
     * <P>
     * 
     * @return The current byte used for receive framing.
     */
    public abstract int getReceiveFramingByte();

    /**
     * Returns the integer value of the receive threshold. If the receive threshold is
     * disabled, then the value returned is meaningless.
     * <P>
     * 
     * @return The integer value of the receive threshold.
     */
    public abstract int getReceiveThreshold();

    /**
     * Returns the integer value of the receive timeout. If the receive timeout is disabled,
     * then the value returned is meaningless.
     * <P>
     * 
     * @return The integer value of the receive timeout.
     */
    public abstract int getReceiveTimeout();

    /**
     * Returns <code>true</code> if the port status is open.
     * @return <code>true</code> if the port status is open.
     */
    protected final synchronized boolean isOpen() {
        return this.portStatus == PORT_STATUS_OPEN;
    }

    /**
     * Returns <code>true</code> if receive framing is enabled.
     * @return <code>true</code> if receive framing is enabled.
     */
    public abstract boolean isReceiveFramingEnabled();

    /**
     * Returns <code>true</code> if receive threshold is enabled.
     * @return <code>true</code> if receive threshold is enabled.
     */
    public abstract boolean isReceiveThresholdEnabled();

    /**
     * Returns <code>true</code> if receive timeout is enabled.
     * @return <code>true</code> if receive timeout is enabled.
     */
    public abstract boolean isReceiveTimeoutEnabled();

    /**
     * Reads data into the <code>buffer</code> byte array if any
     * bytes are available and returns the number of bytes read.
     * @param buffer
     * @param offset
     * @param length
     * @return The number of bytes read.
     * @throws IOException
     */
    protected final synchronized int readBytes(byte[] buffer, int offset, int length) throws IOException {
        checkStatus();
        try {
            return dispatch.readBytes(this.portHandle, buffer, offset, length);
        } catch (IOException e) {
            abort();
            throw e;
        }
    };

    /**
     * Sets the input buffer size.
     * @param size
     */
    public abstract void setInputBufferSize(int size);

    /**
     * Sets the output buffer size.
     * @param size
     */
    public abstract void setOutputBufferSize(int size);

    /**
     * Writes data from the <code>buffer</code> byte array.
     * Returns the number of bytes written.
     * @param buffer
     * @param offset
     * @param length
     * @return The number of bytes written.
     * @throws IOException
     */
    protected final synchronized int writeBytes(byte[] buffer, int offset, int length) throws IOException {
        checkStatus();
        try {
            return dispatch.writeBytes(this.portHandle, buffer, offset, length);
        } catch (IOException e) {
            abort();
            throw e;
        }
    }
}
