/*-------------------------------------------------------------------------
|   rxtx is a native interface to serial ports in java.
|   Copyright 1997, 1998, 1999 by Trent Jarvi jarvi@ezlink.com.
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


/* javax.comm.SerialPort constants */
#define DATABITS_5	5
#define DATABITS_6	6
#define DATABITS_7	7
#define DATABITS_8	8
#define STOPBITS_1	1
#define STOPBITS_2	2
#define PARITY_NONE	0
#define PARITY_ODD	1
#define PARITY_EVEN	2
#define PARITY_MARK	3
#define PARITY_SPACE	4

/* javax.comm.SerialPortEvent constants */
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
int translate_speed( JNIEnv* env, jint speed );
int translate_data_bits( JNIEnv *env, int *cflag, jint dataBits );
int translate_stop_bits( JNIEnv *env, int *cflag, jint stopBits );
int translate_parity( JNIEnv *env, int *cflag, jint parity );
int read_byte_array( int fd, unsigned char *buffer, int length, int threshold,
   int timeout );
int get_java_fd( JNIEnv *env, jobject jobj );
void send_modem_events( JNIEnv *env, jobject jobj, jmethodID method,
   int event, int change, int state );
void IOException( JNIEnv *Env, char *msg );
void UnsupportedCommOperationException( JNIEnv *env, char *msg );
