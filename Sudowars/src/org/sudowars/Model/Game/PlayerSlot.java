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
package org.sudowars.Model.Game;

import java.io.Serializable;

import org.sudowars.Model.SudokuUtil.NoteManager;


/**
 * This class defines a slot for a player during a game. 
 */
public class PlayerSlot implements Serializable {

	/**
	 * 
	 */
	protected boolean hasPaused;
	protected Player attachedPlayer = null;
	protected NoteManager notes = null;
	
	/**
	 * Attaches to current slot to a specific player.
	 *
	 * @param player Reference to a <code>Player</code> which will be attached to this slot.
	 *
	 * @throws IllegalArgumentException if specified player is <code>null</code>
	 */
	public void setPlayer(Player player) throws IllegalArgumentException {
		if (player == null) {
			throw new IllegalArgumentException("player to attach to slot cannot be null.");
		}
		this.attachedPlayer = player;
	}
	
	/**
	 * Gets the player currently attached to this instance.
	 *
	 * @return Reference to a {@link Player}.
	 */
	public Player getPlayer() {
		return this.attachedPlayer;
	}
	
	/**
	 * Gets the {@link NoteManager} associated with the current instance.
	 *
	 * @return Reference to a note manager instance.
	 */
	public NoteManager getNoteManager() {
		return this.notes;
	}
	
	/**
	 * Sets the paused state of this slot indicating of the player attached to it triggered a pause.
	 *
	 * @param state A boolean flag to indicate if a paused was triggered.
	 */
	void setPausedState(boolean state) {
		this.hasPaused = state;
	}
	
	/**
	 * Sets the {@link NoteManager} of the current instance.
	 *
	 * @param noteManager Reference to a note manager instance.
	 *
	 * @throws IllegalArgumentException if given reference is <code>null</code>
	 */
	void setNoteManager(NoteManager noteManager) throws IllegalArgumentException {
		if (noteManager == null) {
			throw new IllegalArgumentException("noteManager to set cannot be null.");
		}
		this.notes = noteManager;
	}

	/**
	 * Method to distribute the onGameAborted event to all attached listeners.
	 * @param game TODO
	 */
	protected final void onGameAborted(Game game) {
		assert this != null && getPlayer() != null && game.participatingPlayers.contains(this);
		
		Player abortingPlayer = getPlayer();
		abortingPlayer.pauseGame(game);
		game.isAborted = true;
		game.abortingPlayerSlot = this;
		GameAbortedEvent eventData = new GameAbortedEvent(game, abortingPlayer);
		for (GameAbortedEventListener listener : game.registeredOnGameAbortObservers) {
			listener.onGameAborted(eventData);
		}
	}
	
	
	
	
	
	
	//COLLAPSE HIERARCHY
	private static final long serialVersionUID = 7389208174636844121L;

	/**
	 * Indicates whether the player attached to this slot triggered a pause.
	 *
	 * @return <code>true</code> if pause was triggered, otherwise <code>false</code>
	 */
	public boolean hasPaused() {
		return hasPaused;
	}
	
	/**
	 * Gets the hashcode of the current instance.
	 * @return an integer value indicating the hashcode of the current instance
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((this.attachedPlayer == null) ? 0 : this.attachedPlayer.hashCode());
		result = prime * result + (this.hasPaused ? 1231 : 1237);
		//result = prime * result + ((notes == null) ? 0 : notes.hashCode());
		return result;
	}
	
	/**
	 * Indicates whether the current instance is equal to a given object.
	 * @param otherObject reference to an object to check for equality
	 * @return {@code true} if given obejct refers to the same instance,<br>
	 * or refers to another SingleplayerPlayerSlot instance with equal attributes/properties,<br>
	 * otherwise {@code false}
	 */
	@Override
	public boolean equals(Object otherObject) {
		boolean result = false;
		if (otherObject instanceof PlayerSlot) {
			PlayerSlot otherSlot = (PlayerSlot) otherObject;
			result = (this == otherSlot || attributesEqual(this, otherSlot));
		}
		return result;
	}
	
	private static boolean attributesEqual(PlayerSlot first, PlayerSlot second) {
		assert first != null && second != null;
		
		return (first.hasPaused == second.hasPaused && objectsEqual(first.attachedPlayer, second.attachedPlayer)
				&& objectsEqual(first.notes, second.notes));
		
	}
	
	/**Compares to objects taking into consideration that at least one of them may be null.
	 * 
	 * @param first reference to an object
	 * @param second reference to an object
	 * @return {@code true}, if both objects are {@code null} or {@code first.equals(second) == true},<br>
	 * otherwise {@code false}
	 */
	static boolean objectsEqual(Object first, Object second) {
		//taking into consideration that at least one of them may be null during runtime
		boolean result = true;
		if (first == null) {
			if (second != null) {
				result = false;
			}
		} else if(second == null) {
			if (first != null) {
				result = false;
			}
		}
		else {
			//none is null
			result = first.equals(second);
		}
		return result;
	}
}
