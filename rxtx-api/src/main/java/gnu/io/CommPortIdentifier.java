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
package gnu.io;

import gnu.io.spi.CommDriver;
import java.io.FileDescriptor;
import java.util.HashMap;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * A
 * <code>CommPortIdentifier</code> represents a port on the system. In contrast
 * to
 * <code>CommPort</code> this class represents the port as an abstract device
 * and deals with the process of discovering, obtaining/managing ownership and
 * open the port. When a port is opened, a port type dependent
 * <code>CommPort</code> object is obtained and can be used to actually read and
 * write the port.
 *
 * @author Trent Jarvi
 */
public final class CommPortIdentifier extends Object {

    /**
     * The port is a RS232 serial port.
     */
    public static final int PORT_SERIAL = 1;
    /**
     * The name of the corresponding port. On linux and probably other systems
     * this will typically look like /dev/ttyx on Windows COMx where x is some
     * integer.
     */
    private String portName;
    /**
     * States whether the port is available to be assigned to a new owner. When
     * it is false, the port is currently owned.
     */
    private boolean available = true;
    /**
     * The name of the current owner (might be
     * <code>null</code>) and
     * <code>null</code> if the port is not currently owned.
     */
    private String owner;
    /**
     * When the port is open this will hold the port object.
     */
    private CommPort commPort;
    /**
     * The driver which is used to access the associated port.
     */
    private CommDriver driver;
    // TODO (by Alexander Graf) commPortIndex and next are part of reinventing
    // the wheel by implementing a linked list
    static CommPortIdentifier CommPortIndex;
    CommPortIdentifier next;
    /**
     * A PORT_* constant indicating the port hardware type.
     */
    private int portType;
    // TODO (by Alexander Graf) the access to this field from all corners of
    // rxtx is really ugly and should be rewritten
    static Object Sync;
    /**
     * A list of registered
     * <code>PortOwnershipListener</code>s.
     */
    // TODO (by Alexander Graf) the list of listeners should have a plural name
    // and should be private. Should use interface type here and Vector is
    // deprecated
    Vector ownershipListener;

    /*
     * Static block to initialize the class. Loads the rxtx driver.
     */
    static {
        Sync = new Object();
    }

    /**
     * Creates a new port identifier. This is typically called by a drivers
     * <code>initialize()</code> method.
     *
     * @param portName a name for the port which is used as a per-driver-unique
     * identifier. Because the name might be exposed to the user it should be
     * unique across all drivers.
     * @param commPort (unused by rxtx; set to null)
     * @param portType the hardware type of the port
     * @param driver the driver which is used to access this port
     */
    CommPortIdentifier(String portName, CommPort commPort, int portType,
            CommDriver driver) {
        this.portName = portName;
        this.commPort = commPort;
        this.portType = portType;
        this.next = null;
        this.driver = driver;
    }

    static void addPortName(String portName, int portType, CommDriver driver) {
        // TODO (by Alexander Graf) the usage of the constructor with a null
        // value for the port is a major design problem. Rxtx currently creates
        // the port objects on a per-open basis. This clashes with the design
        // idea of the comm API where a port object is created by the driver
        // once and given to the CommPortIdentifier constructor.
        // This problem is again introduced by the poor design of the comm APIs
        // SPI.
        addIdentifierToList(new CommPortIdentifier(portName, null, portType, driver));
    }

    /**
     * Adds a new port identifier to the list of available ports on the system.
     *
     * @param identifier the identifier to add to the list
     */
    //TODO (Alexander Graf) This method/class seems to reinvent the wheel
    //by implementing its own linked list functionallity
    private static void addIdentifierToList(CommPortIdentifier identifier) {
        synchronized (Sync) {
            if (CommPortIndex == null) {
                CommPortIndex = identifier;
            } else {
                CommPortIdentifier index = CommPortIndex;
                while (index.next != null) {
                    index = index.next;
                }
                index.next = identifier;
            }
        }
    }

    /**
     * Registers an ownership listener. The listener will be informed when other
     * applications open or close the port which is represented by this
     * <code>CommPortIdentifier</code>. When the port is currently owned by this
     * application the listener is informed when other applications request
     * ownership of the port.
     *
     * @param c the ownership listener to register
     */
    public void addPortOwnershipListener(CommPortOwnershipListener c) {
        if (ownershipListener == null) {
            ownershipListener = new Vector();
        }
        if (!ownershipListener.contains(c)) {
            ownershipListener.addElement(c);
        }
    }

    /**
     * Returns a textual representation of the current owner of the port. An
     * owner is an application which is currently using the port (in the sense
     * that it opened the port and has not closed it yet).
     *
     * To check if a port is owned use the
     * <code>isCurrentlyOwned</code> method. Do not rely on this method to
     * return null. It can't be guaranteed that owned ports have a non null
     * owner.
     *
     * @return the port owner or null if the port is not currently owned
     */
    public String getCurrentOwner() {
        return owner;
    }

    /**
     * Returns the name of the port which is represented by this
     * <code>CommPortIdentifier</code>. Be aware of the fact that there are
     * different conventions for port names on different operating systems (for
     * example a port name on linux looks like '/dev/tty1', on Windows like
     * 'COM1'). Therefor the application should not parse this string in a
     * probably non cross platform compatible way.
     *
     * @return the name of the associated port
     */
    public String getName() {
        return portName;
    }

    /**
     * Returns the associated port identifier of a port with the given name. If
     * no port with the given name can be found a
     * <code>NoSuchPortException</code> is thrown.
     *
     * @param portName the name of the port whose identifier is looked up
     * @return the identifier of the port
     * @throws NoSuchPortException when no port with the given name was found
     */
    public static CommPortIdentifier getPortIdentifier(String portName)
            throws NoSuchPortException {
        CommPortIdentifier index;

        synchronized (Sync) {
            index = CommPortIndex;
            while (index != null && !index.portName.equals(portName)) {
                index = index.next;
            }
            if (index == null) {
                /*
                 * This may slow things down but if you pass the string for the
                 * port after a device is plugged in, you can find it now.
                 *
                 * http://bugzilla.qbang.org/show_bug.cgi?id=48
                 */
                getPortIdentifiers();
                index = CommPortIndex;
                while (index != null && !index.portName.equals(portName)) {
                    index = index.next;
                }
            }
        }
        if (index != null) {
            return index;
        } else {
            throw new NoSuchPortException(portName + " not found");
        }
    }

    /**
     * Retrieves the port identifier for a given port.
     *
     * @param port the port whose identifier is requested
     * @return the identifier of the port
     * @throws NoSuchPortException if the given port is invalid. This might
     * happen when the physical port was removed from the system (for example a
     * USB based adapter).
     */
    public static CommPortIdentifier getPortIdentifier(CommPort port)
            throws NoSuchPortException {
        CommPortIdentifier c;
        synchronized (Sync) {
            c = CommPortIndex;
            while (c != null && c.commPort != port) {
                c = c.next;
            }
        }
        if (c != null) {
            return c;
        }
        throw new NoSuchPortException(port + " not found");
    }

    /**
     * Returns an enumeration of port identifiers which represent the
     * communication ports currently available on the system.
     *
     * @return the enumeration of port identifiers
     */
    public static Enumeration getPortIdentifiers() {
        //Do not allow anybody get any ports while we are re-initializing
        //because the CommPortIndex points to invalid instances during that time
        synchronized (Sync) {
            //Remember old ports in order to restore them for ownership events later
            HashMap oldPorts = new HashMap();
            CommPortIdentifier p = CommPortIndex;
            while (p != null) {
                oldPorts.put(p.portName, p);
                p = p.next;
            }
            //CommPortIndex = null;
            //Restore old CommPortIdentifier objects where possible,
            //in order to support proper ownership event handling.
            //Clients might still have references to old identifiers!
            CommPortIdentifier curPort = CommPortIndex;
            CommPortIdentifier prevPort = null;
            while (curPort != null) {
                CommPortIdentifier matchingOldPort =
                        (CommPortIdentifier) oldPorts.get(curPort.portName);
                if ((matchingOldPort != null)
                        && (matchingOldPort.portType == curPort.portType)) {
                    //replace new port by old one
                    matchingOldPort.driver = curPort.driver;
                    matchingOldPort.next = curPort.next;
                    if (prevPort == null) {
                        CommPortIndex = matchingOldPort;
                    } else {
                        prevPort.next = matchingOldPort;
                    }
                    prevPort = matchingOldPort;
                } else {
                    prevPort = curPort;
                }
                curPort = curPort.next;
            }

        }
        return new CommPortEnumerator();
    }

    /**
     * Returns the port hardware type of the associated port. The port type is
     * encoded as one of the available
     * <code>PORT_*</code> constants.
     *
     * @return the port type
     */
    public int getPortType() {
        return portType;
    }

    /**
     * Determines whether the associated port is in use by an application
     * (including this application).
     *
     * @return true if an application is using the port, false if the port is
     * not currently owned.
     */
    public synchronized boolean isCurrentlyOwned() {
        return !available;
    }

    // TODO (by Alexander Graf) this method is for legacy support of the
    //comm API and is obviosly unsupported by rxtx
    // It should be deprecated (and possibly removed some day) when we don't
    // care about compatibility to the comm API
    public synchronized CommPort open(FileDescriptor f)
            throws UnsupportedCommOperationException {
        throw new UnsupportedCommOperationException();
    }

    private native String native_psmisc_report_owner(String PortName);
    private boolean HideOwnerEvents;

    /**
     * Opens the port. When the port is in use by another application an
     * ownership request is propagated. When the current owner does not close
     * the port within
     * <code>timeLimit</code> milliseconds, the open operation will fail with a
     * <code>PortInUseException</code>.
     *
     * The
     * <code>owner</code> should contain the name of the application which opens
     * the port. Later it can be read by other applications to distinguish the
     * owner of the port.
     *
     * @param owner the name of the application which is opening the port
     * @param timeLimit the time limit to obtain ownership in milliseconds
     * @return the port object of the successfully opened port
     * @throws gnu.io.PortInUseException when the exclusive ownership of the
     * port could not be obtained
     */
    // TODO (by Alexander Graf) is there really done an ownership request that
    // can reach applications outside of this virtual machine, probably not
    // not written in java? I can't see where this request is done.
    public CommPort open(String owner, int timeLimit)
            throws gnu.io.PortInUseException {
        boolean isAvailable;
        synchronized (this) {
            isAvailable = this.available;
            if (isAvailable) {
                //assume ownership inside the synchronized block
                this.available = false;
                this.owner = owner;
            }
        }
        if (!isAvailable) {
            long waitTimeEnd = System.currentTimeMillis() + timeLimit;
            //fire the ownership event outside the synchronized block
            fireOwnershipEvent(CommPortOwnershipListener.PORT_OWNERSHIP_REQUESTED);
            long waitTimeCurr;
            synchronized (this) {
                while (!available && (waitTimeCurr = System.currentTimeMillis()) < waitTimeEnd) {
                    try {
                        wait(waitTimeEnd - waitTimeCurr);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                isAvailable = this.available;
                if (isAvailable) {
                    //assume ownership inside the synchronized block
                    this.available = false;
                    this.owner = owner;
                }
            }
        }
        if (!isAvailable) {
            throw new gnu.io.PortInUseException(getCurrentOwner());
        }
        //At this point, the CommPortIdentifier is owned by us.
        try {
            if (commPort == null) {
                commPort = driver.getCommPort(portName, portType);
            }
            if (commPort != null) {
                fireOwnershipEvent(CommPortOwnershipListener.PORT_OWNED);
                return commPort;
            } else {
                String errMsg;
                try {
                    errMsg = native_psmisc_report_owner(portName);
                } catch (Throwable t) {
                    errMsg = "Port " + portName
                            + " already owned... unable to open.";
                }
                throw new gnu.io.PortInUseException(errMsg);
            }
        } finally {
            if (commPort == null) {
                //something went wrong reserving the commport -> unown the port
                synchronized (this) {
                    this.available = true;
                    this.owner = null;
                }
            }
        }
    }

    /**
     * Removes the given ownership listener from this port identifier.
     *
     * @param c the listener to remove
     */
    public void removePortOwnershipListener(CommPortOwnershipListener c) {
        if (ownershipListener != null) {
            ownershipListener.removeElement(c);
        }
    }

    /**
     * Clean up the ownership information and send the event.
     */
    void internalClosePort() {
        synchronized (this) {
            owner = null;
            available = true;
            // TODO (by Alexander Graf) to set the commPort to null is
            // incompatible with the idea behind the architecture of the
            // constructor, where commPort is assigned by the driver once
            // in a virtual machines lifetime.
            commPort = null;
            /*
             * this tosses null pointer??
             */
            notifyAll();
        }
        fireOwnershipEvent(CommPortOwnershipListener.PORT_UNOWNED);
    }

    private void fireOwnershipEvent(int eventType) {
        if (ownershipListener != null) {
            CommPortOwnershipListener c;
            //TODO (by Alexander Graf) for statement abuse ...
            for (Enumeration e = ownershipListener.elements();
                    e.hasMoreElements();
                    c.ownershipChange(eventType)) { // ... here
                c = (CommPortOwnershipListener) e.nextElement();
            }
        }
    }
}
