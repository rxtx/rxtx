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
#include "config.h"
#include "javax_comm_RXTXPort.h"
#ifndef __LCC__ 
#   include <unistd.h>
#else /* windows lcc compiler for fd_set. probably wrong */
#   include<winsock.h>
#endif
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
#else
#	include <win32termios.h>
/*  FIXME  returns 0 in all cases on win32
#define S_ISCHR(m)	(((m)&S_IFMT) == S_IFCHR)
*/
#define S_ISCHR(m) (1)
#endif /* WIN32 */
#ifdef HAVE_TERMIOS_H
#	include <termios.h>
#endif
#ifdef HAVE_SIGNAL_H
#   include <signal.h>
#endif
#ifdef HAVE_SYS_SIGNAL_H
#   include <sys/signal.h>
#endif
#include <sys/types.h>
#ifdef HAVE_SYS_TIME_H
#   include <sys/time.h>
#endif
#   include <fcntl.h>
#ifdef HAVE_SYS_FCNTL_H
#   include <sys/fcntl.h>
#endif
#ifdef HAVE_SYS_FILE_H
#   include <sys/file.h>
#endif

#if defined(__linux__)
#	include <linux/types.h> /* fix for linux-2.3.4? kernels */
#	include <linux/serial.h>
#	include <linux/version.h>
#endif
#if defined(__hpux__)
#include <sys/modem.h>
#endif

extern int errno;
#include "SerialImp.h"
/* #define DEBUG */

/* this is so diff will not generate noise when merging 1.4 and 1.5 changes
 * It will eventually be removed.
 * */
#define RXTXPort(foo) Java_javax_comm_RXTXPort_ ## foo
#define RXTXCommDriver(foo) Java_javax_comm_RXTXCommDriver_ ## foo

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
#endif /* DEBUG && __linux__ */
	/* This bit of code checks to see if there is a signal handler installed
	   for SIGIO, and installs SIG_IGN if there is not.  This is necessary
	   for the native threads jdk, but we don't want to do it with green
	   threads, because it slows things down.  Go figure. */

	/* POSIX signal handling functions */
#if !defined(WIN32)
	struct sigaction handler;
	sigaction( SIGIO, NULL, &handler );
	if( !handler.sa_handler ) signal( SIGIO, SIG_IGN );
#endif /* !WIN32 */
#if defined(DEBUG) && defined(__linux__)
	/* Lets let people who upgraded kernels know they may have problems */
	if (uname (&name) == -1)
	{
		report("RXTX WARNING:  cannot get system name\n");
		return;
	}
	if(!strcmp(name.release,UTS_RELEASE))
	{
		fprintf(stderr, LINUX_KERNEL_VERSION_ERROR, UTS_RELEASE,
			name.release);
		getchar();
	}
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
	const char *filename = (*env)->GetStringUTFChars( env, jstr, 0 );

	if (!fhs_lock(filename)) goto fail;

	do {
		fd=open (filename, O_RDWR | O_NOCTTY | O_NONBLOCK );
	}  while (fd < 0 && errno==EINTR);
	(*env)->ReleaseStringUTFChars( env, jstr, NULL );
	if( fd < 0 ) goto fail;

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

	return (jint)fd;

fail:
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
	int result;
	int fd = get_java_var( env, jobj,"fd","I" );
	const char *filename = (*env)->GetStringUTFChars( env, jstr, 0 );

	if (fd > 0)
	{
		do {
			result=close (fd);
		}  while (result < 0 && errno==EINTR);
		fhs_unlock(filename);
	}
	(*env)->ReleaseStringUTFChars( env, jstr, NULL );
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
	if( !cspeed ) return;
	if( tcgetattr( fd, &ttyset ) < 0 ) goto fail;
	if( !translate_data_bits( env, &(ttyset.c_cflag), dataBits ) ) return;
	if( !translate_stop_bits( env, &(ttyset.c_cflag), stopBits ) ) return;
	if( !translate_parity( env, &(ttyset.c_cflag), parity ) ) return;
#ifdef __FreeBSD__
	if( cfsetspeed( &ttyset, cspeed ) < 0 ) goto fail;
#else
	if( cfsetispeed( &ttyset, cspeed ) < 0 ) goto fail;
	if( cfsetospeed( &ttyset, cspeed ) < 0 ) goto fail;
#endif  /* __FreeBSD__ */
	if( tcsetattr( fd, TCSANOW, &ttyset ) < 0 ) goto fail;
	return;

fail:
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
	switch( speed ) {
		case 0:		return B0;
		case 50:		return B50;
		case 75:		return B75;
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
		case 57600:	return B57600;
		case 115200:	return B115200;
#ifdef B230400
		case 230400:	return B230400;
#endif /* B230400 */
#ifdef B460800
		case 460800:	return B460800;
#endif /* B460800 */
	}

	throw_java_exception( env, UNSUPPORTED_COMM_OPERATION,
		"translate_speed", "speed" );
	return 0;
}

/*----------------------------------------------------------
 translate_data_bits

   accept:     javax.comm.SerialPort.DATABITS_* constant
   perform:    set proper termios c_cflag bits
   return:     1 if successful
					0 if an exception is thrown
   exceptions: UnsupportedCommOperationException
----------------------------------------------------------*/
int translate_data_bits( JNIEnv *env, tcflag_t *cflag, jint dataBits )
{
	int temp = (*cflag) & ~CSIZE;

	switch( dataBits ) {
		case DATABITS_5:
			(*cflag) = temp | CS5;
			return 1;
		case DATABITS_6:
			(*cflag) = temp | CS6;
			return 1;
		case DATABITS_7:
			(*cflag) = temp | CS7;
			return 1;
		case DATABITS_8:
			(*cflag) = temp | CS8;
			return 1;
	}

	throw_java_exception( env, UNSUPPORTED_COMM_OPERATION,
		"translate_data_bits", "data bits" );
	return 0;
}

/*----------------------------------------------------------
 translate_stop_bits

   accept:     javax.comm.SerialPort.STOPBITS_* constant
   perform:    set proper termios c_cflag bits
   return:     1 if successful
					0 if an exception is thrown
   exceptions: UnsupportedCommOperationException
   comments:   If you specify 5 data bits and 2 stop bits, the port will
               allegedly use 1.5 stop bits.  Does anyone care?
----------------------------------------------------------*/
int translate_stop_bits( JNIEnv *env, tcflag_t *cflag, jint stopBits )
{
	switch( stopBits ) {
		case STOPBITS_1:
			(*cflag) &= ~CSTOPB;
			return 1;
		/*  ok.. lets try putting it in and see if anyone notices */
		case STOPBITS_1_5:
			translate_data_bits( env, cflag, DATABITS_5 );
		case STOPBITS_2:
			(*cflag) |= CSTOPB;
			return 1;
	}

	throw_java_exception( env, UNSUPPORTED_COMM_OPERATION,
		"translate_stop_bits", "stop bits" );
	return 0;
}

/*----------------------------------------------------------
 translate_parity

   accept:     javax.comm.SerialPort.PARITY_* constant
   perform:    set proper termios c_cflag bits
   return:     1 if successful
               0 if an exception is thrown
   exceptions: UnsupportedCommOperationException
   comments:   The CMSPAR bit should be used for 'mark' and 'space' parity,
               but it's not in glibc's includes.  Oh well, rarely used anyway.
----------------------------------------------------------*/
int translate_parity( JNIEnv *env, tcflag_t *cflag, jint parity )
{
	(*cflag) &= ~(PARENB | PARODD);
	switch( parity ) {
		case PARITY_NONE:
			return 1;
		case PARITY_EVEN:
			(*cflag) |= PARENB;
			return 1;
		case PARITY_ODD:
			(*cflag) |= PARENB | PARODD;
			return 1;
#ifdef CMSPAR
		case PARITY_MARK:
			(*cflag) |= PARENB | PARODD | CMSPAR;
			return 1;
		case PARITY_SPACE:
			(*cflag) |= PARENB | CMSPAR;
			return 1;
#endif /* CMSPAR */
	}

	throw_java_exception( env, UNSUPPORTED_COMM_OPERATION,
		"translate_parity", "parity" );
	return 0;
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

	do {
		result=write (fd, &byte, sizeof(unsigned char));
	}  while (result < 0 && errno==EINTR);
	if(result >= 0)
		return;
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
	int result=0,total=0,i;

	unsigned char *bytes = (unsigned char *)malloc( count );

	jbyte *body = (*env)->GetByteArrayElements( env, jbarray, 0 );
	for( i = 0; i < count; i++ ) bytes[ i ] = body[ i + offset ];
	(*env)->ReleaseByteArrayElements( env, jbarray, body, 0 );
	do {
		result=write (fd, bytes + total, count - total);
		if(result >0){
			total += result;
		}
	}  while ((total<count)||(result < 0 && errno==EINTR));
	free( bytes );
	if( result < 0 ) throw_java_exception( env, IO_EXCEPTION,
		"writeArray", strerror( errno ) );
}


/*----------------------------------------------------------
RXTXPort.drain

   accept:      none
   perform:     wait until all data is transmitted
   return:      none
   exceptions:  IOException
   comments:    java.io.OutputStream.flush() is equivalent to tcdrain,
                not tcflush, which throws away unsent bytes

                count logic added to avoid infinite loops when EINTR is
                true...  Thread.yeild() was suggested.
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(drain)( JNIEnv *env,
	jobject jobj )
{
	int fd = get_java_var( env, jobj,"fd","I" );
	int result, count=0;

	do {
		result=tcdrain (fd);
		count++;
	}  while (result && errno==EINTR && count <5);

	if( result ) throw_java_exception( env, IO_EXCEPTION, "drain",
		strerror( errno ) );
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
	tcsendbreak( fd, (int)( duration / 250 ) );
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

	if( tcgetattr( fd, &ttyset ) < 0 ) goto fail;
	return(ttyset.c_cc[ VTIME ] * 100);
fail:
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

	if( tcgetattr( fd, &ttyset ) < 0 ) goto fail;
	return(ttyset.c_cc[ VTIME ] > 0 ? JNI_TRUE:JNI_FALSE);
fail:
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

	ioctl( fd, TIOCMGET, &result );
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

	ioctl( fd, TIOCMGET, &result );
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

	ioctl( fd, TIOCMGET, &result );
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

	ioctl( fd, TIOCMGET, &result );
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

	ioctl( fd, TIOCMGET, &result );
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

	ioctl( fd, TIOCMGET, &result );
	if( state == JNI_TRUE ) result |= TIOCM_RTS;
	else result &= ~TIOCM_RTS;
	ioctl( fd, TIOCMSET, &result );
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

	ioctl( fd, TIOCMGET, &result );
	if( state == JNI_TRUE ) result |= TIOCM_DSR;
	else result &= ~TIOCM_DSR;
	ioctl( fd, TIOCMSET, &result );
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

	ioctl( fd, TIOCMGET, &result );
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

	ioctl( fd, TIOCMGET, &result );
	if( state == JNI_TRUE ) result |= TIOCM_DTR;
	else result &= ~TIOCM_DTR;
	ioctl( fd, TIOCMSET, &result );
	return;
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
int read_byte_array( int fd, unsigned char *buffer, int length, int timeout )
{
	int ret, left, bytes = 0;
        fd_set rfds;
	struct timeval sleep;
	struct timeval *psleep=&sleep;

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
		do {
			if( timeout == 0 ) psleep = NULL;
			ret=select( fd + 1, &rfds, NULL, NULL, psleep );
		}  while (ret < 0 && errno==EINTR);
		if( ret == 0 ) break;
		if( ret < 0 ) return -1;
		ret = read( fd, buffer + bytes, left );
		if( ret == 0 ) break;
		if( ret < 0 ) return -1;
		bytes += ret;
		left -= ret;
	}
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

	if( tcgetattr( fd, &ttyset ) < 0 ) goto fail;
	ttyset.c_cc[ VMIN ] = threshold;
	ttyset.c_cc[ VTIME ] = vtime/100;
	if( tcsetattr( fd, TCSANOW, &ttyset ) < 0 ) goto fail;

	return;
fail:
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

	bytes = read_byte_array( fd, buffer, 1, timeout );
	if( bytes < 0 ) {
		throw_java_exception( env, IO_EXCEPTION, "readByte",
			strerror( errno ) );
		return -1;
	}
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
	unsigned char *buffer;
	int fd = get_java_var( env, jobj, "fd", "I" );
	int timeout = get_java_var( env, jobj, "timeout", "I" );

	if( length > SSIZE_MAX || length < 0 ) {
		throw_java_exception( env, ARRAY_INDEX_OUT_OF_BOUNDS,
			"readArray", "Invalid length" );
		return -1;
	}

	buffer = (unsigned char *)malloc( sizeof( unsigned char ) * length );
	if( buffer == 0 ) {
		throw_java_exception( env, OUT_OF_MEMORY, "readArray",
			"Unable to allocate buffer" );
		return -1;
	}

	bytes = read_byte_array( fd, buffer, length, timeout );
	if( bytes < 0 ) {
		free( buffer );
		throw_java_exception( env, IO_EXCEPTION, "readArray",
			strerror( errno ) );
		return -1;
	}
	body = (*env)->GetByteArrayElements( env, jbarray, 0 );
	memcpy(body + offset, buffer, bytes);
	(*env)->ReleaseByteArrayElements( env, jbarray, body, 0 );
	free( buffer );
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

	if( ioctl( fd, FIONREAD, &result ) )
	{
		throw_java_exception( env, IO_EXCEPTION, "nativeavailable",
			strerror( errno ) );
		return -1;
	}
	else return (jint)result;
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
   exceptions:  IOException
   comments:  there is no differentiation between input and output hardware
              flow control
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(setflowcontrol)( JNIEnv *env,
	jobject jobj, jint flowmode )
{
	struct termios ttyset;
	int fd = get_java_var( env, jobj,"fd","I" );

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
	return;
fail:
	throw_java_exception( env, IO_EXCEPTION, "setHWFC",
		strerror( errno ) );
	return;
}

/*----------------------------------------------------------
RXTXPort.eventLoop

   accept:      none
   perform:     periodically check for SerialPortEvents
   return:      none
   exceptions:  none
   comments:
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(eventLoop)( JNIEnv *env, jobject jobj )
{
	int fd, ret, change;
	fd_set rfds;
	struct timeval tv_sleep;
	unsigned int mflags, omflags;
	jboolean interrupted = 0;
#if defined TIOCSERGETLSR
	struct stat fstatbuf;
#endif /* TIOCSERGETLSR */

#if defined(TIOCGICOUNT)
	struct serial_icounter_struct sis, osis;
	/* JK00: flag if this can be used on this port */
	int has_tiocgicount = 1;
#endif /* TIOCGICOUNT */

#if defined(TIOCSERGETLSR)
	int has_tiocsergetlsr = 1;
#endif /* TIOCSERGETLSR */

	fd = get_java_var(env, jobj, "fd", "I");

#if defined(TIOCGICOUNT)
	/* Some multiport serial cards do not implement TIOCGICOUNT ... */
	/* So use the 'dumb' mode to enable using them after all! JK00 */
	if( ioctl( fd, TIOCGICOUNT, &osis ) < 0 ) {
		report("Port does not support TIOCGICOUNT events\n" );
		has_tiocgicount = 0;
	}
#endif /*  TIOCGICOUNT */

#if defined(TIOCSERGETLSR)
	/* JK00: work around for multiport cards without TIOCSERGETLSR */
	/* Cyclades is one of those :-(				       */
	if( ioctl( fd, TIOCSERGETLSR, &change ) ) {
		report("Port does not support TIOCSERGETLSR\n" );
			has_tiocsergetlsr = 0;
	}
#endif /* TIOCSERGETLSR */

	if( ioctl( fd, TIOCMGET, &omflags) <0 ) {
		report("Port does not support events\n" );
 		return;
	}

	FD_ZERO( &rfds );
	while( !interrupted ) {
		FD_SET( fd, &rfds );
		tv_sleep.tv_sec = 0;
		tv_sleep.tv_usec = 100000;
		do {
			ret=select( fd + 1, &rfds, NULL, NULL, &tv_sleep );
		}  while (ret < 0 && errno==EINTR);
		if( ret < 0 ) break;

		interrupted = is_interrupted(env, jobj);
		if(interrupted) return;

#if defined TIOCSERGETLSR
		/* JK00: work around for Multi IO cards without TIOCSERGETLSR */
		if( has_tiocsergetlsr ) {
			if (fstat(fd, &fstatbuf))  break;
			if( ioctl( fd, TIOCSERGETLSR, &change ) ) break;
			else if( change )
				send_event( env, jobj, SPE_OUTPUT_BUFFER_EMPTY,
					1 );
		}
#endif /* TIOCSERGETLSR */
#if defined(TIOCGICOUNT)
	/*	wait for RNG, DSR, CD or CTS  but not DataAvailable
	 *      The drawback here is it never times out so if someone
	 *      reads there will be no chance to try again.
	 *      This may make sense if the program does not want to
	 *      be notified of data available or errors.
	 *	ret=ioctl(fd,TIOCMIWAIT);
	 */
		/* JK00: only use it if supported by this port */
		if (has_tiocgicount) {
			if( ioctl( fd, TIOCGICOUNT, &sis ) ) break;
			while( sis.frame != osis.frame ) {
				send_event( env, jobj, SPE_FE, 1);
				osis.frame++;
			}
			while( sis.overrun != osis.overrun ) {
				send_event( env, jobj, SPE_OE, 1);
				osis.overrun++;
			}
			while( sis.parity != osis.parity ) {
				send_event( env, jobj, SPE_PE, 1);
				osis.parity++;
			}
			while( sis.brk != osis.brk ) {
				send_event( env, jobj, SPE_BI, 1);
				osis.brk++;
			}
			osis = sis;
		}
#endif /*  TIOCGICOUNT */
	       /* A Portable implementation */

		if( ioctl( fd, TIOCMGET, &mflags ) ) break;

		change = (mflags&TIOCM_CTS) - (omflags&TIOCM_CTS);
		if( change ) send_event( env, jobj, SPE_CTS, change );

		change = (mflags&TIOCM_DSR) - (omflags&TIOCM_DSR);
		if( change ) send_event( env, jobj, SPE_DSR, change );

		change = (mflags&TIOCM_RNG) - (omflags&TIOCM_RNG);
		if( change ) send_event( env, jobj, SPE_RI, change );

		change = (mflags&TIOCM_CD) - (omflags&TIOCM_CD);
		if( change ) send_event( env, jobj, SPE_CD, change );

		omflags = mflags;

		ioctl( fd, FIONREAD, &change );
		if( change ) {
			if(!send_event( env, jobj, SPE_DATA_AVAILABLE, 1 ))
				usleep(100000); /* select wont block */
		}
	}
	return;
}

/*----------------------------------------------------------
 isDeviceGood

   accept:      a port name
   perform:     see if the port is valid on this OS.
   return:      JNI_TRUE if it exhists otherwise JNI_FALSE
   exceptions:  none
   comments:
----------------------------------------------------------*/
JNIEXPORT jboolean  JNICALL RXTXCommDriver(isDeviceGood)(JNIEnv *env,
	jobject jobj, jstring tty_name)
{
	jboolean result;
	static struct stat mystat;
	char teststring[256];
	int fd,i;
	const char *name = (*env)->GetStringUTFChars(env, tty_name, 0);


	for(i=0;i<64;i++){
#if defined(_GNU_SOURCE)
		snprintf(teststring, 256, "%s%s%i",DEVICEDIR,name, i);
#else
		sprintf(teststring,"%s%s%i",DEVICEDIR,name, i);
#endif /* _GNU_SOURCE */
		stat(teststring,&mystat);
/* XXX the following hoses freebsd when it tries to open the port later on */
#ifndef __FreeBSD__
		if(S_ISCHR(mystat.st_mode)){
			fd=open(teststring,O_RDONLY|O_NONBLOCK);
			if (fd>0){
				close(fd);
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
		fd=open(teststring,O_RDONLY|O_NONBLOCK);
		if (fd>0){
			close(fd);
			result=JNI_TRUE;
		}
	}
	(*env)->ReleaseStringUTFChars(env, tty_name, name);
	return( JNI_TRUE );
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
	return (*env)->NewStringUTF(env, DEVICEDIR);
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
	report("setInputBufferSize is not implemented\n");
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
	report("getInputBufferSize is not implemented\n");
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
	report("setOutputBufferSize is not implemented\n");
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
	report("getOutputBufferSize is not implemented\n");
	return(1);
}

/*----------------------------------------------------------
 is_interrupted

   accept:      
   perform:     see if the port is being closed. 
   return:      a positive value if the port is being closed.
   exceptions:  none
   comments:
----------------------------------------------------------*/
jboolean is_interrupted(JNIEnv *env, jobject jobj)
{
	jmethodID foo;
	jclass jclazz;
	int result;

	(*env)->ExceptionClear(env);

	jclazz = (*env)->GetObjectClass( env, jobj );
	if(jclazz == NULL) return JNI_TRUE;

	foo = (*env)->GetMethodID( env, jclazz, "checkMonitorThread", "()Z");
	if(foo == NULL) return JNI_TRUE;

	result = (*env)->CallBooleanMethod( env, jobj, foo );

#ifdef DEBUG
	if((*env)->ExceptionOccurred(env)) {
		report ("an error occured calling sendEvent()\n");
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
#endif /* DEBUG */
	(*env)->DeleteLocalRef( env, jclazz );

	return(result);
}

/*----------------------------------------------------------
 send_event

   accept:      The event type and the event state     
   perform:     if state is > 0 send a JNI_TRUE event otherwise send JNI_FALSE
   return:      a positive value if the port is being closed.
   exceptions:  none
   comments:
----------------------------------------------------------*/
int send_event(JNIEnv *env, jobject jobj, jint type, int flag)
{
	int result;
	jmethodID foo;
	jclass jclazz = (*env)->GetObjectClass( env, jobj );

	if(jclazz == NULL) return JNI_TRUE;
	foo = (*env)->GetMethodID( env, jclazz, "sendEvent", "(IZ)Z" );

	(*env)->ExceptionClear(env);

	result = (*env)->CallBooleanMethod( env, jobj, foo, type,
		flag > 0 ? JNI_TRUE : JNI_FALSE );

#ifdef DEBUG
	if((*env)->ExceptionOccurred(env)) {
		report ("an error occured calling sendEvent()\n");
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
#endif /* DEBUG */
	(*env)->DeleteLocalRef( env, jclazz );
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

	if( !jfd ) {
		(*env)->ExceptionDescribe( env );
		(*env)->ExceptionClear( env );
		return result;
	}
	result = (int)( (*env)->GetIntField( env, jobj, jfd ) );
/* ct7 & gel * Added DeleteLocalRef */
	(*env)->DeleteLocalRef( env, jclazz );
#ifdef DEBUG
	if(!strncmp("fd",id,2) && result == 0)
		report("invalid file descriptor\n");
#endif /* DEBUG */
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
	if( !clazz ) {
		(*env)->ExceptionDescribe( env );
		(*env)->ExceptionClear( env );
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
	fprintf(stderr, msg);
#endif /* DEBUG */
}

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

		The File System Hierarchy Standard
		http://www.pathname.com/fhs/

		FSSTND
		ftp://tsx-11.mit.edu/pub/linux/docs/linux-standards/fsstnd/

		Proposed Changes to the File System Hierarchy Standard
		ftp://scicom.alphacdc.com/pub/linux/devlock-0.X.tgz

		"UNIX Network Programming", W. Richard Stevens,
		Prentice-Hall, 1990, pages 96-101.

----------------------------------------------------------*/
int fhs_lock(const char *filename)
{
#ifdef LOCKFILES
	int i,j,fd, pid;
	char lockinfo[12], file[80], pid_buffer[20], message[80],*p;
	struct stat buf;
	const char *lockdirs[]={ "/etc/locks", "/usr/spool/kermit", 
		"/usr/spool/locks", "/usr/spool/uucp", "/usr/spool/uucp/",
		"/usr/spool/uucp/LCK", "/var/lock", "/var/lock/modem", 
		"/var/spool/lock", "/var/spool/locks", "/var/spool/uucp",NULL
	};

	/* no lock dir? just return success */

	if (stat(LOCKDIR,&buf)!=0)
	{
		report("could not find lock directory.\n");
		return 1;
	}

	/* 
	 * There is a zoo of lockdir possibilities
	 * Its possible to check for stale processes with most of them.
	 * for now we will just check for the lockfile on most
	 * Problem lockfiles will be dealt with.  Some may not even be in use.
	 *
	 * TODO follow symbolic links (/dev/modem...)
	 */

	j=0;
	while(lockdirs[j])
	{
		if(strncmp(lockdirs[j],LOCKDIR,strlen(lockdirs[j])))
		{
			i=strlen(filename);
			p=(char *) filename+i;
			while(*(p-1)!='/' && i-- !=1) p--;
			sprintf(file,"%s/LCK..%s",lockdirs[j],p);
			if(stat(file,&buf)==0)
			{
				printf("-----------------------------------\n");
				printf("RXTX Error:  Unexpected lock file: %s\n Please report to the RXTX developers\n", file);
				printf("-----------------------------------\n");
				return 0;
				
			}
			
		}
		j++;
	}
	
	/* 
	check if the device is already locked

	There is much to do here.

		1) UUCP style locks
			/var/spool/uucp
		2) SVR4 locks
			/var/spool/locks
		3) FSSTND locks
			/var/lock (done)
		4) handle stale locks  (done except kermit locks)
		5) handle minicom lockfile contents (FSSTND?)
			"     16929 minicom root\n"  (done)
		6) there are other Lock conventions that use Major and Minor
		   numbers...
		7) Stevens recommends LCK..<pid>
	most are caught above.  If they turn out to be problematic rather than
	an exercise, we will handle them.
	*/

	i=strlen(filename);
	p=(char *) filename+i;
	while(*(p-1)!='/' && i-- !=1) p--;
	sprintf(file,"%s/LCK..%s",LOCKDIR,p);

	if(stat(file,&buf)==0)
	{
		/* check if its a stale lock */
		fd=open(file,O_RDONLY);
		read(fd,pid_buffer,11);
		close(fd);
		sscanf(pid_buffer, "%d", &pid);

		if( kill((pid_t) pid, 0) && errno==ESRCH )
		{
			report("RXTX Warning:  Removing stale lock file.\n");
			if(unlink(file) != 0)
			{
				snprintf(message, 80, "RXTX Error:  Unable to \
					remove stale lock file: %s\n",
					file
				);
				report(message);
				return 0;
			}
		}
		else return 0;
	}
	fd=open(file, O_CREAT | O_WRONLY | O_EXCL, 0666);
	if(fd < 0)
	{
		snprintf(message, 80,
			"RXTX Error: Unable to create lock file: %s\n\n", file);
		return 0;
	}
	sprintf(lockinfo,"%10d\n",getpid());
	write(fd, lockinfo,11);
	close(fd);
	return 1;

#else /* FIXME... This needs to work on all systems that use Lock Files */
	return 1;
#endif /* LOCKFILES */

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
void fhs_unlock(const char *filename)
{
#ifdef LOCKFILES
	char file[80],*p;
	int i;

	i=strlen(filename);
	p=(char *) filename+i;
	while(*(p-1)!='/' && i-- !=0) p--;
	sprintf(file,"%s/LCK..%s",LOCKDIR,p);

	unlink(file);
#endif /* LOCKFILES */
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
	int i;

	fprintf(stderr,"%s %o\n",foo,ttyset->c_iflag);
	fprintf(stderr,"%s %o\n",foo,ttyset->c_lflag);
	fprintf(stderr,"%s %o\n",foo,ttyset->c_oflag);
	fprintf(stderr,"%s %o\n",foo,ttyset->c_cflag);
	for(i=0;i<NCCS;i++)
	{
		fprintf(stderr,"%s %o ",foo,ttyset->c_cc[i]);
	}
	fprintf(stderr,"\n");
}
