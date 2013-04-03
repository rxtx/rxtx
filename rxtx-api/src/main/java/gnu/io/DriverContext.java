/*-------------------------------------------------------------------------
 |   RXTX License v 2.1 - LGPL v 2.1 + Linking Over Controlled Interface.
 |   RXTX is a native interface to serial ports in java.
 |   Copyright 2013 by Alexander Graf <alex at antistatix.de> and others
 |   who actually wrote it.  See individual source files for more information.
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

/**
 * The
 * <code>DriverContext</code> gives access to a part of the API which is
 * reserved for driver implementations only.
 *
 * @author Alexander Graf [alex -at- antistatix.de]
 * @since TBD
 */
public final class DriverContext {

    /**
     * The singleton instance of this class.
     */
    private static DriverContext instance = new DriverContext();

    /**
     * Creates the singleton instance.
     */
    private DriverContext() {
    }

    /**
     * @return the singleton instance of this class which must not be available
     * outside of this package
     */
    static DriverContext getInstance() {
        return instance;
    }

    /**
     * Returns a factory class which can be used by device driver
     * implementations to create various events.
     *
     * @return the factory instance
     */
    public EventFactory getEventFactory() {
        return EventFactory.getInstance();
    }

    /**
     * Creates a new loader for native libraries. <p>The library loader can be
     * used by drivers which require a native library (JNI library). The loader
     * is able to discover libraries in a JAR file as well as on the native
     * library path.</p>
     *
     * <p>To find a library in a JAR, a
     * <code>ClassLoader</code> is required. Typically this is the class loader
     * of the driver implementation class: We assume YourDriver extends
     * CommDriver, then the class loader can be obtained by:
     * <code>YourDriver.class.getClassLoader()</code></p>
     *
     * <p>The given classLoader will be used to find
     * the native library in the resources (e.g. a JAR file) at the given
     * resourcePath.</p>
     *
     * @param classLoader The class loader which has access to the JAR file.
     * @param resourcePath the path inside the JAR without trailing slash
     * @return the new library loader instance
     * @throws NullPointerException if <code>classLoader</code> or
     * <code>resourcePath</code> is null
     */
    public LibraryLoader createLibraryLoader(final ClassLoader classLoader,
            final String resourcePath) {
        if (classLoader == null) {
            throw new NullPointerException("ClassLoader must not be null.");
        }
        if (resourcePath == null) {
            throw new NullPointerException("resource path must not be null");
        }
        return new LibraryLoader(classLoader, resourcePath);
    }

    /**
     * Provides a port to the API. Using this method, drivers can register ports
     * which are available through them. When the user is interested in such a
     * port the API might request a
     * <code>CommPort</code> object later, providing the
     * <code>portName</code>,
     * <code>portType</code> combination.
     *
     * @param portName a port name which uniquely identifies a port provided by
     * this driver.
     * @param portType the port type encoded as <code>CommPortIdentifier.PORT_*</code> constant
     * @param driver the driver which is providing the port
     */
    public void registerPort(final String portName, final int portType,
            final CommDriver driver) {
        CommPortIdentifier.addPortName(portName, portType, driver);
    }
}
