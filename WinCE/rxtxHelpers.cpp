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
#include "StdAfx.h"
#include <stdio.h>
#include <stdlib.h>
#include "rxtxHelpers.h"

/*----------------------------------------------------------
get_java_int_var

   accept:      env (keyhole to java)
                jobj (java RXTXPort object)
   return:      the jint field from the java object, casted to long
   exceptions:  none
   comments:
----------------------------------------------------------*/
long get_java_int_var( JNIEnv *env, jobject jobj, char *id)
{
	long result = 0;
	jclass jclazz = env->GetObjectClass(jobj);
	jfieldID jfd = env->GetFieldID(jclazz, id, "I");

	if( !jfd ) {
    IF_DEBUG
    (
		  env->ExceptionDescribe();
    )
		env->ExceptionClear();
		env->DeleteLocalRef(jclazz);
		return result;
	}
	result = (long)( env->GetIntField(jobj, jfd) );
	env->DeleteLocalRef(jclazz);
	return result;
}


/*----------------------------------------------------------
get_java_boolean_var

   accept:      env (keyhole to java)
                jobj (java RXTXPort object)
   return:      the jboolean field from the java object, converted to bool
   exceptions:  none
   comments:
----------------------------------------------------------*/
bool get_java_boolean_var( JNIEnv *env, jobject jobj, char *id)
{
	bool result = FALSE;
	jclass jclazz = env->GetObjectClass(jobj);
	jfieldID jfd = env->GetFieldID(jclazz, id, "Z");

	if( !jfd ) {
    IF_DEBUG
    (
		  env->ExceptionDescribe();
    )
		env->ExceptionClear();
		env->DeleteLocalRef(jclazz);
		return result;
	}
	result = (env->GetBooleanField(jobj, jfd)) != JNI_FALSE;
	env->DeleteLocalRef(jclazz);
	return result;
}


/*----------------------------------------------------------
get_java_boolean_var2

   accept:      env (keyhole to java)
                jobj (java RXTXPort object)
                jclazz (class of jobj)
   return:      the jboolean field from the java object, converted to bool
   exceptions:  none
   comments:    can be faster for fetching many variables one by one
----------------------------------------------------------*/
bool get_java_boolean_var2(JNIEnv *env, jobject jobj, jclass jclazz, char *id)
{
	bool result = FALSE;
	jfieldID jfd = env->GetFieldID(jclazz, id, "Z");

	if( !jfd ) {
    IF_DEBUG
    (
		  env->ExceptionDescribe();
    )
		env->ExceptionClear();
		env->DeleteLocalRef(jclazz);
		return result;
	}
	result = (env->GetBooleanField(jobj, jfd)) != JNI_FALSE;
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
void throw_java_exception(JNIEnv *env, const char *exc, const char *foo, const char *msg)
{
	char buf[250];
	jclass clazz = env->FindClass(exc);
	if( !clazz ) {
    IF_DEBUG
    (
		  env->ExceptionDescribe();
    )
		env->ExceptionClear();
		return;
	}
	_snprintf(buf, 60, "%s: %s in %s", exc, msg, foo);
	env->ThrowNew(clazz, buf);
	env->DeleteLocalRef(clazz);
}

// Unicode version:
void throw_java_exceptionW(JNIEnv *env, const char *exc, const wchar_t *foo, const wchar_t *msg)
{
	wchar_t buf[500];
  char *lpcBuf;
  int msgLen;

	jclass clazz = env->FindClass(exc);
	if( !clazz ) {
		env->ExceptionDescribe();
		env->ExceptionClear();
		return;
	}
	msgLen = swprintf(buf, L"%S: %s in %s", exc, msg, foo);
  
  IF_DEBUG
  (
    printj(env, L"!!!!! Throwing %s\n", buf);
    //MessageBox(NULL, buf, L"throw_java_exceptionW"), MB_OK | MB_ICONEXCLAMATION | MB_SETFOREGROUND);
  )

  lpcBuf = (char *)malloc((msgLen+1)*sizeof(wchar_t)); // Will be ok - mbs not longer than wcs
  lpcBuf[msgLen] = '\0';
  wcstombs(lpcBuf, buf, msgLen);
  env->ThrowNew(clazz, lpcBuf);
  free(lpcBuf);
	env->DeleteLocalRef(clazz);
}


/*----------------------------------------------------------
get_fd

   accept:      env (keyhole to java)
                jobj (java RXTXPort object)
   return:      "fd" field from the java object, as HANDLE
   exceptions:  none
   comments:
----------------------------------------------------------*/
HANDLE get_fd(JNIEnv *env, jobject jobj)
{
  HANDLE fd = (HANDLE)get_java_int_var(env, jobj, "fd");
  return fd!=0?fd:INVALID_HANDLE_VALUE;
}


/*----------------------------------------------------------
get_eis

   accept:      env (keyhole to java)
                jobj (java RXTXPort object)
   return:      "eis" field from the java object, as (EventInfoStruct *)
   exceptions:  none
   comments:
----------------------------------------------------------*/
EventInfoStruct *get_eis(JNIEnv *env, jobject jobj)
{
  return (EventInfoStruct *)get_java_int_var(env, jobj, "eis");
}


/*----------------------------------------------------------
printj

   accept:      like vwprintf()
   return:      number of jchars written or -1
   exceptions:  none
   comments:    prints data using System.out.print()
----------------------------------------------------------*/
int printj(JNIEnv *env, wchar_t *fmt, ...)
{
  wchar_t buf[1024];
  int retval;
  jstring jsBuf;
  jclass clsSystem, clsOut;
  jfieldID jfid;
  jobject objOut;
  jmethodID midPrint;

  va_list ap;
  va_start(ap, fmt);
  retval = _vsnwprintf(buf, 1024, fmt, ap);
  va_end(ap);
  buf[1023] = '\0';
  
  if((clsSystem = env->FindClass("java/lang/System")) == NULL)
  {
		IF_DEBUG
    (
      env->ExceptionDescribe();
    )
		env->ExceptionClear();
    return -1;
  }
  
  if((jfid = env->GetStaticFieldID(clsSystem, "out", "Ljava/io/PrintStream;")) == NULL)
  {
		IF_DEBUG
    (
      env->ExceptionDescribe();
    )
		env->ExceptionClear();
    env->DeleteLocalRef(clsSystem);
    return -1;
  }
  
  objOut = env->GetStaticObjectField(clsSystem, jfid);
	clsOut = env->GetObjectClass(objOut);

  if((midPrint = env->GetMethodID(clsOut, "print", "(Ljava/lang/String;)V")) == NULL)
  {
		IF_DEBUG
    (
      env->ExceptionDescribe();
    )
		env->ExceptionClear();
    env->DeleteLocalRef(clsOut);
    env->DeleteLocalRef(clsSystem);
    return -1;
  }
  
	jsBuf = env->NewString(buf, wcslen(buf));

  env->CallVoidMethod(objOut, midPrint, jsBuf);

 	env->DeleteLocalRef(jsBuf);
  env->DeleteLocalRef(clsOut);
  env->DeleteLocalRef(clsSystem);
  
  return retval;
}



/*----------------------------------------------------------
CommEventThread

   accept:      communication structure ptr
   return:      0 - normal finish, other - error
   exceptions:  none
   comments:    runs as separate thread
----------------------------------------------------------*/
DWORD __stdcall CommEventThread(LPVOID lpEventInfo)
{ 
  DWORD dwErr;
  EventInfoStruct *EventInfo = (EventInfoStruct *)lpEventInfo;
  HANDLE hPort = EventInfo->fd;
  
  // Specify a set of events to be monitored for the port.
  if(!SetCommMask(hPort, EventInfo->ef))
  {
    dwErr = GetLastError();
    IF_DEBUG
    (
      LPCWSTR lpMsgBuf;
      CreateErrorMsg(dwErr, lpMsgBuf);
      MessageBoxW(NULL, lpMsgBuf, L"!!! CommEventThread - SetCommMask() error", MB_OK | MB_ICONEXCLAMATION | MB_SETFOREGROUND);
      ReleaseErrorMsg(lpMsgBuf);
    )
    return dwErr;
  }

  // Thread is ready to work
  EventInfo->eventThreadReady = true;

  do
  {
    // Wait for an event to occur for the port.
    if(!WaitCommEvent(hPort, &(EventInfo->event), NULL))
    {
      dwErr = GetLastError();
      if(dwErr == ERROR_INVALID_PARAMETER) 
      { // Mask is empty - let's wait for a moment and continue
        MessageBeep(MB_ICONQUESTION);
        Sleep(200);
        continue;
      }

      if(dwErr == ERROR_INVALID_HANDLE)
      { // Port was closed
        //MessageBox(NULL, L"--- CommEventThread - Port closed", L"ERROR_INVALID_HANDLE", MB_OK | MB_ICONEXCLAMATION | MB_SETFOREGROUND);
        free(EventInfo);
        return 0;
      }
      
      IF_DEBUG
      (
        LPCWSTR lpMsgBuf;
        WCHAR MsgBuf2[1000];
        CreateErrorMsg(dwErr, lpMsgBuf);
        wsprintfW(MsgBuf2, L"%ld %s, CommMask==%ld", dwErr, lpMsgBuf, EventInfo->ef);
        MessageBoxW(NULL, MsgBuf2, L"!!! CommEventThread - WaitCommEvent() error", MB_OK | MB_ICONEXCLAMATION | MB_SETFOREGROUND);
        ReleaseErrorMsg(lpMsgBuf);
      )
      return dwErr;
    }

    IF_DEBUG
    (
      MessageBeep(MB_ICONEXCLAMATION);
      //MessageBoxW(NULL, L"--- RXTXPort.CommEventThread() - CommEvent", L"CommEventThread", MB_OK | MB_ICONEXCLAMATION | MB_SETFOREGROUND);
    )
    
    // Re-specify the set of events to be monitored for the port.
    if(!SetCommMask(hPort, EventInfo->ef))
    {
      dwErr = GetLastError();
      IF_DEBUG
      (
        LPCWSTR lpMsgBuf;
        CreateErrorMsg(dwErr, lpMsgBuf);
        MessageBoxW(NULL, lpMsgBuf, L"!!! CommEventThread - SetCommMask() error", MB_OK | MB_ICONEXCLAMATION | MB_SETFOREGROUND);
        ReleaseErrorMsg(lpMsgBuf);
      )
      return dwErr;
    }

    if(!SetEvent(EventInfo->eventHandle))
    {
      dwErr = GetLastError();
      if(dwErr == ERROR_INVALID_HANDLE) // Event was closed
      {
        //MessageBoxW(NULL, L"--- CommEventThread - Event closed", L"ERROR_INVALID_HANDLE", MB_OK | MB_ICONEXCLAMATION | MB_SETFOREGROUND);
        free(EventInfo);
        return 0;
      }

      IF_DEBUG
      (
        LPCWSTR lpMsgBuf;
        CreateErrorMsg(dwErr, lpMsgBuf);
        MessageBoxW(NULL, lpMsgBuf, L"!!! CommEventThread - SetEvent() error", MB_OK | MB_ICONEXCLAMATION | MB_SETFOREGROUND);
        ReleaseErrorMsg(lpMsgBuf);
      )
      return dwErr;
    }

    // Wait for receiving and clearing event
    while(EventInfo->event != 0)
      Sleep(0);

  } while(TRUE);
}


/*----------------------------------------------------------
setEventFlags

   accept:      EventFlag table
   return:      none
   exceptions:  none
   comments:    sets EventFlag table according to Java 
                MonitorThread fields
----------------------------------------------------------*/
/*void setEventFlags(JNIEnv *env, jobject jobjPort, bool ef[])
{
	jfieldID jfid;
  jclass cls;
  jobject jobj;
  jclass clsPort = env->GetObjectClass(jobjPort);

  if((jfid = env->GetFieldID(clsPort, "monThread", "Lgnu/io/RXTXPort$MonitorThread;")) == NULL)
  {
		IF_DEBUG
    (
      env->ExceptionDescribe();
    )
		env->ExceptionClear();
    env->DeleteLocalRef(clsPort);
    return;
  }
  jobj = env->GetObjectField(jobjPort, jfid);
	cls = env->GetObjectClass(jobj);

  ef[SPE_DATA_AVAILABLE] = get_java_boolean_var2(env, jobj, cls, "Data");
  ef[SPE_OUTPUT_BUFFER_EMPTY] = get_java_boolean_var2(env, jobj, cls, "Output");
  ef[SPE_CTS] = get_java_boolean_var2(env, jobj, cls, "CTS");
  ef[SPE_DSR] = get_java_boolean_var2(env, jobj, cls, "DSR");
  ef[SPE_RI] = get_java_boolean_var2(env, jobj, cls, "RI");
  ef[SPE_CD] = get_java_boolean_var2(env, jobj, cls, "CD");
  ef[SPE_OE] = get_java_boolean_var2(env, jobj, cls, "OE");
  ef[SPE_PE] = get_java_boolean_var2(env, jobj, cls, "PE");
  ef[SPE_FE] = get_java_boolean_var2(env, jobj, cls, "FE");
  ef[SPE_BI] = get_java_boolean_var2(env, jobj, cls, "BI");

  env->DeleteLocalRef(clsPort);
  env->DeleteLocalRef(cls);
  env->DeleteLocalRef(jobj);
}
*/

/*----------------------------------------------------------
InitialiseEventInfoStruct

   accept:      Port handle
   return:      0 - OK, other - error number
   exceptions:  none
   comments:    Structure for communication with thread
----------------------------------------------------------*/
int InitialiseEventInfoStruct(HANDLE hPort, EventInfoStruct **EventInfoPtr)
{
  DWORD dwErr;
  WCHAR wsEventName[MAX_PATH];
  
  (*EventInfoPtr) = (EventInfoStruct *)malloc(sizeof(EventInfoStruct));
  if(*EventInfoPtr == NULL)
    return ERROR_NOT_ENOUGH_MEMORY;

  memset(*EventInfoPtr, 0, sizeof(EventInfoStruct));
  (*EventInfoPtr)->fd = hPort;
  wsprintfW(wsEventName, L"rxtxPort.SerEvt%lx", hPort);
  (*EventInfoPtr)->eventHandle = CreateEventW(NULL, FALSE, FALSE, wsEventName);
  dwErr = GetLastError();
  if((*EventInfoPtr)->eventHandle == NULL || dwErr == ERROR_ALREADY_EXISTS)
  {
    IF_DEBUG
    (
      LPCWSTR lpMsgBuf;
      CreateErrorMsg(dwErr, lpMsgBuf);
      MessageBoxW(NULL, lpMsgBuf, L"!!! InitialiseEventInfoStruct - CreateEvent() error", MB_OK | MB_ICONEXCLAMATION | MB_SETFOREGROUND);
      ReleaseErrorMsg(lpMsgBuf);
    )
    free(*EventInfoPtr);
    return dwErr;
  }

  // Set initial event flags: It should be not empty, but unused here and relatively rare event
  (*EventInfoPtr)->ef = EV_POWER; //In WinNT you can use EV_EVENT2 instead
  return 0;
}


/*----------------------------------------------------------
SendEvents

   accept:      
   return:      
   exceptions:  none
   comments:    Sends events needed
----------------------------------------------------------*/
int SendEvents(JNIEnv *env, jobject jobj, DWORD dwEvent, EventInfoStruct *EventInfo, jmethodID jmSendEvent)
{
  DWORD dwErrors;
  COMSTAT Stat;

  IF_DEBUG
  (
    printj(env, L"--- SendEvents(): event %#lx\n", dwEvent);
  )

  if(!ClearCommError(EventInfo->fd, &dwErrors, &Stat))
  {
    IF_DEBUG
    (
      LPCWSTR lpMsgBuf;
      CreateErrorMsg(GetLastError(), lpMsgBuf);
      printj(env, L"!!! SendEvents - ClearCommError() error: %s\n", lpMsgBuf);
      ReleaseErrorMsg(lpMsgBuf);
    )
    return -1;
  }

  // Ignore not subscribed events
  dwEvent &= EventInfo->ef;

  if(dwEvent & EV_RXCHAR)
  {
    IF_DEBUG
    (
      printj(env, L"--- SendEvents(): EV_RXCHAR\n");
    )
    if(env->CallBooleanMethod(jobj, jmSendEvent, SPE_DATA_AVAILABLE, JNI_TRUE) == JNI_TRUE)
      return SPE_DATA_AVAILABLE;
  }

  if(dwEvent & EV_TXEMPTY)
  {
    IF_DEBUG
    (
      printj(env, L"--- SendEvents(): EV_TXEMPTY\n");
    )
    if(env->CallBooleanMethod(jobj, jmSendEvent, SPE_OUTPUT_BUFFER_EMPTY, JNI_TRUE) == JNI_TRUE)
      return SPE_OUTPUT_BUFFER_EMPTY;
  }

  if(dwEvent & EV_CTS)
  {
    IF_DEBUG
    (
      printj(env, L"--- SendEvents(): EV_CTS\n");
    )
    if(env->CallBooleanMethod(jobj, jmSendEvent, SPE_CTS, Stat.fCtsHold?JNI_TRUE:JNI_TRUE) == JNI_TRUE)
      return SPE_CTS;
  }

  if(dwEvent & EV_DSR)
  {
    IF_DEBUG
    (
      printj(env, L"--- SendEvents(): EV_DSR\n");
    )
    if(env->CallBooleanMethod(jobj, jmSendEvent, SPE_DSR, Stat.fDsrHold?JNI_TRUE:JNI_TRUE) == JNI_TRUE)
      return SPE_DSR;
  }

  if(dwEvent & EV_RING)
  {
    IF_DEBUG
    (
      printj(env, L"--- SendEvents(): EV_RING\n");
    )
    if(env->CallBooleanMethod(jobj, jmSendEvent, SPE_RI, JNI_TRUE) == JNI_TRUE)
      return SPE_RI;
  }
  
  if(dwEvent & EV_RLSD)
  {
    IF_DEBUG
    (
      printj(env, L"--- SendEvents(): EV_RLSD\n");
    )
    if(env->CallBooleanMethod(jobj, jmSendEvent, SPE_CD, Stat.fRlsdHold?JNI_TRUE:JNI_TRUE) == JNI_TRUE)
      return SPE_CD;
  }

  if(dwEvent & EV_ERR)
  {
    if(dwErrors & CE_OVERRUN)
    {
      IF_DEBUG
      (
        printj(env, L"--- SendEvents(): CE_OVERRUN\n");
      )
      if(env->CallBooleanMethod(jobj, jmSendEvent, SPE_OE, JNI_TRUE) == JNI_TRUE)
        return SPE_OE;
    }

    if(dwErrors & CE_RXPARITY)
    {
      IF_DEBUG
      (
        printj(env, L"--- SendEvents(): CE_RXPARITY\n");
      )
      if(env->CallBooleanMethod(jobj, jmSendEvent, SPE_PE, JNI_TRUE) == JNI_TRUE)
        return SPE_PE;
    }

    if(dwErrors & CE_FRAME)
    {
      IF_DEBUG
      (
        printj(env, L"--- SendEvents(): CE_FRAME\n");
      )
      if(env->CallBooleanMethod(jobj, jmSendEvent, SPE_FE, JNI_TRUE) == JNI_TRUE)
        return SPE_FE;
    }
  }

  if(dwEvent & EV_BREAK)
  {
    IF_DEBUG
    (
      printj(env, L"--- SendEvents(): EV_BREAK\n");
    )
    if(env->CallBooleanMethod(jobj, jmSendEvent, SPE_BI, JNI_TRUE) == JNI_TRUE)
      return SPE_BI;
  }
  
  return 0;
}

