/*-------------------------------------------------------------------------
|   rxtx is a native interface to serial ports in java.
|   Copyright 2002-2004 Michal Hobot MichalHobot@netscape.net
|   Copyright 1997-2004 by Trent Jarvi taj@parcelfarce.linux.theplanet.co.uk
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
#if !defined(Included_RXTXHELPERS_H)
#define Included_RXTXSERIAL_H

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

#define PORT_SERIAL		1
#define PORT_PARALLEL	2
#define PORT_I2C		  3
#define PORT_RS485		4
#define PORT_RAW		  5

#define CreateErrorMsg(dwError, lpMsgBuf)          \
          FormatMessage(                           \
                  FORMAT_MESSAGE_ALLOCATE_BUFFER | \
                  FORMAT_MESSAGE_FROM_SYSTEM |     \
                  FORMAT_MESSAGE_IGNORE_INSERTS,   \
                  NULL,                            \
                  dwError,                         \
                  0,                               \
                  (LPTSTR) & (lpMsgBuf),           \
                  0,                               \
                  NULL                             \
                 ),                                \
          ((WCHAR *)lpMsgBuf)[wcslen((WCHAR *)lpMsgBuf)-2] = '\0'

#define ReleaseErrorMsg(lpMsgBuf) LocalFree((LPVOID)(lpMsgBuf))

#if defined(DEBUG)
#  define IF_DEBUG(x) {x}
#else
#  define IF_DEBUG(x)  
#endif

/* java exception class names */
#define UNSUPPORTED_COMM_OPERATION "javax/comm/UnsupportedCommOperationException"
#define ARRAY_INDEX_OUT_OF_BOUNDS "java/lang/ArrayIndexOutOfBoundsException"
#define OUT_OF_MEMORY "java/lang/OutOfMemoryError"
#define IO_EXCEPTION "java/io/IOException"
#define PORT_IN_USE_EXCEPTION "javax/comm/PortInUseException"


typedef struct
{
  /* Port handle */
  HANDLE fd;
  /* flags for events */
  DWORD ef;
  /* event handle for Monitor interthread signalling*/
  HANDLE eventHandle;
  /* current serial event */
  DWORD event;
  /* EventThread sets this flag to TRUE when it's ready */
  bool eventThreadReady;
} EventInfoStruct;


long get_java_int_var(JNIEnv *, jobject, char *);
bool get_java_boolean_var(JNIEnv *, jobject, char *);
bool get_java_boolean_var2(JNIEnv *, jobject, jclass, char *);
void throw_java_exception(JNIEnv *, const char *, const char *, const char *);
void throw_java_exceptionW(JNIEnv *, const char *, const wchar_t *, const wchar_t *);
HANDLE get_fd(JNIEnv *, jobject);
EventInfoStruct *get_eis(JNIEnv *, jobject);
int printj(JNIEnv *, wchar_t *, ...);
DWORD __stdcall CommEventThread(LPVOID);
//void setEventFlags(JNIEnv *env, jobject jobj, bool ef[]);
int InitialiseEventInfoStruct(HANDLE, EventInfoStruct **);
int SendEvents(JNIEnv *, jobject, DWORD, EventInfoStruct *, jmethodID);

#endif //Included_RXTXHELPERS_H
