/*-------------------------------------------------------------------------
|   RXTX License v 2.1 - LGPL v 2.1 + Linking Over Controlled Interface.
|   RXTX is a native interface to serial ports in java.
|   Copyright 1997-2008 by Trent Jarvi tjarvi@qbang.org and others who
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TooManyListenersException;

/**
 * A serial port event handler object. The <code>SerialPortEventHandler</code>
 * class encapsulates the port event handling logic and a port monitoring thread.
 * 
 * <dl>
 * <dt><b>Thread-safe:</b></dt>
 * <dd>This class may be used in multi-threaded applications.</dd>
 * </dl>
 * 
 * @author <a href="http://www.rxtx.org">The RXTX Project</a>
 */
public class SerialPortEventHandler {
    private static final String source = SerialPortEventHandler.class.getName();
    private static final Integer BI = new Integer(SerialPortEvent.BI);
    private static final Integer CD = new Integer(SerialPortEvent.CD);
    private static final Integer CTS = new Integer(SerialPortEvent.CTS);
    private static final Integer DA = new Integer(SerialPortEvent.DATA_AVAILABLE);
    private static final Integer DSR = new Integer(SerialPortEvent.DSR);
    private static final Integer FE = new Integer(SerialPortEvent.FE);
    private static final Integer OBE = new Integer(SerialPortEvent.OUTPUT_BUFFER_EMPTY);
    private static final Integer ORE = new Integer(SerialPortEvent.OE);
    private static final Integer PE = new Integer(SerialPortEvent.PE);
    private static final Integer RI = new Integer(SerialPortEvent.RI);

    // Guarded by this
    private final Map eventHandlers = new HashMap(); // <Integer, EventHandler>
    private Thread eventMonitor = null;
    private SerialPortEventListener listener = null;
    private final SerialPortImpl port;

    protected SerialPortEventHandler(SerialPortImpl port) {
        this.port = port;
    }

    public synchronized void addEventListener(SerialPortEventListener listener) throws TooManyListenersException {
        if (this.listener != null) {
            throw new TooManyListenersException();
        }
        this.listener = listener;
        this.eventMonitor = new EventMonitor();
        this.eventMonitor.start();
    }

    public synchronized void notifyOnBreakInterrupt(boolean enable) {
        if (enable) {
            if (!this.eventHandlers.containsKey(BI)) {
                EventHandler handler = new EventHandler(BI.intValue()) {
                    protected boolean getCurrentValue() {
                        return SerialPortEventHandler.this.port.isBI();
                    }
                };
                this.eventHandlers.put(BI, handler);
            }
        } else {
            this.eventHandlers.remove(BI);
        }
    }

    public synchronized void notifyOnCarrierDetect(boolean enable) {
        if (enable) {
            if (!this.eventHandlers.containsKey(CD)) {
                EventHandler handler = new EventHandler(CD.intValue()) {
                    protected boolean getCurrentValue() {
                        return SerialPortEventHandler.this.port.isCD();
                    }
                };
                this.eventHandlers.put(CD, handler);
            }
        } else {
            this.eventHandlers.remove(CD);
        }
    }

    public synchronized void notifyOnCTS(boolean enable) {
        if (enable) {
            if (!this.eventHandlers.containsKey(CTS)) {
                EventHandler handler = new EventHandler(CTS.intValue()) {
                    protected boolean getCurrentValue() {
                        return SerialPortEventHandler.this.port.isCTS();
                    }
                };
                this.eventHandlers.put(CTS, handler);
            }
        } else {
            this.eventHandlers.remove(CTS);
        }
    }

    public synchronized void notifyOnDataAvailable(boolean enable) {
        if (enable) {
            if (!this.eventHandlers.containsKey(DA)) {
                EventHandler handler = new EventHandler(DA.intValue()) {
                    protected boolean getCurrentValue() {
                        return SerialPortEventHandler.this.port.isDataAvailable();
                    }
                };
                this.eventHandlers.put(DA, handler);
            }
        } else {
            this.eventHandlers.remove(DA);
        }
    }

    public synchronized void notifyOnDSR(boolean enable) {
        if (enable) {
            if (!this.eventHandlers.containsKey(DSR)) {
                EventHandler handler = new EventHandler(DSR.intValue()) {
                    protected boolean getCurrentValue() {
                        return SerialPortEventHandler.this.port.isDSR();
                    }
                };
                this.eventHandlers.put(DSR, handler);
            }
        } else {
            this.eventHandlers.remove(DSR);
        }
    }

    public synchronized void notifyOnFramingError(boolean enable) {
        if (enable) {
            if (!this.eventHandlers.containsKey(FE)) {
                EventHandler handler = new EventHandler(FE.intValue()) {
                    protected boolean getCurrentValue() {
                        return SerialPortEventHandler.this.port.isFramingError();
                    }
                };
                this.eventHandlers.put(FE, handler);
            }
        } else {
            this.eventHandlers.remove(FE);
        }
    }

    public synchronized void notifyOnOutputEmpty(boolean enable) {
        if (enable) {
            if (!this.eventHandlers.containsKey(OBE)) {
                EventHandler handler = new EventHandler(OBE.intValue()) {
                    protected boolean getCurrentValue() {
                        return SerialPortEventHandler.this.port.isOutputBufferEmpty();
                    }
                };
                this.eventHandlers.put(OBE, handler);
            }
        } else {
            this.eventHandlers.remove(OBE);
        }
    }

    public synchronized void notifyOnOverrunError(boolean enable) {
        if (enable) {
            if (!this.eventHandlers.containsKey(ORE)) {
                EventHandler handler = new EventHandler(ORE.intValue()) {
                    protected boolean getCurrentValue() {
                        return SerialPortEventHandler.this.port.isOverrunError();
                    }
                };
                this.eventHandlers.put(ORE, handler);
            }
        } else {
            this.eventHandlers.remove(ORE);
        }
    }

    public synchronized void notifyOnParityError(boolean enable) {
        if (enable) {
            if (!this.eventHandlers.containsKey(PE)) {
                EventHandler handler = new EventHandler(PE.intValue()) {
                    protected boolean getCurrentValue() {
                        return SerialPortEventHandler.this.port.isParityError();
                    }
                };
                this.eventHandlers.put(PE, handler);
            }
        } else {
            this.eventHandlers.remove(PE);
        }
    }

    public synchronized void notifyOnRingIndicator(boolean enable) {
        if (enable) {
            if (!this.eventHandlers.containsKey(RI)) {
                EventHandler handler = new EventHandler(RI.intValue()) {
                    protected boolean getCurrentValue() {
                        return SerialPortEventHandler.this.port.isRI();
                    }
                };
                this.eventHandlers.put(RI, handler);
            }
        } else {
            this.eventHandlers.remove(RI);
        }
    }

    private synchronized void processEvents() {
        Iterator i = this.eventHandlers.values().iterator();
        while (i.hasNext() && !Thread.interrupted()) {
            EventHandler handler = (EventHandler) i.next();
            SerialPortEvent event = handler.getEvent();
            if (event != null) {
                this.listener.serialEvent(event);
            }
        }
    }

    public synchronized void removeEventListener() {
        if (this.eventMonitor != null) {
            this.eventMonitor.interrupt();
            this.eventMonitor = null;
        }
        if (this.listener != null) {
            this.listener = null;
        }
        this.eventHandlers.clear();
    }

    private abstract class EventHandler {

        public final int eventType;
        private boolean oldValue;

        protected EventHandler(int eventType) {
            this.eventType = eventType;
            this.oldValue = getCurrentValue();
        }

        protected abstract boolean getCurrentValue();

        protected SerialPortEvent getEvent() {
            boolean currentValue = getCurrentValue();
            if (currentValue != this.oldValue) {
                SerialPortEvent event = new SerialPortEvent(SerialPortEventHandler.this.port, this.eventType, this.oldValue, currentValue);
                this.oldValue = currentValue;
                return event;
            }
            return null;
        }
    }

    private class EventMonitor extends Thread {

        private EventMonitor() {
            super("Serial Port Event Monitor");
        }

        public void run() {
            while (!Thread.interrupted()) {
                try {
                    SerialPortEventHandler.this.processEvents();
                } catch (Throwable t) {
                    Log.log(t, "Uncaught exception thrown while processing events: ", source);
                }
                try {
                    Thread.sleep(50);  // Sufficient up to 19200 baud
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }
}
