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

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

/**
 * Communications port management. <code>CommPortIdentifier</code> is the central class
 * for controlling access to communications ports. It includes methods for:
 * <UL>
 * <LI>Determining the communications ports made available by the driver.</li>
 * <LI>Opening communications ports for I/O operations.</li>
 * <LI>Determining port ownership.</li>
 * <LI>Resolving port ownership contention.</li>
 * <LI>Managing events that indicate changes in port ownership status.</li>
 * </UL>
 * <p>
 * An application uses the static method {@link #getPortIdentifiers()} to generate a list
 * of available ports. It then chooses a port from this list and calls
 * {@link #open(String, int)} to create a {@link gnu.io.CommPort} object. Finally, it
 * casts the <code>CommPort</code> object to a physical communications device class - like
 * {@link gnu.io.SerialPort} or {@link gnu.io.ParallelPort}.
 * </p>
 * 
 * <dl>
 * <dt><b>Thread-safe:</b></dt>
 * <dd>This class may be used in multi-threaded applications.</dd>
 * </dl>
 * 
 * @author <a href="http://www.rxtx.org">The RXTX Project</a>
 */
public class CommPortIdentifier {
    private static final String source = CommPortIdentifier.class.getName();
    private static final Map addedPorts = new HashMap(); // <String, CommPortIdentifier>
    private static Map currentPorts = createCurrentPorts(); // <String, CommPortIdentifier>
    private static final Dispatcher dispatch = Dispatcher.getInstance();
    public static final int PORT_SERIAL = 1;
    public static final int PORT_PARALLEL = 2;

    /**
     * Adds <CODE>portName</CODE> to the list of ports.
     * @param portName Name of the port
     * @param portType Port type
     * @param driver Reference to a <code>CommDriver</code> instance.
     */
    public static void addPortName(String portName, int portType, CommDriver driver) {
        synchronized (addedPorts) {
            if (!addedPorts.containsKey(portName)) {
                addedPorts.put(portName, new CommPortIdentifier(portName, portType, driver));
            }
        }
    }

    private static Map createCurrentPorts() {
        Map portMap = new HashMap(); // <String, CommPortIdentifier>
        Iterator i = PortInfo.getCurrentPortInfos().iterator();
        while (i.hasNext()) {
            PortInfo info = (PortInfo) i.next();
            portMap.put(info.portName, new CommPortIdentifier(info.portName, info.portType));
        }
        return portMap;
    }

    protected static void updateCurrentPorts() {
        Map portMap = new HashMap(); // <String, CommPortIdentifier>
        Iterator i = PortInfo.getCurrentPortInfos().iterator();
        while (i.hasNext()) {
            PortInfo info = (PortInfo) i.next();
            if (currentPorts.containsKey(info.portName)) {
                portMap.put(info.portName, currentPorts.get(info.portName));
            } else {
                portMap.put(info.portName, new CommPortIdentifier(info.portName, info.portType));
            }
        }
        currentPorts = portMap;
    }

    /**
     * Returns the <CODE>CommPortIdentifier</CODE> object corresponding to a port that has
     * already been opened by the application.
     * 
     * @param port
     * @return A <CODE>CommPortIdentifier</CODE> object.
     * @throws NoSuchPortException
     */
    public static CommPortIdentifier getPortIdentifier(CommPort port) throws NoSuchPortException {
        return getPortIdentifier(port.getName());
    }

    /**
     * Returns the <CODE>CommPortIdentifier</CODE> object related to a port name.
     * @param portName
     * @return A <CODE>CommPortIdentifier</CODE> object.
     * @throws NoSuchPortException
     */
    public static CommPortIdentifier getPortIdentifier(String portName) throws NoSuchPortException {
        CommPortIdentifier cpi = (CommPortIdentifier) currentPorts.get(portName);
        if (cpi == null) {
            synchronized (addedPorts) {
                cpi = (CommPortIdentifier) addedPorts.get(portName);
            }
            if (cpi == null) {
                throw new NoSuchPortException();
            }
        }
        return cpi;
    }

    /**
     * Returns an <code>Enumeration</code> object that enumerates a list of
     * <CODE>CommPortIdentifier</CODE> objects. The <code>Enumeration</code> object
     * is backed by a copy of the list, so it is safe to use in multi-threaded
     * applications.
     * 
     * @return An <code>Enumeration</code> object.
     */
    public static Enumeration getPortIdentifiers() {
        List values = new ArrayList(); // <CommPortIdentifier>
        values.addAll(currentPorts.values());
        synchronized (addedPorts) {
            values.addAll(addedPorts.values());
        }
        return new CommPortEnumerator(values.iterator());
    }

    // --------------------------------------------------------------------- //

    // Non-final fields guarded by this
    private String owner = null;
    private CommPort port = null;
    private final CommDriver driver;
    private final Set listeners = new HashSet();
    private final String portName;
    private final int portType;

    protected CommPortIdentifier(String portName, int portType) {
        this.portName = portName;
        this.portType = portType;
        this.driver = new DefaultDriver();;
    }

    protected CommPortIdentifier(String portName, int portType, CommDriver driver) {
        this.portName = portName;
        this.portType = portType;
        this.driver = driver;
    }

    /**
     * Registers an interested application so that it can receive notification of changes
     * in port ownership. This includes notification of the following events:
     * <UL>
     * <LI> <CODE>PORT_OWNED</CODE>: Port became owned.</li>
     * <LI> <CODE>PORT_UNOWNED</CODE>: Port became available.</li>
     * <LI> <CODE>PORT_OWNERSHIP_REQUESTED</CODE>:
     * If the application owns this port and is willing to give up ownership, then it
     * should call <CODE>close</CODE> now.</li>
     * </UL>
     * <P>
     * The <CODE>ownershipChange</CODE> method of the listener registered using
     * <CODE>addPortOwnershipListener</CODE> will be called with one of the above events.
     * <P>
     * 
     * @param listener
     */
    public synchronized void addPortOwnershipListener(CommPortOwnershipListener listener) {
        this.listeners.add(listener);
    }

    /**
     * Returns <code>true</code> if <code>appname</code>
     * has ownership of this port.
     * @param appname
     * @return <code>true</code> if <code>appname</code>
     * has ownership of this port.
     */
    private synchronized boolean assumeOwnership(String appname) {
        if (this.owner == null) {
            this.port = this.driver.getCommPort(this.portName, this.portType);
            this.owner = appname;
            notifyListeners(CommPortOwnershipListener.PORT_OWNED);
            return true;
        }
        return appname.equals(this.owner);
    }

    /**
     * Returns the current owner of the port, or <code>null</code> if
     * the port is not owned.
     * @return The current owner of the port, or <code>null</code> if
     * the port is not owned.
     */
    public synchronized String getCurrentOwner() {
        return this.owner;
    }

    /**
     * Returns the port name.
     * @return The port name.
     */
    public String getName() {
        return this.portName;
    }

    /**
     * Returns the port type.
     * @return The port type.
     */
    public int getPortType() {
        return this.portType;
    }

    /**
     * Returns <code>true</code> if the port is currently owned.
     * @return <code>true</code> if the port is currently owned.
     */
    public synchronized boolean isCurrentlyOwned() {
        return this.owner != null;
    }

    private synchronized void notifyListeners(int eventType) {
        Iterator i = this.listeners.iterator();
        while (i.hasNext()) {
            CommPortOwnershipListener listener = (CommPortOwnershipListener) i.next();
            listener.ownershipChange(eventType);
        }
    }

    /**
     * This method is not supported in RXTX. It throws
     * <code>UnsupportedCommOperationException</code>.
     * @throws UnsupportedCommOperationException
     */
    public CommPort open(FileDescriptor fd) throws UnsupportedCommOperationException {
        throw new UnsupportedCommOperationException();
    }

    /**
     * Opens the device port. Callers of this method obtain exclusive ownership of the
     * port. If the port is owned by some other application, a
     * <code>PORT_OWNERSHIP_REQUESTED</code> event is sent using the
     * <code>CommPortOwnershipListener</code> event mechanism. If the application that
     * owns the port calls <code>close</close> during the event processing, then this
     * open will succeed.
     * <p>
     * <hr>
     * <p>
     * The original <code>javax.comm</code> specification made no provision for ports that
     * have been disconnected - like virtual ports. The RXTX implementation will throw
     * <code>IllegalStateException</code> if the port has been disconnected.
     * </p>
     * 
     * @param appname
     *            The name of the application making this call. This name will become the
     *            owner of the port. Used for resolving ownership contention.
     * @param timeout
     *            The time in milliseconds to block while waiting for the port to open.
     * @return A <code>CommPort</code> instance
     * @throws PortInUseException
     * @throws IllegalStateException If the port has been disconnected
     */
    public CommPort open(String appname, int timeout) throws PortInUseException {
        boolean ownershipAssumed = assumeOwnership(appname);
        if (!ownershipAssumed)
        {
            notifyListeners(CommPortOwnershipListener.PORT_OWNERSHIP_REQUESTED);
            long now = System.currentTimeMillis();
            long waitTimeEnd = now + timeout;
            while (!ownershipAssumed && now < waitTimeEnd)
            {
                try
                {
                    Thread.sleep(waitTimeEnd - now);
                    ownershipAssumed = assumeOwnership(appname);
                } catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    break;
                }
                now = System.currentTimeMillis();
            }
        }
        if (!ownershipAssumed)
            throw new PortInUseException(getCurrentOwner());
        return this.port;
    }

    /**
     * Clears ownership information and sends the
     * {@link gnu.io.CommPortOwnershipListener#PORT_UNOWNED} notification
     * to all port ownership listeners.
     */
    protected synchronized void portClosed() {
        this.owner = null;
        this.port = null;
        /*
         * TODO: From CommPortOwnershipListener -
         * "When close is called from within a CommPortOwnership event callback,
         * a new CommPortOwnership event will not be generated."
         */
        notifyListeners(CommPortOwnershipListener.PORT_UNOWNED);
    }

    /**
     * Deregisters a <CODE>CommPortOwnershipListener</CODE> that was registered using
     * <CODE>addPortOwnershipListener</CODE>.
     * 
     * @param listener
     */
    public synchronized void removePortOwnershipListener(CommPortOwnershipListener listener) {
        this.listeners.remove(listener);
    }

    private class DefaultDriver implements CommDriver {
        /*
         * Developers: RXTX uses DefaultDriver as nothing more than
         * a CommPort factory.
         */
        public CommPort getCommPort(String portName, int portType) {
            if (portType != PORT_SERIAL && portType != PORT_PARALLEL) {
                throw new IllegalArgumentException("portType must be PORT_SERIAL or PORT_PARALLEL");
            }
            try {
                synchronized (Dispatcher.class) {
                    int portHandle = dispatch.open(portName, portType);
                    if (portType == PORT_SERIAL) {
                        return new SerialPortImpl(CommPortIdentifier.this, portHandle);
                    } else {
                        return new ParallelPortImpl(CommPortIdentifier.this, portHandle);
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("IOException thrown: " + e.getMessage());
            }
        }

        public void initialize() {
        }
    }
}
