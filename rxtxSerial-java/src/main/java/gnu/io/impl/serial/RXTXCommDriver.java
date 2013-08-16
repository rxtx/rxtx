/*-------------------------------------------------------------------------
 |   RXTX License v 2.1 - LGPL v 2.1 + Linking Over Controlled Interface.
 |   RXTX is a native interface to serial ports in java.
 |   Copyright 1998 Kevin Hester, kevinh@acm.org
 |   Copyright 2000-2012 Trent Jarvi tjarvi@qbang.org and others who
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
package gnu.io.impl.serial;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.DriverContext;
import gnu.io.LibraryLoader;
import gnu.io.PortInUseException;
import gnu.io.spi.CommDriver;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is the default rxtx serial driver implementation.
 *
 * @author Trent Jarvi
 * @author Holger Lehmann (IBM)
 * @author Peter Bennett (Bencom)
 * @author Alexander Graf
 *
 * @deprecated Do NOT use this class. Your code WILL break. As an API user you
 * must not depend on implementation details, this class is one of them. Always
 * code against the service interface. Do not cast instances to this class type.
 *
 */
// TODO (by Alexander Graf) this class seems to implement a driver for both
// serial and parallel ports. The drivers should be separated.
@Deprecated
public final class RXTXCommDriver implements CommDriver {

    private static final Logger LOGGER = Logger.getLogger(
            RXTXCommDriver.class.getName());
    private DriverContext context;
    /**
     * Get the Serial port prefixes for the running OS
     */
    private String deviceDirectory;
    private final String osName = System.getProperty("os.name");

    /**
     * @deprecated Do NOT create instances of this class, your code WILL beak!
     */
    @Deprecated
    public RXTXCommDriver() {
    }

    private native boolean registerKnownPorts(int PortType);

    private native boolean isPortPrefixValid(String dev);

    private native boolean testRead(String dev, int type);

    private native String getDeviceDirectory();
    // for rxtx prior to 2.1.7

    public static native String nativeGetVersion();

    // this method is called by native code
    DriverContext getDriverContext() {
        return context;
    }

    // FIXME: This method is never called.
    private final String[] getValidPortPrefixes(String candidatePortPrefixes[]) {
        /*
         * 256 is the number of prefixes ( COM, cua, ttyS, ...) not the number
         * of devices (ttyS0, ttyS1, ttyS2, ...)
         *
         * On a Linux system there are about 400 prefixes in deviceDirectory.
         * registerScannedPorts() assigns CandidatePortPrefixes to something
         * less than 50 prefixes.
         *
         * Trent
         */

        String validPortPrefixes[] = new String[256];
        if (candidatePortPrefixes == null) {
            LOGGER.log(Level.WARNING,
                    "No ports prefixes known for this System.\n"
                    + "Please check the port prefixes listed for OS {0} "
                    + "in RXTXCommDriver.registerScannedPorts()", osName);
        }
        int i = 0;
        for (int j = 0; j < candidatePortPrefixes.length; j++) {
            if (isPortPrefixValid(candidatePortPrefixes[j])) {
                validPortPrefixes[i++] =
                        candidatePortPrefixes[j];
            }
        }
        String[] returnArray = new String[i];
        System.arraycopy(validPortPrefixes, 0, returnArray, 0, i);
        return returnArray;
    }

    /**
     * handle solaris/sunos /dev/cua/a convention
     */
    private void checkSolaris(String PortName, int PortType) {
        for (char p = 'a'; p <= 'z'; p++) {
            String suffix = String.valueOf(p);
            if (testRead(PortName.concat(suffix), PortType)) {
                context.registerPort(PortName.concat(suffix), PortType, this);
            }
        }
        /**
         * check for 0-9 in case we have them (Solaris USB)
         */
        for (char p = '0'; p <= '9'; p++) {
            String suffix = String.valueOf(p);
            if (testRead(PortName.concat(suffix), PortType)) {
                context.registerPort(PortName.concat(suffix), PortType, this);
            }
        }
    }

    private void registerValidPorts(String candidateDeviceNames[],
            String validPortPrefixes[], int portType) {
        int i = 0;
        int p = 0;
        /*
         * FIXME quick fix to get COM1-8 on windows working. The Read test is
         * not working properly and its crunch time...
         * if(osName.toLowerCase().indexOf("windows") != -1 ) { for( i=0;i <
         * CandidateDeviceNames.length;i++ ) { CommPortIdentifier.addPortName(
         * CandidateDeviceNames[i], PortType, this ); } return;
         *
         * }
         */
        if (candidateDeviceNames != null && validPortPrefixes != null) {
            for (i = 0; i < candidateDeviceNames.length; i++) {
                for (p = 0; p < validPortPrefixes.length; p++) {
                    /*
                     * this determines: device file Valid ports /dev/ttyR[0-9]*
                     * != /dev/ttyS[0-9]* /dev/ttySI[0-9]* != /dev/ttyS[0-9]*
                     * /dev/ttyS[0-9]* == /dev/ttyS[0-9]*
                     *
                     * Otherwise we check some ports multiple times. Perl would
                     * rock here.
                     *
                     * If the above passes, we try to read from the port. If
                     * there is no err the port is added. Trent
                     */
                    String v = validPortPrefixes[p];
                    int vl = v.length();
                    String c = candidateDeviceNames[i];
                    if (c.length() < vl) {
                        continue;
                    }
                    String cu =
                            c.substring(vl).toUpperCase();
                    String cl =
                            c.substring(vl).toLowerCase();
                    if (!(c.regionMatches(0, v, 0, vl)
                            && cu.equals(cl))) {
                        continue;
                    }
                    String portName;
                    if (osName.toLowerCase().indexOf("windows") == -1) {
                        portName = deviceDirectory + c;
                    } else {
                        portName = c;
                    }
                    if (osName.equals("Solaris")
                            || osName.equals("SunOS")) {
                        checkSolaris(portName, portType);
                    } else if (testRead(portName, portType)) {
                        context.registerPort(portName, portType, this);
                    }
                }
            }
        }
    }

    public void initialize(final DriverContext context) {
        this.context = context;

        LibraryLoader loader = context.createLibraryLoader(
                RXTXCommDriver.class.getClassLoader(),
                "gnu/io/impl/serial");
        final boolean installationSuccessful = loader.load("rxtxSerial");
        if (installationSuccessful) {
            deviceDirectory = getDeviceDirectory();
            if (!registerSpecifiedPorts(CommPortIdentifier.PORT_SERIAL)) {
                if (!registerKnownPorts(CommPortIdentifier.PORT_SERIAL)) {
                    registerScannedPorts(CommPortIdentifier.PORT_SERIAL);
                }
            }
        }
    }

    private void addSpecifiedPorts(String names, int portType) {
        final String pathSep = System.getProperty("path.separator", ":");
        final StringTokenizer tok = new StringTokenizer(names, pathSep);

        while (tok.hasMoreElements()) {
            final String portName = tok.nextToken();

            if (testRead(portName, portType)) {
                context.registerPort(portName, portType, this);
                LOGGER.log(Level.FINEST, "registered port {0}", portName);
            } else {
                LOGGER.log(Level.INFO, "port {0} not accessible", portName);
            }
        }
    }


    /*
     * Register ports specified in the file "gnu.io.rxtx.properties" Key system
     * properties: gnu.io.rxtx.SerialPorts gnu.io.rxtx.ParallelPorts
     *
     * Tested only with sun jdk1.3 The file gnu.io.rxtx.properties may reside in
     * the java extension dir, or it can be anywhere in the classpath.
     *
     * Example: /usr/local/java/jre/lib/ext/gnu.io.rxtx.properties
     *
     * The file contains the following key properties:
     *
     * gnu.io.rxtx.SerialPorts=/dev/ttyS0:/dev/ttyS1:
     * gnu.io.rxtx.ParallelPorts=/dev/lp0:
     *
     */
    private boolean registerSpecifiedPorts(int portType) {
        if (portType != CommPortIdentifier.PORT_SERIAL) {
            throw new IllegalArgumentException("unsupported port type");
        }
        String val = getSpecifiedPorts("gnu.io.rxtx.SerialPorts",
                "gnu.io.SerialPorts");
        if (val != null) {
            addSpecifiedPorts(val, portType);
            return true;
        }
        return false;
    }

    /**
     * Finds the "gnu.io.rxtx.properties" file in java extension dir
     * (legacy support).
     * The file gnu.io.rxtx.properties may reside in the java extension dir,
     * or it can be anywhere in the classpath.
     *
     * @return the properties file object if found or null if no property file
     * found in any extension directory
     */
    private File findPropertyFile() {
        final String[] extDirs = System.getProperty("java.ext.dirs").split(":");
        final String separator = System.getProperty("file.separator");
        for (final String extDir : extDirs) {
            final String extFile =
                    extDir + separator + "gnu.io.rxtx.properties";
            final File file = new File(extFile);
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }

    /**
     * Loads the "gnu.io.rxtx.properties" file or resource.
     * The file gnu.io.rxtx.properties may reside in the java extension dir,
     * or it can be anywhere in the classpath.
     *
     * @return the loaded properties or an empty property set if no property
     * source was found.
     */
    private Properties loadRxtxProperties() {
        final Properties props = new Properties();
        final File propertyFile = findPropertyFile();
        if (propertyFile != null) {
            FileInputStream in = null;
            try {
                in = new FileInputStream(propertyFile);
                props.load(in);
                LOGGER.log(Level.FINE,
                        "loaded properties from file {0}", propertyFile);
            } catch (FileNotFoundException ex) {
                LOGGER.log(Level.WARNING,
                        "discovered property file disappeared unexpectedly",
                        ex);
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "could not read property file", ex);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ex) {
                        LOGGER.log(Level.WARNING,
                                "could not close property file after loading",
                                ex);
                    }
                }
            }
        } else {
            final ClassLoader loader = Thread.currentThread()
                    .getContextClassLoader();
            final URL propertyResource = loader
                    .getResource("gnu.io.rxtx.properties");
            if (propertyResource != null) {
                try {
                    props.load(propertyResource.openStream());
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE,
                            "could not load property resource file", ex);
                }
            } else {
                LOGGER.log(Level.INFO,
                        "No property file or resource found (using defaults)");
            }
        }
        return props;
    }

    /*
     * Return list of specified ports from System Property or Property File.
     *
     * System Properties take precedence over the gnu.io.rxtx.properties file.
     * Data from the properties file is cached in System Properties to avoid
     * re-loading the file at a later time.
     *
     * @param key1 primary (new) key
     * @param key2 legacy (fallback) key
     * @return RXTX specified ports as per requested keys, or <code>null</code> if none found.
     */
    private String getSpecifiedPorts(String key1, String key2) {
        //Try loading from System Properties first
        String val = System.getProperty(key1);
        if (val == null && key2 != null) {
            val = System.getProperty(key2);
        }
        if (val == null) {
            //Not specified: Try loading from gnu.io.rxtx.properties file
            Properties props = loadRxtxProperties();
            val = props.getProperty(key1);
            if (val != null) {
                System.setProperty(key1, val);
            } else if (key2 != null) {
                val = props.getProperty(key2);
                if (val != null) {
                    System.setProperty(key2, val);
                } else {
                    //Cache empty String: avoid trying to load properties again later
                    System.setProperty(key1, "");
                }
            }
        } else if (val.equals("")) {
            //Return null rather than cached empty String
            val = null;
        }
        return val;
    }

    /*
     * Look for all entries in deviceDirectory, and if they look like they
     * should be serial ports on this OS and they can be opened then register
     * them.
     *
     */
    private void registerScannedPorts(int portType) {
        String[] candidateDeviceNames;
        LOGGER.log(Level.FINEST,
                "scanning device directory {0} for ports of type {1}",
                new Object[]{deviceDirectory, portType});
        if (osName.equals("Windows CE")) {
            String[] temp = {"COM1:", "COM2:", "COM3:", "COM4:",
                "COM5:", "COM6:", "COM7:", "COM8:"};
            candidateDeviceNames = temp;
        } else if (osName.toLowerCase().indexOf("windows") != -1) {
            String[] temp = new String[259];
            for (int i = 1; i <= 256; i++) {
                temp[i - 1] = "COM" + i;
            }
            for (int i = 1; i <= 3; i++) {
                temp[i + 255] = "LPT" + i;
            }
            candidateDeviceNames = temp;
        } else if (osName.equals("Solaris") || osName.equals("SunOS")) {
            /*
             * Solaris uses a few different ways to identify ports. They could
             * be /dev/term/a /dev/term0 /dev/cua/a /dev/cuaa the /dev/???/a
             * appears to be on more systems.
             *
             * The uucp lock files should not cause problems.
             */
            /*
             * File dev = new File( "/dev/term" ); String deva[] = dev.list();
             * dev = new File( "/dev/cua" ); String devb[] = dev.list();
             * String[] temp = new String[ deva.length + devb.length ]; for(int
             * j =0;j<deva.length;j++) deva[j] = "term/" + deva[j]; for(int j
             * =0;j<devb.length;j++) devb[j] = "cua/" + devb[j];
             * System.arraycopy( deva, 0, temp, 0, deva.length );
             * System.arraycopy( devb, 0, temp, deva.length, devb.length ); if(
             * debug ) { for( int j = 0; j< temp.length;j++) System.out.println(
             * temp[j] ); } CandidateDeviceNames=temp;
             */

            /*
             *
             * ok.. Look the the dirctories representing the port kernel driver
             * interface.
             *
             * If there are entries there are possibly ports we can use and need
             * to enumerate.
             */

            String[] term = new String[2];
            int l = 0;
            File dev = null;

            dev = new File("/dev/term");
            if (dev.list().length > 0) {
                term[l++] = "term/";
            }
            /*
             * dev = new File( "/dev/cua0" ); if( dev.list().length > 0 )
             * term[l++] = "cua/";
             */
            String[] temp = new String[l];
            for (l--; l >= 0; l--) {
                temp[l] = term[l];
            }
            candidateDeviceNames = temp;
        } else {
            File dev = new File(deviceDirectory);
            String[] temp = dev.list();
            candidateDeviceNames = temp;
        }
        if (candidateDeviceNames == null) {
            LOGGER.log(Level.FINEST, "no device files to check");
            return;
        }

        String candidatePortPrefixes[] = {};
        if (portType == CommPortIdentifier.PORT_SERIAL) {
            LOGGER.log(Level.FINEST, "scanning for serial ports for os {0}",
                    osName);

            /*
             * There are _many_ possible ports that can be used on Linux.
             * See below in the fake Linux-all-ports case for a list. You
             * may add additional ports here but be warned that too many
             * will significantly slow down port enumeration. Linux 2.6 has
             * udev support which should be faster as only ports the kernel
             * finds should be exposed in /dev
             *
             * See also how to override port enumeration and specifying port
             * in INSTALL.
             *
             * taj
             */
            if (osName.equals("Linux")) {
                String[] temp = {
                    "ttyS", // linux Serial Ports
                    "ttySA", // for the IPAQs
                    "ttyUSB", // for USB frobs
                    "rfcomm", // bluetooth serial device
                    "ttyircomm", // linux IrCommdevices (IrDA serial emu)
                };
                candidatePortPrefixes = temp;
            } else if (osName.equals("Linux-all-ports")) {
                /*
                 * if you want to enumerate all ports ~5000 possible, then
                 * replace the above with this
                 */
                String[] temp = {
                    "comx", // linux COMMX synchronous serial card
                    "holter", // custom card for heart monitoring
                    "modem", // linux symbolic link to modem.
                    "rfcomm", // bluetooth serial device
                    "ttyircomm", // linux IrCommdevices (IrDA serial emu)
                    "ttycosa0c", // linux COSA/SRP synchronous serial card
                    "ttycosa1c", // linux COSA/SRP synchronous serial card
                    "ttyACM", // linux CDC ACM devices
                    "ttyC", // linux cyclades cards
                    "ttyCH", // linux Chase Research AT/PCI-Fast serial card
                    "ttyD", // linux Digiboard serial card
                    "ttyE", // linux Stallion serial card
                    "ttyF", // linux Computone IntelliPort serial card
                    "ttyH", // linux Chase serial card
                    "ttyI", // linux virtual modems
                    "ttyL", // linux SDL RISCom serial card
                    "ttyM", // linux PAM Software's multimodem boards
                    // linux ISI serial card
                    "ttyMX", // linux Moxa Smart IO cards
                    "ttyP", // linux Hayes ESP serial card
                    "ttyR", // linux comtrol cards
                    // linux Specialix RIO serial card
                    "ttyS", // linux Serial Ports
                    "ttySI", // linux SmartIO serial card
                    "ttySR", // linux Specialix RIO serial card 257+
                    "ttyT", // linux Technology Concepts serial card
                    "ttyUSB", //linux USB serial converters
                    "ttyV", // linux Comtrol VS-1000 serial controller
                    "ttyW", // linux specialix cards
                    "ttyX" // linux SpecialX serial card
                };
                candidatePortPrefixes = temp;
            } else if (osName.toLowerCase().indexOf("qnx") != -1) {
                String[] temp = {
                    "ser"
                };
                candidatePortPrefixes = temp;
            } else if (osName.equals("Irix")) {
                String[] temp = {
                    "ttyc", // irix raw character devices
                    "ttyd", // irix basic serial ports
                    "ttyf", // irix serial ports with hardware flow
                    "ttym", // irix modems
                    "ttyq", // irix pseudo ttys
                    "tty4d", // irix RS422
                    "tty4f", // irix RS422 with HSKo/HSki
                    "midi", // irix serial midi
                    "us" // irix mapped interface
                };
                candidatePortPrefixes = temp;
            } else if (osName.equals("FreeBSD")) //FIXME this is probably wrong
            {
                String[] temp = {
                    "ttyd", //general purpose serial ports
                    "cuaa", //dialout serial ports
                    "ttyA", //Specialix SI/XIO dialin ports
                    "cuaA", //Specialix SI/XIO dialout ports
                    "ttyD", //Digiboard - 16 dialin ports
                    "cuaD", //Digiboard - 16 dialout ports
                    "ttyE", //Stallion EasyIO (stl) dialin ports
                    "cuaE", //Stallion EasyIO (stl) dialout ports
                    "ttyF", //Stallion Brumby (stli) dialin ports
                    "cuaF", //Stallion Brumby (stli) dialout ports
                    "ttyR", //Rocketport dialin ports
                    "cuaR", //Rocketport dialout ports
                    "stl" //Stallion EasyIO board or Brumby N 
                };
                candidatePortPrefixes = temp;
            } else if (osName.equals("NetBSD")) // FIXME this is probably wrong
            {
                String[] temp = {
                    "tty0" // netbsd serial ports
                };
                candidatePortPrefixes = temp;
            } else if (osName.equals("Solaris")
                    || osName.equals("SunOS")) {
                String[] temp = {
                    "term/",
                    "cua/"
                };
                candidatePortPrefixes = temp;
            } else if (osName.equals("HP-UX")) {
                String[] temp = {
                    "tty0p",// HP-UX serial ports
                    "tty1p" // HP-UX serial ports
                };
                candidatePortPrefixes = temp;
            } else if (osName.equals("UnixWare")
                    || osName.equals("OpenUNIX")) {
                String[] temp = {
                    "tty00s", // UW7/OU8 serial ports
                    "tty01s",
                    "tty02s",
                    "tty03s"
                };
                candidatePortPrefixes = temp;
            } else if (osName.equals("OpenServer")) {
                String[] temp = {
                    "tty1A", // OSR5 serial ports
                    "tty2A",
                    "tty3A",
                    "tty4A",
                    "tty5A",
                    "tty6A",
                    "tty7A",
                    "tty8A",
                    "tty9A",
                    "tty10A",
                    "tty11A",
                    "tty12A",
                    "tty13A",
                    "tty14A",
                    "tty15A",
                    "tty16A",
                    "ttyu1A", // OSR5 USB-serial ports
                    "ttyu2A",
                    "ttyu3A",
                    "ttyu4A",
                    "ttyu5A",
                    "ttyu6A",
                    "ttyu7A",
                    "ttyu8A",
                    "ttyu9A",
                    "ttyu10A",
                    "ttyu11A",
                    "ttyu12A",
                    "ttyu13A",
                    "ttyu14A",
                    "ttyu15A",
                    "ttyu16A"
                };
                candidatePortPrefixes = temp;
            } else if (osName.equals("Compaq's Digital UNIX") || osName.equals("OSF1")) {
                String[] temp = {
                    "tty0" //  Digital Unix serial ports
                };
                candidatePortPrefixes = temp;
            } else if (osName.equals("BeOS")) {
                String[] temp = {
                    "serial" // BeOS serial ports
                };
                candidatePortPrefixes = temp;
            } else if (osName.equals("Mac OS X")) {
                String[] temp = {
                    // Keyspan USA-28X adapter, USB port 1
                    "cu.KeyUSA28X191.",
                    // Keyspan USA-28X adapter, USB port 1
                    "tty.KeyUSA28X191.",
                    // Keyspan USA-28X adapter, USB port 2
                    "cu.KeyUSA28X181.",
                    // Keyspan USA-28X adapter, USB port 2
                    "tty.KeyUSA28X181.",
                    // Keyspan USA-19 adapter
                    "cu.KeyUSA19181.",
                    // Keyspan USA-19 adapter
                    "tty.KeyUSA19181."
                };
                candidatePortPrefixes = temp;
            } else if (osName.toLowerCase().indexOf("windows") != -1) {
                String[] temp = {
                    "COM" // win32 serial ports
                //"//./COM"    // win32 serial ports
                };
                candidatePortPrefixes = temp;
            } else {
                LOGGER.log(Level.FINEST,
                        "No valid prefixes for serial ports have been entered "
                        + "for {0}",
                        osName);
            }
        } else {
            LOGGER.log(Level.FINEST, "Unknown PortType {0} passed", portType);
        }

        registerValidPorts(candidateDeviceNames, candidatePortPrefixes, portType);
    }

    /**
     * @param portName The name of the port the OS recognizes
     * @param portType CommPortIdentifier.PORT_SERIAL or PORT_PARALLEL
     * @return CommPort getCommPort() will be called by CommPortIdentifier from
     * its openPort() method. PortName is a string that was registered earlier
     * using the CommPortIdentifier.addPortName() method. getCommPort() returns
     * an object that extends either SerialPort or ParallelPort.
     */
    public CommPort getCommPort(String portName, int portType) {
        if (portType != CommPortIdentifier.PORT_SERIAL) {
            LOGGER.log(Level.WARNING,
                    "unknown port type {0} passed, "
                    + "this should never happen",
                    portType);
            return null;
        }
        try {
            if (osName.toLowerCase().indexOf("windows") == -1) {
                return new RXTXPort(context, portName);
            } else {
                return new RXTXPort(context, deviceDirectory + portName);
            }
        } catch (PortInUseException e) {
            LOGGER.log(Level.INFO, "Port {0} in use by another application",
                    portName);
        }
        return null;
    }
}
