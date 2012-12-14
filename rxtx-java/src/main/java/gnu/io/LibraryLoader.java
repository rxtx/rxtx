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

/**
 * LibraryLoader is a Utility class to find and load a native library based on
 * the operating system and CPU architecture of the running system.
 *
 * @author Jeff Benjamin
 */
public final class LibraryLoader {

    /**
     * This private constructor prevents the utility class LibraryLoader from
     * being instantiated.
     */
    private LibraryLoader() {
    }
    /**
     * Holds a operating system and CPU architecture depending value which is
     * used to find the appropriate native library for the platform running this
     * LibraryLoader.
     *
     * The value of this constant is assigned during static class
     * initialization.
     */
    private static final String LIBRARY_SUFFIX;

    static {
        final String osName = System.getProperty("os.name");
        final String osArch = System.getProperty("os.arch");

        if ("Windows 95".equals(osName)
                || "Windows 98".equals(osName)
                || "Windows Me".equals(osName)
                || "Windows NT".equals(osName)
                || "Windows NT (unknown)".equals(osName) // Vista on Java 1.4
                || "Windows Vista".equals(osName) // Vista on newer Java
                || "Windows XP".equals(osName)
                || "Windows 2000".equals(osName)
                || "Windows 2003".equals(osName)) {
            // Windows needs no suffix, the dlls can be loaded
            LIBRARY_SUFFIX = "";
        } else if ("Mac OS X".equals(osName)) {
            /* Check for different Mac OS X versions is not necessary as all
             * versions (i836, x86_64, and ppc) are in the universal binary */
            LIBRARY_SUFFIX = "-mac-universal";
        } else if ("Linux".equals(osName)) {
            if ("x86".equals(osArch)
                    || "i386".equals(osArch)
                    || "i486".equals(osArch)
                    || "i586".equals(osArch)
                    || "i686".equals(osArch)) {
                LIBRARY_SUFFIX = "-linux-x86-32";
            } else if ("x86_64".equals(osArch) || "amd64".equals(osArch)) {
                /* NOTE: Java's os.arch property is a bit of a misnomer,
                 * fortunately for us.  The 32 vs. 64 bit library selection
                 * decision must based on what JVM we're running in and not on
                 * the host OS's architecture.  If we're running 32-bit Java on
                 * a 64-bit Linux OS then we should still use 32-bit shared
                 * library.  Luckily for us, the os.arch property is what the
                 * JVM is and not what the OS is. */
                LIBRARY_SUFFIX = "-linux-x86-64";
            } else {
                // Unknown linux architecture, assume 32 bit.
                LIBRARY_SUFFIX = "-linux-x86-32";
            }
        } else if ("Solaris".equals(osName) || "SunOS".equals(osName)) {
            if ("sparc".equals(osArch)) {
                LIBRARY_SUFFIX = "-solaris-sparc-32";
            } else {
                // Unkown solarix arch
                LIBRARY_SUFFIX = "-solaris-sparc-32";
            }
        } else {
            // Unknown os/arch
            LIBRARY_SUFFIX = "";
        }
    }

    /**
     * Load the given library.
     *
     * A platform specific suffix is added to the library prior loading.
     *
     * @param libraryName the name of the library to load
     */
    public static void loadLibrary(final String libraryName) {
        assert LIBRARY_SUFFIX != null;
        assert libraryName != null;
        System.loadLibrary(libraryName + LIBRARY_SUFFIX);
    }
}
