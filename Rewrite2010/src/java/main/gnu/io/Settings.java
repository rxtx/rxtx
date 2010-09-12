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
package gnu.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * RXTX settings utility class.
 * <p>
 * Settings are stored in a <code>gnu.io.rxtx.properties</code> file. The file can be
 * located anywhere on the classpath. Applications can include the file in their jar file.
 * </p>
 * 
 * <dl>
 * <dt><b>Thread-safe:</b></dt>
 * <dd>This class may be used in multi-threaded applications.</dd>
 * </dl>
 * 
 * @author <a href="http://www.rxtx.org">The RXTX Project</a>
 */
public final class Settings {
    private static final String source = Settings.class.getName();
    private static final Properties props = loadProperties();
    /**
     * The current RXTX Java code version.
     */
    public static final String RXTX_JAVA_VERSION = "RXTX-Rewrite-Alpha";

    /**
     * Returns a setting as a <code>boolean</code> value. Defaults
     * to <code>false</code>.
     * 
     * @param key The setting key.
     * @return The setting as a <code>boolean</code> value.
     */
    public static boolean getBoolean(String key) {
        String result = props.getProperty(key);
        return result == null ? false : "true".equals(result);
    }

    /**
     * Returns a setting as an <code>int</code> value. Defaults
     * to zero.
     * 
     * @param key The setting key.
     * @return The setting as an <code>int</code> value.
     */
    public static int getInt(String key) {
        String result = props.getProperty(key);
        return result == null ? 0 : Integer.valueOf(result).intValue();
    }

    /**
     * Returns a setting as an <code>int</code> value. If the setting
     * doesn't exist, returns <code>defaultValue</code>.
     * 
     * @param key The setting key.
     * @param defaultValue The default value.
     * @return The setting as an <code>int</code> value.
     */
    public static int getInt(String key, int defaultValue) {
        String result = props.getProperty(key);
        return result == null ? defaultValue : Integer.valueOf(result).intValue();
    }

    /**
     * Returns a setting as a <code>long</code> value. Defaults
     * to zero.
     * 
     * @param key The setting key.
     * @return The setting as a <code>long</code> value.
     */
    public static long getLong(String key) {
        String result = props.getProperty(key);
        return result == null ? 0 : Long.valueOf(result).longValue();
    }

    /**
     * Returns a setting as a <code>long</code> value. If the setting
     * doesn't exist, returns <code>defaultValue</code>.
     * 
     * @param key The setting key.
     * @param defaultValue The default value.
     * @return The setting as a <code>long</code> value.
     */
    public static long getLong(String key, long defaultValue) {
        String result = props.getProperty(key);
        return result == null ? defaultValue : Long.valueOf(result).longValue();
    }

    /**
     * Returns a setting as a <code>String</code> value. Defaults
     * to <code>null</code>.
     * 
     * @param key The setting key.
     * @return The setting as a <code>String</code> value.
     */
    public static String getString(String key) {
        return props.getProperty(key);
    }

    /**
     * Returns a setting as a <code>String</code> value. If the setting
     * doesn't exist, returns <code>defaultValue</code>.
     * 
     * @param key The setting key.
     * @param defaultValue The default value.
     * @return The setting as a <code>String</code> value.
     */
    public static String getString(String key, String defaultValue) {
        String result = props.getProperty(key);
        return result == null ? defaultValue : result;
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        String fileLocation = null;
        // Old style: properties must be in JRE folder
        String extensionDirs = System.getProperty("java.ext.dirs");
        String[] dirArray = extensionDirs.split(System.getProperty("path.separator"));
        for (int i = 0; i < dirArray.length; i++) {
            String fileName = dirArray[i] + System.getProperty("file.separator") + "gnu.io.rxtx.properties";
            File file = new File(fileName);
            if (file.exists()) {
                fileLocation = fileName;
                break;
            }
        }
        InputStream inputStream = null;
        try {
            if (fileLocation != null) {
                inputStream = new FileInputStream(fileLocation);
            } else {
                // New style: properties anywhere in classpath
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                inputStream = loader.getResourceAsStream("gnu.io.rxtx.properties");
            }
            if (inputStream != null) {
                props.load(inputStream);
            }
        } catch (IOException e) {
            Log.log(e, "I/O error while reading gnu.io.rxtx.properties file: ", source);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {}
            }
        }
        return props;
    }

    private Settings() {}
}
