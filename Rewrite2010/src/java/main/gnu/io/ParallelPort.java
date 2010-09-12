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
 * A parallel communications port. <CODE>ParallelPort</CODE> describes the low-level
 * interface to a parallel communications port made available by the underlying system.
 * <CODE>ParallelPort</CODE> defines the minimum required functionality for parallel
 * communications ports.
 * 
 * @author <a href="http://www.rxtx.org">The RXTX Project</a>
 */
public abstract class ParallelPort extends CommPort {
    /**
     * Picks the best available mode.
     */
    public static final int LPT_MODE_ANY = 0;
    /**
     * Compatibility mode. Unidirectional.
     */
    public static final int LPT_MODE_SPP = 1;
    /**
     * Byte mode. Bi-directional.
     */
    public static final int LPT_MODE_PS2 = 2;
    /**
     * Extended parallel port.
     */
    public static final int LPT_MODE_EPP = 3;
    /**
     * Enhanced capabilities port.
     */
    public static final int LPT_MODE_ECP = 4;
    /**
     * Nibble Mode. Bi-directional. 4 bits at a time.
     */
    public static final int LPT_MODE_NIBBLE = 5;

    protected ParallelPort(CommPortIdentifier cpi, int portHandle) {
        super(cpi, portHandle);
    }

    /**
     * Registers a <CODE>ParallelPortEventListener</CODE> object for
     * <CODE>ParallelPortEvent</CODE>s. Interest in specific events may be expressed by
     * using the <CODE>notifyOnError</CODE> and <CODE>notifyOnBuffer</CODE> methods.
     * <P>
     * Only one listener per <CODE>ParallelPort</CODE> is supported. Calling
     * <CODE>addEventListener</CODE> multiple times will simply replace the current
     * <CODE>ParallelPortEventListener</CODE> object.
     * <P>
     * After the port is closed, no more events will be generated. Another call to
     * <CODE>open()</CODE> of the port's <CODE>CommPortIdentifier</CODE> object will
     * return a new <CODE>CommPort</CODE> object, and the listener has to be added again to
     * the new <CODE>CommPort</CODE> object to receive events from this port.
     * 
     * @param listener
     * @throws TooManyListenersException
     */
    public abstract void addEventListener(ParallelPortEventListener listener) throws TooManyListenersException;

    /**
     * Returns the currently configured mode.
     * @return The currently configured mode.
     */
    public abstract int getMode();

    /**
     * Returns the number of bytes available in the output buffer.
     * @return The number of bytes available in the output buffer.
     */
    public abstract int getOutputBufferFree();

    /**
     * Returns <code>true</code> if the port is indicating an "Out of Paper" state.
     * @return <code>true</code> if the port is indicating an "Out of Paper" state.
     */
    public abstract boolean isPaperOut();

    /**
     * Returns <code>true</code> if the port is indicating a "Printer Busy" state.
     * @return <code>true</code> if the port is indicating a "Printer Busy" state.
     */
    public abstract boolean isPrinterBusy();

    /**
     * Returns <code>true</code> if the printer has encountered an error.
     * <P>
     * Note: This method is platform dependent.
     * 
     * @return <code>true</code> if the printer has encountered an error.
     */
    public abstract boolean isPrinterError();

    /**
     * Returns <code>true</code> if the printer is in selected state.
     * <P>
     * Note: This method is platform dependent.
     * 
     * @return <code>true</code> if the printer is in selected state.
     */
    public abstract boolean isPrinterSelected();

    /**
     * Returns <code>true</code> if the printer has timed out.
     * <P>
     * Note: This method is platform dependent.
     * 
     * @return <code>true</code> if the printer has timed out.
     */
    public abstract boolean isPrinterTimedOut();

    /**
     * Expresses interest in being notified when the output buffer is empty.
     * @param enable
     */
    public abstract void notifyOnBuffer(boolean enable);

    /**
     * Expresses interest in being notified of port errors.
     * @param enable
     */
    public abstract void notifyOnError(boolean enable);

    /**
     * Deregisters event listener registered using <CODE>addEventListener</CODE>. This is
     * done automatically when a port is closed.
     */
    public abstract void removeEventListener();

    /**
     * Restarts output after an error.
     */
    public abstract void restart();

    /**
     * Sets the parallel port mode.
     * @param mode
     * @throws UnsupportedCommOperationException
     */
    public abstract int setMode(int mode) throws UnsupportedCommOperationException;

    /**
     * Suspends output.
     */
    public abstract void suspend();
}
