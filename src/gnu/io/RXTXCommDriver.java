/*-------------------------------------------------------------------------
|   A wrapper to convert RXTX into Linux Java Comm
|   Copyright 1998 Kevin Hester, kevinh@acm.org
|
|   This library is free software; you can redistribute it and/or
|   modify it under the terms of the GNU Library General Public
|   License as published by the Free Software Foundation; either
|   version 2 of the License, or (at your option) any later version.
|
|   This library is distributed in the hope that it will be useful,
|   but WITHOUT ANY WARRANTY; without even the implied warranty of
|   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
|   Library General Public License for more details.
|
|   You should have received a copy of the GNU Library General Public
|   License along with this library; if not, write to the Free
|   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
--------------------------------------------------------------------------*/
package gnu.io;

import java.io.*;
import javax.comm.*;

/**
   This is the JavaComm for Linux driver.  
*/
public class RXTXCommDriver implements CommDriver {


	/** Serial port prefixes to check for */
	private static final String[] portPrefix = {
		"modem",	// modem ports
		"ttyS",	// standard serial ports
		"ttyd",	// irix serial ports
		"ttyf",	// irix serial ports with hardware flow
		"ttym",	// irix modems
		"ttyq",	// irix pseudo ttys
		"ttyW",	// specialix cards
		"ttyC"	// cyclades cards
	};


   /*
    * initialize() will be called by the CommPortIdentifier's static
    * initializer. The responsibility of this method is:
    * 1) Ensure that that the hardware is present.
    * 2) Load any required native libraries.
    * 3) Register the port names with the CommPortIdentifier.
	 * 
	 * <p>From the NullDriver.java CommAPI sample.
    */
	public void initialize() {
		File dev = new File( "/dev" );
		String[] devs = dev.list();
		for( int i = 0; i < devs.length; i++ ) {
			for( int p = 0; p < portPrefix.length; p++ ) {
				if( devs[ i ].startsWith( portPrefix[ p ] ) ) {
					String portName = "/dev/" + devs[ i ];
					File port = new File( portName );
					if( port.canRead() && port.canWrite() ) 
						CommPortIdentifier.addPortName( portName,
							CommPortIdentifier.PORT_SERIAL, this );
				}
			}
		}
	}


	/*
	 * getCommPort() will be called by CommPortIdentifier from its openPort()
	 * method. portName is a string that was registered earlier using the
	 * CommPortIdentifier.addPortName() method. getCommPort() returns an
	 * object that extends either SerialPort or ParallelPort.
	 *
	 * <p>From the NullDriver.java CommAPI sample.
	 */
	public CommPort getCommPort( String portName, int portType ) {
		try { return new RXTXPort( portName ); }
		catch( IOException e ) {
			e.printStackTrace();
			return null;
		}
	}
}
