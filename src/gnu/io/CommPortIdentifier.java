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
	public static final int PORT_RAW      = 5;  // Raw Port
	private String PortName;
	private boolean Available = true;    
	private String Owner;    
	private CommPort commport;
	private CommDriver RXTXDriver;
 	static CommPortIdentifier   CommPortIndex;
	CommPortIdentifier next;
	private int PortType;
	private static boolean debug=true;
	static Object Sync;
	CpoList cpoList = new CpoList();
	OwnershipEventThread oeThread;



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
				if(debug) System.out.println("CommPortIdentifier:AddIdentifierToList() null");
			}
			else
			{ 
				CommPortIdentifier index  = CommPortIndex; 
				while (index.next != null)
				{
					index = index.next;
					if(debug) System.out.println("CommPortIdentifier:AddIdentifierToList() index.next");
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
	comments:   FIXME
------------------------------------------------------------------------------*/
	public void addPortOwnershipListener(CommPortOwnershipListener c) 
	{ 
		if(debug) System.out.println("FIXME CommPortIdentifier:addPortOwnershipListener()");
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
		return(Owner);
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
		if(debug) System.out.println("CommPortIdentifier:getPortIdentifier(" + s +")\nconfigure --enable-RXTXIDENT is for developers only");
		CommPortIdentifier index = CommPortIndex;

		synchronized (Sync) 
		{
			while (index != null) 
			{
				if (index.PortName.equals(s)) break;
				index = index.next;
			}
		}
		if (index != null) return index;
		else throw new NoSuchPortException();
	}
/*------------------------------------------------------------------------------
	getPortIdentifier()
	accept:
	perform:
	return:
	exceptions:
	comments:    FIXME
------------------------------------------------------------------------------*/
	static public CommPortIdentifier getPortIdentifier(CommPort c) throws NoSuchPortException 	
	{ 
		if(debug) System.out.println("FIXME CommPortIdentifier:getPortIdentifier(CommPort)");
		System.out.println("configure --enable-RXTXIDENT is for developers only");
		//throw new NoSuchPortException();
		return(null);
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
		if(debug) System.out.println("static CommPortIdentifier:getPortIdentifiers()");
		return new CommPortEnumerator();
	}
/*------------------------------------------------------------------------------
	getPortType()
	accept:
	perform:
	return:
	exceptions:
	comments:    FIXME
------------------------------------------------------------------------------*/
	public int getPortType() 
	{ 
		if(debug) System.out.println("FIXME CommPortIdentifier:getPortType()");
		return(PortType);
	}
/*------------------------------------------------------------------------------
	isCurrentlyOwned()
	accept:
	perform:
	return:
	exceptions:
	comments:     FIXME  This is where we want to use psutils code.
------------------------------------------------------------------------------*/
	public synchronized boolean isCurrentlyOwned() 
	{ 
		if(debug) System.out.println("FIXME CommPortIdentifier:isCurrentlyOwned()");
		return(false);
	}
/*------------------------------------------------------------------------------
	open()
	accept:
	perform:
	return:
	exceptions:
	comments:     FIXME
------------------------------------------------------------------------------*/
	public synchronized CommPort open(FileDescriptor f) throws UnsupportedCommOperationException 
	{ 
		if(debug) System.out.println("FIXME CommPortIdentifier:open(FileDescriptor)");
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
			Available = false;
			Owner = TheOwner;
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
	comments:    FIXME
------------------------------------------------------------------------------*/
	public void removePortOwnershipListener(CommPortOwnershipListener c) 
	{ 
		Available=true;
		Owner="";
		if(debug) System.out.println("FIXME CommPortIdentifier:removePortOwnershipListener()");
	}
/*------------------------------------------------------------------------------
	ownershipThreadWaiter()
	accept:
	perform:
	return:
	exceptions:
	comments:    FIXME
------------------------------------------------------------------------------*/
	void ownershipThreadWaiter()
	{
		if(debug) System.out.println("FIXME CommPortIdentifier:ownershipThreadWaiter()");
	}
/*------------------------------------------------------------------------------
	internalClosePort()
	accept:
	perform:
	return:
	exceptions:
	comments:    FIXME
------------------------------------------------------------------------------*/
	synchronized void internalClosePort() 
	{
		if(debug) System.out.println("FIXME CommPortIdentifier:internalClosePort()");
	}
/*------------------------------------------------------------------------------
	fireOwnershipEvent()
	accept:
	perform:
	return:
	exceptions:
	comments:    FIXME
------------------------------------------------------------------------------*/
	void fireOwnershipEvent(int eventType)
	{
		if(debug) System.out.println("FIXME CommPortIdentifier:fireOwnershipEvent()");
	}
/*------------------------------------------------------------------------------
	parsePropsFile()
	accept:
	perform:
	return:
	exceptions:
	comments:    FIXME? Could used in static init
------------------------------------------------------------------------------*/
//	private static String[] parsePropsFile(InputStream in) {}

}

