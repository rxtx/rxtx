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
package gnu.io.spi;

import gnu.io.CommPort;
import gnu.io.DriverContext;
import gnu.io.PortInUseException;

import java.io.IOException;

/**
 * The
 * <code>CommDriver</code> is a service provider interface for device drivers.
 * Device drivers implement the mapping between port hardware and the
 * <code>CommPort</code> class. All device driver implementations must implement
 * this interface. In order to be discovered by the Java SE6
 * <code>ServiceLoader</code> mechanism, the service implementation must be
 * registered in the META-INF/service directory as described in
 * <code>ServiceLoader</code>.
 *
 * @see java.util.ServiceLoader
 *
 * @author Trent Jarvi
 */
public interface CommDriver {

    /**
     * The driver returns a
     * <code>CommPort</code> for a given name and type. The driver
     * implementation must return a
     * <code>CommPort</code> instance of the type which is related to the
     * <code>portType</code> option and matches the given
     * <code>portName</code>. If the driver can not find a device matching the
     * combination of
     * <code>portName</code> and
     * <code>portType</code>
     * <code>null</code> must be returned.
     *
     * It is guaranteed that rxtx will not call this method before calling
     * <code>initialize()</code>.
     *
     * @param portName the name of the port
     * @param portType a constant from <code>CommPortIdentifier.PORT_*</code>
     * describing the type of the port
     * @return the <code>CommPort</code> instance or <code>null</code>
     */
    CommPort getCommPort(String portName, int portType) throws PortInUseException, IOException;

    /**
     * Initializes this driver. This method is called by rxtx once in a virtual
     * machines lifetime when a driver was discovered. The driver can use this
     * call to configure itself, load any required native libraries and register
     * devices via
     * <code>DriverContext.addPortName()</code> to be prepared for calls to
     * <code>getCommPort()</code>.
     *
     * @param context a callback class for the driver to get access to the
     * driver service provider interface
     */
    void initialize(DriverContext context);
}
