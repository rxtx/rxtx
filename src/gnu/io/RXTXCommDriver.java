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
package javax.comm;
import java.io.*;

/**
   This is the JavaComm for Linux driver.  
*/
public class RXTXCommDriver implements CommDriver {


	static String OS;
	private static boolean debug = true;

        static 
	{ 
		if(debug ) System.out.println("RXTXCommDriver {}");
		OS = System.getProperty("os.name");
		if(OS.equals("Win95"))
		{
			System.loadLibrary("SerialW95");
		}
		else 
		{
			System.loadLibrary( "Serial" ); 
		}
	}

	/** Get the Serial port prefixes for the running OS */
	private native boolean IsDeviceGood(String dev);
	private final String[] getPortPrefixes(String AllKnownPorts[]) {
		int i=0;
		String PortString[]=new String [256];

		if (debug) System.out.println("RXTXCommDriver:getPortPrefixes()");
		for(int j=0;j<AllKnownPorts.length;j++){
			if(IsDeviceGood(AllKnownPorts[j])) {
				PortString[i++]=AllKnownPorts[j];
			}
		}
		String PortString2[] =new String[i];
		for(int j=0;j<i;j++){
			PortString2[j]=PortString[j];
		}
		return PortString2;
	}

	private void RegisterValidPorts(
		String devs[],
		String Prefix[],
		int PortType
	) {
		if (debug) System.out.println("RXTXCommDriver:RegisterValidPorts()");
		for( int i = 0; i < devs.length; i++ ) {
			for( int p = 0; p < Prefix.length; p++ ) {
				if( devs[ i ].startsWith( Prefix[ p ] ) ) {
					String portName = "/dev/" + devs[ i ];
					File port = new File( portName );
					if( port.canRead() && port.canWrite() ) 
					 CommPortIdentifier.addPortName( 
							portName,
							PortType,
							this 
						);
				}
			}
		}
	}

   /*
    * initialize() will be called by the CommPortIdentifier's static
    * initializer. The responsibility of this method is:
    * 1) Ensure that that the hardware is present.
    * 2) Load any required native libraries.
    * 3) Register the port names with the CommPortIdentifier.
	 * 
	 * <p>From the NullDriver.java CommAPI sample.
         *
         * added printerport stuff
         * Holger Lehmann
         * July 12, 1999
         * IBM

         * Added ttyM for Moxa boards
         * Removed obsolete device cuaa
         * Peter Bennett
         * January 02, 2000
         * Bencom

    */
	public void initialize() {
		File dev = new File( "/dev" );
		String[] devs = dev.list();
		if (debug) System.out.println("RXTXCommDriver:initialize()");
		String[] AllKnownSerialPorts={
			"comx",      // linux COMMX synchronous serial card
			"holter",    // custom card for heart monitoring
			"modem",     // linux symbolic link to modem.
			"ttyircomm", // linux IrCommdevices (IrDA serial emu)
			"ttycosa0c", // linux COSA/SRP synchronous serial card
			"ttycosa1c", // linux COSA/SRP synchronous serial card
			"ttyC", // linux cyclades cards
			"ttyCH",// linux Chase Research AT/PCI-Fast serial card
			"ttyD", // linux Digiboard serial card
			"ttuE", // linux Stallion serial card
			"ttyF", // linux Computone IntelliPort serial card
			"ttyH", // linux Chase serial card
			"ttyI", // linux virtual modems
			"ttyL", // linux SDL RISCom serial card
			"ttyM", // linux PAM Software's multimodem boards
			        // linux ISI serial card
			"ttyMX",// linux Moxa Smart IO cards
			"ttyP", // linux Hayes ESP serial card
			"ttyR", // linux comtrol cards
			        // linux Specialix RIO serial card
			"ttyS", // linux Serial Ports
			"ttySI",// linux SmartIO serial card
			"ttySR",// linux Specialix RIO serial card 257+
			"ttyT", // linux Technology Concepts serial card
			"ttyUSB",//linux USB serial converters
			"ttyV", // linux Comtrol VS-1000 serial controller
			"ttyW", // linux specialix cards
			"ttyX", // linux SpecialX serial card
			"ttyf", // irix serial ports with hardware flow
			"ttym", // irix modems
			"ttyq", // irix pseudo ttys
			"ttyd", // irix serial ports
			"cuaa", // FreeBSD Serial Ports
			"tty0" // netbsd serial ports 
		};
	/** Get the Parallel port prefixes for the running os
	* Holger Lehmann
	* July 12, 1999
	* IBM
	*/
		String[] AllKnownParallelPorts={
			"lp"    // linux printer port
		};
		String[] AllKnownRS485Ports={};
		String[] AllKnownI2CPorts={};
		RegisterValidPorts(
			devs,
			getPortPrefixes(AllKnownSerialPorts),
			CommPortIdentifier.PORT_SERIAL
		);
		RegisterValidPorts(
			devs,
			getPortPrefixes(AllKnownParallelPorts),
			CommPortIdentifier.PORT_PARALLEL
		);
		RegisterValidPorts(
			devs,
			getPortPrefixes(AllKnownRS485Ports),
			CommPortIdentifier.PORT_RS485
		);
		RegisterValidPorts(
			devs,
			getPortPrefixes(AllKnownI2CPorts),
			CommPortIdentifier.PORT_I2C
		);
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
		if (debug) System.out.println("RXTXCommDriver:getCommPort("+portName+","+portType+")");
		try {
			if (portType==CommPortIdentifier.PORT_SERIAL)
			{
				return new RXTXPort( portName ); 
			}
			else if (portType==CommPortIdentifier.PORT_PARALLEL)
			{
				return new LPRPort( portName ); 
			}
			else if (portType==CommPortIdentifier.PORT_I2C)
			{
				return new I2C( portName ); 
			}
			else if (portType==CommPortIdentifier.PORT_RS485)
			{
				return new RS485( portName ); 
			}
		} catch( IOException e ) {
			e.printStackTrace();
		}
		return null;
	}
}
