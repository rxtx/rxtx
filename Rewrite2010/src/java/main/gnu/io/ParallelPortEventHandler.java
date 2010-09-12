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
 * A parallel port event handler object. The <code>ParallelPortEventHandler</code>
 * class encapsulates the port event handling logic and a port monitoring thread.
 * 
 * <dl>
 * <dt><b>Thread-safe:</b></dt>
 * <dd>This class may be used in multi-threaded applications.</dd>
 * </dl>
 * @author <a href="http://www.rxtx.org">The RXTX Project</a>
 */
public class ParallelPortEventHandler {
    private static final String source = ParallelPortEventHandler.class.getName();
    static public final Integer ERROR = new Integer(ParallelPortEvent.PAR_EV_ERROR);
    static public final Integer BUFFER = new Integer(ParallelPortEvent.PAR_EV_BUFFER);

    // Guarded by this
    private final Map eventHandlers = new HashMap(); // <Integer, EventHandler>
    private Thread eventMonitor = null;
    private ParallelPortEventListener listener = null;
    private final ParallelPortImpl port;

    protected ParallelPortEventHandler(ParallelPortImpl port) {
        this.port = port;
    }

    public synchronized void addEventListener(ParallelPortEventListener listener) throws TooManyListenersException {
        if (this.listener != null) {
            throw new TooManyListenersException();
        }
        this.listener = listener;
        this.eventMonitor = new EventMonitor();
        this.eventMonitor.start();
    }

    public synchronized void notifyOnBuffer(boolean enable) {
        if (enable) {
            if (!this.eventHandlers.containsKey(BUFFER)) {
                EventHandler handler = new EventHandler(BUFFER.intValue()) {
                    protected boolean getCurrentValue() {
                        return ParallelPortEventHandler.this.port.isOutputBufferEmpty();
                    }
                };
                this.eventHandlers.put(BUFFER, handler);
            }
        } else {
            this.eventHandlers.remove(BUFFER);
        }
    }

    public synchronized void notifyOnError(boolean enable) {
        if (enable) {
            if (!this.eventHandlers.containsKey(ERROR)) {
                EventHandler handler = new EventHandler(ERROR.intValue()) {
                    protected boolean getCurrentValue() {
                        return ParallelPortEventHandler.this.port.isPrinterError();
                    }
                };
                this.eventHandlers.put(ERROR, handler);
            }
        } else {
            this.eventHandlers.remove(ERROR);
        }
    }

    private synchronized void processEvents() {
        Iterator i = this.eventHandlers.values().iterator();
        while (i.hasNext() && !Thread.interrupted()) {
            EventHandler handler = (EventHandler) i.next();
            ParallelPortEvent event = handler.getEvent();
            if (event != null) {
                this.listener.parallelEvent(event);
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

        protected ParallelPortEvent getEvent() {
            boolean currentValue = getCurrentValue();
            if (currentValue != this.oldValue) {
                ParallelPortEvent event = new ParallelPortEvent(ParallelPortEventHandler.this.port, this.eventType, this.oldValue, currentValue);
                this.oldValue = currentValue;
                return event;
            }
            return null;
        }
    }

    private class EventMonitor extends Thread {

        private EventMonitor() {
            super("Parallel Port Event Monitor");
        }

        public void run() {
            while (!Thread.interrupted()) {
                try {
                    ParallelPortEventHandler.this.processEvents();
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
