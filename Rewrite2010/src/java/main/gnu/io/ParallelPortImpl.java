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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;

/**
 * A <code>ParallelPort</code> implementation.
 * 
 * <dl>
 * <dt><b>Thread-safe:</b></dt>
 * <dd>This class may be used in multi-threaded applications.</dd>
 * </dl>
 * @author <a href="http://www.rxtx.org">The RXTX Project</a>
 */
public final class ParallelPortImpl extends ParallelPort {
    private final static Dispatcher dispatch = Dispatcher.getInstance();

    private final ParallelPortEventHandler eventHandler;

    public ParallelPortImpl(CommPortIdentifier cpi, int portHandle) {
        super(cpi, portHandle);
        this.eventHandler = new ParallelPortEventHandler(this);
    }

    protected synchronized void abort() {
        this.eventHandler.removeEventListener();
        super.abort();
    }

    public synchronized void addEventListener(ParallelPortEventListener listener) throws TooManyListenersException {
        checkStatus();
        this.eventHandler.addEventListener(listener);
    }

    public synchronized void close() {
        this.eventHandler.removeEventListener();
        super.close();
    }

    public synchronized void disableReceiveFraming() {
        checkStatus();
        this.receiveFramingEnabled = false;
    }

    public synchronized void disableReceiveThreshold() {
        checkStatus();
        this.receiveThresholdEnabled = false;
    }

    public synchronized void disableReceiveTimeout() {
        checkStatus();
        this.receiveTimeoutEnabled = false;
    }

    public synchronized void enableReceiveFraming(int framingByte) throws UnsupportedCommOperationException {
        checkStatus();
        this.receiveFramingEnabled = true;
        this.receiveFramingByte = framingByte;
    }

    public synchronized void enableReceiveThreshold(int threshold) throws UnsupportedCommOperationException {
        checkStatus();
        this.receiveThresholdEnabled = true;
        this.receiveThreshold = threshold;
    }

    public synchronized void enableReceiveTimeout(int receiveTimeout) throws UnsupportedCommOperationException {
        checkStatus();
        this.receiveTimeoutEnabled = true;
        this.receiveTimeout = receiveTimeout;
    }

    public synchronized int getInputBufferSize() {
        checkStatus();
        try {
            return dispatch.getInputBufferSize(this.portHandle);
        } catch (IOException e) {
            abort(e);
            return 0;
        }
    }

    public synchronized InputStream getInputStream() throws IOException {
        checkStatus();
        if (this.inputStream == null) {
            if (!dispatch.isPortReadable(this.portHandle)) {
                return null;
            }
            this.inputStream = new PortInputStream(this);
        }
        return this.inputStream;
    }

    public synchronized int getMode() {
        checkStatus();
        try {
            return dispatch.getMode(this.portHandle);
        } catch (IOException e) {
            abort(e);
            return 0;
        }
    }

    public synchronized int getOutputBufferFree() {
        checkStatus();
        try {
            return dispatch.getOutputBufferFree(this.portHandle);
        } catch (IOException e) {
            abort(e);
            return 0;
        }
    }

    public synchronized int getOutputBufferSize() {
        checkStatus();
        try {
            return dispatch.getOutputBufferSize(this.portHandle);
        } catch (IOException e) {
            abort(e);
            return 0;
        }
    }

    public synchronized OutputStream getOutputStream() throws IOException {
        checkStatus();
        if (this.outputStream == null) {
            if (!dispatch.isPortWritable(this.portHandle)) {
                return null;
            }
            this.outputStream = new PortOutputStream(this);
        }
        return this.outputStream;
    }

    public synchronized int getReceiveFramingByte() {
        checkStatus();
        return this.receiveFramingByte;
    }

    public synchronized int getReceiveThreshold() {
        checkStatus();
        return this.receiveThreshold;
    }

    public synchronized int getReceiveTimeout() {
        checkStatus();
        return this.receiveTimeout;
    }

    public synchronized boolean isPaperOut() {
        checkStatus();
        try {
            return dispatch.isPaperOut(this.portHandle);
        } catch (IOException e) {
            abort(e);
            return false;
        }
    }

    /**
     * Returns <code>true</code> if the output buffer is empty.
     * @return <code>true</code> if the output buffer is empty.
     */
    public synchronized boolean isOutputBufferEmpty() {
        checkStatus();
        try {
            return dispatch.isOutputBufferEmpty(this.portHandle);
        } catch (IOException e) {
            abort(e);
            return false;
        }
    }

    public synchronized boolean isPrinterBusy() {
        checkStatus();
        try {
            return dispatch.isPrinterBusy(this.portHandle);
        } catch (IOException e) {
            abort(e);
            return false;
        }
    }

    public synchronized boolean isPrinterError() {
        checkStatus();
        try {
            return dispatch.isPrinterError(this.portHandle);
        } catch (IOException e) {
            abort(e);
            return false;
        }
    }

    public synchronized boolean isPrinterSelected() {
        checkStatus();
        try {
            return dispatch.isPrinterSelected(this.portHandle);
        } catch (IOException e) {
            abort(e);
            return false;
        }
    }

    public synchronized boolean isPrinterTimedOut() {
        checkStatus();
        try {
            return dispatch.isPrinterTimedOut(this.portHandle);
        } catch (IOException e) {
            abort(e);
            return false;
        }
    }

    public synchronized boolean isReceiveFramingEnabled() {
        checkStatus();
        return this.receiveFramingEnabled;
    }

    public synchronized boolean isReceiveThresholdEnabled() {
        checkStatus();
        return this.receiveThresholdEnabled;
    }

    public synchronized boolean isReceiveTimeoutEnabled() {
        checkStatus();
        return this.receiveTimeoutEnabled;
    }

    public synchronized void notifyOnBuffer(boolean enable) {
        checkStatus();
        this.eventHandler.notifyOnBuffer(enable);
    }

    public synchronized void notifyOnError(boolean enable) {
        checkStatus();
        this.eventHandler.notifyOnError(enable);
    }

    public synchronized void removeEventListener() {
        checkStatus();
        this.eventHandler.removeEventListener();
    }

    public synchronized void restart() {
        checkStatus();
        try {
            dispatch.restart(this.portHandle);
        } catch (IOException e) {
            abort(e);
        }
    }

    public synchronized void setInputBufferSize(int size) {
        checkStatus();
        try {
            dispatch.setInputBufferSize(this.portHandle, size);
        } catch (IOException e) {
            abort(e);
        }
    }

    public synchronized int setMode(int mode) throws UnsupportedCommOperationException {
        checkStatus();
        try {
            return dispatch.setMode(this.portHandle, mode);
        } catch (IOException e) {
            abort(e);
            return 0;
        }
    }

    public synchronized void setOutputBufferSize(int size) {
        checkStatus();
        try {
            dispatch.setOutputBufferSize(this.portHandle, size);
        } catch (IOException e) {
            abort(e);
        }
    }

    public synchronized void suspend() {
        checkStatus();
        try {
            dispatch.suspend(this.portHandle);
        } catch (IOException e) {
            abort(e);
        }
    }
}
