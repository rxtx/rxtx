/*
* Test.class is just that.. a Test
*/


import java.io.*;
import java.util.*;
import javax.comm.*;

public class Test {

InputStream inputStream;
OutputStream outputStream;
SerialPort serialPort;
Thread readThread;

	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.print("Test.class /dev/ports/serialx\n");
			System.exit(-1);
		}
		System.out.print("opening the Port\n");		
		Test reader = new Test(args[0]);
	}

	public Test(String PortName) {
		RXTXCommDriver TxPort = new RXTXCommDriver();
		System.out.print("open Ports\n");		
		serialPort = (SerialPort) TxPort.getCommPort(PortName, CommPortIdentifier.PORT_SERIAL);
		System.out.print("Get Streams\n");		
		try {
			inputStream = serialPort.getInputStream();
			outputStream = serialPort.getOutputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			System.out.println("Baud is " + serialPort.getBaudRate());	
			System.out.println("Bits is " + serialPort.getDataBits());	
			System.out.println("Stop is " + serialPort.getStopBits());	
			System.out.println("Par is " + serialPort.getParity());	
			System.out.print("Set Params\n");		
			serialPort.setSerialPortParams(19200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			System.out.println("Baud is " + serialPort.getBaudRate());	
			System.out.println("Bits is " + serialPort.getDataBits());	
			System.out.println("Stop is " + serialPort.getStopBits());	
			System.out.println("Par is " + serialPort.getParity());	
			System.out.print("Set Params\n");		
			serialPort.setSerialPortParams(9600, SerialPort.DATABITS_7, SerialPort.STOPBITS_2, SerialPort.PARITY_ODD);
			System.out.println("Baud is " + serialPort.getBaudRate());	
			System.out.println("Bits is " + serialPort.getDataBits());	
			System.out.println("Stop is " + serialPort.getStopBits());	
			System.out.println("Par is " + serialPort.getParity());	
		} catch (UnsupportedCommOperationException e) {
			e.printStackTrace();
		}
		System.out.print("Sending 0x01\n");		
		try {
			outputStream.write((byte)0x01);
			System.out.print("0x01 Sent\n");		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

