/*-------------------------------------------------------------------------
|   rxtx is a native interface to serial ports in java.
|   Copyright 1997-2000 by Trent Jarvi trentjarvi@yahoo.com
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
package  javax.comm;

import  java.io.*;
import  java.util.*;

/*------------------------------------------------------------------------------
Lots of stubs here.  Taken from the javadoc material produced from Sun's
commapi porting file.  Not used yet.
------------------------------------------------------------------------------*/

public class CommPortIdentifier 
{
	public static final int PORT_SERIAL   = 1;  // rs232 Port
	public static final int PORT_PARALLEL = 2;  // Parallel Port
	public static final int PORT_I2C      = 3;  // i2c Port
	public static final int PORT_RS485    = 4;  // rs485 Port
	private String PortName;
	private boolean Available = true;    
	private String Owner;    
	private CommPort commport;
	private CommDriver RXTXDriver;
 	private static CommPortIdentifier   CommPortIndex;
	private CommPortIdentifier next;
	private int PortType;
	private static boolean debug=true;
	private static Object Sync;
	private static String Properties;



/*------------------------------------------------------------------------------
	static {}   aka initialization
	accept:       -
	perform:      load the rxtx driver
	return:       -
	exceptions:   Throwable
	comments:     static block to initialize the class
------------------------------------------------------------------------------*/
	// initialization only done once....
	static 
	{
		if(debug) System.out.println("CommPortIdentifier:static initialization()");
		Sync = new Object();
		Properties=System.getProperty("java.home")+"//lib//javax.com.properties";
		try 
		{
			CommDriver RXTXDriver = (CommDriver) Class.forName("javax.comm.RXTXCommDriver").newInstance();
			RXTXDriver.initialize();
		} 
		catch (Throwable e) 
		{
			System.err.println(e + "thrown while loading " + "javax.comm.RXTXCommDriver");
		}
	}
	CommPortIdentifier ( String pn, CommPort cp, int pt, CommDriver driver) 
	{
		PortName        = pn;
		commport        = cp;
		PortType        = pt;
		next            = null;
		RXTXDriver      = driver;

	}

/*------------------------------------------------------------------------------
	addPortName()
	accept:         Name of the port s, Port type, 
                        reverence to RXTXCommDriver.
	perform:        place a new CommPortIdentifier in the linked list
	return: 	none.
	exceptions:     none.
	comments:
------------------------------------------------------------------------------*/
	public static void addPortName(String s, int type, CommDriver c) 
	{ 

		if(debug) System.out.println("CommPortIdentifier:addPortName("+s+")");
		SecurityManager MySecurity = System.getSecurityManager();
		if (MySecurity != null) 
		{
			MySecurity.checkRead(Properties);
		}
		AddIdentifierToList(new CommPortIdentifier(s, null, type, c));
	}
/*------------------------------------------------------------------------------
	AddIdentifierToList()
	accept:         
	perform:        
	return: 	
	exceptions:     
	comments:
------------------------------------------------------------------------------*/
	private static void AddIdentifierToList( CommPortIdentifier cpi)
	{
		if(debug) System.out.println("CommPortIdentifier:AddIdentifierToList()");
		synchronized (Sync) 
		{
			if (CommPortIndex == null) 
			{
				CommPortIndex = cpi;
			}
			else
			{ 
				CommPortIdentifier index  = CommPortIndex; 
				while (index.next != null)
				{
					index = index.next;
				}
				index.next = cpi;
			} 
		}
	}
/*------------------------------------------------------------------------------
	addPortOwnershipListener()
	accept:
	perform:
	return:
	exceptions:
	comments:
------------------------------------------------------------------------------*/
	public void addPortOwnershipListener(CommPortOwnershipListener c) 
	{ 
		if(debug) System.out.println("CommPortIdentifier:addPortOwnershipListener()");
	}
/*------------------------------------------------------------------------------
	getCurrentOwner()
	accept:
	perform:
	return:
	exceptions:
	comments:
------------------------------------------------------------------------------*/
	public String getCurrentOwner() 
	{ 
		if(debug) System.out.println("CommPortIdentifier:getCurrentOwner()");
		String s=new String();
		return(s);
	}
/*------------------------------------------------------------------------------
	getName()
	accept:
	perform:
	return:
	exceptions:
	comments:
------------------------------------------------------------------------------*/
	public String getName() 
	{ 
		if(debug) System.out.println("CommPortIdentifier:getName()");
		return(this.PortName);
	}
/*------------------------------------------------------------------------------
	getPortIdentifier()
	accept:
	perform:
	return:
	exceptions:
	comments:
------------------------------------------------------------------------------*/
	static public CommPortIdentifier getPortIdentifier(String s) throws NoSuchPortException 
	{ 
		if(debug) System.out.println("CommPortIdentifier:getPortIdentifier(" + s +")");
		System.out.println("configure --enable-RXTXIDENT is for developers only");
		SecurityManager MySecurity = System.getSecurityManager();
		if (MySecurity != null) 
		{ 
			MySecurity.checkRead(Properties); 
		}
		CommPortIdentifier index = CommPortIndex;

		synchronized (Sync) 
		{
			while (index != null) 
			{
				if (index.PortName.equals(s))
					break;
				index = index.next;
			}
		}
		if (index != null) 
			return index;
		else 
			throw new NoSuchPortException();
	}
/*------------------------------------------------------------------------------
	getPortIdentifier()
	accept:
	perform:
	return:
	exceptions:
	comments:
------------------------------------------------------------------------------*/
	static public CommPortIdentifier getPortIdentifier(CommPort c) throws NoSuchPortException 	
	{ 
		if(debug) System.out.println("CommPortIdentifier:getPortIdentifier()");
		System.out.println("configure --enable-RXTXIDENT is for developers only");
		throw new NoSuchPortException();
	}
/*------------------------------------------------------------------------------
	getPortIdentifiers()
	accept:
	perform:
	return:
	exceptions:
	comments:
------------------------------------------------------------------------------*/
	static public Enumeration getPortIdentifiers() 
	{ 
		if(debug) System.out.println("CommPortIdentifier:getPortIdentifiers()");
		return new CommPortEnumerator();
	}
/*------------------------------------------------------------------------------
	getPortType()
	accept:
	perform:
	return:
	exceptions:
	comments:
------------------------------------------------------------------------------*/
	public int getPortType() 
	{ 
		if(debug) System.out.println("CommPortIdentifier:getPortType()");
		return(1);
	}
/*------------------------------------------------------------------------------
	isCurrentlyOwned()
	accept:
	perform:
	return:
	exceptions:
	comments:
------------------------------------------------------------------------------*/
	public boolean isCurrentlyOwned() 
	{ 
		if(debug) System.out.println("CommPortIdentifier:isCurrentlyOwned()");
		return(false);
	}
/*------------------------------------------------------------------------------
	open()
	accept:
	perform:
	return:
	exceptions:
	comments:
------------------------------------------------------------------------------*/
	public CommPort open(FileDescriptor f) throws UnsupportedCommOperationException 
	{ 
		if(debug) System.out.println("CommPortIdentifier:open(FileDescriptor)");
		throw new UnsupportedCommOperationException();
	}
/*------------------------------------------------------------------------------
	open()
	accept:      application makeing the call and milliseconds to block
                     during open.
	perform:     open the port if possible
	return:      CommPort if successfull
	exceptions:  PortInUseException if in use.
	comments:
------------------------------------------------------------------------------*/
	private boolean HideOwnerEvents;

	public synchronized CommPort open(String TheOwner, int i) throws PortInUseException 
	{ 
		if(debug) System.out.println("CommPortIdentifier:open("+TheOwner + ", " +i+")");
		commport = RXTXDriver.getCommPort(PortName,PortType);
		if(Available)
		{
			if (debug) System.out.println("RXTXDriver.getCommPort() Worked!");
			Available = false;
			Owner = TheOwner;
			if (commport==null)
			{
				if (debug) System.out.println("RXTXDriver.getCommPort() failed");
			}
			return commport;
		}
		/* 
		possibly talk other jvms about getting the port?
		NativeFindOwner(PortName);
		throw new PortInUseException(); 
		*/
		if (debug) System.out.println("RXTXDriver.getCommPort() Yikes!");
		return null;
	}
/*------------------------------------------------------------------------------
	removePortOwnership()
	accept:
	perform:
	return:
	exceptions:
	comments:
------------------------------------------------------------------------------*/
	public void removePortOwnershipListener(CommPortOwnershipListener c) 
	{ 
		Available=true;
		if(debug) System.out.println("CommPortIdentifier:removePortOwnershipListener()");
	}

}

class CommPortEnumerator implements Enumeration {
	private CommPortIdentifier CPI;
	private boolean debug=true;
/*------------------------------------------------------------------------------
        nextElement()
        accept:
        perform:
        return:
        exceptions:
        comments:
------------------------------------------------------------------------------*/
	public Object nextElement() 
	{ 
		if(debug) System.out.println("CommPortEnumerator:nextElement()");
		return(CPI);
	}
/*------------------------------------------------------------------------------
        hasMoreElements()
        accept:
        perform:
        return:
        exceptions:
        comments:
------------------------------------------------------------------------------*/
	public boolean hasMoreElements() 
	{ 
		if(debug) System.out.println("CommPortEnumerator:hasMoreElements");
		return(false); 
	}
}
