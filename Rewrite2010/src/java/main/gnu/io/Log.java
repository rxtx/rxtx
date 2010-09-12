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

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A simple logger. Messages are sent to the console only. Logging can be
 * disabled by setting the <code>gnu.io.logging.disabled</code> property
 * to true.
 * <dl>
 * <dt><b>Thread-safe:</b></dt>
 * <dd>This class may be used in multi-threaded applications.</dd>
 * </dl>
 * 
 * @author <a href="http://www.rxtx.org">The RXTX Project</a>
 */
public final class Log {
    private static boolean enabled = !Settings.getBoolean("gnu.io.logging.disabled");

    private static String buildMessage(String message, String source) {
        StringBuffer sb = new StringBuffer();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm.ss");
        sb.append(df.format(new Date()));
        sb.append(" [");
        sb.append(Thread.currentThread().getName());
        sb.append("] ");
        sb.append(source);
        sb.append(" --> ");
        sb.append(message);
        return sb.toString();
    }

    /**
     * Logs a message to the console.
     * @param message The message to log.
     * @param source The source of the message - a Java class or
     * a native file name.
     */
    public static void log(String message, String source) {
        if (enabled) {
            synchronized (Log.class) {
                System.out.println(buildMessage(message, source));
            }
        }
    }

    /**
     * Logs a message to the console and prints a stack trace.
     * @param t The exception containing the stack trace.
     * @param message The message to log.
     * @param source The source of the message - a Java class or
     * a native file name.
     */
    public static void log(Throwable t, String message, String source) {
        if (enabled) {
            synchronized (Log.class) {
                System.out.println(buildMessage(message, source));
                t.printStackTrace();
            }
        }
    }

    private Log() {
    }
}
