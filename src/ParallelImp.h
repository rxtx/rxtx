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

/* gnu.io.ParallelPort constants */
/*  this appears to be handled in /usr/src/linux/misc/parport_pc.c */
#define LPT_MODE_ANY	0
#define LPT_MODE_SPP	1
#define LPT_MODE_PS2	2
#define LPT_MODE_EPP	3
#define LPT_MODE_ECP	4
#define LPT_MODE_NIBBLE	5

/* some popular releases of Slackware do not have SSIZE_MAX */

#ifndef SSIZE_MAX
#	if defined(INT_MAX)
#		define SSIZE_MAX  INT_MAX
#	elif defined(MAXINT)
#		define SSIZE_MAX MAXINT
#	else
#		define SSIZE_MAX 2147483647 /* ugh */
#	endif
#endif

/* gnu.io.ParallelPortEvent constants */
#define PAR_EV_ERROR	1
#define PAR_EV_BUFFER	2

/* java exception class names */
#define UNSUPPORTED_COMM_OPERATION "gnu/io/UnsupportedCommOperationException"
#define ARRAY_INDEX_OUT_OF_BOUNDS "java/lang/ArrayIndexOutOfBoundsException"
#define OUT_OF_MEMORY "java/lang/OutOfMemoryError"
#define IO_EXCEPTION "java/io/IOException"
#define PORT_IN_USE_EXCEPTION "gnu/io/PortInUseException"

/*
Flow Control defines inspired by reading how mgetty by Gert Doering does it
*/

/* PROTOTYPES */
jboolean is_interrupted(JNIEnv *, jobject );
int send_event(JNIEnv *, jobject, jint, int );
int read_byte_array( int fd, unsigned char *buffer, int length, int threshold,
   int timeout );
int get_java_var( JNIEnv *, jobject, char *, char * );
void report(char *);
void report_error(char *);
void throw_java_exception( JNIEnv *, char *, char *, char * );
void throw_java_exception_system_msg( JNIEnv *, char *, char * );

