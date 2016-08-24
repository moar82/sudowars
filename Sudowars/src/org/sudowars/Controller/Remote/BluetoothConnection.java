/*******************************************************************************
 * Copyright (c) 2011 - 2012 Adrian Vielsack, Christof Urbaczek, Florian Rosenthal, Michael Hoff, Moritz Lüdecke, Philip Flohr.
 * 
 * This file is part of Sudowars.
 * 
 * Sudowars is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Sudowars is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Sudowars.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * 
 * 
 * Diese Datei ist Teil von Sudowars.
 * 
 * Sudowars ist Freie Software: Sie können es unter den Bedingungen
 * der GNU General Public License, wie von der Free Software Foundation,
 * Version 3 der Lizenz oder (nach Ihrer Option) jeder späteren
 * veröffentlichten Version, weiterverbreiten und/oder modifizieren.
 * 
 * Sudowars wird in der Hoffnung, dass es nützlich sein wird, aber
 * OHNE JEDE GEWÄHELEISTUNG, bereitgestellt; sogar ohne die implizite
 * Gewährleistung der MARKTFÄHIGKEIT oder EIGNUNG FÜR EINEN BESTIMMTEN ZWECK.
 * Siehe die GNU General Public License für weitere Details.
 * 
 * Sie sollten eine Kopie der GNU General Public License zusammen mit diesem
 * Programm erhalten haben. Wenn nicht, siehe <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 * initial API and implementation:
 * Adrian Vielsack
 * Christof Urbaczek
 * Florian Rosenthal
 * Michael Hoff
 * Moritz Lüdecke
 * Philip Flohr 
 ******************************************************************************/
package org.sudowars.Controller.Remote;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.sudowars.DebugHelper;
import org.sudowars.R;
import org.sudowars.R.integer;
import org.sudowars.R.string;
import org.sudowars.Controller.Local.Activity.MultiplayerSettings;
import org.sudowars.Model.CommandManagement.Command;
import org.sudowars.Model.CommandManagement.MultiplayerSettingsCommands.KickMultiplayerClientCommand;
import org.sudowars.Model.CommandManagement.MultiplayerSettingsCommands.KickMultiplayerClientCommand.KickStatus;
import org.sudowars.Model.Difficulty.Difficulty;
import org.sudowars.Model.Difficulty.DifficultyEasy;
import org.sudowars.Model.Difficulty.DifficultyHard;
import org.sudowars.Model.Difficulty.DifficultyMedium;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Handler;
import android.widget.Toast;

/**
 * This class handles the bluetooth connection (connecting and transfering data) and
 * takes care of eventually damaged Packages that may occur during transfer between two bluetooth devices.
 * 
 */

public class BluetoothConnection implements Serializable {
	private static BluetoothConnection btActive = null;
	private static final long serialVersionUID = 4029656510931959533L;
	
	Handler timeSyncHandler = new Handler();
	/**
	 * event handler to handle events from this class
	 */
	private Handler bluetoothEventHandler;
	
	/**
	 * The state of the bluetooth connection
	 */
	int state = 0;
	
	/**
	 * Message for new Received Data
	 * Data can be retreived via {@link BluetoothConnection#getDeliveredCommand()}
	 */
	public final static int MESSAGE_NEW_DATA = 1;
	
	/**
	 * Message for a changed Bluetooth State eg. Connected
	 */
	public final static int MESSAGE_BT_STATE_CHANGE = 2;
	
	/**
	 * Message for a successfull delivered Command
	 * The delivered Command can be retreived via {@link BluetoothConnection#getDeliveredCommand()}
	 */
	public final static int MESSAGE_BT_PACKET_DELIVERED = 3;
	
	/**
	 * Packet list to handle lost packets
	 */
	ArrayList<BluetoothPacket> btPackets;
	
	static final UUID uuid_secure = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66"); 
	
	/**
	 * In this state ether a connection has been closed or the class was freshly created
	 */
	public static final int STATE_NONE = 0;
	
	/**
	 * This state is present after calling the connect function
	 */
	public static final int STATE_CONNECTING = 1;
	
	/**
	 * This state represents a connected socket
	 */
	public static final int STATE_CONNECTED = 2;
	/**
	 * State for listening
	 */
	public static final int STATE_LISTENING = 4;
	
	private LinkedList<BluetoothPacket> deliveredCommand;
	
	Object socketClose = new Object();
	
	/**
	 * Thread to run When Connected, handles Packet reception
	 */
	ConnectedThread cndThread;
	
	/**
	 * Packet Handler, takes care of damaged Packages
	 */
	PacketHandler ptHandler;
	
	SendThread sendQueue;
	
	/**
	 * Current Data received in buffer
	 */
	Command currentData;
	LinkedList<BluetoothPacket> currentPacket;
	private ArrayList<BluetoothPacket> sentPacket;
	protected SudowarsSocket swSocket;
	public TimeSyncer tsync;
	
	public SocketEvent sckEvent = new SocketEvent() {
		@Override
		public void onListening() {
			setState(STATE_LISTENING);			
		}
		
		@Override
		public void onConnecting() {
			setState(STATE_CONNECTING);
		}
		
		@Override
		public void onConnected() {
			connected();
		}
		
		@Override
		public void onClose() {
			connectionClosed();
		}
	};
	
	private Runnable timeSyncRunnable = new Runnable() {
		@Override
		public void run() {
			if (tsync != null && state == STATE_CONNECTED) {
				tsync.syncTime();
				timeSyncHandler.postDelayed(timeSyncRunnable, 5000);
			}
		}
	};
	
	/**
	 * Constructs a new {@link BluetoothConnection}
	 */
	public BluetoothConnection () {
		this.state = STATE_NONE;
		if (btActive != null){
			if (swSocket != null)
				swSocket.close();
		}
		swSocket = new SudowarsBluetoothSocket();
		swSocket.setEventHandler(sckEvent);
		this.tsync = new TimeSyncer(this.swSocket);
		btActive = this;
		this.deliveredCommand = new LinkedList<BluetoothPacket>();
		this.currentPacket = new LinkedList<BluetoothPacket>();
		sentPacket = new ArrayList<BluetoothPacket>(256);
		for (int n  = 0; n < 256; n++)
			sentPacket.add(null);
	}
	
	public Command getDeliveredCommand() {
		if (this.deliveredCommand == null)
			return null;
		Command ret = (Command) this.deliveredCommand.poll().getCommand();
		
		return ret;
	}
	
	 /**
     * Sets the event handler to handle events from this class
     *
     * @param bluetoothEventHandler the bluetoothEventHandler
     */
    public void setBluetoothEventHandler(Handler bluetoothEventHandler) {
            this.bluetoothEventHandler = bluetoothEventHandler;
    }
    
    public void sendCommandAsync(Command cmd) {
    	this.sendQueue.sendAsync(cmd);
    	DebugHelper.log(DebugHelper.PackageName.BluetoothConnection, "Send async, got new Command");
    }
    
    /**
     * Returns an uniquely ID for a Bluetooth Packet
     * @return
     */
    public int getNextPacketId() {
    	return BluetoothPacket.getNextCommandId();
    }
	
	public static BluetoothConnection getActiveBluetoothConnection() {
		return btActive;
	}
	
	/**
	 * This command creates a {@link BluetoothPacket} and sends it to the receiver
	 *
	 * @param data the command as serializable command object
	 *
	 * @return returns True on success
	 */
	public void sendCommand(Command data) {
		BluetoothPacket btPacket = new BluetoothPacket(data);
		if (ptHandler != null && this.state == STATE_CONNECTED){
			ptHandler.addOutgoingPacket(btPacket);
			sentPacket.set((int)(btPacket.getPacketId() & 0xFF), btPacket);
			swSocket.sendData(btPacket.getPacket());
			debugHex(btPacket.getCRC(), "Bluetooth.PacketOut.Checksum");
		}
	}
	
	
	void setState(int newState) {
		
		this.state = newState;
		if (this.bluetoothEventHandler != null)
			this.bluetoothEventHandler.obtainMessage(MESSAGE_BT_STATE_CHANGE).sendToTarget();
	}
	
	/**
	 * Closes the bluetooth connection if active, also triggers an onConnectionStateChange event
	 */
	public void closeConnection () {
			if (this.state == STATE_NONE)
				return;
			this.state = STATE_NONE;
			
			if (this.swSocket.isConnected())
				this.swSocket.close();
			if (this.sendQueue != null)
				this.sendQueue.endThread();
			BluetoothConnection.btActive = null;	
	}
	
	void connected() {
		DebugHelper.log(DebugHelper.PackageName.BluetoothConnection, "Starting Transmission Threads");
		state = STATE_CONNECTED;
		this.cndThread = new ConnectedThread();
		this.ptHandler = new PacketHandler(this);
		this.cndThread.start();
		this.sendQueue = new SendThread();
		if (this instanceof BluetoothServer)
			timeSyncHandler.postDelayed(timeSyncRunnable, 5000);
		this.sendQueue.start();
		setState(STATE_CONNECTED);
		
		DebugHelper.log(DebugHelper.PackageName.BluetoothConnection, "Connected Threads started!");
	}
	protected void connectionClosed() {
		DebugHelper.log(DebugHelper.PackageName.BluetoothConnection, "BluetoothConnection: Connection has been closed!");
		this.timeSyncHandler.removeCallbacks(timeSyncRunnable);
		
		setState(STATE_NONE);
	}
	
	private class ConnectedThread extends Thread {
		private ConnectedThread() {}
		
		public void run() {
			byte[] header = new byte[10];
			int len;
			
			while (swSocket.isConnected()) {
				swSocket.recv(header);
				
				len = ((int)(header[8] & 0xFF) << 8) | (int)(header[9] & 0xFF);
				
				byte[] data = new byte[len];
				swSocket.recv(data);				
				debugHex(data, "Received");
				
				if (ptHandler.checkPacket(header, data))
					continue;
				
				BluetoothPacket p = new BluetoothPacket(header, data);
				debugHex(p.getCRC(), "BluetoothPacketOut.Checksum");
				if (p.isValid()) {
					currentPacket.offer(p);
					ptHandler.sendSuccessCommand(p);
					//currentData = (Command) p.getCommand();
					DebugHelper.log(DebugHelper.PackageName.BluetoothConnection, "Got valid Packet, invoking event");
					bluetoothEventHandler.obtainMessage(MESSAGE_NEW_DATA).sendToTarget();
				} else {
					ptHandler.sendResendCommand(p);
				}
			}
		}
	}
	
	private void debugHex(byte[] data, String logd){
		 ptHandler.debugHex(data, 0, logd);
	}
	
	public class PacketHandler implements Serializable {
		private static final long serialVersionUID = 4977147614426859999L;
		
		/**
		 * Data byte for a successfull Transmitted Command
		 */
		private static final byte CMD_OK = 0x1A;
		/**
		 * Data byte for a corrupted Packet
		 */
		private static final byte CMD_CORRUPT = (byte)0xE1;
		
		/**
		 * Counter for Statistics
		 */
		//private long resentPackets = 0;
		//private long sentPackets = 0;
		
		BluetoothConnection btConnection;
		
		List<BluetoothPacket> btPackets = null;
		
		/**
		 * Constructor
		 * @param btc Active Bluetooth Connection
		 */
		public PacketHandler(BluetoothConnection btc) {
			this.btConnection = btc;
			btPackets = new ArrayList<BluetoothPacket>();
		}
		
		/**
		 * Add a Packet to the Outgoing Packets
		 * @param btPacket
		 */
		public void addOutgoingPacket(BluetoothPacket btPacket) {
			if (btPacket == null)
				return;
			this.btPackets.add(btPacket);
			//this.sentPackets++;
			btPacket.setSent();
		}
		
		/**
		 * Get a Packet by ID
		 * @param ID
		 * @return
		 */
		private BluetoothPacket getPacketById(byte ID) {
			Iterator<BluetoothPacket> pckIterator = this.btPackets.iterator();
			BluetoothPacket ret;
			while (pckIterator.hasNext()) {
				ret = pckIterator.next();
				if (ret.getPacketId() == ID)
					return ret;
			}
			return null;
		}
		
		/**
		 * Checks weather the Packet Received is a Command Packet
		 * @param header
		 * @param data
		 * @return
		 */
		public boolean checkPacket(byte[] header, byte[] data) {
			if (header.length < 10)
				return false;
			if (header[0] != 'S' && header[1] != 'W')
				return false;
			if ((header[2] & 0xFF) + (header[3] & 0xFF) + (header[4] & 0xFF) + (header[5] & 0xFF) +  (header[6] & 0xFF) > 0)
				return false;
			
			BluetoothPacket tmp;
			if (data[0] == TimeSyncer.CMD_TIMESYNC && data.length == 9) {
				
				tsync.syncTimeCommand(data);
				return true;
			} else if (data[0] == TimeSyncer.CMD_TIMESYNC_PONG && data.length == 2) {
				tsync.syncTimePongCommand();
				return true;
			} else if (data[0] == CMD_OK) {
				tmp = getPacketById(header[7]);
				if (tmp == null){
					DebugHelper.log(DebugHelper.PackageName.BluetoothConnection_PacketHandler, "Could not find a Packet for removing! Aborting!!");
					
					return true;
				}
				
				deliveredCommand.offer(tmp);
				bluetoothEventHandler.obtainMessage(MESSAGE_BT_PACKET_DELIVERED).sendToTarget();
				btPackets.remove(tmp);
				//sendQueue.notifySent();
			} else if (data[0] == CMD_CORRUPT) {
				tmp = getPacketById(header[7]);
				
				if (tmp == null){
					DebugHelper.log(DebugHelper.PackageName.BluetoothConnection_PacketHandler, "Could not find a Packet for resend! Aborting!!");
					
					return true;
				}
				if (tmp.isRemoteCorrupted()) {
					DebugHelper.log(DebugHelper.PackageName.BluetoothConnection_PacketHandler, "Could not transfer one Packet, dropping!");
					closeConnection();
					return true;
				}
					
				DebugHelper.log(DebugHelper.PackageName.BluetoothConnection_PacketHandler, "Received a currupt packetRequest! resending");
				//this.resentPackets ++;
				tmp.markRemoteCorrupted();
				swSocket.sendData(tmp.getPacket());
			}
			
			return true;
		}
		
		/**
		 * Send a command to Toggle a resend of a Packet
		 * @param btPacket
		 */
		public void sendResendCommand(BluetoothPacket btPacket) {
			byte[] data = {
					'S', 'W',	//Magic
					0, 0, 0, 0, 0,	//Command Header
					btPacket.getPacketId(),
					0, 1,
					CMD_CORRUPT
			};
			swSocket.sendData(data);
		}
		
		/**
		 * Send a command to take a Successfull received packet out of the Buffer
		 * @param btPacket
		 */
		public void sendSuccessCommand(BluetoothPacket btPacket) {
			byte[] data = {
					'S', 'W',	//Magic
					0, 0, 0, 0, 0,	//Command Header
					btPacket.getPacketId(),
					0, 1,
					CMD_OK
			};
			swSocket.sendData(data);
		}

		void debugHex(byte[] data, int begin, String logd) {
			String out = "";
			for (int n = begin; n < data.length; n++) {
				out += " ";
				if (n % 16 == 0 && n != 0){
					DebugHelper.log(DebugHelper.PackageName.BluetoothConnection, out);
					out = "";
				}
				if ((data[n] & 0xFF) < 16)
					out += "0";
				out += Integer.toHexString(data[n] & 0xFF);
			}
			DebugHelper.log(DebugHelper.PackageName.BluetoothConnection, out);
		}
	}
	
	public void stop() {
		this.closeConnection();
		if (this.swSocket != null)
			this.swSocket.stop();
		if (this.sendQueue != null)
			this.sendQueue.endThread();
	}
	
	/**
	 * Returns the difficulty
	 * 
	 * @param multiplayerSettings TODO
	 * @return the difficulty
	 */
	public Difficulty decodeDifficulty(MultiplayerSettings multiplayerSettings) {
		Difficulty difficulty;
		
		if (multiplayerSettings.settings.getDifficulty() == 0) {
			difficulty = new DifficultyEasy();
		} else if (multiplayerSettings.settings.getDifficulty() == 1){
			difficulty = new DifficultyMedium();
		} else {
			difficulty = new DifficultyHard();
		}
		
		return difficulty;
	}

	/**
	 * Running on a click on button {@link btnBan}.
	 * @param multiplayerSettings TODO
	 */
	public void onBtBanClick(MultiplayerSettings multiplayerSettings) {
		if (this instanceof BluetoothServer && sckEvent.getState(this) == BluetoothConnection.STATE_CONNECTED) {
			KickMultiplayerClientCommand command = new KickMultiplayerClientCommand(KickStatus.KICKBAN);
			((BluetoothServer) this).sendCommandAsync((Command) command);
			((BluetoothServer) this).ban();
			multiplayerSettings.tglRemoteReady.setChecked(false);
			Toast.makeText(multiplayerSettings.getApplicationContext(), string.text_ban, Toast.LENGTH_SHORT).show();
			
			DebugHelper.log(DebugHelper.PackageName.MultiplayerSettings, "Remote banned.");
		}
	}

	/**
	 * Running on a click on button {@link BtnVisible}.
	 * @param multiplayerSettings TODO
	 */
	public void onBtVisibleClick(MultiplayerSettings multiplayerSettings) {
		if (multiplayerSettings.visibleCounter <= 0
				&& this instanceof BluetoothServer
				&& BluetoothAdapter.getDefaultAdapter().isEnabled()) {	
			DebugHelper.log(DebugHelper.PackageName.MultiplayerSettings, "Make bluetooth device discoverable.");
			
			Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
					multiplayerSettings.getResources().getInteger(integer.bluetooth_discoverable_duration));
			multiplayerSettings.startActivityForResult(discoverableIntent, MultiplayerSettings.REQUEST_DISCOVERABLE);
		} else {
			Toast.makeText(multiplayerSettings.getApplicationContext(), multiplayerSettings.getString(string.button_bluetooth_is_visible_before)+" "
									+multiplayerSettings.visibleCounter+" "+multiplayerSettings.getString(string.button_bluetooth_is_visible_after),
					Toast.LENGTH_SHORT).show();
		}
	}

	private class SendThread extends Thread {
		private BlockingQueue<Command> btPacketsToSend;
		Boolean EXIT = false;
		
		public SendThread () {
			btPacketsToSend = new LinkedBlockingQueue<Command>();
		}
		
		public void run() {
			Command toSend;
			try {
				while (!EXIT) {
					toSend = btPacketsToSend.take();
					sendCommand(toSend);
					DebugHelper.log(DebugHelper.PackageName.BluetoothConnection, "Sent a message async");
				}
			} catch (InterruptedException e ) {
				Thread.currentThread().interrupt();
				DebugHelper.log(DebugHelper.PackageName.BluetoothConnection, "SendThread was interrupted");
			}
		}
		
		public void sendAsync(Command c){
			try {
				btPacketsToSend.put(c);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		public void endThread() {
			EXIT = true;
			this.interrupt();
		}
	}
}
