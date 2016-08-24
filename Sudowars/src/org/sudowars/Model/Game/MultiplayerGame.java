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

import java.util.ArrayList;

import org.sudowars.DebugHelper;
import org.sudowars.R;
import org.sudowars.R.string;
import org.sudowars.Controller.Local.Activity.MultiplayerPlay;
import org.sudowars.Controller.Local.Activity.MultiplayerSettings;
import org.sudowars.Controller.Remote.BluetoothConnection;
import org.sudowars.Controller.Remote.BluetoothServer;
import org.sudowars.Model.CommandManagement.Command;
import org.sudowars.Model.CommandManagement.MultiplayerSettingsCommands.KickMultiplayerClientCommand;
import org.sudowars.Model.CommandManagement.MultiplayerSettingsCommands.KickMultiplayerClientCommand.KickStatus;
import org.sudowars.Model.Sudoku.Sudoku;
import org.sudowars.Model.Sudoku.Field.Cell;
import org.sudowars.Model.Sudoku.Field.DataCell;
import org.sudowars.Model.SudokuManagement.IO.FileIO;
import org.sudowars.Model.SudokuUtil.GameState;
import org.sudowars.Model.SudokuUtil.NoteManager;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.widget.Toast;

public class MultiplayerGame extends Game {
	
	private static final long serialVersionUID = -4259255707301239439L;
	private static final int PLAYER_COUNT = 2;
	private static final int POSITIVE_INCREMENT = 1;
	private static final int NEGATIVE_INCREMENT = 2;
	
	/**
	 * Initializes a new instance of the {@link MultiplayerGame} class with a given sudoku.
	 *
	 * @param sudoku The {@link Sudoku} to solve during the game.
	 *
	 * @throws IllegalArgumentException if the given sudoku is <code>null</code>
	 */
	public MultiplayerGame(Sudoku<Cell> sudoku) throws IllegalArgumentException{
		super(sudoku);
		this.participatingPlayers = new ArrayList<PlayerSlot>(PLAYER_COUNT);
		this.participatingPlayers.add(createPlayerSlot());
		this.participatingPlayers.add(createPlayerSlot());
	}
		
	/**
	 * Creates a new, empty {@link MultiplayerPlayerSlot}.
	 *
	 * @return Reference to a {@link MultiplayerPlayerSlot} which has not yet been attached to a player.
	 */
	protected MultiplayerPlayerSlot createPlayerSlot() {
		return new MultiplayerPlayerSlot();
	}
	
	/**
	* Aborts the current game and exposes all cells which have not been solved yet.<br>
	* All exposed cells are attached to the opponent of the given player, but without dispersing a score for that
	* The call of this method triggers the onChange event followed by the onGameAborted event.
	* @param abortingPlayer reference to the {@link Player} who triggered the abandonment. 
	* @param timestamp A Long value indicating the abandonment time. 
	* @throws IllegalArgumentException if no player has yet been attached to the game,<br>
	* or the given player is not equal to one of the players attached 
	*/
	@Override
	public void abortGame(Player abortingPlayer, long timestamp) throws IllegalArgumentException {
		PlayerSlot playerSlot = getPlayerSlotOfPlayer(abortingPlayer);
		if (playerSlot != null && !isAborted()) {
			//get the opponent of the aborting player
			PlayerSlot slotToExpose = this.participatingPlayers.get(0).equals(playerSlot) ? this.participatingPlayers.get(1) : this.participatingPlayers.get(0);
			//expose all cells first, but do not disperse points for that
			exposeAllCells(this.sudoku, slotToExpose, timestamp);
			onChange(null);
			playerSlot.onGameAborted(this);
		}
	}
	
	/**
	 * Attaches the given cell to the given player, if the given value is equal to the cell's underlying solution
	 * and the cell's owner is still pending.<br>
	 * In case of success, the onChange event is triggered.
	 * If additionally the sudoku is now successfully solved, the the onSuccessfullyFinish event is triggered.
	 * @param player Reference to the <code>Player</code> who shall be attached to the given cell.
	 * @param cell Reference to the <code>GameCell</code> who is affected.
	 * @param value The value of the cell.
	 * @return {@code true}, in case of success, otherwise {@code false}
	 * @throws IllegalArgumentException if given player or game cell was {@code null}, <br>
	 * or the given value was less or equal than constant defined by {@link DataCell#NOT_SET},<br>
	 * or the given cell doesn't fit to the game, i.e. the cell isn't part of the underlying field
	 * @throws IllegalArgumentException if no player has yet been attached to the game,<br>
	 * or the given player is not equal to one of the players attached
	 * @throws IllegalStateException if the cell is already attached to a player
	 * @see Game#onChange(GameCell)
	 * @see #MISSING()
	 * @see GameCell#isOwnerPending()
	 * @see GameCell#attachToPlayer(PlayerSlot)
	 */
	public boolean attachCellToPlayer(Player player, GameCell cell, int value) throws IllegalArgumentException, IllegalStateException {
		if (cell == null || player == null || value <= DataCell.NOT_SET) {
			throw new IllegalArgumentException("invalid argument given.");
		}
		MultiplayerPlayerSlot involvedPlayer = (MultiplayerPlayerSlot) getPlayerSlotOfPlayer(player);
		GameCell gameCell = getGameCellByIndex(cell.getIndex(), this.sudoku.getField());
		if (!gameCell.isOwnerPending()) throw new IllegalStateException();
		
		boolean result = false;
		if (!this.isAborted()) {
			if (value == gameCell.getSolution()) {
				result = gameCell.attachToPlayer(involvedPlayer);
				involvedPlayer.getScore().increment(POSITIVE_INCREMENT);
				onChange(gameCell);
				if (successfullySolved(this.sudoku)) {
					player.onSuccessfullyFinish(this);
				}
			}
		}
		return result;
	}
	
	/**
	 * Set the value of the given cell, if the passed value is equal to the cell's underlying solution <br>
	 * and if the given cell is not yet attached to another player.<br>
	 * If the passed value was wrong, the score of the given player is decremented.<br>
	 * If execution was successful or the passed value was wrong, the onChange event is triggered.
	 * @param player Reference to the <code>Player</code> who sets the value.
	 * @param cell Reference to the <code>GameCell</code> whose value is to be set.
	 * @param value The new value of the cell.
	 * @param timestamp A long value indicating the time to execute this operation.
	 * @return <code>true</code> if value was successfully set, otherwise <code>false</code>,<br>
	 * which is returned iff the cell's value has already been set at an earlier point in time.
	 * @throws IllegalArgumentException if given player or game cell was {@code null}, <br>
	 * or the given value was less or equal than constant defined by {@link DataCell#NOT_SET},<br>
	 * or the given cell doesn't fit to the game, i.e. the cell isn't part of the underlying field
	 * @throws IllegalArgumentException if no player has yet been attached to the game,<br>
	 * or the given player is not equal to one of the players attached
	 * @throws IllegalArgumentException if the given timestamp was equal to or less than constant defined by {@link GameCell#TIMESTAMP_UNSET}
	 * @throws IllegalStateException if the given cell is initial
	 * @see Game#onChange() 
	 * @see MultiplayerGame#attachCellToPlayer(Player, GameCell, int)
	 * @see GameCell#isOwnerPending()
	 * @see GameCell#getTimestamp()
	 * @see GameCell#isInitial()
	 */
	@Override
	public boolean setValue(Player player, GameCell cell, int value, long timestamp) throws IllegalArgumentException, IllegalStateException {
		if (player == null || cell == null || value <= DataCell.NOT_SET) {
			throw new IllegalArgumentException("invalid argument given.");
		}
		MultiplayerPlayerSlot involvedPlayer = (MultiplayerPlayerSlot) getPlayerSlotOfPlayer(player);
		GameCell gameCell = getGameCellByIndex(cell.getIndex(), this.sudoku.getField());
				
		boolean result = false;
		if (!this.isAborted()) {
			if (gameCell.getOwningPlayer() != null) throw new IllegalStateException();
			//check if value was correct
			if (gameCell.getSolution() == value) {
				if (gameCell.isOwnerPending()) {
					if (timestamp < gameCell.getTimestamp()) {
						gameCell.setValue(value, timestamp);
						result = true;
					} 
				} else {
					gameCell.setValue(value, timestamp);
					result = true;
				}
				if (result) {
					onChange(gameCell);
				}
			} else {
				involvedPlayer.getScore().decrement(NEGATIVE_INCREMENT);
				onChange(gameCell);
			}
		}
		return result;
	}
		
	/**
	 * Attaches the first player to the current game.
	 *
	 * @param firstPlayer The {@link Player} playing the current game.
	 *
	 * @throws IllegalArgumentException if given player is <code>null</code>
	 */
	public void setFirstPlayer(Player firstPlayer) throws IllegalArgumentException {
		this.participatingPlayers.get(0).setPlayer(firstPlayer);
	}
	
	/**
	 * Attaches the second player to the current game.
	 *
	 * @param player The {@link secondPlayer} playing the current game.
	 *
	 * @throws IllegalArgumentException if given player is <code>null</code>
	 */
	public void setSecondPlayer(Player secondPlayer) throws IllegalArgumentException {
		this.participatingPlayers.get(1).setPlayer(secondPlayer);
	}
	
	/**
	 * Gets the current score of the given player.
	 * @param player reference to a {@link Player}
	 * @return the {@link Score} of the given player
	 * @throws IllegalArgumentException if given player was {@code null},<br>
	 * or doesn't participate at the current game
	 * @see Score#getCurrentScore()
	 */
	public Score getScoreOfPlayer(Player player) throws IllegalArgumentException {
		MultiplayerPlayerSlot slot = (MultiplayerPlayerSlot) getPlayerSlotOfPlayer(player);
		return slot.getScore();
	}

	/**
	 * Swaps the players participating at the game, i.e. the first player becomes the second and vice versa.
	 * @throws IllegalStateException if less than two players are participating
	 */
	public void swapSlots() throws IllegalStateException {
		if (this.participatingPlayers.size() == 2) {
			PlayerSlot ps1 = this.participatingPlayers.get(0);
			PlayerSlot ps2 = this.participatingPlayers.get(1);
			this.participatingPlayers = new ArrayList<PlayerSlot>(PLAYER_COUNT);
			this.participatingPlayers.add(ps2);
			this.participatingPlayers.add(ps1);
		} else {
			throw new IllegalStateException();
		}
			
	}

	/**
	 * Running on a click on button {@link btnKick}.
	 * @param multiplayerSettings TODO
	 */
	public void onBtKickClick(MultiplayerSettings multiplayerSettings) {
		if (multiplayerSettings.connection instanceof BluetoothServer && multiplayerSettings.connection.sckEvent.getState(multiplayerSettings.connection) == BluetoothConnection.STATE_CONNECTED) {
			KickMultiplayerClientCommand command = new KickMultiplayerClientCommand(KickStatus.KICK);
			((BluetoothServer) multiplayerSettings.connection).sendCommandAsync((Command) command);
			((BluetoothServer) multiplayerSettings.connection).kick();
			multiplayerSettings.tglRemoteReady.setChecked(false);
			multiplayerSettings.tglLocalReady.setChecked(false);
			Toast.makeText(multiplayerSettings.getApplicationContext(), string.text_kick, Toast.LENGTH_SHORT).show();
			
			DebugHelper.log(DebugHelper.PackageName.MultiplayerSettings, "Remote kicked.");
		}
	}

	/**
	 * Starts the Game, if local and remote are ready
	 * @param multiplayerSettings TODO
	 */
	public void startGame(MultiplayerSettings multiplayerSettings) {
		if (multiplayerSettings.tglLocalReady.isChecked() && multiplayerSettings.tglRemoteReady.isChecked()) {
			multiplayerSettings.tglLocalReady.setClickable(false);
			
			FileIO savedGames = new FileIO(multiplayerSettings.getApplicationContext());
			Intent intent = new Intent(multiplayerSettings, MultiplayerPlay.class);
			
			Player localPlayer = new Player(BluetoothAdapter.getDefaultAdapter().getAddress() == null ?
							"Local":BluetoothAdapter.getDefaultAdapter().getAddress());
			Player remotePlayer = new Player(multiplayerSettings.connection.sckEvent.getRemoteDeviceName(multiplayerSettings.connection) == null ?
							"Remote":multiplayerSettings.connection.sckEvent.getRemoteDeviceName(multiplayerSettings.connection));
			
			setFirstPlayer(localPlayer);
			setSecondPlayer(remotePlayer);
			
			if (multiplayerSettings.settings.isNewGame()) {
				localPlayer.setNoteManagerOfPlayer(this, new NoteManager());
				remotePlayer.setNoteManagerOfPlayer(this, new NoteManager());
			}
			
			savedGames.saveMultiplayerGame(new GameState(this, multiplayerSettings.connection.decodeDifficulty(multiplayerSettings)));
			
			DebugHelper.log(DebugHelper.PackageName.MultiplayerSettings, "Size: "
					+ getSudoku().getField().getStructure().toString()
					+ " Difficulty: " + multiplayerSettings.connection.decodeDifficulty(multiplayerSettings).toString());
			
			multiplayerSettings.startActivity(intent);
			multiplayerSettings.finish();
		}
	}

	/**
	 * Kicked the user
	 * 
	 * @param multiplayerSettings TODO
	 * @param ban the status, if a user was kicked
	 */
	public void onKick(MultiplayerSettings multiplayerSettings, boolean ban) {
		if (ban) {
			Toast.makeText(multiplayerSettings.getApplicationContext(), string.text_banned, Toast.LENGTH_LONG).show();
			DebugHelper.log(DebugHelper.PackageName.MultiplayerSettings, "Local was banned.");
		} else {
			Toast.makeText(multiplayerSettings.getApplicationContext(), string.text_kicked, Toast.LENGTH_LONG).show();
			DebugHelper.log(DebugHelper.PackageName.MultiplayerSettings, "Local was kicked.");
		}
		
		multiplayerSettings.finish();
	}
}
