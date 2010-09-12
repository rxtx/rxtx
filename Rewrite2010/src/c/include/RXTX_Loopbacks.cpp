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

/*-------------------------------------------------------------------------
This file is intended to contain platform-independent code. It should not
contain preprocessor directives for platform-specific code. Preprocessor
directives that accommodate compiler differences are allowed.
--------------------------------------------------------------------------*/

#include <RXTX_Globals.h>
#ifdef RXTX_INCLUDE_SOFTWARE_LOOPBACKS
#include <RXTX_Exceptions.h>
#include <RXTX_Classes.h>
#include <RXTX_Loopbacks.h>

#ifndef _THIS_FILE_
#define _THIS_FILE_ "RXTX_Loopbacks.cpp"
#endif

// ------------------------------------------------------------------------- //
// Class ParallelLoopback
// ------------------------------------------------------------------------- //

ParallelLoopback::ParallelLoopback(const char *portName) :
    bytesSaved(0),
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

ParallelLoopback::~ParallelLoopback()
{
    delete portName;
}

const char* ParallelLoopback::getName() const
{
    return portName;
}

const int ParallelLoopback::getType() const
{
    return portType;
}

void ParallelLoopback::close() {}

const int ParallelLoopback::getInputBufferSize() const
{
    return inputBufferSize;
}

const int ParallelLoopback::getOutputBufferSize() const
{
    return outputBufferSize;
}

const bool ParallelLoopback::isPortReadable() const
{
    return true;
}

const bool ParallelLoopback::isPortWritable() const
{
    return true;
}

const bool ParallelLoopback::isOutputBufferEmpty() const
{
    return false;
}

void ParallelLoopback::setInputBufferSize(int size)
{
    inputBufferSize = size;
}

void ParallelLoopback::setOutputBufferSize(int size)
{
    outputBufferSize = size;
}

void ParallelLoopback::abort() {}

Buffer* ParallelLoopback::readBytes(int length)
{
    if (bytesSaved == 0)
    {
        return NULL;
    }
    int resultLen = bytesSaved < length ? bytesSaved : length;
    char *result = new char[resultLen];
    int index = 0;
    while (index < resultLen - 1)
    {
        int bytesRemaining = length - index;
        Buffer *buffer = *buffers.begin();
        int copyCount = buffer->size < bytesRemaining ? buffer->size : bytesRemaining;
        memcpy(result + index, buffer->data, copyCount);
        index += copyCount;
        if (copyCount < buffer->size)
        {
            int newSize = buffer->size - copyCount;
            char *newData = new char[newSize];
            memcpy(newData, buffer->data + copyCount, newSize);
            delete buffer->data;
            buffer->data = newData;
            buffer->size = newSize;
        }
        else
        {
            buffers.pop_front();
            delete buffer;
        }
    }
    bytesSaved -= resultLen;
    return new Buffer(result, resultLen);
}

const int ParallelLoopback::writeBytes(Buffer *buffer)
{
    if (bytesSaved + buffer->size > outputBufferSize)
    {
        delete buffer;
        return 0;
    }
    buffers.push_back(buffer);
    bytesSaved += buffer->size;
    return buffer->size;
}

// ------------------------------------------------------------------------- //

const int ParallelLoopback::getMode() const
{
    return mode;
}

const int ParallelLoopback::getOutputBufferFree() const
{
    return outputBufferSize - bytesSaved;
}

const bool ParallelLoopback::isPaperOut() const
{
    return false;
}

const bool ParallelLoopback::isPrinterBusy() const
{
    return false;
}

const bool ParallelLoopback::isPrinterError() const
{
    return false;
}

const bool ParallelLoopback::isPrinterSelected() const
{
    return true;
}

const bool ParallelLoopback::isPrinterTimedOut() const
{
    return false;
}

void ParallelLoopback::restart()
{
}

int ParallelLoopback::setMode(int mode)
{
    this->mode = mode;
    return mode;
}

void ParallelLoopback::suspend()
{
}

// ------------------------------------------------------------------------- //
// Class SerialLoopback
// ------------------------------------------------------------------------- //

SerialLoopback::SerialLoopback(const char *portName) :
    bytesSaved(0),
    inputBufferSize(RXTX_INITIAL_BUFFER_SIZE),
    outputBufferSize(RXTX_INITIAL_BUFFER_SIZE),
    baudRate(9600),
    dataBits(RXTX_SERIAL_DATABITS_8),
    flowControlMode(RXTX_SERIAL_FLOWCONTROL_NONE),
    parity(RXTX_SERIAL_PARITY_NONE),
    stopBits(RXTX_SERIAL_STOPBITS_1),
    cd(false),
    cts(false),
    dsr(false),
    dtr(false),
    ri(false),
    rts(false),
    portType(RXTX_PORT_SERIAL)
{
    int length = strlen(portName) + 1;
    this->portName = new char[length];
    strcpy(this->portName, portName);
    this->portName[length] = 0;
}

SerialLoopback::~SerialLoopback()
{
    delete portName;
}

void SerialLoopback::close() {}

const char* SerialLoopback::getName() const
{
    return portName;
}

const int SerialLoopback::getType() const
{
    return portType;
}

const int SerialLoopback::getInputBufferSize() const
{
    return inputBufferSize;
}

const int SerialLoopback::getOutputBufferSize() const
{
    return outputBufferSize;
}

const bool SerialLoopback::isDataAvailable() const
{
    return true;
}

const bool SerialLoopback::isPortReadable() const
{
    return true;
}

const bool SerialLoopback::isPortWritable() const
{
    return true;
}

void SerialLoopback::setInputBufferSize(int size)
{
    inputBufferSize = size;
}

void SerialLoopback::setOutputBufferSize(int size)
{
    outputBufferSize = size;
}

void SerialLoopback::abort() {}

Buffer* SerialLoopback::readBytes(int length)
{
    if (bytesSaved == 0)
    {
        return NULL;
    }
    int resultLen = bytesSaved < length ? bytesSaved : length;
    char *result = new char[resultLen];
    int index = 0;
    while (index < resultLen - 1)
    {
        int bytesRemaining = length - index;
        Buffer *buffer = *buffers.begin();
        int copyCount = buffer->size < bytesRemaining ? buffer->size : bytesRemaining;
        memcpy(result + index, buffer->data, copyCount);
        index += copyCount;
        if (copyCount < buffer->size)
        {
            int newSize = buffer->size - copyCount;
            char *newData = new char[newSize];
            memcpy(newData, buffer->data + copyCount, newSize);
            delete buffer->data;
            buffer->data = newData;
            buffer->size = newSize;
        }
        else
        {
            buffers.pop_front();
            delete buffer;
        }
    }
    bytesSaved -= resultLen;
    return new Buffer(result, resultLen);
}

const int SerialLoopback::writeBytes(Buffer *buffer)
{
    if (bytesSaved + buffer->size > outputBufferSize)
    {
        delete buffer;
        return 0;
    }
    buffers.push_back(buffer);
    bytesSaved += buffer->size;
    return buffer->size;
}

// ------------------------------------------------------------------------- //

const int SerialLoopback::getBaudRate() const
{
    return baudRate;
}

const int SerialLoopback::getDataBits() const
{
    return dataBits;
}

const int SerialLoopback::getFlowControlMode() const
{
    return flowControlMode;
}

const int SerialLoopback::getParity() const
{
    return parity;
}

const int SerialLoopback::getStopBits() const
{
    return stopBits;
}

const bool SerialLoopback::isBI() const
{
    return false;
}

const bool SerialLoopback::isFramingError() const
{
    return false;
}

const bool SerialLoopback::isOverrunError() const
{
    return false;
}

const bool SerialLoopback::isParityError() const
{
    return false;
}

const bool SerialLoopback::isCD() const
{
    return false;
}

const bool SerialLoopback::isCTS() const
{
    return false;
}

const bool SerialLoopback::isDSR() const
{
    return false;
}

const bool SerialLoopback::isDTR() const
{
    return false;
}

const bool SerialLoopback::isRI() const
{
    return false;
}

const bool SerialLoopback::isRTS() const
{
    return false;
}

void SerialLoopback::sendBreak(int duration)
{
}

void SerialLoopback::setDTR(bool state)
{
    dtr = state;
}

void SerialLoopback::setFlowControlMode(int flowControlMode)
{
    this->flowControlMode = flowControlMode;
}

void SerialLoopback::setRTS(bool state)
{
    rts = state;
}

void SerialLoopback::setSerialPortParams(int baudRate, int dataBits, int stopBits, int parity)
{
    this->baudRate = baudRate;
    this->dataBits = dataBits;
    this->stopBits = stopBits;
    this->parity = parity;
}

#endif
