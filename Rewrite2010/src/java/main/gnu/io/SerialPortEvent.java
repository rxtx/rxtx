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

import java.util.EventObject;

/**
 * A serial port event.
 * 
 * @author <a href="http://www.rxtx.org">The RXTX Project</a>
 */
public class SerialPortEvent extends EventObject {
    /**
     * Data available at the serial port. This event will be generated once when new data
     * arrive at the serial port. Even if the user doesn't read the data, it won't be
     * generated again until next time new data arrive.
     */
    public static final int DATA_AVAILABLE = 1;
    /**
     * Output buffer is empty. The event will be generated after a write is completed,
     * when the system buffer becomes empty again.
     */
    public static final int OUTPUT_BUFFER_EMPTY = 2;
    /**
     * Clear to send.
     */
    public static final int CTS = 3;
    /**
     * Data set ready.
     */
    public static final int DSR = 4;
    /**
     * Ring indicator.
     */
    public static final int RI = 5;
    /**
     * Carrier detect.
     */
    public static final int CD = 6;
    /**
     * Overrun error.
     */
    public static final int OE = 7;
    /**
     * Parity error.
     */
    public static final int PE = 8;
    /**
     * Framing error.
     */
    public static final int FE = 9;
    /**
     * Break interrupt.
     */
    public static final int BI = 10;

    private final int eventType;
    private final boolean newValue;
    private final boolean oldValue;

    /**
     * Constructs a <CODE>SerialPortEvent</CODE> with the specified serial port, event
     * type, old and new values. Application programs should not directly create
     * <CODE>SerialPortEvent</CODE> objects.
     * 
     * @param source
     * @param eventType
     * @param oldValue
     * @param newValue
     */
    public SerialPortEvent(SerialPort source, int eventType, boolean oldValue, boolean newValue) {
        super(source);
        this.eventType = eventType;
        this.newValue = newValue;
        this.oldValue = oldValue;
    }

    /**
     * Returns the type of this event.
     * @return The type of this event.
     */
    public int getEventType() {
        return this.eventType;
    }

    /**
     * Returns the new value of the state change that caused the
     * <CODE>SerialPortEvent</CODE> to be propagated.
     * 
     * @return The new value of the state change.
     */
    public boolean getNewValue() {
        return this.newValue;
    }

    /**
     * Returns the old value of the state change that caused the
     * <CODE>SerialPortEvent</CODE> to be propagated.
     * 
     * @return The old value of the state change.
     */
    public boolean getOldValue() {
        return this.oldValue;
    }
}
