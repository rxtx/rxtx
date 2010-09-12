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

#ifndef _THIS_FILE_
#define _THIS_FILE_ "W32_SerialPort.cpp"
#endif

// http://msdn.microsoft.com/en-us/library/ff802693.aspx
// http://msdn.microsoft.com/en-us/library/aa363140%28v=VS.85%29.aspx

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
// Class SerialPortImpl
// ------------------------------------------------------------------------- //

SerialPortImpl::SerialPortImpl(const char *portName, HANDLE hComm) :
    hComm(hComm),
    originalDCB(NULL),
    originalTimeouts(NULL),
    commProperties(NULL),
    closed(false),
    initialized(false),
    inputBufferSize(RXTX_INITIAL_BUFFER_SIZE),
    outputBufferSize(RXTX_INITIAL_BUFFER_SIZE),
    baudRate(9600),
    dataBits(RXTX_SERIAL_DATABITS_8),
    flowControlMode(RXTX_SERIAL_FLOWCONTROL_NONE),
    parity(RXTX_SERIAL_PARITY_NONE),
    stopBits(RXTX_SERIAL_STOPBITS_1),
    portType(RXTX_PORT_SERIAL)
{
    int length = strlen(portName) + 1;
    this->portName = new char[length];
    strcpy(this->portName, portName);
    this->portName[length] = 0;
}

SerialPortImpl::~SerialPortImpl()
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
    if (originalDCB != NULL)
        delete originalDCB;
    if (originalTimeouts != NULL)
        delete originalTimeouts;
    if (commProperties != NULL)
        delete commProperties;
}

void SerialPortImpl::close()
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

const int SerialPortImpl::getInputBufferSize() const
{
    checkStatus();
    return inputBufferSize;
}

const int SerialPortImpl::getOutputBufferSize() const
{
    checkStatus();
    return outputBufferSize;
}

const char* SerialPortImpl::getName() const
{
    checkStatus();
    return portName;
}

const int SerialPortImpl::getType() const
{
    checkStatus();
    return portType;
}

const bool SerialPortImpl::isDataAvailable() const
{
    // Needs implementation
    return true;
}

const bool SerialPortImpl::isPortReadable() const
{
    // Needs implementation
    return true;
}

const bool SerialPortImpl::isPortWritable() const
{
    // Needs implementation
    return true;
}

void SerialPortImpl::setInputBufferSize(int size)
{
    checkStatus();
    if (size % 2 != 0)
        size--;  // W32 requires even size
    if (size > commProperties->dwMaxRxQueue && commProperties->dwMaxRxQueue > 0)
        size = commProperties->dwMaxRxQueue;
    if( !SetupComm(hComm, size, outputBufferSize))
        throw IllegalArgumentException();
    inputBufferSize = size;
}

void SerialPortImpl::setOutputBufferSize(int size)
{
    checkStatus();
    if (size % 2 != 0)
        size--;  // W32 requires even size
    if (size > commProperties->dwMaxTxQueue && commProperties->dwMaxTxQueue > 0)
        size = commProperties->dwMaxTxQueue;
    if( !SetupComm(hComm, inputBufferSize, size))
        throw IllegalArgumentException();
    outputBufferSize = size;
}

// ------------------------------------------------------------------------- //

const int SerialPortImpl::getBaudRate() const
{
    checkStatus();
    return baudRate;
}

const int SerialPortImpl::getDataBits() const
{
    checkStatus();
    return dataBits;
}

const int SerialPortImpl::getFlowControlMode() const
{
    checkStatus();
    return flowControlMode;
}

const int SerialPortImpl::getParity() const
{
    checkStatus();
    return parity;
}

const int SerialPortImpl::getStopBits() const
{
    checkStatus();
    return stopBits;
}

const bool SerialPortImpl::isCD() const
{
    checkStatus();
    DWORD modemStatus;
    if(!GetCommModemStatus(hComm, &modemStatus))
        throw IOException();
    return modemStatus & MS_RLSD_ON != 0;
}

const bool SerialPortImpl::isBI() const
{
    // TODO: Needs implementation
    return false;
}

const bool SerialPortImpl::isFramingError() const
{
    // TODO: Needs implementation
    return false;
}

const bool SerialPortImpl::isOverrunError() const
{
    // TODO: Needs implementation
    return false;
}

const bool SerialPortImpl::isParityError() const
{
    // TODO: Needs implementation
    return false;
}

const bool SerialPortImpl::isCTS() const
{
    checkStatus();
    DWORD modemStatus;
    if(!GetCommModemStatus(hComm, &modemStatus))
        throw IOException();
    return modemStatus & MS_CTS_ON != 0;
}

const bool SerialPortImpl::isDSR() const
{
    checkStatus();
    DWORD modemStatus;
    if(!GetCommModemStatus(hComm, &modemStatus))
        throw IOException();
    return modemStatus & MS_DSR_ON != 0;
}

const bool SerialPortImpl::isDTR() const
{
    checkStatus();
    // Not sure about this
    DCB dcb = {0};
    if (!GetCommState(hComm, &dcb))
        throw IOException();
    return dcb.fDtrControl == TRUE;
}

const bool SerialPortImpl::isRI() const
{
    checkStatus();
    DWORD modemStatus;
    if(!GetCommModemStatus(hComm, &modemStatus))
        throw IOException();
    return modemStatus & MS_RING_ON != 0;
}

const bool SerialPortImpl::isRTS() const
{
    checkStatus();
    // Not sure about this
    DCB dcb = {0};
    if (!GetCommState(hComm, &dcb))
        throw IOException();
    return dcb.fRtsControl == TRUE;
}

void SerialPortImpl::sendBreak(int duration)
{
    checkStatus();
    // This is a two-step process that must be partially
    // implemented in Java: Send break start, wait duration,
    // send break stop.
}

void SerialPortImpl::setDTR(bool state)
{
    checkStatus();
    if (flowControlMode == RXTX_SERIAL_FLOWCONTROL_NONE)
        throw IllegalStateException();
    if (!EscapeCommFunction(hComm, state ? SETDTR : CLRDTR))
        throw IOException();
}

void SerialPortImpl::setFlowControlMode(int flowControl)
{
    checkStatus();
    DCB dcb = {0};
    if (!GetCommState(hComm, &dcb))
        throw IOException();
    resetDcbFlowControl(&dcb);
    if (flowControl == RXTX_SERIAL_FLOWCONTROL_NONE)
    {
        if (!SetCommState(hComm, &dcb))
            throw IOException();
        flowControlMode = flowControl;
        return;
    }
    if (flowControl & RXTX_SERIAL_FLOWCONTROL_RTSCTS_IN != 0 ||
        flowControl & RXTX_SERIAL_FLOWCONTROL_RTSCTS_OUT != 0)
    {
        // W32 does not have separate CTS/RTS in/out settings
        dcb.fOutxCtsFlow = TRUE;
        dcb.fRtsControl = RTS_CONTROL_HANDSHAKE;
        if (!SetCommState(hComm, &dcb))
            throw IOException();
        flowControlMode = RXTX_SERIAL_FLOWCONTROL_RTSCTS_IN & RXTX_SERIAL_FLOWCONTROL_RTSCTS_OUT;
        return;
    }
    if (flowControl & RXTX_SERIAL_FLOWCONTROL_XONXOFF_IN != 0 ||
        flowControl & RXTX_SERIAL_FLOWCONTROL_XONXOFF_OUT != 0)
    {
        if (flowControl & RXTX_SERIAL_FLOWCONTROL_XONXOFF_IN != 0)
            dcb.fInX = TRUE;
        if (flowControl & RXTX_SERIAL_FLOWCONTROL_XONXOFF_OUT != 0)
            dcb.fOutX = TRUE;
        if (!SetCommState(hComm, &dcb))
            throw IOException();
        flowControlMode = flowControl;
        return;
    }
    throw IllegalArgumentException();
}

void SerialPortImpl::setRTS(bool state)
{
    checkStatus();
    if (flowControlMode == RXTX_SERIAL_FLOWCONTROL_NONE)
        throw IllegalStateException();
    if (!EscapeCommFunction(hComm, state ? SETRTS : CLRRTS))
        throw IOException();
}

void SerialPortImpl::setSerialPortParams(int baudRate, int dataBits, int stopBits, int parity)
{
    checkStatus();
    if (baudRate > 256000 && commProperties->dwMaxBaud != BAUD_USER)
        throw UnsupportedCommOperationException();
    DCB dcb = {0};
    if (!GetCommState(hComm, &dcb))
        throw IOException();
    dcb.BaudRate = xlateBaudRate(baudRate);
    dcb.ByteSize = dataBits;
    dcb.Parity = xlateParity(parity);
    if (parity == RXTX_SERIAL_PARITY_NONE)
        dcb.fParity = FALSE;
    else
        dcb.fParity = TRUE;
    dcb.StopBits = xlateStopBits(stopBits);
    if (!SetCommState(hComm, &dcb))
        throw IOException();
    this->baudRate = baudRate;
    this->dataBits = dataBits;
    this->stopBits = stopBits;
    this->parity = parity;
}

// ------------------------------------------------------------------------- //

void SerialPortImpl::abort()
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

inline void SerialPortImpl::checkStatus() const
{
    if (closed)
        throw IllegalStateException();
}

void SerialPortImpl::initializePort()
{
    DCB dcb = {0};
    if (!GetCommState(hComm, &dcb))
        throw IOException();
    originalDCB = new DCB;
    memcpy(originalDCB, &dcb, sizeof DCB);
    dcb.BaudRate = xlateBaudRate(baudRate);
    dcb.ByteSize = dataBits;
    dcb.Parity = xlateParity(parity);
    dcb.StopBits = xlateStopBits(stopBits);
    if (!SetCommState(hComm, &dcb))
        throw IOException();
    COMMTIMEOUTS timeouts = {0};
    if (!GetCommTimeouts(hComm, &timeouts))
        throw IOException();
    originalTimeouts = new COMMTIMEOUTS;
    memcpy(originalTimeouts, &timeouts, sizeof COMMTIMEOUTS);
    timeouts.ReadIntervalTimeout = MAXDWORD; 
    timeouts.ReadTotalTimeoutMultiplier = 0;
    timeouts.ReadTotalTimeoutConstant = 0;
    timeouts.WriteTotalTimeoutMultiplier = 0;
    timeouts.WriteTotalTimeoutConstant = 0;
    if (!SetCommTimeouts(hComm, &timeouts))
        throw IOException();
    COMMPROP commProps = {0};
    if (!GetCommProperties(hComm, &commProps))
        throw IOException();
    commProperties = new COMMPROP;
    memcpy(commProperties, &commProps, sizeof COMMPROP);
    if( !SetupComm(hComm, inputBufferSize, outputBufferSize))
        throw IOException();
    initialized = true;
}

Buffer* SerialPortImpl::readBytes(int length)
{
    checkStatus();
    char *charArray = new char[length];
    DWORD bytesRead;
    if (!ReadFile(hComm, charArray, length, &bytesRead, NULL))
        throw IOException();
    return new Buffer(charArray, bytesRead);
}

void SerialPortImpl::resetDcbFlowControl(LPDCB dcb)
{
    dcb->fOutxCtsFlow = FALSE;
    dcb->fOutxDsrFlow = FALSE;
    dcb->fDtrControl = DTR_CONTROL_DISABLE;
    dcb->fOutX = FALSE;
    dcb->fInX = FALSE;
    dcb->fRtsControl = RTS_CONTROL_DISABLE;
}

void SerialPortImpl::restoreSettings()
{
    // This is cleanup code - do not throw exceptions
    PurgeComm(hComm, PURGE_RXCLEAR | PURGE_TXCLEAR);
    if (originalDCB != NULL)
        SetCommState(hComm, originalDCB);
    if (originalTimeouts != NULL)
        SetCommTimeouts(hComm, originalTimeouts);
    if (commProperties != NULL)
        SetupComm(hComm, commProperties->dwCurrentRxQueue, commProperties->dwCurrentTxQueue);
}

const int SerialPortImpl::writeBytes(Buffer *buffer)
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

DWORD SerialPortImpl::xlateBaudRate(int baudRate)
{
    if (baudRate <= 110)
        return CBR_110;
    if (baudRate <= 300)
        return CBR_300;
    if (baudRate <= 600)
        return CBR_600;
    if (baudRate <= 1200)
        return CBR_1200;
    if (baudRate <= 2400)
        return CBR_2400;
    if (baudRate <= 4800)
        return CBR_4800;
    if (baudRate <= 9600)
        return CBR_9600;
    if (baudRate <= 14400)
        return CBR_14400;
    if (baudRate <= 19200)
        return CBR_19200;
    if (baudRate <= 38400)
        return CBR_38400;
    if (baudRate <= 56000)
        return CBR_56000;
    if (baudRate <= 57600)
        return CBR_57600;
    if (baudRate <= 115200)
        return CBR_115200;
    if (baudRate <= 128000)
        return CBR_128000;
    if (baudRate <= 256000)
        return CBR_256000;
    return baudRate;
}

BYTE SerialPortImpl::xlateParity(int parity)
{
    switch (parity)
    {
    case RXTX_SERIAL_PARITY_NONE :
        return NOPARITY;
    case RXTX_SERIAL_PARITY_ODD:
        return ODDPARITY;
    case RXTX_SERIAL_PARITY_EVEN:
        return EVENPARITY;
    case RXTX_SERIAL_PARITY_MARK :
        return MARKPARITY;
    case RXTX_SERIAL_PARITY_SPACE:
        return SPACEPARITY;
    default:
        throw IllegalArgumentException();
    }
}

BYTE SerialPortImpl::xlateStopBits(int stopBits)
{
    switch (stopBits)
    {
    case RXTX_SERIAL_STOPBITS_1:
        return ONESTOPBIT;
    case RXTX_SERIAL_STOPBITS_1_5:
        return ONE5STOPBITS;
    case RXTX_SERIAL_STOPBITS_2:
        return TWOSTOPBITS;
    default:
        throw IllegalArgumentException();
    }
}
