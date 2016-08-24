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
package org.sudowars.Model.Difficulty;

import java.io.File;
import java.io.Serializable;

import org.sudowars.Model.Sudoku.Field.FieldStructure;
import org.sudowars.Model.SudokuManagement.Pool.SudokuFilePool;

/**
 * This class defines the difficulty of a {@link Sudoku}. 
 * 
 * To calculate the difficulty a {@link DifficultyEvaluator} solve the given Sudoku 
 * and have to interpret the steps necessary to find a solution. This rating is 
 * represented by a double value which is independent from the {@link FieldStructure}. 
 * This class provides the functionality to group these values and build difficulty 
 * classes (e.g. easy or hard) to use out of the package.
 */
public abstract class Difficulty implements Serializable {
	
	private static final long serialVersionUID = -5476646547089500717L;
	
	protected double lowerBound;
	protected double upperBound;
	private double value = (this.upperBound + this.lowerBound) / 2.0;
	
	/**
	 * Returns the lower bound of the difficulty
	 * @return the lower bound of the difficulty
	 */
	public double getLowerBound() {
		return this.lowerBound;
	}

	/**
	 * Returns the higher bound of the difficulty
	 * @return the lower bound of the difficulty
	 */
	public double getUpperBound() {
		return this.upperBound;
	}
	
	/**
	 * Gets the difficulty value of the current instance, if no value is set the arithmetic mean of 
	 * the difficulty bounds will be returned
	 * @return the difficulty value
	 * @see Difficulty#setValue(double)
	 */
	public double getValue() {
		return this.value;
	}
	
	/**
	 * Sets the difficulty value of the current instance.
	 * @param newValue the new value
	 * @throws IllegalArgumentException if given value was out of bounds of the current 
	 * 				difficulty type (lowerBound <= Value < upperBound)
	 * @see Difficulty#getValue(double)
	 */
	public void setValue(double newValue) throws IllegalArgumentException {
		if (newValue < this.lowerBound || newValue >= this.upperBound) {
			throw new IllegalArgumentException("given value to set was out of bounds.");
		}
		this.value = newValue;
	}
	
	/**
	 * Returns an integer hash code for this object.
	 * @return this object's hash code.
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(lowerBound);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(upperBound);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	/**
	 * Compares this instance with the specified object and indicates if they are equal.
	 * @param obj the object to compare this instance with.
	 * @return <code>true</code> if the specified object is equal to this object, <code>false</code> otherwise.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Difficulty other = (Difficulty) obj;
		if (Double.doubleToLongBits(lowerBound) != Double
				.doubleToLongBits(other.lowerBound))
			return false;
		if (Double.doubleToLongBits(upperBound) != Double
				.doubleToLongBits(other.upperBound))
			return false;
		return true;
	}

	public int getSudokuCount(FieldStructure structure, SudokuFilePool sudokuFilePool) {
		assert structure != null && this != null;
		
		int result = 0;
		File directory = getDirectoryForSudokuType(structure, sudokuFilePool);
		if (directory != null) {
			result = directory.listFiles().length;
		}
		return result;
	}

	public File getDirectoryForSudokuType(FieldStructure structure, SudokuFilePool sudokuFilePool) {
		assert structure != null && this != null;
		
		File result = null;
		
		File temp = new File(sudokuFilePool.rootDirectory, String.valueOf(structure.getWidth()) + String.valueOf(structure.getHeight()) + "/" + toString());
		for (File dir : sudokuFilePool.dirs) {
			if (dir.getAbsolutePath().equals(temp.getAbsolutePath())) {
				result = dir;
				break;
			}
		}
		return result;
	}

	/**
	 * Returns the difficulty code
	 * 
	 * @return the difficulty code
	 */
	public int encodeDifficulty() {		
		int code;
		
		if (this instanceof DifficultyEasy) {
			code = 0;
		} else if (this instanceof DifficultyMedium) {
			code = 1;
		} else {
			code = 2;
		}
		
		return code;
	}
	
}


