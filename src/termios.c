//#define TRACE
/*-------------------------------------------------------------------------
|   rxtx is a native interface to serial ports in java.
|   Copyright 1997-2001 by Trent Jarvi trentjarvi@yahoo.com.
|   Copyright 1998-2001 by Wayne roberts wroberts1@home.com
|
|   This library is free software; you can redistribute it and/or
|   modify it under the terms of the GNU Library General Public
|   License as published by the Free Software Foundation; either
|   version 2 of the License, or (at your option) any later version.
|
|   If you compile this program with cygwin32 tools this package falls
|   under the GPL.  See COPYING.CYGNUS for details.
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
#include <windows.h>
#include <stdio.h>
#include <errno.h>
#include "win32termios.h"

/*
 * odd malloc.h error with lcc compiler
 * winsock has FIONREAD with lcc
 */

#ifdef __LCC__
#	include <winsock.h>
#else
#	include <malloc.h>
#endif /* __LCC__ */

#define SIGIO 0

int my_errno;
struct termios_list
{
	char *filename;
	int my_errno;
	HANDLE hComm;
	struct termios *ttyset;
	struct serial_struct *sstruct;
	int flags;
	OVERLAPPED rol;
	OVERLAPPED wol;
	OVERLAPPED sol;
	int fd;
	struct termios_list *next;
	struct termios_list *prev;
};
struct termios_list *first_tl = NULL;

/*----------------------------------------------------------
get_fd()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:    None
   comments:    
----------------------------------------------------------*/

int get_fd( char *filename )
{
	struct termios_list *index = first_tl;

	ENTER( "get_fd" );
	if( !index )
	{
		return -1;
	}

	while( strcmp( index->filename, filename ) )
	{
		index = index->next;
		if( !index->next )
			return( -1 );
	}
	LEAVE( "get_fd" );
	return( index->fd );
}

/*----------------------------------------------------------
get_filename()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:    None
   comments:    
----------------------------------------------------------*/

char *get_filename( int fd )
{
	struct termios_list *index = first_tl;

	ENTER( "get_filename" );
	if( !index )
		return( "bad" );
	while( index->fd != fd )
	{
		if( index->next == NULL )
			return( "bad" );
		index = index->next;
	}
	LEAVE( "get_filename" );
	return( index->filename );
}

/*----------------------------------------------------------
dump_termios_list()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:    None
   comments:    
----------------------------------------------------------*/

void dump_termios_list( char *foo )
{
#ifdef DEBUG
	struct termios_list *index = first_tl;
	printf( "============== %s start ===============\n", foo );
	if ( index )
	{
		printf( "%i filename | %s\n", index->fd, index->filename );
	}
/*
	if ( index->next )
	{
		printf( "%i filename | %s\n", index->fd, index->filename );
	}
*/
	printf( "============== %s end  ===============\n", foo );
#endif
}

/*----------------------------------------------------------
set_errno()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:    None
   comments:   FIXME   
----------------------------------------------------------*/

void set_errno( int error )
{
	my_errno = error;
}

/*----------------------------------------------------------
nanosleep()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:    Sleep( MilliSeconds )
   comments:    
----------------------------------------------------------*/

int nanosleep( const struct timespec *req, struct timespec *rem )
{
	if ( req->tv_nsec )
		Sleep( req->tv_nsec/1000000 );
	if ( req->tv_sec )
		Sleep( req->tv_sec * 1000 );
	return 0;
}

/*----------------------------------------------------------
CBR_toB()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:     None
   comments:    
----------------------------------------------------------*/

int CBR_to_B( int Baud )
{
	switch ( Baud )
	{

		case 0:			return( B0 );
		case 50:		return( B50 );
		case 75:		return( B75 );
		case CBR_110:		return( B110 );
		case 134:		return( B134 );
		case 150:		return( B150 );
		case 200:		return( B200 );
		case CBR_300:		return( B300 );
		case CBR_600:		return( B600 );
		case CBR_1200:		return( B1200 );
		case 1800:		return( B1800 );
		case CBR_2400:		return( B2400 );
		case CBR_4800:		return( B4800 );
		case CBR_9600:		return( B9600 );
		case CBR_19200:		return( B19200 );
		case CBR_28800:		return( B28800 );
		case CBR_38400:		return( B38400 );
		case CBR_57600:		return( B57600 );
		case CBR_115200:	return( B115200 );
		/*  14400, 128000 and 256000 are windows specific but need to
		 *  work.
		 *  hosed on my hardware....
		 */
		case CBR_14400:		return( B14400 );
		case CBR_128000:	return( B128000 );
		case CBR_256000:	return( B256000 );

		/*  The following could be used on linux and should be able to
		 *  work on windows if we get control of baud/divisor.
		 */

		case CBR_230400:	return( B230400 );
		case CBR_460800:	return( B460800 );
		case CBR_500000:	return( B500000 );
		case CBR_576000:	return( B576000 );
		case CBR_921600:	return( B921600 );
		case CBR_1000000:	return( B1000000 );
		case CBR_1152000:	return( B1152000 );
		case CBR_1500000:	return( B1500000 );
		case CBR_2000000:	return( B2000000 );
		case CBR_2500000:	return( B2500000 );
		case CBR_3000000:	return( B3000000 );
		case CBR_3500000:	return( B3500000 );
		case CBR_4000000:	return( B4000000 );
		default:
			set_errno(EINVAL );
			return -1;
	}
}

/*----------------------------------------------------------
B_to_CBR()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:
   comments:      None
----------------------------------------------------------*/

int B_to_CBR( int Baud )
{
	int ret;
	ENTER( "B_to_CBR" );
	switch ( Baud )
	{
		case 0:		ret = 0;		break;
		case B50:	ret = 50;		break;
		case B75:	ret = 75;		break;
		case B110:	ret = CBR_110;		break;
		case B134:	ret = 134;		break;
		case B150:	ret = 150;		break;
		case B200:	ret = 200;		break;
		case B300:	ret = CBR_300;		break;
		case B600:	ret = CBR_600;		break;
		case B1200:	ret = CBR_1200;		break;
		case B1800:	ret = 1800;		break;
		case B2400:	ret = CBR_2400;		break;
		case B4800:	ret = CBR_4800;		break;
		case B9600:	ret = CBR_9600;		break;
		case B19200:	ret = CBR_19200;	break;
		case B38400:	ret = CBR_38400;	break;
		case B57600:	ret = CBR_57600;	break;
		case B115200:	ret = CBR_115200;	break;

		/*  14400, 128000 and 256000 are windows specific but need to
		 *  work.
		 */
		case B14400:	ret = CBR_14400;	break;
		case B128000:	ret = CBR_128000;	break;
		case B256000: 	ret = CBR_256000;	break;

		/*  The following could be used on linux and should be able to
		 *  work on windows if we get control of baud/divisor.
		 */
		case B230400:	ret = CBR_230400;	break;
		case B460800:	ret = CBR_460800;	break;
		case B500000:	ret = CBR_500000;	break;
		case B576000:	ret = CBR_576000;	break;
		case B921600:	ret = CBR_921600;	break;
		case B1000000:	ret = CBR_1000000;	break;
		case B1152000:	ret = CBR_1152000;	break;
		case B1500000:	ret = CBR_1500000;	break;
		case B2000000:	ret = CBR_2000000;	break;
		case B2500000:	ret = CBR_2500000;	break;
		case B3000000:	ret = CBR_3000000;	break;
		case B3500000:	ret = CBR_3500000;	break;
		case B4000000:	ret = CBR_4000000;	break;
	
		default:
			fprintf( stderr, "B_to_CBR: invalid baudrate: %#o\n",
				Baud );
			set_errno( EINVAL );
			return -1;
	}
	LEAVE( "B_to_CBR" );
	return ret;
}

/*----------------------------------------------------------
bytesize_to_termios()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:      None
   comments:    
----------------------------------------------------------*/

int bytesize_to_termios( int ByteSize )
{
	switch ( ByteSize )
	{
		case 5: return( CS5 );
		case 6: return( CS6 );
		case 7: return( CS7 );
		case 8:
		default: return( CS8 );
	}
}

/*----------------------------------------------------------
termios_to_bytesize()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:     None
   comments:    
----------------------------------------------------------*/

int termios_to_bytesize( int cflag )
{
	switch ( cflag )
{
		case CS5: return( 5 );
		case CS6: return( 6 );
		case CS7: return( 7 );
		case CS8:
		default: return( 8 );
	}
}

/*----------------------------------------------------------
get_dos_port()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:     None
   comments:    
----------------------------------------------------------*/

const char *get_dos_port( char const *name )
{
	ENTER( "get_dos_port" );
	if ( !strcmp( name, "/dev/cua0" ) ) return( "COM1" );
	if ( !strcmp( name, "/dev/cua1" ) ) return( "COM2" );
	if ( !strcmp( name, "/dev/cua2" ) ) return( "COM3" );
	if ( !strcmp( name, "/dev/cua3" ) ) return( "COM4" );
	LEAVE( "get_dos_port" );
	return( ( const char * ) name );
}

/*----------------------------------------------------------
ClearError()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:     ClearCommError()
   comments:    
----------------------------------------------------------*/

static BOOL ClearError( HANDLE hComPort )
{
	COMSTAT Stat;
	DWORD ErrCode;

	return ClearCommError( hComPort, &ErrCode, &Stat );
}

/*----------------------------------------------------------
FillDCB()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:     GetCommState(),  SetCommState(), SetCommTimeouts()
   comments:    
----------------------------------------------------------*/

BOOL FillDCB( DCB *dcb, HANDLE hCommPort, COMMTIMEOUTS Timeout )
{

	ENTER( "FillDCB" );
	dcb->DCBlength = sizeof( dcb );
	if ( !GetCommState( hCommPort, dcb ) )
{
		printf( "GetCommState\n" );
		return( -1 );
	}
	dcb->BaudRate        = CBR_9600 ;
	dcb->ByteSize        = 8;
	dcb->Parity          = NOPARITY;
	dcb->StopBits        = ONESTOPBIT;
	dcb->fDtrControl     = DTR_CONTROL_ENABLE;
	dcb->fRtsControl     = RTS_CONTROL_ENABLE;
	dcb->fOutxCtsFlow    = FALSE;
	dcb->fOutxDsrFlow    = FALSE;
	dcb->fDsrSensitivity = FALSE;
	dcb->fOutX           = FALSE;
	dcb->fInX            = FALSE;
	dcb->fTXContinueOnXoff = FALSE;
	dcb->XonChar         = 0x11;
	dcb->XoffChar        = 0x13;
	dcb->XonLim          = 0;
	dcb->XoffLim         = 0;
	dcb->fParity = TRUE;
	if ( EV_BREAK|EV_CTS|EV_DSR|EV_ERR|EV_RING|( EV_RLSD & EV_RXFLAG ) )
		dcb->EvtChar = '\n'; 
	else dcb->EvtChar = '\0';
	if ( !SetCommState( hCommPort, dcb ) )
	{
		printf( "SetCommState\n" );
		YACK();
		return( -1 );
	}
	if ( !SetCommTimeouts( hCommPort, &Timeout ) )
	{
		YACK();
		printf( "SetCommTimeouts\n" );
		return( -1 );
	}
	LEAVE( "FillDCB" );
	return ( TRUE ) ;
}

/*----------------------------------------------------------
close()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:      SetCommMask(), CloseHandle()
   comments:    
----------------------------------------------------------*/

int close( int fd )
{
	/*
	errno = EBADF;
	errno = EINTR;
	errno = EIO;
	*/
	struct termios_list *index;

	ENTER( "close" );
	if( !first_tl || !first_tl->hComm )
	{
		printf( "gotit!" );
		return( 0 );
	}
	if ( fd <= 0 )
	{
		printf( "close -fd" );
		return 0;
	}
	index = find_port( fd );
	if ( !index )
	{
		printf( "close !index" );
		//fprintf( stderr, "No info known about the port being closed %i\n", fd );
		return -1;
	}

	if ( index->hComm != INVALID_HANDLE_VALUE )
	{
		if ( !SetCommMask( index->hComm, EV_RXCHAR ) )
		{
			YACK();
			fprintf( stderr, "eventLoop hung\n" );
		}
		CloseHandle( index->hComm );
	}
/*
	else
		fprintf( stderr, "close():  Invalid Port Reference for %s\n",
			index->filename );
*/
	if ( index->next  && index->prev )
	{
		index->next->prev = index->prev;
		index->prev->next = index->next;
	}
	else if ( index->prev )
	{
		index->prev->next = NULL;
	}
	else if ( index->next )
	{
		index->next->prev = NULL;
		first_tl = index->next;
	}
	else
		first_tl = NULL;
	if ( index )
	{
		if ( index->rol.hEvent ) CloseHandle( index->rol.hEvent );
		if ( index->wol.hEvent ) CloseHandle( index->wol.hEvent );
		if ( index->sol.hEvent ) CloseHandle( index->sol.hEvent );
		if ( index->ttyset )  free( index->ttyset );
		if ( index->sstruct )  free( index->sstruct );
		if ( index->filename ) free( index->filename );
		free( index );
	}
	//dump_termios_list( "close" );
	LEAVE( "close" );
	return 0;
}

/*----------------------------------------------------------
cfmakeraw()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:     None
   comments:    
----------------------------------------------------------*/

void cfmakeraw( struct termios *s_termios )
{
	ENTER( "cfmakeraw" );
	s_termios->c_iflag &= ~( IGNBRK|BRKINT|PARMRK|ISTRIP
		|INLCR|IGNCR|ICRNL|IXON );
	s_termios->c_oflag &= ~OPOST;
	s_termios->c_lflag &= ~( ECHO|ECHONL|ICANON|ISIG|IEXTEN );
	s_termios->c_cflag &= ~( CSIZE|PARENB );
	s_termios->c_cflag |= CS8;
	LEAVE( "cfmakeraw" );
}

/*----------------------------------------------------------
init_termios()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:
   comments:    
----------------------------------------------------------*/

BOOL init_serial_struct( struct serial_struct *sstruct )
{
	/* int */

	/*
	FIXME

	This needs to use inb() to read the actual baud_base
	and divisor from the UART registers.  Question is how
	far do we take this?

	*/

	sstruct->custom_divisor = 0;
	sstruct->baud_base = 115200;

	/* not currently used check values before using */

	/* unsigned short */

	sstruct->close_delay = 0;
	sstruct->closing_wait = 0;
	sstruct->iomem_reg_shift = 0;

	/* int */

	sstruct->type = 0;
	sstruct->line = 0;
	sstruct->irq = 0;
	sstruct->flags = 0;
	sstruct->xmit_fifo_size = 0;
	sstruct->hub6 = 0;

	/* unsigned int */

	sstruct->port = 0;
	sstruct->port_high = 0;

	/* char */

	sstruct->io_type = 0;

	/* unsigned char * */

	sstruct->iomem_base = NULL;

	return TRUE;

}
/*----------------------------------------------------------
init_termios()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:
   comments:    
----------------------------------------------------------*/

BOOL init_termios(struct termios *ttyset )
{
	ENTER( "init_termios" );
	if ( !ttyset )
		return FALSE;
	memset( ttyset, 0, sizeof( struct termios ) );
	//cfsetospeed( ttyset, B9600 );
	cfsetospeed( ttyset, 14400 );
	cfmakeraw( ttyset );
	ttyset->c_cc[VINTR] = 0x03;	/* 0: C-c */
	ttyset->c_cc[VQUIT] = 0x1c;	/* 1: C-\ */
	ttyset->c_cc[VERASE] = 0x7f;	/* 2: <del> */
	ttyset->c_cc[VKILL] = 0x15;	/* 3: C-u */
	ttyset->c_cc[VEOF] = 0x04;	/* 4: C-d */
	ttyset->c_cc[VTIME] = 0;	/* 5: read timeout */
	ttyset->c_cc[VMIN] = 1;		/* 6: read returns after this
						many bytes */
	ttyset->c_cc[VSUSP] = 0x1a;	/* 10: C-z */
	ttyset->c_cc[VEOL] = '\r';	/* 11: */
	ttyset->c_cc[VREPRINT] = 0x12;	/* 12: C-r */
//	ttyset->c_cc[VDISCARD] = 0x;	/* 13: IEXTEN only */
	ttyset->c_cc[VWERASE] = 0x17;	/* 14: C-w */
	ttyset->c_cc[VLNEXT] = 0x16;	/* 15: C-w */
	ttyset->c_cc[VEOL2] = '\n';	/* 16: */
	LEAVE( "init_termios" );
	return TRUE;
	/* default VTIME = 0, VMIN = 1: read blocks forever until one byte */
}

/*----------------------------------------------------------
port_opened()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:     None
   comments:    
----------------------------------------------------------*/

int port_opened( const char *filename )
{
	struct termios_list *index = first_tl;
	
	ENTER( "port_opened" );
	if ( ! index )
		return 0;
	if( !strcmp( index->filename, filename ) )
		return 1;
	while ( index->next )
	{
		index = index->next;
		if( !strcmp( index->filename, filename ) )
			return 1;
	}
	LEAVE( "port_opened" );
	return 0;
}

/*----------------------------------------------------------
open_port()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:   CreateFile(), SetupComm(), CreateEvent()
   comments:    
	FILE_FLAG_OVERLAPPED allows one to break out the select()
	so RXTXPort.close() does not hang.

	The setDTR() and setDSR() are the functions that noticed
	to be blocked in the java close.  Basically ioctl(TIOCM[GS]ET)
	are where it hangs.

	FILE_FLAG_OVERLAPPED also means we need to create valid OVERLAPPED
	structure in serial_select.
----------------------------------------------------------*/

int open_port( struct termios_list *port )
{
	ENTER( "open_port" );
	port->hComm = CreateFile( port->filename,
		GENERIC_READ | GENERIC_WRITE,
		0,
		0,
		OPEN_EXISTING,
		FILE_FLAG_OVERLAPPED,
		0
	);
	if ( port->hComm == INVALID_HANDLE_VALUE )
	{
		YACK();
		errno = EINVAL;
		//printf( "open failed %s\n", port->filename );
		return -1;
	}
	if( !SetupComm( port->hComm, 2048, 1024 ) )
	{
		YACK();
		return -1;
	}

	memset( &port->rol, 0, sizeof( OVERLAPPED ) );
	memset( &port->wol, 0, sizeof( OVERLAPPED ) );
	memset( &port->sol, 0, sizeof( OVERLAPPED ) );

	port->rol.hEvent = CreateEvent( NULL, TRUE, FALSE, NULL );

	if ( !port->rol.hEvent )
	{
		YACK();
		fprintf( stderr, "Could not create read overlapped\n" );
		goto fail;
	}

	port->sol.hEvent = CreateEvent( NULL, TRUE, FALSE, NULL );

	if ( !port->sol.hEvent )	
	{
		YACK();
		fprintf( stderr, "Could not create select overlapped\n" );
		goto fail;
	}
	port->wol.hEvent = CreateEvent( NULL, TRUE, FALSE, NULL );

	if ( !port->wol.hEvent )	
	{
		YACK();
		fprintf( stderr, "Could not create write overlapped\n" );
		goto fail;
	}
	LEAVE("open_port" );
	return( 0 );
fail:
	return( -1 );
}

/*----------------------------------------------------------
termios_list()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:     None
   comments:    
----------------------------------------------------------*/

struct termios_list *find_port( int fd )
{

	struct termios_list *index = first_tl;

	ENTER( "find_port" );
	if( !first_tl )
	{
		LEAVE( "find_port" );
		return NULL;
	}

	while( index->fd )
	{
		if ( index->fd == fd )
		{
			LEAVE( "find_port" );
			return index;
		}
		if ( !index->next )
			break;
		index = index->next;
	}
	LEAVE( "find_port" );
	return NULL;
}

/*----------------------------------------------------------
get_free_fd()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:       None
   comments:    
----------------------------------------------------------*/

int get_free_fd()
{
	int next, last;
	struct termios_list *index = first_tl;

	ENTER( "get_free_fd" );
	if ( !index )
	{
		return( 1 );
	}
	if ( !index->fd )
	{
		printf( "!index->fd\n" );
		return( 1 );
	}
	if ( index->fd > 1)
	{
		first_tl = index;
		return ( 1 );
	}
	
	last = index->fd;

	while( index->next )
	{
		next = index->next->fd;
		if ( next !=  last + 1 )
		{
			return( last + 1 );
			
		}
		index = index->next;
		last = next;
	}
	LEAVE( "get_free_fd" );
	return( index->fd + 1 );
}

/*----------------------------------------------------------
add_port()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:      None
   comments:    
----------------------------------------------------------*/

struct termios_list *add_port( const char *filename )
{
	struct termios_list *index = first_tl;
	struct termios_list *port;

	ENTER( "add_port" );

	port = malloc( sizeof( struct termios_list ) );
	if( !port )
		goto fail;
	memset( port, 0, sizeof( struct termios_list ) );

	port->ttyset = malloc( sizeof( struct termios ) );
	if( ! port->ttyset )
		goto fail;
	memset( port->ttyset, 0, sizeof( struct termios ) );

	port->sstruct = malloc( sizeof( struct serial_struct ) );
	if( ! port->ttyset )
		goto fail;
	memset( port->sstruct, 0, sizeof( struct serial_struct ) );

	port->filename=strdup( filename );
	if( ! port->filename )
		goto fail;

	port->fd = get_free_fd();

	
	if ( !first_tl )
	{
		port->prev = NULL;
		first_tl = port;
	}
	else
	{
		while ( index->next )
			index = index->next;
		if ( port == first_tl )
		{
			port->prev = NULL;
			port->next = first_tl;
			first_tl->prev = port;
			first_tl = port;
		}
		else
		{
			port->prev = index;
			index->next = port;
		}
	}
	port->next = NULL;
	LEAVE( "add_port" );
	return port;

fail:
	fprintf( stderr, "add_port:  Out Of Memory\n");
	if ( port->ttyset )  free( port->ttyset );
	if ( port->sstruct )  free( port->sstruct );
	if ( port->filename ) free( port->filename );
	if ( port ) free( port );
	return port;
}

/*----------------------------------------------------------
check_port_capabilities()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:      GetCommProperties(), GetCommState()
   comments:    
----------------------------------------------------------*/

int check_port_capabilities( struct termios_list *index )
{
	COMMPROP cp;
	DCB	dcb;

	ENTER( "check_port_capabilities" );
	/* check for capabilities */
	GetCommProperties( index->hComm, &cp );
	if ( !( cp.dwProvCapabilities & PCF_DTRDSR ) )
		printf( "%s: no DTR & DSR support\n", index->filename );
	if ( !( cp.dwProvCapabilities & PCF_RLSD ) )
		printf( "%s: no carrier detect (RLSD) support\n",
			index->filename );
	if ( !( cp.dwProvCapabilities & PCF_RTSCTS ) )
		printf( "%s: no RTS & CTS support\n", index->filename );
	if ( !( cp.dwProvCapabilities & PCF_TOTALTIMEOUTS ) )
		printf( "%s: no timeout support\n", index->filename );
	if ( !GetCommState( index->hComm, &dcb ) )
	{
		YACK();
		printf( "GetCommState\n" );
		return -1;
	}
	LEAVE( "check_port_capabilities" );
	return 0;

}

/*----------------------------------------------------------
serial_open()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:    None
   comments:    
----------------------------------------------------------*/

int serial_open( const char *filename, int flags, ... )
{
	struct termios_list *port;

	ENTER( "serial_open" );
	if ( port_opened( filename ) )
	{
		printf( "Port is already opened" );
		return( -1 );
	}
	port = add_port( filename );
	if( !port )
	{
		printf( "open !port\n" );
		return( -1 );
	}
	
	if ( open_port( port ) )
	{
/*
		fprintf( stderr, "open():  Invalid Port Reference for %s\n",
			filename );
*/
		close( port->fd );
		return -1;
	}

	if( check_port_capabilities( port ) )
	{
		printf( "check_port_capabilites!" );
		close( port->fd );
		return -1;
	}

	init_termios( port->ttyset );
	init_serial_struct( port->sstruct );

	/* set default condition */
	tcsetattr( port->fd, 0, port->ttyset );

	/* if opened with non-blocking, then operating non-blocking */
	if ( flags & O_NONBLOCK )
		port->flags = O_NONBLOCK;
	else
		port->flags = 0;


	dump_termios_list( "open filename" );
	if( !first_tl->hComm )
		fprintf( stderr, "open():  Invalid Port Reference for %s\n",
			port->filename );
	if ( first_tl->hComm == INVALID_HANDLE_VALUE )
		printf( "test\n" );
	LEAVE( "serial_open" );
	return( port->fd );
}


/*----------------------------------------------------------
serial_write()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:     WriteFile(), GetLastError(), ClearError(),
                 WaitForSingleObject(),  GetOverlappedResult(),
                 FlushFileBuffers(), Sleep()
   comments:    
----------------------------------------------------------*/

int serial_write( int fd, char *Str, int length )
{
	DWORD nBytes, pendingResult;
	struct termios_list *index;

	ENTER( "serial_write" );
	if ( fd <= 0 )
		return 0;
	index = find_port( fd );
	if ( !index )
	{
		fprintf( stderr, "No info known about the port. write %i\n", fd );
		return -1;
	}
	/***** output mode flags (c_oflag) *****/
	/* FIXME: OPOST: enable ONLCR, OXTABS & ONOEOT */
	/* FIXME: ONLCR: convert newline char to CR & LF */
	/* FIXME: OXTABS: convert tabs to spaces */
	/* FIXME: ONOEOT: discard ^D (004) */

	if ( !WriteFile( index->hComm, Str, length, &nBytes, &index->wol ) )
	{
		if ( GetLastError() != ERROR_IO_PENDING )
		{
			ClearError( index->hComm );
			printf( "write error\n" );
			nBytes=-1;
			goto end;
		}
	}
	pendingResult = WaitForSingleObject( index->wol.hEvent, INFINITE );
	switch( pendingResult )
	{
		case WAIT_TIMEOUT:
			return( 0 );
		case WAIT_FAILED:
			return( -1 );
		case WAIT_OBJECT_0:
			break;
	}
	
			//if ( 	( pendingResult !=  WAIT_OBJECT_0 ) ||
	GetOverlappedResult( index->hComm, &index->wol, &nBytes, FALSE );
	FlushFileBuffers( index->hComm );
	/*
		I'm sure there is a better way to do this but write() will
		outrace read() without this currently.

		I think what we really want to wait for is the other process's
		read thread.
	*/
	Sleep( 50 );
end:
	LEAVE( "serial_write" );
	return nBytes;
}

/*----------------------------------------------------------
serial_read()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:      ReadFile(), GetLastError(), WaitForSingleObject()
                  GetOverLappedResult()
   comments:    
----------------------------------------------------------*/

int serial_read( int fd, void *vb, int size )
{
	DWORD nBytes = 0, total = 0, waiting = 0;
	char *b = ( char * )vb;
	int err, vmin;
	struct termios_list *index;

	ENTER( "serial_read" );
	if ( fd <= 0 )
		return 0;
	index = find_port( fd );
	if ( !index )
	{
		fprintf( stderr, "No info known about the port. read %i\n", fd );
		return -1;
	}

	/* FIXME: CREAD: without this, data cannot be read
	   FIXME: PARMRK: mark framing & parity errors
	   FIXME: IGNCR: ignore \r
	   FIXME: ICRNL: convert \r to \n
	   FIXME: INLCR: convert \n to \r
	*/

	if ( index->flags & O_NONBLOCK )
	{
		/* if vmin would 1 or more, then we would block */
		vmin = 0;
	}
	else
	{
		/* read blocks forever on VMIN chars */
		vmin = index->ttyset->c_cc[VMIN];
	}
	
	while ( ( ( nBytes <= vmin ) || ( size > 0 ) ) && !waiting )
	{
		if ( !ReadFile( index->hComm, b, size, &nBytes, &index->rol ) )
		{
			err = GetLastError();
			switch ( err )
			{
				case ERROR_BROKEN_PIPE:
					nBytes = 0;
					break;
				case ERROR_MORE_DATA:
					break;
				case ERROR_IO_PENDING:
					break;
				default:
					YACK();
					return -1;
			}
		}
		waiting = WaitForSingleObject( index->rol.hEvent, 500 );
		if ( waiting == WAIT_OBJECT_0 )
		{
			if ( ! GetOverlappedResult( index->hComm,
							&index->rol,
							&nBytes,
							FALSE ) )
			{
				fprintf( stderr, "read error\n" );
			}
			else
			{
				waiting = 0;
			}
		}
		else if ( waiting == WAIT_TIMEOUT )
		{
			fprintf( stderr, "read timeout\n" );
		}
		else
		{
			fprintf( stderr, "read overlap structure problem\n" );
		}
		size -= nBytes;
		b += nBytes;
		total += nBytes;
		/* wait for no chars, can return whenever */
		if ( vmin == 0 ) break;
	}
	LEAVE( "serial_read" );
	return total;
}

/*----------------------------------------------------------
cfsetospeed()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:     None
   comments:    
----------------------------------------------------------*/

int cfsetospeed( struct termios *s_termios, speed_t speed )
{
	ENTER( "cfsetospeed" );
	if ( speed & ~CBAUD )
{
		fprintf( stderr, "cfsetospeed: not speed: %#o\n", speed );
		return 0;
	}
	s_termios->c_ispeed = s_termios->c_ospeed = speed;
	/* clear baudrate */
	s_termios->c_cflag &= ~CBAUD;
	s_termios->c_cflag |= speed;
	LEAVE( "cfsetospeed" );
	return 1;
}

/*----------------------------------------------------------
cfsetispeed()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:     None
   comments:    
----------------------------------------------------------*/

int cfsetispeed( struct termios *s_termios, speed_t speed )
{
	return cfsetospeed( s_termios, speed );
}

/*----------------------------------------------------------
cfsetspeed()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:     None
   comments:    
----------------------------------------------------------*/

int cfsetspeed( struct termios *s_termios, speed_t speed )
{
	return cfsetospeed( s_termios, speed );
}

/*----------------------------------------------------------
cfgetospeed()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:     None
   comments:    
----------------------------------------------------------*/

speed_t cfgetospeed( struct termios *s_termios )
{
	return s_termios->c_ospeed;
}

/*----------------------------------------------------------
cfgetispeed()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:     None
   comments:    
----------------------------------------------------------*/

speed_t cfgetispeed( struct termios *s_termios )
{
	return s_termios->c_ispeed;
}

/*----------------------------------------------------------
TermiosToDCB()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:     None
   comments:    
----------------------------------------------------------*/

int TermiosToDCB( struct termios *s_termios, DCB *dcb )
{
	s_termios->c_ispeed = s_termios->c_cflag & CBAUD;
	s_termios->c_ospeed = s_termios->c_ispeed;
	dcb->BaudRate        = B_to_CBR( s_termios->c_ispeed );
	return 0;
}

/*----------------------------------------------------------
DCBToTermios()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:     None
   comments:    
----------------------------------------------------------*/

void DCBToTermios( DCB *dcb, struct termios *s_termios )
{
	s_termios->c_ispeed = CBR_to_B( dcb->BaudRate );
	s_termios->c_ospeed = s_termios->c_ispeed;
	s_termios->c_cflag = s_termios->c_ispeed & CBAUD;
}

/*----------------------------------------------------------
show_DCB()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:     None
   comments:    
----------------------------------------------------------*/
void show_DCB( myDCB )
{

#ifdef DEBUG
	printf( "DCBlength: %ld\n", myDCB.DCBlength );
	printf( "BaudRate: %ld\n", myDCB.BaudRate );
	if ( myDCB.fBinary )
		printf( "fBinary\n" );
	if ( myDCB.fParity )
	{
		printf( "fParity: " );
		if ( myDCB.fErrorChar )
			printf( "fErrorChar: %#x\n", myDCB.ErrorChar );
		else
			printf( "fErrorChar == false\n" );
	}
	if ( myDCB.fOutxCtsFlow )
		printf( "fOutxCtsFlow\n" );
	if ( myDCB.fOutxDsrFlow )
		printf( "fOutxDsrFlow\n" );
	if ( myDCB.fDtrControl & DTR_CONTROL_HANDSHAKE );
		printf( "DTR_CONTROL_HANDSHAKE\n" );
	if ( myDCB.fDtrControl & DTR_CONTROL_ENABLE );
		printf( "DTR_CONTROL_ENABLE\n" );
	if ( myDCB.fDtrControl & DTR_CONTROL_DISABLE );
		printf( "DTR_CONTROL_DISABLE\n" );
	if ( myDCB.fDsrSensitivity )
		printf( "fDsrSensitivity\n" );
	if ( myDCB.fTXContinueOnXoff )
		printf( "fTXContinueOnXoff\n" );
	if ( myDCB.fOutX )
		printf( "fOutX\n" );
	if ( myDCB.fInX )
		printf( "fInX\n" );
	if ( myDCB.fNull )
		printf( "fNull\n" );
	if ( myDCB.fRtsControl & RTS_CONTROL_TOGGLE )
		printf( "RTS_CONTROL_TOGGLE\n" );
	if ( myDCB.fRtsControl == 0 )
		printf( "RTS_CONTROL_HANDSHAKE ( fRtsControl==0 )\n" );
	if ( myDCB.fRtsControl & RTS_CONTROL_HANDSHAKE )
		printf( "RTS_CONTROL_HANDSHAKE\n" );
	if ( myDCB.fRtsControl & RTS_CONTROL_ENABLE )
		printf( "RTS_CONTROL_ENABLE\n" );
	if ( myDCB.fRtsControl & RTS_CONTROL_DISABLE )
		printf( "RTS_CONTROL_DISABLE\n" );
	if ( myDCB.fAbortOnError )
		printf( "fAbortOnError\n" );
	printf( "XonLim: %d\n", myDCB.XonLim );
	printf( "XoffLim: %d\n", myDCB.XoffLim );
	printf( "ByteSize: %d\n", myDCB.ByteSize );
	switch ( myDCB.Parity )
	{
		case EVENPARITY:
			printf( "EVENPARITY" );
			break;
		case MARKPARITY:
			printf( "MARKPARITY" );
			break;
		case NOPARITY:
			printf( "NOPARITY" );
			break;
		case ODDPARITY:
			printf( "ODDPARITY" );
			break;
		default:
			printf( "unknown Parity (%#x ):", myDCB.Parity );
			break;
	}
	printf( "\n" );
	switch( myDCB.StopBits )
	{
		case ONESTOPBIT:
			printf( "ONESTOPBIT" );
			break;
		case ONE5STOPBITS:
			printf( "ONE5STOPBITS" );
			break;
		case TWOSTOPBITS:
			printf( "TWOSTOPBITS" );
			break;
		default:
			printf( "unknown StopBits (%#x ):", myDCB.StopBits );
			break;
	}
	printf( "\n" );
	printf( "XonChar: %#x\n", myDCB.XonChar );
	printf( "XoffChar: %#x\n", myDCB.XoffChar );
	printf( "EofChar: %#x\n", myDCB.EofChar );
	printf( "EvtChar: %#x\n", myDCB.EvtChar );
	printf( "\n" );
#endif /* DEBUG */
}

/*----------------------------------------------------------
tcgetattr()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:    GetCommState(), GetCommTimeouts()
   comments:    
----------------------------------------------------------*/

int tcgetattr( int fd, struct termios *s_termios )
{
	DCB myDCB;
	COMMTIMEOUTS timeouts;
	struct termios_list *index;
	int flag;

	ENTER( "tcgetattr" );
	if ( fd <= 0 )
		return 0;
	index = find_port( fd );
	if ( !index )
	{
		fprintf( stderr, "No info known about the port. tcgetattr %i\n",
			fd );
		return -1;
	}
	if ( !GetCommState( index->hComm, &myDCB ) )
	{
		fprintf( stderr, "GetCommState failed\n" );
		return -1;
	}
	memcpy( s_termios, index->ttyset, sizeof( struct termios ) );

	show_DCB( myDCB );

	/***** input mode flags (c_iflag ) ****/
	/* parity check enable */
	if ( myDCB.fParity )
	{
		s_termios->c_iflag |= INPCK;
		s_termios->c_iflag &= ~IGNPAR;
	} else {
		s_termios->c_iflag &= ~INPCK;
		s_termios->c_iflag |= IGNPAR;
	}
	/* FIXME: IGNBRK: ignore break */
	/* FIXME: BRKINT: interrupt on break */
	if ( myDCB.fOutX ) s_termios->c_iflag |= IXON;
	/* IXON: output start/stop control */
	else s_termios->c_iflag &= IXON;
	if ( myDCB.fInX ) s_termios->c_iflag |= IXOFF;
	/* IXOFF: input start/stop control */
	else s_termios->c_iflag &= IXOFF;
	if ( myDCB.fTXContinueOnXoff ) s_termios->c_iflag |= IXANY;
	/* IXANY: any char restarts output */
	else s_termios->c_iflag &= ~IXANY;
	/* FIXME: IMAXBEL: if input buffer full, send bell */

	/***** control mode flags (c_cflag ) *****/
	/* FIXME: CLOCAL: DONT send SIGHUP on modem disconnect */
	/* FIXME: HUPCL: generate modem disconnect when all has closed or
		exited */
	/* CSTOPB two stop bits ( otherwise one) */
	if ( myDCB.StopBits == TWOSTOPBITS ) s_termios->c_cflag |= CSTOPB;	
	if ( myDCB.StopBits == ONESTOPBIT ) s_termios->c_cflag &= ~CSTOPB;	
	/* PARENB enable parity bit */
	if ( myDCB.fParity )
	{
		printf( "setting parity\n" );
		flag = 0;
		s_termios->c_cflag |= PARENB;
		flag |= PARENB;
		if ( myDCB.Parity == ODDPARITY )
		{
			printf( "ODDPARITY\n" );
			flag |= PARODD;
			s_termios->c_cflag |= PARODD;
		}
		if ( myDCB.Parity == EVENPARITY )
		{
			printf( "EVENPARITY\n" );
			flag &= ~PARODD;
			s_termios->c_cflag &= ~PARODD;
		}
	} else
	{
		flag &= ~PARENB;
		s_termios->c_cflag &= ~PARENB;
	}
	/* CSIZE */
	s_termios->c_cflag |= bytesize_to_termios( myDCB.ByteSize );
	/* CTS_OFLOW: cts output flow control */
	if ( myDCB.fOutxCtsFlow == TRUE ) s_termios->c_cflag |= CCTS_OFLOW;
	else s_termios->c_cflag &= ~CCTS_OFLOW;
	/* CRTS_IFLOW: rts input flow control */
	if ( myDCB.fRtsControl == TRUE ) s_termios->c_cflag |= CRTS_IFLOW;
	else s_termios->c_cflag &= ~CRTS_IFLOW;
	/* MDMBUF: carrier based flow control of output */
	/* CIGNORE: tcsetattr will ignore control modes & baudrate */

	/***** NOT SUPPORTED: local mode flags (c_lflag) *****/
	/* ICANON: canonical (not raw) mode */
	/* ECHO: echo back to terminal */
	/* ECHOE: echo erase */
	/* ECHOPRT: hardcopy echo erase */
	/* ECHOK: show KILL char */
	/* ECHOKE: BSD ECHOK */
	/* ECHONL: ICANON only: echo newline even with no ECHO */
	/* ECHOCTL: if ECHO, then control-A are printed as '^A' */
	/* ISIG: recognize INTR, QUIT & SUSP */
	/* IEXTEN: implmentation defined */
	/* NOFLSH: dont clear i/o queues on INTR, QUIT or SUSP */
	/* TOSTOP: background process generate SIGTTOU */
	/* ALTWERASE: alt-w erase distance */
	/* FLUSHO: user DISCARD char */
	/* NOKERNINFO: disable STATUS char */
	/* PENDIN: input line needsd reprinting, set by REPRINT char */
	/***** END - NOT SUPPORTED *****/

	/***** control characters (c_cc[NCCS] ) *****/

	if ( !GetCommTimeouts( index->hComm, &timeouts ) )
	{
		YACK();
		printf( "GetCommTimeouts\n" );
		return -1;
	}

	/*
	s_termios->c_cc[VTIME] = timeouts.ReadTotalTimeoutConstant/100;
	s_termios->c_cc[VMIN] = ( timeouts.ReadTotalTimeoutConstant == 0 ) ? 0 :
		timeouts.ReadIntervalTimeout/timeouts.ReadTotalTimeoutConstant;
	*/

	s_termios->c_cc[VSTART] = myDCB.XonChar;
	s_termios->c_cc[VSTOP] = myDCB.XoffChar;

#ifdef DEBUG
	printf( "tcgetattr: VTIME:%d, VMIN:%d\n", s_termios->c_cc[VTIME],
		s_termios->c_cc[VMIN] );
#endif /* DEBUG */

	/***** line discipline ( c_line ) ( == c_cc[33] ) *****/

	DCBToTermios( &myDCB, s_termios ); /* baudrate */
	LEAVE( "tcgetattr" );
	return 0;
}

/*
	`TCSANOW'
		Make the change immediately.

	`TCSADRAIN'
		Make the change after waiting until all queued output has
		been written.  You should usually use this option when
		changing parameters that affect output.

	`TCSAFLUSH'
		This is like `TCSADRAIN', but also discards any queued input.

	`TCSASOFT'
		This is a flag bit that you can add to any of the above
		alternatives.  Its meaning is to inhibit alteration of the
		state of the terminal hardware.  It is a BSD extension; it is
		only supported on BSD systems and the GNU system.

		Using `TCSASOFT' is exactly the same as setting the `CIGNORE'
		bit in the `c_cflag' member of the structure TERMIOS-P points
		to.  *Note Control Modes::, for a description of `CIGNORE'.
*/

/*----------------------------------------------------------
tcsetattr()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:     GetCommState(), GetCommTimeouts(), SetCommState(),
                 SetCommTimeouts()
   comments:    
----------------------------------------------------------*/
int tcsetattr( int fd, int when, struct termios *s_termios )
{
	int vtime;
	DCB dcb;
	COMMTIMEOUTS timeouts;
	struct termios_list *index;

	ENTER( "tcsetattr" );
	if ( fd <= 0 )
		return 0;
	index = find_port( fd );
	if ( !index )
	{
		fprintf( stderr, "No info known about the port. tcsetattr %i\n", fd );
		return -1;
	}
#ifdef DEBUG
	printf( "tcsetattr: " );
#endif /* DEBUG */
	fflush( stdout );
	if ( s_termios->c_lflag & ICANON )
	{
		fprintf( stderr, "tcsetattr: no canonical mode support\n" );
		/* and all other c_lflags too */
		return -1;
	}
	if ( !GetCommState( index->hComm, &dcb ) )
	{
		YACK();
		printf( "GetCommState\n" );
		return -1;
	}
	if ( !GetCommTimeouts( index->hComm, &timeouts ) )
	{
		YACK();
		printf( "GetCommTimeouts\n" );
		return -1;
	}

	/* FIXME: CLOCAL: DONT send SIGHUP on modem disconnect */
	/* FIXME: HUPCL: generate modem disconnect when all has closed or
			exited */
	/* FIXME: CREAD: without this, data cannot be read */
	/* FIXME: MDMBUF: carrier based flow control of output */

	/*** control flags, c_cflag **/
	if ( !( s_termios->c_cflag & CIGNORE ) )
	{
		/* CIGNORE: ignore control modes and baudrate */
		/* baudrate */
		if ( TermiosToDCB( s_termios, &dcb ) < 0 )
			return -1;
		dcb.ByteSize = termios_to_bytesize( s_termios->c_cflag );
		if ( s_termios->c_cflag & PARENB )
		{
			if ( s_termios->c_cflag & PARODD )
				dcb.Parity = ODDPARITY;
			else
				dcb.Parity = EVENPARITY;
		} else
			dcb.Parity = NOPARITY;
		if ( s_termios->c_cflag & CSTOPB ) dcb.StopBits = TWOSTOPBITS;
			else dcb.StopBits = ONESTOPBIT;
		if ( s_termios->c_cflag & CRTS_IFLOW )
			dcb.fRtsControl = RTS_CONTROL_HANDSHAKE;
		else dcb.fRtsControl = RTS_CONTROL_ENABLE;
		if ( s_termios->c_cflag & CCTS_OFLOW ) dcb.fOutxCtsFlow = TRUE;
		else dcb.fOutxCtsFlow = FALSE;
	}

	/*** input flags, c_iflag **/
	if( ( s_termios->c_iflag & INPCK ) && !( s_termios->c_iflag & IGNPAR ) )
	{
		dcb.fParity = TRUE;
	} else dcb.fParity = FALSE;
	/* not in win95? */
	if ( s_termios->c_iflag & ISTRIP ) dcb.fBinary = FALSE;
	/* ISTRIP: strip to seven bits */
	else dcb.fBinary = TRUE;
	/* FIXME: IGNBRK: ignore break */
	/* FIXME: BRKINT: interrupt on break */
	dcb.fOutX = ( s_termios->c_iflag & IXON ) ? TRUE : FALSE;
	dcb.fInX = ( s_termios->c_iflag & IXOFF ) ? TRUE : FALSE;
	dcb.fTXContinueOnXoff = ( s_termios->c_iflag & IXANY ) ? TRUE : FALSE;
	/* FIXME: IMAXBEL: if input buffer full, send bell */

	/* no DTR control in termios? */
	dcb.fDtrControl     = DTR_CONTROL_ENABLE;
	/* no DSR control in termios? */
	dcb.fOutxDsrFlow    = FALSE;
	/* DONT ignore rx bytes when DSR is OFF */
	dcb.fDsrSensitivity = FALSE;
	dcb.XonChar         = s_termios->c_cc[VSTART];
	dcb.XoffChar        = s_termios->c_cc[VSTOP];
	dcb.XonLim          = 0;	/* ? */
	dcb.XoffLim         = 0;	/* ? */
	if ( EV_BREAK|EV_CTS|EV_DSR|EV_ERR|EV_RING | ( EV_RLSD & EV_RXFLAG ) )
		dcb.EvtChar = '\n';
	else
		dcb.EvtChar = '\0';

	if ( !SetCommState( index->hComm, &dcb ) )
	{
		fprintf( stderr, "SetCommState error\n" );
		YACK();
		return -1;
	}

#ifdef DEBUG
	printf( "VTIME:%d, VMIN:%d\n", s_termios->c_cc[VTIME],
		s_termios->c_cc[VMIN] );
#endif /* DEBUG */
	vtime = s_termios->c_cc[VTIME] * 100;
	timeouts.ReadTotalTimeoutConstant = vtime;
	/* max between bytes */
	timeouts.ReadIntervalTimeout = vtime;
	if ( s_termios->c_cc[VMIN] > 0 && vtime > 0 )
	{
		/* read blocks forever on VMIN chars */
	} else if ( s_termios->c_cc[VMIN] == 0 && vtime == 0 )
	{
		/* read returns immediately */
		timeouts.ReadIntervalTimeout = MAXDWORD;
		timeouts.ReadTotalTimeoutConstant = 0;
		timeouts.ReadTotalTimeoutMultiplier = 0;
	}
#ifdef DEBUG
	printf( "ReadIntervalTimeout=%ld\n", timeouts.ReadIntervalTimeout );
	printf( "c_cc[VTIME] = %d, c_cc[VMIN] = %d\n",
		s_termios->c_cc[VTIME], s_termios->c_cc[VMIN] );
	printf( "ReadTotalTimeoutConstant: %ld\n",
			timeouts.ReadTotalTimeoutConstant );
	printf( "ReadIntervalTimeout : %ld\n", timeouts.ReadIntervalTimeout );
	printf( "ReadTotalTimeoutMultiplier: %ld\n",
		timeouts.ReadTotalTimeoutMultiplier );
#endif /* DEBUG */
	if ( !SetCommTimeouts( index->hComm, &timeouts ) )
	{
		YACK();
		printf( "SetCommTimeouts\n" );
		return -1;
	}
	memcpy( index->ttyset, s_termios, sizeof( struct termios ) );
	LEAVE( "tcsetattr" );
	return 0;
}

/*----------------------------------------------------------
tcsendbreak()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:     None
   comments:    
----------------------------------------------------------*/

int tcsendbreak( int fd, int duration )
{
	/* FIXME send a stream of zero bits for duration */
	return 1;
}

/*----------------------------------------------------------
tcdrain()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:     None
   comments:    
----------------------------------------------------------*/

int tcdrain ( int fd )
{
	/* FIXME block until all queued output has been transmitted */
	return 1;
}

/*----------------------------------------------------------
tcflush()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:     None
   comments:    
----------------------------------------------------------*/

int tcflush( int fd, int queue_selector )
{
	/* FIXME clear input & output queues */
	return 1;
}

/*----------------------------------------------------------
tcflow()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:     None
   comments:   FIXME 
----------------------------------------------------------*/

int tcflow( int fd, int action )
{
	switch ( action )
	{
		/* Suspend transmission of output */
		case TCOOFF: break;
		/* Restart transmission of output */
		case TCOON: break;
		/* Transmit a STOP character */
		case TCIOFF: break;
		/* Transmit a START character */
		case TCION: break;
		default: return -1;
	}
	return 1;
}

/*----------------------------------------------------------
ioctl()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:     GetCommError(), GetCommModemStatus, EscapeCommFunction()
   comments:  FIXME
	the DCB struct is:

	typedef struct _DCB
	{
		DWORD DCBlength, BaudRate, fBinary:1, fParity:1;
		DWORD fOutxCtsFlow:1, fOutxDsrFlow:1, fDtrControl:2;
		DWORD fDsrSensitivity:1, fTXContinueOnXoff:1;
		DWORD fOutX:1, fInX:1, fErrorChar:1, fNull:1;
		DWORD fRtsControl:2, fAbortOnError:1, fDummy2:17;
		WORD wReserved, XonLim, XoffLim;
		BYTE ByteSize, Parity, StopBits;
		char XonChar, XoffChar, ErrorChar, EofChar, EvtChar;
		WORD wReserved1;
	} DCB;

----------------------------------------------------------*/

int ioctl( int fd, int request, ... )
{
	DWORD dwStatus = 0;
	va_list ap;
	int *arg, ret;
	struct serial_struct *sstruct;
	struct async_struct *astruct;
	struct serial_multiport_struct *mstruct;
	DWORD ErrCode;
	COMSTAT Stat;
#ifdef TIOCGICOUNT
	struct serial_icounter_struct *sistruct;
#endif  /* TIOCGICOUNT */
	struct termios_list *index;

	ENTER( "ioctl" );
	if ( fd <= 0 )
		return 0;
	index = find_port( fd );
	if ( !index )
	{
		fprintf( stderr, "No info known about the port. ioctl %i\n", fd );
		return -1;
	}

	va_start( ap, request );
	
	switch( request )
	{
		case TCSBRK:
			arg = va_arg( ap, int * );
			return -ENOIOCTLCMD;;
		case TCSBRKP:
			arg = va_arg( ap, int * );
			return -ENOIOCTLCMD;;
		case TIOCGSOFTCAR:
			arg = va_arg( ap, int * );
			return -ENOIOCTLCMD;;
		case TIOCSSOFTCAR:
			arg = va_arg( ap, int * );
			return -ENOIOCTLCMD;;
		case TIOCMGET:
			arg = va_arg( ap, int * );
			GetCommModemStatus( index->hComm, &dwStatus );
			if ( dwStatus & MS_RLSD_ON ) *arg |= TIOCM_CAR;
			else *arg &= ~TIOCM_CAR;
			if ( dwStatus & MS_RING_ON ) *arg |= TIOCM_RNG;
			else *arg &= ~TIOCM_RNG;
			if ( dwStatus & MS_DSR_ON ) *arg |= TIOCM_DSR;
			else *arg &= ~TIOCM_DSR;
			if ( dwStatus & MS_CTS_ON ) *arg |= TIOCM_CTS;
			else *arg &= ~TIOCM_CTS;

/*			if ( index->dwFunction & SETDTR ) *arg |= TIOCM_DTR;
			if ( index->dwFunction & CLRDTR ) *arg &= ~TIOCM_DTR;
			if ( index->dwFunction & SETRTS ) *arg |= TIOCM_RTS;
			if ( index->dwFunction & CLRRTS ) *arg &= ~TIOCM_RTS;	*/
/*
			TIOCM_LE
			TIOCM_ST
			TIOCM_SR
*/
			break;
		/* TIOCMIS, TIOCMBIC and TIOCMSET all do the same thing... */
		case TIOCMBIS:
			arg = va_arg( ap, int * );
			return -ENOIOCTLCMD;;
		case TIOCMBIC:
			arg = va_arg( ap, int * );
			return -ENOIOCTLCMD;;
		case TIOCMSET:
			arg = va_arg( ap, int * );
			EscapeCommFunction( index->hComm,
				( *arg & TIOCM_DTR ) ? SETDTR : CLRDTR );
			EscapeCommFunction( index->hComm,
				( *arg & TIOCM_RTS ) ? SETRTS : CLRRTS );
			break;
		/* get the serial struct info from the underlying API */
		case TIOCGSERIAL:
			sstruct = va_arg( ap, struct serial_struct * );
			sstruct = index->sstruct;
			printf( "TIOCGSERIAL\n" );
			return 0;
		/* set the serial struct info from the underlying API */
		case TIOCSSERIAL:
			printf( "TIOCSSERIAL\n" );
			index->sstruct = sstruct;
			arg = va_arg( ap, int * );
			return 0;
		case TIOCSERCONFIG:
		case TIOCSERGETLSR:
			arg = va_arg( ap, int * );
			ret = !ClearCommError( index->hComm, &ErrCode, &Stat );
			if ( ret == 0 )
			{
				/* FIXME ? */
				set_errno( EBADFD );
				return -1;
			}
			if ( (int ) Stat.cbOutQue != 0 )
			{
				*arg = 0;
				ret = 0;
			}
			else
			{
				*arg = TIOCSER_TEMP;
				ret = 1;
			}
			break;
		case TIOCSERGSTRUCT:
			astruct = va_arg( ap, struct async_struct * );
			return -ENOIOCTLCMD;;
		case TIOCSERGETMULTI:
			mstruct = va_arg( ap, struct serial_multiport_struct * );
			return -ENOIOCTLCMD;;
		case TIOCSERSETMULTI:
			mstruct = va_arg( ap, struct serial_multiport_struct * );
			return -ENOIOCTLCMD;;
		case TIOCMIWAIT:
			arg = va_arg( ap, int * );
			return -ENOIOCTLCMD;;
		/*
			On linux this fills a struct with all the line info
			(data available, bytes sent, ...
		*/
#ifdef TIOCGICOUNT
		case TIOCGICOUNT:
			sistruct= va_arg( ap, struct  serial_icounter_struct * );
			return -ENOIOCTLCMD;;
		/* abolete ioctls */
#endif /* TIOCGICOUNT */
		case TIOCSERGWILD:
		case TIOCSERSWILD:
			fprintf( stderr, "TIOCSER[GS]WILD absolete\n" );
			return 0;
		/*  number of bytes available for reading */
		case FIONREAD:
			arg = va_arg( ap, int * );
			ret = ClearCommError( index->hComm, &ErrCode, &Stat );
			if ( ret == 0 )
			{
				/* FIXME ? */
				set_errno( EBADFD );
				return -1;
			}
			*arg = ( int ) Stat.cbInQue;
			break;

		/* pending bytes to be sent */
		case TIOCOUTQ:
			arg = va_arg( ap, int * );
			return -ENOIOCTLCMD;;
		default:
			printf( "FIXME:  ioctl: unknown request: %#x\n",
				request );
			return -ENOIOCTLCMD;;
	}
	va_end( ap );
	LEAVE( "ioctl" );
	return 0;
}

/*----------------------------------------------------------
fcntl()

   accept:      
   perform:     
   return:      
   exceptions:  
   win32api:    None
   comments:    FIXME
----------------------------------------------------------*/

int fcntl( int fd, int command, ... )
{
	int arg, ret = 0;
	va_list ap;
	struct termios_list *index;

	ENTER( "fcntl" );
	if ( fd <= 0 )
		return 0;
	index = find_port( fd );
	if ( !index )
	{
		fprintf( stderr, "No info known about the port. fcntl %i\n", fd );
		return -1;
	}

	va_start( ap, command );

	arg = va_arg( ap, int );
	switch ( command )
	{
		case F_SETOWN:	/* set ownership of fd */
			break;
		case F_SETFL:	/* set operating flags */
#ifdef DEBUG
			printf( "F_SETFL fd=%d flags=%d\n", fd, arg );
#endif
			index->flags = arg;
			break;
		case F_GETFL:	/* get operating flags */
			ret = index->flags;
			break;
		default:
			fprintf( stderr, "unknown fcntl command %#x\n", command );
			break;
	}

	va_end( ap );
	LEAVE( "fcntl" );
	return ret;
}

/*----------------------------------------------------------
serial_select()

   accept:      
   perform:     
   return:      number of fd's changed on success or -1 on error.
   exceptions:  
   win32api:    SetCommMask(), GetCommEvent(), WaitSingleObject() 
   comments:    
----------------------------------------------------------*/
#ifndef __LCC__

int  serial_select( int  n,  fd_set  *readfds,  fd_set  *writefds,
			fd_set *exceptfds, struct timeval *timeout )
{

	DWORD dwCommEvent, wait = WAIT_TIMEOUT;
	int fd = n-1;
	struct termios_list *index;

	ENTER( "serial_select" );
	if ( fd <= 0 )
		return 0;
	index = find_port( fd );
	if ( !index )
	{
		fprintf( stderr, "No info known about the port. select %i\n",
			fd );
		return -1;
	}
	if ( !SetCommMask( index->hComm,
		EV_RXCHAR|EV_TXEMPTY|EV_BREAK|EV_CTS|EV_DSR|
		EV_ERR|EV_RING|EV_RLSD|EV_RXFLAG ) )
	{
		YACK();
		goto fail;
	}
	
	if ( !WaitCommEvent( index->hComm, &dwCommEvent, &index->sol ) )
	{
		if ( GetLastError() != ERROR_IO_PENDING )
			goto fail;
	}
	while ( wait == WAIT_TIMEOUT )
	{
		wait = WaitForSingleObject( index->sol.hEvent, 5000 );
		//wait = WaitForSingleObject( index->rol.hEvent, 500 );
#ifdef DEBUG
		switch ( wait )
		{
			case WAIT_OBJECT_0:
				printf( "Status Object\n" );
				break;
			case WAIT_OBJECT_0 + 1:
				printf( "Read Object\n" );
				break;
			case WAIT_OBJECT_0 + 2:
				printf( "Write Object\n" );
				break;
		}
#endif /* DEBUG */
	}
	LEAVE( "serial_select" );
	return( 1 );
fail:

	printf( "< select called error %i\n", n );
	set_errno( EBADFD );
	return( -1 );
	
}

/*----------------------------------------------------------
Report()

   accept:      
   perform:     
   return:      number of fd's changed on success or -1 on error.
   exceptions:  
   win32api:    none
   comments:    
----------------------------------------------------------*/

void Report( char *msg )
{
#ifdef DEBUG
	fprintf( stderr, msg );
#endif /* DEBUG */
}


/*----------------------- END OF LIBRARY -----------------*/


#define O_RDONLY 00
#define PORT_SERIAL 1
/*
	NT Port Enumeration.

	The basic idea is to open a port and try to read a byte from it.
	The The read should time out and return 0.

	The hardware setup is:
	
	COM1 valid
	COM2 valid
	COM3 invalid
	COM4 invalid

	If only COM3 is opened a valid reference for the port is obtained.
	read times out.  The port looks valid.

	if COM1 is opened the open on COM3 fails but the open on COM4 works.
	if COM2 is opened the open on COM4 fails but the open on COM3 works.

	The registry would be another approach.  The registry on NT shows
	COM1-4 in the above configuration.


*/
#ifdef asdf
int main( int argc, char *argv[] )
{
	struct termios ttyset;
	int fd =open( "COM1" , O_RDONLY | CLOCAL);
	int cspeed = translate_speed( env, speed );
	if( !cspeed )
	{
		fprintf( stderr, "Invalid Speed Selected\n");
		return;
	}
	if( tcgetattr( fd, &ttyset ) < 0 )
	{
		fprintf( stderr, "Cannot Get Serial Port Settings\n");
		goto fail;
	}
	printf( "Set: %i\n", ttyset.c_cflag);
					/* FIXME */
	if( !translate_data_bits( env, &( ttyset.c_cflag), dataBits ) )
	{
		fprintf( stderr, "Invalid Data Bits Selected\n");
		return;
	}
	if( !translate_stop_bits( env, &( ttyset.c_cflag), stopBits ) )
	{
		fprintf( stderr, "Invalid Stop Bits Selected\n");
		return;
	}
	if( !translate_parity( env, &( ttyset.c_cflag), parity ) )
	{
		fprintf( stderr, "Invalid Parity Selected\n");
		return;
	}
#ifdef __FreeBSD__
	if( cfsetspeed( &ttyset, cspeed ) < 0 )
	{
		fprintf( stderr, "Cannot Set Speed\n");
		goto fail;
	}
#else
	if( cfsetispeed( &ttyset, cspeed ) < 0 )
	{
		fprintf( stderr, "Cannot Set Input Speed\n");
		goto fail;
	}
	if( cfsetospeed( &ttyset, cspeed ) < 0 )
	{
		fprintf( stderr, "Cannot Set Output Speed\n");
		goto fail;
	}
#endif  /* __FreeBSD__ */
	if( tcsetattr( fd, TCSANOW, &ttyset ) < 0 )
	{
		fprintf( stderr, "Cannot Set Serial Port Parameters.\n");
		goto fail;
	}
	return;

fail:
	throw_java_exception( env, UNSUPPORTED_COMM_OPERATION,
		"nativeSetSerialPortParams", strerror( errno ) );
}
#endif
int main( int argc, char *argv[] )
{
	struct termios ttyset;
	char c;
	int fd[4];
	//char *name = "COM3";
	int ret = 1;
	int port_type = 1;

	/* CLOCAL eliminates open blocking on modem status lines */
	printf("trying testRead()\n");
	if ((fd[3] = open("COM1" , O_RDONLY | CLOCAL)) < 0) {
		ret = 0;
		goto END;
	}
/*
	if ((fd[1] = open("COM2", O_RDONLY | CLOCAL)) < 0) {
		ret = 0;
		goto END;
	}
	if ((fd[3] = open("COM4", O_RDONLY | CLOCAL)) < 0) {
		ret = 0;
		goto END;
	}
	ret = write(fd[3],"test",4);
	if ( ret < 0 )
	{
		ret = 0;
		goto END;
	}
	ret = read(fd[3],name,1);
	if ( ret < 0 )
	{
		ret = 0;
		goto END;
	}
	ret = read(fd[3],name,4);
	if ( ret < 0 )
	{
		ret = 0;
		goto END;
	}
	if ((fd[2] = open("COM3", O_RDONLY | CLOCAL)) < 0) {
		ret = 0;
		goto END;
	}
*/

	if ( port_type == PORT_SERIAL )
	{
		int saved_flags;
		struct termios saved_termios;

		if (tcgetattr(fd[3], &ttyset) < 0) {
			ret = 0;
			goto END;
		}

		/* save, restore later */
		if ((saved_flags = fcntl(fd[3], F_GETFL)) < 0) {
			ret = 0;
			goto END;
		}

		memcpy(&saved_termios, &ttyset, sizeof(struct termios));

		if (fcntl(fd[3], F_SETFL, O_NONBLOCK) < 0) {
			ret = 0;
			goto END;
		}

		cfmakeraw(&ttyset);
		ttyset.c_cc[VMIN] = ttyset.c_cc[VTIME] = 0;

		if (tcsetattr(fd[3], TCSANOW, &ttyset) < 0) {
			ret = 0;
			tcsetattr(fd[3], TCSANOW, &saved_termios);
			goto END;
		}
		if (tcgetattr(fd[3], &ttyset) < 0) {
			ret = 0;
			goto END;
		}
		if (tcsetattr(fd[3], TCSANOW, &ttyset) < 0) {
			ret = 0;
			tcsetattr(fd[3], TCSANOW, &saved_termios);
			goto END;
		}
		if (tcgetattr(fd[3], &ttyset) < 0) {
			ret = 0;
			goto END;
		}
		if (read(fd[3], &c, 1) < 0)
		{
#ifdef EWOULDBLOCK
			if ( errno != EWOULDBLOCK )
			{
				ret = 0;
			}
#else
			ret = 0;
#endif /* EWOULDBLOCK */
		}

		/* dont walk over unlocked open devices */
		tcsetattr(fd[3], TCSANOW, &saved_termios);
		fcntl(fd[3], F_SETFL, saved_flags);
	}
END:
	close(fd[3]);
	return ret;
}
#ifdef asdf
int main( int argc, char *argv[] )
{
	struct termios ttyset;
	int fd = open("COM1", 1);
	//int cspeed = translate_speed( env, speed );
	int cspeed = B9600;
	for(;;)
	{
		if( !cspeed ) return;
		if( tcgetattr( fd, &ttyset ) < 0 ) goto fail;
		ttyset.c_cflag &= ~CSIZE;
		ttyset.c_cflag |= CS8;
		ttyset.c_cflag |= PARENB;
		ttyset.c_cflag &= ~CSTOPB;
#ifdef __FreeBSD__
		if( cfsetspeed( &ttyset, cspeed ) < 0 ) goto fail;
#else
		if( cfsetispeed( &ttyset, cspeed ) < 0 ) goto fail;
		if( cfsetospeed( &ttyset, cspeed ) < 0 ) goto fail;
#endif  /* __FreeBSD__ */
		if( tcsetattr( fd, TCSANOW, &ttyset ) < 0 ) goto fail;
		printf(".");
	}
fail:
	return;
}

int main( int argc, char *argv[] )
{
	int fd[4] = { 0,0,0,0 };
	int res, seed, foo;
	char file[8], vb[80];
	int printflag = 0;

	fd_set  *readfds;
	fd_set  *writefds;
	fd_set *exceptfds;
	struct timeval *timeout;
	int Fd, ret, change;
	fd_set rfds;
	struct timeval tv_sleep;
	unsigned int mflags, omflags;
#undef TIOCSERGETLSR
#if defined TIOCSERGETLSR
	struct stat fstatbuf;
#endif /* TIOCSERGETLSR */

#if defined(TIOCGICOUNT)
	struct serial_icounter_struct sis, osis;
	/* JK00: flag if this can be used on this port */
	int has_tiocgicount = 1;
#endif /* TIOCGICOUNT */

#if defined(TIOCSERGETLSR)
	int has_tiocsergetlsr = 1;
#endif /* TIOCSERGETLSR */


	Fd = open( "COM1", 1 );
#if defined(TIOCGICOUNT)
	/* Some multiport serial cards do not implement TIOCGICOUNT ... */
	/* So use the 'dumb' mode to enable using them after all! JK00 */
	if( ioctl( Fd, TIOCGICOUNT, &osis ) < 0 )
	{
		Report("Port does not support TIOCGICOUNT events\n" );
		has_tiocgicount = 0;
	}
#endif /*  TIOCGICOUNT */

#if defined(TIOCSERGETLSR)
	/* JK00: work around for multiport cards without TIOCSERGETLSR */
	/* Cyclades is one of those :-(				       */
	if( ioctl( Fd, TIOCSERGETLSR, &change ) )
	{
		Report("Port does not support TIOCSERGETLSR\n" );
			has_tiocsergetlsr = 0;
	}
#endif /* TIOCSERGETLSR */

	if( ioctl( Fd, TIOCMGET, &omflags) <0 )
	{
		Report("Port does not support events\n" );
		return;
	}

	FD_ZERO( &rfds );
	while( 1 )
	{
		printf(".");
		FD_SET( Fd, &rfds );
		tv_sleep.tv_sec = 0;
		tv_sleep.tv_usec = 100000;
		do {
			ret=serial_select( Fd + 1, &rfds, NULL, NULL, &tv_sleep );
		}  while (ret < 0 && errno==EINTR);
		if( ret < 0 ) break;

#if defined TIOCSERGETLSR
		/* JK00: work around for Multi IO cards without TIOCSERGETLSR */
		if( has_tiocsergetlsr )
		{
			if (fstat(Fd, &fstatbuf))  break;
			if( ioctl( Fd, TIOCSERGETLSR, &change ) ) break;
			else if( change )
			{
			/*	send_event( env, jobj, SPE_OUTPUT_BUFFER_EMPTY,
					1 );
			*/
			}
		}
#endif /* TIOCSERGETLSR */
#if defined(TIOCGICOUNT)
	/*	wait for RNG, DSR, CD or CTS  but not DataAvailable
	 *      The drawback here is it never times out so if someone
	 *      reads there will be no chance to try again.
	 *      This may make sense if the program does not want to
	 *      be notified of data available or errors.
	 *	ret=ioctl(Fd,TIOCMIWAIT);
	 */
		/* JK00: only use it if supported by this port */
		if (has_tiocgicount)
		{
			if( ioctl( Fd, TIOCGICOUNT, &sis ) ) break;
			while( sis.frame != osis.frame )
			{
				//send_event( env, jobj, SPE_FE, 1);
				osis.frame++;
			}
			while( sis.overrun != osis.overrun )
			{
				//send_event( env, jobj, SPE_OE, 1);
				osis.overrun++;
			}
			while( sis.parity != osis.parity )
			{
				//send_event( env, jobj, SPE_PE, 1);
				osis.parity++;
			}
			while( sis.brk != osis.brk )
			{
				//send_event( env, jobj, SPE_BI, 1);
				osis.brk++;
			}
			osis = sis;
		}
#endif /*  TIOCGICOUNT */
	       /* A Portable implementation */

		if( ioctl( Fd, TIOCMGET, &mflags ) ) break;

		change = (mflags&TIOCM_CTS) - (omflags&TIOCM_CTS);
		//if( change ) send_event( env, jobj, SPE_CTS, change );

		change = (mflags&TIOCM_DSR) - (omflags&TIOCM_DSR);
		//if( change ) send_event( env, jobj, SPE_DSR, change );

		//change = (mflags&TIOCM_RNG) - (omflags&TIOCM_RNG);
		//if( change ) send_event( env, jobj, SPE_RI, change );

		change = (mflags&TIOCM_CD) - (omflags&TIOCM_CD);
		//if( change ) send_event( env, jobj, SPE_CD, change );

		omflags = mflags;

		ioctl( Fd, FIONREAD, &change );
		if( change )
		{
			//if(!send_event( env, jobj, SPE_DATA_AVAILABLE, 1 ))
			{
				usleep(100000); /* select wont block */
			}
		}
	}
#ifdef asdf

	res = open( "COM1", 1 );
	seed = open( "COM2", 1 );
	if ( res >= 0 )
	{
		strcpy(vb,"test");
		for(foo= 0;;foo++)
		{
			if( foo%100 == 0 ) printf("%i\n", foo);
			serial_select(res + 1, readfds, writefds, exceptfds, timeout);
			serial_select(seed + 1, readfds, writefds, exceptfds, timeout);
			serial_write(res, vb, 4);
			serial_write(seed, vb, 4);
		}
	}
#endif/* asdf */
#ifdef asdf
	res = open( "COM1", 1 );
	seed = open( "COM2", 1 );
	if ( res <= 0 )
	{
		strcpy(vb,"test");
		for(foo= 0;;foo++)
		{
			if( foo%100 == 0 ) printf("%i\n", foo);
			serial_write(seed, vb, 4);
			serial_read(res, file, 4);
		}
	}
	else
	{
		strcpy(vb,"test");
		for(foo= 0;;foo++)
		{
			if( foo%100 == 0 ) printf("%i\n", foo);
			serial_write(seed, vb, 4);
			serial_read(res, file,  4);
		}
	}
#endif/* asdf */
#ifdef asdf
	for(;;)
	{
		res = open( "COM1", 1 );
		if ( res <= 0 )
		{
			printf("Open Failed\n");
		}
		else
		{
			serial_read(res, vb,  1);
			res = close(res);
			if ( res != 0 )
				printf("close Failed\n");
		}
		res = open( "COM2", 1 );
		if ( res <= 0 )
		{
			printf("Open Failed\n");
		}
		else
		{
			serial_read(res, vb,  1);
			res = close(res);
			if ( res != 0 )
				printf("close Failed\n");
		}
		res = open( "COM3", 1 );
		if ( res <= 0 )
		{
			printf("Open Failed\n");
		}
		else
		{
			serial_read(res, vb,  1);
			res = close(res);
			if ( res != 0 )
				printf("close Failed\n");
		}
		res = open( "COM4", 1 );
		if ( res <= 0 )
		{
			printf("Open Failed\n");
		}
		else
		{
			serial_read(res, vb,  1);
			res = close(res);
			if ( res != 0 )
				printf("close Failed\n");
		}
		printf(".");
	}
#endif /* asdf */
#ifdef asdf
	for(;;)
	{

		seed = (int) (4.0*rand()/RAND_MAX + 1.0);
		foo  = (int) (2*rand()/RAND_MAX + 1.0);
		res = -1;

		if( foo == 1 )
		{
			if ( fd[ seed - 1] == 0 )
			{
				sprintf(file, "COM%i",
					seed );
				res = open( file, 1 );
				if(res > 0 )
				{
					fd[ seed - 1] = res;
				}
				res = -1;
				//serial_read(res, vb,  1);
				printflag = 1;
			}
		}
		else
		{
			if ( fd[ seed - 1] != 0 )
			{
				res = close ( fd[ seed - 1 ] );
				if (res != -1)
					fd[ seed - 1 ] = 0;
				res = -1;
				printflag = 1;
			}
		}
		if ( printflag )
		{
			printf("\n%5i %5i %5i %5i",fd[0],fd[1],fd[2],fd[3] );
			printflag = 0;
		}
		//usleep( 10000 );
	}
#endif /* asdf */
}
#endif /* asdf */
#endif
