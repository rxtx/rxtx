/*-------------------------------------------------------------------------
|   rxtx is a native interface to serial ports in java.
|   Copyright 1997-2001 by Trent Jarvi trentjarvi@yahoo.com
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
#if defined(__MWERKS__) /* dima */
#include "RXTXPort.h" /* dima */
#else  /* dima */
#include "config.h"
#include "gnu_io_RXTXPort.h"
#endif /* dima */
#ifndef __LCC__
#   include <unistd.h>
#else /* windows lcc compiler for fd_set. probably wrong */
#   include<winsock.h>
#endif /* __LCC__ */
#include <time.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <errno.h>
#include <limits.h>
#include <sys/stat.h>
#ifndef WIN32
#include <sys/ioctl.h>
#include <sys/param.h>
#include <sys/utsname.h>
#include <pthread.h>
#else
#	include <win32termios.h>
/*  FIXME  returns 0 in all cases on win32
#define S_ISCHR(m)	(((m)&S_IFMT) == S_IFCHR)
*/
#	if !defined(S_ISCHR)
#		define S_ISCHR(m) (1)
#	endif /* S_ISCHR(m) */
#endif /* WIN32 */
#ifdef HAVE_TERMIOS_H
#	include <termios.h>
#endif /* HAVE_TERMIOS_H */
#ifdef HAVE_SIGNAL_H
#   include <signal.h>
#endif /* HAVE_SIGNAL_H */
#ifdef HAVE_SYS_SIGNAL_H
#   include <sys/signal.h>
#endif /* HAVE_SYS_SIGNAL_H */
#include <sys/types.h>
#ifdef HAVE_SYS_TIME_H
#   include <sys/time.h>
#endif /* HAVE_SYS_TIME_H */
#   include <fcntl.h>
#ifdef HAVE_SYS_FCNTL_H
#   include <sys/fcntl.h>
#endif /* HAVE_SYS_FCNTL_H */
#ifdef HAVE_SYS_FILE_H
#   include <sys/file.h>
#endif /* HAVE_SYS_FILE_H */

#if defined(__linux__)
#	include <linux/types.h> /* fix for linux-2.3.4? kernels */
#	include <linux/serial.h>
#	include <linux/version.h>
#endif /* __linux__ */
#if defined(__sun__)
#	include <sys/filio.h>
#	include <sys/mkdev.h>
#endif /* __sun__ */
#if defined(__hpux__)
#	include <sys/modem.h>
#endif /* __hpux__ */
/* FIXME -- new file */
#if defined(__APPLE__)
#	include <CoreFoundation/CoreFoundation.h>
#	include <IOKit/IOKitLib.h>
#	include <IOKit/serial/IOSerialKeys.h>
#	include <IOKit/IOBSD.h>
#endif /* __APPLE__ */
#ifdef __unixware__
#	include  <sys/filio.h>
#endif /* __unixware__ */
#ifdef HAVE_PWD_H
#include	<pwd.h>
#endif /* HAVE_PWD_H */
#ifdef HAVE_GRP_H
#include 	<grp.h>
#endif /* HAVE_GRP_H */

extern int errno;
#ifdef TRENT_IS_HERE
#undef TIOCSERGETLSR
/*
#define DEBUG
#define DEBUG_MW
#define DONT_USE_OUTPUT_BUFFER_EMPTY_CODE
#define SIGNALS
*/
#endif /* TRENT_IS_HERE */
#include "SerialImp.h"

/* this is so diff will not generate noise when merging 1.4 and 1.5 changes
 * It will eventually be removed.
 * */
#define RXTXPort(foo) Java_gnu_io_RXTXPort_ ## foo
#define RXTXCommDriver(foo) Java_gnu_io_RXTXCommDriver_ ## foo

#if defined(__sun__)
/*----------------------------------------------------------
cfmakeraw

   accept:      termios to be set to raw
   perform:     initializes the termios structure.
   return:      int 0 on success
   exceptions:  none
   comments:    this is how linux cfmakeraw works.
		termios(3) manpage
----------------------------------------------------------*/

int cfmakeraw ( struct termios *term )
{
	ENTER( "cfmakeraw" );
	term->c_iflag &= ~(IGNBRK|BRKINT|PARMRK|ISTRIP|INLCR|IGNCR|ICRNL|IXON);
	term->c_oflag &= ~OPOST;
	term->c_lflag &= ~(ECHO|ECHONL|ICANON|ISIG|IEXTEN);
	term->c_cflag &= ~(CSIZE|PARENB);
	term->c_cflag |= CS8;
	LEAVE( "cfmakeraw" );
	return( 0 );
}
#endif /* __sun__ */

struct event_info_struct *master_index = NULL;
//int eventloop_interrupted = 0;

/*----------------------------------------------------------
RXTXPort.Initialize

   accept:      none
   perform:     Initialize the native library
   return:      none
   exceptions:  none
   comments:    Basically this just causes rxtx to ignore signals.  signal
		handlers where tried but the VM (circa 1.1) did not like it.

		It also allows for some sanity checks on linux boxes if DEBUG
		is enabled.
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(Initialize)(
	JNIEnv *env,
	jclass jclazz
	)
{
#if defined DEBUG && defined(__linux__)
	struct utsname name;
	char message[80];
#endif /* DEBUG && __linux__ */
	/* This bit of code checks to see if there is a signal handler installed
	   for SIGIO, and installs SIG_IGN if there is not.  This is necessary
	   for the native threads jdk, but we don't want to do it with green
	   threads, because it slows things down.  Go figure. */

	/* POSIX signal handling functions */
#if !defined(WIN32)
	struct sigaction old_action;
	sigaction(SIGIO, NULL, &old_action);
	/* green threads already has handler, no touch */
	if (old_action.sa_handler == NULL) {
		/* no handler when using native threads, set to ignore */
		struct sigaction new_action;
		sigset_t block_mask;
		sigemptyset(&block_mask);
		new_action.sa_handler = SIG_IGN;
		new_action.sa_flags = SA_RESTART;
		new_action.sa_mask = block_mask;
		sigaction(SIGIO, &new_action, NULL);
	} 
#endif /* !WIN32 */
	ENTER( "RXTXPort:Initialize" );
#if defined(DEBUG) && defined(__linux__)
	/* Lets let people who upgraded kernels know they may have problems */
	if (uname (&name) == -1)
	{
		report( "RXTX WARNING:  cannot get system name\n" );
		LEAVE( "RXTXPort:Initialize" );
		return;
	}
	if(strcmp(name.release,UTS_RELEASE)!=0)
	{
		sprintf( message, LINUX_KERNEL_VERSION_ERROR, UTS_RELEASE,
			name.release );
		report( message );
		getchar();
	}
	LEAVE( "RXTXPort:Initialize" );
#endif /* DEBUG && __linux__ */
}

/*----------------------------------------------------------
RXTXPort.open

   accept:      The device to open.  ie "/dev/ttyS0"
   perform:     open the device, set the termios struct to sane settings and
                return the filedescriptor
   return:      fd
   exceptions:  IOExcepiton
   comments:    Very often people complain about not being able to get past
                this function and it turns out to be permissions on the
                device file or bios has the device disabled.
----------------------------------------------------------*/
JNIEXPORT jint JNICALL RXTXPort(open)(
	JNIEnv *env,
	jobject jobj,
	jstring jstr
	)
{
	struct termios ttyset;
	int fd;
	int  pid = -1;
	char message[80];
	const char *filename;
	jclass jclazz = (*env)->GetObjectClass( env, jobj );
	jfieldID jfid = (*env)->GetFieldID( env, jclazz, "pid", "I" );

	if( !jfid ) {
		(*env)->ExceptionDescribe( env );
		(*env)->ExceptionClear( env );
		(*env)->DeleteLocalRef( env, jclazz );
		return -1;
	}

#ifndef WIN32
	pid = getpid();
#endif /* WIN32 */

	(*env)->SetIntField(env, jobj, jfid, ( jint ) pid );
	(*env)->DeleteLocalRef( env, jclazz );

 	filename = (*env)->GetStringUTFChars( env, jstr, 0 );

	/* 
		LOCK is one of three functions defined in SerialImp.h

			uucp_lock		Solaris
			fhs_lock		Linux
			system_does_not_lock	Win32
	*/
			
	ENTER( "RXTXPort:open" );
	if ( LOCK( filename) )
	{
		sprintf( message, "locking has failed for %s\n", filename );
		report( message );
		goto fail;
	}
	else
	{
		sprintf( message, "locking worked for %s\n", filename );
		report( message );
	}

	do {
		fd=OPEN (filename, O_RDWR | O_NOCTTY | O_NONBLOCK );
	}  while (fd < 0 && errno==EINTR);
	if( fd < 0 ) goto fail;
	(*env)->ReleaseStringUTFChars( env, jstr, filename );

	if( tcgetattr( fd, &ttyset ) < 0 ) goto fail;
	ttyset.c_iflag = INPCK;
	ttyset.c_lflag = 0;
	ttyset.c_oflag = 0;
	ttyset.c_cflag = CREAD | CS8 | CLOCAL;
	ttyset.c_cc[ VMIN ] = 0;
	ttyset.c_cc[ VTIME ] = 0;

#ifdef __FreeBSD__
	if( cfsetspeed( &ttyset, B9600 ) < 0 ) goto fail;
#else
	if( cfsetispeed( &ttyset, B9600 ) < 0 ) goto fail;
	if( cfsetospeed( &ttyset, B9600 ) < 0 ) goto fail;
#endif
	if( tcsetattr( fd, TCSANOW, &ttyset ) < 0 ) goto fail;
#ifndef WIN32
	fcntl( fd, F_SETOWN, getpid() );
#endif /* WIN32 */
#ifdef FASYNC
	fcntl( fd, F_SETFL, FASYNC );
#endif /* FASYNC */

	sprintf( message, "fd returned is %i\n", fd );
	report( message );
	LEAVE( "RXTXPort:open" );
	return (jint)fd;

fail:
	(*env)->ReleaseStringUTFChars( env, jstr, filename );
	LEAVE( "RXTXPort:open" );
	throw_java_exception( env, PORT_IN_USE_EXCEPTION, "open",
		strerror( errno ) );
	return -1;
}

/*----------------------------------------------------------
RXTXPort.nativeClose

   accept:      none
   perform:     get the fd from the java end and close it
   return:      none
   exceptions:  none
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(nativeClose)( JNIEnv *env,
	jobject jobj,jstring jstr )
{
	int result, pid;
	int fd = get_java_var( env, jobj,"fd","I" );
	const char *filename = (*env)->GetStringUTFChars( env, jstr, 0 );
	jclass jclazz = (*env)->GetObjectClass( env, jobj );
	pid = get_java_var( env, jobj,"pid","I" );

	report(">pid\n");
	if( !pid ) {
		(*env)->ExceptionDescribe( env );
		(*env)->ExceptionClear( env );
		(*env)->DeleteLocalRef( env, jclazz );
		report("Close not detecting thread pid");
		return;
	}
	report("<pid\n");

	/* 
		UNLOCK is one of three functions defined in SerialImp.h

			uucp_unlock		Solaris
			fhs_unlock		Linux
			system_does_not_unlock	Win32
	*/

	ENTER( "RXTXPort:nativeClose" );
	if (fd > 0)
	{
		do {
			result=CLOSE (fd);
		}  while ( result < 0 && errno == EINTR );
		UNLOCK( filename, pid );
	}
	report("Delete\n");
	(*env)->DeleteLocalRef( env, jclazz );
	report("release\n");
	(*env)->ReleaseStringUTFChars( env, jstr, filename );
	LEAVE( "RXTXPort:nativeClose" );
	return;
}

/*----------------------------------------------------------
 RXTXPort.nativeSetSerialPortParams

   accept:     speed, data bits, stop bits, parity
   perform:    set the serial port parameters
   return:     void
   exceptions: UnsupportedCommOperationException
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(nativeSetSerialPortParams)(
	JNIEnv *env, jobject jobj, jint speed, jint dataBits, jint stopBits,
	jint parity )
{
	struct termios ttyset;
	int fd = get_java_var( env, jobj,"fd","I" );
	int cspeed = translate_speed( env, speed );
#ifdef TIOCGSERIAL
	struct serial_struct sstruct;
#endif /* TIOCGSERIAL */


	ENTER( "RXTXPort:nativeSetSerialPortParams" );
	if( !cspeed )
	{
		report( "Invalid Speed Selected\n" );
		goto fail;
	}

	if( tcgetattr( fd, &ttyset ) < 0 )
	{
		report( "Cannot Get Serial Port Settings\n" );
		goto fail;
	}

	if( !translate_data_bits( env, &(ttyset.c_cflag), dataBits ) )
	{
		report( "Invalid Data Bits Selected\n" );
		goto fail;
		return;
	}

	if( !translate_stop_bits( env, &(ttyset.c_cflag), stopBits ) )
	{
		report( "Invalid Stop Bits Selected\n" );
		goto fail;
	}

	if( !translate_parity( env, &(ttyset.c_cflag), parity ) )
	{
		report( "Invalid Parity Selected\n" );
		goto fail;
	}

#ifdef TIOCGSERIAL
	if ( cspeed > 1000000 )
	{
		/*
		The following speeds can not be set using cfset*speed()
		The defines are added in SerialImp.h.

		The speed is set to 38400 which is actually a custom
		speed.

		The baud_base and desired speed are then used to
		calculate a custom divisor.

		On linux the setserial man page covers this.
		*/

		if ( ioctl( fd, TIOCGSERIAL, &sstruct ) < 0 )
		{
			goto fail;
		}

		switch( cspeed )
		{
			case B14400:
				cspeed = 14400;
				break;
			case B28800:
				cspeed = 28800;
				break;
			case B128000:
				cspeed = 128000;
				break;
			case B256000:
				cspeed = 256000;
				break;
			default:
				goto fail;
		}
	
		if ( cspeed < 1  || sstruct.baud_base < 1 )
		{
			goto fail;
		}

		sstruct.custom_divisor = ( sstruct.baud_base/cspeed );

		if (	sstruct.baud_base < 1 ||
			ioctl( fd, TIOCSSERIAL, &sstruct ) < 0 )
		{
			goto fail;
		}

		cspeed = 38400;
	}
#endif /* TIOCGSERIAL */

#ifdef __FreeBSD__
	if( cfsetspeed( &ttyset, cspeed ) < 0 )
	{
		report( "Cannot Set Speed\n" );
		goto fail;
	}
#else
	if(
		cfsetispeed( &ttyset, cspeed ) < 0 ||
		cfsetospeed( &ttyset, cspeed ) < 0 )
	{
		report( "Cannot Set Speed\n" );
		goto fail;
	}
#endif  /* __FreeBSD__ */

	if( tcsetattr( fd, TCSANOW, &ttyset ) < 0 )
	{
		goto fail;
	}

	LEAVE( "RXTXPort:nativeSetSerialPortParams" );
	return;

fail:
	LEAVE( "RXTXPort:nativeSetSerialPortParams" );
	throw_java_exception( env, UNSUPPORTED_COMM_OPERATION,
		"nativeSetSerialPortParams", strerror( errno ) );
}

/*----------------------------------------------------------
 translate_speed

   accept:     speed in bits-per-second
   perform:    convert bits-per-second to a speed_t constant
   return:     speed_t constant
   exceptions: UnsupportedCommOperationException
   comments:   Only the lowest level code should know about
               the magic constants.
----------------------------------------------------------*/
int translate_speed( JNIEnv *env, jint speed )
{
	LEAVE( "RXTXPort:translate_speed" );
	switch( speed ) {
		case 0:		return B0;
		case 50:	return B50;
		case 75:	return B75;
		case 110:	return B110;
		case 134:	return B134;
		case 150:	return B150;
		case 200:	return B200;
		case 300:	return B300;
		case 600:	return B600;
		case 1200:	return B1200;
		case 1800:	return B1800;
		case 2400:	return B2400;
		case 4800:	return B4800;
		case 9600:	return B9600;
		case 19200:	return B19200;
		case 38400:	return B38400;
#ifdef B57600
		case 57600:	return B57600;
#endif /* B57600 */
#ifdef B115200
		case 115200:	return B115200;
#endif /* B115200 */
#ifdef B230400
		case 230400:	return B230400;
#endif /* B230400 */
#ifdef B460800
		case 460800:	return B460800;
#endif /* B460800 */
		case 14400:	return B14400;
		case 28800:	return B28800;
#ifdef B128000  /* dima */
		case 128000:	return B128000;
#endif  /* dima */
#ifdef B256000  /* dima */
		case 256000:	return B256000;
#endif  /* dima */
	}

	LEAVE( "RXTXPort:translate_speed" );
	throw_java_exception( env, UNSUPPORTED_COMM_OPERATION,
		"", "Baud rate not supported" );
	return 0;
}

/*----------------------------------------------------------
 translate_data_bits

   accept:     gnu.io.SerialPort.DATABITS_* constant
   perform:    set proper termios c_cflag bits
   return:     1 if successful
					0 if an exception is thrown
   exceptions: UnsupportedCommOperationException
----------------------------------------------------------*/
int translate_data_bits( JNIEnv *env, tcflag_t *cflag, jint dataBits )
{
	int temp = (*cflag) & ~CSIZE;

	ENTER( "translate_date_bits" );
	switch( dataBits ) {
		case JDATABITS_5:
			(*cflag) = temp | CS5;
			return 1;
		case JDATABITS_6:
			(*cflag) = temp | CS6;
			return 1;
		case JDATABITS_7:
			(*cflag) = temp | CS7;
			return 1;
		case JDATABITS_8:
			(*cflag) = temp | CS8;
			return 1;
	}

	LEAVE( "RXTXPort:translate_date_bits" );
	throw_java_exception( env, UNSUPPORTED_COMM_OPERATION,
		"", "databit value not supported" );
	return 0;
}

/*----------------------------------------------------------
 translate_stop_bits

   accept:     gnu.io.SerialPort.STOPBITS_* constant
   perform:    set proper termios c_cflag bits
   return:     1 if successful
					0 if an exception is thrown
   exceptions: UnsupportedCommOperationException
   comments:   If you specify 5 data bits and 2 stop bits, the port will
               allegedly use 1.5 stop bits.  Does anyone care?
----------------------------------------------------------*/
int translate_stop_bits( JNIEnv *env, tcflag_t *cflag, jint stopBits )
{
	ENTER( "translate_stop_bits" );
	switch( stopBits ) {
		case STOPBITS_1:
			(*cflag) &= ~CSTOPB;
			LEAVE( "RXTXPort:translate_stop_bits" );
			return 1;
		/*  ok.. lets try putting it in and see if anyone notices */
		case STOPBITS_1_5:
			translate_data_bits( env, cflag, JDATABITS_5 );
		case STOPBITS_2:
			(*cflag) |= CSTOPB;
			LEAVE( "RXTXPort:translate_stop_bits" );
			return 1;
	}

	LEAVE( "RXTXPort:translate_stop_bits" );
	throw_java_exception( env, UNSUPPORTED_COMM_OPERATION,
		"", "stopbit value not supported" );
	return 0;
}

/*----------------------------------------------------------
 translate_parity

   accept:     javx.comm.SerialPort.PARITY_* constant
   perform:    set proper termios c_cflag bits
   return:     1 if successful
               0 if an exception is thrown
   exceptions: UnsupportedCommOperationException
   comments:   The CMSPAR bit should be used for 'mark' and 'space' parity,
               but it's not in glibc's includes.  Oh well, rarely used anyway.
----------------------------------------------------------*/
int translate_parity( JNIEnv *env, tcflag_t *cflag, jint parity )
{
	ENTER( "translate_parity" );
	(*cflag) &= ~(PARENB | PARODD);
	switch( parity ) {
		case JPARITY_NONE:
			LEAVE( "translate_parity" );
			return 1;
		case JPARITY_EVEN:
			(*cflag) |= PARENB;
			LEAVE( "translate_parity" );
			return 1;
		case JPARITY_ODD:
			(*cflag) |= PARENB | PARODD;
			LEAVE( "translate_parity" );
			return 1;
#ifdef CMSPAR
		case JPARITY_MARK:
			(*cflag) |= PARENB | PARODD | CMSPAR;
			LEAVE( "translate_parity" );
			return 1;
		case JPARITY_SPACE:
			(*cflag) |= PARENB | CMSPAR;
			LEAVE( "translate_parity" );
			return 1;
#endif /* CMSPAR */
	}

	LEAVE( "translate_parity" );
	throw_java_exception( env, UNSUPPORTED_COMM_OPERATION,
		"", "parity value not supported" );
	return 0;
}
/*----------------------------------------------------------
thread_write()

   accept:      
   perform:     
   return:      none
   exceptions:  
   comments:	
----------------------------------------------------------*/

#ifndef TIOCSERGETLSR
void *thread_write( void *arg )
{
	struct event_info_struct *eis = ( struct event_info_struct * ) arg;
	struct tpid_info_struct *t = eis->tpid;
	int result;

	if( !eis->jclazz )
	{
		report("thread_write: jclazz is borked\n");
		return( NULL );
	}
	report("thread_write: have jclazz\n");
	while( t->tcdrain == 1 ) usleep(2000);
	if( !t->closing )
	{
		result = write( eis->fd, t->buff, t->length );
		t->length = result;
	}
	else
	{
		t->length = 0;
#ifdef SIGNALS
		pthread_cond_signal( t->cpt );
#else
		t->done = 1;
#endif
		return( NULL );
	}

#ifdef SIGNALS
	report("thread_write: cond_signal\n");
	pthread_cond_signal( t->cpt );
#else
		t->done = 1;
#endif
	if( t->closing)
	{
		report("thread_write: thread_write returning before tcdrain\n");
		return( NULL );
	}
	report("thread_write: tcdrain\n");
	t->tcdrain = 1;
	if( tcdrain( eis->fd ) == 0 && t->done == 1 )
	{
		report("thread_write: setting the dang OUTPUT_BUFFER_EMPTY\n" );
		/* while(eis->output_buffer_empty_flag == 1 ) usleep(100); */
		eis->output_buffer_empty_flag = 1;
	}
	else report("thread_write: NOT sending the blasted OUTPUT_BUFFER_EMPTY\n" );
	t->tcdrain = 0;
	report("thread_write: return(NULL)\n" );
	pthread_exit( NULL );
	/* -Wall ?fix */
	return( NULL );
}

/*----------------------------------------------------------
spawn_write_thread()

   accept:      
   perform:     
   return:      none
   exceptions:  
   comments:	
----------------------------------------------------------*/

int spawn_write_thread( int fd, char *buff, int length,
			JNIEnv *env, jobject *jobj)
{
	struct event_info_struct *eis;
	struct tpid_info_struct *t;
	char msg[80];

	report("spawn_write_thread: entering\n");

#ifdef DONT_USE_OUTPUT_BUFFER_EMPTY_CODE
	return(write( fd, buff, length )); 
#else
	eis = (struct event_info_struct * )
		get_java_var( env, *jobj, "eis", "I" );
	sprintf( msg, "spawn_write_thread: got eis %i\n", (int) eis );
	report( msg );

	/*
	   The eventLoop has not started. just write and return since
	   Nothing is there to get OUTPUT_BUFFER_EMPTY
	*/
	if ( ! eis )
	{
		report_error( "NO EVENT LOOP!!\n");
		return( write( fd, buff, length )); 
	}

	t = eis->tpid;

#ifdef SIGNALS
	report("spawn_write_thread: lock mutex\n");
	while( t->write_counter && !t->closing && pthread_mutex_trylock( t->mutex ))
	{
		report("spawn_write_thread: mutex locked\n");
		pthread_cond_wait( t->cpt, t->mutex );
	}
#else
	while( t->write_counter && !t->closing ) usleep(2000);
	t->done = 0;
#endif

	if( t->closing ) return( 0 );

	/* info for the thread to write and send event when output is empty */
	t->length = length;
	t->buff = buff;

	/* queue next write */
	t->write_counter++;

	//sprintf(msg, "spawn_write_thread: %s %i\n", buff, length);
	//report( msg );

	/*----------------------------------------------*/
	report("spawn_write_thread: create thread_write\n");
	pthread_create( &t->tpid, NULL, thread_write, eis );
	pthread_detach( t->tpid );
	/*----------------------------------------------*/
#ifdef SIGNALS
	report(">spawn_write_thread: cond_wait\n");
	pthread_cond_wait( t->cpt, t->mutex );
#else
	while(! t->done ) usleep(1000);
#endif
	t->write_counter--;
#ifdef SIGNALS
	report("spawn_write_thread: unlock mutex\n");
	pthread_mutex_unlock( t->mutex );
	report("<spawn_write_thread: cond_wait\n");
#endif
	sprintf(msg, "spawn_write_thread: exiting! return = %i\n", t->length );
	report( msg );
	return(t->length);
#endif /* DONT_USE_OUTPUT_BUFFER_EMPTY_CODE */
}
#endif /* TIOCSERGETLSR */

/*----------------------------------------------------------
finalize_thread_write( )

   accept:      none
   perform:     
   return:      none
   exceptions:  none
   comments:	
----------------------------------------------------------*/
void finalize_thread_write( struct event_info_struct *eis )
{
#ifndef TIOCSERGETLSR
	/* used to shut down any remaining write threads */

	report("entering finalize_thread_write\n");
	if( ! eis ) return;
	if( ! eis->tpid ) return;
	eis->tpid->closing = 1;
#ifdef SIGNALS
	while( eis->tpid->write_counter && pthread_mutex_trylock( eis->tpid->mutex ))
	{
		report("finalize_thread_write: mutex locked\n");
		pthread_cond_wait( eis->tpid->cpt, eis->tpid->mutex );
	}
	report(">mutex_unlock\n");
	pthread_mutex_unlock( eis->tpid->mutex );
	report("<mutex_unlock\n");
#else
	while( eis->tpid->write_counter ) usleep(100);
#endif
	if ( eis->tpid->cpt )
	{
		pthread_cond_destroy( eis->tpid->cpt );
		report(">free cpt\n");
		free( eis->tpid->cpt );
		report("<free cpt\n");
	}
	report(">free tpid\n");
	if ( eis->tpid ) free( eis->tpid );
	report("<free tpid\n");

	/* need to clean up again after working events */
	report("leaving finalize_thread_write\n");
#endif /* TIOCSERGETLSR */
}

/*----------------------------------------------------------
add_tpid

   accept:      struct tpid_info_struct pointer,,
   perform:     
   return:      
   exceptions:  None
   comments:    This is used to cleanup threads and make sure the
		write()'s happen in order on systems without access to the LSR
----------------------------------------------------------*/
#ifndef TIOCSERGETLSR
struct tpid_info_struct *add_tpid( struct tpid_info_struct *p )
{
	struct tpid_info_struct *q;

	/* FIXME TRENT.  Do we need to use key funcitons here? */
	q = malloc( sizeof(struct tpid_info_struct) );
	if( !q )
	{
		report_error("RXTX: Out of Memory\n");
		return( NULL );
	}
	q->tpid = 0;
	q->closing = 0;
	q->write_counter = 0;
	q->tcdrain = 0;
	return( q );
}
#endif /* TIOCSERGETLSR */

static void warn_sig_abort( int signo )
{
	char msg[80];
	sprintf( msg, "RXTX Recieved Signal %i\n", signo );
	report_error( msg );
}
/*----------------------------------------------------------
init_thread_write( )

   accept:      none
   perform:     
   return:      none
   exceptions:  none
   comments:	
----------------------------------------------------------*/
int init_thread_write( struct event_info_struct *eis )
{
#ifndef TIOCSERGETLSR
	struct tpid_info_struct *t = add_tpid( NULL);
	sigset_t newmask, oldmask;
	struct sigaction newaction, oldaction;
	jfieldID jeis;

	report("init_thread_write:  start\n");
	sigemptyset(&newmask);
	sigaddset(&newmask, SIGCHLD);
	newaction.sa_handler = warn_sig_abort;
	sigemptyset( &newaction.sa_mask );
	newaction.sa_flags = SA_INTERRUPT;
	sigaction(SIGABRT, &newaction, &oldaction);
	pthread_sigmask( SIG_BLOCK, &newmask, &oldmask );
	sigprocmask( SIG_SETMASK, &newmask, &oldmask );
/*
	sigfillset(&newmask);
	sigprocmask( SIG_SETMASK, &newmask, &oldmask );
*/

	eis->tpid = t;
	t->mutex = malloc(sizeof(pthread_mutex_t));
	t->cpt = malloc(sizeof(pthread_cond_t));

	report("thread_write: init mutex\n");
	pthread_mutex_init( t->mutex, NULL );
	report("thread_write: init cond\n");
	pthread_cond_init( t->cpt, NULL );
	report("thread_write: set eis\n");
	jeis  = (*eis->env)->GetFieldID( eis->env, eis->jclazz, "eis", "I" );
	report("thread_write: got eis\n");
	(*eis->env)->SetIntField(eis->env, *eis->jobj, jeis, ( jint ) eis );
	report("thread_write: set eis\n");
#else
	eis->tpid = NULL;
#endif /* TIOCSERGETLSR */
	report("init_thread_write:  stop\n");
	return( 1 );
}

/*----------------------------------------------------------
RXTXPort.writeByte

   accept:      byte to write (passed as int)
   perform:     write a single byte to the port
   return:      none
   exceptions:  IOException
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(writeByte)( JNIEnv *env,
	jobject jobj, jint ji )
{
	unsigned char byte = (unsigned char)ji;
	int fd = get_java_var( env, jobj,"fd","I" );
	int result;

	ENTER( "RXTXPort:writeByte" );
	/*
	   jobject and env are passed to the thread_write code above for
	   systems that do not have access to the LSR used to detect if
	   output outputbuffer is empty

	   win32 implements its own write in termios.c
	
	   other systems use the API write directly
	*/
	do {
#ifdef TIOCSERGETLSR
		result=WRITE (fd, &byte, sizeof(unsigned char));
#else
		//printf("writeByte %c>>\n", byte);
		result=spawn_write_thread (fd, &byte, sizeof(unsigned char),
						env, &jobj);
		report("writeByte<<\n");
#endif /* TIOCSERGETLSR */
	}  while (result < 0 && errno==EINTR);
	LEAVE( "RXTXPort:writeByte" );
	if(result >= 0)
	{
/*
		report_verbose( "sending OUTPUT_BUFFER_EMPTY\n" );
		send_event( env, jobj, SPE_OUTPUT_BUFFER_EMPTY, 1 );
*/
		return;
	}
	throw_java_exception( env, IO_EXCEPTION, "writeByte",
		strerror( errno ) );
}

/*----------------------------------------------------------
RXTXPort.writeArray

   accept:      jbarray: bytes used for writing
                offset: offset in array to start writing
                count: Number of bytes to write
   perform:     write length bytes of jbarray
   return:      none
   exceptions:  IOException
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(writeArray)( JNIEnv *env,
	jobject jobj, jbyteArray jbarray, jint offset, jint count )
{
	int fd = get_java_var( env, jobj,"fd","I" );
	int result=0,total=0;
	jbyte *body = (*env)->GetByteArrayElements( env, jbarray, 0 );
	char message[1000];
#if defined ( __sun__ )
	struct timespec retspec;

	retspec.tv_sec = 0;
	retspec.tv_nsec = 50000;
#endif /* __sun__ */

	ENTER( "writeArray" );
	/* warning Will Rogers */
	sprintf( message, "::::RXTXPort:writeArray(%s);\n", (char *) body );
	report_verbose( message );

	/*
	   jobject and env are passed to the thread_write code above for
	   systems that do not have access to the LSR used to detect if
	   output outputbuffer is empty

	   win32 implements its own write in termios.c
	
	   other systems use the API write directly
	*/

	do {
#ifdef TIOCSERGETLSR
		result=WRITE (fd, body + total + offset, count - total); /* dima */
#else
		result=spawn_write_thread (fd, body + total + offset,
						count - total, env, &jobj);
#endif /* TIOCSERGETLSR */
		if(result >0){
			total += result;
		}
	}  while ( ( total < count ) || (result < 0 && errno==EINTR ) );
	(*env)->ReleaseByteArrayElements( env, jbarray, body, 0 );
	/*
		50 ms sleep to make sure read can get in

		what I think is happening here is the data writen is causing
		signals, the event loop can't select with data available

		I think things like BlackBox with 2 ports open are getting
		signals for both the reciever and transmitter since they
		are the same PID.

		Things just start spinning out of control after that.
	*/
	LEAVE( "RXTXPort:writeArray" );
	if( result < 0 ) throw_java_exception( env, IO_EXCEPTION,
		"writeArray", strerror( errno ) );
}

/*----------------------------------------------------------
RXTXPort.nativeDrain

   accept:      none
   perform:     wait until all data is transmitted
   return:      none
   exceptions:  IOException
   comments:    java.io.OutputStream.flush() is equivalent to tcdrain,
                not tcflush, which throws away unsent bytes

                count logic added to avoid infinite loops when EINTR is
                true...  Thread.yeild() was suggested.
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(nativeDrain)( JNIEnv *env,
	jobject jobj )
{
	int fd = get_java_var( env, jobj,"fd","I" );
	int result, count=0;

	char message[80];

	ENTER( "SerialImp.c:drain()" );
	do {
		report_verbose( "trying tcdrain\n" );
		result=tcdrain(fd);
		count++;
	}  while (result && errno==EINTR && count <3);

	sprintf( message, "RXTXPort:drain() returns: %i\n", result ); 
	report_verbose( message );
	LEAVE( "RXTXPort:drain()" );
	if( result ) throw_java_exception( env, IO_EXCEPTION, "nativeDrain",
		strerror( errno ) );
	return;
}

/*----------------------------------------------------------
RXTXPort.sendBreak

   accept:     duration in milliseconds.
   perform:    send break for actual time.  not less than 0.25 seconds.
   exceptions: none
   comments:   not very precise
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(sendBreak)( JNIEnv *env,
	jobject jobj, jint duration )
{
	int fd = get_java_var( env, jobj,"fd","I" );
	ENTER( "RXTXPort:sendBreak()" );
	tcsendbreak( fd, (int)( duration / 250 ) );
	LEAVE( "RXTXPort:sendBreak()" );
}

/*----------------------------------------------------------
RXTXPort.NativegetReceiveTimeout

   accept:     none
   perform:    get termios.c_cc[VTIME]
   return:     VTIME
   comments:   see  NativeEnableReceiveTimeoutThreshold
----------------------------------------------------------*/
JNIEXPORT jint JNICALL RXTXPort(NativegetReceiveTimeout)(
	JNIEnv *env,
	jobject jobj
	)
{
	int fd = get_java_var( env, jobj,"fd","I" );
	struct termios ttyset;

	ENTER( "RXTXPort:nativegetRecieveTimeout()" );
	if( tcgetattr( fd, &ttyset ) < 0 ) goto fail;
	LEAVE( "RXTXPort:nativegetRecieveTimeout()" );
	return(ttyset.c_cc[ VTIME ] * 100);
fail:
	LEAVE( "RXTXPort:nativegetRecieveTimeout()" );
	throw_java_exception( env, IO_EXCEPTION, "getReceiveTimeout",
		strerror( errno ) );
	return -1;
}

/*----------------------------------------------------------
RXTXPort.NativeisReceiveTimeoutEnabled

   accept:     none
   perform:    determine if VTIME is none 0
   return:     JNI_TRUE if VTIME > 0 else JNI_FALSE
   comments:   see  NativeEnableReceiveTimeoutThreshold
----------------------------------------------------------*/
JNIEXPORT jboolean JNICALL RXTXPort(NativeisReceiveTimeoutEnabled)(
	JNIEnv *env,
	jobject jobj
	)
{
	int fd = get_java_var( env, jobj,"fd","I" );
	struct termios ttyset;
	ENTER( "RXTXPort:NativeisRecieveTimeoutEnabled()" );
	if( tcgetattr( fd, &ttyset ) < 0 ) goto fail;
	LEAVE( "RXTXPort:NativeisRecieveTimeoutEnabled()" );
	return(ttyset.c_cc[ VTIME ] > 0 ? JNI_TRUE:JNI_FALSE);
fail:
	LEAVE( "RXTXPort:NativeisRecieveTimeoutEnabled()" );
	throw_java_exception( env, IO_EXCEPTION, "isReceiveTimeoutEnabled",
		strerror( errno ) );
	return JNI_FALSE;
}

/*----------------------------------------------------------
RXTXPort.isDSR

   accept:      none
   perform:     check status of DSR
   return:      true if TIOCM_DSR is set
                false if TIOCM_DSR is not set
   exceptions:  none
   comments:    DSR stands for Data Set Ready
----------------------------------------------------------*/
JNIEXPORT jboolean JNICALL RXTXPort(isDSR)( JNIEnv *env,
	jobject jobj )
{
	unsigned int result = 0;
	int fd = get_java_var( env, jobj,"fd","I" );
	char message[80];

	ENTER( "RXTXPort:isDSR" );
	ioctl( fd, TIOCMGET, &result );
	sprintf( message, "RXTXPort:isDSR returns %i\n", result & TIOCM_DSR );
	report( message );
	LEAVE( "RXTXPort:isDSR" );
	if( result & TIOCM_DSR ) return JNI_TRUE;
	else return JNI_FALSE;
}

/*----------------------------------------------------------
RXTXPort.isCD

   accept:      none
   perform:     check status of CD
   return:      true if TIOCM_CD is set
                false if TIOCM_CD is not set
   exceptions:  none
   comments:    CD stands for Carrier Detect
                The following comment has been made...
                "well, it works, there might ofcourse be a bug, but making DCD
                permanently on fixed it for me so I don't care"

----------------------------------------------------------*/
JNIEXPORT jboolean JNICALL RXTXPort(isCD)( JNIEnv *env,
	jobject jobj )
{
	unsigned int result = 0;
	int fd = get_java_var( env, jobj,"fd","I" );
	char message[80];

	ENTER( "RXTXPort:isCD" );
	ioctl( fd, TIOCMGET, &result );
	sprintf( message, "RXTXPort:isCD returns %i\n", result & TIOCM_CD );
	LEAVE( "RXTXPort:isCD" );
	if( result & TIOCM_CD ) return JNI_TRUE;
	else return JNI_FALSE;
}

/*----------------------------------------------------------
RXTXPort.isCTS

   accept:      none
   perform:     check status of CTS
   return:      true if TIOCM_CTS is set
                false if TIOCM_CTS is not set
   exceptions:  none
   comments:    CTS stands for Clear To Send.
----------------------------------------------------------*/
JNIEXPORT jboolean JNICALL RXTXPort(isCTS)( JNIEnv *env,
	jobject jobj )
{
	unsigned int result = 0;
	int fd = get_java_var( env, jobj,"fd","I" );
	char message[80];

	ENTER( "RXTXPort:isCTS" );
	ioctl( fd, TIOCMGET, &result );
	sprintf( message, "RXTXPort:isCTS returns %i\n", result & TIOCM_CTS );
	report( message );
	LEAVE( "RXTXPort:isCTS" );
	if( result & TIOCM_CTS ) return JNI_TRUE;
	else return JNI_FALSE;
}

/*----------------------------------------------------------
RXTXPort.isRI

   accept:      none
   perform:     check status of RI
   return:      true if TIOCM_RI is set
                false if TIOCM_RI is not set
   exceptions:  none
   comments:    RI stands for Ring Indicator
----------------------------------------------------------*/
JNIEXPORT jboolean JNICALL RXTXPort(isRI)( JNIEnv *env,
	jobject jobj )
{
	unsigned int result = 0;
	int fd = get_java_var( env, jobj,"fd","I" );
	char message[80];

	ENTER( "RXTXPort:isRI" );
	ioctl( fd, TIOCMGET, &result );
	sprintf( message, "RXTXPort:isRI returns %i\n", result & TIOCM_RI );
	report( message );
	LEAVE( "RXTXPort:isRI" );
	if( result & TIOCM_RI ) return JNI_TRUE;
	else return JNI_FALSE;
}

/*----------------------------------------------------------
RXTXPort.isRTS

   accept:      none
   perform:     check status of RTS
   return:      true if TIOCM_RTS is set
                false if TIOCM_RTS is not set
   exceptions:  none
   comments:    tcgetattr with c_cflag CRTS_IFLOW
----------------------------------------------------------*/
JNIEXPORT jboolean JNICALL RXTXPort(isRTS)( JNIEnv *env,
	jobject jobj )
{
	unsigned int result = 0;
	int fd = get_java_var( env, jobj,"fd","I" );
	char message[80];

	ENTER( "RXTXPort:isRTS" );
	ioctl( fd, TIOCMGET, &result );
	sprintf( message, "RXTXPort:isRTS returns %i\n", result & TIOCM_RTS );
	report( message );
	LEAVE( "RXTXPort:isRTS" );
	if( result & TIOCM_RTS ) return JNI_TRUE;
	else return JNI_FALSE;
}

/*----------------------------------------------------------
RXTXPort.setRTS

   accept:      state  flag to set/unset.
   perform:     depends on the state flag
                if true TIOCM_RTS is set
                if false TIOCM_RTS is unset
   return:      none
   exceptions:  none
   comments:    tcsetattr with c_cflag CRTS_IFLOW
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(setRTS)( JNIEnv *env,
	jobject jobj, jboolean state )
{
	unsigned int result = 0;
	int fd = get_java_var( env, jobj,"fd","I" );
	char message[80];

	ENTER( "RXTXPort:setRTS" );
	ioctl( fd, TIOCMGET, &result );
	if( state == JNI_TRUE ) result |= TIOCM_RTS;
	else result &= ~TIOCM_RTS;
	ioctl( fd, TIOCMSET, &result );
	if( state == JNI_TRUE ) result |= TIOCM_DSR;
	sprintf( message, "setRTS( %i )\n", state );
	report( message );
	LEAVE( "RXTXPort:setRTS" );
	return;
}

/*----------------------------------------------------------
RXTXPort.setDSR

   accept:      state  flag to set/unset.
   perform:     depends on the state flag
                if true TIOCM_DSR is set
                if false TIOCM_DSR is unset
   return:      none
   exceptions:  none
   comments:    tcsetattr with c_cflag CRTS_IFLOW
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(setDSR)( JNIEnv *env,
	jobject jobj, jboolean state )
{
	unsigned int result = 0;
	int fd = get_java_var( env, jobj,"fd","I" );
	char message[80];

	ENTER( "RXTXPort:setDSR()" );
	ioctl( fd, TIOCMGET, &result );
	
	sprintf( message, "setDSR( %i )\n", state );
	if( state == JNI_TRUE ) result |= TIOCM_DSR;
	else result &= ~TIOCM_DSR;
	ioctl( fd, TIOCMSET, &result );
	sprintf( message, "setDSR( %i )\n", state );
	report( message );
	LEAVE( "RXTXPort:setDSR()" );
	return;
}

/*----------------------------------------------------------
RXTXPort.isDTR

   accept:      none
   perform:     check status of DTR
   return:      true if TIOCM_DTR is set
                false if TIOCM_DTR is not set
   exceptions:  none
   comments:    DTR stands for Data Terminal Ready
----------------------------------------------------------*/
JNIEXPORT jboolean JNICALL RXTXPort(isDTR)( JNIEnv *env,
	jobject jobj )
{
	unsigned int result = 0;
	int fd = get_java_var( env, jobj,"fd","I" );
	char message[80];

	ENTER( "RXTXPort:isDTR" );
	ioctl( fd, TIOCMGET, &result );
	sprintf( message, "iSDTR( ) returns %i\n", result& TIOCM_DTR );
	report( message );
	LEAVE( "RXTXPort:isDTR" );
	if( result & TIOCM_DTR ) return JNI_TRUE;
	else return JNI_FALSE;
}

/*----------------------------------------------------------
RXTXPort.setDTR

   accept:      new DTR state
   perform:     if state is true, TIOCM_DTR is set
                if state is false, TIOCM_DTR is unset
   return:      none
   exceptions:  none
   comments:    DTR stands for Data Terminal Ready
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(setDTR)( JNIEnv *env,
	jobject jobj, jboolean state )
{
	unsigned int result = 0;
	int fd = get_java_var( env, jobj,"fd","I" );
	char message[80];

	ENTER( "RXTXPort:setDTR" );
	ioctl( fd, TIOCMGET, &result );
	if( state == JNI_TRUE ) result |= TIOCM_DTR;
	else result &= ~TIOCM_DTR;
	ioctl( fd, TIOCMSET, &result );
	sprintf( message, "setDTR( %i )\n", state );
	report( message );
	LEAVE( "RXTXPort:setDTR" );
	return;
}

/*----------------------------------------------------------
RXTXPort.nativeGetParityErrorChar

   accept:      -
   perform:     check the ParityErrorChar
   return:      The ParityErrorChar as an int.
   exceptions:  UnsupportedCommOperationException if not implemented
   comments:    It appears the Parity char is usually \0.  The windows
		API allows for this to be changed.  I cant find may
		examples of this being done.  Maybe for a reason.

		Use a direct call to the termios file until we find a
		solution.
----------------------------------------------------------*/
JNIEXPORT jint JNICALL RXTXPort(nativeGetParityErrorChar)( JNIEnv *env,
	jobject jobj )
{
	unsigned int result = 0;

	ENTER( "nativeGetParityErrorChar" );
#ifdef WIN32
	result = ( jint ) termiosGetParityErrorChar(
			get_javavar(env, jobj, "fd", "I" ) );
#else
	/*
	   arg!  I cant find a way to change it from \0 in Linux.  I think
		   the frame and parity error characters are hardcoded.
	*/
		result = ( jint ) '\0';

#endif /* WIN32 */
	LEAVE( "nativeGetParityErrorChar" );
	return( ( jint ) result );
}

/*----------------------------------------------------------
RXTXPort.nativeGetEndOfInputChar

   accept:      -
   perform:     check the EndOf InputChar
   return:      the EndOfInputChar as an int.  -1 on error
   exceptions:  UnsupportedCommOperationException if not implemented
   comments:    
----------------------------------------------------------*/
JNIEXPORT jint JNICALL RXTXPort(nativeGetEndOfInputChar)( JNIEnv *env,
	jobject jobj )
{
	int fd = get_java_var( env, jobj,"fd","I" );
	struct termios ttyset;

	ENTER( "nativeGetEndOfInputChar" );
	if( tcgetattr( fd, &ttyset ) < 0 ) goto fail;
	LEAVE( "nativeGetEndOfInputChar" );
	return( (jint) ttyset.c_cc[VEOF] );
fail:
	LEAVE( "nativeGetEndOfInputChar" );
	report( "nativeGetEndOfInputChar failed\n" );
	return( ( jint ) -1 );
}

/*----------------------------------------------------------
RXTXPort.nativeSetParityErrorChar

   accept:      the ParityArrorCharacter as an int.
   perform:     Set the ParityErrorChar
   return:      JNI_TRUE on success
   exceptions:  UnsupportedCommOperationException if not implemented
   comments:    It appears the Parity char is usually \0.  The windows
		API allows for this to be changed.  I cant find may
		examples of this being done.  Maybe for a reason.

		Use a direct call to the termios file until we find a
		solution.
----------------------------------------------------------*/
JNIEXPORT jboolean JNICALL RXTXPort(nativeSetParityErrorChar)( JNIEnv *env,
	jobject jobj, jbyte value )
	{

#ifdef WIN32
		int fd = get_java_var( env, jobj,"fd","I" );
		ENTER( "nativeSetParityErrorChar" );
		termiosSetParityError( fd, ( char ) value );
		LEAVE( "nativeSetParityErrorChar" );
		return( JNI_TRUE );
#else
		ENTER( "nativeSetParityErrorChar" );
	/*
	   arg!  I cant find a way to change it from \0 in Linux.  I think
	   the frame and parity error characters are hardcoded.
	*/

	throw_java_exception( env, UNSUPPORTED_COMM_OPERATION,
		"Not implemented... yet",
		strerror( errno ) );
	LEAVE( "nativeSetParityErrorChar" );
	return( JNI_FALSE );
#endif /* WIN32 */
}

/*----------------------------------------------------------
RXTXPort.nativeSetEndOfInputChar

   accept:      The EndOfInputChar as an int
   perform:     set the EndOfInputChar
   return:      JNI_TRUE on success
   exceptions:  UnsupportedCommOperationException if not implemented
   comments:    This may cause troubles on Windows.
		Lets give it a shot and see what happens.

		See termios.c for the windows bits.

		EofChar = val;
		fBinary = false;  //winapi docs say always use true. ?
----------------------------------------------------------*/
JNIEXPORT jboolean JNICALL RXTXPort(nativeSetEndOfInputChar)( JNIEnv *env,
	jobject jobj, jbyte value )
{
	int fd = get_java_var( env, jobj,"fd","I" );
	struct termios ttyset;

	ENTER( "nativeSetEndOfInputChar" );
	if( tcgetattr( fd, &ttyset ) < 0 ) goto fail;
	ttyset.c_cc[VEOF] = ( char ) value;
	if( tcsetattr( fd, TCSANOW, &ttyset ) < 0 ) goto fail;
	LEAVE( "nativeSetEndOfInputChar" );
	return( JNI_TRUE );
fail:
	throw_java_exception( env, IO_EXCEPTION, "nativeSetEndOfInputChar",
		strerror( errno ) );
	report( "nativeSetEndOfInputChar failed\n" );
	LEAVE( "nativeSetEndOfInputChar" );
	return( JNI_FALSE );
}

/*----------------------------------------------------------
read_byte_array

   accept:      int                fd   file descriptor to read from
                unsigned char *buffer   buffer to read data into
                int            length   number of bytes to read
		int           timeout   milliseconds to wait before returning
   perform:     read bytes from the port into a buffer
   return:      status of read
                -1 fail (IOException)
                 0 timeout
                >0 number of bytes read
   comments:    According to the Communications API spec, a receive threshold
                of 1 is the same as having the threshold disabled.
		
		The nuts and bolts are documented in
		NativeEnableReceiveTimeoutThreshold()
----------------------------------------------------------*/
int read_byte_array(	JNIEnv *env,
			jobject *jobj,	
			int fd,
			unsigned char *buffer,
			int length,
			int timeout )
{
	int ret, left, bytes = 0;
	/* int count = 0; */
	fd_set rfds;
	struct timeval sleep;
	struct timeval *psleep=&sleep;

	ENTER( "read_byte_array" );
	left = length;
	FD_ZERO( &rfds );
	FD_SET( fd, &rfds );
	if( timeout != 0 )
	{
		sleep.tv_sec = timeout / 1000;
		sleep.tv_usec = 1000 * ( timeout % 1000 );
	}
	while( bytes < length )
	{
         /* FIXME: In Linux, select updates the timeout automatically, so
            other OSes will need to update it manually if they want to have
            the same behavior.  For those OSes, timeouts will occur after no
            data AT ALL is received for the timeout duration.  No big deal. */
#ifndef WIN32
		do {
			if( timeout == 0 ) psleep = NULL;
			ret=SELECT( fd + 1, &rfds, NULL, NULL, psleep );
		}  while (ret < 0 && errno==EINTR);
#else
		/*
		    the select() needs some work before the above will
		    work on win32.  The select code cannot be accessed
		    from both the Monitor Thread and the Reading Thread.

		    This keeps the cpu from racing but the select() code
		    will have to be fixed at some point.
		*/
		ret = RXTXPort(nativeavailable)( env, *jobj );
#endif /* WIN32 */
		if( ret == 0 )
		{
			report( "read_byte_array: select returned 0\n" );
			LEAVE( "read_byte_array" );
			break;
		}
		if( ret < 0 )
		{
			report( "read_byte_array: select returned -1\n" );
			LEAVE( "read_byte_array" );
			return -1;
		}
		ret = READ( fd, buffer + bytes, left );
		if( ret == 0 )
		{
			report( "read_byte_array: read returned 0 bytes\n" );
			LEAVE( "read_byte_array" );
			break;
		}
		else if( ret < 0 )
		{
			report( "read_byte_array: read returned -1\n" );
			LEAVE( "read_byte_array" );
			return -1;
		}
		bytes += ret;
		left -= ret;
	}
	LEAVE( "read_byte_array" );
	return bytes;
}

/*----------------------------------------------------------
NativeEnableReceiveTimeoutThreshold
   accept:      int  threshold, int vtime,int buffer
   perform:     Set c_cc->VMIN to threshold and c_cc=>VTIME to vtime
   return:      void
   exceptions:  IOException
   comments:    This is actually all handled in read with select in
                canonical input mode.
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(NativeEnableReceiveTimeoutThreshold)(
	JNIEnv *env, jobject jobj, jint vtime, jint threshold, jint buffer)
{
	int fd = get_java_var( env, jobj,"fd","I" );
	struct termios ttyset;

	ENTER( "RXTXPort:NativeEnableRecieveTimeoutThreshold" );
	if( tcgetattr( fd, &ttyset ) < 0 ) goto fail;
	ttyset.c_cc[ VMIN ] = threshold;
	ttyset.c_cc[ VTIME ] = vtime/100;
	if( tcsetattr( fd, TCSANOW, &ttyset ) < 0 ) goto fail;

	LEAVE( "RXTXPort:NativeEnableRecieveTimeoutThreshold" );
	return;
fail:
	LEAVE( "RXTXPort:NativeEnableRecieveTimeoutThreshold" );
	throw_java_exception( env, IO_EXCEPTION, "TimeoutThreshold",
		strerror( errno ) );
	return;
}

/*----------------------------------------------------------
RXTXPort.readByte

   accept:      none
   perform:     Read a single byte from the port
   return:      The byte read
   exceptions:  IOException
----------------------------------------------------------*/
JNIEXPORT jint JNICALL RXTXPort(readByte)( JNIEnv *env,
	jobject jobj )
{
	int bytes;
	unsigned char buffer[ 1 ];
	int fd = get_java_var( env, jobj,"fd","I" );
	int timeout = get_java_var( env, jobj, "timeout", "I" );

	ENTER( "RXTXPort:readByte" );
	bytes = read_byte_array( env, &jobj, fd, buffer, 1, timeout );
	if( bytes < 0 ) {
		LEAVE( "RXTXPort:readByte" );
		throw_java_exception( env, IO_EXCEPTION, "readByte",
			strerror( errno ) );
		return -1;
	}
	LEAVE( "RXTXPort:readByte" );
	return (bytes ? (jint)buffer[ 0 ] : -1);
}

/*----------------------------------------------------------
RXTXPort.readArray

   accept:       offset (offset to start storing data in the jbarray) and
                 Length (bytes to read)
   perform:      read bytes from the port into a byte array
   return:       bytes read on success
                 0 on read timeout
   exceptions:   IOException
   comments:     throws ArrayIndexOutOfBoundsException if asked to
                 read more than SSIZE_MAX bytes
----------------------------------------------------------*/
JNIEXPORT jint JNICALL RXTXPort(readArray)( JNIEnv *env,
	jobject jobj, jbyteArray jbarray, jint offset, jint length )
{
	int bytes;
	jbyte *body;
	int fd = get_java_var( env, jobj, "fd", "I" );
	int timeout = get_java_var( env, jobj, "timeout", "I" );

	ENTER( "readArray" );
	if( length > SSIZE_MAX || length < 0 ) {
		report( "RXTXPort:readArray length > SSIZE_MAX" );
		LEAVE( "RXTXPort:readArray" );
		throw_java_exception( env, ARRAY_INDEX_OUT_OF_BOUNDS,
			"readArray", "Invalid length" );
		return -1;
	}
	body = (*env)->GetByteArrayElements( env, jbarray, 0 );
	bytes = read_byte_array( env, &jobj, fd, (unsigned char *)(body+offset), length, timeout );/* dima */
	(*env)->ReleaseByteArrayElements( env, jbarray, body, 0 );
	if( bytes < 0 ) {
		report( "RXTXPort:readArray bytes < 0" );
		LEAVE( "RXTXPort:readArray" );
		throw_java_exception( env, IO_EXCEPTION, "readArray",
			strerror( errno ) );
		return -1;
	}
	LEAVE( "RXTXPort:readArray" );
	return (bytes ? bytes : -1);
}

/*----------------------------------------------------------
RXTXPort.nativeavailable

   accept:      none
   perform:     find out the number of bytes available for reading
   return:      available bytes
                -1 on error
   exceptions:  none
----------------------------------------------------------*/
JNIEXPORT jint JNICALL RXTXPort(nativeavailable)( JNIEnv *env,
	jobject jobj )
{
	int fd = get_java_var( env, jobj,"fd","I" );
	int result;

	ENTER( "RXTXPort:nativeavailable" );
	if( ioctl( fd, FIONREAD, &result ) )
	{
#ifdef __unixware__
	/*
	    On SCO OpenServer FIONREAD always fails for serial devices,
	    so try ioctl FIORDCHK instead; will only tell us whether
	    bytes are available, not how many, but better than nothing.
	*/
		result = ioctl(fd, FIORDCHK, 0);
		sprintf(message, "    FIORDCHK result %d, errno %d\n", result , result == -1 ? errno : 0);
		report( message );
		if (result == -1) {
#endif /* __unixware__ */
			LEAVE( "RXTXPort:nativeavailable IOException" );
			throw_java_exception( env, IO_EXCEPTION,
						"nativeavailable",
			strerror( errno ) );
			return -1;
#ifdef __unixware__
		} else
		{
			LEAVE( "RXTXPort:nativeavailable" );
			return (jint)result;
		}
#endif /* __unixware__ */
	}
	LEAVE( "RXTXPort:nativeavailable" );
	return (jint)result;
}

/*----------------------------------------------------------
RXTXPort.setflowcontrol

   accept:      flowmode
	FLOWCONTROL_NONE        none
	FLOWCONTROL_RTSCTS_IN   hardware flow control
	FLOWCONTROL_RTSCTS_OUT         ""
	FLOWCONTROL_XONXOFF_IN  input software flow control
	FLOWCONTROL_XONXOFF_OUT output software flow control
   perform:     set flow control to flowmode
   return:      none
   exceptions:  UnsupportedCommOperationException
   comments:  there is no differentiation between input and output hardware
              flow control
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(setflowcontrol)( JNIEnv *env,
	jobject jobj, jint flowmode )
{
	struct termios ttyset;
	int fd = get_java_var( env, jobj,"fd","I" );

	ENTER( "RXTXPort:setflowcontrol" );
	if( tcgetattr( fd, &ttyset ) ) goto fail;
	
	if ( flowmode & ( FLOWCONTROL_RTSCTS_IN | FLOWCONTROL_RTSCTS_OUT ) )
		ttyset.c_cflag |= HARDWARE_FLOW_CONTROL;
	else ttyset.c_cflag &= ~HARDWARE_FLOW_CONTROL;

	ttyset.c_iflag &= ~IXANY;

	if ( flowmode & FLOWCONTROL_XONXOFF_IN )
		ttyset.c_iflag |= IXOFF;
	else ttyset.c_iflag &= ~IXOFF;

	if ( flowmode & FLOWCONTROL_XONXOFF_OUT )
		ttyset.c_iflag |= IXON;
	else ttyset.c_iflag &= ~IXON;

	if( tcsetattr( fd, TCSANOW, &ttyset ) ) goto fail;
	LEAVE( "RXTXPort:setflowcontrol" );
	return;
fail:
	LEAVE( "RXTXPort:setflowcontrol" );
	throw_java_exception( env, UNSUPPORTED_COMM_OPERATION, "",
		"flow control type not supported" );
	return;
}

/*----------------------------------------------------------
unlock_monitor_thread

   accept:      event_info_struct
   perform:     unlock the monitor thread so event notification can start.
   return:      none
   exceptions:  none
   comments:    Events can be missed otherwise.
----------------------------------------------------------*/

void unlock_monitor_thread( struct event_info_struct *eis )
{
	JNIEnv *env = eis->env;

	jfieldID jfid = (*env)->GetFieldID( env, eis->jclazz,
		"MonitorThreadLock", "Z" );
	(*env)->SetBooleanField( env, *eis->jobj, jfid, JNI_FALSE );
}

/*----------------------------------------------------------
check_line_status_register

   accept:      event_info_struct
   perform:     check for changes on the LSR
   return:      0 on success
   exceptions:  none
   comments:    not supported on all devices/drivers.
----------------------------------------------------------*/
int check_line_status_register( struct event_info_struct *eis )
{
#if defined TIOCSERGETLSR
	struct stat fstatbuf;

	if( ! eis->eventflags[SPE_OUTPUT_BUFFER_EMPTY] )
	{
		return 0;
	}
	if ( fstat( eis->fd, &fstatbuf ) )
	{
		report( "check_line_status_register: fstat\n" );
		return( 1 );
	}
	if( ioctl( eis->fd, TIOCSERGETLSR, &eis->change ) )
	{
		report( "TIOCSERGETLSR\n is nonnull\n" );
		return( 1 );
	}
	else if( eis->change )
	{
		report_verbose( "sending OUTPUT_BUFFER_EMPTY\n" );
		send_event( eis, SPE_OUTPUT_BUFFER_EMPTY, 1 );
	}
#else
	if( eis->output_buffer_empty_flag == 1 && 
		eis->eventflags[SPE_OUTPUT_BUFFER_EMPTY] )
	{
		report("sending SPE_OUTPUT_BUFFER_EMPTY\n");
		send_event( eis, SPE_OUTPUT_BUFFER_EMPTY, 1 );
		eis->output_buffer_empty_flag = 0;
	}
#endif /* TIOCSERGETLSR */
	return( 0 );
}

/*----------------------------------------------------------
has_line_status_register_access

   accept:      fd of interest
   perform:     check for access to the LSR
   return:      0 if not available
   exceptions:  none
   comments:    not supported on all devices/drivers.
		JK00: work around for multiport cards without TIOCSERGETLSR 
		Cyclades is one of those :-(
----------------------------------------------------------*/
int has_line_status_register_access( int fd )
{
#if defined(TIOCSERGETLSR)
	int change;

	if( !ioctl( fd, TIOCSERGETLSR, &change ) ) {
		return(1);
	}
#endif /* TIOCSERGETLSR */
	report( "Port does not support TIOCSERGETLSR\n" );
	return( 0 );
}

/*----------------------------------------------------------
check_cgi_count

   accept:      fd of interest
   perform:     check for access to TIOCGICOUNT
   return:      0 if not available
   exceptions:  none
   comments:    not supported on all devices/drivers.
	 *	wait for RNG, DSR, CD or CTS  but not DataAvailable
	 *      The drawback here is it never times out so if someone
	 *      reads there will be no chance to try again.
	 *      This may make sense if the program does not want to
	 *      be notified of data available or errors.
	 *	ret=ioctl(fd,TIOCMIWAIT);
----------------------------------------------------------*/
void check_cgi_count( struct event_info_struct *eis )
{
#if defined(TIOCGICOUNT)

	/* JK00: only use it if supported by this port */

	struct serial_icounter_struct sis;

	if( ioctl( eis->fd, TIOCGICOUNT, &sis ) )
	{
		report( "TIOCGICOUNT\n is not 0\n" );
		return;
	}
	while( sis.frame != eis->osis.frame ) {
		send_event( eis, SPE_FE, 1);
		eis->osis.frame++;
	}
	while( sis.overrun != eis->osis.overrun ) {
		send_event( eis, SPE_OE, 1);
		eis->osis.overrun++;
	}
	while( sis.parity != eis->osis.parity ) {
		send_event( eis, SPE_PE, 1);
		eis->osis.parity++;
	}
	while( sis.brk != eis->osis.brk ) {
		send_event( eis, SPE_BI, 1);
		eis->osis.brk++;
	}
	eis->osis = sis;
#endif /*  TIOCGICOUNT */
}

/*----------------------------------------------------------
port_has_changed_fionread

   accept:      fd of interest
   perform:     check if FIONREAD has changed
   return:      0 if no data available
   exceptions:  none
   comments:    
----------------------------------------------------------*/
int port_has_changed_fionread( int fd )
{
	int change, rc;
	char message[80];

	rc = ioctl( fd, FIONREAD, &change );
#ifdef __unixware__
	/*
	   On SCO OpenServer FIONREAD always fails for serial devices,
	   so rely upon select() result to know whether data available.
	*/
	if( (rc != -1 && change) || (rc == -1 && ret > 0) ) 
		return( 1 );
#else
	sprintf( message, "change is %i\n", change );
	report_verbose( message );
	if( change )
		return( 1 );
#endif /* __unixware__ */
	return( 0 );
		
}
		
/*----------------------------------------------------------
check_tiocmget_changes

   accept:      event_info_struct
   perform:     use TIOCMGET to report events
   return:      none
   exceptions:  none
   comments:    not supported on all devices/drivers.
----------------------------------------------------------*/
void check_tiocmget_changes( struct event_info_struct * eis )
{
	unsigned int mflags;
	int change = eis->change;

	if( ioctl( eis->fd, TIOCMGET, &mflags ) )
	{
		report( "ioctl(TIOCMGET)\n" );
		return;
	}

	change = (mflags&TIOCM_CTS) - (eis->omflags&TIOCM_CTS);
	if( change ) send_event( eis, SPE_CTS, change );

	change = (mflags&TIOCM_DSR) - (eis->omflags&TIOCM_DSR);
	if( change ) send_event( eis, SPE_DSR, change );

	change = (mflags&TIOCM_RNG) - (eis->omflags&TIOCM_RNG);
	if( change ) send_event( eis, SPE_RI, change );

	change = (mflags&TIOCM_CD) - (eis->omflags&TIOCM_CD);
	if( change ) send_event( eis, SPE_CD, change );

	eis->omflags = mflags;
}

/*----------------------------------------------------------
system_wait

   accept:      
   perform:     
   return:      
   exceptions:  none
   comments:    
----------------------------------------------------------*/
void system_wait()
{
#if defined (__sun__ )
/*
	struct timespec retspec, tspec;
	retspec.tv_sec = 0;
	retspec.tv_nsec = 100000000;
	do {
		tspec = retspec;
		nanosleep( &tspec, &retspec );
	} while( tspec.tv_nsec != 0 );
*/
#else
#ifdef TRENT_IS_HERE_DEBUGGING_THREADS
	/* On NT4 The following was observed in a intense test:
		50000   95%   179 sec
		200000  95%   193 sec
		1000000 95%   203 sec	some callback failures sometimes.
		2000000 0-95% 		callback failures.
	*/
#endif /* TRENT_IS_HERE_DEBUGGING_THREADS */
#endif /* __sun__ */
}

/*----------------------------------------------------------
driver_has_tiocgicount

   accept:      fd of interest
   perform:     check for access to TIOCGICOUNT
   return:      0 if not available
   exceptions:  none
   comments:    not supported on all devices/drivers.
		Some multiport serial cards do not implement TIOCGICOUNT ... 
		So use the 'dumb' mode to enable using them after all! JK00
----------------------------------------------------------*/
int driver_has_tiocgicount( struct event_info_struct * eis )
{
#if defined(TIOCGICOUNT)

	/* Some multiport serial cards do not implement TIOCGICOUNT ... */
	/* So use the 'dumb' mode to enable using them after all! JK00 */

	if( ioctl( eis->fd, TIOCGICOUNT, &eis->osis ) < 0 ) {
		report_verbose( "Port does not support TIOCGICOUNT events\n" );
		return(0);
	}
	else
		return(1);
#endif /*  TIOCGICOUNT */
	return(0);
	
}

/*----------------------------------------------------------
report_serial_events

   accept:      event_info_struct
   perform:     send events if they occured
   return:      0 if not available
   exceptions:  none
   comments:    not supported on all devices/drivers.
----------------------------------------------------------*/
void report_serial_events( struct event_info_struct *eis )
{
	/* JK00: work around for Multi IO cards without TIOCSERGETLSR */
	/* if( eis->has_tiocsergetlsr ) we have a fix for output empty */
		if( check_line_status_register( eis ) )
			return;

	if ( eis->has_tiocgicount )
		check_cgi_count( eis );

	check_tiocmget_changes( eis );

	if(!eis->eventflags[SPE_DATA_AVAILABLE] )
		return;

	if( port_has_changed_fionread( eis->fd ) )
	{
		if(!send_event( eis, SPE_DATA_AVAILABLE, 1 ))
		{
			/* select wont block */
			system_wait();
		}
	}
}

/*----------------------------------------------------------
initialise_event_info_struct

   accept:      event_info_struct for this thread.
   perform:     initialise or reset the event_info_struct
   return:      1 on success
   exceptions:  none
   comments:    
----------------------------------------------------------*/
int initialise_event_info_struct( struct event_info_struct *eis )
{
	int i;
	jobject jobj = *eis->jobj;
	JNIEnv *env = eis->env;
	struct event_info_struct *index = master_index;

	if ( eis->initialised == 1 )
		goto end;

	if( index )
	{
		while( index->next )
			index = index->next;
		index->next = eis;
		eis->prev = index;
		eis->next = NULL;
	}
	else
	{
		master_index = eis;
		master_index->next = NULL;
		master_index->prev = NULL;
	}

	for( i = 0; i < 11; i++ ) eis->eventflags[i] = 0;
	eis->output_buffer_empty_flag = 0;

	eis->fd = get_java_var( env, jobj, "fd", "I" );
	eis->has_tiocsergetlsr = has_line_status_register_access( eis->fd );
	eis->has_tiocgicount = driver_has_tiocgicount( eis );

	eis->jclazz = (*env)->GetObjectClass( env, jobj );
	if(eis->jclazz == NULL)
		goto fail;
	if( ioctl( eis->fd, TIOCMGET, &eis->omflags) < 0 ) {
		report( "Port does not support events\n" );
	}

	eis->send_event = (*env)->GetMethodID( env, eis->jclazz, "sendEvent",
		"(IZ)Z" );
	if(eis->send_event == NULL)
		goto fail;
#ifdef oldcode
	eis->checkMonitorThread = (*env)->GetMethodID( env, eis->jclazz,
		"checkMonitorThread", "()Z" );
	if(eis->checkMonitorThread == NULL) goto fail;
#endif /* oldcode */
	eis->eventloop_interrupted = 0;
end:
	FD_ZERO( &eis->rfds );
	FD_SET( eis->fd, &eis->rfds );
	eis->tv_sleep.tv_sec = 0;
	eis->tv_sleep.tv_usec = 1000;
	eis->initialised = 1;
	return( 1 );
fail:
	report_error("initialise failed!\n");
	finalize_event_info_struct( eis );
	return( 0 );
}

/*----------------------------------------------------------
finalize_event_info_struct

   accept:      event_info_struct for this thread.
   perform:     free resources
   return:      none
   exceptions:  none
   comments:    
----------------------------------------------------------*/
void finalize_event_info_struct( struct event_info_struct *eis )
{
	
	if( eis->jclazz)
	{
		(*eis->env)->DeleteLocalRef( eis->env, eis->jclazz );
	}
	if( eis->next && eis->prev ) 
	{
		eis->prev->next = eis->next;
		eis->next->prev = eis->prev;
	}
	else if( eis->next )
	{
		eis->next->prev = NULL;
		master_index = eis->next;
	}
	else if( eis->prev )
		eis->prev->next = NULL;
	else master_index = NULL;
}

/*----------------------------------------------------------
RXTXPort.eventLoop

   accept:      none
   perform:     periodically check for SerialPortEvents
   return:      none
   exceptions:  none
   comments:	please keep this function clean.
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(eventLoop)( JNIEnv *env, jobject jobj )
{
	struct event_info_struct eis;
	eis.env = env;
	eis.jobj = &jobj;
	eis.initialised = 0;

	report( ">RXTXPort:eventLoop\n" );
	if ( !initialise_event_info_struct( &eis ) ) goto end;
	if ( !init_thread_write( &eis ) ) goto end;
	unlock_monitor_thread( &eis );
	do{
		/* report( "." ); */
		do {
			eis.ret = SELECT( eis.fd + 1, &eis.rfds, NULL, NULL,
					&eis.tv_sleep );
			/* nothing goes between this call and select */
			if( eis.eventloop_interrupted )
			{
				report("got interrupt\n");
				finalize_thread_write( &eis );
				finalize_event_info_struct( &eis );
				return;
			}
			usleep(1000);
		}  while ( eis.ret < 0 && errno == EINTR );
		if( eis.ret >= 0 )
			report_serial_events( &eis );
		initialise_event_info_struct( &eis );
	} while( 1 );
end:
	report( "<RXTXPort:eventLoop\n" );
}

/*----------------------------------------------------------
RXTXCommDriver.testRead

   accept:      tty_name The device to be tested
   perform:     test if the device can be read from
   return:      JNI_TRUE if the device can be read from
   exceptions:  none
   comments:    From Wayne Roberts wroberts1@home.com
   		check tcget/setattr returns.
		support for non serial ports Trent
----------------------------------------------------------*/

JNIEXPORT jboolean  JNICALL RXTXCommDriver(testRead)(
	JNIEnv *env,
	jobject jobj,
	jstring tty_name,
	jint port_type
)
{
	struct termios ttyset;
	char c;
#ifdef TRENT_IS_HERE_DEBUGGING_ENUMERATION
	char message[80];
#endif /* TRENT_IS_HERE_DEBUGGING_ENUMERATION */
	int fd;
	const char *name = (*env)->GetStringUTFChars(env, tty_name, 0);
	int ret = JNI_TRUE;
	int pid = -1;

	ENTER( "RXTXPort:testRead" );
#ifdef TRENT_IS_HERE_DEBUGGING_ENUMERATION
	/* vmware lies about which ports are there causing irq conflicts */
	/* this is for testing only */
	if( !strcmp( name, "COM1" ) )
	{
		sprintf( message, " %s is good!\n", name );
		report( message );
		return( JNI_TRUE );
	}
	return( JNI_FALSE );
#endif /* TRENT_IS_HERE_DEBUGGING_ENUMERATION */

	/* 
		LOCK is one of three functions defined in SerialImp.h

			uucp_lock		Solaris
			fhs_lock		Linux
			system_does_not_lock	Win32
	*/

	if ( LOCK( name ) )
	{
		(*env)->ReleaseStringUTFChars(env, tty_name, name);
		LEAVE( "RXTXPort:testRead no lock" );
		return JNI_FALSE;
	}

	/* CLOCAL eliminates open blocking on modem status lines */
	if ((fd = OPEN(name, O_RDONLY | CLOCAL)) <= 0)
	{
		report_verbose( "testRead() open failed\n" );
		ret = JNI_FALSE;
		goto END;
	}
/*
	do {
		fd=OPEN ( name, O_RDWR | O_NOCTTY | O_NONBLOCK );
	}  while ( fd < 0 && errno==EINTR );
*/
	if( fd < 0 )
	{
		ret = JNI_FALSE;
		goto END;
	}

	if ( port_type == PORT_SERIAL )
	{
		int saved_flags;
		struct termios saved_termios;

		if (tcgetattr(fd, &ttyset) < 0) {
			ret = JNI_FALSE;
			goto END;
		}

		/* save, restore later */
		if ( ( saved_flags = fcntl(fd, F_GETFL ) ) < 0 )
		{
			report( "testRead() fcntl(F_GETFL) failed\n" );
			ret = JNI_FALSE;
			goto END;
		}

		memcpy( &saved_termios, &ttyset, sizeof( struct termios ) );

		if ( fcntl( fd, F_SETFL, O_NONBLOCK ) < 0 )
		{
			report( "testRead() fcntl(F_SETFL) failed\n" );
			ret = JNI_FALSE;
			goto END;
		}

		cfmakeraw(&ttyset);
		ttyset.c_cc[VMIN] = ttyset.c_cc[VTIME] = 0;

		if ( tcsetattr( fd, TCSANOW, &ttyset) < 0 )
		{
			report( "testRead() tcsetattr failed\n" );
			ret = JNI_FALSE;
			tcsetattr( fd, TCSANOW, &saved_termios );
			goto END;
		}
		if ( READ( fd, &c, 1 ) < 0 )
		{
#ifdef EWOULDBLOCK
			if ( errno != EWOULDBLOCK )
			{
				report( "testRead() read failed\n" );
				ret = JNI_FALSE;
			}
#else
			ret = JNI_FALSE;
#endif /* EWOULDBLOCK */
		}

		/* dont walk over unlocked open devices */
		tcsetattr( fd, TCSANOW, &saved_termios );
		fcntl( fd, F_SETFL, saved_flags );
	}

	/* 
		UNLOCK is one of three functions defined in SerialImp.h

			uucp_unlock		Solaris
			fhs_unlock		Linux
			system_does_not_unlock	Win32
	*/

END:
	/* We opened the file in this thread, use this pid to unlock */
#ifndef WIN32
	pid = getpid();
#endif /* WIN32 */
	UNLOCK(name, pid );
	(*env)->ReleaseStringUTFChars( env, tty_name, name );
	CLOSE( fd );
	LEAVE( "RXTXPort:testRead" );
	return ret;
}

#if defined(__APPLE__)
/*----------------------------------------------------------
 createSerialIterator()
   accept:      
   perform:     
   return:      
   exceptions:  
   comments:
		Code courtesy of Eric Welch at Keyspan, except for the bugs
		which are courtesy of Joseph Goldstone (joseph@lp.com)
----------------------------------------------------------*/

kern_return_t
createSerialIterator(io_iterator_t *serialIterator)
{
    kern_return_t    kernResult;
    mach_port_t        masterPort;
    CFMutableDictionaryRef    classesToMatch;
    if ((kernResult=IOMasterPort(NULL, &masterPort)) != KERN_SUCCESS)
    {
	printf( "IOMasterPort returned %d\n", kernResult);
	return kernResult;
    }
    if ((classesToMatch = IOServiceMatching(kIOSerialBSDServiceValue)) == NULL)
    {
	printf( "IOServiceMatching returned NULL\n" );
	return kernResult;
    }
    CFDictionarySetValue(classesToMatch, CFSTR(kIOSerialBSDTypeKey), CFSTR(kIOSerialBSDRS232Type));
    kernResult = IOServiceGetMatchingServices(masterPort, classesToMatch, serialIterator);
    if (kernResult != KERN_SUCCESS)
    {
	printf( "IOServiceGetMatchingServices returned %d\n", kernResult);
    }
    return kernResult;
}

/*----------------------------------------------------------
 getRegistryString()

   accept:      
   perform:     
   return:      
   exceptions:  
   comments:
		Code courtesy of Eric Welch at Keyspan, except for the bugs
		which are courtesy of Joseph Goldstone (joseph@lp.com)
----------------------------------------------------------*/
char *
getRegistryString(io_object_t sObj, char *propName)
{
    static char resultStr[256];
    CFTypeRef   nameCFstring;
    resultStr[0] = 0;
    nameCFstring = IORegistryEntryCreateCFProperty(sObj,
            CFStringCreateWithCString(kCFAllocatorDefault, propName, kCFStringEncodingASCII),
                                                   kCFAllocatorDefault, 0);
    if (nameCFstring)
    {
        CFStringGetCString(nameCFstring, resultStr, sizeof(resultStr), kCFStringEncodingASCII);
        CFRelease(nameCFstring);
    }
    return resultStr;
}

/*----------------------------------------------------------
 registerKnownSerialPorts()
   accept:      
   perform:     
   return:      
   exceptions:  
   comments:
----------------------------------------------------------*/
int
registerKnownSerialPorts(JNIEnv *env, jobject jobj, jint portType) /* dima */
{
    io_iterator_t    theSerialIterator;
    io_object_t      theObject;
    int              numPorts = 0;/* dima it should initiated */
    if (createSerialIterator(&theSerialIterator) != KERN_SUCCESS)
    {
        printf( "createSerialIterator failed\n" );
    } else {
	jclass cls; /* dima */
	jmethodID mid; /* dima */
        cls = (*env)->FindClass(env,"gnu/io/CommPortIdentifier" ); /* dima */
        if (cls == 0) { /* dima */
            report( "can't find class of gnu/io/CommPortIdentifier\n" ); /* dima */
            return numPorts; /* dima */
        } /* dima */
        mid = (*env)->GetStaticMethodID(env, cls, "addPortName", "(Ljava/lang/String;ILgnu/io/CommDriver;)V" ); /* dima */

        if (mid == 0) {
            printf( "getMethodID of CommDriver.addPortName failed\n" );
        } else {
            while (theObject = IOIteratorNext(theSerialIterator))
            {
 /* begin dima */
            	jstring	tempJstring;
				tempJstring = (*env)->NewStringUTF(env,getRegistryString(theObject, kIODialinDeviceKey));
                (*env)->CallStaticVoidMethod(env, cls, mid,tempJstring,portType,jobj);/* dima */
 				(*env)->DeleteLocalRef(env,tempJstring);
                numPorts++;

 				tempJstring = (*env)->NewStringUTF(env,getRegistryString(theObject, kIOCalloutDeviceKey));
               (*env)->CallStaticVoidMethod(env, cls, mid,tempJstring,portType,jobj);/* dima */
 				(*env)->DeleteLocalRef(env,tempJstring);
                numPorts++;
/* end dima */
            }
        }
    }
    return numPorts;
}
#endif /* __APPLE__ */

/*----------------------------------------------------------
 registerKnownPorts

   accept:      the type of port
   perform:     register any ports of the desired type a priori known to this OS
   return:      JNI_TRUE if any such ports were registered otherwise JNI_FALSE
   exceptions:  none
   comments:
----------------------------------------------------------*/
JNIEXPORT jboolean JNICALL RXTXCommDriver(registerKnownPorts)(JNIEnv *env,
    jobject jobj, jint portType)
{
	enum {PORT_TYPE_SERIAL = 1,
		PORT_TYPE_PARALLEL,
		PORT_TYPE_I2C,
		PORT_TYPE_RS485,
		PORT_TYPE_RAW};
	jboolean result = JNI_FALSE;
	char message[80];

	switch(portType) {
		case PORT_TYPE_SERIAL:
#if defined(__APPLE__)
			if (registerKnownSerialPorts(env, jobj,
				PORT_TYPE_SERIAL) > 0) {/* dima */
				result = JNI_TRUE;
			}
#endif
           		 break;
		case PORT_TYPE_PARALLEL: break;
		case PORT_TYPE_I2C:      break;
		case PORT_TYPE_RS485:    break;
		case PORT_TYPE_RAW:      break;
		default:
			sprintf( message, "unknown portType %d handed to \
				native RXTXCommDriver.registerKnownPorts() \
				 method.\n",
				(int) portType
			);
			report( message );
	}
	return result;
}
    
/*----------------------------------------------------------
 isPortPrefixValid

   accept:      a port prefix
   perform:     see if the port prefix matches a port that is valid on this OS.
   return:      JNI_TRUE if it exists otherwise JNI_FALSE
   exceptions:  none
   comments:
----------------------------------------------------------*/
JNIEXPORT jboolean  JNICALL RXTXCommDriver(isPortPrefixValid)(JNIEnv *env,
	jobject jobj, jstring tty_name)
{
	jboolean result;
	static struct stat mystat;
	char teststring[256];
	int fd,i;
	const char *name = (*env)->GetStringUTFChars(env, tty_name, 0);

	ENTER( "RXTXCommDriver:isPortPrefixValid" );
	for(i=0;i<64;i++){
#if defined(__sun__)
		/* Solaris uses /dev/cua/a instead of /dev/cua0 */
		if( i > 25 ) break;
		sprintf(teststring,"%s%s%c",DEVICEDIR, name, i + 97 );
		fprintf(stderr, "testing: %s\n", teststring);
#else 
#if defined(_GNU_SOURCE)
		snprintf(teststring, 256, "%s%s%i",DEVICEDIR,name, i);
#else
		sprintf(teststring,"%s%s%i",DEVICEDIR,name, i);
#endif /* _GNU_SOURCE */
		stat(teststring,&mystat);
#endif /* __sun__ */
/* XXX the following hoses freebsd when it tries to open the port later on */
#ifndef __FreeBSD__
		if(S_ISCHR(mystat.st_mode)){
			fd=OPEN(teststring,O_RDONLY|O_NONBLOCK);
			if (fd>0){
				CLOSE(fd);
				result=JNI_TRUE;
				break;
			}
			else
				result=JNI_FALSE;
		}
		else
			result=JNI_FALSE;
#else
		result=JNI_TRUE;
#endif  /* __FreeBSD __ */
	}
#if defined(_GNU_SOURCE)
	snprintf(teststring, 256, "%s%s",DEVICEDIR,name);
#else
	sprintf(teststring,"%s%s",DEVICEDIR,name);
#endif /* _GNU_SOURCE */
	stat(teststring,&mystat);
	if(S_ISCHR(mystat.st_mode)){
		fd=OPEN(teststring,O_RDONLY|O_NONBLOCK);
		if (fd>0){
			CLOSE(fd);
			result=JNI_TRUE;
		}
	}
	(*env)->ReleaseStringUTFChars(env, tty_name, name);
	LEAVE( "RXTXCommDriver:isPortPrefixValid" );
	return(result);
}

/*----------------------------------------------------------
 getDeviceDirectory

   accept:      
   perform:     
   return:      the directory containing the device files
   exceptions:  
   comments:    use this to avoid hard coded "/dev/"
   		values are in SerialImp.h
----------------------------------------------------------*/

JNIEXPORT jstring  JNICALL RXTXCommDriver(getDeviceDirectory)(JNIEnv *env,
	jobject jobj)
{
	ENTER( "RXTXCommDriver:getDeviceDirectory" );
	return (*env)->NewStringUTF(env, DEVICEDIR);
	LEAVE( "RXTXCommDriver:getDeviceDirectory" );
}

/*----------------------------------------------------------
 setInputBufferSize

   accept:      
   perform:     
   return:      none
   exceptions:  none
   comments:    see fopen/fclose/fwrite/fread man pages.
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(setInputBufferSize)(JNIEnv *env,
	jobject jobj,  jint size )
{
	report( "setInputBufferSize is not implemented\n" );
}

/*----------------------------------------------------------
 getIputBufferSize

   accept:      
   perform:     
   return:      none
   exceptions:  none
   comments:    see fopen/fclose/fwrite/fread man pages.
----------------------------------------------------------*/
JNIEXPORT jint JNICALL RXTXPort(getInputBufferSize)(JNIEnv *env,
	jobject jobj)
{
	report( "getInputBufferSize is not implemented\n" );
	return(1);
}

/*----------------------------------------------------------
 setOutputBufferSize

   accept:      
   perform:     
   return:      none
   exceptions:  none
   comments:    see fopen/fclose/fwrite/fread man pages.
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(setOutputBufferSize)(JNIEnv *env,
	jobject jobj, jint size )
{
	report( "setOutputBufferSize is not implemented\n" );
}

/*----------------------------------------------------------
 getOutputBufferSize

   accept:      
   perform:     
   return:      none
   exceptions:  none
   comments:    see fopen/fclose/fwrite/fread man pages.
----------------------------------------------------------*/
JNIEXPORT jint JNICALL RXTXPort(getOutputBufferSize)(JNIEnv *env,
	jobject jobj)
{
	report( "getOutputBufferSize is not implemented\n" );
	return(1);
}

/*----------------------------------------------------------
 interruptEventLoop

   accept:      nothing
   perform:     increment eventloop_interrupted 
   return:      nothing
   exceptions:  none
   comments:    all eventloops in this PID will check if their thread
		is interrupted.  When all the interrupted threads exit
		they will decrement the var leaving it 0.
		the remaining threads will continue.
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(interruptEventLoop)(JNIEnv *env,
	jobject jobj)
{
	struct event_info_struct *index = master_index;
	int fd = get_java_var( env, jobj, "fd", "I" );
	int searching = 1;


	while( searching )
	{
		index = master_index;
		if( index )
		{
			while( index->fd != fd &&
				index->next ) index = index->next;
			if ( index->fd == fd ) searching = 0;
		}
		else
			report("x");
		if( searching )
		{
			report("@");
			usleep(1000);
		}
	}
	index->eventloop_interrupted = 1;
#ifdef WIN32
	termios_interrupt_event_loop( index->fd, 1 );
#endif /* WIN32 */
	report("interrupted\n");
}

/*----------------------------------------------------------
 is_interrupted

   accept:      event_info_struct
   perform:     see if the port is being closed.
   return:      a positive value if the port is being closed.
   exceptions:  none
   comments:
----------------------------------------------------------*/
jboolean is_interrupted( struct event_info_struct *eis )
{
	int result;
	JNIEnv *env = eis->env;

	ENTER( "is_interrupted" );
	(*env)->ExceptionClear(env);
	result = (*env)->CallBooleanMethod( env, *eis->jobj,
			eis->checkMonitorThread );
#ifdef DEBUG
	if((*env)->ExceptionOccurred(env)) {
		report ( "an error occured calling sendEvent()\n" );
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
#endif /* DEBUG */
	LEAVE( "RXTXCommDriver:is_interrupted" );
	return(result);
}

/*----------------------------------------------------------
 nativeSetEventFlag

   accept:      fd for finding the struct, event to flag, flag.
   perform:     toggle the flag
   return:      none
   exceptions:  none
   comments:	all the logic used to be done in Java but its too noisy
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(nativeSetEventFlag)( JNIEnv *env,
							jobject jobj,
							jint fd,
							jint event,
							jboolean flag )
{
	struct event_info_struct *index = master_index;

	if( !index )
	{
		report_error("nativeSetEventFlag !index\n");
		return;
	}
	while( index->fd != fd && index->next ) index = index->next;
	if( index->fd != fd )
	{
		report_error("nativeSetEventFlag !fd\n");
		return;
	}
	index->eventflags[event] = (int) flag;
#ifdef WIN32
	termios_setflags( fd, index->eventflags );
#endif /* win32 */
	
}

/*----------------------------------------------------------
 send_event

   accept:      event_info_structure, event type and true/false
   perform:     if state is > 0 send a JNI_TRUE event otherwise send JNI_FALSE
   return:      a positive value if the port is being closed.
   exceptions:  none
   comments:
----------------------------------------------------------*/
int send_event( struct event_info_struct *eis, jint type, int flag )
{
	int result;
	JNIEnv *env = eis->env;

	ENTER( "send_event" );
	if(eis->jclazz == NULL) return JNI_TRUE;

	(*env)->ExceptionClear(env);

	result = (*env)->CallBooleanMethod( env, *eis->jobj, eis->send_event,
		type, flag > 0 ? JNI_TRUE : JNI_FALSE );

#ifdef DEBUG
	if((*eis->env)->ExceptionOccurred(eis->env)) {
		report ( "an error occured calling sendEvent()\n" );
		(*eis->env)->ExceptionDescribe(eis->env);
		(*eis->env)->ExceptionClear(eis->env);
	}
#endif /* DEBUG */
	LEAVE( "send_event" );
	return(result);
}

/*----------------------------------------------------------
get_java_var

   accept:      env (keyhole to java)
                jobj (java RXTXPort object)
   return:      the fd field from the java object
   exceptions:  none
   comments:
----------------------------------------------------------*/
int get_java_var( JNIEnv *env, jobject jobj, char *id, char *type )
{
	int result = 0;
	jclass jclazz = (*env)->GetObjectClass( env, jobj );
	jfieldID jfd = (*env)->GetFieldID( env, jclazz, id, type );

	ENTER( "get_java_var" );
	if( !jfd ) {
		(*env)->ExceptionDescribe( env );
		(*env)->ExceptionClear( env );
		(*env)->DeleteLocalRef( env, jclazz );
		LEAVE( "get_java_var" );
		return result;
	}
	result = (int)( (*env)->GetIntField( env, jobj, jfd ) );
/* ct7 & gel * Added DeleteLocalRef */
	(*env)->DeleteLocalRef( env, jclazz );
	if(!strncmp( "fd",id,2) && result == 0)
		report_error( "invalid file descriptor\n" );
	LEAVE( "get_java_var" );
	return result;
}

/*----------------------------------------------------------
throw_java_exception

   accept:      env (keyhole to java)
                *exc (exception class name)
                *foo (function name)
                *msg (error message)
   perform:     Throw a new java exception
   return:      none
   exceptions:  haha!
   comments:
----------------------------------------------------------*/
void throw_java_exception( JNIEnv *env, char *exc, char *foo, char *msg )
{
	char buf[ 60 ];
	jclass clazz = (*env)->FindClass( env, exc );
	ENTER( "throw_java_exception" );	
	if( !clazz ) {
		(*env)->ExceptionDescribe( env );
		(*env)->ExceptionClear( env );
		LEAVE( "throw_java_exception" );
		return;
	}
#if defined(_GNU_SOURCE)
	snprintf( buf, 60, "%s in %s", msg, foo );
#else
	sprintf( buf,"%s in %s", msg, foo );
#endif /* _GNU_SOURCE */
	(*env)->ThrowNew( env, clazz, buf );
/* ct7 * Added DeleteLocalRef */
	(*env)->DeleteLocalRef( env, clazz );
	LEAVE( "throw_java_exception" );
}

/*----------------------------------------------------------
 report_warning

   accept:      string to send to report as an message
   perform:     send the string to stderr or however it needs to be reported.
   return:      none
   exceptions:  none
   comments:
----------------------------------------------------------*/
void report_warning(char *msg)
{
#ifndef DEBUG_MW
	fprintf(stderr, msg);
#else
	mexWarnMsgTxt( (const char *) msg );
#endif /* DEBUG_MW */
}

/*----------------------------------------------------------
 report_verbose

   accept:      string to send to report as an verbose message
   perform:     send the string to stderr or however it needs to be reported.
   return:      none
   exceptions:  none
   comments:
----------------------------------------------------------*/
void report_verbose(char *msg)
{
#ifdef DEBUG_VERBOSE
#ifndef DEBUG_MW
	mexErrMsgTxt( msg );
#else
	fprintf(stderr, msg);
#endif /* DEBUG_MW */
#endif /* DEBUG_VERBOSE */
}
/*----------------------------------------------------------
 report_error

   accept:      string to send to report as an error
   perform:     send the string to stderr or however it needs to be reported.
   return:      none
   exceptions:  none
   comments:
----------------------------------------------------------*/
void report_error(char *msg)
{
#ifndef DEBUG_MW
	fprintf(stderr, msg);
#else
	mexWarnMsgTxt( msg );
#endif /* DEBUG_MW */
}

/*----------------------------------------------------------
 report

   accept:      string to send to stderr
   perform:     if DEBUG is defined send the string to stderr.
   return:      none
   exceptions:  none
   comments:
----------------------------------------------------------*/
void report(char *msg)
{
#ifdef DEBUG
#	ifndef DEBUG_MW
		fprintf(stderr, msg);
#	else
		mexPrintf( msg );
#	endif /* DEBUG_MW */
#endif /* DEBUG */
}

#ifndef WIN32
/*----------------------------------------------------------
 fhs_lock

   accept:      The name of the device to try to lock
                termios struct
   perform:     Create a lock file if there is not one already.
   return:      1 on failure 0 on success
   exceptions:  none
   comments:    This is for linux and freebsd only currently.  I see SVR4 does
                this differently and there are other proposed changes to the
		Filesystem Hierachy Standard

		more reading:

----------------------------------------------------------*/
int fhs_lock( const char *filename )
{
	/*
	 * There is a zoo of lockdir possibilities
	 * Its possible to check for stale processes with most of them.
	 * for now we will just check for the lockfile on most
	 * Problem lockfiles will be dealt with.  Some may not even be in use.
	 *
	 */
	int fd,j;
	char lockinfo[12], message[80];
	char file[80], *p;

	j = strlen( filename );
	p = ( char * ) filename + j;
	/*  FIXME  need to handle subdirectories /dev/cua/... 
	    SCO Unix use lowercase all the time
			taj
	*/
	while( *( p - 1 ) != '/' && j-- != 1 )
	{
#if defined ( __unixware__ )
		*p = tolower( *p );
#endif /* __unixware__ */
		p--;
	}
	sprintf( file, "%s/LCK..%s", LOCKDIR, p );
	if ( check_lock_status( filename ) )
	{
		report( "fhs_lock() lockstatus fail\n" );
		return 1;
	}
	fd = open( file, O_CREAT | O_WRONLY | O_EXCL, 0666 );
	if( fd < 0 )
	{
		sprintf( message,
			"RXTX fhs_lock() Error: creating lock file: %s\n",
			file );
		report( message );
		return 1;
	}
	sprintf( lockinfo, "%10d\n",(int) getpid() );
	sprintf( message, "CREATING LOCK: %s\n", lockinfo );
	report( message );
	write( fd, lockinfo, 11 );
	close( fd );
	return 0;
}

/*----------------------------------------------------------
 uucp_lock

   accept:     char * filename.  Device to be locked 
   perform:    Try to get a uucp_lock
   return:     int 0 on success
   exceptions: none 
   comments: 
		The File System Hierarchy Standard
		http://www.pathname.com/fhs/

		UUCP Lock Files
		http://docs.freebsd.org/info/uucp/uucp.info.UUCP_Lock_Files.html

		FSSTND
		ftp://tsx-11.mit.edu/pub/linux/docs/linux-standards/fsstnd/

		Proposed Changes to the File System Hierarchy Standard
		ftp://scicom.alphacdc.com/pub/linux/devlock-0.X.tgz

		"UNIX Network Programming", W. Richard Stevens,
		Prentice-Hall, 1990, pages 96-101.

		There is much to do here.

		1) UUCP style locks (done)
			/var/spool/uucp
		2) SVR4 locks
			/var/spool/locks
		3) FSSTND locks (done)
			/var/lock
		4) handle stale locks  (done except kermit locks)
		5) handle minicom lockfile contents (FSSTND?)
			"     16929 minicom root\n"  (done)
		6) there are other Lock conventions that use Major and Minor
		   numbers...
		7) Stevens recommends LCK..<pid>

		most are caught above.  If they turn out to be problematic
		rather than an exercise, we will handle them.

----------------------------------------------------------*/
int uucp_lock( const char *filename )
{
	char lockfilename[80], lockinfo[12], message[80];
	char name[80];
	int fd;
	struct stat buf;

	sprintf( message, "uucp_lock( %s );\n", filename ); 
	report( message );

	if ( check_lock_status( filename ) )
	{
		report( "RXTX uucp check_lock_status true\n" );
		return 1;
	}
	if ( stat( LOCKDIR, &buf ) != 0 )
	{
		report( "RXTX uucp_lock() could not find lock directory.\n" );
		return 1;
	}
	if ( stat( filename, &buf ) != 0 )
	{
		report( "RXTX uucp_lock() could not find device.\n" );
		sprintf( message, "device was %s\n", name );
		report( message );
		return 1;
	}
	sprintf( lockfilename, "%s/LK.%03d.%03d.%03d",
		LOCKDIR,
		(int) major( buf.st_dev ),
	 	(int) major( buf.st_rdev ),
		(int) minor( buf.st_rdev )
	);
	sprintf( lockinfo, "%10d\n", (int) getpid() );
	if ( stat( lockfilename, &buf ) == 0 )
	{
		sprintf( message, "RXTX uucp_lock() %s is there\n",
			lockfilename );
		report( message );
		report_error( message );
		return 1;
	}
	fd = open( lockfilename, O_CREAT | O_WRONLY | O_EXCL, 0666 );
	if( fd < 0 )
	{
		sprintf( message,
			"RXTX uucp_lock() Error: creating lock file: %s\n",
			lockfilename );
		report( message );
		return 1;
	}
	write( fd, lockinfo,11 );
	close( fd );
	return 0;
}

/*----------------------------------------------------------
 check_lock_status

   accept:      the lock name in question
   perform:     Make sure everything is sane
   return:      0 on success
   exceptions:  none
   comments:    
----------------------------------------------------------*/
int check_lock_status( const char *filename )
{
	struct stat buf;
	/*  First, can we find the directory? */

	if ( stat( LOCKDIR, &buf ) != 0 )
	{
		report( "could not find lock directory.\n" );
		return 1;
	}

	/*  OK.  Are we able to write to it?  If not lets bail */

	if ( check_group_uucp() )
	{
		report_error( "No permission to create lock file.

		please see: How can I use Lock Files with rxtx? in INSTALL\n" );
		exit(0);
	}

	/* is the device alread locked */

	if ( is_device_locked( filename ) )
	{
		report( "device is locked by another application\n" );
		return 1;	
	}
	return 0;
	
}

/*----------------------------------------------------------
 fhs_unlock

   accept:      The name of the device to unlock
   perform:     delete the lock file
   return:      none
   exceptions:  none
   comments:    This is for linux only currently.  I see SVR4 does this
                differently and there are other proposed changes to the
		Filesystem Hierachy Standard
----------------------------------------------------------*/
void fhs_unlock( const char *filename, int openpid )
{
	char file[80],*p;
	int i;

	i = strlen( filename );
	p = ( char * ) filename + i;
	/*  FIXME  need to handle subdirectories /dev/cua/... */
	while( *( p - 1 ) != '/' && i-- != 1 ) p--;
	sprintf( file, "%s/LCK..%s", LOCKDIR, p );

	if( !check_lock_pid( file, openpid ) )
	{
		unlink(file);
		report("Removing LockFile\n");
	}
	else
	{
		report("Unable to remove LockFile\n");
	}
}

/*----------------------------------------------------------
 uucp_unlock

   accept:     char *filename the device that is locked      
   perform:    remove the uucp lockfile if it exists 
   return:     none 
   exceptions: none 
   comments:   http://docs.freebsd.org/info/uucp/uucp.info.UUCP_Lock_Files.html 
----------------------------------------------------------*/
void uucp_unlock( const char *filename, int openpid )
{
	struct stat buf;
	char file[80], message[80];
	/* FIXME */

	sprintf( message, "uucp_unlock( %s );\n", filename );
	report( message );

	if ( stat( filename, &buf ) != 0 ) 
	{
		/* hmm the file is not there? */
		report( "uucp() unlock no such device\n" );
		return;
	}
	sprintf( file, LOCKDIR"/LK.%03d.%03d.%03d",
		(int) major( buf.st_dev ),
	 	(int) major( buf.st_rdev ),
		(int) minor( buf.st_rdev )
	);
	if ( stat( file, &buf ) != 0 ) 
	{
		/* hmm the file is not there? */
		report( "uucp() unlock no such lockfile\n" );
		return;
	}
	if( !check_lock_pid( file, openpid ) )
	{ 
		sprintf( message, "uucp unlinking %s\n", file );
		report( message );
		unlink(file);
	}
	else
	{
		sprintf( message, "uucp unlinking failed %s\n", file );
		report( message );
	}
}

/*----------------------------------------------------------
 check_lock_pid

   accept:     the name of the lockfile 
   perform:    make sure the lock file is ours.
   return:     0 on success
   exceptions: none
   comments:   
----------------------------------------------------------*/
int check_lock_pid( const char *file, int openpid )
{
	int fd, lockpid;
	char pid_buffer[12];
	char message[80];

	fd=open( file, O_RDONLY );
	if ( fd < 0 )
	{
		return( 1 );
	}
	if ( read( fd, pid_buffer, 11 ) < 0 )
	{
		close( fd );
		return( 1 );
	}
	close( fd );
	pid_buffer[11] = '\0';
	lockpid = atol( pid_buffer );
	/* Native threads JVM's have multiple pids */
	if ( lockpid != getpid() && lockpid != getppid() && lockpid != openpid )
	{
		sprintf(message, "lock = %s pid = %i gpid=%i openpid=%i\n",
			pid_buffer, (int) getpid(), (int) getppid(), openpid );
		report( message );
		return( 1 );
	}
	return( 0 );
}

/*----------------------------------------------------------
 check_group_uucp

   accept:     none
   perform:    check if the user is root or in group uucp
   return:     0 on success 
   exceptions: none 
   comments:   
		This checks if the effective user is in group uucp so we can
		create lock files.  If not we give them a warning and bail.
		If its root we just skip the test.

		if someone really wants to override this they can use the			USER_LOCK_DIRECTORY --not recommended.
----------------------------------------------------------*/
int check_group_uucp()
{
	struct group *g = getgrnam( "uucp" );
	struct passwd *user = getpwuid( geteuid() );

#ifndef USER_LOCK_DIRECTORY
	if( strcmp( user->pw_name, "root" ) )
	{
		while( *g->gr_mem )
		{
			if( !strcmp( *g->gr_mem, user->pw_name ) )
			{
				break;
			}
			(void) *g->gr_mem++;
		}
		if( !*g->gr_mem )
		{
			report( UUCP_ERROR );
			return 1;
		}
	}
#endif /* USER_LOCK_DIRECTORY */
	return 0;
}

/*----------------------------------------------------------
 The following should be able to follow symbolic links.  I think the stat
 method used below will work on more systems.  This was found while looking
 for information.

 * realpath() doesn't exist on all of the systems my code has to run
   on (HP-UX 9.x, specifically)
----------------------------------------------------------
int different_from_LOCKDIR(const char* ld)
{
	char real_ld[MAXPATHLEN];
	char real_LOCKDIR[MAXPATHLEN];
	if (strncmp(ld, LOCKDIR, strlen(ld)) == 0)
		return 0;
	if (realpath(ld, real_ld) == NULL)
		return 1;
	if (realpath(LOCKDIR, real_LOCKDIR) == NULL)
		return 1;
	if (strncmp(real_ld, real_LOCKDIR, strlen(real_ld)) == 0)
		return 0;
	else
		return 1;
}
*/

/*----------------------------------------------------------
 is_device_locked

   accept:      char * filename.  The device in question including the path.
   perform:     see if one of the many possible lock files is aready there
		if there is a stale lock, remove it.
   return:      1 if the device is locked or somethings wrong.
		0 if its possible to create our own lock file.
   exceptions:  none
   comments:    check if the device is already locked
----------------------------------------------------------*/
int is_device_locked( const char *port_filename )
{
	const char *lockdirs[] = { "/etc/locks", "/usr/spool/kermit",
		"/usr/spool/locks", "/usr/spool/uucp", "/usr/spool/uucp/",
		"/usr/spool/uucp/LCK", "/var/lock", "/var/lock/modem",
		"/var/spool/lock", "/var/spool/locks", "/var/spool/uucp",
		LOCKDIR, NULL
	};
	const char *lockprefixes[] = { "LCK..", "lk..", "LK.", NULL }; 
	char *p, file[80], pid_buffer[20], message[80];
	int i = 0, j, k, fd , pid;
	struct stat buf;
	struct stat buf2;

	j = strlen( port_filename );
	p = ( char * ) port_filename+j;
	while( *( p-1 ) != '/' && j-- !=1 ) p--;

	while( lockdirs[i] )
	{
		/*
		   Look for lockfiles in all known places other than the
		   defined lock directory for this system
		   report any unexpected lockfiles.

		   Is the suspect lockdir there?
		   if it is there is it not the expected lock dir?
		*/
		if( !stat( lockdirs[i], &buf2 ) &&
			strncmp( lockdirs[i], LOCKDIR, strlen( lockdirs[i] ) ) )
		{
			j = strlen( port_filename );
			p = ( char *  ) port_filename + j;
		/*
		   SCO Unix use lowercase all the time
			taj
		*/
			while( *( p - 1 ) != '/' && j-- != 1 )
			{
#if defined ( __unixware__ )
				*p = tolower( *p );
#endif /* __unixware__ */
				p--;
			}
			k=0;
			while ( lockprefixes[k] )
			{
				/* FHS style */
				sprintf( file, "%s/%s%s", lockdirs[i],
					lockprefixes[k], p );
				if( stat( file, &buf ) == 0 )
				{
					sprintf( message, UNEXPECTED_LOCK_FILE,
						file );
					report( message );
					return 1;
				}

				/* UUCP style */
				stat(port_filename , &buf );
				sprintf( file, "%s/%s%03d.%03d.%03d",
					lockdirs[i],
					lockprefixes[k],
					(int) major( buf.st_dev ),
					(int) major( buf.st_rdev ),
					(int) minor( buf.st_rdev )
				);
				if( stat( file, &buf ) == 0 )
				{
					sprintf( message, UNEXPECTED_LOCK_FILE,
						file );
					report( message );
					return 1;
				}
				k++;
			}
		}
		i++;
	}

	/*
		OK.  We think there are no unexpect lock files for this device
		Lets see if there any stale lock files that need to be
		removed.
	*/
		 
#ifdef FHS
	/*  FHS standard locks */
	i = strlen( port_filename );
	p = ( char * ) port_filename + i;
	while( *(p-1) != '/' && i-- != 1)
	{
#if defined ( __unixware__ )
		*p = tolower( *p );
#endif /* __unixware__ */
		p--;
	}
	sprintf( file, "%s/%s%s", LOCKDIR, LOCKFILEPREFIX, p );
#else 
	/*  UUCP standard locks */
	if ( stat( port_filename, &buf ) != 0 )
	{
		report( "RXTX is_device_locked() could not find device.\n" );
			return 1;
	}
	sprintf( file, "%s/LK.%03d.%03d.%03d",
		LOCKDIR,
		(int) major( buf.st_dev ),
 		(int) major( buf.st_rdev ),
		(int) minor( buf.st_rdev )
	);

#endif /* FHS */

	if( stat( file, &buf ) == 0 )
	{

		/* check if its a stale lock */
		fd=open( file, O_RDONLY );
		read( fd, pid_buffer, 11 );
		/* FIXME null terminiate pid_buffer? need to check in Solaris */
		close( fd );
		sscanf( pid_buffer, "%d", &pid );

		if( kill( (pid_t) pid, 0 ) && errno==ESRCH )
		{
			sprintf( message,
				"RXTX Warning:  Removing stale lock file. %s\n",
				file );
			report( message );
			if( unlink( file ) != 0 )
			{
				snprintf( message, 80, "RXTX Error:  Unable to \
					remove stale lock file: %s\n",
					file
				);
				report( message );
				return 1;
			}
		}
	}
	return 0;
}
#endif /* WIN32 */

/*----------------------------------------------------------
 system_does_not_lock

   accept:      the filename the system thinks should be locked.
   perform:     avoid trying to create lock files on systems that dont use them
   return:      0 for success ;)
   exceptions:  none
   comments:    OS's like Win32 may not have lock files.
----------------------------------------------------------*/
int system_does_not_lock( const char * filename )
{
	return 0;
}

/*----------------------------------------------------------
 system_does_not_unlock

   accept:      the filename the system thinks should be locked.
   perform:     avoid trying to create lock files on systems that dont use them
   return:      none
   exceptions:  none
   comments:    OS's like Win32 may not have lock files.
----------------------------------------------------------*/
void system_does_not_unlock( const char * filename, int openpid )
{
	return;
}

/*----------------------------------------------------------
 dump_termios

   accept:      string to indicate where this was called.
                termios struct
   perform:     print the termios struct to stderr.
   return:      none
   exceptions:  none
   comments:    used to debug the termios struct.
----------------------------------------------------------*/
void dump_termios(char *foo,struct termios *ttyset)
{
#ifdef DEBUG
	int i;

	fprintf(stderr, "%s c_iflag=%#x\n", foo, ttyset->c_iflag);
	fprintf(stderr, "%s c_lflag=%#x\n", foo, ttyset->c_lflag);
	fprintf(stderr, "%s c_oflag=%#x\n", foo, ttyset->c_oflag);
	fprintf(stderr, "%s c_cflag=%#x\n", foo, ttyset->c_cflag);
	fprintf(stderr, "%s c_cc[]: ", foo);
	for(i=0; i<NCCS; i++)
	{
		fprintf(stderr,"%d=%x ", i, ttyset->c_cc[i]);
	}
	fprintf(stderr,"\n" );
#endif /* DEBUG */
}
