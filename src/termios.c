/* read TODO! */
/*-------------------------------------------------------------------------
|   rxtx is a native interface to serial ports in java.
|   Copyright 1997-2000 by Trent Jarvi trentjarvi@yahoo.com.
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
#include "malloc.h"

#define SIZE 64	/* number of serial ports */
int my_errno;
struct termios_list {
	char *filename;
	int my_errno;
	HANDLE hComm;
	struct termios ttyset;
};
struct termios_list *tl[SIZE];

void set_errno(int error) {
  my_errno = error;
}

void usleep(unsigned long usec)
{
	sleep(1);
}
int CBR_to_B(int Baud) {
	switch (Baud) {
		case 75:		return(B75);
		case CBR_110:		return(B110);
		case 134:		return(B134);
		case 150:		return(B150);
		case CBR_300:		return(B300);
		case CBR_600:		return(B600);
		case CBR_1200:		return(B1200);
		case 1800:		return(B1800);
		case CBR_2400:		return(B2400);
		case CBR_4800:		return(B4800);
		case CBR_9600:		return(B9600);
		case CBR_19200:		return(B19200);
		case CBR_38400:		return(B38400);
		case CBR_115200:	return(B115200);
		case CBR_57600:		return(B57600);
		default:
			set_errno(EINVAL);
			return -1;
	}
}

int B_to_CBR(int Baud) {
	int ret;
	int br;
	switch (Baud) {
		case B75:	br = 75;	ret = 75;		break;
		case B110:	br = 110;	ret = CBR_110;		break;
		case B134:	br = 134;	ret = 134;		break;
		case B150:	br = 150;	ret = 150;		break;
		case B300:	br = 300;	ret = CBR_300;		break;
		case B600:	br = 600;	ret = CBR_600;		break;
		case B1200:	br = 1200;	ret = CBR_1200;		break;
		case B1800:	br = 1800;	ret = 1800;		break;
		case B2400:	br = 2400;	ret = CBR_2400;		break;
		case B4800:	br = 4800;	ret = CBR_4800;		break;
		case B9600:	br = 9600;	ret = CBR_9600;		break;
		case B19200:	br = 19200;	ret = CBR_19200;	break;
		case B38400:	br = 38400;	ret = CBR_38400;	break;
		case B115200:	br = 115200;	ret = CBR_115200;	break;
		case B57600:	br = 57600;	ret = CBR_57600;	break;
		default:
			fprintf(stderr, "B_to_CBR: invalid baudrate: %#o\n", Baud);
			set_errno(EINVAL);
			return -1;
	}
//	printf("[B_to_CBR: %d]\n", br);
	return ret;
}

int bytesize_to_termios(int ByteSize){
   switch (ByteSize) {
      case 5: return(CS5);
      case 6: return(CS6);
      case 7: return(CS7);
      case 8:
      default: return(CS8);
   }
}

int termios_to_bytesize(int cflag) {
   switch (cflag) {
      case CS5: return(5);
      case CS6: return(6);
      case CS7: return(7);
      case CS8:
      default: return(8);
   }
}
/*
const char *get_dos_port(char const *name){
   if (!strcmp(name, "/dev/cua0")) return("COM1");
   if (!strcmp(name, "/dev/cua1")) return("COM2");
   if (!strcmp(name, "/dev/cua2")) return("COM3");
   if (!strcmp(name, "/dev/cua3")) return("COM4");
   return((const char *) name);
}	*/

static BOOL ClearError(HANDLE hComPort) {
    COMSTAT Stat;
    DWORD ErrCode;
    return ClearCommError(hComPort, &ErrCode, &Stat);
}

/*
BOOL FillDCB(DCB *dcb) {

    dcb->DCBlength = sizeof(dcb);
    if (!GetCommState(hCommPort, &dcb)) {
       printf("GetCommState\n");
       return(-1);
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
    if (EV_BREAK|EV_CTS|EV_DSR|EV_ERR|EV_RING|(EV_RLSD & EV_RXFLAG)) dcb.EvtChar = '\n';      
    else dcb.EvtChar = '\0';
    if (!SetCommState(hCommPort, &dcb)) {
       printf("SetCommState\n");
       return(-1);
    }
    if (!SetCommTimeouts(hCommPort, &Timeout)) {
       printf("SetCommTimeouts\n");
       return(-1);
    }
    return ( TRUE ) ;
} */

int close(int fd) {
	return CloseHandle(tl[fd]->hComm);
}

void cfmakeraw(struct termios *s_termios) {
	s_termios->c_iflag &= ~(IGNBRK|BRKINT|PARMRK|ISTRIP
								|INLCR|IGNCR|ICRNL|IXON);
	s_termios->c_oflag &= ~OPOST;
	s_termios->c_lflag &= ~(ECHO|ECHONL|ICANON|ISIG|IEXTEN);
	s_termios->c_cflag &= ~(CSIZE|PARENB);
	s_termios->c_cflag |= CS8;
}

BOOL init_termios(struct termios *ttyset) {
	if (!ttyset)
		return FALSE;
	memset(ttyset, 0, sizeof(struct termios));
	cfsetospeed(ttyset, B9600);
	cfmakeraw(ttyset);
	ttyset->c_cc[VINTR] = 0x03;	/* 0: C-c */
	ttyset->c_cc[VQUIT] = 0x1c;	/* 1: C-\ */
	ttyset->c_cc[VERASE] = 0x7f;	/* 2: <del> */
	ttyset->c_cc[VKILL] = 0x15;	/* 3: C-u */
	ttyset->c_cc[VEOF] = 0x04;	/* 4: C-d */
	ttyset->c_cc[VTIME] = 0;	/* 5: read timeout */
	ttyset->c_cc[VMIN] = 1;		/* 6: read returns after this many bytes */
	ttyset->c_cc[VSUSP] = 0x1a;	/* 10: C-z */
	ttyset->c_cc[VEOL] = '\r';	/* 11: */
	ttyset->c_cc[VREPRINT] = 0x12;	/* 12: C-r */
//	ttyset->c_cc[VDISCARD] = 0x;	/* 13: IEXTEN only */
	ttyset->c_cc[VWERASE] = 0x17;	/* 14: C-w */
	ttyset->c_cc[VLNEXT] = 0x16;	/* 15: C-w */
	ttyset->c_cc[VEOL2] = '\n';	/* 16: */
	return TRUE;
	/* default VTIME = 0, VMIN = 1: read blocks forever until one byte */
}

int serial_open(const char *filename, int flags) {
	int fd;
	COMMPROP cp;
	DCB	dcb;
	for (fd = 0; fd<SIZE; fd++) {
		if (!tl[fd]) continue;
		if (!strcmp(filename, tl[fd]->filename)) {
			printf("open: %s already open\n", filename);
			return -1;
		}
	}

	for (fd = 0; fd<SIZE; fd++) /* find next free */
		if (!tl[fd]) break;

//	printf("fd: %d\n", fd);
	if (fd == SIZE) {
		printf("open: no more free ports");
		return -1;
	}
	tl[fd] = malloc(sizeof(struct termios_list));

	if (!tl[fd]) {
		printf("open: out of memory\n");
		return -1;
	}
	tl[fd]->filename = strdup(filename);
	tl[fd]->hComm = CreateFile(filename,
		GENERIC_READ | GENERIC_WRITE,
		0,
		0,
		OPEN_EXISTING,
		0,	//		FILE_FLAG_OVERLAPPED,
		0);
	if (tl[fd]->hComm == INVALID_HANDLE_VALUE) {
		printf("cannot open %s\n", filename);
		free(tl[fd]);
		return -1;
	}
	SetupComm(tl[fd]->hComm, 2048, 1024);
	GetCommProperties(tl[fd]->hComm, &cp);
	/* check for capabilities */
	if (!(cp.dwProvCapabilities & PCF_DTRDSR))
		printf("%s: no DTR & DSR support\n", filename);
	if (!(cp.dwProvCapabilities & PCF_RLSD))
		printf("%s: no carrier detect (RLSD) support\n", filename);
	if (!(cp.dwProvCapabilities & PCF_RTSCTS))
		printf("%s: no RTS & CTS support\n", filename);
	if (!(cp.dwProvCapabilities & PCF_TOTALTIMEOUTS))
		printf("%s: no timeout support\n", filename);
	if (!GetCommState(tl[fd]->hComm, &dcb)) {
       printf("GetCommState\n");
       return -1;
	}
	init_termios(&tl[fd]->ttyset);
	tcsetattr(fd, 0, &tl[fd]->ttyset);	/* set default condition */
	printf("open: ospeed: %#o\n", cfgetospeed(&tl[fd]->ttyset));
	
	return fd;
}

int serial_write(int fd, char *Str, int length) {
	DWORD nBytes;
    /***** output mode flags (c_oflag) *****/
	/* FIXME: OPOST: enable ONLCR, OXTABS & ONOEOT */
	/* FIXME: ONLCR: convert newline char to CR & LF */
	/* FIXME: OXTABS: convert tabs to spaces */
	/* FIXME: ONOEOT: discard ^D (004) */
	if (!WriteFile(tl[fd]->hComm, Str, length, &nBytes, NULL)) {
		ClearError(tl[fd]->hComm);
		nBytes=-1;
	}
	FlushFileBuffers(tl[fd]->hComm);
	return nBytes;
}

int serial_read(int fd, void *vb, int size) {
	/* FIXME: CREAD: without this, data cannot be read */

	/* FIXME: PARMRK: mark framing & parity errors */
	/* FIXME: IGNCR: ignore \r */
	/* FIXME: ICRNL: convert \r to \n */
	/* FIXME: INLCR: convert \n to \r */
	DWORD nBytes = 0, total = 0;
	int err;
	int vmin = tl[fd]->ttyset.c_cc[VMIN];
	char *b = (char *)vb;
	while (nBytes <= vmin && size > 0) {
		if (!ReadFile(tl[fd]->hComm, b, size, &nBytes, NULL)) {
			err = GetLastError();
			switch (err) {
				case ERROR_BROKEN_PIPE:
					nBytes = 0;
					break;
				case ERROR_MORE_DATA:
					break;
				default:
					return -1;
			}
		}
		if (vmin == 0) break;
		size -= nBytes;
		b += nBytes;
		total += nBytes;
	}
	return total;
}  

int cfsetospeed(struct termios *s_termios, speed_t speed) {
	if (speed & ~CBAUD) {
		fprintf(stderr, "cfsetospeed: not speed: %#o\n", speed);
		return 0;
	}
    s_termios->c_ispeed = s_termios->c_ospeed = speed;
	s_termios->c_cflag &= ~CBAUD;	/* clear baudrate */
	s_termios->c_cflag |= speed;
	return 1;
}

int cfsetispeed(struct termios *s_termios, speed_t speed) {
	return cfsetospeed(s_termios, speed);
}

int cfsetspeed(struct termios *s_termios, speed_t speed) {
	return cfsetospeed(s_termios, speed);
}

speed_t cfgetospeed(struct termios *s_termios) {
    return s_termios->c_ospeed;
}

speed_t cfgetispeed(struct termios *s_termios) {
    return s_termios->c_ispeed;
}

int TermiosToDCB(struct termios *s_termios, DCB *dcb) {
	s_termios->c_ispeed = s_termios->c_cflag & CBAUD;
	s_termios->c_ospeed = s_termios->c_ispeed;
    dcb->BaudRate        = B_to_CBR(s_termios->c_ispeed);
	return 0;
}

void DCBToTermios(DCB *dcb, struct termios *s_termios) {
    s_termios->c_ispeed = CBR_to_B(dcb->BaudRate);
    s_termios->c_ospeed = s_termios->c_ispeed;
	s_termios->c_cflag = s_termios->c_ispeed & CBAUD;
}

int tcgetattr(int fd, struct termios *s_termios) {
	DCB myDCB;
	COMMTIMEOUTS timeouts;
//	printf("tcgetattr:\n");
	if (!GetCommState(tl[fd]->hComm, &myDCB)) {
		printf("GetCommState failed\n");
		return -1;
	}
	memcpy(s_termios, &tl[fd]->ttyset, sizeof(struct termios));
#if 0
	printf("DCBlength: %ld\n", myDCB.DCBlength);
	printf("BaudRate: %ld\n", myDCB.BaudRate);
	if (myDCB.fBinary) printf("fBinary\n");
	if (myDCB.fParity) {	/* parity checking */
		printf("fParity: ");
		if (myDCB.fErrorChar)	/* if ErrorChar is used replace */
			printf("fErrorChar: %#x\n", myDCB.ErrorChar);
		else
			printf("fErrorChar == false\n");
	}
	if (myDCB.fOutxCtsFlow)
		printf("fOutxCtsFlow\n"); /* if CTS off, then suspend transmitting */
	if (myDCB.fOutxDsrFlow)
		printf("fOutxDsrFlow\n"); /* if DSR off, then suspend transmitting */
	if (myDCB.fDtrControl & DTR_CONTROL_HANDSHAKE);
		printf("DTR_CONTROL_HANDSHAKE\n");	/* enable DTR handshaking */
	if (myDCB.fDtrControl & DTR_CONTROL_ENABLE);
		printf("DTR_CONTROL_ENABLE\n");		/* turns DTR on */
	if (myDCB.fDtrControl & DTR_CONTROL_DISABLE);
		printf("DTR_CONTROL_DISABLE\n");	/* turns DTR off */
	if (myDCB.fDsrSensitivity)
		printf("fDsrSensitivity\n");	/* ignore bytes when DSR is off */
	if (myDCB.fTXContinueOnXoff)	/* tx stops when rx buffer full */
		printf("fTXContinueOnXoff\n");	/* and driver sent XoffChar */
	if (myDCB.fOutX)		/* tx stops when XoffChar is received */
		printf("fOutX\n");	/* XonChar restarts tx */
	if (myDCB.fInX)		/* send XoffChar when rx buffer fills to XoffLim */
		printf("fInX\n"); /* and send XonChar when buffer empties to XonLim */
	if (myDCB.fNull) 
		printf("fNull\n");	/* zero valued bytes on input are discarded */
	if (myDCB.fRtsControl & RTS_CONTROL_TOGGLE)
		/* RTS on when three bytes in rx buffer, otherwise off */
		printf("RTS_CONTROL_TOGGLE\n");
	if (myDCB.fRtsControl == 0)
		printf("RTS_CONTROL_HANDSHAKE (fRtsControl==0)\n");
	if (myDCB.fRtsControl & RTS_CONTROL_HANDSHAKE)
		/* RTS on when rx buffer less than half full */
		/* RTS off when rx buffer more than 75% full */
		printf("RTS_CONTROL_HANDSHAKE\n");
	if (myDCB.fRtsControl & RTS_CONTROL_ENABLE)
		printf("RTS_CONTROL_ENABLE\n");		/* RTS is turned on */
	if (myDCB.fRtsControl & RTS_CONTROL_DISABLE)
		printf("RTS_CONTROL_DISABLE\n");	/* RTS is turned off */
	if (myDCB.fAbortOnError)
		printf("fAbortOnError\n"); /* read & write aborts on error */

	/* send XON when rx buffer fills to this */
	printf("XonLim: %d\n", myDCB.XonLim);

	/* send XOFF when rx buffer empties to this */
	printf("XoffLim: %d\n", myDCB.XoffLim);
	printf("ByteSize: %d\n", myDCB.ByteSize);	/* data bits */
	switch (myDCB.Parity) {
		case EVENPARITY: printf("EVENPARITY"); break;
		case MARKPARITY: printf("MARKPARITY"); break;
		case NOPARITY: printf("NOPARITY"); break;
		case ODDPARITY: printf("ODDPARITY"); break;
		default: printf("unknown Parity (%#x):", myDCB.Parity); break;
	}
	printf("\n");
	switch(myDCB.StopBits) {
		case ONESTOPBIT: printf("ONESTOPBIT"); break;
		case ONE5STOPBITS: printf("ONE5STOPBITS"); break;
		case TWOSTOPBITS: printf("TWOSTOPBITS"); break;
		default: printf("unknown StopBits (%#x):", myDCB.StopBits); break;
	}
	printf("\n");
	printf("XonChar: %#x\n", myDCB.XonChar);
	printf("XoffChar: %#x\n", myDCB.XoffChar);
	printf("EofChar: %#x\n", myDCB.EofChar);
	printf("EvtChar: %#x\n", myDCB.EvtChar);
	printf("\n");
#endif

    /***** input mode flags (c_iflag) ****/
	if (myDCB.fParity) {	/* parity check enable */
		s_termios->c_iflag |= INPCK;
		s_termios->c_iflag &= ~IGNPAR; 
	} else {
		s_termios->c_iflag &= ~INPCK;
		s_termios->c_iflag |= IGNPAR; 
	}
	/* FIXME: IGNBRK: ignore break */
	/* FIXME: BRKINT: interrupt on break */
	if (myDCB.fOutX) s_termios->c_iflag |= IXON;
	else s_termios->c_iflag &= IXON;	/* IXON: output start/stop control */
	if (myDCB.fInX) s_termios->c_iflag |= IXOFF;
	else s_termios->c_iflag &= IXOFF;	/* IXOFF: input start/stop control */
    if (myDCB.fTXContinueOnXoff) s_termios->c_iflag |= IXANY;
    else s_termios->c_iflag &= ~IXANY;/* IXANY: any char restarts output */
	/* FIXME: IMAXBEL: if input buffer full, send bell */

    /***** control mode flags (c_cflag) *****/
	/* FIXME: CLOCAL: DONT send SIGHUP on modem disconnect */
	/* FIXME: HUPCL: generate modem disconnect when all has closed or exited */
	/* CSTOPB two stop bits (otherwise one) */
	if (myDCB.StopBits == TWOSTOPBITS) s_termios->c_cflag |= CSTOPB;	
	if (myDCB.StopBits == ONESTOPBIT) s_termios->c_cflag &= ~CSTOPB;	
	if (myDCB.fParity) {	/* PARENB enable parity bit */
		s_termios->c_cflag |= PARENB;
		/* PARODD use odd parity (otherwise even) */
		if (myDCB.Parity == ODDPARITY) s_termios->c_cflag |= PARODD;
		if (myDCB.Parity == EVENPARITY) s_termios->c_cflag &= ~PARODD;
	} else s_termios->c_cflag &= ~PARENB;
	s_termios->c_cflag |= bytesize_to_termios(myDCB.ByteSize); /* CSIZE */
	/* CTS_OFLOW: cts output flow control */
	if (myDCB.fOutxCtsFlow == TRUE) s_termios->c_cflag |= CCTS_OFLOW;
	else s_termios->c_cflag &= ~CCTS_OFLOW;
	/* CRTS_IFLOW: rts input flow control */
	if (myDCB.fRtsControl == TRUE) s_termios->c_cflag |= CRTS_IFLOW;
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

    /***** control characters (c_cc[NCCS]) *****/

	if (!GetCommTimeouts(tl[fd]->hComm, &timeouts)) {
       printf("GetCommTimeouts\n");
       return -1;
	}
/*	s_termios->c_cc[VTIME] = timeouts.ReadTotalTimeoutConstant/100;
	s_termios->c_cc[VMIN] = (timeouts.ReadTotalTimeoutConstant == 0) ? 0 :
		timeouts.ReadIntervalTimeout/timeouts.ReadTotalTimeoutConstant;	*/
	s_termios->c_cc[VSTART] = myDCB.XonChar;
	s_termios->c_cc[VSTOP] = myDCB.XoffChar;

//	printf("tcgetattr: VTIME:%d, VMIN:%d\n", s_termios->c_cc[VTIME], s_termios->c_cc[VMIN]);

    /***** line discipline (c_line) (== c_cc[33]) *****/

	DCBToTermios(&myDCB, s_termios); /* baudrate */
	return(1);
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
int tcsetattr(int fd, int when, struct termios *s_termios) {
	int vtime;
	DCB dcb;
	COMMTIMEOUTS timeouts;
//	printf("tcsetattr: ");
	fflush(stdout);
	if (s_termios->c_lflag & ICANON) {
		printf("tcsetattr: no canonical mode support\n");
		return -1;	/* and all other c_lflags too */
	}
    if (!GetCommState(tl[fd]->hComm, &dcb)) {
       printf("GetCommState\n");
       return -1;
    }
	if (!GetCommTimeouts(tl[fd]->hComm, &timeouts)) {
       printf("GetCommTimeouts\n");
       return -1;
	}

	/* FIXME: CLOCAL: DONT send SIGHUP on modem disconnect */
	/* FIXME: HUPCL: generate modem disconnect when all has closed or exited */
	/* FIXME: CREAD: without this, data cannot be read */
	/* FIXME: MDMBUF: carrier based flow control of output */

	/*** control flags, c_cflag **/
	if (!(s_termios->c_cflag & CIGNORE)) {
		/* CIGNORE: ignore control modes and baudrate */
    	if (TermiosToDCB(s_termios, &dcb) < 0)	/* baudrate */
			return -1;
    	dcb.ByteSize = termios_to_bytesize(s_termios->c_cflag);
		if (s_termios->c_cflag & PARENB) {
			if (s_termios->c_cflag & PARODD)
				dcb.Parity = ODDPARITY;
			else
				dcb.Parity = EVENPARITY;
		} else
			dcb.Parity = NOPARITY;
		if (s_termios->c_cflag & CSTOPB) dcb.StopBits = TWOSTOPBITS;
	    else dcb.StopBits = ONESTOPBIT;
		if (s_termios->c_cflag & CRTS_IFLOW)
			dcb.fRtsControl = RTS_CONTROL_HANDSHAKE;
    	else dcb.fRtsControl = RTS_CONTROL_ENABLE;
		if (s_termios->c_cflag & CCTS_OFLOW) dcb.fOutxCtsFlow    = TRUE;
	    else dcb.fOutxCtsFlow    = FALSE;
	}

	/*** input flags, c_iflag **/
	if ((s_termios->c_iflag & INPCK) && !(s_termios->c_iflag & IGNPAR)) {
		dcb.fParity = TRUE;
   	} else dcb.fParity = FALSE;
	if (s_termios->c_iflag & ISTRIP) dcb.fBinary = FALSE; /* not in win95? */
	else dcb.fBinary = TRUE;	 /* ISTRIP: strip to seven bits */
	/* FIXME: IGNBRK: ignore break */
	/* FIXME: BRKINT: interrupt on break */
	dcb.fOutX = (s_termios->c_iflag & IXON) ? TRUE : FALSE;
	dcb.fInX = (s_termios->c_iflag & IXOFF) ? TRUE : FALSE;
    dcb.fTXContinueOnXoff = (s_termios->c_iflag & IXANY) ? TRUE : FALSE;
	/* FIXME: IMAXBEL: if input buffer full, send bell */

    dcb.fDtrControl     = DTR_CONTROL_ENABLE;	/* no DTR control in termios? */
    dcb.fOutxDsrFlow    = FALSE;	/* no DSR control in termios? */
    dcb.fDsrSensitivity = FALSE;	/* DONT ignore rx bytes when DSR is OFF */
    dcb.XonChar         = s_termios->c_cc[VSTART];
    dcb.XoffChar        = s_termios->c_cc[VSTOP];
    dcb.XonLim          = 0;	/* ? */
    dcb.XoffLim         = 0;	/* ? */
    if (EV_BREAK|EV_CTS|EV_DSR|EV_ERR|EV_RING|(EV_RLSD & EV_RXFLAG))
		dcb.EvtChar = '\n';      
    else
		dcb.EvtChar = '\0';

    if (!SetCommState(tl[fd]->hComm, &dcb)) {
       printf("SetCommState\n");
       return(-1);
    }

//	printf("VTIME:%d, VMIN:%d\n", s_termios->c_cc[VTIME], s_termios->c_cc[VMIN]);
	vtime = s_termios->c_cc[VTIME] * 100;
	timeouts.ReadTotalTimeoutConstant = vtime;
	timeouts.ReadIntervalTimeout = vtime;	/* max between bytes */
	if (s_termios->c_cc[VMIN] > 0 && vtime > 0) {
		timeouts.ReadIntervalTimeout = s_termios->c_cc[VMIN] * vtime;
		printf("ReadIntervalTimeout %ld\n", timeouts.ReadIntervalTimeout);
	} else if (s_termios->c_cc[VMIN] == 0 && vtime == 0) {
		timeouts.ReadIntervalTimeout = 1; /* read returns immediately */
		printf("vmin & vtime == 0\n");
	}
#if 0
	printf("c_cc[VTIME] = %d, c_cc[VMIN] = %d\n",
		s_termios->c_cc[VTIME], s_termios->c_cc[VMIN]);
	printf("ReadTotalTimeoutConstant: %ld\n", timeouts.ReadTotalTimeoutConstant);
	printf("ReadIntervalTimeout : %ld\n", timeouts.ReadIntervalTimeout);
	printf("ReadTotalTimeoutMultiplier: %ld\n",
		timeouts.ReadTotalTimeoutMultiplier);
#endif
    if (!SetCommTimeouts(tl[fd]->hComm, &timeouts)) {
       printf("SetCommTimeouts\n");
       return(-1);
    }
	memcpy(&tl[fd]->ttyset, s_termios, sizeof(struct termios));
    return ( TRUE ) ;
}

int tcsendbreak(int fd, int duration) {
	/* send a stream of zero bits for duration */
	return 1;
}

int tcdrain (int fd) {
	/* block until all queued output has been transmitted */
	return 1;
}

int tcflush(int fd, int queue_selector) {
	/* clear input & output queues */
	return 1;
}

int tcflow(int fd, int action) {
	switch (action) {
	    case TCOOFF: break;		/* Suspend transmission of output */
    	case TCOON: break;		/* Restart transmission of output */
    	case TCIOFF: break;		/* Transmit a STOP character */
    	case TCION: break;		/* Transmit a START character */
		default: return -1;
	}
	return 1;
}

int ioctl(int fd, int request, unsigned int *arg) {
	DWORD dwStatus = 0;
	switch(request) {
		case TIOCMGET:
			GetCommModemStatus(tl[fd]->hComm, &dwStatus);
			if (dwStatus & MS_RLSD_ON) *arg |= TIOCM_CAR;
			else *arg &= ~TIOCM_CAR;
			if (dwStatus & MS_RING_ON) *arg |= TIOCM_RNG;
			else *arg &= ~TIOCM_RNG;
			if (dwStatus & MS_DSR_ON) *arg |= TIOCM_DSR;
			else *arg &= ~TIOCM_DSR;
			if (dwStatus & MS_CTS_ON) *arg |= TIOCM_CTS;
			else *arg &= ~TIOCM_CTS;

/*			if (tl[fd]->dwFunction & SETDTR) *arg |= TIOCM_DTR;
			if (tl[fd]->dwFunction & CLRDTR) *arg &= ~TIOCM_DTR;
			if (tl[fd]->dwFunction & SETRTS) *arg |= TIOCM_RTS;
			if (tl[fd]->dwFunction & CLRRTS) *arg &= ~TIOCM_RTS;	*/
/*			TIOCM_LE
			TIOCM_ST
			TIOCM_SR	*/
			break;
		case TIOCMSET:
			EscapeCommFunction(tl[fd]->hComm,
				(*arg & TIOCM_DTR) ? SETDTR : CLRDTR);
			EscapeCommFunction(tl[fd]->hComm,
				(*arg & TIOCM_RTS) ? SETRTS : CLRRTS);
/*			TIOCM_LE
			TIOCM_ST
			TIOCM_SR	*/
			break;
		default:
			printf("ioctl: unknown request: %#x\n", request);
			return -1;
	}
	return 0;
}

int fcntl(int fd, int command, int arg) {
	fprintf(stderr, "FIXME: fcntl(%d, %#o, %#o)\n", fd, command, arg);
	return 0;
}
int  serial_select(int  n,  fd_set  *readfds,  fd_set  *writefds, fd_set *exceptfds, struct timeval *timeout) {
	return 1;
	/* FIXME */
}

