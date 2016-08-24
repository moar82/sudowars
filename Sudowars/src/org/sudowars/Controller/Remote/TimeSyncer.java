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

import org.sudowars.DebugHelper;

import android.os.SystemClock;

public class TimeSyncer implements Serializable {
	public static final byte CMD_TIMESYNC = (byte)0x45;
	public static final byte CMD_TIMESYNC_PONG = (byte)0xF0;
	private static final long serialVersionUID = 4137026736197609825L;
	
	private long timeOffset = 0;
	
	private long sentTs = 0;
	
	private SudowarsSocket swSocket;
	
	public TimeSyncer (SudowarsSocket swSocket) {
		this.swSocket = swSocket;
	}
	
	private long getActualTimestamp() {
		return SystemClock.uptimeMillis();
	}
	
	private long byteToLong(byte data[]) {
		long ret = 0;
		for (int n = 0; n < 8; n++) {
			ret <<= 8;
			ret |= (data[n] & 0xFF);
		}
		return ret;
	}
	
	private byte[] longToByte(long l) {
		byte ret[] = new byte[8];
		for (int n = 0; n < 8; n++) {
			ret[7 - n] = (byte) (l & 0xFF);
			l >>= 8;
		}
		return ret;
	}
	
	public void syncTime() {
		sentTs = getActualTimestamp();
		sendPacket(CMD_TIMESYNC, longToByte(sentTs));
		DebugHelper.log(DebugHelper.PackageName.TimeSyncer, "New Sync Time Command Actual time is " + this.getCorrectedTimestamp());
	}
	
	private byte[] cutTheCrap(byte[] data) {
		byte ret[] = new byte[data.length - 1];
		System.arraycopy(data, 1, ret, 0, data.length - 1);
		return ret;
	}
	
	public void syncTimeCommand(byte[] data) {
		long timeReceived = getActualTimestamp();
		
		this.timeOffset = byteToLong(cutTheCrap(data)) - timeReceived;
		
		sendPacket(CMD_TIMESYNC_PONG);
		DebugHelper.log(DebugHelper.PackageName.TimeSyncer, "New Time Offset: " + this.timeOffset + " Actual Synced time is " + this.getCorrectedTimestamp());
	}
	
	public void syncTimePongCommand() {
		long timeReceived = getActualTimestamp();
		this.timeOffset = ((timeReceived - sentTs) >> 1) * -1;
		DebugHelper.log(DebugHelper.PackageName.TimeSyncer, "New Time Offset: " + this.timeOffset + " Actual Synced time is " + this.getCorrectedTimestamp());
	}
	
	public long getCorrectedTimestamp() {
		return getActualTimestamp() + this.timeOffset;
	}
	
	private void sendPacket (byte cmd) {
		byte data[] = new byte[1];
		sendPacket(cmd, data);
	}
	
	private void sendPacket (byte cmd, byte[] data) {
		int length = data.length + 1;
		byte syncPacketHeader[] = {
				'S', 'W', //Magic Header
				0x00, 0x00, 0x00, 0x00, 0x00,	//Fake CRC
				0x00,							//Fake PacketID
				(byte)(length >> 8), (byte)(length & 0xFF),	//Payload Length
				cmd		//CMD
		};
		
		byte syncPacket[] = new byte[11 + data.length];
		System.arraycopy(syncPacketHeader, 0, syncPacket, 0, 11);
		System.arraycopy(data, 0, syncPacket, 11, data.length);
		
		swSocket.sendData(syncPacket);
	}

	/**
	 * This Function tries to make a connection to the device mac address
	 *
	 * @param bluetoothConnection TODO
	 * @param deviceMac device mac address to connect to
	 * @return <code>true</code>, if connected
	 */
	public boolean connect(BluetoothConnection bluetoothConnection, String deviceMac) {
		return bluetoothConnection.swSocket.connect(deviceMac);
	}

	public long getCorrectedUpTime() {
		if (this != null)
			return getCorrectedTimestamp();
		return 0;
	}
}
