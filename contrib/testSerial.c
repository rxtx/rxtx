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
#include <termios.h>
#include <unistd.h>
#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdlib.h>

int main(int argc, char *argv[])
{
	int i, fd ;
	struct termios ttyset;

	if (argc < 1 )
	{
		printf("usage ./testSerial /dev/ttyS0\n");
		exit(1);
	}
	fd = open (argv[1], O_RDWR | O_NOCTTY | O_NONBLOCK );

		// wait for minicom/rxtx to start and user to press a key
		// You can set up the port in minicom/rxtx now.  Dont
		// exit minicom until you have hit a key to continue.

	fgetc(stdin);

	// get the attributes.
	
	tcgetattr( fd, &ttyset );

	/* print the attributes */
       
	fprintf(stderr, "c_iflag=%#x\n", ttyset.c_iflag);

	fprintf(stderr, "c_lflag=%#x\n", ttyset.c_lflag);
	fprintf(stderr, "c_oflag=%#x\n", ttyset.c_oflag);
	fprintf(stderr, "c_cflag=%#x\n", ttyset.c_cflag);
	fprintf(stderr, "c_cc[]: ");
	for(i=0; i<NCCS; i++)
	{
		fprintf(stderr,"%d=%x ", i, ttyset.c_cc[i]);
	}
	fprintf(stderr,"\n" );
	exit(0);
}
