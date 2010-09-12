/*-------------------------------------------------------------------------
|   RXTX License v 2.1 - LGPL v 2.1 + Linking Over Controlled Interface.
|   RXTX is a native interface to serial ports in java.
|   Copyright 1998 Kevin Hester, kevinh@acm.org
|   Copyright 2000-2008 Trent Jarvi tjarvi@qbang.org and others who
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

import java.io.IOException;
import java.util.List;

/**
 * Native library dispatcher object. The <code>gnu.io.*</code> classes delegate
 * their native calls to this object. It is the central dispatcher for all
 * interactions between the <code>gnu.io</code> API and the underlying
 * platform.
 * 
 * <p>This object is a singleton - a reference to it is obtained
 * by calling the static {@link #getInstance()} method.</p>
 * 
 * <p>The <code>Dispatcher</code> class defines a contract between the Java
 * code and the native code. Both sides must obey the contract in order for
 * the RXTX library to perform as designed.</p>
 *
 * <p>Native code implementations are required to supply four ports for unit
 * testing: <code>SERIAL_PASS, SERIAL_FAIL, PARALLEL_PASS,</code> and
 * <code>PARALLEL_FAIL</code>. The <code>*_PASS</code> ports are for testing
 * successful method calls, and the <code>*_FAIL</code> ports are for
 * testing unsuccessful method calls. Details of the expected results
 * can be found in each <code>Dispatcher</code> method description.</p>
 * 
 * <p>Java code is responsible for thread synchronization - so native code is
 * shielded from multi-threaded issues. Only one thread may open or close a
 * port at a time - concurrent port open requests or concurrent port close
 * requests are not allowed. Native code port methods may be called concurrently,
 * but only one thread is allowed per port.</p>
 *  
 * <dl><dt><b>Thread-safe:</b></dt>
 * <dd>This class may be used in multi-threaded applications.</dd></dl>
 * 
 * @author <a href="http://www.rxtx.org">The RXTX Project</a>
*/
public final class Dispatcher {
    /* Developers: This class should not have any behaviors other than
     * loading the native library.
     */
    private static final Dispatcher instance = new Dispatcher();

    static {
        System.loadLibrary("RXTXnative");
    }

    /**
     * Returns a <code>Dispatcher</code> object.
     * @return A <code>Dispatcher</code> object.
     */
    public static Dispatcher getInstance() {
        return instance;
    }

    private Dispatcher() {
    }

    /**
     * Aborts all port operations - called during system shutdown or when something
     * catastrophic has happened.
     * <p>
     * Native code should try to clean up internal data structures and release resources
     * as best as possible - absorbing any errors or exceptions along the way. If the
     * port hardware supports it, a hardware reset should be performed. It is crucial
     * that this method return quickly and silently without throwing any additional
     * exceptions or generating errors.
     * </p>
     * <p>
     * Java code must synchronize on <code>Dispatcher.class</code> when calling this
     * method.
     * </p>
     * 
     * @param portHandle The port handle or token.
     * 
     * </dl><dl>
     * <dt><b>Unit tests:</b></dt>
     * <dd><code>SERIAL_PASS, SERIAL_FAIL, PARALLEL_PASS,</code> and
     * <code>PARALLEL_FAIL</code> - Do nothing.</dd></dl>
     */
    public native void abort(int portHandle);

    /**
     * Closes an open port.
     * 
     * <p>
     * Java code must synchronize on <code>Dispatcher.class</code> when calling this
     * method.
     * </p>
     * @param portHandle The port handle or token.
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>SERIAL_PASS, PARALLEL_PASS</code> - Do nothing.</dd>
     * <dd><code>SERIAL_FAIL, PARALLEL_FAIL</code> - Throw
     * <code>IOException</code>.</dd></dl>
     */
    public native void close(int portHandle) throws IOException;

    /**
     * Returns the currently configured baud rate.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @return The currently configured baud rate.
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>SERIAL_PASS</code> - Return the current baud rate.</dd>
     * <dd><code>SERIAL_FAIL</code> - Throw
     * <code>IOException</code>.</dd></dl>
     */
    public native int getBaudRate(int portHandle) throws IOException;

    /**
     * Returns the currently configured number of data bits.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @return The currently configured number of data bits.
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>SERIAL_PASS</code> - Return the number of data bits.</dd>
     * <dd><code>SERIAL_FAIL</code> - Throw
     * <code>IOException</code>.</dd></dl>
     */
    public native int getDataBits(int portHandle) throws IOException;

    /**
     * Returns the currently configured flow control mode.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @return The currently configured flow control mode.
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>SERIAL_PASS</code> - Return the flow control mode.</dd>
     * <dd><code>SERIAL_FAIL</code> - Throw
     * <code>IOException</code>.</dd></dl>
     */
    public native int getFlowControlMode(int portHandle) throws IOException;

    /**
     * Returns the input buffer size in bytes.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @return The input buffer size in bytes.
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>SERIAL_PASS, PARALLEL_PASS</code> - Return the input buffer
     * size in bytes.</dd>
     * <dd><code>SERIAL_FAIL, PARALLEL_FAIL</code> - Throw
     * <code>IOException</code>.</dd></dl>
     */
    public native int getInputBufferSize(int portHandle) throws IOException;

    /**
     * Returns the currently configured mode.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @return The currently configured mode.
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>PARALLEL_PASS</code> - Return the current mode.</dd>
     * <dd><code>PARALLEL_FAIL</code> - Throw
     * <code>IOException</code>.</dd></dl>
     */
    public native int getMode(int portHandle) throws IOException;

    /**
     * Returns the number of bytes available in the output buffer.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @return The number of bytes available in the output buffer.
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>PARALLEL_PASS</code> - Return the number of bytes available
     * in the output buffer.</dd>
     * <dd><code>PARALLEL_FAIL</code> - Throw
     * <code>IOException</code>.</dd></dl>
     */
    public native int getOutputBufferFree(int portHandle) throws IOException;

    /**
     * Returns the output buffer size in bytes.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @return The output buffer size in bytes.
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>SERIAL_PASS, PARALLEL_PASS</code> - Return the output buffer
     * size in bytes.</dd>
     * <dd><code>SERIAL_FAIL, PARALLEL_FAIL</code> - Throw
     * <code>IOException</code>.</dd></dl>
     */
    public native int getOutputBufferSize(int portHandle) throws IOException;

    /**
     * Returns the currently configured parity setting.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @return The currently configured parity setting.
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>SERIAL_PASS</code> - Return the current parity setting.</dd>
     * <dd><code>SERIAL_FAIL</code> - Throw
     * <code>IOException</code>.</dd></dl>
     */
    public native int getParity(int portHandle) throws IOException;

    /**
     * Returns the port type of <code>portName</code>. If <code>portName</code> is not a
     * valid port, the method throws <code>NoSuchPortException</code>.
     * <p>
     * Java code must synchronize on <code>Dispatcher.class</code> when calling this
     * method.
     * </p>
     * @param portName A port name.
     * @return The type of the port:
     * {@link gnu.io.CommPortIdentifier#PORT_PARALLEL} or
     * {@link gnu.io.CommPortIdentifier#PORT_SERIAL}.
     * @throws NoSuchPortException If <code>portName</code> is not a valid port.
     */
    public native int getPortType(String portName) throws NoSuchPortException;

    /**
     * Returns the currently defined stop bits.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @return The currently defined stop bits.
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>SERIAL_PASS</code> - Return the current stop bits.</dd>
     * <dd><code>SERIAL_FAIL</code> - Throw
     * <code>IOException</code>.</dd></dl>
     */
    public native int getStopBits(int portHandle) throws IOException;

    /**
     * Returns a <code>List</code> of <code>PortInfo</code> elements.
     * If the environment does not include any valid ports, then an
     * empty <code>List</code> is returned.
     * <p>
     * Java code must synchronize on <code>Dispatcher.class</code> when calling this
     * method.
     * </p>
     * @return A <code>List</code> of <code>PortInfo</code> elements.
     */
    public native List getValidPortInfos(); // <PortInfo>

    /**
     * Returns the state of the break interrupt, if supported by the
     * underlying implementation.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @return The state of the break interrupt.
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>SERIAL_PASS</code> - Return the state of the break interrupt.</dd>
     * <dd><code>SERIAL_FAIL</code> - Throw
     * <code>IOException</code>.</dd></dl>
     */
    public native boolean isBI(int portHandle) throws IOException;

    /**
     * Returns the state of the CD (Carrier Detect) bit in the UART, if supported by the
     * underlying implementation.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @return The state of the CD (Carrier Detect) bit.
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>SERIAL_PASS</code> - Return the state of the CD bit.</dd>
     * <dd><code>SERIAL_FAIL</code> - Throw
     * <code>IOException</code>.</dd></dl>
     */
    public native boolean isCD(int portHandle) throws IOException;

    /**
     * Returns the state of the CTS (Clear To Send) bit in the UART, if supported by the
     * underlying implementation.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @return The state of the CTS (Clear To Send) bit.
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>SERIAL_PASS</code> - Return the state of the CTS bit.</dd>
     * <dd><code>SERIAL_FAIL</code> - Throw
     * <code>IOException</code>.</dd></dl>
     */
    public native boolean isCTS(int portHandle) throws IOException;

    /**
     * Returns <code>true</code> if new data arrived at the serial port.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @return <code>true</code> if new data arrived at the serial port.
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>SERIAL_PASS</code> - Return <code>true</code> if new data
     * arrived at the serial port.</dd>
     * <dd><code>SERIAL_FAIL</code> - Throw
     * <code>IOException</code>.</dd></dl>
     */
    public native boolean isDataAvailable(int portHandle) throws IOException;

    /**
     * Returns the state of the DSR (Data Set Ready) bit in the UART, if supported by the
     * underlying implementation.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @return The state of the DSR (Data Set Ready) bit.
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>SERIAL_PASS</code> - Return the state of the DSR bit.</dd>
     * <dd><code>SERIAL_FAIL</code> - Throw
     * <code>IOException</code>.</dd></dl>
     */
    public native boolean isDSR(int portHandle) throws IOException;

    /**
     * Returns the state of the DTR (Data Terminal Ready) bit in the UART, if supported by the
     * underlying implementation.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @return The state of the DTR (Data Terminal Ready) bit.
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>SERIAL_PASS</code> - Return the state of the DTR bit.</dd>
     * <dd><code>SERIAL_FAIL</code> - Throw
     * <code>IOException</code>.</dd></dl>
     */
    public native boolean isDTR(int portHandle) throws IOException;

    /**
     * Returns <code>true</code> if there was a framing error.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @return <code>true</code> if there was a framing error.
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>SERIAL_PASS</code> - Return <code>false</code>.</dd>
     * <dd><code>SERIAL_FAIL</code> - Return <code>true</code>.</dd></dl>
     */
    public native boolean isFramingError(int portHandle) throws IOException;

    /**
     * Returns <code>true</code> if the output buffer is empty.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @return <code>true</code> if the output buffer is empty.
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>SERIAL_PASS</code> - Return <code>true</code> if output buffer
     * is empty.</dd>
     * <dd><code>SERIAL_FAIL</code> - Throw
     * <code>IOException</code>.</dd></dl>
     */
    public native boolean isOutputBufferEmpty(int portHandle) throws IOException;

    /**
     * Returns <code>true</code> if there was an overrun error.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @return <code>true</code> if there was an overrun error.
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>SERIAL_PASS</code> - Return <code>false</code>.</dd>
     * <dd><code>SERIAL_FAIL</code> - Return <code>true</code>.</dd></dl>
     */
    public native boolean isOverrunError(int portHandle) throws IOException;

    /**
     * Returns <code>true</code> if the port is indicating an "Out of Paper" state.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @return <code>true</code> if the port is indicating an "Out of Paper" state.
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>PARALLEL_PASS</code> - Return <code>false</code>.</dd>
     * <dd><code>PARALLEL_FAIL</code> - Return <code>true</code>.</dd></dl>
     */
    public native boolean isPaperOut(int portHandle) throws IOException;

    /**
     * Returns <code>true</code> if there was a parity error.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @return <code>true</code> if there was a parity error.
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>SERIAL_PASS</code> - Return <code>false</code>.</dd>
     * <dd><code>SERIAL_FAIL</code> - Return <code>true</code>.</dd></dl>
     */
    public native boolean isParityError(int portHandle) throws IOException;

    /**
     * Returns <code>true</code> if the port is readable.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @return <code>true</code> if the port is readable.
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>SERIAL_PASS, PARALLEL_PASS</code> - Return <code>true</code>.</dd>
     * <dd><code>SERIAL_FAIL, PARALLEL_FAIL</code> - Return <code>false</code>.</dd>
     * </dl>
     */
    public native boolean isPortReadable(int portHandle) throws IOException;

    /**
     * Returns <code>true</code> if the port is writable.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @return <code>true</code> if the port is writable.
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>SERIAL_PASS, PARALLEL_PASS</code> - Return <code>true</code>.</dd>
     * <dd><code>SERIAL_FAIL, PARALLEL_FAIL</code> - Return <code>false</code>.</dd>
     * </dl>
     */
    public native boolean isPortWritable(int portHandle) throws IOException;

    /**
     * Returns <code>true</code> if the port is indicating a "Printer Busy" state.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @return <code>true</code> if the port is indicating a "Printer Busy" state.
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>PARALLEL_PASS</code> - Return <code>false</code>.</dd>
     * <dd><code>PARALLEL_FAIL</code> - Return <code>true</code>.</dd></dl>
     */
    public native boolean isPrinterBusy(int portHandle) throws IOException;

    /**
     * Returns <code>true</code> if the printer has encountered an error.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @return <code>true</code> if the printer has encountered an error.
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>PARALLEL_PASS</code> - Return <code>false</code>.</dd>
     * <dd><code>PARALLEL_FAIL</code> - Return <code>true</code>.</dd></dl>
     */
    public native boolean isPrinterError(int portHandle) throws IOException;

    /**
     * Returns <code>true</code> if the printer is in selected state.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @return <code>true</code> if the printer is in selected state.
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>PARALLEL_PASS</code> - Return <code>true</code>.</dd>
     * <dd><code>PARALLEL_FAIL</code> - Return <code>false</code>.</dd></dl>
     */
    public native boolean isPrinterSelected(int portHandle) throws IOException;

    /**
     * Returns <code>true</code> if the printer has timed out.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @return <code>true</code> if the printer has timed out.
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>PARALLEL_PASS</code> - Return <code>false</code>.</dd>
     * <dd><code>PARALLEL_FAIL</code> - Return <code>true</code>.</dd></dl>
     */
    public native boolean isPrinterTimedOut(int portHandle) throws IOException;

    /**
     * Returns the state of the RI (Ring Indicator) bit in the UART, if supported by the
     * underlying implementation.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @return The state of the RI (Ring Indicator) bit.
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>SERIAL_PASS</code> - Return the state of the RI bit.</dd>
     * <dd><code>SERIAL_FAIL</code> - Throw
     * <code>IOException</code>.</dd></dl>
     */
    public native boolean isRI(int portHandle) throws IOException;

    /**
     * Returns the state of the RTS (Request To Send) bit in the UART, if supported by the
     * underlying implementation.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @return The state of the RTS (Request To Send) bit.
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>SERIAL_PASS</code> - Return the state of the RTS bit.</dd>
     * <dd><code>SERIAL_FAIL</code> - Throw
     * <code>IOException</code>.</dd></dl>
     */
    public native boolean isRTS(int portHandle) throws IOException;

    /**
     * Opens a port. Native code opens the port, and returns an <code>int</code>
     * - which is a port handle or token. If the port is already open
     * when this method is called, then native code will close and
     * reopen it.
     * <p>
     * Java code must synchronize on <code>Dispatcher.class</code> when calling this
     * method.
     * </p>
     * @param portName The name of the port.
     * @param portType The port type - {@link gnu.io.CommPortIdentifier#PORT_PARALLEL}
     * or {@link gnu.io.CommPortIdentifier#PORT_SERIAL}.
     * @return A port handle or token. Java code will use this handle in native
     * method calls to indicate which port is being referenced in the call.
     * @throws IOException If there was an error while opening the port.
     * @throws IllegalArgumentException If <code>portName</code> is not a
     * valid port name, or <code>portType</code> is not a valid port type,
     * or <code>timeout</code> is less than zero.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>SERIAL_PASS, PARALLEL_PASS</code> - Return a handle or token.</dd>
     * <dd><code>SERIAL_FAIL, PARALLEL_FAIL</code> - Throw
     * <code>IOException</code>.</dd></dl>
     */
    public native int open(String portName, int portType) throws IOException;

    /**
     * Reads data into the <code>buffer</code> byte array if any
     * bytes are available and returns the number of bytes read.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param buffer
     * @param offset
     * @param length
     * @return The number of bytes read.
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>SERIAL_PASS, PARALLEL_PASS</code> - Return the number of bytes
     * read.</dd>
     * <dd><code>SERIAL_FAIL, PARALLEL_FAIL</code> - Throw
     * <code>IOException</code>.</dd></dl>
     */
    public native int readBytes(int portHandle, byte[] buffer, int offset, int length) throws IOException;

    /**
     * Restarts output after an error.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>PARALLEL_PASS</code> - Do nothing.</dd>
     * <dd><code>PARALLEL_FAIL</code> - Throw
     * <code>IOException</code>.</dd></dl>
     */
    public native void restart(int portHandle) throws IOException;

    /**
     * Sends a break of <CODE>duration</CODE> milliseconds duration.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @param duration The break duration in milliseconds.
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>SERIAL_PASS</code> - Simulate a break of
     * <CODE>duration</CODE> milliseconds.</dd>
     * <dd><code>SERIAL_FAIL</code> - Throw
     * <code>IOException</code>.</dd></dl>
     */
    public native void sendBreak(int portHandle, int duration) throws IOException;

    /**
     * Sets or clears the DTR (Data Terminal Ready) bit in the UART, if supported by the
     * underlying implementation.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @param state
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>SERIAL_PASS</code> - Set DTR to <CODE>state</CODE>.</dd>
     * <dd><code>SERIAL_FAIL</code> - Throw
     * <code>IOException</code>.</dd></dl>
     */
    public native void setDTR(int portHandle, boolean state) throws IOException;

    /**
     * Sets the flow control mode.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @param flowcontrol
     * @throws UnsupportedCommOperationException
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>SERIAL_PASS</code> - Set flow control to <CODE>flowcontrol</CODE>.</dd>
     * <dd><code>SERIAL_FAIL</code> - Throw
     * <code>IOException</code>.</dd></dl>
     */
    public native void setFlowControlMode(int portHandle, int flowcontrol) throws IOException, UnsupportedCommOperationException;

    /**
     * Sets the input buffer size.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @param size
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>SERIAL_PASS, PARALLEL_PASS</code> - Set input buffer size
     * to <CODE>size</CODE>.</dd>
     * <dd><code>SERIAL_FAIL, PARALLEL_FAIL</code> - Throw
     * <code>IOException</code>.</dd></dl>
     */
    public native void setInputBufferSize(int portHandle, int size) throws IOException;

    /**
     * Sets the parallel port mode.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @param mode
     * @throws UnsupportedCommOperationException
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>PARALLEL_PASS</code> - Set mode to <CODE>mode</CODE>.</dd>
     * <dd><code>PARALLEL_FAIL</code> - Throw
     * <code>IOException</code>.</dd></dl>
     */
    public native int setMode(int portHandle, int mode) throws IOException, UnsupportedCommOperationException;

    /**
     * Sets the output buffer size.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @param size
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>SERIAL_PASS, PARALLEL_PASS</code> - Set output buffer size
     * to <CODE>size</CODE>.</dd>
     * <dd><code>SERIAL_FAIL, PARALLEL_FAIL</code> - Throw
     * <code>IOException</code>.</dd></dl>
     */
    public native void setOutputBufferSize(int portHandle, int size) throws IOException;

    /**
     * Sets or clears the RTS (Request To Send) bit in the UART, if supported by the
     * underlying implementation.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @param state
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>SERIAL_PASS</code> - Set RTS to <CODE>state</CODE>.</dd>
     * <dd><code>SERIAL_FAIL</code> - Throw
     * <code>IOException</code>.</dd></dl>
     */
    public native void setRTS(int portHandle, boolean state) throws IOException;

    /**
     * Sets the serial port parameters.
     * <p>Default: 9600 baud, 8 data bits, 1 stop bit, no parity.</p>
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param baudRate
     * @param dataBits
     * @param stopBits
     * @param parity
     * @throws UnsupportedCommOperationException
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>SERIAL_PASS</code> - Set the serial port parameters.</dd>
     * <dd><code>SERIAL_FAIL</code> - Throw
     * <code>IOException</code>.</dd></dl>
     */
    public native void setSerialPortParams(int portHandle, int baudRate, int dataBits, int stopBits, int parity) throws IOException, UnsupportedCommOperationException;

    /**
     * Suspends output.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param portHandle The port handle or token.
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>PARALLEL_PASS</code> - Do nothing.</dd>
     * <dd><code>PARALLEL_FAIL</code> - Throw
     * <code>IOException</code>.</dd></dl>
     */
    public native void suspend(int portHandle) throws IOException;

    /**
     * Returns the native library version.
     * @return The native library version.
     */
    public native String version();

    /**
     * Writes data from the <code>buffer</code> byte array.
     * Returns the number of bytes written.
     * <p>
     * Java code must synchronize on the <code>CommPort</code> instance
     * when calling this method.
     * </p>
     * @param buffer
     * @param offset
     * @param length
     * @return The number of bytes written.
     * @throws IllegalArgumentException If <code>portHandle</code>
     * is not a valid handle or token.
     * @throws IllegalStateException If the port is closed.
     * @throws IOException If communication with the port device has been lost.
     * 
     * </dl><dl><dt><b>Unit tests:</b></dt>
     * <dd><code>SERIAL_PASS, PARALLEL_PASS</code> - Return the number of bytes
     * written.</dd>
     * <dd><code>SERIAL_FAIL, PARALLEL_FAIL</code> - Throw
     * <code>IOException</code>.</dd></dl>
     */
    public native int writeBytes(int portHandle, byte[] buffer, int offset, int length) throws IOException;
}
