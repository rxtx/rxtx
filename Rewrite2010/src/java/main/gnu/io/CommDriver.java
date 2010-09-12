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

/**
 * Part of the loadable device driver interface. <code>CommDriver</code> should not be
 * used by application-level programs.
 * <p>
 * <hr>
 * <p>
 * This is perhaps the most confusing part of the original <code>javax.comm</code>
 * API: <i>"CommDriver should not be used by application-level programs"</i> -
 * and yet it is used as an argument to a <code>CommPortIdentifier</code> public method.
 * </p>
 * <p>
 * Not only are application-level programs allowed to use this interface, RXTX
 * <i><b>encourages</b></i> its use. Application developers can implement custom port
 * types by writing their own implementation of this interface, and then passing it as an
 * argument to {@link gnu.io.CommPortIdentifier#addPortName(String, int, CommDriver)}.
 * Example:
 * </p>
 * <blockquote>
 * <pre>
 * import gnu.io.CommDriver;
 * import gnu.io.CommPortIdentifier;
 * 
 * public class AcmeCommDriver implements CommDriver {
 * 
 *     public static final int PORT_ACME = 54321; // Acme port type
 *     private static final AcmeCommDriver instance = new AcmeCommDriver();
 * 
 *     static {
 *         instance.initialize();
 *     }
 * 
 *     public void initialize() {
 *         System.loadLibrary(&quot;AcmeDevice&quot;);
 *         CommPortIdentifier.addPortName(&quot;ACME&quot;, PORT_ACME, this);
 *     }
 * 
 *     public CommPort getCommPort(String portName, int portType) {
 *         // ...
 *     }
 * }
 * </pre>
 * </blockquote>
 * 
 * @author <a href="http://www.rxtx.org">The RXTX Project</a>
 */
public interface CommDriver {

    /**
     * Returns an object that extends <code>CommPort</code> - like
     * {@link gnu.io.SerialPort} or {@link gnu.io.ParallelPort}.
     * <p>
     * <hr>
     * <p>
     * The original <code>javax.comm</code> specification made no provision for ports that
     * have been disconnected - like virtual ports. The RXTX implementation will throw
     * <code>IOException</code> if the port has been disconnected.
     * </p>
     * 
     * @param portName The port name.
     * @param portType The port type.
     * 
     * @return An object that extends <code>CommPort</code>.
     * @throws IOException If the port has been disconnected
     */
    CommPort getCommPort(String portName, int portType);

    /**
     * Called by the <code>CommPortIdentifier</code> static initializer. The
     * responsibility of this method is: 1) Ensure that that the hardware is present. 2)
     * Load any required native libraries. 3) Register the port names with the
     * <code>CommPortIdentifier</code>.
     */
    void initialize();
}
