/*
 * @(#)SimpleWrite.java 1.12 98/06/25 SMI
 * 
 * Copyright (c) 1998 Sun Microsystems, Inc. All Rights Reserved.
 */
import java.io.*;
import java.util.*;
import gnu.io.*;

/**
 * Class declaration
 *
 *
 * @author
 * @version 1.10, 08/04/00
 */
public class SimpleWrite {
    static Enumeration        portList;
    static CommPortIdentifier portId;
    static String             messageString = "Hello, world!";
    static SerialPort         serialPort;
    static OutputStream       outputStream;
    static boolean            outputBufferEmptyFlag = false;
    /**
     * Method declaration
     *
     *
     * @param args
     *
     * @see
     */
    public static void main(String[] args) {
        boolean portFound = false;
        String  defaultPort = "/dev/term/a";

        if (args.length > 0) {
            defaultPort = args[0];
        } 

        portList = CommPortIdentifier.getPortIdentifiers();

        while (portList.hasMoreElements()) {
            System.out.println("loop");
            portId = (CommPortIdentifier) portList.nextElement();

            System.out.println("1");
            if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {

                if (portId.getName().equals(defaultPort)) {
            		System.out.println("2");
                    System.out.println("Found port " + defaultPort);

                    portFound = true;

                    try {
            		System.out.println("3");
                        serialPort = 
                            (SerialPort) portId.open("SimpleWrite", 2000);
                    } catch (PortInUseException e) {
                        System.out.println("Port in use.");

                        continue;
                    } 

            		System.out.println("4");
                    try {
                        outputStream = serialPort.getOutputStream();
                    } catch (IOException e) {}

            		System.out.println("5");
                    try {
                        serialPort.setSerialPortParams(9600, 
 
SerialPort.DATABITS_8, 
 
SerialPort.STOPBITS_1, 
 
SerialPort.PARITY_NONE);
                    } catch (UnsupportedCommOperationException e) {}
        
            	System.out.println("6");

                    try {
                        serialPort.notifyOnOutputEmpty(true);
                    } catch (Exception e) {
                        System.out.println("Error setting event notification");
                        System.out.println(e.toString());
                        System.exit(-1);
                    }
                    
                    
                    System.out.println(
                        "Writing \""+messageString+"\" to "
                        +serialPort.getName());

                    System.out.println("WRITE");
                    try {
                        outputStream.write(messageString.getBytes());
                    } catch (IOException e) {}

                    System.out.println("SLEEP");
                    try {
                       Thread.sleep(2000);  // Be sure data is xferred before closing
                    } catch (Exception e) {}
                    System.out.println("CLOSE");
                    serialPort.close();
                    System.exit(1);
                } 
            } 
        } 

        if (!portFound) {
            System.out.println("port " + defaultPort + " not found.");
        } 
    } 
}
