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

/**
 * The main application object. This object serves as the execution
 * path point-of-entry.
 *
 */
public class Start {
    private static final String source = Start.class.getName();
    private static final Dispatcher dispatch = Dispatcher.getInstance();

    public static void main(String[] args) {
        Log.log("RXTX Java code version is " + Settings.RXTX_JAVA_VERSION, source);
        Log.log("RXTX native code version is " + dispatch.version(), source);
        SerialPort port = null;
        try {
            CommPortIdentifier cpi = CommPortIdentifier.getPortIdentifier("COM4");
            port = (SerialPort) cpi.open("Start", 1000);
            port.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN);
            port.setSerialPortParams(512000, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            port.setInputBufferSize(1024);
            port.setOutputBufferSize(4096);
            InputStream in = port.getInputStream();
            OutputStream out = port.getOutputStream();
            byte[] data = new byte[2048];
            int bytesRead = 0;
            int bytesWritten = 0;
            // Bandwidth test
            Log.log("Starting bandwidth test... ", source);
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 512; i++) {
                out.write(data);
                out.flush();
                bytesWritten += data.length;
                int result = in.read(data);
                bytesRead += result;
            }
            long runTime = System.currentTimeMillis() - startTime;
            Float et = new Float((float)runTime/1000);
            Float rate = new Float(1 / et.floatValue());
            Log.log("Transfer rate: " + rate + " MB/s, ET = " + et, source);
            port.close();
            // InputStream/OutputStream test
            Log.log("Starting data integrity test... ", source);
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) (i & 0x000F);
            }
            port = (SerialPort) cpi.open("Start", 1000);
            port.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN);
            in = port.getInputStream();
            out = port.getOutputStream();
            byte[] readData = new byte[data.length];
            bytesWritten = 0;
            out.write(data);
            out.flush();
            bytesRead = in.read(readData);
            boolean errorsFound = false;
            if (bytesRead != data.length) {
                Log.log("Error: number of bytes read not equal to number of bytes written", source);
            }
            for (int i = 0; i < data.length; i++) {
                if (readData[i] != (byte) (i & 0x000F)) {
                    Log.log("Data mismatch at byte " + i + ", wrote " +
                            (i & 0x000F) + ", read " + readData[i], source);
                    errorsFound = true;
                    break;
                }
            }
            if (!errorsFound) {
                Log.log("Data integrity test passed", source);
            }
        } catch (Exception e) {
            Log.log(e, "Exception thrown while testing port: ", source);
        } finally {
            if (port != null) {
                port.close();
            }
        }
        ParallelPort parPort = null;
        try {
            Log.log("Testing parallel port", source);
            CommPortIdentifier cpi = CommPortIdentifier.getPortIdentifier("LPT1");
            parPort = (ParallelPort) cpi.open("Start", 1000);
            parPort.setInputBufferSize(1024);
            parPort.setOutputBufferSize(4096);
            Log.log("Printer selected = " + parPort.isPrinterSelected(), source);
        } catch (Exception e) {
            Log.log(e, "Exception thrown while testing port: ", source);
        } finally {
            if (parPort != null) {
                parPort.close();
            }
        }
        System.exit(0); // Required to shut down threads
    }
}
