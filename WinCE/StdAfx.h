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
// stdafx.h : include file for standard system include files,
//  or project specific include files that are used frequently, but
//      are changed infrequently
//

#if !defined(AFX_STDAFX_H__B78985E1_0CAF_4EE2_8807_4D04C75EAFCE__INCLUDED_)
#define AFX_STDAFX_H__B78985E1_0CAF_4EE2_8807_4D04C75EAFCE__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000


#include <jni.h>
#include "gnu_io_RXTXPort.h"
#include "gnu_io_RXTXCommDriver.h"

// Insert your headers here
#define WIN32_LEAN_AND_MEAN		// Exclude rarely-used stuff from Windows headers

#include <windows.h>
#include <string.h>
// TODO: reference additional headers your program requires here

//{{AFX_INSERT_LOCATION}}
// Microsoft eMbedded Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_STDAFX_H__B78985E1_0CAF_4EE2_8807_4D04C75EAFCE__INCLUDED_)
