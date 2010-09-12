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

import java.util.EventListener;

/**
 * Propagates various communications port ownership events. When a port is opened, a
 * <CODE>CommPortOwnership</CODE> event of type <CODE>PORT_OWNED</CODE> will be
 * propagated. When a port is closed, a <CODE>CommPortOwnership</CODE> event of type
 * <CODE>PORT_UNOWNED</CODE> will be propagated.
 * <P>
 * Multiple applications that are seeking ownership of a communications port can resolve
 * their differences as follows:
 * <UL>
 * <LI>ABCapp calls <CODE>open</CODE> and takes ownership of port.</li>
 * <LI>XYZapp calls <CODE>open</CODE> sometime later.</li>
 * <LI>While processing XYZapp's <CODE>open</CODE>, <CODE>CommPortIdentifier</CODE> will
 * propagate a <CODE>CommPortOwnership</CODE> event with the event type
 * <CODE>PORT_OWNERSHIP_REQUESTED</CODE>.</li>
 * <LI>If ABCapp is registered to listen to these events and if it is willing to give up
 * ownership, it calls <CODE>close</CODE> from within the event callback.</li>
 * <LI>After the event has been fired, <CODE>CommPortIdentifier</CODE> checks to see if
 * ownership was given up, and if so, turns over ownership of the port to XYZapp by
 * returning success from <CODE>open</CODE>.</li>
 * </UL>
 * 
 * Note: When a <CODE>close</CODE> is called from within a <CODE>CommPortOwnership</CODE>
 * event callback, a new <CODE>CommPortOwnership</CODE> event will <I>not</I> be
 * generated.
 * <P>
 * 
 * @author <a href="http://www.rxtx.org">The RXTX Project</a>
 */
public interface CommPortOwnershipListener extends EventListener {
    /**
     * The port just went from unowned to owned state, when an application successfully
     * called <CODE>CommPortIdentifier.open</CODE>.
     */
    public static final int PORT_OWNED = 1;
    /**
     * The port just went from owned to unowned state, when the port's owner called
     * <CODE>CommPort.close</CODE>.
     */
    public static final int PORT_UNOWNED = 2;
    /**
     * Ownership contention. The port is owned by one application and another application
     * wants ownership. If the owner of this port is listening to this event, it can call
     * <CODE>CommPort.close</CODE> during the processing of this event and thereby give up
     * ownership of the port.
     */
    public static final int PORT_OWNERSHIP_REQUESTED = 3;

    /**
     * Propagates a <CODE>CommPortOwnership</CODE> event. This method will be called with
     * the type set to one of the variables <CODE>PORT_OWNED</CODE>,
     * <CODE>PORT_UNOWNED</CODE>, or <CODE>PORT_OWNERSHIP_REQUESTED</CODE>.
     * 
     * @param type
     */
    void ownershipChange(int type);
}
