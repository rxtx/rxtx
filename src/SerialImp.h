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

/* gnu.io.SerialPort constants */
#define JDATABITS_5		5
#define JDATABITS_6		6
#define JDATABITS_7		7
#define JDATABITS_8		8
#define JPARITY_NONE		0
#define JPARITY_ODD		1
#define JPARITY_EVEN		2
#define JPARITY_MARK		3
#define JPARITY_SPACE		4
#define STOPBITS_1		1
#define STOPBITS_2		2
#define STOPBITS_1_5		3
#define FLOWCONTROL_NONE	0
#define FLOWCONTROL_RTSCTS_IN	1
#define FLOWCONTROL_RTSCTS_OUT	2
#define FLOWCONTROL_XONXOFF_IN	4
#define FLOWCONTROL_XONXOFF_OUT	8

/* gnu.io.SerialPortEvent constants */
#define SPE_DATA_AVAILABLE       1
#define SPE_OUTPUT_BUFFER_EMPTY  2
#define SPE_CTS                  3
#define SPE_DSR                  4
#define SPE_RI                   5
#define SPE_CD                   6
#define SPE_OE                   7
#define SPE_PE                   8
#define SPE_FE                   9
#define SPE_BI                  10

#define PORT_SERIAL		 1
#define PORT_PARALLEL		 2
#define PORT_I2C		 3
#define PORT_RS485		 4
#define PORT_RAW		 5

/* glue for unsupported linux speeds see also win32termios.h */

#if !defined(__APPLE__) //dima
#define B14400		1010001
#define B28800		1010002
#define B128000		1010003
#define B256000		1010004
#endif //dima


/*  Ports known on the OS */
#if defined(__linux__)
#	define DEVICEDIR "/dev/"
#	define LOCKDIR "/var/lock"
#	define LOCKFILEPREFIX "LCK.."
#	define FHS
#endif /* __linux__ */
#if defined(__sgi__) || defined(sgi)
#	define DEVICEDIR "/dev/"
#	define LOCKDIR "/usr/spool/uucp"
#	define LOCKFILEPREFIX "LK."
#	define UUCP
#endif /* __sgi__ || sgi */
#if defined(__FreeBSD__)
#	define DEVICEDIR "/dev/"
#	define LOCKDIR "/var/spool/uucp"
#	define LOCKFILEPREFIX "LK."
#	define UUCP
#endif
#if defined(__APPLE__)
#	define DEVICEDIR "/dev/"
#	define LOCKDIR "/var/spool/uucp"
#	define LOCKFILEPREFIX "LK."
#	define UUCP
#endif /* __FreeBSD__ */
#if defined(__NetBSD__)
#	define DEVICEDIR "/dev/"
#	define LOCKDIR "/usr/spool/uucp"
#	define LOCKFILEPREFIX "LK."
#	define UUCP
#endif /* __NetBSD__ */
#if defined(__hpux__)
/* modif cath */
#	define DEVICEDIR "/dev/"
#	define LOCKDIR "/usr/spool/uucp"
#	define LOCKFILEPREFIX "LK."
#	define UUCP
#endif /* __hpux__ */
#if defined(__osf__)  /* Digital Unix */
#	define DEVICEDIR "/dev/"
#	define LOCKDIR ""
#	define LOCKFILEPREFIX "LK."
#	define UUCP
#endif /* __osf__ */
#if defined(__sun__) /* Solaris */
#	define DEVICEDIR "/dev/"
#	define LOCKDIR "/var/spool/locks"
#	define LOCKFILEPREFIX "LK."
#	define UUCP
#endif /* __sun__ */
#if defined(__BEOS__)
#	define DEVICEDIR "/dev/ports/"
#	define LOCKDIR ""
#	define LOCKFILEPREFIX ""
#	define UUCP
#endif /* __BEOS__ */
#if defined(WIN32)
#	define DEVICEDIR ""
#	define LOCKDIR ""
#	define LOCKFILEPREFIX ""
#	define OPEN serial_open
#	define CLOSE serial_close
#	define WRITE serial_write
#	define READ serial_read
#else /* use the system calls for Unix */
#	define OPEN open
#	define CLOSE close
#	define WRITE write
#	define READ read
#ifdef TRACE
#define ENTER(x) report("entering "x" \n");
#define LEAVE(x) report("leaving "x" \n");
#else
#define ENTER(x)
#define LEAVE(x)
#endif /* TRACE */

#endif /* WIN32 */


/*  That should be all you need to look at in this file for porting */
#ifdef UUCP
#	define LOCK uucp_lock
#	define UNLOCK uucp_unlock
#elif defined(FHS)
#	define LOCK fhs_lock
#	define UNLOCK fhs_unlock
#else /* FSH */
#	define LOCK system_does_not_lock
#	define UNLOCK system_does_not_unlock
#endif /* UUCP */

/* java exception class names */
#define UNSUPPORTED_COMM_OPERATION "gnu/io/UnsupportedCommOperationException"
#define ARRAY_INDEX_OUT_OF_BOUNDS "java/lang/ArrayIndexOutOfBoundsException"
#define OUT_OF_MEMORY "java/lang/OutOfMemoryError"
#define IO_EXCEPTION "java/io/IOException"
#define PORT_IN_USE_EXCEPTION "gnu/io/PortInUseException"

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

/*
Flow Control defines inspired by reading how mgetty by Gert Doering does it
*/

#ifdef CRTSCTS
#define HARDWARE_FLOW_CONTROL CRTSCTS
#else
#	ifdef CCTS_OFLOW
#	define HARDWARE_FLOW_CONTROL CCTS_OFLOW|CRST_IFLOW
#	else
#		ifdef RTSFLOW
#		define HARDWARE_FLOW_CONTROL RTSFLOW|CTSFLOW
#		else
#			ifdef CRTSFL
#			define HARDWARE_FLOW_CONTROL CRTSFL
#			else
#				ifdef CTSCD
#				define HARDWARE_FLOW_CONTROL CTSCD
#				else
#					define HARDWARE_FLOW_CONTROL 0
#				endif
#			endif
#		endif
#	endif
#endif

/* PROTOTYPES */
#ifdef DEBUG_MW
extern void mexWarnMsgTxt( const char * );
extern void mexErrMsgTxt( const char * );
extern int mexPrintf( const char *, ... );
#	define printf mexPrintf
#endif /* DEBUG_MW */
#ifdef __BEOS__
data_rate translate_speed( JNIEnv*, jint  );
int translate_data_bits( JNIEnv *, data_bits *, jint );
int translate_stop_bits( JNIEnv *, stop_bits *, jint );
int translate_parity( JNIEnv *, parity_mode *, jint );
#else
int translate_speed( JNIEnv*, jint  );
int translate_data_bits( JNIEnv *, tcflag_t *, jint );
int translate_stop_bits( JNIEnv *, tcflag_t *, jint );
int translate_parity( JNIEnv *, tcflag_t *, jint );
#endif
int read_byte_array( int, unsigned char *, int, int );
int get_java_var( JNIEnv *, jobject, char *, char * );
jboolean is_interrupted(JNIEnv *, jobject );
int send_event(JNIEnv *, jobject, jint, int );
void dump_termios(char *,struct termios *);
void report_error(char *);
void report_warning(char *);
void report(char *);
void throw_java_exception( JNIEnv *, char *, char *, char * );
int lock_device( const char * );
void unlock_device( const char * );
int is_device_locked( const char * );
int check_lock_status( const char * );
void fhs_unlock(const char *, int );
int fhs_lock( const char *);
void uucp_unlock( const char *, int );
int uucp_lock( const char * );
int system_does_not_lock( const char * );
void system_does_not_unlock( const char * );
int check_group_uucp();
int check_lock_pid( const char *, int );

#define UNEXPECTED_LOCK_FILE "RXTX Error:  Unexpected lock file: %s\n Please report to the RXTX developers\n"
#define LINUX_KERNEL_VERSION_ERROR "\n\n\nRXTX WARNING:  This library was compiled to run with OS release %s and you are currently running OS release %s.  In some cases this can be a problem.  Try recompiling RXTX if you notice strange behavior.  If you just compiled RXTX make sure /usr/include/linux is a symbolic link to the include files that came with the kernel source and not an older copy.\n\n\npress enter to continue\n"
#define UUCP_ERROR "\n\n\nRXTX WARNING:  This library requires the user running applications to be in\ngroup uucp.  Please consult the INSTALL documentation.  More information is\navaiable under the topic 'How can I use Lock Files with rxtx?'\n" 
