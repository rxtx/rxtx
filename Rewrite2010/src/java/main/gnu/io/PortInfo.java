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
/* Martin Pool <mbp@linuxcare.com> added support for explicitly-specified
 * lists of ports, October 2000. */
/* Joseph Goldstone <joseph@lp.com> reorganized to support registered ports,
 * known ports, and scanned ports, July 2001 */

package gnu.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Port information. This class encapsulates code for maintaining a list of available
 * ports. RXTX supports two methods of determining available ports: a user-specified list
 * of ports in a property, and automatic port scanning.
 * <p>
 * Serial ports can be specified in the <code>gnu.io.rxtx.SerialPorts</code> or
 * <code>gnu.io.SerialPorts</code> property. The property value is a list of port names
 * separated by a colon [:]. Parallel ports can be specified in the
 * <code>gnu.io.rxtx.ParallelPorts</code> or <code>gnu.io.ParallelPorts</code> property.
 * The property value is a list of port names separated by a colon [:]. If ports are
 * specified in a property, then automatic port scanning is disabled.
 * </p>
 * <p>
 * This class contains a port scanning thread that scans for port additions/deletions. The
 * port scanning interval can be set with the <code>gnu.io.port.scanning.interval</code>
 * property (in milliseconds). The minimum value is 1000 (one second) and the default is
 * 2000 (two seconds).
 * </p>
 * 
 * <dl>
 * <dt><b>Thread-safe:</b></dt>
 * <dd>This class may be used in multi-threaded applications.</dd>
 * </dl>
 * 
 * @author <a href="http://www.rxtx.org">The RXTX Project</a>
 */
public final class PortInfo {
    private static final String source = PortInfo.class.getName();
    private static final Dispatcher dispatch = Dispatcher.getInstance();
    // Guarded by copy-on-modify
    private static List currentPortInfos; // <PortInfo>

    static {
        /*
         * First try to find ports specified in the properties file. If that doesn't
         * exist, then have native code scan for ports.
         */
        List portNames = getSpecifiedPorts(); // <String>
        if (portNames != null) {
            currentPortInfos = portNamesToPortInfos(portNames);
        } else {
            synchronized (Dispatcher.class) {
                currentPortInfos = new ArrayList();
            }
            Thread portScanner = new PortScanner();
            portScanner.start();
        }
    }

    /**
     * Returns a list of available ports. The returned <code>List</code>
     * is a copy, so it is thread-safe.
     * @return A list of available ports.
     */
    public static List getCurrentPortInfos() {
        synchronized (Dispatcher.class) {
            if (currentPortInfos.size() == 0) {
                currentPortInfos = dispatch.getValidPortInfos();
            }
        }
        List portInfos = new ArrayList();
        portInfos.addAll(currentPortInfos);
        return portInfos;
    }

    private static List getSpecifiedPorts() {
        List ports = null;
        String portString = null;
        if ((portString = Settings.getString("gnu.io.rxtx.SerialPorts")) == null) {
            portString = Settings.getString("gnu.io.SerialPorts");
            if (portString != null && portString.length() > 0) {
                ports = Arrays.asList(portString.split(":"));
            }
        }
        if ((portString = Settings.getString("gnu.io.rxtx.ParallelPorts")) == null) {
            portString = Settings.getString("gnu.io.ParallelPorts");
            if (portString != null && portString.length() > 0) {
                if (ports == null) {
                    ports = Arrays.asList(portString.split(":"));
                } else {
                    ports.addAll(Arrays.asList(portString.split(":")));
                }
            }
        }
        return ports;
    }

    private static List portNamesToPortInfos(List portNames) {
        List portInfos = new ArrayList();
        Iterator i = portNames.iterator();
        synchronized (Dispatcher.class) {
            while (i.hasNext()) {
                String portName = (String) i.next();
                try {
                    int portType = dispatch.getPortType(portName);
                    portInfos.add(new PortInfo(portName, portType));
                } catch (Exception e) {
                    // TODO: Log exception
                }
            }
        }
        return portInfos;
    }

    // --------------------------------------------------------------------- //

    /**
     * The port name.
     */
    public final String portName;
    /**
     * The port type.
     */
    public final int portType;

    private PortInfo(String portName, int portType) {
        this.portName = portName;
        this.portType = portType;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        try {
            PortInfo that = (PortInfo) obj;
            return this.portName.equals(that.portName) && this.portType == that.portType;
        } catch (Exception e) {}
        return false;
    }

    private static class PortScanner extends Thread {
        private final long scanningInterval;

        private PortScanner() {
            super("Port Scanner");
            long scanningInterval = Settings.getLong("gnu.io.port.scanning.interval", 2000);
            this.scanningInterval = scanningInterval < 1000 ? 1000 : scanningInterval;
        }

        public void run() {
            Runtime runtime = Runtime.getRuntime();
            runtime.addShutdownHook(new Thread() {public void run(){PortScanner.this.interrupt();}});
            while (!Thread.interrupted()) {
                try {
                    Thread.sleep(this.scanningInterval);
                } catch (InterruptedException e) {
                    break;
                }
                List newPortInfos = null;
                try {
                    synchronized (Dispatcher.class) {
                        newPortInfos = dispatch.getValidPortInfos();
                    }
                } catch (Exception e) {
                    Log.log(e, "Exception thrown in port scanner thread: ", source);
                }
                if (newPortInfos == null) {
                    continue;
                }
                if (!currentPortInfos.containsAll(newPortInfos) || !newPortInfos.containsAll(currentPortInfos)) {
                    currentPortInfos = newPortInfos;
                    CommPortIdentifier.updateCurrentPorts();
                }
            }
            Log.log("Port Scanner thread interrupted", source);
        }
    }
}
