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
package javax.comm;
//import  java.io.*;
//import  java.util.*;


class CpoList 
{
	static boolean debug = true;
	CpoListEntry CpoListIndex;
/*------------------------------------------------------------------------------
        add()
        accept:       The Listener to be added
        perform:      Add the Listener to the list
        return:       None
        exceptions:   None
        comments:
------------------------------------------------------------------------------*/
	synchronized void add(CommPortOwnershipListener c) 
	{ 
		if(debug) System.out.println("CpoList:add()");
		CpoListEntry index = CpoListIndex;
		// Do we already have this List Entry?
		while (index != null && index.CpoListener != c)
			index = index.next;
		// no? then create it.
		if (CpoListIndex == null)
		{
			CpoListEntry temp = new CpoListEntry(c);
			temp.next = index;
			CpoListIndex = temp;
		}
	}
/*------------------------------------------------------------------------------
        remove()
        accept:      the listener to delete
        perform:     delete the listener from the linked list
        return:      None
        exceptions:  None
        comments:
------------------------------------------------------------------------------*/
	synchronized void remove(CommPortOwnershipListener c) 
	{ 
		CpoListEntry index = CpoListIndex;
		CpoListEntry last = null;

		if(debug) System.out.println("CpoList:remove()");
		// Do we already have this List Entry?
		while (index != null && index.CpoListener != c)
		{
			last = index;
			index = index.next;
		}
		// If we got it, delete it
		if (index != null)
		{
			if (last == null) CpoListIndex = index.next;
			else last.next = index.next;
		}
	}
/*------------------------------------------------------------------------------
        clonelist()
        accept:      None
        perform:     make a copy of the linked list
        return:      reference to the cloned list
        exceptions:  None
        comments:
------------------------------------------------------------------------------*/
	synchronized CpoList clonelist() 
	{ 
		if(debug) System.out.println("CpoList:clonelist()");
		CpoListEntry index = CpoListIndex;
		CpoListEntry Newindex = null;
		CpoListEntry temp = null;

		while ( index != null )
		{
			temp = new CpoListEntry(index.CpoListener);
			temp.next = Newindex;
			Newindex = temp;
			index = index.next;
		}
		CpoList clone = new CpoList();
		clone.CpoListIndex = Newindex;
		return(clone); 
	}
/*------------------------------------------------------------------------------
        isEmpty()
        accept:       None
        perform:      check if the list is empty
        return:       true if the list is empty else false
        exceptions:   None
        comments:    
------------------------------------------------------------------------------*/
	synchronized boolean isEmpty() 
	{  
		if(debug) System.out.println("CpoList:isEmpty()");
		return(CpoListIndex == null);
	}
/*------------------------------------------------------------------------------
        fireOwnershipEvent()
        accept:      the event type
        perform:     Send the event to all the listeners
        return:      None
        exceptions:  None
        comments:
------------------------------------------------------------------------------*/
	synchronized void fireOwnershipEvent(int i) 
	{ 
		if(debug) System.out.println("CpoList:fireOwnershipEvent()");
		CpoListEntry index = CpoListIndex;
		while ( index != null )
		{
			index.CpoListener.ownershipChange(i);
			index=index.next;
		}
	}
/*------------------------------------------------------------------------------
        dump()
        accept:      None
        perform:     print out the entire list.
        return:      None
        exceptions:  None
        comments:
------------------------------------------------------------------------------*/
	synchronized void dump() 
	{ 
		if(debug) System.out.println("CpoList:dump()");
		CpoListEntry index = CpoListIndex;
		while( index !=null )
		{
			System.out.println(index.CpoListener.toString());
			index = index.next;
		}
	}
}

