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
package org.sudowars.Controller.Local.Activity;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.sudowars.DebugHelper;
import org.sudowars.R;
import org.sudowars.Controller.Remote.BluetoothConnection;
import org.sudowars.Model.CommandManagement.*;
import org.sudowars.Model.CommandManagement.GameCommands.*;
import org.sudowars.Model.CommandManagement.MultiplayerSettingsCommands.MultiplayerPauseCommand;
import org.sudowars.Model.CommandManagement.MultiplayerSettingsCommands.RemoteReadyCommand;
import org.sudowars.Model.Game.GameCell;
import org.sudowars.Model.Game.GameChangedEvent;
import org.sudowars.Model.Game.GameChangedEventListener;
import org.sudowars.Model.Game.Player;
import org.sudowars.Model.Game.MultiplayerGame;
import org.sudowars.Model.SudokuManagement.IO.FileIO;

/**
 * Shows a running Sudoku game.
 */
public class MultiplayerPlay extends Play {
	/**
	 * flag, if counter is running
	 */
	private boolean counterIsRunning;
	
	/**
	 * the remote player
	 */
	protected Player remotePlayer;
	
	/**
	 * the countdown text, which is show on the screen
	 */
	private TextView lblCountdown;
	
	/**
	 * the pause text, which is show on the screen
	 */
	private TextView lblPauseText;
	
	/**
	 * the ready button of the local player
	 */
	private ToggleButton tglLocalReady;
	
	/**
	 * the ready button of the remote player
	 */
	private ToggleButton tglRemoteReady;
	
	/**
	 * the points of the local player
	 */
	private TextView lblLocalScore;
	
	/**
	 * the old points of the local player
	 */
	private TextView lblLocalOldScore;
	
	/**
	 * the old points of the opponend player
	 */
	private TextView lblRemoteScore;
	
	/**
	 * the points of the opponend player
	 */
	private TextView lblRemoteOldScore;
	
	/**
	 * Tricker, if local player leave the game
	 */
	private boolean playerLeftGame;
	
	/**
	 * the counter for the countdown
	 */
	private Counter counter;

	/**
	 * Animation for showing new score
	 */
	private Animation scoreInAnimation;
	
	/**
	 * Animation for hidding old score of local player
	 */
	private Animation localScoreOutAnimation;
	
	/**
	 * Animation for hidding old score of opponend player
	 */
	private Animation remoteScoreOutAnimation;
	
	/**
	 * the bluetooth API, to communicate between client and server
	 */
	private BluetoothConnection connection;
	
	/**
	 * the ready view
	 */
	private LinearLayout ready;
	
	/**
	 * the multiplayer content
	 */
	private LinearLayout play_content;
	
	/**
	 * the Bluetooth handler
	 */
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case BluetoothConnection.MESSAGE_BT_STATE_CHANGE:
				if (MultiplayerPlay.this.connection.sckEvent.getState(MultiplayerPlay.this.connection) != BluetoothConnection.STATE_CONNECTED) {
					if (MultiplayerPlay.this.counter != null) {
						MultiplayerPlay.this.counter.stop();
        			}
					
					MultiplayerPlay.this.localPlayer.pauseGame(MultiplayerPlay.this.game);
					MultiplayerPlay.this.remotePlayer.pauseGame(MultiplayerPlay.this.game);
					
					if (!MultiplayerPlay.this.gameState.isFinished()) {
						MultiplayerPlay.this.playerLeftGame = true;
					}
					
        			MultiplayerPlay.this.setupButtons();
				}

				break;
			case BluetoothConnection.MESSAGE_NEW_DATA:
				Command command = MultiplayerPlay.this.connection.sckEvent.getCurrentCommand(MultiplayerPlay.this.connection);
				
				if (command != null) {
					if (command instanceof GameCommand) {
						if (((GameCommand) command).execute(MultiplayerPlay.this.game, MultiplayerPlay.this.remotePlayer)) {
							if (command instanceof MultiplayerGameSetCellValueCommand
									&& ((MultiplayerGameSetCellValueCommand) command).isCreatingPlayer(MultiplayerPlay.this.remotePlayer)) {
								MultiplayerPlay.this.sudokuField.vibrate(MultiplayerPlay.this, MultiplayerPlay.this.getResources().getInteger(
										R.integer.vibrate_opponent_set_cell));
							} else if (command instanceof GiveUpCommand && MultiplayerPlay.this.counterIsRunning) {
								MultiplayerPlay.this.counter.onFinish();
							}
						} 
						if (command instanceof MultiplayerGameSetCellValueCommand && !((MultiplayerGameSetCellValueCommand) command).wasExecuted()) {
							MultiplayerPlay.this.sendCommand(command);
						}
					} else if (command instanceof MultiplayerPauseCommand) {
						((MultiplayerPauseCommand) command).execute((MultiplayerGame) game, MultiplayerPlay.this.remotePlayer);
						MultiplayerPlay.this.counter.stop();
						MultiplayerPlay.this.setupButtons();
					} else if (command instanceof RemoteReadyCommand) {
        				((RemoteReadyCommand) command).execute(MultiplayerPlay.this);
        			}
				}
				
				break;
			}
			
			MultiplayerPlay.this.refresh();
		}
	};

	/*
	 * (non-Javadoc)
	 * @see org.sudowars.Controller.Local.Play#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		this.savedGames = new FileIO(this.getApplicationContext());
		this.gameState = this.savedGames.loadMultiplayerGame();
		this.counterIsRunning = true;
		
		super.onCreate(savedInstanceState);
		
		if (!(this.game instanceof MultiplayerGame)) {
			throw new IllegalStateException("Game is no instance of MultiplayerGame.");
		}
		
		this.connection = (BluetoothConnection) BluetoothConnection.getActiveBluetoothConnection();
		
		if (this.connection != null) {
			this.connection.setBluetoothEventHandler(mHandler);
		} else {
			this.finish();
		}
		
		this.game.addOnChangeListener(new GameChangedEventListener() {
				@Override
				public void onGameChanged(GameChangedEvent event) {
					MultiplayerPlay.this.refreshScore();
				}
			});
		
		this.remotePlayer = this.game.getPlayers().get(1);
		this.setupAnimations();
		this.refreshScore();

		this.playerLeftGame = false;
		
		this.sudokuField.setVisibility(ViewGroup.GONE);
		this.keypad.setVisibility(ViewGroup.GONE);
		this.startCountDown();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
		
		if (!this.gameState.isFinished() && !this.game.isPaused()) {
			MultiplayerPauseCommand command = new MultiplayerPauseCommand();
			
			if (this.sendCommand(command)) {
				command.execute((MultiplayerGame) this.game, this.localPlayer);
				this.counter.stop();
				
				this.setupButtons();
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.multiplayer_play, menu);
		
    	RelativeLayout localScoreView = (RelativeLayout) menu.findItem(R.id.score_local).getActionView();
    	localScoreView.setBackgroundColor(this.getResources().getColor(R.color.actionbar_score_local_background));
		this.lblLocalScore = (TextView) localScoreView.findViewById(R.id.lblScore);
		this.lblLocalScore.setTextColor(this.getResources().getColor(R.color.actionbar_score_local_foreground));
		this.lblLocalOldScore = (TextView) localScoreView.findViewById(R.id.lblOldScore);
		this.lblLocalOldScore.setTextColor(this.getResources().getColor(R.color.actionbar_score_local_foreground));
		
		RelativeLayout remoteScoreView = (RelativeLayout) menu.findItem(R.id.score_remote).getActionView();
        remoteScoreView.setBackgroundColor(this.getResources().getColor(R.color.actionbar_score_remote_background));
		this.lblRemoteScore = (TextView) remoteScoreView.findViewById(R.id.lblScore);
		this.lblRemoteScore.setTextColor(this.getResources().getColor(R.color.actionbar_score_remote_foreground));
		this.lblRemoteOldScore = (TextView) remoteScoreView.findViewById(R.id.lblOldScore);
		this.lblRemoteOldScore.setTextColor(this.getResources().getColor(R.color.actionbar_score_remote_foreground));
		
		this.refreshScore();
		
		return super.onCreateOptionsMenu(menu);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sudowars.Controller.Local.Play#onConfigurationChanged(android.content.res.Configuration)
	 */
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		
		this.setupButtons();
		this.refreshScore();
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
	 */
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		
		if (!this.game.isPaused() && !this.gameState.isFinished() && !this.counterIsRunning) {
			menu.findItem(R.id.give_up).setEnabled(true);
		} else {
			menu.findItem(R.id.give_up).setEnabled(false);
		}
		
		if (!this.game.isPaused() && !this.gameState.isFinished() && !this.counterIsRunning) {
			menu.findItem(R.id.pause).setEnabled(true);
		} else {
			menu.findItem(R.id.pause).setEnabled(false);
		}
		
		return true;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sudowars.Controller.Local.Play#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.pause) {
			if (!this.game.isPaused() && !this.gameState.isFinished()) {
				this.onPause();
			}
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onBackPressed()
	 */
	@Override
	public void onBackPressed() {
		if (!this.game.isPaused() || this.counterIsRunning) {
			this.onPause();
		} else {
			this.saveGame();
			this.finish();
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sudowars.Controller.Local.Play#refresh()
	 */
	protected void refresh() {
		if (!this.counterIsRunning) {
			if (this.game.isPaused() && !this.gameState.isFinished()) {
				if (this.playerLeftGame) {
					this.ready.setVisibility(View.GONE);
				} else {
					this.ready.setVisibility(View.VISIBLE);
					this.tglLocalReady.setChecked(!this.game.hasPaused(this.localPlayer));
					this.tglRemoteReady.setChecked(!this.game.hasPaused(this.remotePlayer));
				}
			} else {
				this.ready.setVisibility(View.GONE);
				super.refresh();
			}
		} else {
			this.ready.setVisibility(View.GONE);
		}
	}
	
	/**
	 * Refresh the score
	 */
	private void refreshScore() {
		if (this.lblLocalScore != null && this.lblRemoteScore != null) {
			int localScore = ((MultiplayerGame) this.game).getScoreOfPlayer(this.localPlayer).getCurrentScore();
			int remoteScore = ((MultiplayerGame) this.game).getScoreOfPlayer(this.remotePlayer).getCurrentScore();
			
			if (localScore > 999) {
				localScore = 999;
			} else if (localScore < -999) {
				localScore = -999;
			}
			
			if (remoteScore > 999) {
				remoteScore = 999;
			} else if (remoteScore < -999) {
				remoteScore = -999;
			}
			
			if (!this.lblLocalScore.getText().equals(Integer.toString(localScore))) {
				this.lblLocalOldScore.setText(this.lblLocalScore.getText());
				this.lblLocalOldScore.startAnimation(this.localScoreOutAnimation);
				this.lblLocalScore.startAnimation(this.scoreInAnimation);
				this.lblLocalScore.setText(Integer.toString(localScore));
			}
			
			if (!this.lblRemoteScore.getText().equals(Integer.toString(remoteScore))) {
				this.lblRemoteOldScore.setText(this.lblRemoteScore.getText());
				this.lblRemoteOldScore.startAnimation(this.remoteScoreOutAnimation);
				this.lblRemoteScore.startAnimation(this.scoreInAnimation);
				this.lblRemoteScore.setText(Integer.toString(remoteScore));
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sudowars.Controller.Local.Play#onSymbolToggled(int)
	 */
	protected boolean onSymbolToggled(int symbolId) {
		boolean error = !super.onSymbolToggled(symbolId);
		
		if (!error) {
			GameCell selectedCell = this.sudokuField.noteManager.getSelectedCell(this.sudokuField);
			GameCommand command;
	
			if (!this.localPlayer.getNoteManagerOfPlayer(this.game).hasNote(selectedCell, symbolId + 1)) {
				command = new AddNoteCommand(selectedCell, symbolId + 1);
			} else {
				command = new RemoveNoteCommand(selectedCell, symbolId + 1);
			}
			
			if (!error) {
				error = !this.sendCommand(command);
			}
	
			if (!error) {
				error = !command.execute(this.game, this.localPlayer);
			}
		}
		
		return !error;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sudowars.Controller.Local.Play#onSymbolLongPress(int)
	 */
	protected boolean onSymbolLongPress(int symbolId) {
		boolean error = !super.onSymbolLongPress(symbolId);
		
		if (!error) {
			GameCell selectedCell = this.sudokuField.noteManager.getSelectedCell(this.sudokuField);
			
			if (selectedCell.isSet()) {
				this.noteManager.notificate(this, R.string.notification_cell_is_already_set, Toast.LENGTH_SHORT);
				error = true;
			}
			
			if (!error) {
				GameCommand command = new MultiplayerGameSetCellValueCommand(selectedCell, symbolId + 1, this.connection.tsync.getCorrectedUpTime(), localPlayer);
				
				error = !command.execute(this.game, localPlayer);
				if (error) {
					this.sudokuField.highlightWrongInput(selectedCell, 2000);
				}
				
				this.sendCommand(command);
			}
		}
		
		return !error;
	}

	/*
	 * (non-Javadoc)
	 * @see org.sudowars.Controller.Local.Play#onBtnInvertClick()
	 */
	protected boolean onBtnInvertClick() {
		boolean error = !super.onBtnInvertClick();
		
		if (!error) {
			InvertCellCommand command = new InvertCellCommand(this.sudokuField.noteManager.getSelectedCell(this.sudokuField));
			error = !this.sendCommand(command);
			
			if (!error) {
				error = !command.execute(this.game, this.localPlayer);
			}
		}
		
		return !error;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sudowars.Controller.Local.Play#onBtnClearClick()
	 */
	protected boolean onBtnClearClick() {
		boolean error = !super.onBtnInvertClick();
		GameCell selectedCell = this.sudokuField.noteManager.getSelectedCell(this.sudokuField);
		
		if (!error && this.noteManager.hasNotes(selectedCell) && !selectedCell.isSet()) {
			ClearCellCommand command = new ClearCellCommand(selectedCell);
			
			if (selectedCell.isSet()) {
				this.noteManager.notificate(this, R.string.notification_cell_is_already_set, Toast.LENGTH_SHORT);
			} else {
				error = !this.sendCommand(command);
				if (!error) {
					error = !command.execute(this.game, this.localPlayer);
				}
			}
		}
		
		return !error;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sudowars.Controller.Local.Play#onGameFinished(java.lang.String)
	 */
	protected void onGameFinished(String text) {
		if (this.game.isAborted()) {
			if (this.game.getAbortingPlayer().equals(this.localPlayer)) {
				text = getString(R.string.text_defeat_multiplayer);
			} else {
				text = getString(R.string.text_win_multiplayer);
			}
		} else {
			int localScore = ((MultiplayerGame) this.game).getScoreOfPlayer(this.localPlayer).getCurrentScore();
			int RemoteScore = ((MultiplayerGame) this.game).getScoreOfPlayer(((MultiplayerGame) this.game).getPlayers().get(1)).getCurrentScore();
			
			if (localScore > RemoteScore) {
				text = getString(R.string.text_win_multiplayer);
			} else if (localScore < RemoteScore) {
				text = getString(R.string.text_defeat_multiplayer);
			} else {
				text = getString(R.string.text_draw_multiplayer);
			}
		}
		
		super.onGameFinished(text);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sudowars.Controller.Local.Play#onGivingUp()
	 */
	protected void onGivingUp() {
		GiveUpCommand command = new GiveUpCommand();
		
		if (this.sendCommand(command)) {
			command.execute(game, localPlayer);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sudowars.Controller.Local.Play#onGameAborted(org.sudowars.Model.Game.Player)
	 */
	protected void onGameAborted() {
		String text;
		
		if (this.localPlayer.equals(this.game.getAbortingPlayer())) {
			text = getString(R.string.text_defeat_multiplayer);
		} else {
			text = getString(R.string.text_win_multiplayer);
		}
		
		super.onGameFinished(text);
	}
	
	/**
	 * Running on a click on button {@link tglLocalReady}.
	 */
	private void onTglLocalReadyToggle () {
		RemoteReadyCommand command = new RemoteReadyCommand(this.tglLocalReady.isChecked());
		this.connection.sendCommand((Command) command);
		
		if (this.tglLocalReady.isChecked()) {
			this.game.getPlayers().get(0).resumeGame(this.game);
		} else {
			this.game.getPlayers().get(0).pauseGame(this.game);
		}
		
		if (!this.game.isPaused()) {
			this.startCountDown();
		}
	}
	
	/**
	 * Function used to set the RemoteReady state via remote command object
	 * @param state the state of the remote player
	 */
	public void setRemoteReadyState(boolean state) {
		if (state) {
			this.remotePlayer.resumeGame(this.game);
		} else {
			this.remotePlayer.pauseGame(this.game);
		}
		
		if (!this.game.isPaused()) {
			this.startCountDown();
		} else {
			this.refresh();
		}
	}
	
	/**
	 * Display a countdown and starts the game afterwards
	 */
	private void startCountDown() {
		DebugHelper.log(DebugHelper.PackageName.MultiplayerPlay, "Start countdown.");
		
		this.ready.setVisibility(View.GONE);
		
		this.counter = new Counter(3999, 1000);
		this.sudokuField.vibrate(this, this.getResources().getInteger(R.integer.vibrate_countdown_on_start));
		this.counter.start();
	}
	
	/**
	 * Returns true, if the command sending to remote was successful, otherwise false.
	 * 
	 * @param command the command to send the remote
	 * @return true, if the command sending to remote was successful, otherwise false
	 */
	private boolean sendCommand(Command command) {
		boolean result = false;
		
		if (this.connection != null) {
			this.connection.sendCommandAsync(command);
			result = true;
		}
		
		return result;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sudowars.Controller.Local.Play#saveGame()
	 */
	protected void saveGame() {
		if (this.gameState.isFinished()) {
			this.savedGames.deleteMultiplayerGame();
		} else {
			this.savedGames.saveMultiplayerGame(this.gameState);
		}
	}
	
	/**
	 * Setup view.
	 */
	protected void setupView() {
		CharSequence textCountdown = "";
		if (this.lblCountdown != null) {
			textCountdown = this.lblCountdown.getText();
		}
		
		LayoutInflater inflater = LayoutInflater.from(this.getApplicationContext());
		this.play_content = (LinearLayout) inflater.inflate(R.layout.play_content, null, false);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT);
		this.play_content.setLayoutParams(lp);
		this.play_content.setVisibility(LinearLayout.GONE);
		this.lblPauseText = (TextView) play_content.findViewById(R.id.lblPauseText);
		this.lblCountdown = (TextView) play_content.findViewById(R.id.lblCountdown);
		this.ready = (LinearLayout) play_content.findViewById(R.id.ready);
		
		this.lblCountdown.setText(textCountdown);
		this.tglLocalReady = (ToggleButton) this.ready.findViewById(R.id.tglLocalReady);
		this.tglLocalReady.setOnClickListener(
                new OnClickListener() {
                	public void onClick(View v) {
                		MultiplayerPlay.this.onTglLocalReadyToggle();
                    }
                });
		this.tglRemoteReady = (ToggleButton) this.ready.findViewById(R.id.tglRemoteReady);
		
		super.setupView();
		this.root.addView(this.play_content);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sudowars.Controller.Local.Play#setupButtons()
	 */
	protected void setupButtons() {
		//Handle countdown, pause text, Sudoku field and keypad
		if ((this.game.isPaused() && !this.gameState.isFinished()) || this.counterIsRunning) {
			this.sudokuField.setVisibility(ViewGroup.GONE);
			this.keypad.setVisibility(ViewGroup.GONE);
			this.play_content.setVisibility(LinearLayout.VISIBLE);
			
			if ((this.game.isPaused() && !this.gameState.isFinished() && !this.counterIsRunning) || this.playerLeftGame) {
				this.lblCountdown.setVisibility(View.GONE);
				this.lblPauseText.setVisibility(View.VISIBLE);
				
				if (this.playerLeftGame) {
					this.lblPauseText.setText(this.getResources().getString(R.string.text_remote_left));
				}
			} else {
				this.lblCountdown.setVisibility(View.VISIBLE);
				this.lblPauseText.setVisibility(View.GONE);
			}
		} else {
			this.sudokuField.setVisibility(ViewGroup.VISIBLE);
			this.keypad.setVisibility(ViewGroup.VISIBLE);
			this.play_content.setVisibility(LinearLayout.GONE);
			this.lblCountdown.setVisibility(View.GONE);
			this.lblPauseText.setVisibility(View.GONE);
		}

		//Handle ready buttons
		if (!this.gameState.isFinished()
			&& (this.game.isStarted() && !this.counterIsRunning && !this.playerLeftGame)
			&& (!this.game.isPaused() || this.gameState.isFinished())) {
			
			super.setupButtons();
		}

		this.refresh();
	}
	
	/**
	 * Setup the animations
	 */
	private void setupAnimations() {
		this.scoreInAnimation = AnimationUtils.loadAnimation(this, R.anim.score_in);
		this.localScoreOutAnimation = AnimationUtils.loadAnimation(this, R.anim.score_out);
		this.localScoreOutAnimation.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
				MultiplayerPlay.this.lblLocalOldScore.setVisibility(View.VISIBLE);
				
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				MultiplayerPlay.this.lblLocalOldScore.setVisibility(View.INVISIBLE);
				
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {
			}
			
		});
		
		this.remoteScoreOutAnimation = AnimationUtils.loadAnimation(this, R.anim.score_out);
		this.remoteScoreOutAnimation.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
				MultiplayerPlay.this.lblRemoteOldScore.setVisibility(View.VISIBLE);
				
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				MultiplayerPlay.this.lblRemoteOldScore.setVisibility(View.INVISIBLE);
				
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {
			}
		});
	}
	
	/**
	 * The Counter to display a countdown before the game begin
	 */
	private class Counter extends CountDownTimer {
		/*
		 * The Constructor
		 */
		public Counter(long millisInFuture, long countDownInterval) {
			super(millisInFuture, countDownInterval);
			
			MultiplayerPlay.this.lblCountdown.setVisibility(View.VISIBLE);
			MultiplayerPlay.this.lblPauseText.setVisibility(View.GONE);
			MultiplayerPlay.this.counterIsRunning = true;
		}
		
		/*
		 * (non-Javadoc)
		 * @see android.os.CountDownTimer#onFinish()
		 */
		@Override
		public void onFinish() {
			if (MultiplayerPlay.this.counterIsRunning) {
				MultiplayerPlay.this.sudokuField.vibrate(MultiplayerPlay.this, MultiplayerPlay.this.getResources().getInteger(R.integer.vibrate_countdown_on_finish));
				
				MultiplayerPlay.this.counterIsRunning = false;
				MultiplayerPlay.this.game.startGame();
				MultiplayerPlay.this.setupButtons();
			}
		}
		
		/**
		 * Stops the countdown
		 */
		public void stop() {
			super.cancel();
			
			MultiplayerPlay.this.counterIsRunning = false;

			DebugHelper.log(DebugHelper.PackageName.MultiplayerPlay, "Cancel countdown.");
		}
		
		/*
		 * (non-Javadoc)
		 * @see android.os.CountDownTimer#onTick(long)
		 */
		@Override
		public void onTick(long millisUntilFinished) {
			if (MultiplayerPlay.this.counterIsRunning) {
				MultiplayerPlay.this.lblCountdown.setText(String.valueOf(millisUntilFinished/1000));
			} else {
				this.onFinish();
			}
		}
	}
}
