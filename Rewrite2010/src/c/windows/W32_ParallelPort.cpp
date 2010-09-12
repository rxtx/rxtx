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

#include "W32_Classes.h"
#include <RXTX_Exceptions.h>
#include <winioctl.h>

#ifndef _THIS_FILE_
#define _THIS_FILE_ "W32_ParallelPort.cpp"
#endif

// http://msdn.microsoft.com/en-us/library/cc308431.aspx
// Note that the header file is from the DDK, so some operations
// are not available to application code because they require
// kernel-level permission.

#define IOCTL_PAR_QUERY_INFORMATION CTL_CODE(FILE_DEVICE_PARALLEL_PORT, 1, METHOD_BUFFERED, FILE_ANY_ACCESS)
#define IOCTL_PAR_SET_INFORMATION   CTL_CODE(FILE_DEVICE_PARALLEL_PORT, 2, METHOD_BUFFERED, FILE_ANY_ACCESS)
#define IOCTL_IEEE1284_GET_MODE     CTL_CODE(FILE_DEVICE_PARALLEL_PORT, 5, METHOD_BUFFERED, FILE_ANY_ACCESS)

#define PARALLEL_INIT            0x1
#define PARALLEL_AUTOFEED        0x2
#define PARALLEL_PAPER_EMPTY     0x4
#define PARALLEL_OFF_LINE        0x8
#define PARALLEL_POWER_OFF       0x10
#define PARALLEL_NOT_CONNECTED   0x20
#define PARALLEL_BUSY            0x40
#define PARALLEL_SELECTED        0x80

/*

  Object states:
    1. Open, not initialized
    2. Open, initialized
    3. Closed, not initialized
    4. Closed, initialized

  Closing the port:
    1. If already closed, do nothing
    2. If initialized, restore settings to original values
    3. Close port

  Error handling:
    Check return values on all OS calls. If an error is indicated
    for an I/O operation, then throw an IOException - Java code is responsible
    for closing the port (the abort function will be called).
    If an error is indicated when changing settings, throw an
    IllegalArgumentException. This will alow an application to try a
    different setting.
    The basic idea is to have two classes of exceptions:
      1. A serious error has occurred and use of this port should be discontinued.
      2. An attempt to change a setting failed, try again with another setting.

  Object data members:
    Object data members contain RXTX-defined values. They are translated to Win 32
    values in the functions that use them.
        
*/

// ------------------------------------------------------------------------- //
// Class ParallelPortImpl
// ------------------------------------------------------------------------- //

ParallelPortImpl::ParallelPortImpl(const char *portName, HANDLE hComm) :
    hComm(hComm),
    closed(false),
    initialized(false),
    inputBufferSize(RXTX_INITIAL_BUFFER_SIZE),
    outputBufferSize(RXTX_INITIAL_BUFFER_SIZE),
    mode(RXTX_PARALLEL_MODE_ANY),
    portType(RXTX_PORT_PARALLEL)
{
    int length = strlen(portName) + 1;
    this->portName = new char[length];
    strcpy(this->portName, portName);
    this->portName[length] = 0;
}

ParallelPortImpl::~ParallelPortImpl()
{
    if (!closed)
    {
        try
        {
            if (initialized)
                restoreSettings();
            CloseHandle(hComm);
        }
        catch (...) {}
    }
    delete portName;
}

void ParallelPortImpl::close()
{
    if (!closed)
    {
        bool caught = false;
        try
        {
            if (initialized)
                restoreSettings();
            CloseHandle(hComm);
        }
        catch (...)
        {
            caught = true;
        }
        hComm = NULL;
        closed = true;
        if (caught)
            throw IOException();
    }
}

const int ParallelPortImpl::getInputBufferSize() const
{
    checkStatus();
    return inputBufferSize;
}

const int ParallelPortImpl::getOutputBufferSize() const
{
    checkStatus();
    return outputBufferSize;
}

const char* ParallelPortImpl::getName() const
{
    checkStatus();
    return portName;
}

const int ParallelPortImpl::getType() const
{
    checkStatus();
    return portType;
}

const bool ParallelPortImpl::isPortReadable() const
{
    // Needs implementation
    return true;
}

const bool ParallelPortImpl::isPortWritable() const
{
    // Needs implementation
    return true;
}

const bool ParallelPortImpl::isOutputBufferEmpty() const
{
    // Needs implementation
    return false;
}

void ParallelPortImpl::setInputBufferSize(int size)
{
    checkStatus();
    inputBufferSize = size;
}

void ParallelPortImpl::setOutputBufferSize(int size)
{
    checkStatus();
    outputBufferSize = size;
}

// ------------------------------------------------------------------------- //

const int ParallelPortImpl::getMode() const
{
    checkStatus();
    return 0;
}

const int ParallelPortImpl::getOutputBufferFree() const
{
    checkStatus();
    return 0;
}

const bool ParallelPortImpl::isPaperOut() const
{
    checkStatus();
    return getPortFlags() & PARALLEL_PAPER_EMPTY != 0;
}

const bool ParallelPortImpl::isPrinterBusy() const
{
    checkStatus();
    return getPortFlags() & PARALLEL_BUSY != 0;
}

const bool ParallelPortImpl::isPrinterError() const
{
    checkStatus();
    return getPortFlags() & (PARALLEL_PAPER_EMPTY |
        PARALLEL_OFF_LINE |
        PARALLEL_POWER_OFF |
        PARALLEL_NOT_CONNECTED |
        PARALLEL_BUSY) != 0;
}

const bool ParallelPortImpl::isPrinterSelected() const
{
    checkStatus();
    return getPortFlags() & PARALLEL_SELECTED != 0;
}

const bool ParallelPortImpl::isPrinterTimedOut() const
{
    checkStatus();
    return false;
}

void ParallelPortImpl::restart()
{
    checkStatus();
    // Not implemented
}

int ParallelPortImpl::setMode(int mode)
{
    checkStatus();
    return 0;
}

void ParallelPortImpl::suspend()
{
    checkStatus();
    // Not implemented
}

// ------------------------------------------------------------------------- //

void ParallelPortImpl::abort()
{
    if (!closed)
    {
        try
        {
            if (initialized)
                restoreSettings();
            CloseHandle(hComm);
        }
        catch (...) {}
        hComm = NULL;
        closed = true;
    }
}

inline void ParallelPortImpl::checkStatus() const
{
    if (closed)
        throw IllegalStateException();
}

DWORD ParallelPortImpl::getPortFlags() const
{
    DWORD outBuffer;
    DWORD bytesReturned;
    if (!DeviceIoControl(hComm, IOCTL_PAR_QUERY_INFORMATION, NULL, 0,
        &outBuffer, sizeof outBuffer, &bytesReturned, NULL))
        throw IOException();
    return outBuffer;
}

void ParallelPortImpl::initializePort()
{
    // Not implemented
    initialized = true;
}

Buffer* ParallelPortImpl::readBytes(int length)
{
    checkStatus();
    char *charArray = new char[length];
    DWORD bytesRead;
    if (!ReadFile(hComm, charArray, length, &bytesRead, NULL))
        throw IOException();
    return new Buffer(charArray, bytesRead);
}

void ParallelPortImpl::restoreSettings()
{
    // Not implemented
}

const int ParallelPortImpl::writeBytes(Buffer *buffer)
{
    checkStatus();
    DWORD bytesWritten;
    if (!WriteFile(hComm, buffer->data, buffer->size, &bytesWritten, NULL))
    {
        delete buffer;
        throw IOException();
    }
    delete buffer;
    return bytesWritten;
}

