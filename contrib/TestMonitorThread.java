/*-------------------------------------------------------------------------
|   rxtx is a native interface to serial ports in java.
|   Copyright 1997-2004 by Trent Jarvi taj@parcelfarce.linux.theplanet.co.uk
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
/*
	
	test   :  TestMonitorThread
	Author :  Trent Jarvi
	added  :  Sat Oct 13 17:45:31 MDT 2001
	Problem:  Monitor Thread didnt go away in the past.  This makes sure
		  the thread does exit and is GC's.  You can watch the threads
		  in a native threads jvm with top.
	todo   :  There are still some issues with the way BlackBox open/closes
		  the ports and the removal of the eventListener.

*/
import gnu.io.*;
import java.util.*;


public class TestMonitorThread implements SerialPortEventListener
{

	public TestMonitorThread()
	{
		CommPortIdentifier cpi;
    		Enumeration ports;
		SerialPort port = null;
		Date d = new Date();
		long result, t1 = d.getTime(), t2 = d.getTime();

		ports = CommPortIdentifier.getPortIdentifiers();
		while ( ports.hasMoreElements() )
		{
			cpi = (CommPortIdentifier) ports.nextElement();
			if ( cpi.getPortType() == CommPortIdentifier.PORT_SERIAL )
			{
				if ( cpi.getName().equals( "/dev/ttyS0" ) )
				{
					try {
						port = (SerialPort) cpi.open("TestMonitorThread", 2000);
					} catch (PortInUseException e) {}
					break;
				}
			} 
		} 
		for( int i=0;i<30;i++ )
		{
			try {
				port.addEventListener(this);
			} catch (TooManyListenersException e ) {
				e.printStackTrace();
			}
			t2 =  new Date().getTime();
			port.removeEventListener();
			System.out.println( t2 - t1 );
			t1 = t2;
		}
		port.close();
	}
	public static void main( String[] args )
	{
		System.out.println(">my TestMonitorThread");
		TestMonitorThread thisTestMonitorThread = new TestMonitorThread();
		System.out.println("<my TestMonitorThread");
	}
	public void serialEvent(SerialPortEvent event)
	{
		switch (event.getEventType())
		{
			case SerialPortEvent.BI:
			case SerialPortEvent.OE:
			case SerialPortEvent.FE:
			case SerialPortEvent.PE:
			case SerialPortEvent.CD:
			case SerialPortEvent.CTS:
			case SerialPortEvent.DSR:
			case SerialPortEvent.RI:
			case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
			case SerialPortEvent.DATA_AVAILABLE:
				System.out.println("Event");
				break;
		}
	}
}
