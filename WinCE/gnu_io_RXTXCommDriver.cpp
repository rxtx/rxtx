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
#include "rxtxHelpers.h"

/*
nativeGetVersion

   accept:      none
   perform:     return the current version 
   return:      version
   exceptions:  none
   comments:    This is used to avoid mixing versions of the .jar and
		native library.
		First introduced in rxtx-1.5-9
 * Class:     gnu_io_RXTXCommDriver
 * Method:    nativeGetVersion
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_gnu_io_RXTXCommDriver_nativeGetVersion(JNIEnv *env, jclass cls)
{
  return env->NewStringUTF("RXTX-2.0-1");
}

/*
 registerKnownPorts

   accept:      the type of port
   perform:     register any ports of the desired type a priori known to this OS
   return:      JNI_TRUE if any such ports were registered otherwise JNI_FALSE
   exceptions:  none
   comments:
 * Class:     gnu_io_RXTXCommDriver
 * Method:    registerKnownPorts
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_gnu_io_RXTXCommDriver_registerKnownPorts(JNIEnv *env, jobject jobj, jint portType)
{
	enum {PORT_TYPE_SERIAL = 1,
		PORT_TYPE_PARALLEL,
		PORT_TYPE_I2C,
		PORT_TYPE_RS485,
		PORT_TYPE_RAW};
	jboolean result = JNI_FALSE;

	switch(portType) {
		case PORT_TYPE_SERIAL:
      // We could check here what COMs are on system and register them.
      break;
		
    case PORT_TYPE_PARALLEL:
		case PORT_TYPE_I2C:
		case PORT_TYPE_RS485:
		case PORT_TYPE_RAW:      
      break;
		
    default:
      // Wrong port type - I'd raise exception, but there's no defined for Java
      IF_DEBUG
      (
        printj(env, L"!!! RXTXCommDriver.registerKnownPorts(%ld): Wrong port type\n", portType);
        //MessageBox(NULL, TEXT("RXTXCommDriver.registerKnownPorts(): Wrong port type"), TEXT("Error"), MB_OK);
      )
      break;
	}
	return result;
}

/*
 isPortPrefixValid

   accept:      a port prefix
   perform:     see if the port prefix matches a port that is valid on this OS.
   return:      JNI_TRUE if it exists otherwise JNI_FALSE
   exceptions:  none
   comments:
 * Class:     gnu_io_RXTXCommDriver
 * Method:    isPortPrefixValid
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_gnu_io_RXTXCommDriver_isPortPrefixValid(JNIEnv *env, jobject jobj, jstring dev)
{
	jboolean retVal;
  const char *szDev = env->GetStringUTFChars(dev, NULL);
  if( strncmp(szDev, "COM", 3) == 0 || strncmp(szDev, "LPT", 3) == 0)  // is first 3 chars OK?
    retVal = JNI_TRUE;
  else
    retVal = JNI_FALSE;
  
  env->ReleaseStringUTFChars(dev, szDev);

  return retVal;

}

/*
 testRead

   accept:      dev The device to be tested
   perform:     test if the device can be read from
   return:      JNI_TRUE if the device can be read from
   exceptions:  none
   comments:    From Wayne Roberts wroberts1@home.com
   		check tcget/setattr returns.
		support for non serial ports Trent
 * Class:     gnu_io_RXTXCommDriver
 * Method:    testRead
 * Signature: (Ljava/lang/String;I)Z
 */
JNIEXPORT jboolean JNICALL Java_gnu_io_RXTXCommDriver_testRead(JNIEnv *env, jobject jobj, jstring dev, jint type)
{
	jboolean retVal;
  DWORD dwError;

  if ( type == PORT_SERIAL )
  {
    const WCHAR *wszDev = env->GetStringChars(dev, NULL); 
    HANDLE hPort = CreateFileW(wszDev,       // Pointer to the name of the port
                               GENERIC_READ | GENERIC_WRITE,
                                             // Access (read-write) mode
                               0,            // Share mode
                               NULL,         // Pointer to the security attribute
                               OPEN_EXISTING,// How to open the serial port
                               0,            // Port attributes
                               NULL);        // Handle to port with attribute
                                             // to copy
    // If it fails to open the port, return FALSE.
    if ( hPort == INVALID_HANDLE_VALUE ) 
    { // Could not open the port.
      IF_DEBUG
      (
        ;//printj(env, TEXT("!!! RXTXCommDriver.testRead(%s, %ld): cannot open port\n"), wszDev, type);
        //MessageBox(NULL, TEXT("RXTXCommDriver.testRead(): cannot open port"), wszDev /*TEXT("Error")*/, MB_OK | MB_SETFOREGROUND);
      )
      dwError = GetLastError();
      retVal = JNI_FALSE;
    }
    else
    { // Port open OK - let's close it and return TRUE
      if (!CloseHandle(hPort))
        dwError = GetLastError();
      IF_DEBUG
      (
        ;//printj(env, TEXT("--- RXTXCommDriver.testRead(%s, %ld): port open OK\n"), wszDev, type);
        //MessageBox(NULL, TEXT("RXTXCommDriver.testRead(): port open OK"), wszDev /*TEXT("Success")*/, MB_OK | MB_SETFOREGROUND);
      )
      retVal = JNI_TRUE;
    }
    env->ReleaseStringChars(dev, wszDev);
  }
  else
    retVal = JNI_FALSE;

  return retVal;
}

/*
 getDeviceDirectory

   accept:      
   perform:     
   return:      the directory containing the device files
   exceptions:  
   comments:    we need it only for Unix
 * Class:     gnu_io_RXTXCommDriver
 * Method:    getDeviceDirectory
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_gnu_io_RXTXCommDriver_getDeviceDirectory(JNIEnv *env, jobject jobj)
{
  return env->NewStringUTF("");
}
