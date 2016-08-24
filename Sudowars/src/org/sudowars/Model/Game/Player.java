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
import java.util.List;

import org.sudowars.R;
import org.sudowars.R.color;
import org.sudowars.Controller.Local.Activity.Play;
import org.sudowars.Model.SudokuUtil.NoteManager;

/**
 * This class is used to describe a player.
 * @see PlayerSlot
 */
public class Player implements Serializable {

	private static final long serialVersionUID = -8500359963346360261L;
	private final String nickname;
	
	/**
	 * Initializes a new instance of the {@link Player} class with a given nickname,<br>
	 * which is the bluetooth name of the user's device.
	 *
	 * @param nickname The nickname of the player.
	 *
	 * @throws IllegalArgumentException if the given nickname is <code>null</code> or empty
	 */
	public Player(String nickname) throws IllegalArgumentException {
		if ((nickname == null || nickname.length() == 0)) {
			throw new IllegalArgumentException("given nickname was null.");
		}
		this.nickname = nickname;
	}
	
	/**
	 * Gets the nickname of the current instance.
	 *
	 * @return The nickname of the player.
	 */
	public String getNickname() {
		return this.nickname;
	}
	
	/**
	 * Indicates whether the current instance is equal to a given object.
	 * @param otherObject reference to an object to check for equality
	 * @return {@code true} if given obejct is a valid player instance with same nickname,<br>
	 * otherwise {@code false}
	 */
	@Override
	public boolean equals(Object otherObject) {
		boolean result = false;
		if (otherObject instanceof Player) {
			Player otherPlayer = (Player) otherObject;
			if (this == otherPlayer || this.nickname.equals(otherPlayer.nickname)) {
				result = true;
			}
		}
		return result;
	}
	
	/**
	 * Gets the hashcode of the current instance.
	 * @return an integer value indicating the hashcode of the current instance
	 */
	@Override
	public int hashCode() {
		int hashCode = 47;
		
		return 31 * hashCode + this.nickname.hashCode();
	}

	/**
	 * Resumes the current game.
	 *
	 * @param game TODO
	 * @return <code>true</code> if game is now not paused,
	 * <br><code>false</code> if given player didn't trigger the pause preliminarily, or
	 * game wasn't paused
	 * @throws IllegalArgumentException if given player was {@code null} or doesn't participate
	 * @see #MISSING()
	 */
	public boolean resumeGame(Game game) throws IllegalArgumentException {
		PlayerSlot slot = game.getPlayerSlotOfPlayer(this);
		
		if (!game.isStarted || !game.isPaused || game.isAborted) {
			return false;
		} else {
			slot.setPausedState(false);
			boolean isPaused = false;
			
			for (PlayerSlot p : game.participatingPlayers) {
				if (p.hasPaused) {
					isPaused = true;
					break;
				}
			}
			
			game.isPaused = isPaused;
			
			return !game.isPaused;
		}
	}

	/**
	 * Method to distribute the onSuccessfullyFinish event to all attached listeners.
	 * @param game TODO
	 */
	protected final void onSuccessfullyFinish(Game game) {
		assert this != null;
		pauseGame(game);
		GameFinishedEvent eventData = new GameFinishedEvent(game, this);
		//inform all attached listeners here
		for (GameFinishedEventListener listener : game.registeredOnFinishObservers) {
			listener.onGameSuccessfullyFinish(eventData);
		}
	}

	/**
	 * Pauses the current game
	 *
	 * @param game TODO
	 * @return <code>true</code> if game is now paused, otherwise <code>false</code>
	 * @throws IllegalArgumentException if given player was {@code null} or doesn't participate
	 * @see #MISSING()
	 */
	public boolean pauseGame(Game game) throws IllegalArgumentException {
		PlayerSlot slot = game.getPlayerSlotOfPlayer(this);
		boolean result = false;
		if (game.isStarted && !game.isAborted) {
			slot.setPausedState(true);
			result = true;
			if (!game.isPaused()) {
				game.isPaused = true;
				game.stopwatch.stop();
			}
		}
		return result;
	}

	/**
	 * Gets the {@link NoteManager} of the specified player.
	 * @param game TODO
	 * @return the {@link NoteManager} of the specified player
	 * @throws IllegalArgumentException if given player was {@code null},<br>
	 * or doesn't participate at the current game
	 */
	public NoteManager getNoteManagerOfPlayer(Game game) throws IllegalArgumentException {
		//the following call either throws an IllegalArgumentException or returns a value != null
		PlayerSlot slot = game.getPlayerSlotOfPlayer(this);
		return slot.getNoteManager();
	}

	/**
	 * Sets the {@link NoteManager} for a given player.
	 * @param game TODO
	 * @param noteManager reference to a {@link NoteManager}
	 * @throws IllegalArgumentException if given player was {@code null},<br>
	 * or doesn't participate at the current game,<br>
	 * or the given note manager was {@code null}
	 */
	public void setNoteManagerOfPlayer(Game game, NoteManager noteManager) throws IllegalArgumentException {
		//the following call either throws an IllegalArgumentException or returns a value != null
		PlayerSlot slot = game.getPlayerSlotOfPlayer(this);
		slot.setNoteManager(noteManager);
	}

}
