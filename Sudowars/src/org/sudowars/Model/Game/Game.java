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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.graphics.Canvas;
import android.os.SystemClock;

import org.sudowars.Model.Sudoku.Sudoku;
import org.sudowars.Model.Sudoku.Field.Cell;
import org.sudowars.Model.Sudoku.Field.Field;
import org.sudowars.Model.Sudoku.Field.FieldBuilder;
import org.sudowars.Model.Sudoku.RuleManagement.DependencyManager;
import org.sudowars.View.SudokuField;

/**
 * This class provides fundamental game functionality.
 */
public abstract class Game implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5181586542160404692L;
	
	transient private List<GameChangedEventListener> registeredOnChangeObservers;
	transient List<GameFinishedEventListener> registeredOnFinishObservers;
	transient private List<StopWatchTickEventListener> registeredOnStopWatchTickObservers;
	transient List<GameAbortedEventListener> registeredOnGameAbortObservers;
	
	protected final Sudoku<GameCell> sudoku;
	protected List<PlayerSlot> participatingPlayers;
	
	final StopWatch stopwatch;
	
	boolean isPaused = true;
	boolean isStarted = false;
	boolean isAborted = false;
	PlayerSlot abortingPlayerSlot = null;
	
	/**
	 * Initializes a new instance of the {@link Game} class.
	 *
	 * @param sudoku A reference to a {@link Sudoku} to use during the game.
	 *
	 * @throws IllegalArgumentException if the given sudoku is <code>null</code>
	 */
	public Game(Sudoku<Cell> sudoku) throws IllegalArgumentException {
		if (sudoku == null) {
			throw new IllegalArgumentException("given sudoku cannot be null.");
		}
		this.sudoku = createGameSudoku(sudoku.getField(), sudoku.getDependencyManager());
		this.stopwatch = new GameStopWatch(this);
		initializeObserverLists();
	}
		
	private void initializeObserverLists() {
		this.registeredOnChangeObservers = new LinkedList<GameChangedEventListener>();
		this.registeredOnFinishObservers = new LinkedList<GameFinishedEventListener>();
		this.registeredOnStopWatchTickObservers = new LinkedList<StopWatchTickEventListener>();
		this.registeredOnGameAbortObservers = new LinkedList<GameAbortedEventListener>();
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		initializeObserverLists();
	}
	
	private static Sudoku<GameCell> createGameSudoku(Field<Cell> field, DependencyManager dependencies) {
		assert field != null && dependencies != null;
		
		GameCellBuilder gameCellBuilder = new GameCellBuilder(field);
		Field<GameCell> sudokuField = new FieldBuilder<GameCell>().build(field.getStructure(), gameCellBuilder);
		return new Sudoku<GameCell>(sudokuField, dependencies);
	}
	
	/**
	 * Creates a new, empty {@link PlayerSlot}
	 *
	 * @return A reference to a {@link PlayerSlot} which has not yet been attached to a player.
	 */
	protected abstract PlayerSlot createPlayerSlot();
	
	/**
	 * Method to distribute the onTick event of the game's stopwatch to all attached listeners.
	 */
	private void onStopWatchTick(int tickCount, long elapsedMilliseconds) {
		for (StopWatchTickEventListener listener : this.registeredOnStopWatchTickObservers) {
			listener.onTick(tickCount, elapsedMilliseconds);
		}
	}
	
	/**
	 * Method to distribute the onChange event to all attached listeners.
	 */
	protected final void onChange(GameCell changedCell) {
		GameChangedEvent eventData = new GameChangedEvent(this, changedCell);
		//inform all attached listeners here
		for (GameChangedEventListener listener : this.registeredOnChangeObservers) {
			listener.onGameChanged(eventData);
		}
	}
	
	/**
	 * Gets the sudoku of the game
	 *
	 * @return A reference to a {@link Sudoku}.
	 */
	public Sudoku<GameCell> getSudoku() {
		return this.sudoku;
	}
	

	/**
	 * Gets the elapsed time of the current game.
	 *
	 * @return The elapsed time, in milliseconds.
	 * @see Game#addOnStopWatchTickListener(StopWatchTickEventListener)
	 */
	public long getGameTime() {
		return this.stopwatch.getElapsedTime();
	}
	
	/**
	 * Gets a list containing the {@link Player}s participating at the game.
	 *
	 * @return A READ-ONLY list containing the {@link Player}s participating at the game,<br>
	 * which is empty if no players have joined yet  
	 */
	public final List<Player> getPlayers() {
		ArrayList<Player> players = new ArrayList<Player>(this.participatingPlayers.size());
		Player player = null;
		for (PlayerSlot slot : this.participatingPlayers) {
			player = slot.getPlayer();
			if (player != null) {
				players.add(player);
			}
		}
		return Collections.unmodifiableList(players);
	}
	
	/**
	 * Adds the given observer to the onGameAborted observer list of the current instance.
	 *
	 * @param actionListener A reference to an {@link GameAbortedEventListener} instance.
	 *
	 * @return <code>true</code>, if the execution was successfully
	 * @see List#add(Object)
	 */
	public final boolean addOnGameAbortListener(GameAbortedEventListener actionListener) {
		return this.registeredOnGameAbortObservers.add(actionListener);
	}
	
	/**
	 * Removes the first occurrence of the specified element from the onGameAborted observer list of the current instance.
	 *
	 * @param listener The {@link GameAbortedEventListener} to remove.
	 *
	 * @return <code>true</code>, if element was contained in the list, otherwise <code>false</code>
	 * @see List#remove(Object)
	 */
	public final boolean removeOnGameAbortListener(GameFinishedEventListener listener) {
		return this.registeredOnGameAbortObservers.remove(listener);
	}
	
	/**
	 * Adds the given observer to the onChange observer list of the current instance.
	 *
	 * @param actionListener A reference to an {@link GameChangedEventListener} instance.
	 *
	 * @return <code>true</code>, if the execution was successfully
	 * @see List#add(Object)
	 */
	public final boolean addOnChangeListener(GameChangedEventListener actionListener) {
		return this.registeredOnChangeObservers.add(actionListener);
	}
	
	/**
	 * Adds the given observer to the onSuccessfullyFinish observer list of the current instance, <br>
	 * which is fired, if the game's sudoku was successfully solved.
	 *
	 * @param actionListener A reference to an {@link GameFinishedEventListener} instance.
	 *
	 * @return <code>true</code>, if the execution was successfully
	 * @see List#add(Object)
	 */
	public final boolean addOnSuccessfullyFinishListener(GameFinishedEventListener actionListener) {
		return this.registeredOnFinishObservers.add(actionListener);
	}
	
	/**
	 * Adds the given observer to the onStopWatchTick observer list of the stop watch of current instance.
	 *
	 * @param actionListener A reference to an {@link StopWatchTickEventListener} instance.
	 *
	 * @return <code>true</code>, if the execution was successfully
	 * @see List#add(Object)
	 */
	public final boolean addOnStopWatchTickListener(StopWatchTickEventListener actionListener) {
		return this.registeredOnStopWatchTickObservers.add(actionListener);
	}
	
	/**
	 * Removes the first occurrence of the specified element from the onSuccessfullyFinish observer list of the current instance.
	 *
	 * @param listener The {@link GameFinishedEventListener} to remove.
	 *
	 * @return <code>true</code>, if element was contained in the list, otherwise <code>false</code>
	 * @see List#remove(Object)
	 */
	public final boolean removeOnSuccessfullyFinishListener(GameFinishedEventListener listener) {
		return this.registeredOnFinishObservers.remove(listener);
	}
	
	/**
	 * Removes the first occurrence of the specified element from the onChange observer list of the current instance.
	 *
	 * @param listener The {@link GameChangedEventListener} to remove.
	 *
	 * @return <code>true</code>, if element was contained in the list, otherwise <code>false</code>
	 * @see List#remove(Object)
	 */
	public final boolean removeOnChangeListener(GameChangedEventListener listener) {
		return this.registeredOnChangeObservers.remove(listener);
	}
	
	/**
	 * Removes the first occurrence of the specified element from the onStopWatchTick observer list of stop watch of the current instance.
	 *
	 * @param listener The {@link StopWatchTickEventListener} to remove.
	 *
	 * @return <code>true</code>, if element was contained in the list, otherwise <code>false</code>
	 * @see List#remove(Object)
	 */
	public final boolean removeOnStopWatchTickListener(StopWatchTickEventListener listener) {
		return this.registeredOnStopWatchTickObservers.remove(listener);
	}
	
	/**
	 * Indicates whether the given player triggered the current game pause. 
	 * @param player A reference to a {@link Player}.
	 * @return {@code true}, if the player triggered the current game pause, <br>
	 * {@code false}, if the given player didn't trigger the pause, or the game is not paused
	 * @throws IllegalArgumentException if given player was {@code null} or doesn't participate
	 */
	public boolean hasPaused(Player player) throws IllegalArgumentException {
		PlayerSlot slot = getPlayerSlotOfPlayer(player);
		
		if (isPaused()) {
			return slot.hasPaused;
		}
		return false;
	}
	
	/**Starts the game.
	*/
	public final void startGame() {
		if (!this.isAborted) {
			this.isPaused = false;
			this.isStarted = true;
			this.stopwatch.start();
		}
	}
	
	/**
	* Aborts the current game and exposes all cells which have not been solved yet.<br>
	* The call of this method should trigger the onChange event followed by the onGameAborted event.
	* @param abortingPlayer reference to the {@link Player} who triggered the abandonment. 
	*  @param timestamp A Long value indicating the abandonment time. 
	* @throws IllegalArgumentException if no player has yet been attached to the game,<br>
	* or the given player is not equal to one of the players attached 
	*/
	public abstract void abortGame(Player abortingPlayer, long timestamp) throws IllegalArgumentException; 
	
	/**
	 * Gets the player slot of a specified player.
	 * @param player reference to a {@link Player}
	 * @return the underlying {@link PlayerSlot} of the given player
	 * @throws IllegalArgumentException if given player was {@code null}<br>
	 * or no players participate at the game,<br>
	 * or the given player doesn't participate
	 */
	protected PlayerSlot getPlayerSlotOfPlayer (Player player)	throws IllegalArgumentException {
		if (player == null) {
			throw new IllegalArgumentException("given player was null.");
		}
		if (this.participatingPlayers == null || this.participatingPlayers.isEmpty()) {
			throw new IllegalArgumentException("no players participating.");
		}
		PlayerSlot slot = getPlayerSlotOfPlayer(player, this.participatingPlayers);
		if (slot == null) {
			throw new IllegalArgumentException("given player doesn't participate.");
		}
		return slot;
	}
	
	private static PlayerSlot getPlayerSlotOfPlayer(Player player, List<PlayerSlot> participatingPlayers) {
		assert player != null && participatingPlayers != null;
		
		for (PlayerSlot playerSlot : participatingPlayers) {
			if (player.equals(playerSlot.getPlayer())) {
				return playerSlot;
			}
		}
		return null;
	}
	
	/**
	 * Indicates whether the current game is aborted, i.e. not finished successfully
	 * @return {@code true} if game was aborted, otherwise {@code false}
	 */
	public boolean isAborted() {
		return this.isAborted;
	}
	
	/**
	 * Gets the player who aborted the game, if exists.
	 * @return reference to the {@link Player} who aborted the game, {@code null}, if game was not yet aborted
	 */
	public Player getAbortingPlayer() {
		if (this.isAborted) return this.abortingPlayerSlot.getPlayer();
		return null;
	}
	
	/**
	 * Indicates whether the game is currently paused.
	 *
	 * @return <code>true</code> if game is paused or not yet started, otherwise <code>false</code>
	 */
	public boolean isPaused() {
		return this.isPaused || !this.isStarted;
	}
	
	/**
	 * Indicates whether the game is currently started.
	 * @return <code>true</code> if game has been started
	 */
	public boolean isStarted() {
		return this.isStarted;
	}
	/**
	 * Sets the value of the given cell and attaches it to the given player.
	 *
	 * @param player A reference to the <code>Player</code> who sets the value.
	 * @param cell A reference to the <code>null</code> whose value is to be set.
	 * @param value The new value of the cell.
	 *
	 * @return <code>true</code> if value was successfully set, otherwise <code>false</code>
	 * @throws IllegalArgumentException if value is equal or less than {@link DataCell#NOT_SET}
	 * @throws IllegalArgumentException if player or cell is {@code null}
	 */
	public abstract boolean setValue(Player player, GameCell cell, int value, long timestamp) throws IllegalArgumentException;
	
	/**
	 * Gets the {@link GameCell} of a field with the given index.
	 * @param index an integer value indicating the cell's index
	 * @param field a field containing {@code GameCell}s
	 * @return reference to the {@link GameCell} with the given index
	 * @throws IllegalArgumentException  if the given index is out of bounds
	 * defined by the underlying field structure
	 */
	protected static GameCell getGameCellByIndex(int index, Field<GameCell> field) throws IllegalArgumentException {
		assert index >= 0 && field != null;
		
		return field.getCell(index);
	}
	
	protected static boolean successfullySolved(Sudoku<GameCell> sudoku) {
		assert sudoku != null;
		
		boolean result = false;
		Field<GameCell> field = sudoku.getField();
		if (field.isFilled()) {
			result = true;
			for (GameCell cell : field.getCells()) {
				if (cell.getValue() != cell.getSolution()) {
					result = false;
					break;
				}
			}
		}
		return result;
	}
	
	/**
	 * Exposes all cells which have not been solved by any of the players yet.
	 * @param sudoku the game's sudoku
	 * @param playerSlot the player who is expected to own all cells to be exposed
	 */
	protected static void exposeAllCells(Sudoku<GameCell> sudoku, PlayerSlot playerSlot, long timestamp) {
		assert sudoku != null && playerSlot != null;
		for (GameCell currentCell : sudoku.getField().getCells()) {
			if (currentCell != null && !currentCell.isInitial() && currentCell.getValue() != currentCell.getSolution()) {
				currentCell.setValue(currentCell.getSolution(), timestamp);
				currentCell.attachToPlayer(playerSlot);
			}
		}
	}
	
	private final class GameStopWatch extends StopWatch implements Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = -5802064952119912158L;
		private final Game game;
		
		GameStopWatch(Game game) {
			super();
			this.game = game;
		}
		
		@SuppressWarnings("synthetic-access")
		@Override
		public void step() {
			if (this.game != null && !this.game.isPaused() && !this.game.isAborted()) {
				this.game.onStopWatchTick(this.tickCount++, this.elapsedMilliseconds);
			}
		}
		
		private void writeObject(ObjectOutputStream out) throws IOException {
			// TODO clone!
			if (this.running) {
				//ensure elapsed time is up to date
				this.elapsedMilliseconds += (SystemClock.uptimeMillis() - this.lastLogTime);
			}
			out.defaultWriteObject();
			//out.writeLong(this.elapsedMilliseconds);
			//out.writeLong(this.tickInterval);
		}
		
		private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
			in.defaultReadObject();
			this.lastLogTime = SystemClock.uptimeMillis();
			this.running = false;
			this.tickCount--;
		}
	}
	
	/**
	 * Returns number of incorrect filled cells
	 * 
	 * @return number of incorrect filled cells
	 */
	public int getIncorrectCellsSize() {
		int mistakes = 0;
		List<GameCell> cells = this.sudoku.getField().getCells();
		
		for (GameCell c : cells) {
			if (c.isSet() && c.getValue() != c.getSolution()) {
				mistakes++;
			}
		}
		
		return mistakes;
	}
	
	/**
	 * Returns <code>true</code> if the sudoku has incorrect filled cells, otherwise <code>false</code>
	 * 
	 * @return <code>true</code> if the sudoku has incorrect filled cells, otherwise <code>false</code>
	 */
	public boolean hasIncorrectCells() {
		List<GameCell> cells = this.sudoku.getField().getCells();
		
		for (GameCell c : cells) {
			if (c.isSet() && c.getValue() != c.getSolution()) {
				return true;
			}
		}
		
		return false;
	}

	/**
	 * Disables the Field
	 * @param sudokuField TODO
	 * @param disabled is true field is disabled
	 */
	public void setDisabled(SudokuField sudokuField, boolean disabled) {
		if (disabled) {
			sudokuField.selectedFieldX = -1;
			sudokuField.selectedFieldY = -1;
			sudokuField.invalidate();
		}
		
		sudokuField.fieldDisabled = disabled;
	}

	/**
	 * Draws a dummy Canvas for the android Editor
	 * @param sudokuField TODO
	 * @param canvas Canvas to draw on
	 */
	public void drawDummy(SudokuField sudokuField, Canvas canvas) {
		
		for (int n = 0; n <= 3; n++) {
			canvas.drawLine(0, n * 3 * sudokuField.squareSize, sudokuField.getWidth(), n * 3 * sudokuField.squareSize, sudokuField.boldLinePaint);
			canvas.drawLine( n * 3 * sudokuField.squareSize, 0, n * 3 * sudokuField.squareSize, sudokuField.getHeight(), sudokuField.boldLinePaint);
		}
		
		//Drawing the normal Lines
		
		for (int n = 0; n < 9; n++) {
			if (n % 3 == 0)
				continue;
			canvas.drawLine(0, n * sudokuField.squareSize, sudokuField.getWidth(), n * sudokuField.squareSize, sudokuField.linePaint);
			canvas.drawLine( n * sudokuField.squareSize, 0, n * sudokuField.squareSize, sudokuField.getHeight(), sudokuField.linePaint);
		}
		
	}

	/**
	 * Method to test Multitouch
	 * @param sudokuField TODO
	 */
	public float getScaleFactor(SudokuField sudokuField) {
		return sudokuField.scaleFactor;
	}
}
