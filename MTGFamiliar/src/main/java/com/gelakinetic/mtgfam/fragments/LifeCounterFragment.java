package com.gelakinetic.mtgfam.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.LcPlayer;
import com.gelakinetic.mtgfam.helpers.LcPlayer.HistoryEntry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * TODO save life / poison state on rotation, etc
 */
public class LifeCounterFragment extends FamiliarFragment implements TextToSpeech.OnInitListener, AudioManager.OnAudioFocusChangeListener, TextToSpeech.OnUtteranceCompletedListener {

	private static final int REMOVE_PLAYER_DIALOG = 0;
	private static final int RESET_CONFIRM_DIALOG = 1;
	private static final String DISPLAY_MODE = "display_mode";
	private static final String LIFE_ANNOUNCE = "life_announce";
	private static final int IMPROBABLE_NUMBER = 531865548;
	private static final String OVER_9000_KEY = "@over_9000";

	private LinearLayout mLinearLayout;
	private ArrayList<LcPlayer> mPlayers = new ArrayList<LcPlayer>();
	private ImageView mPoisonButton;
	private ImageView mLifeButton;
	private int mDisplayMode;
	private int mListSizeWidth = -1;
	private int mListSizeHeight = -1;
	private View mScrollView;
	private int mLargestPlayerNumber = 0;

	/* Stuff for TTS */
	private TextToSpeech mTts;
	private boolean mTtsInit;
	private AudioManager mAudioManager;
	private MediaPlayer m9000Player;
	LinkedList<String> mVocalizations = new LinkedList<String>();

	/**
	 * Force the child fragments to override onCreateView
	 *
	 * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment,
	 * @param container          If non-null, this is the parent view that the fragment's UI should be attached to. The
	 *                           fragment should not add the view itself, but this can be used to generate the
	 *                           LayoutParams of the view.
	 * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given
	 *                           here.
	 * @return The inflated view
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mListSizeWidth = -1;
		mListSizeHeight = -1;

		View myFragmentView = inflater.inflate(R.layout.life_counter_frag, container, false);
		assert myFragmentView != null;

		mLinearLayout = (LinearLayout) myFragmentView.findViewById(R.id.playerList);
		mScrollView = myFragmentView.findViewById(R.id.playerScrollView);
		ViewTreeObserver viewTreeObserver = mScrollView.getViewTreeObserver();
		assert viewTreeObserver != null;
		viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			public void onGlobalLayout() {
				if (mListSizeWidth == -1) {
					mListSizeWidth = mScrollView.getWidth();
					mListSizeHeight = mScrollView.getHeight();
					for (LcPlayer player : mPlayers) {
						player.setSize(getActivity().getResources().getConfiguration().orientation, mListSizeWidth, mListSizeHeight);
					}
				}
			}
		});

		mPoisonButton = (ImageView) myFragmentView.findViewById(R.id.poison_button);
		mPoisonButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				setMode(LcPlayer.POISON);
			}
		});
		mLifeButton = (ImageView) myFragmentView.findViewById(R.id.life_button);
		mLifeButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				setMode(LcPlayer.LIFE);
			}
		});
		myFragmentView.findViewById(R.id.reset_button).setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				showDialog(RESET_CONFIRM_DIALOG);
			}
		});

		if (savedInstanceState != null) {
			mDisplayMode = savedInstanceState.getInt(DISPLAY_MODE, LcPlayer.LIFE);
		}

		return myFragmentView;
	}

	/**
	 * Assume that every fragment has a menu
	 * Assume that every fragment wants to retain it's instance state (onCreate/onDestroy called
	 * once, onCreateView called on rotations etc)
	 *
	 * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mTtsInit = false;
		mTts = new TextToSpeech(getActivity(), this);
		mTts.setOnUtteranceCompletedListener(this);

		mAudioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);

		m9000Player = MediaPlayer.create(getActivity(), R.raw.over_9000);
		m9000Player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			public void onCompletion(MediaPlayer mp) {
				onUtteranceCompleted(LIFE_ANNOUNCE);
			}
		});
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mTts != null) {
			mTts.stop();
			mTts.shutdown();
		}
		if (m9000Player != null) {
			m9000Player.reset();
			m9000Player.release();
		}
	}

	/**
	 * TODO
	 */
	@Override
	public void onPause() {
		super.onPause();
		String playerData = "";
		for (LcPlayer player : mPlayers) {
			playerData += player.toString();
		}
		((FamiliarActivity) getActivity()).mPreferenceAdapter.setPlayerData(playerData);
		for (LcPlayer player : mPlayers) {
			mLinearLayout.removeView(player.getView());
		}
		mPlayers.clear();
	}

	/**
	 * TODO
	 */
	@Override
	public void onResume() {
		super.onResume();

		String playerData = ((FamiliarActivity) getActivity()).mPreferenceAdapter.getPlayerData();
		if (playerData == null || playerData.length() == 0) {
			addPlayer();
			addPlayer();
		}
		else {
			String[] playerLines = playerData.split("\n");
			for (String line : playerLines) {
				addPlayer(line);
			}
		}
		setMode(mDisplayMode);
	}

	/**
	 * @param menu     The options menu in which you place your items.
	 * @param inflater The inflater to use to inflate the menu
	 */
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.life_counter_menu, menu);
	}

	/**
	 * TODO
	 *
	 * @param menu
	 */
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		if (!mTtsInit) {
			menu.findItem(R.id.announce_life).setVisible(false);
		}
		else {
			menu.findItem(R.id.announce_life).setVisible(true);
		}
	}

	/**
	 * @param item
	 * @return
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.add_player:
				addPlayer();
				return true;
			case R.id.remove_player:
				showDialog(REMOVE_PLAYER_DIALOG);
				return true;
			case R.id.announce_life:
				announceLifeTotals();
				return true;
			case R.id.change_gathering:
				return true;
			case R.id.set_gathering:
				return true;
			case R.id.display_mode:
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * TODO
	 *
	 * @param outState Bundle in which to place your saved state.
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt(DISPLAY_MODE, mDisplayMode);
		super.onSaveInstanceState(outState);
	}

	/**
	 * Remove any showing dialogs, and show the requested one
	 *
	 * @param id the ID of the dialog to show
	 */
	void showDialog(final int id) {
		/* DialogFragment.show() will take care of adding the fragment in a transaction. We also want to remove any
		currently showing dialog, so make our own transaction and take care of that here. */

		/* If the fragment isn't visible (maybe being loaded by the pager), don't show dialogs */
		if (!this.isVisible()) {
			return;
		}

		removeDialog(getFragmentManager());

		/* Create and show the dialog. */
		final FamiliarDialogFragment newFragment = new FamiliarDialogFragment() {

			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				/* This will be set to false if we are returning a null dialog. It prevents a crash */
				setShowsDialog(true);

				switch (id) {
					case REMOVE_PLAYER_DIALOG: {
						String[] names = new String[mPlayers.size()];
						for (int i = 0; i < mPlayers.size(); i++) {
							names[i] = mPlayers.get(i).mName;
						}

						AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
						builder.setTitle(getString(R.string.life_counter_remove_player));

						builder.setItems(names, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int item) {
								mLinearLayout.removeView(mPlayers.get(item).getView());
								mPlayers.remove(item);
								mLinearLayout.invalidate();
							}
						});

						return builder.create();
					}
					case RESET_CONFIRM_DIALOG: {
						AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
						builder
								.setMessage(getString(R.string.life_counter_clear_dialog_text))
								.setCancelable(true)
								.setPositiveButton(getString(R.string.dialog_both), new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										/* Remove all players */
										for (LcPlayer player : mPlayers) {
											mLinearLayout.removeView(player.getView());
										}
										mPlayers.clear();

										/* Add default players */
										mLargestPlayerNumber = 0;
										addPlayer();
										addPlayer();

										dialog.dismiss();
									}
								}).setNeutralButton(getString(R.string.dialog_life), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								for (LcPlayer player : mPlayers) {
									player.resetStats();
								}
								mLinearLayout.invalidate();
								dialog.dismiss();
							}
						}).setNegativeButton(getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.dismiss();
							}
						});

						return builder.create();
					}
					default: {
						savedInstanceState.putInt("id", id);
						return super.onCreateDialog(savedInstanceState);
					}
				}
			}
		};
		newFragment.show(getFragmentManager(), FamiliarActivity.DIALOG_TAG);
	}

	/**
	 * TODO
	 *
	 * @param displayMode
	 */
	private void setMode(int displayMode) {
		mDisplayMode = displayMode;

		switch (displayMode) {
			case LcPlayer.LIFE:
				mPoisonButton.setImageResource(R.drawable.lc_poison_enabled);
				mLifeButton.setImageResource(R.drawable.lc_life_disabled);
				break;
			case LcPlayer.POISON:
				mPoisonButton.setImageResource(R.drawable.lc_poison_disabled);
				mLifeButton.setImageResource(R.drawable.lc_life_enabled);
				break;
		}

		for (LcPlayer player : mPlayers) {
			player.setMode(displayMode);
		}
	}

	/**
	 * TODO
	 */
	private void addPlayer() {
		LcPlayer player = new LcPlayer((FamiliarActivity) getActivity());

		mLargestPlayerNumber++;
		player.mName = getString(R.string.life_counter_default_name) + " " + mLargestPlayerNumber;

		mPlayers.add(player);
		mLinearLayout.addView(player.newView());
		if (mListSizeHeight != -1) {
			player.setSize(getActivity().getResources().getConfiguration().orientation, mListSizeWidth, mListSizeHeight);
		}

	}

	/**
	 * TODO
	 *
	 * @param line
	 */
	private void addPlayer(String line) {
		try {
			LcPlayer player = new LcPlayer((FamiliarActivity) getActivity());

			String[] data = line.split(";");

			player.mName = data[0];

			/* Keep track of the largest numbered player, for adding new players */
			try {
				String nameParts[] = player.mName.split(" ");
				int number = Integer.parseInt(nameParts[nameParts.length - 1]);
				if (number > mLargestPlayerNumber) {
					mLargestPlayerNumber = number;
				}
			} catch (NumberFormatException e) {
				/* eat it */
			}

			player.mLife = Integer.parseInt(data[1]);

			try {
				player.mDefaultLifeTotal = Integer.parseInt(data[5]);
			} catch (Exception e) {
				player.mDefaultLifeTotal = 20; // TODO static const?
			}

			try {
				String[] lifeHistory = data[2].split(","); // ArrayIndexOutOfBoundsException??
				player.mLifeHistory = new ArrayList<HistoryEntry>(lifeHistory.length);
				HistoryEntry entry;
				for (int i = lifeHistory.length - 1; i >= 0; i--) {
					entry = new HistoryEntry();
					entry.mAbsolute = Integer.parseInt(lifeHistory[i]);
					if (i != lifeHistory.length - 1) {
						entry.mDelta = entry.mAbsolute - player.mLifeHistory.get(0).mAbsolute;
					}
					else {
						entry.mDelta = entry.mAbsolute - player.mDefaultLifeTotal;
					}
					player.mLifeHistory.add(0, entry);
				}

			} catch (NumberFormatException e) {
				/* Eat it */
			} catch (ArrayIndexOutOfBoundsException e) {
				/* Eat it */
			}

			player.mPoison = Integer.parseInt(data[3]);

			try {
				String[] poisonHistory = data[4].split(","); // ArrayIndexOutOfBoundsException??
				player.mPoisonHistory = new ArrayList<HistoryEntry>(poisonHistory.length);
				HistoryEntry entry;
				for (int i = poisonHistory.length - 1; i >= 0; i--) {
					entry = new HistoryEntry();
					entry.mAbsolute = Integer.parseInt(poisonHistory[i]);
					if (i != poisonHistory.length - 1) {
						entry.mDelta = entry.mAbsolute - player.mPoisonHistory.get(0).mAbsolute;
					}
					else {
						entry.mDelta = entry.mAbsolute;
					}
					player.mPoisonHistory.add(0, entry);
				}

			} catch (NumberFormatException e) {
				/* Eat it */
			} catch (ArrayIndexOutOfBoundsException e) {
				/* Eat it */
			}

			try {
				String[] commanderLifeString = data[6].split(",");
				player.mCommanderHistory = new ArrayList<HistoryEntry>(commanderLifeString.length);
				HistoryEntry entry;
				for (String aCommanderLifeString : commanderLifeString) {
					entry = new HistoryEntry();
					entry.mAbsolute = Integer.parseInt(aCommanderLifeString);
					player.mCommanderHistory.add(entry);
				}
			} catch (NumberFormatException e) {
				/* Eat it */
			} catch (ArrayIndexOutOfBoundsException e) {
				/* Eat it */
			}

			try {
				player.mCommanderCasting = Integer.parseInt(data[7]);
			} catch (Exception e) {
				player.mCommanderCasting = 0;
			}

			mPlayers.add(player);
			mLinearLayout.addView(player.newView());
			if (mListSizeHeight != -1) {
				player.setSize(getActivity().getResources().getConfiguration().orientation, mListSizeWidth, mListSizeHeight);
			}
		} catch (
				ArrayIndexOutOfBoundsException e
				)

		{
			/* Eat it */
		}
	}

	/**
	 * When mTts is initialized, set the boolean flag and display the option in the ActionBar
	 *
	 * @param i
	 */
	@Override
	public void onInit(int i) {
		mTtsInit = true;
		getActivity().invalidateOptionsMenu();
	}

	/**
	 * Build a LinkedList of all the things to say, which can include TTS calls and MediaPlayer calls. Then call
	 * onUtteranceCompleted to start running through the LinkedList, even though no utterance was spoken.
	 */
	private void announceLifeTotals() {
		if (mTtsInit) {
			mVocalizations.clear();
			for (LcPlayer p : mPlayers) {
				switch (mDisplayMode) {
					case LcPlayer.LIFE:
						if (p.mLife > 9000) {
							/* If the life is over 9000, split the string on an IMPROBABLE_NUMBER, and insert a call to the m9000Player */
							String tmp = String.format(getString(R.string.life_counter_spoken_life), p.mName, IMPROBABLE_NUMBER);
							String parts[] = tmp.split(Integer.toString(IMPROBABLE_NUMBER));
							mVocalizations.add(parts[0]);
							mVocalizations.add(OVER_9000_KEY);
							mVocalizations.add(parts[1]);
						}
						else {
							mVocalizations.add(String.format(getString(R.string.life_counter_spoken_life), p.mName, p.mLife));
						}
						break;
					case LcPlayer.POISON:
						mVocalizations.add(String.format(getString(R.string.life_counter_spoken_poison), p.mName, p.mPoison));
						break;
				}

			}

			if (mVocalizations.size() > 0) {
				int res = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
				if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
					onUtteranceCompleted(LIFE_ANNOUNCE);
				}
			}
		}
	}

	/**
	 * Necessary to implement OnAudioFocusChangeListener, ignored
	 *
	 * @param i some irrelevant integer
	 */
	@Override
	public void onAudioFocusChange(int i) {

	}

	/**
	 * This is called every time an utterance is completed, as well as when the m9000Player finishes shouting.
	 * It polls an item out of the LinkedList and speaks it, or returns audio focus to the system.
	 *
	 * @param key A key to determine what was just uttered. This is ignored
	 */
	@Override
	public void onUtteranceCompleted(String key) {
		if (mVocalizations.size() > 0) {
			String toSpeak = mVocalizations.poll();
			if (toSpeak.equals(OVER_9000_KEY)) {
				try {
					m9000Player.stop();
					m9000Player.prepare();
					m9000Player.start();
				} catch (IOException e) {
					/* If the media was not played, fall back to TTSing "over 9000" */
					HashMap<String, String> ttsParams = new HashMap<String, String>();
					ttsParams.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_MUSIC));
					ttsParams.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, LIFE_ANNOUNCE);
					mTts.speak(getString(R.string.life_counter_over_9000), TextToSpeech.QUEUE_FLUSH, ttsParams);
				}
			}
			else {
				HashMap<String, String> ttsParams = new HashMap<String, String>();
				ttsParams.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_MUSIC));
				ttsParams.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, LIFE_ANNOUNCE);
				mTts.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, ttsParams);
			}
		}
		else {
			mAudioManager.abandonAudioFocus(this);
		}
	}
}