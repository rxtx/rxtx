/*-------------------------------------------------------------------------
|   A wrapper to convert RXTX into Linux Java Comm
|   Copyright 1998 Kevin Hester, kevinh@acm.org
|   Copyright 2000 Trent Jarvi, TrentJarvi@yahoo.com
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
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

/**
* @author Kevin Hester
* @author Trent Jarvi
* @version %I%, %G%
* @since JDK1.0
*/

public class RXTXCommDriver implements CommDriver
{

	private static boolean debug = false;

	static
	{
		if(debug ) System.out.println("RXTXCommDriver {}");
		System.loadLibrary( "Serial" );
	}

	/** Get the Serial port prefixes for the running OS */
	private native boolean isDeviceGood(String dev);

	/*  create a vector of know ports of some type (RS232, RS485, ...) */

	private final Vector getPortPrefixes(String AllKnownPorts[])
	{

		if (debug)
			System.out.println("RXTXCommDriver:getPortPrefixes()");

		Vector v = new Vector();

		for(int j=0;j<AllKnownPorts.length;j++)
		{
			if(isDeviceGood(AllKnownPorts[j]))
			{
				v.addElement(AllKnownPorts[j]);
			}
		}
		return(v);
	}

	/*  If the port is readable and writeable add it to CommPortIdentifier
	 */

	private void addIfPortGood(String pName, int pType)
	{
		String portName = "/dev/" + pName;
		File port = new File( portName );

		if( port.canRead())
		{
			if( port.canWrite() )
			{
				CommPortIdentifier.addPortName( portName, pType, this );
			}
		}
	}

	/*  v is a Vector of requested ports with out the #'s.  ie ttyS
	 *  d is a Vector of all devices on the  system.  ie  ttyS0
	 *  if  the item in d starts with the item in v request that the
	 *  port be added to CommPortIdentifier if its readable and writable.
	 */
	private void RegisterValidPorts(
		Vector d,
		Vector v,
		int portType
	)
	{
		String s,t,portName;
		Enumeration req, exhist;

		if (debug)
			System.out.println(
				"RXTXCommDriver:RegisterValidPorts()");

		long ms = System.currentTimeMillis();
		for (req = v.elements() ; req.hasMoreElements() ;)
		{
			s=(String) req.nextElement();
			for( exhist = d.elements(); exhist.hasMoreElements(); )
			{
				t = (String) exhist.nextElement();
				if( t.startsWith( s ))
				{
					addIfPortGood( t, portType);
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
    /*
	See SerialImp.c's *KnownPorts[] when adding ports
    */
	public void initialize()
	{

		if (debug) System.out.println("RXTXCommDriver:initialize()");
		
		File dev = new File( "/dev" );
		String[] list = dev.list();
		Vector devs = new Vector();

		/*  fill a vector with all the devices files in /dev */
		int j=0;while(j<list.length) devs.addElement(list[j++]);


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
			"ttyE", // linux Stallion serial card
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

			"ttyc", // irix raw character devices
			"ttyd", // irix basic serial ports
			"ttyf", // irix serial ports with hardware flow
			"ttym", // irix modems
			"ttyq", // irix pseudo ttys
			"tty4d",// irix RS422
			"tty4f",// irix RS422 with HSKo/HSki
			"midi", // irix serial midi
			"us",   // irix mapped interface

			"cuaa", // FreeBSD Serial Ports

			"tty0", // netbsd serial ports

			"tty0p",// HP-UX serial ports
			"tty1p",// HP-UX serial ports

			"serial",// BeOS serial ports

			"COM"    // win32 serial ports
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
		String[] AllKnownRAWPorts={};
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
		RegisterValidPorts(
			devs,
			getPortPrefixes(AllKnownRAWPorts),
			CommPortIdentifier.PORT_RAW
		);
	}


	/*
	 * getCommPort() will be called by CommPortIdentifier from its
	 * openPort() method. portName is a string that was registered earlier
	 * using the CommPortIdentifier.addPortName() method. getCommPort()
	 * returns an object that extends either SerialPort or ParallelPort.
	 *
	 * <p>From the NullDriver.java CommAPI sample.
	 */
	public CommPort getCommPort( String portName, int portType )
	{
		if (debug) System.out.println("RXTXCommDriver:getCommPort("
			+portName+","+portType+")");
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
			else if (portType==CommPortIdentifier.PORT_RAW)
			{
				return new Raw( portName );
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
