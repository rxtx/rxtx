/*-------------------------------------------------------------------------
|   rxtx is a native interface to serial ports in java.
|   Copyright 1997-2004 by Trent Jarvi taj@www.linux.org.uk
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
OK.  Finally found the missing bits to get rxtx lp support up to speed.
http://people.redhat.com/twaugh/parport/html/parportguide.html
Patches available for linux 2.2 and 2.4.

This is just a quick test to see if the stuff is working.

*/

#include <sys/io.h>
#include <linux/ppdev.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>


int main(int argc, char **args)
{
	int compat=IEEE1284_MODE_COMPAT;
	int nibble=IEEE1284_MODE_NIBBLE;
	int byte=IEEE1284_MODE_BYTE;
	int epp=IEEE1284_MODE_EPP;
	int ecp=IEEE1284_MODE_ECP;

	
	int fd=open("/dev/lp0",O_NONBLOCK);
	if(!ioctl(fd,PPCLAIM))
		printf("PPCLAIM Failed\n");
	else
		printf("PPCLAIM works!\n");
	if(!ioctl(fd,PPNEGOT,&compat))
		printf("PPNEGOT compat Failed\n");
	else
		printf("PPNEGOT compat works!\n");
	if(!ioctl(fd,PPNEGOT,nibble))
		printf("PPNEGOT nibble Failed\n");
	else
		printf("PPNEGOT nibble works!\n");
	if(!ioctl(fd,PPNEGOT,byte))
		printf("PPNEGOT byte Failed\n");
	else
		printf("PPNEGOT byte works!\n");
	if(!ioctl(fd,PPNEGOT,epp))
		printf("PPNEGOT epp Failed\n");
	else
		printf("PPNEGOT epp works!\n");
	if(!ioctl(fd,PPNEGOT,ecp))
		printf("PPNEGO ecpT Failed\n");
	else
		printf("PPNEGOT ecp works!\n");
	if(!ioctl(fd, PPSETMODE,&compat))
		printf("PPSETMODE compat Failed\n");
	else
		printf("PPSETMODE compat works!\n");
	if(!ioctl(fd, PPSETMODE,&nibble))
		printf("PPSETMODE nibble  Failed\n");
	else
		printf("PPSETMODE nibble  works!\n");
	if(!ioctl(fd, PPSETMODE,&byte))
		printf("PPSETMODE byte Failed\n");
	else
		printf("PPSETMODE byte works!\n");
	if(!ioctl(fd, PPSETMODE,&epp))
		printf("PPSETMODE epp Failed\n");
	else
		printf("PPSETMODE epp works!\n");
	if(!ioctl(fd, PPSETMODE,&ecp))
		printf("PPSETMODE ecp Failed\n");
	else
		printf("PPSETMODE ecp works!\n");
	if(!ioctl(fd,PPRELEASE))
		printf("PPRELEASE Failed\n");
	else
		printf("PPRELEASE works!\n");
	exit(0);
}
