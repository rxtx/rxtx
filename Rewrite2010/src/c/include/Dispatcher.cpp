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
#include <JNI_Support.h>
#include <gnu_io_Dispatcher.h>

#ifndef _THIS_FILE_
    #define _THIS_FILE_ "Dispatcher.cpp"
#endif

#define BEGIN_NATIVE_FUNCTION \
    try { \

#define BEGIN_NATIVE_FUNC_GET_PORT \
    try { \
        PortManager& portManager = PortManager::getInstance(); \
        CommPort* port = portManager.getPort((int) portHandle); \
        if (port == NULL) \
            throw IllegalArgumentException(); \

#define END_NATIVE_FUNC_HANDLE_EXCEPTIONS \
    } \
    catch (JavaRuntimeException &re) \
    { \
        JavaRuntimeException noWarn = re; \
    } \
    catch (IllegalArgumentException &iae) \
    { \
        jclass exceptionClass = iae.getJavaClass(env); \
        if (exceptionClass != NULL) \
            env->ThrowNew(exceptionClass, "Invalid argument"); \
    } \
    catch (IllegalStateException &ise) \
    { \
        jclass exceptionClass = ise.getJavaClass(env); \
        if (exceptionClass != NULL) \
            env->ThrowNew(exceptionClass, "Illegal state"); \
    } \
    catch (IOException &ioe) \
    { \
        jclass exceptionClass = ioe.getJavaClass(env); \
        if (exceptionClass != NULL) \
            env->ThrowNew(exceptionClass, "I/O error occurred"); \
    } \
    catch (...) \
    { \
    } \

#define HANDLE_NSP_EXCEPTION \
    } \
    catch (NoSuchPortException &nspe) \
    { \
        jclass exceptionClass = nspe.getJavaClass(env); \
        if (exceptionClass != NULL) \
            env->ThrowNew(exceptionClass, "No such port"); \

#define HANDLE_UCO_EXCEPTION \
    } \
    catch (UnsupportedCommOperationException &ucoe) \
    { \
        jclass exceptionClass = ucoe.getJavaClass(env); \
        if (exceptionClass != NULL) \
            env->ThrowNew(exceptionClass, "Unsupported comm operation"); \

#define TEST_AND_CAST_TO_SERIAL_PORT \
        if (port->getType() != RXTX_PORT_SERIAL) \
            throw IllegalArgumentException(); \
        SerialPort* serialPort = (SerialPort*) port; \

#define TEST_AND_CAST_TO_PARALLEL_PORT \
        if (port->getType() != RXTX_PORT_PARALLEL) \
            throw IllegalArgumentException(); \
        ParallelPort* parallelPort = (ParallelPort*) port; \

/*
* Class:     gnu_io_Dispatcher
* Method:    abort
* Signature: (I)V
* Dispatches to: CommPort::abort()
*/
JNIEXPORT void JNICALL Java_gnu_io_Dispatcher_abort(JNIEnv *env, jobject thisObj, jint portHandle)
{
    try {
        PortManager& portManager = PortManager::getInstance();
        portManager.closePort((int) portHandle);
    }
    catch (...)
    {
        // Catch everything and do nothing on abort
    }
}

/*
* Class:     gnu_io_Dispatcher
* Method:    close
* Signature: (I)V
*/
JNIEXPORT void JNICALL Java_gnu_io_Dispatcher_close(JNIEnv *env, jobject thisObj, jint portHandle)
{
    BEGIN_NATIVE_FUNCTION
        PortManager& portManager = PortManager::getInstance();
        portManager.closePort((int) portHandle);
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
}

/*
* Class:     gnu_io_Dispatcher
* Method:    getBaudRate
* Signature: (I)I
* Dispatches to: SerialPort::getBaudRate()
*/
JNIEXPORT jint JNICALL Java_gnu_io_Dispatcher_getBaudRate(JNIEnv *env, jobject thisObj, jint portHandle)
{
    BEGIN_NATIVE_FUNC_GET_PORT
    TEST_AND_CAST_TO_SERIAL_PORT
           return (jint) serialPort->getBaudRate();
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
    return NULL;
}

/*
* Class:     gnu_io_Dispatcher
* Method:    getDataBits
* Signature: (I)I
* Dispatches to: SerialPort::getDataBits()
*/
JNIEXPORT jint JNICALL Java_gnu_io_Dispatcher_getDataBits(JNIEnv *env, jobject thisObj, jint portHandle)
{
    BEGIN_NATIVE_FUNC_GET_PORT
    TEST_AND_CAST_TO_SERIAL_PORT
           return (jint) serialPort->getDataBits();
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
    return NULL;
}

/*
* Class:     gnu_io_Dispatcher
* Method:    getFlowControlMode
* Signature: (I)I
* Dispatches to: SerialPort::getFlowControlMode()
*/
JNIEXPORT jint JNICALL Java_gnu_io_Dispatcher_getFlowControlMode(JNIEnv *env, jobject thisObj, jint portHandle)
{
    BEGIN_NATIVE_FUNC_GET_PORT
    TEST_AND_CAST_TO_SERIAL_PORT
           return (jint) serialPort->getFlowControlMode();
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
    return NULL;
}

/*
* Class:     gnu_io_Dispatcher
* Method:    getInputBufferSize
* Signature: (I)I
* Dispatches to: CommPort::getInputBufferSize()
*/
JNIEXPORT jint JNICALL Java_gnu_io_Dispatcher_getInputBufferSize(JNIEnv *env, jobject thisObj, jint portHandle)
{
    BEGIN_NATIVE_FUNC_GET_PORT
           return (jint) port->getInputBufferSize();
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
    return NULL;
}

/*
* Class:     gnu_io_Dispatcher
* Method:    getMode
* Signature: (I)I
* Dispatches to: ParallelPort::getMode()
*/
JNIEXPORT jint JNICALL Java_gnu_io_Dispatcher_getMode(JNIEnv *env, jobject thisObj, jint portHandle)
{
    BEGIN_NATIVE_FUNC_GET_PORT
    TEST_AND_CAST_TO_PARALLEL_PORT
           return (jint) parallelPort->getMode();
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
    return NULL;
}

/*
* Class:     gnu_io_Dispatcher
* Method:    getOutputBufferFree
* Signature: (I)I
* Dispatches to: ParallelPort::getOutputBufferFree()
*/
JNIEXPORT jint JNICALL Java_gnu_io_Dispatcher_getOutputBufferFree(JNIEnv *env, jobject thisObj, jint portHandle)
{
    BEGIN_NATIVE_FUNC_GET_PORT
    TEST_AND_CAST_TO_PARALLEL_PORT
           return (jint) parallelPort->getOutputBufferFree();
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
    return NULL;
}

/*
* Class:     gnu_io_Dispatcher
* Method:    getOutputBufferSize
* Signature: (I)I
* Dispatches to: CommPort::getOutputBufferSize()
*/
JNIEXPORT jint JNICALL Java_gnu_io_Dispatcher_getOutputBufferSize(JNIEnv *env, jobject thisObj, jint portHandle)
{
    BEGIN_NATIVE_FUNC_GET_PORT
           return (jint) port->getOutputBufferSize();
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
    return NULL;
}

/*
* Class:     gnu_io_Dispatcher
* Method:    getParity
* Signature: (I)I
* Dispatches to: SerialPort::getParity()
*/
JNIEXPORT jint JNICALL Java_gnu_io_Dispatcher_getParity(JNIEnv *env, jobject thisObj, jint portHandle)
{
    BEGIN_NATIVE_FUNC_GET_PORT
    TEST_AND_CAST_TO_SERIAL_PORT
           return (jint) serialPort->getParity();
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
    return NULL;
}

/*
* Class:     gnu_io_Dispatcher
* Method:    getPortType
* Signature: (Ljava/lang/String;)I
*/
JNIEXPORT jint JNICALL Java_gnu_io_Dispatcher_getPortType(JNIEnv *env, jobject thisObj, jstring jPortName)
{
    const char *portName = NULL;
    int portType = NULL;
    BEGIN_NATIVE_FUNCTION
        portName = env->GetStringUTFChars(jPortName, NULL);
        if (portName == NULL)
            throw JavaRuntimeException();
        portType = PortInfo::getType(portName);
    HANDLE_NSP_EXCEPTION
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
    if (portName != NULL)
        env->ReleaseStringUTFChars(jPortName, portName);
    return (jint) portType;
}

/*
* Class:     gnu_io_Dispatcher
* Method:    getStopBits
* Signature: (I)I
* Dispatches to: SerialPort::getStopBits()
*/
JNIEXPORT jint JNICALL Java_gnu_io_Dispatcher_getStopBits(JNIEnv *env, jobject thisObj, jint portHandle)
{
    BEGIN_NATIVE_FUNC_GET_PORT
    TEST_AND_CAST_TO_SERIAL_PORT
           return (jint) serialPort->getStopBits();
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
    return NULL;
}

/*
* Class:     gnu_io_Dispatcher
* Method:    getValidPortNames
* Signature: ()[Lgnu/io/PortInfo;
*/
JNIEXPORT jobject JNICALL Java_gnu_io_Dispatcher_getValidPortInfos(JNIEnv *env, jobject thisObj)
{
    BEGIN_NATIVE_FUNCTION
        // TODO: Clean this up
        jclass listClass = env->FindClass("java/util/ArrayList");
        if (listClass == NULL)
            throw JavaRuntimeException();
        jmethodID listConstructor = env->GetMethodID(listClass, "<init>", "()V");
        if (listConstructor == NULL)
            throw JavaRuntimeException();
        jobject listObject = env->NewObjectV(listClass, listConstructor, NULL);
        if (listObject == NULL)
            throw JavaRuntimeException();
        jmethodID addMethod = env->GetMethodID(listClass, "add", "(Ljava/lang/Object;)Z");
        if (addMethod == NULL)
            throw JavaRuntimeException();
        PortInfoList portInfoList;
        portInfoList.getValidPorts();
#ifdef RXTX_INCLUDE_SOFTWARE_LOOPBACKS
        portInfoList.addPortInfo("PARALLEL_PASS", RXTX_PORT_PARALLEL);
        portInfoList.addPortInfo("SERIAL_PASS", RXTX_PORT_SERIAL);
#endif
        jclass portInfoClass = PortInfo::getJavaClass(env);
        jmethodID portInfoConst = PortInfo::getJavaConstructor(env);
        // TODO: Convert this to an iterator class
        const std::list<PortInfo*> *portInfos = portInfoList.getList();
        for (std::list<PortInfo*>::const_iterator i = portInfos->begin(); i != portInfos->end(); i++)
        {
            PortInfo *portInfo = *i;
            TO_JSTRING(jPortName, portInfo->getName())
            jint jPortType = portInfo->getType();
            jobject jPortInfo = NULL;
            jPortInfo = env->NewObject(portInfoClass, portInfoConst, jPortName, jPortType);
            if (jPortInfo == NULL)
                throw JavaRuntimeException();
            jboolean result = env->CallBooleanMethod(listObject, addMethod, jPortInfo);
            if (result == NULL)
                throw JavaRuntimeException();
        }
        return listObject;
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
    return NULL;
}

/*
* Class:     gnu_io_Dispatcher
* Method:    isBI
* Signature: (I)Z
* Dispatches to: SerialPort::isBI()
*/
JNIEXPORT jboolean JNICALL Java_gnu_io_Dispatcher_isBI(JNIEnv *env, jobject thisObj, jint portHandle)
{
    BEGIN_NATIVE_FUNC_GET_PORT
    TEST_AND_CAST_TO_SERIAL_PORT
    return serialPort->isBI() ? JNI_TRUE : JNI_FALSE;
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
    return NULL;
}

/*
* Class:     gnu_io_Dispatcher
* Method:    isCD
* Signature: (I)Z
* Dispatches to: SerialPort::isCD()
*/
JNIEXPORT jboolean JNICALL Java_gnu_io_Dispatcher_isCD(JNIEnv *env, jobject thisObj, jint portHandle)
{
    BEGIN_NATIVE_FUNC_GET_PORT
    TEST_AND_CAST_TO_SERIAL_PORT
           return serialPort->isCD() ? JNI_TRUE : JNI_FALSE;
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
    return NULL;
}

/*
* Class:     gnu_io_Dispatcher
* Method:    isCTS
* Signature: (I)Z
* Dispatches to: SerialPort::isCTS()
*/
JNIEXPORT jboolean JNICALL Java_gnu_io_Dispatcher_isCTS(JNIEnv *env, jobject thisObj, jint portHandle)
{
    BEGIN_NATIVE_FUNC_GET_PORT
    TEST_AND_CAST_TO_SERIAL_PORT
           return serialPort->isCTS() ? JNI_TRUE : JNI_FALSE;
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
    return NULL;
}

/*
* Class:     gnu_io_Dispatcher
* Method:    isDataAvailable
* Dispatches to: SerialPort::isDataAvailable()
* Signature: (I)Z
*/
JNIEXPORT jboolean JNICALL Java_gnu_io_Dispatcher_isDataAvailable(JNIEnv *env, jobject thisObj, jint portHandle)
{
    BEGIN_NATIVE_FUNC_GET_PORT
    TEST_AND_CAST_TO_SERIAL_PORT
           return serialPort->isDataAvailable() ? JNI_TRUE : JNI_FALSE;
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
    return NULL;
}

/*
* Class:     gnu_io_Dispatcher
* Method:    isDSR
* Signature: (I)Z
* Dispatches to: SerialPort::isDSR()
*/
JNIEXPORT jboolean JNICALL Java_gnu_io_Dispatcher_isDSR(JNIEnv *env, jobject thisObj, jint portHandle)
{
    BEGIN_NATIVE_FUNC_GET_PORT
    TEST_AND_CAST_TO_SERIAL_PORT
           return serialPort->isDSR() ? JNI_TRUE : JNI_FALSE;
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
    return NULL;
}

/*
* Class:     gnu_io_Dispatcher
* Method:    isDTR
* Signature: (I)Z
* Dispatches to: SerialPort::isDTR()
*/
JNIEXPORT jboolean JNICALL Java_gnu_io_Dispatcher_isDTR(JNIEnv *env, jobject thisObj, jint portHandle)
{
    BEGIN_NATIVE_FUNC_GET_PORT
    TEST_AND_CAST_TO_SERIAL_PORT
           return serialPort->isDTR() ? JNI_TRUE : JNI_FALSE;
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
    return NULL;
}

/*
* Class:     gnu_io_Dispatcher
* Method:    isFramingError
* Signature: (I)Z
* Dispatches to: SerialPort::isFramingError()
*/
JNIEXPORT jboolean JNICALL Java_gnu_io_Dispatcher_isFramingError(JNIEnv *env, jobject thisObj, jint portHandle)
{
    BEGIN_NATIVE_FUNC_GET_PORT
    TEST_AND_CAST_TO_SERIAL_PORT
           return serialPort->isFramingError() ? JNI_TRUE : JNI_FALSE;
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
    return NULL;
}

/*
* Class:     gnu_io_Dispatcher
* Method:    isOutputBufferEmpty
* Signature: (I)Z
* Dispatches to: ParallelPort::isOutputBufferEmpty()
*/
JNIEXPORT jboolean JNICALL Java_gnu_io_Dispatcher_isOutputBufferEmpty(JNIEnv *env, jobject thisObj, jint portHandle)
{
    BEGIN_NATIVE_FUNC_GET_PORT
    TEST_AND_CAST_TO_PARALLEL_PORT
           return parallelPort->isOutputBufferEmpty() ? JNI_TRUE : JNI_FALSE;
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
    return NULL;
}

/*
* Class:     gnu_io_Dispatcher
* Method:    isOverrunError
* Signature: (I)Z
* Dispatches to: SerialPort::isOverrunError()
*/
JNIEXPORT jboolean JNICALL Java_gnu_io_Dispatcher_isOverrunError(JNIEnv *env, jobject thisObj, jint portHandle)
{
    BEGIN_NATIVE_FUNC_GET_PORT
    TEST_AND_CAST_TO_SERIAL_PORT
           return serialPort->isOverrunError() ? JNI_TRUE : JNI_FALSE;
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
    return NULL;
}

/*
* Class:     gnu_io_Dispatcher
* Method:    isPaperOut
* Signature: (I)Z
* Dispatches to: ParallelPort::isPaperOut()
*/
JNIEXPORT jboolean JNICALL Java_gnu_io_Dispatcher_isPaperOut(JNIEnv *env, jobject thisObj, jint portHandle)
{
    BEGIN_NATIVE_FUNC_GET_PORT
    TEST_AND_CAST_TO_PARALLEL_PORT
           return parallelPort->isPaperOut() ? JNI_TRUE : JNI_FALSE;
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
    return NULL;
}

/*
* Class:     gnu_io_Dispatcher
* Method:    isParityError
* Signature: (I)Z
* Dispatches to: SerialPort::isParityError()
*/
JNIEXPORT jboolean JNICALL Java_gnu_io_Dispatcher_isParityError(JNIEnv *env, jobject thisObj, jint portHandle)
{
    BEGIN_NATIVE_FUNC_GET_PORT
    TEST_AND_CAST_TO_SERIAL_PORT
           return serialPort->isParityError() ? JNI_TRUE : JNI_FALSE;
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
    return NULL;
}

/*
* Class:     gnu_io_Dispatcher
* Method:    isPortReadable
* Signature: (I)Z
* Dispatches to: CommPort::isPortReadable()
*/
JNIEXPORT jboolean JNICALL Java_gnu_io_Dispatcher_isPortReadable(JNIEnv *env, jobject thisObj, jint portHandle)
{
    BEGIN_NATIVE_FUNC_GET_PORT
           return port->isPortReadable() ? JNI_TRUE : JNI_FALSE;
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
    return NULL;
}

/*
* Class:     gnu_io_Dispatcher
* Method:    isPortWritable
* Signature: (I)Z
* Dispatches to: CommPort::isPortWritable()
*/
JNIEXPORT jboolean JNICALL Java_gnu_io_Dispatcher_isPortWritable(JNIEnv *env, jobject thisObj, jint portHandle)
{
    BEGIN_NATIVE_FUNC_GET_PORT
           return port->isPortWritable() ? JNI_TRUE : JNI_FALSE;
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
    return NULL;
}

/*
* Class:     gnu_io_Dispatcher
* Method:    isPrinterBusy
* Signature: (I)Z
* Dispatches to: ParallelPort::isPrinterBusy()
*/
JNIEXPORT jboolean JNICALL Java_gnu_io_Dispatcher_isPrinterBusy(JNIEnv *env, jobject thisObj, jint portHandle)
{
    BEGIN_NATIVE_FUNC_GET_PORT
    TEST_AND_CAST_TO_PARALLEL_PORT
           return parallelPort->isPrinterBusy() ? JNI_TRUE : JNI_FALSE;
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
    return NULL;
}

/*
* Class:     gnu_io_Dispatcher
* Method:    isPrinterError
* Signature: (I)Z
* Dispatches to: ParallelPort::isPrinterError()
*/
JNIEXPORT jboolean JNICALL Java_gnu_io_Dispatcher_isPrinterError(JNIEnv *env, jobject thisObj, jint portHandle)
{
    BEGIN_NATIVE_FUNC_GET_PORT
    TEST_AND_CAST_TO_PARALLEL_PORT
           return parallelPort->isPrinterError() ? JNI_TRUE : JNI_FALSE;
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
    return NULL;
}

/*
* Class:     gnu_io_Dispatcher
* Method:    isPrinterSelected
* Signature: (I)Z
* Dispatches to: ParallelPort::isPrinterSelected()
*/
JNIEXPORT jboolean JNICALL Java_gnu_io_Dispatcher_isPrinterSelected(JNIEnv *env, jobject thisObj, jint portHandle)
{
    BEGIN_NATIVE_FUNC_GET_PORT
    TEST_AND_CAST_TO_PARALLEL_PORT
           return parallelPort->isPrinterSelected() ? JNI_TRUE : JNI_FALSE;
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
    return NULL;
}

/*
* Class:     gnu_io_Dispatcher
* Method:    isPrinterTimedOut
* Signature: (I)Z
* Dispatches to: ParallelPort::isPrinterTimedOut()
*/
JNIEXPORT jboolean JNICALL Java_gnu_io_Dispatcher_isPrinterTimedOut(JNIEnv *env, jobject thisObj, jint portHandle)
{
    BEGIN_NATIVE_FUNC_GET_PORT
    TEST_AND_CAST_TO_PARALLEL_PORT
           return parallelPort->isPrinterTimedOut() ? JNI_TRUE : JNI_FALSE;
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
    return NULL;
}

/*
* Class:     gnu_io_Dispatcher
* Method:    isRI
* Signature: (I)Z
* Dispatches to: SerialPort::isRI()
*/
JNIEXPORT jboolean JNICALL Java_gnu_io_Dispatcher_isRI(JNIEnv *env, jobject thisObj, jint portHandle)
{
    BEGIN_NATIVE_FUNC_GET_PORT
    TEST_AND_CAST_TO_SERIAL_PORT
           return serialPort->isRI() ? JNI_TRUE : JNI_FALSE;
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
    return NULL;
}

/*
* Class:     gnu_io_Dispatcher
* Method:    isRTS
* Signature: (I)Z
* Dispatches to: SerialPort::isRTS()
*/
JNIEXPORT jboolean JNICALL Java_gnu_io_Dispatcher_isRTS(JNIEnv *env, jobject thisObj, jint portHandle)
{
    BEGIN_NATIVE_FUNC_GET_PORT
    TEST_AND_CAST_TO_SERIAL_PORT
           return serialPort->isRTS() ? JNI_TRUE : JNI_FALSE;
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
    return NULL;
}

/*
* Class:     gnu_io_Dispatcher
* Method:    open
* Signature: (Ljava/lang/String;I)I
*/
JNIEXPORT jint JNICALL Java_gnu_io_Dispatcher_open(JNIEnv *env, jobject thisObj, jstring jPortName, jint portType)
{
    int result = 0;
    const char *portName = NULL;
    BEGIN_NATIVE_FUNCTION
        if (portType != RXTX_PORT_SERIAL && portType != RXTX_PORT_PARALLEL)
            throw IllegalArgumentException();
        portName = env->GetStringUTFChars(jPortName, NULL);
        if (portName != NULL) {
            PortManager& portManager = PortManager::getInstance();
            result = portManager.openPort(portName, (int) portType);
        }
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
    if (portName != NULL)
        env->ReleaseStringUTFChars(jPortName, portName);
    return (jint) result;
}

/*
* Class:     gnu_io_Dispatcher
* Method:    readBytes
* Signature: (I[BII)I
* Dispatches to: CommPort::readBytes(int)
*/
JNIEXPORT jint JNICALL Java_gnu_io_Dispatcher_readBytes(JNIEnv *env, jobject thisObj, jint portHandle, jbyteArray byteArray, jint offset, jint length)
{
    Buffer *readBuffer = NULL;
    BEGIN_NATIVE_FUNC_GET_PORT
        if (length < 1 || offset < 0)
            throw IllegalArgumentException();
        readBuffer = port->readBytes(length);
        if (readBuffer == NULL)
            return 0;
        env->SetByteArrayRegion(byteArray, offset, readBuffer->size, (jbyte*)readBuffer->data);
        int result = readBuffer->size;
        delete readBuffer;
        return (jint) result;
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
    if (readBuffer != NULL)
        delete readBuffer;
    return NULL;
}

/*
* Class:     gnu_io_Dispatcher
* Method:    restart
* Signature: (I)V
* Dispatches to: ParallelPort::restart()
*/
JNIEXPORT void JNICALL Java_gnu_io_Dispatcher_restart(JNIEnv *env, jobject thisObj, jint portHandle)
{
    BEGIN_NATIVE_FUNC_GET_PORT
    TEST_AND_CAST_TO_PARALLEL_PORT
           parallelPort->restart();
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
}

/*
* Class:     gnu_io_Dispatcher
* Method:    sendBreak
* Signature: (II)V
* Dispatches to: SerialPort::sendBreak(int)
*/
JNIEXPORT void JNICALL Java_gnu_io_Dispatcher_sendBreak(JNIEnv *env, jobject thisObj, jint portHandle, jint duration)
{
    BEGIN_NATIVE_FUNC_GET_PORT
    TEST_AND_CAST_TO_SERIAL_PORT
           serialPort->sendBreak((int) duration);
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
}


/*
* Class:     gnu_io_Dispatcher
* Method:    setDTR
* Signature: (IZ)V
* Dispatches to: SerialPort::setDTR(bool)
*/
JNIEXPORT void JNICALL Java_gnu_io_Dispatcher_setDTR(JNIEnv *env, jobject thisObj, jint portHandle, jboolean enable)
{
    BEGIN_NATIVE_FUNC_GET_PORT
    TEST_AND_CAST_TO_SERIAL_PORT
           serialPort->setDTR(enable == JNI_TRUE);
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
}

/*
* Class:     gnu_io_Dispatcher
* Method:    setFlowControlMode
* Signature: (II)V
* Dispatches to: SerialPort::setFlowControlMode(int)
*/
JNIEXPORT void JNICALL Java_gnu_io_Dispatcher_setFlowControlMode(JNIEnv *env, jobject thisObj, jint portHandle, jint flowControlMode)
{
    BEGIN_NATIVE_FUNC_GET_PORT
    TEST_AND_CAST_TO_SERIAL_PORT
        if (flowControlMode < RXTX_SERIAL_FLOWCONTROL_NONE || flowControlMode > RXTX_SERIAL_FLOWCONTROL_XONXOFF_OUT)
            throw IllegalArgumentException();
           serialPort->setFlowControlMode((int) flowControlMode);
    HANDLE_UCO_EXCEPTION
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
}

/*
* Class:     gnu_io_Dispatcher
* Method:    setInputBufferSize
* Signature: (II)V
* Dispatches to: CommPort::setInputBufferSize(int)
*/
JNIEXPORT void JNICALL Java_gnu_io_Dispatcher_setInputBufferSize(JNIEnv *env, jobject thisObj, jint portHandle, jint size)
{
    BEGIN_NATIVE_FUNC_GET_PORT
        if (size < 0)
            throw IllegalArgumentException();
           port->setInputBufferSize((int) size);
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
}

/*
* Class:     gnu_io_Dispatcher
* Method:    setMode
* Signature: (II)I
* Dispatches to: ParallelPort::setMode(int)
*/
JNIEXPORT jint JNICALL Java_gnu_io_Dispatcher_setMode(JNIEnv *env, jobject thisObj, jint portHandle, jint mode)
{
    BEGIN_NATIVE_FUNC_GET_PORT
    TEST_AND_CAST_TO_PARALLEL_PORT
        if (mode < RXTX_PARALLEL_MODE_ANY || mode > RXTX_PARALLEL_MODE_NIBBLE)
            throw IllegalArgumentException();
           return (jint) parallelPort->setMode((int) mode);
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
    return NULL;
}

/*
* Class:     gnu_io_Dispatcher
* Method:    setOutputBufferSize
* Signature: (II)V
* Dispatches to: CommPort::setOutputBufferSize(int)
*/
JNIEXPORT void JNICALL Java_gnu_io_Dispatcher_setOutputBufferSize(JNIEnv *env, jobject thisObj, jint portHandle, jint size)
{
    BEGIN_NATIVE_FUNC_GET_PORT
        if (size < 0)
            throw IllegalArgumentException();
           port->setOutputBufferSize((int) size);
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
}

/*
* Class:     gnu_io_Dispatcher
* Method:    setRTS
* Signature: (IZ)V
* Dispatches to: SerialPort::setRTS(bool)
*/
JNIEXPORT void JNICALL Java_gnu_io_Dispatcher_setRTS(JNIEnv *env, jobject thisObj, jint portHandle, jboolean rts)
{
    BEGIN_NATIVE_FUNC_GET_PORT
    TEST_AND_CAST_TO_SERIAL_PORT
           serialPort->setRTS(rts == JNI_TRUE);
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
}

/*
* Class:     gnu_io_Dispatcher
* Method:    setSerialPortParams
* Signature: (IIIII)V
* Dispatches to: SerialPort::setSerialPortParams(int, int, int, int)
*/
JNIEXPORT void JNICALL Java_gnu_io_Dispatcher_setSerialPortParams(JNIEnv *env, jobject thisObj, jint portHandle, jint baudRate, jint dataBits, jint stopBits, jint parity)
{
    BEGIN_NATIVE_FUNC_GET_PORT
    TEST_AND_CAST_TO_SERIAL_PORT
    if (dataBits < RXTX_SERIAL_DATABITS_5 ||
        dataBits > RXTX_SERIAL_DATABITS_8 ||
        stopBits < RXTX_SERIAL_STOPBITS_1 ||
        stopBits > RXTX_SERIAL_STOPBITS_1_5 ||
        parity < RXTX_SERIAL_PARITY_NONE ||
        parity > RXTX_SERIAL_PARITY_SPACE)
        throw IllegalArgumentException();
           serialPort->setSerialPortParams((int) baudRate, (int) dataBits, (int) stopBits, (int) parity);
    HANDLE_UCO_EXCEPTION
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
}

/*
* Class:     gnu_io_Dispatcher
* Method:    suspend
* Signature: (I)V
* Dispatches to: ParallelPort::suspend()
*/
JNIEXPORT void JNICALL Java_gnu_io_Dispatcher_suspend(JNIEnv *env, jobject thisObj, jint portHandle)
{
    BEGIN_NATIVE_FUNC_GET_PORT
    TEST_AND_CAST_TO_PARALLEL_PORT
           parallelPort->suspend();
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
}

/*
* Class:     gnu_io_Dispatcher
* Method:    version
* Signature: ()Ljava/lang/String;
*/
JNIEXPORT jstring JNICALL Java_gnu_io_Dispatcher_version(JNIEnv *env, jobject thisObj)
{
    try
    {
        TO_JSTRING(jString, RXTX_NATIVE_VERSION)
        return jString;
    }
    catch (...) {}
    return NULL;
}

/*
* Class:     gnu_io_Dispatcher
* Method:    writeBytes
* Signature: (I[BII)I
* Dispatches to: CommPort::writeBytes(char*, int)
*/
JNIEXPORT jint JNICALL Java_gnu_io_Dispatcher_writeBytes(JNIEnv *env, jobject thisObj, jint portHandle, jbyteArray byteArray, jint offset, jint length)
{
    jbyte *writeBuff = NULL;
    BEGIN_NATIVE_FUNC_GET_PORT
        if (length < 1 || offset < 0)
            throw IllegalArgumentException();
        writeBuff = new jbyte[length]; // Will be deleted by port->writeBytes(...)
        env->GetByteArrayRegion(byteArray, offset, length, writeBuff);
        int result = port->writeBytes(new Buffer((char*) writeBuff, length));
        return (jint) result;
    END_NATIVE_FUNC_HANDLE_EXCEPTIONS
    if (writeBuff != NULL)
        delete writeBuff;
    return NULL;
}
