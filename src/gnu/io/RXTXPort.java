/*-------------------------------------------------------------------------
|   A wrapper to convert RXTX into Linux Java Comm
|   Copyright 1998 Kevin Hester, kevinh@acm.org
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
package gnu.io;

import javax.comm.*;

import java.io.*;
import java.util.*;


/**
  * Comm API serial port implementation.  Currently, it's just a wrapper for
  * NativePort, because javah pukes when I feed it a class that extends
  * SerialPort.  I don't know why.  -DPL
  */
public final class RXTXPort extends SerialPort {

	private final NativePort port;

	public RXTXPort( String name ) throws IOException {
		port = new NativePort( this, name );
	}

	public InputStream getInputStream() throws IOException {
		return port.getInputStream();
	}

	public OutputStream getOutputStream() throws IOException {
		return port.getOutputStream();
	}

	public void enableReceiveThreshold( int thresh ) {
		port.enableReceiveThreshold( thresh );
	}
	public void disableReceiveThreshold() {
		port.disableReceiveThreshold();
	}
	public boolean isReceiveThresholdEnabled() {
		return port.isReceiveThresholdEnabled();
	}
	public int getReceiveThreshold()	{
		return port.getReceiveThreshold();
	}

	public void enableReceiveTimeout( int timeout ) {
		port.enableReceiveTimeout( timeout );
	}
	public void disableReceiveTimeout() {
		port.disableReceiveTimeout();
	}
	public boolean isReceiveTimeoutEnabled() {
		return port.isReceiveTimeoutEnabled();
	}
	public int getReceiveTimeout() {
		return port.getReceiveTimeout();
	}

	public void enableReceiveFraming( int framingByte )
		throws UnsupportedCommOperationException
	{
		port.enableReceiveFraming( framingByte );
	}
	public void disableReceiveFraming() {
		port.disableReceiveFraming();
	}
	public boolean isReceiveFramingEnabled() {
		return port.isReceiveFramingEnabled();
	}
	public int getReceiveFramingByte() {
		return port.getReceiveFramingByte();
	}

	public void setInputBufferSize( int size ) {
		port.setInputBufferSize( size );
	}
	public int getInputBufferSize() {
		return port.getInputBufferSize();
	}
	public void setOutputBufferSize( int size ) {
		port.setOutputBufferSize( size );
	}
	public int getOutputBufferSize() {
		return port.getOutputBufferSize();
	}
	
	public void setSerialPortParams( int speed, int dataBits, int stopBits,
		int parity ) throws UnsupportedCommOperationException
	{
		port.setSerialPortParams( speed, dataBits, stopBits, parity );
	}
	public int getBaudRate() { return port.getBaudRate(); }
	public int getDataBits() { return port.getDataBits(); }
	public int getStopBits() { return port.getStopBits(); }
	public int getParity() { return port.getParity(); }

	public void sendBreak( int millis ) {
		port.sendBreak( millis );
	}

	public void setFlowControlMode( int flowcontrol ) {
		port.setFlowControlMode( flowcontrol );
	}

	public int getFlowControlMode() {
		return port.getFlowControlMode();
	}

	public void setDTR( boolean dtr ) { port.setDTR( dtr ); }
	public boolean isDTR() { return port.isDTR(); }
	public void setRTS( boolean rts ) { port.setRTS( rts ); }
	public boolean isRTS() { return port.isRTS(); }
	public boolean isCTS() { return port.isCTS(); }
	public boolean isDSR() { return port.isDSR(); }
	public boolean isRI() { return port.isRI(); }
	public boolean isCD() { return port.isCD(); }

	public void addEventListener( SerialPortEventListener lsnr )
		throws TooManyListenersException
	{
		port.addEventListener( lsnr );
	}

	public void removeEventListener() {
		port.removeEventListener();
	}

	public void notifyOnDataAvailable( boolean enable ) {
		port.notifyOnDataAvailable( enable );
	}
	public void notifyOnOutputEmpty( boolean enable ) {
		port.notifyOnOutputEmpty( enable );
	}
	public void notifyOnCTS( boolean enable ) {
		port.notifyOnCTS( enable );
	}
	public void notifyOnDSR( boolean enable ) {
		port.notifyOnDSR( enable );
	}
	public void notifyOnRingIndicator( boolean enable ) {
		port.notifyOnRingIndicator( enable );
	}
	public void notifyOnCarrierDetect( boolean enable ) {
		port.notifyOnCarrierDetect( enable );
	}
	public void notifyOnOverrunError( boolean enable ) {
		port.notifyOnOverrunError( enable );
	}
	public void notifyOnParityError( boolean enable ) {
		port.notifyOnParityError( enable );
	}
	public void notifyOnFramingError( boolean enable ) {
		port.notifyOnFramingError( enable );
	}
	public void notifyOnBreakInterrupt( boolean enable ) {
		port.notifyOnBreakInterrupt( enable );
	}
}
