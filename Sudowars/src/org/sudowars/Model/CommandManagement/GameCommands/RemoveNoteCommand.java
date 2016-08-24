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
package org.sudowars.Model.CommandManagement.GameCommands;

import org.sudowars.Model.Game.Game;
import org.sudowars.Model.Game.GameCell;
import org.sudowars.Model.Game.Player;

/**
 * The class RemoveNoteCommand. It provides the possibility to remove a note from a cell.
 */
public class RemoveNoteCommand extends NoteCommand {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -6279070382750246165L;

	/**
	 * Initializes a new instance of the RemoveNoteCommand class.
	 *
	 * @param cell The cell on which the command is executed.
   * @param noteValue The value of the note used during the command.
   *
	 * @throws IllegalArgumentException if given cell is <code>null</code> or noteValue < <code>1</code>
	 */
	public RemoveNoteCommand(GameCell cell, int noteValue) throws IllegalArgumentException {
		super(cell, noteValue);
	}

	/**
	* Executes the current command on the specified game in the name of the given player and 
	* removes the note from the given cell.
	*
	* @param game A reference to game on which to execute the current instance.
	* @param executingPlayer the player in whose name to execute the command
	*
	* @return <code>true</code>, if the command was executed successfully, <code>false</code> otherwise
	*
	* @throws IllegalArgumentException if at least one of the given params is <code>null</code>
	*/
	public boolean execute(Game game, Player executingPlayer) throws IllegalArgumentException {
		if (game == null) {
			throw new IllegalArgumentException("Game is null");
		}
		if (executingPlayer == null) {
			throw new IllegalArgumentException("executingPlayer is null");
		}
		GameCell cell = game.getSudoku().getField().getCell(cellIndex);
		return  executingPlayer.getNoteManagerOfPlayer(game).removeNote(cell, noteValue);	
	}

	@Override
	public GameCommand getInvertedCommand(Game game) {
		return new AddNoteCommand(this.getCell(game), this.noteValue);
	}
}
