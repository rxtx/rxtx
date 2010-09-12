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
#include <RXTX_Exceptions.h>
#include <RXTX_Classes.h>
#include <time.h>

#ifndef _THIS_FILE_
    #define _THIS_FILE_ "RXTX_Classes.cpp"
#endif

// ------------------------------------------------------------------------- //
// Class PortInfo
// ------------------------------------------------------------------------- //

PortInfo::PortInfo(const char* portName, int portType)
{
    int length = strlen(portName) + 1;
    name = new char[length];
    strcpy(name, portName);
    name[length] = 0;
    type = portType;
}

PortInfo::~PortInfo()
{
    delete name;
}

const char* PortInfo::getName() const
{
    return name;
}

const int PortInfo::getType() const
{
    return type;
}

jclass PortInfo::getJavaClass(JNIEnv *env)
{
    jclass portInfoClass = env->FindClass("gnu/io/PortInfo");
    if (portInfoClass == NULL)
        throw JavaRuntimeException();
    return portInfoClass;
}

jmethodID PortInfo::getJavaConstructor(JNIEnv *env)
{
    jmethodID portInfoConstructor = env->GetMethodID(getJavaClass(env), "<init>", "(Ljava/lang/String;I)V");
    if (portInfoConstructor == NULL)
        throw JavaRuntimeException();
    return portInfoConstructor;
}

const int PortInfo::getType(const char* portName)
{
    PortInfoList portInfoList;
    portInfoList.getValidPorts();
    const std::list<PortInfo*> *portInfos = portInfoList.getList();
    for (std::list<PortInfo*>::const_iterator i = portInfos->begin(); i != portInfos->end(); i++)
    {
        PortInfo *portInfo = *i;
        if (strcmp(portName, portInfo->getName()) == 0)
            return portInfo->getType();
    }
    throw NoSuchPortException();
}

// ------------------------------------------------------------------------- //
// Class PortInfoList
// ------------------------------------------------------------------------- //

PortInfoList::~PortInfoList()
{
    while (!portInfos.empty())
    {
        PortInfo* portInfo = *portInfos.begin();
        portInfos.pop_front();
        delete portInfo;
    }
}

void PortInfoList::addPortInfo(const char* portName, int portType)
{
    portInfos.push_back(new PortInfo(portName, portType));
}

const int PortInfoList::size() const
{
    return portInfos.size();
}

const std::list<PortInfo*>* PortInfoList::getList()
{
    return &portInfos;
}

// ------------------------------------------------------------------------- //
// Class PortManager
// ------------------------------------------------------------------------- //

PortEntry::~PortEntry()
{
    delete port;
}

PortManager::~PortManager()
{
    while (!ports.empty())
    {
        PortEntry* entry = *ports.begin();
        ports.pop_front();
        delete entry;
    }
}

const int PortManager::openPort(const char* portName, int portType)
{
    for (std::list<PortEntry*>::iterator i = ports.begin(); i != ports.end(); i++)
    {
        PortEntry *entry = *i;
        if (strcmp(entry->port->getName(), portName) == 0 && entry->port->getType() == portType)
        {
            closePort(entry->portHandle);
            break;
        }
    }
    // Enforce opaque port handle by making it a random number
    srand(time(NULL));
    int portHandle = rand();
    CommPort* port = getPort(portHandle);
    while (port != NULL)
    {
        portHandle = rand();
        port = getPort(portHandle);
    }
    port = CommPortFactory::getInstance(portName, portType);
    ports.push_back(new PortEntry(portHandle, port));
    return portHandle;
}

CommPort* PortManager::getPort(int portHandle) const
{
    // TODO: Not thread-safe. List contents can change
    // during iteration.
    for (std::list<PortEntry*>::const_iterator i = ports.begin(); i != ports.end(); i++)
    {
        PortEntry *entry = *i;
        if (entry->portHandle == portHandle)
            return entry->port;
    }
    return NULL;
}

void PortManager::closePort(int portHandle)
{
    for (std::list<PortEntry*>::iterator i = ports.begin(); i != ports.end(); i++)
    {
        PortEntry *entry = *i;
        if (entry->portHandle == portHandle)
        {
            exception* caught = NULL;
            try
            {
                entry->port->close();
            }
            catch (exception &e)
            {
                caught = &e;
                entry->port->abort();
            }
            ports.erase(i);
            delete entry;
            if (caught != NULL)
                throw *caught;
            break;
        }
    }
}

PortManager& PortManager::getInstance()
{
    static PortManager theInstance;
    return theInstance;
}
