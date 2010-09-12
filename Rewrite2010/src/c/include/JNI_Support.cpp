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
#include <JNI_Support.h>
#include <string>

#ifndef _THIS_FILE_
    #define _THIS_FILE_ "JNI_Support.cpp"
#endif

// ------------------------------------------------------------------------- //
// Global helper functions
// ------------------------------------------------------------------------- //

char* buildSourceFileString(const char *fileName, int lineNumber)
{
    int len = strlen(fileName) + 12;
    char *str = new char[len];
    memset(str, 0, len);
    sprintf(str, "%s(%d)", fileName, lineNumber);
    return str;
}

/**
* Logs a message to the console.
* 
* @param env JNI environment.
* @param message The message to log.
* @param source The source of the message (usually the file name).
* @param lineNumber Source file line number.
*
* @throws JavaRuntimeException
* 
*/
jclass logClass = NULL;
jmethodID logMethod;
void log(JNIEnv *env, const char *message,  const char *source, int lineNumber)
{
    if (logClass == NULL)
    {
        logClass = env->FindClass("gnu/io/Log");
        if (logClass == NULL)
            throw JavaRuntimeException();
        logMethod = env->GetStaticMethodID(logClass, "log", "(Ljava/lang/String;Ljava/lang/String;)V");
        if (logMethod == NULL)
            throw JavaRuntimeException();
    }
    char* srcFileStr = buildSourceFileString(source, lineNumber);
    TO_JSTRING(jMessage, message)
    TO_JSTRING(jSrcFileStr, srcFileStr)
    delete srcFileStr;
    env->CallStaticVoidMethod(logClass, logMethod, jMessage, jSrcFileStr);
}

// ------------------------------------------------------------------------- //
// Native library load
// ------------------------------------------------------------------------- //

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved)
{
    JNIEnv *env;
    if (jvm->GetEnv((void **)&env, JNI_VERSION_1_4))
        return JNI_ERR;
    try
    {
        log(env, "Native library initialized", _THIS_FILE_, __LINE__);
    } 
    catch (JavaRuntimeException &e)
    {
        JavaRuntimeException noWarn = e;
        return JNI_ERR;
    }
    return JNI_VERSION_1_4;
}
