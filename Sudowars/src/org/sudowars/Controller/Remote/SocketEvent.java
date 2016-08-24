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

import java.io.IOException;

import org.sudowars.DebugHelper;
import org.sudowars.Controller.Remote.SudowarsBluetoothSocket.INTERNAL_STATE;
import org.sudowars.Model.CommandManagement.Command;

public abstract class SocketEvent {
	abstract public void onClose();
	abstract public void onConnected();
	abstract public void onConnecting();
	abstract public void onListening();
	/**
	 * Returns the device name of the remote device
	 * 
	 * @param bluetoothConnection TODO
	 * @return String the device name
	 */
	public String getRemoteDeviceName(BluetoothConnection bluetoothConnection) {
		return bluetoothConnection.swSocket.getRemoteHost();
	}
	public Command getCurrentCommand(BluetoothConnection bluetoothConnection) {
		Command cmd = (Command)(bluetoothConnection.currentPacket.poll().getCommand());
		
		return cmd; 
	}
	/**
	 * Query the state of the BluetoothConnection
	 *
	 * @param bluetoothConnection TODO
	 * @return an integer representing the states, see STATE_* values
	 */
	public int getState (BluetoothConnection bluetoothConnection) {
		return bluetoothConnection.state;
	}
	boolean _recv(SudowarsBluetoothSocket sudowarsBluetoothSocket, byte[] data) {
		if (sudowarsBluetoothSocket.internalState != INTERNAL_STATE.STATE_CONNECTED) 
			return false;
		if (sudowarsBluetoothSocket.inp == null)
			return false;
		try {
			sudowarsBluetoothSocket.inp.read(data);
			DebugHelper.log(DebugHelper.PackageName.BluetoothConnection, "Read " + data.length + " Bytes");
		} catch (IOException e) {
			if (data.length < 30)
				sudowarsBluetoothSocket.close();
			return false;
		}
		return true;
	}
}
