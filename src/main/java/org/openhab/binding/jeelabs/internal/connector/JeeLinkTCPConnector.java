/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.jeelabs.internal.connector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.TooManyListenersException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import org.openhab.binding.jeelabs.internal.JeeLinkMessage;

/**
 * JeeLink connector for remote TCP communication.
 *
 * @author Chris Whiteford - Initial contribution
 */
public class JeeLinkTCPConnector implements JeeLinkConnectorInterface {

	private static final Logger logger = LoggerFactory.getLogger(JeeLinkTCPConnector.class);

	private UDPListener _udpListener = null;	

	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}

	

	private class UDPListener extends Thread {

	
		private DatagramSocket socket;
		BlockingQueue _queue = new ArrayBlockingQueue(1024);

		public UDPListener() {

		}

		public void connect(Integer port) throws IOException {
			logger.debug("UDP: Connecting...");
        
		    socket = new DatagramSocket(port);
		}

		@Override
		public void run()
		{
			byte[] buffer = new byte[20];
			byte[] trimmedBytes = new byte[10];

			logger.debug("Im running!");
			DatagramPacket dp = new DatagramPacket(buffer, buffer.length);

			while(true)
			{
				if(socket != null)
				{
					try
					{
						socket.receive(dp);	
						//String s = new String(dp.getData(), 0, dp.getLength());
						//logger.debug(s);

						trimmedBytes = Arrays.copyOfRange(dp.getData(), 0, 10);

						String hexString = bytesToHex(trimmedBytes);
						logger.debug("UDP Bytes In: {}", hexString);

						try 
						{
							//Convert bytes to a message
							JeeLinkMessage msg = new JeeLinkMessage(trimmedBytes, false);
							_queue.put(msg);	
						}
						catch (InterruptedException e)
						{
							logger.debug("UDP: Interrupted Exception");
						}
						

					}
					catch (IOException e) 
					{
						logger.debug("UDP: interrupted");
					}
				}
				else
				{
					logger.debug("No Socket");
				}
			}

		}

		public void disconnect() {
			logger.debug("UDP: Disconnecting...");
			socket.disconnect();
			socket = null;
		}

		public BlockingQueue getQueue() {
			return _queue;
		}

	}

	public JeeLinkTCPConnector() {
	}

	// Guess I need to implement connect(String) to conform to the interface
	@Override
	public void connect(String ip) throws IOException 
	{
	}

	public void connect(Integer port) throws IOException {
		_udpListener = new UDPListener();

		_udpListener.connect(port);

		_udpListener.start();
        
	}

	@Override
	public void disconnect() {
		if(_udpListener != null)
		{
			_udpListener.disconnect();	
			_udpListener.stop();
			_udpListener = null;
		}
		
	}

	public BlockingQueue messageQueue() {
		return _udpListener.getQueue();
	}
}
