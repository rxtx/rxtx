/*-------------------------------------------------------------------------
|   A wrapper to convert RXTX into Linux Java Comm
|   Copyright 1998 Kevin Hester, kevinh@acm.org
|   Copyright 2000-2001 Trent Jarvi, trentjarvi@yahoo.com
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

/* Martin Pool <mbp@linuxcare.com> added support for explicitly-specified
 * lists of ports, October 2000. */
/* Joseph Goldstone <joseph@lp.com> reorganized to support registered ports,
 * known ports, and scanned ports, July 2001 */

package javax.comm;

import java.io.*;
import java.util.*;
import javax.comm.*;
import java.util.StringTokenizer;

/**
   This is the JavaComm for Linux driver.
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
	private String deviceDirectory;
	private String osName;
`	private native boolean registerKnownPorts(int PortType);
	private native boolean isPortPrefixValid(String dev);
	private native boolean testRead(String dev, int type);
	private native String getDeviceDirectory();

	private final String[] getValidPortPrefixes(String CandidatePortPrefixes[])
	{
		/*
		256 is the number of prefixes ( COM, cua, ttyS, ...) not
		the number of devices (ttyS0, ttyS1, ttyS2, ...)

		On a Linux system there are about 400 prefixes in
		deviceDirectory.
		registerScannedPorts() assigns CandidatePortPrefixes to
		something less than 50 prefixes.

		Trent
		*/

		String ValidPortPrefixes[]=new String [256];
		if(CandidatePortPrefixes==null)
		{
			if (debug)
				System.out.println("\nRXTXCommDriver:getValidPortPrefixes() No ports prefixes known for this System.\nPlease check the port prefixes listed for " + osName + " in RXTXCommDriver:registerScannedPorts()\n");
		}
		int i=0;
		for(int j=0;j<CandidatePortPrefixes.length;j++){
			if(isPortPrefixValid(CandidatePortPrefixes[j])) {
				ValidPortPrefixes[i++]=
					new String(CandidatePortPrefixes[j]);
			}
		}
		String[] returnArray=new String[i];
		System.arraycopy(ValidPortPrefixes, 0, returnArray, 0, i);
		if(ValidPortPrefixes[0]==null)
		{
			if (debug)
			{
				System.out.println("\nRXTXCommDriver:getValidPortPrefixes() No ports matched the list assumed for this\nSystem in the directory " + deviceDirectory + ".  Please check the ports listed for \"" + osName + "\" in\nRXTXCommDriver:registerScannedPorts()\nTried:");
				for(int j=0;j<CandidatePortPrefixes.length;j++){
					System.out.println("\t" +
						CandidatePortPrefixes[i]);
				}
			}
		}
		else
		{
			if (debug)
				System.out.println("\nRXTXCommDriver:getValidPortPrefixes()\nThe following port prefixes have been identified as valid on " + osName + ":\n");
			for(int j=0;j<returnArray.length;j++)
			{
				if (debug)
					System.out.println("\t" + j + " " +
						returnArray[j]);
			}
		}
		return returnArray;
	}

	private void registerValidPorts(
		String CandidateDeviceNames[],
		String ValidPortPrefixes[],
		int PortType
	) {
		int p =0 ;
		int i =0;
		if (debug)
		{
			System.out.println("Entering registerValidPorts()");
	/*
			System.out.println(" Candidate devices:");
			for (int dn=0;dn<CandidateDeviceNames.length;dn++)
				System.out.println("  "	+
					CandidateDeviceNames[dn]);
			System.out.println(" valid port prefixes:");
			for (int pp=0;pp<ValidPortPrefixes.length;pp++)
				System.out.println("  "+ValidPortPrefixes[pp]);
	*/
		}
		if ( CandidateDeviceNames!=null && ValidPortPrefixes!=null)
		{
			for( i = 0;i<CandidateDeviceNames.length; i++ ) {
				for( p = 0;p<ValidPortPrefixes.length; p++ ) {
					String PortName = new String(deviceDirectory + CandidateDeviceNames[ i ]);
					if( CandidateDeviceNames[ i ].startsWith( ValidPortPrefixes[ p ] ) ) {
						if (debug)
							System.out.println("testing read on device "+CandidateDeviceNames[ i ]);
						if (testRead(PortName, PortType))
						{
							CommPortIdentifier.addPortName(
								PortName,
								PortType,
								this
							);
						}
					}
				}
			}
		}
		if (debug)
			System.out.println("Leaving registerValidPorts()");
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

	/**
	*  Determine the OS and where the OS has the devices located
	*/
	public void initialize()
	{

		if (debug) System.out.println("RXTXCommDriver:initialize()");

		osName=System.getProperty("os.name");
		deviceDirectory=getDeviceDirectory();

	/*
	 First try to register ports specified in the properties
	 file.  If that doesn't exist, then scan for ports.
	*/
		for (int PortType=CommPortIdentifier.PORT_SERIAL;PortType<=CommPortIdentifier.PORT_RAW;PortType++) {
			if (!registerSpecifiedPorts(PortType)) {
				if (!registerKnownPorts(PortType)) {
					registerScannedPorts(PortType);
				}
			}
		}
	}

	private void addSpecifiedPorts(String names, int PortType)
	{
		final String pathSep = System.getProperty("path.separator", ":");
		final StringTokenizer tok = new StringTokenizer(names, pathSep);

		while (tok.hasMoreElements())
		{
			String PortName = tok.nextToken();

			if (testRead(PortName, PortType))
				CommPortIdentifier.addPortName(PortName,
					PortType, this);
		}
	}

   /*
    * Register ports specified by the gnu.io.rxtx.SerialPorts and
    * gnu.io.rxtx.ParallelPorts system properties.
    */
	private boolean registerSpecifiedPorts(int PortType)
	{
		String val = null;
		/* FIXME */
		if (debug)
			System.out.println("checking for system-known ports of type "+PortType);
		if (debug)
			System.out.println("checking registry for ports of type "+PortType);
		switch (PortType) {
			case CommPortIdentifier.PORT_SERIAL:
				if ((val = System.getProperty("gnu.io.rxtx.SerialPorts")) == null)
				val = System.getProperty("javax.comm.SerialPorts");
				break;

			case CommPortIdentifier.PORT_PARALLEL:
				if ((val = System.getProperty("gnu.io.rxtx.ParallelPorts")) == null)
				val = System.getProperty("javax.comm.ParallelPorts");
				break;

			case CommPortIdentifier.PORT_I2C:
				if ((val = System.getProperty("gnu.io.rxtx.I2CPorts")) == null)
				val = System.getProperty("javax.comm.I2CPorts");
				break;

			case CommPortIdentifier.PORT_RS485:
				if ((val = System.getProperty("gnu.io.rxtx.RS485Ports")) == null)
				val = System.getProperty("javax.comm.RS485Ports");
				break;

			case CommPortIdentifier.PORT_RAW:
				if ((val = System.getProperty("gnu.io.rxtx.RawPorts")) == null)
				val = System.getProperty("javax.comm.RawPorts");
				break;

			default:
				if (debug)
				System.out.println("unknown port type "+PortType+" passed to RXTXCommDriver.registerSpecifiedPorts()");
		}

		if (val != null) {
			addSpecifiedPorts(val, PortType);
			return true;
		} else return false;
	}

   /*
    * Look for all entries in deviceDirectory, and if they look like they should
    * be serial ports on this OS and they can be opened then register
    * them.
    *
    */
	private void registerScannedPorts(int PortType)
	{
		String[] CandidateDeviceNames;
		if (debug)
			System.out.println("scanning device directory "+deviceDirectory+" for ports of type "+PortType);
		if(osName.toLowerCase().indexOf("windows") != -1 )
		{
			String[] temp = { "COM1", "COM2","COM3","COM4" };
			CandidateDeviceNames=temp;
		}
		else
		{
			File dev = new File( deviceDirectory );
			String[] temp = dev.list();
			CandidateDeviceNames=temp;
		}

		if (CandidateDeviceNames==null)
		{
			if (debug)
				System.out.println("RXTXCommDriver:registerScannedPorts() no Device files to check ");
			return;
		}

		String CandidatePortPrefixes[] = {};
		switch (PortType) {
			case CommPortIdentifier.PORT_SERIAL:
				if (debug)
					System.out.println("scanning for serial ports for os "+osName);
				if(osName.equals("Linux"))
				{
					String[] Temp = {
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
					};
					CandidatePortPrefixes=Temp;
				}
				else if(osName.equals("Irix"))
				{
					String[] Temp = {
						"ttyc", // irix raw character devices
						"ttyd", // irix basic serial ports
						"ttyf", // irix serial ports with hardware flow
						"ttym", // irix modems
						"ttyq", // irix pseudo ttys
						"tty4d",// irix RS422
						"tty4f",// irix RS422 with HSKo/HSki
						"midi", // irix serial midi
						"us"    // irix mapped interface
					};
					CandidatePortPrefixes=Temp;
				}
				else if(osName.equals("FreeBSD")) //FIXME this is probably wrong
				{
					String[] Temp = {
						"cuaa"  // FreeBSD Serial Ports
					};
					CandidatePortPrefixes=Temp;
				}
				else if(osName.equals("NetBSD")) // FIXME this is probably wrong
				{
					String[] Temp = {
						"tty0"  // netbsd serial ports
					};
					CandidatePortPrefixes=Temp;
				}
				else if(osName.equals("HP-UX"))
				{
					String[] Temp = {
						"tty0p",// HP-UX serial ports
						"tty1p" // HP-UX serial ports
					};
					CandidatePortPrefixes=Temp;
				}
				else if (osName.equals("Compaq's Digital UNIX"))
				{
					String[] Temp = {
						"tty0"  //  Digital Unix serial ports
					};
					CandidatePortPrefixes=Temp;
				}

				else if(osName.equals("BeOS"))
				{
					String[] Temp = {
						"serial" // BeOS serial ports
					};
					CandidatePortPrefixes=Temp;
				}
				else if(osName.equals("Mac OS X"))
				{
					String[] Temp = {
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
					CandidatePortPrefixes=Temp;
				}
				else if(osName.toLowerCase().indexOf("windows") != -1 )
				{
					String[] Temp = {
						"COM"    // win32 serial ports
					};
					CandidatePortPrefixes=Temp;
				}
				else
				{
					if (debug)
						System.out.println("No valid prefixes for serial ports have been entered for "+osName + " in RXTXCommDriver.java.  This may just be a typo in the method registerScanPorts().");
				}
				break;

			case CommPortIdentifier.PORT_PARALLEL:
				if (debug)
					System.out.println("scanning for parallel ports for os "+osName);
			/** Get the Parallel port prefixes for the running os
			* Holger Lehmann
			* July 12, 1999
			* IBM
			*/
				if(osName.equals("Linux")
			/*
				|| osName.equals("NetBSD") FIXME
				|| osName.equals("HP-UX")  FIXME
				|| osName.equals("Irix")   FIXME
				|| osName.equals("BeOS")   FIXME
				|| osName.equals("Compaq's Digital UNIX")   FIXME
			*/
				)
				{
					String[] temp={
						"lp"    // linux printer port
					};
					CandidatePortPrefixes=temp;
				}
				else if(osName.equals("FreeBSD"))
				{
					String[] temp={
						"lpt"
					};
					CandidatePortPrefixes=temp;
				}
				else  /* printer support is green */
				{
					String [] temp={};
					CandidatePortPrefixes=temp;
				}
				break;

			case CommPortIdentifier.PORT_I2C:
				if (debug)
					System.out.println("scanning for I2C ports for os "+osName);
				break;

			case CommPortIdentifier.PORT_RS485:
				if (debug)
					System.out.println("scanning for RS485 ports for os "+osName);
				break;

			case CommPortIdentifier.PORT_RAW:
				if (debug)
					System.out.println("scanning for RAW ports for os "+osName);
				break;

			default:
				if (debug)
					System.out.println("Unknown PortType "+PortType+" passed to RXTXCommDriver.registerScannedPorts()");
		}
		registerValidPorts(CandidateDeviceNames, CandidatePortPrefixes, PortType);
	}


	/*
	 * <p>From the NullDriver.java CommAPI sample.
	 */
	/**
	*  @param PortName The name of the port the OS recognizes
	*  @param PortType CommPortIdentifier.PORT_SERIAL or PORT_PARALLEL
	*  @returns CommPort
	*  getCommPort() will be called by CommPortIdentifier from its
	*  openPort() method. PortName is a string that was registered earlier
	*  using the CommPortIdentifier.addPortName() method. getCommPort()
	*  returns an object that extends either SerialPort or ParallelPort.
	*/
	public CommPort getCommPort( String PortName, int PortType )
	{
		if (debug) System.out.println("RXTXCommDriver:getCommPort("
			+PortName+","+PortType+")");
		try {
			switch (PortType) {
				case CommPortIdentifier.PORT_SERIAL:
					return new RXTXPort( PortName );
				case CommPortIdentifier.PORT_PARALLEL:
					return new LPRPort( PortName );
				case CommPortIdentifier.PORT_I2C:
					return new I2C( PortName );
				case CommPortIdentifier.PORT_RS485:
					return new RS485( PortName );
				case CommPortIdentifier.PORT_RAW:
					return new Raw( PortName );
				default:
					if (debug)
						System.out.println("unknown PortType  "+PortType+" passed to RXTXCommDriver.getCommPort()");
			}
		} catch( PortInUseException e ) {
			if (debug)
				System.out.println(
					"Port "+PortName+" in use by another application");
		}
		return null;
	}
}
