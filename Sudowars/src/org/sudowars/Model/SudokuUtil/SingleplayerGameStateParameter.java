package org.sudowars.Model.SudokuUtil;

import org.sudowars.Model.CommandManagement.DeltaManager;
import org.sudowars.Model.Difficulty.Difficulty;
import org.sudowars.Model.Game.SingleplayerGame;
import org.sudowars.Model.Sudoku.Field.DataCell;
import org.sudowars.Model.Sudoku.Field.Field;

public class SingleplayerGameStateParameter {
	public SingleplayerGame game;
	public Difficulty difficulty;
	public boolean obviousMistakesShown;
	public boolean solveCellAllowed;
	public boolean bookmarkAllowed;
	public boolean backToFirstError;
	public Field<DataCell> correctSolvedField;
	public DeltaManager deltaManager;

	public SingleplayerGameStateParameter(SingleplayerGame game,
			Difficulty difficulty, boolean obviousMistakesShown,
			boolean solveCellAllowed, boolean bookmarkAllowed,
			boolean backToFirstError, Field<DataCell> correctSolvedField,
			DeltaManager deltaManager) {
		this.game = game;
		this.difficulty = difficulty;
		this.obviousMistakesShown = obviousMistakesShown;
		this.solveCellAllowed = solveCellAllowed;
		this.bookmarkAllowed = bookmarkAllowed;
		this.backToFirstError = backToFirstError;
		this.correctSolvedField = correctSolvedField;
		this.deltaManager = deltaManager;
	}
}