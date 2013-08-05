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
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * The DriverManager class gives access to the driver loading process for the
 * API user.
 *
 * @author Alexander Graf [alex -at- antistatix.de]
 * @since TBD
 */
public final class DriverManager {

    /**
     * The singleton instance of this manager.
     */
    private static DriverManager instance;
    /**
     * The ServiceLoader instance used to load all driver implementations.
     */
    private final ServiceLoader<CommDriver> loader;
    /**
     * Holds all known driver classes. This is used to prevent the drivers from
     * being initialized twice. Initialization of known drivers is skipped in
     * loadDrivers().
     */
    private final List<Class> drivers = new ArrayList<Class>();

    /**
     * Creates the singleton instance.
     */
    private DriverManager() {
        loader = ServiceLoader.load(CommDriver.class);
    }

    /**
     * Returns the singleton instance of
     * <code>DriverManager</code>.
     *
     * @return the DriverManager instance
     */
    public static DriverManager getInstance() {
        synchronized (DriverManager.class) {
            if (instance == null) {
                instance = new DriverManager();
            }
            return instance;
        }
    }

    /**
     * Loads and initializes all currently registered
     * <code>CommDriver</code>s.
     *
     * In most implementations this method is called only once before any other
     * interaction with this API happens, to ensure that all drivers are loaded.
     *
     * For advanced applications which use plugins and might add drivers at
     * runtime, this method must be called whenever a new driver was added to
     * the class path.
     */
    public void loadDrivers() {
        synchronized (this) {
            loader.reload();
            for (CommDriver driver : loader) {
                final Class<? extends CommDriver> driverClass =
                        driver.getClass();
                if (!drivers.contains(driverClass)) {
                    drivers.add(driverClass);
                    driver.initialize(DriverContext.getInstance());
                }
            }
        }
    }
}
