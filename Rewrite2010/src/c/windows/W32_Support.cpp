/*-------------------------------------------------------------------------
|   RXTX License v 2.1 - LGPL v 2.1 + Linking Over Controlled Interface.
|   RXTX is a native interface to serial ports in java.
|   Copyright 1997-2007 by Trent Jarvi tjarvi@qbang.org and others who
|   actually wrote it.  See individual source files for more information.
|
|   A copy of the LGPL v 2.1 may be found at
|   http://www.gnu.org/licenses/lgpl.txt on March 4th 2007.  A copy is
|   here for your convenience.
|
|   This library is free software; you can redistribute it and/or
|   modify it under the terms of the GNU Lesser General Public
|   License as published by the Free Software Foundation; either
|   version 2.1 of the License, or (at your option) any later version.
|
|   This library is distributed in the hope that it will be useful,
|   but WITHOUT ANY WARRANTY; without even the implied warranty of
|   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
|   Lesser General Public License for more details.
|
|   An executable that contains no derivative of any portion of RXTX, but
|   is designed to work with RXTX by being dynamically linked with it,
|   is considered a "work that uses the Library" subject to the terms and
|   conditions of the GNU Lesser General Public License.
|
|   The following has been added to the RXTX License to remove
|   any confusion about linking to RXTX.   We want to allow in part what
|   section 5, paragraph 2 of the LGPL does not permit in the special
|   case of linking over a controlled interface.  The intent is to add a
|   Java Specification Request or standards body defined interface in the 
|   future as another exception but one is not currently available.
|
|   http://www.fsf.org/licenses/gpl-faq.html#LinkingOverControlledInterface
|
|   As a special exception, the copyright holders of RXTX give you
|   permission to link RXTX with independent modules that communicate with
|   RXTX solely through the Sun Microsytems CommAPI interface version 2,
|   regardless of the license terms of these independent modules, and to copy
|   and distribute the resulting combined work under terms of your choice,
|   provided that every copy of the combined work is accompanied by a complete
|   copy of the source code of RXTX (the version of RXTX used to produce the
|   combined work), being distributed under the terms of the GNU Lesser General
|   Public License plus this exception.  An independent module is a
|   module which is not derived from or based on RXTX.
|
|   Note that people who make modified versions of RXTX are not obligated
|   to grant this special exception for their modified versions; it is
|   their choice whether to do so.  The GNU Lesser General Public License
|   gives permission to release a modified version without this exception; this
|   exception also makes it possible to release a modified version which
|   carries forward this exception.
|
|   You should have received a copy of the GNU Lesser General Public
|   License along with this library; if not, write to the Free
|   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
|   All trademarks belong to their respective owners.
--------------------------------------------------------------------------*/

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#define WIN32_LEAN_AND_MEAN

#include <windows.h>
#include <RXTX_Globals.h>
#include <RXTX_Classes.h>
#include <RXTX_Loopbacks.h>
#include <RXTX_Exceptions.h>
#include "W32_Classes.h"

#ifndef _THIS_FILE_
    #define _THIS_FILE_ "W32_Support.cpp"
#endif

// ------------------------------------------------------------------------- //
// Class CommPortFactory
// ------------------------------------------------------------------------- //

CommPort* CommPortFactory::getInstance(const char *portName, int portType)
{
#ifdef RXTX_INCLUDE_SOFTWARE_LOOPBACKS
    if (strcmp("PARALLEL_PASS", portName) == 0)
        return new ParallelLoopback(portName);
    if (strcmp("SERIAL_PASS", portName) == 0)
        return new SerialLoopback(portName);
#endif
    // Do not open overlapped. Let the Java application
    // handle multi-threaded I/O.
    HANDLE hComm = CreateFile( portName,
        GENERIC_READ | GENERIC_WRITE,
        0,
        0,
        OPEN_EXISTING,
        0,
        0
    );
    if (hComm == INVALID_HANDLE_VALUE)
    {
        LPVOID lpMsgBuf;
        FormatMessage( 
            FORMAT_MESSAGE_ALLOCATE_BUFFER | 
            FORMAT_MESSAGE_FROM_SYSTEM | 
            FORMAT_MESSAGE_IGNORE_INSERTS,
            NULL,
            GetLastError(),
            MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
            (LPTSTR) &lpMsgBuf,
            0,
            NULL 
            );
        printf("%s\n", lpMsgBuf);
        delete lpMsgBuf;
        throw IOException();
    }
    if (portType == RXTX_PORT_PARALLEL)
    {
        ParallelPortImpl* port = new ParallelPortImpl(portName, hComm);
        try
        {
            port->initializePort();
        }
        catch (exception &e)
        {
            delete port;
            throw e;
        }
        return port;
    }
    if (portType == RXTX_PORT_SERIAL)
    {
        SerialPortImpl* port = new SerialPortImpl(portName, hComm);
        try
        {
            port->initializePort();
        }
        catch (exception &e)
        {
            delete port;
            throw e;
        }
        return port;
    }
    throw IllegalArgumentException();
}

// ------------------------------------------------------------------------- //
// I/O Port Enumeration
// ------------------------------------------------------------------------- //

// http://msdn.microsoft.com/en-us/library/ms724256%28v=VS.85%29.aspx

void getPortsFromRegistry(PortInfoList* portInfoList, int portType, const char* keyStr)
{
    HKEY hKey;
    if (RegOpenKeyEx(HKEY_LOCAL_MACHINE, keyStr, 0, KEY_QUERY_VALUE, &hKey) != ERROR_SUCCESS)
    {
        return;
    }
    DWORD maxValueNameChars, maxValueDataBytes;
    if (RegQueryInfoKey(hKey, NULL, NULL, NULL, NULL, NULL, NULL, NULL, &maxValueNameChars, &maxValueDataBytes, NULL, NULL) == ERROR_SUCCESS)
    {
        maxValueNameChars++;
        maxValueDataBytes++;
        TCHAR* valueName = new TCHAR[maxValueNameChars];
        BYTE* valueData = new BYTE[maxValueDataBytes];
        DWORD index = 0;
        DWORD dataType;
        DWORD currentValueNameSize = maxValueNameChars;
        DWORD currentDataSize = maxValueDataBytes;
        valueData[0] = 0;
        while (RegEnumValue(hKey, index, valueName, &currentValueNameSize, NULL, &dataType, valueData, &currentDataSize) == ERROR_SUCCESS)
        {
            if (dataType == REG_SZ)
            {
                TCHAR* portName = reinterpret_cast<TCHAR*>(valueData);
                TCHAR* deviceStrPtr = strrchr(portName, '\\');
                if (deviceStrPtr != NULL)
                    portName = deviceStrPtr + 1;
                portInfoList->addPortInfo(portName, portType);
            }
            currentValueNameSize = maxValueNameChars;
            currentDataSize = maxValueDataBytes;
            valueData[0] = 0;
            index++;
        }
        delete valueName;
        delete valueData;
    }
    RegCloseKey(hKey);
}

void PortInfoList::getValidPorts()
{
    getPortsFromRegistry(this, RXTX_PORT_SERIAL, "HARDWARE\\DEVICEMAP\\SERIALCOMM");
    getPortsFromRegistry(this, RXTX_PORT_PARALLEL, "HARDWARE\\DEVICEMAP\\PARALLEL PORTS");
}

// ------------------------------------------------------------------------- //
// DLL Entry Point
// ------------------------------------------------------------------------- //

BOOL APIENTRY DllMain(HANDLE hModule, DWORD  ul_reason_for_call, LPVOID lpReserved)
{
    return TRUE;
}
