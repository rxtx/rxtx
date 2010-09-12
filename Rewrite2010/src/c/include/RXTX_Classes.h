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

#ifndef _RXTX_CLASSES_
#define _RXTX_CLASSES_
#include <list>
#include <map>
#include <string>
#include <JNI_Support.h>

// ------------------------------------------------------------------------- //
// Class Buffer
// ------------------------------------------------------------------------- //

class Buffer
{
public:
    Buffer(char *data, int size) : data(data), size(size) {}
    ~Buffer() {delete data;}
    char *data;
    int size;
};

// ------------------------------------------------------------------------- //
// Class PortInfo
// ------------------------------------------------------------------------- //

class PortInfo
{
public:
    PortInfo(const char* portName, int portType);
    virtual ~PortInfo();

    // Returns the port name
    const char* getName() const;

    // Returns the port type
    const int getType() const;

    // Returns the gnu.io.PortInfo Java class
    static jclass getJavaClass(JNIEnv *env);

    // Returns the gnu.io.PortInfo Java class constructor
    static jmethodID getJavaConstructor(JNIEnv *env);

    // Returns the port type for the supplied port name
    static const int getType(const char *portName);
private:
    char *name;
    int type;
};

// ------------------------------------------------------------------------- //
// Class CommPort
// ------------------------------------------------------------------------- //

class CommPort
{
public:
    virtual ~CommPort() {}

    // Closes the port, absorbing all errors and exceptions
    virtual void abort() = 0;

    // Closes the port
    virtual void close() = 0;

    // Returns the current input buffer size
    virtual const int getInputBufferSize() const = 0;

    // Returns the port name
    virtual const char * getName() const = 0;

    // Returns the current output buffer size
    virtual const int getOutputBufferSize() const = 0;

    // Returns the port type
    virtual const int getType() const = 0;

    // Returns true if the port can be read
    virtual const bool isPortReadable() const = 0;

    // Returns true if the port can be written
    virtual const bool isPortWritable() const = 0;

    // Returns bytes read up to the specified length
    virtual Buffer* readBytes(int length) = 0;

    // Sets the current input buffer size
    virtual void setInputBufferSize(int size) = 0;

    // Sets the current output buffer size
    virtual void setOutputBufferSize(int size) = 0;

    // Writes bytes up to the specified length, returns number of bytes written
    virtual const int writeBytes(Buffer*) = 0;
};

// ------------------------------------------------------------------------- //
// Class CommPortFactory
// ------------------------------------------------------------------------- //

class CommPortFactory
{
public:
    // Returns a CommPort instance, or throws NoSuchPortException
    static CommPort* getInstance(const char *portName, int portType);
private:
    CommPortFactory() {}
    CommPortFactory(CommPortFactory const&);
    CommPortFactory& operator = (CommPortFactory const&);
};

// ------------------------------------------------------------------------- //
// Class ParallelPort
// ------------------------------------------------------------------------- //

class ParallelPort : public CommPort
{
public:
    virtual ~ParallelPort() {}

    // Returns the port type
    virtual const int getMode() const = 0;

    // Returns the number of bytes available in the output buffer
    virtual const int getOutputBufferFree() const = 0;

    // Returns true if the output buffer is empty
    virtual const bool isOutputBufferEmpty() const = 0;

    // Returns true if the paper out signal is raised
    virtual const bool isPaperOut() const = 0;

    // Returns true if the printer busy signal is raised
    virtual const bool isPrinterBusy() const = 0;

    // Returns true if there is a printer error
    virtual const bool isPrinterError() const = 0;

    // Returns true if the printer selected signal is raised
    virtual const bool isPrinterSelected() const = 0;

    // Returns true if the printer timed out
    virtual const bool isPrinterTimedOut() const = 0;

    // Restarts the printer
    virtual void restart() = 0;

    // Sets the port mode
    virtual int setMode(int mode) = 0;

    // Suspends the printer
    virtual void suspend() = 0;
};

// ------------------------------------------------------------------------- //
// Class PortInfoList
// ------------------------------------------------------------------------- //

class PortInfoList
{
public:
    PortInfoList() {}
    ~PortInfoList();

    // Adds a PortInfo to the list
    void addPortInfo(const char *portName, int portType);

    // Adds valid PortInfos to the list
    void getValidPorts();

    // Returns the PortInfo list size
    const int size() const;

    // TODO: Create iterator
    const std::list<PortInfo*> * getList();
private:
    std::list<PortInfo*> portInfos;
};

// ------------------------------------------------------------------------- //
// Class PortManager
// ------------------------------------------------------------------------- //

class PortEntry {
public:
    PortEntry(int portHandle, CommPort *port) : portHandle(portHandle), port(port) {}
    ~PortEntry();
    int portHandle;
    CommPort* port;
};

class PortManager
{
public:
    ~PortManager();
    static PortManager& getInstance();
    const int openPort(const char *portName, int portType);
    CommPort* getPort(int portHandle) const;
    void closePort(int portHandle);
private:
    PortManager() {}
    PortManager(PortManager const&);
    PortManager& operator = (PortManager const&);
    std::list<PortEntry*> ports;
};

// ------------------------------------------------------------------------- //
// Class SerialPort
// ------------------------------------------------------------------------- //

class SerialPort : public CommPort
{
public:
    virtual ~SerialPort() {}

    // Returns the baud rate
    virtual const int getBaudRate() const = 0;

    // Returns the data bits
    virtual const int getDataBits() const = 0;

    // Returns the flow control mode
    virtual const int getFlowControlMode() const = 0;

    // Returns the parity
    virtual const int getParity() const = 0;

    // Returns the stop bits
    virtual const int getStopBits() const = 0;

    // Returns true if the break interrupt signal is raised
    virtual const bool isBI() const = 0;

    // Returns true if the carrier detect signal is raised
    virtual const bool isCD() const = 0;

    // Returns true if the clear to send signal is raised
    virtual const bool isCTS() const = 0;

    // Returns true if data is available for reading
    virtual const bool isDataAvailable() const = 0;

    // Returns true if the data set ready signal is raised
    virtual const bool isDSR() const = 0;

    // Returns true if the data terminal ready signal is raised
    virtual const bool isDTR() const = 0;

    // Returns true if there was a bit-level framing error
    virtual const bool isFramingError() const = 0;

    // Returns true if there was an overrun error
    virtual const bool isOverrunError() const = 0;

    // Returns true if there was a parity error
    virtual const bool isParityError() const = 0;

    // Returns true if the ring indicator signal is raised
    virtual const bool isRI() const = 0;

    // Returns true if the request to send signal is raised
    virtual const bool isRTS() const = 0;

    // Sends a break
    virtual void sendBreak(int duration) = 0;

    // Sets the data terminal ready signal to the specified state
    virtual void setDTR(bool state) = 0;

    // Sets the flow control mode to the specified mode
    virtual void setFlowControlMode(int flowControlMode) = 0;

    // Sets the request to send signal to the specified state
    virtual void setRTS(bool state) = 0;

    // Sets the serial port to the specified settings
    virtual void setSerialPortParams(int baudRate, int dataBits, int stopBits, int parity) = 0;
};

#endif
