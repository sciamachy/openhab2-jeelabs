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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * JeeLink connector for remote TCP communication.
 *
 * @author Chris Whiteford - Initial contribution
 */
public class JeeLinkTCPConnector implements JeeLinkConnectorInterface {

	private static final Logger logger = LoggerFactory.getLogger(JeeLinkTCPConnector.class);

	private UDPListener _udpListener = null;	


	

	private class UDPListener extends Thread {

		private DatagramSocket socket;
		BlockingQueue _queue = new ArrayBlockingQueue(1024);

		public UDPListener() {

		}

		public void connect(String ip, Integer port) throws IOException {
			logger.debug("TCP: Connecting...");
        
		    socket = new DatagramSocket();
		    socket.connect(InetAddress.getByName(ip), port);
		}

		@Override
		public void run()
		{
			byte[] buffer = new byte[20];
			logger.debug("Im running!");
			while(true)
			{
				DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
				if(socket.isConnected())
				{
					try
					{
						socket.receive(dp);	
						
						String s = new String(dp.getData(), 0, dp.getLength());
						logger.debug(s);

						try 
						{
							_queue.put(dp.getData());	
						}
						catch (InterruptedException e)
						{
							logger.debug("TCP: Interrupted Exception");
						}
						

					}
					catch (IOException e) 
					{
						logger.debug("TCP: interrupted");
					}

					Thread.yield();

				}
			}
		}

		public void disconnect() {
			logger.debug("TCP: Disconnecting...");
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

	public void connect(String ip, Integer port) throws IOException {
		_udpListener = new UDPListener();

		_udpListener.connect(ip,port);

		_udpListener.start();
        
	}

	@Override
	public void disconnect() {
		if(_udpListener != null)
		{
			_udpListener.disconnect();	
		}
		
	}

	public BlockingQueue messageQueue() {
		return _udpListener.getQueue();
	}
}
