package sleepfuriously.com.biggsstopwatch2;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;


public class MainActivity extends AppCompatActivity
                    implements View.OnClickListener {

    //-------------------------
    //  Constants
    //-------------------------

    private final static String TAG = MainActivity.class.getSimpleName();

    // The tags for the preferences file.
    //
    private static final String PREFS_SOUND = "sound";
    private static final String PREFS_VIBRATE = "vibrate";
    private static final String PREFS_AWAKE = "awake";

    // Defaults for UI experience
    private static final boolean
        DEFAULT_SOUND_SETTING = true,
        DEFAULT_VIBRATE_SETTING = true,
        DEFAULT_AWAKE_SETTING = false;

    /** number of chars in the counting string */
    private static final float NUM_DISPLAY_CHARS = 9f;

    //-------------------------
    //  Widgets
    //-------------------------

    /** Displays main time */
    TextView m_display1;

    /** Displays split time */
    TextView m_display2;

    /** Stop/Stop button */
    Button m_start_button;

    /** Split/Clear button */
    Button m_split_button;

    //-------------------------
    //  Data
    //-------------------------

    /** current state of this app */
    int m_state;

    /** millis of system clock when start was pressed */
    long m_start_time;

    /** Added to m_start_time to calculate m_elapsed_time (starting/restarting) */
    long m_elapsed_time;

    /** currently displayed split time (millis) */
    long m_split_time;

    /** true = make click sound */
    boolean m_sound = DEFAULT_SOUND_SETTING;

    /** true = vibrate */
    boolean m_vibrate = DEFAULT_VIBRATE_SETTING;

    /** true = keep phone awake */
    boolean m_stay_awake = DEFAULT_VIBRATE_SETTING;

    /** used to make callbacks every hundredth of a second */
    MyCountDown m_timer;

    Vibrator m_vibrator;

    /** used for setting up and making sounds */
    NotificationManager m_mgr;

    /** also used for sounds */
    Notification m_notification;

    /** controls sound system */
    SoundManager m_sound_mgr;

    /** Screen dimensions */
    int mScreenWidth, mScreenHeight, mScreenDensity;


    //-------------------------
    //  Methods
    //-------------------------

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);


        m_display1 = findViewById (R.id.tv_1);
        m_display2 = findViewById (R.id.tv_2);
        m_start_button = findViewById (R.id.butt_1);
        m_split_button = findViewById (R.id.butt_2);

        m_start_button.setOnClickListener(this);
        m_split_button.setOnClickListener(this);

        // Set the font size of the displays
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        mScreenWidth = displayMetrics.widthPixels;
//        mScreenHeight = displayMetrics.heightPixels;
//        mScreenDensity = displayMetrics.densityDpi;
//        float other_density = displayMetrics.density;
//        float sp = m_display1.getTextSize();
//        Log.d(TAG, "width = " + mScreenWidth + ", height = " + mScreenHeight +
//                ", densityDpi = " + mScreenDensity + ", density = " + other_density +
//                ", sp = " + sp);
//
//        int tv_width = m_display1.getWidth();
//        int tv_height = m_display1.getHeight();
//        Log.d (TAG, "tv_width = " + tv_width + ", tv_height = " + tv_height);

        // Calculating the text size.
        //
        // the facts:
        //  9 monospace characters
        //  They need to fit within mScreenWidth
        //  so... mScreenWidth / 9 should be the pixels wide for each character
        //
        float textsize = ((float)mScreenWidth) / NUM_DISPLAY_CHARS;
        m_display1.setTextSize(textsize);
        m_display2.setTextSize(textsize * 0.65f);  // make split time a little smaller


        // Two notes:
        //	shouldn't this be in onResume?
        //	shouldn't this timer be closed in onStop?
        m_timer = new MyCountDown (Long.MAX_VALUE, 20);
        m_timer.start();

        // Restore our data members (if possible)
        if (bundle != null) {
            m_state = bundle.getInt("state");
            m_start_time = bundle.getLong("start_time");
            m_elapsed_time = bundle.getLong("elapsed_time");
            m_split_time = bundle.getLong("split_time");
            m_sound = bundle.getBoolean(PREFS_SOUND);
            m_vibrate = bundle.getBoolean(PREFS_VIBRATE);
            m_stay_awake = bundle.getBoolean(PREFS_AWAKE, false);
            set_widgets();
        }

        // Make the buttons vibrate.
        m_vibrator = (Vibrator) getSystemService (Context.VIBRATOR_SERVICE);

        // Load up a notification manager.
        m_mgr = (NotificationManager)
                getSystemService (Context.NOTIFICATION_SERVICE);
        m_notification = new Notification();
        m_notification.defaults |= Notification.DEFAULT_SOUND;
        m_notification.icon = R.drawable.ic_stat_name;
        m_notification.tickerText = "test";
        m_notification.when = System.currentTimeMillis();

        // Sound stuff.
        m_sound_mgr = new SoundManager();
        m_sound_mgr.initSounds(getBaseContext());
        m_sound_mgr.addSound (1, R.raw.button_click);	// id = 1.

//        // for testing
//        Button add_ten_butt = findViewById(R.id.test_butt);
//        add_ten_butt.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // ten minutes = 10 * seconds per minute * millis per second
//                m_start_time -= (10L * 60L * 1000L);
//            }
//        });


//		Log.i (tag, "m_state = " + m_state);
    } // onCreate (.)

    @Override
    protected void onStart() {
        super.onStart();

        // The Preferences File system.  Load up our
        // settings!
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);

        m_sound = prefs.getBoolean(PREFS_SOUND, DEFAULT_SOUND_SETTING);
        m_vibrate = prefs.getBoolean(PREFS_VIBRATE, DEFAULT_VIBRATE_SETTING);
        m_stay_awake = prefs.getBoolean(PREFS_AWAKE, DEFAULT_AWAKE_SETTING);

    }

    //-----------------------------
    //	Called when this activity is completely obscured.
    //
    @Override
    protected void onStop() {
        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences (this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREFS_SOUND, m_sound);
        editor.putBoolean(PREFS_VIBRATE, m_vibrate);
        editor.putBoolean(PREFS_AWAKE, m_stay_awake);

        // Commit the edits!
        editor.apply();

        super.onStop();
    } // onStop()


    //-----------------------------
    //	Called when this activity once again becomes visible
    //
    @Override
    protected void onResume() {
        super.onResume();

        // Do we need to note that this window should stay awake?
        if (m_stay_awake) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

    } // onResume()


    //-----------------------------
    //	I use Prepare instead of Create so that the options
    //	menu will properly reflect the changes.
    //
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        menu.clear();

        // First menu item (#1) is turn on/off sound
        if (m_sound)
            menu.add(Menu.NONE, 1, Menu.NONE,
                     getString (R.string.menu_sound_off));
        else
            menu.add(Menu.NONE, 1, Menu.NONE,
                     getString (R.string.menu_sound_on));

        // Second menu item (#2) is turn/off vibration
        if (m_vibrate)
            menu.add (Menu.NONE, 2, Menu.NONE,
                      getString (R.string.menu_vibrate_off));
        else
            menu.add (Menu.NONE, 2, Menu.NONE,
                      getString (R.string.menu_vibrate_on));

        if (m_stay_awake)
            menu.add(Menu.NONE, 3, Menu.NONE,
                     getString (R.string.menu_awake_off));
        else
            menu.add(Menu.NONE, 3, Menu.NONE,
                     getString (R.string.menu_awake_on));

        menu.add(Menu.NONE, 4, Menu.NONE, R.string.about);

        return super.onCreateOptionsMenu(menu);
    } // onPrepareOptionsMenu (menu)


    //-----------------------------
    //	Called when a menu item has been selected.
    //
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                m_sound = !m_sound;
                break;
            case 2:
                m_vibrate = !m_vibrate;
                break;

            case 3:
                m_stay_awake = !m_stay_awake;
                if (m_stay_awake) {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
                else {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
                break;

            case 4:
                // Build the dialog.
                AlertDialog.Builder builder =
                        new AlertDialog.Builder (this);
                builder.setIcon (R.mipmap.ic_launcher);
                builder.setTitle(R.string.about);
                builder.setMessage (R.string.about_msg);
                builder.setPositiveButton(R.string.ok, null);

                // And show it.
                AlertDialog dialog = builder.create();
                dialog.show();
                break;
        } // switch

        // Go ahead and shake if they just turned it on.
        if (m_vibrate)
            m_vibrator.vibrate (10);	// Vibrate 100 milliseconds

        return super.onOptionsItemSelected(item);
    } // onOptionsItemSelected (item)


    //-----------------------------
    //	Called before this Activity is killed so that the state
    //	can be restored in the next onCreate().
    //
    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);

        // Save our all-important data members.
        bundle.putInt("state", m_state);
        bundle.putLong("start_time", m_start_time);
        bundle.putLong("elapsed_time", m_elapsed_time);
        bundle.putLong("split_time", m_split_time);
        bundle.putBoolean(PREFS_SOUND, m_sound);
        bundle.putBoolean(PREFS_VIBRATE, m_vibrate);
        bundle.putBoolean(PREFS_AWAKE, m_stay_awake);
    } // onSaveInstanceState (.)


    //-----------------------------
    //	The MEAT of this app.  Controls moving from state to state.
    //	This is a lot, so it's a rather long method.  Doesn't make
    //	sense to divide things up as all the information is here.
    //
    //	There are five states for this device.
    //
    //	0. Start state.  The display is ZERO. Nothing is running.
    //		Start -> 1
    //		Split disabled
    //
    //	1. Running.  The display is spinning.
    //		Start (now STOP) -> 4
    //		Split -> 2
    //
    //	2. Split.  Display shows split time, but we're still
    //		counting.
    //		Start (now STOP)-> 3
    //		Split -> 2
    //
    //	3. Split-Stop.  Display shows a split time, but the counter
    //		has stopped at another time.
    //		Start -> 2
    //		Clear -> 0
    //
    //	4. Final.  Display shows a time, no counter is running.
    //		Start -> 1
    //		Split (now CLEAR) -> 0
    //
    //	preconditions:
    //		m_state		Should be set to the correct state.
    //
    //	input:
    //		button		The button that was pushed.
    //
    //	side effects:
    //		m_state			Will be changed to appropriate state.
    //		m_start_time		May be reset (depends on the states).
    //		m_split_time		May be reset. (might not be needed)
    //
    //	returns:
    //		The new state.
    //
    void next_state (View button) {
        switch (m_state) {
            case 0:			// Clear Display
                // From a clear state (elapsed time = 0),
                // start running.
                m_state = 1;
                m_start_time = SystemClock.elapsedRealtime();
                m_elapsed_time = 0;
                break;

            case 1:			// Running
                if (button == m_start_button) {
                    // Go to a simple stopped state.  Save our
                    // elapsed time and show it.
                    m_state = 4;
                    m_elapsed_time = SystemClock.elapsedRealtime()
                            - m_start_time
                            + m_elapsed_time;
                }
                else {
                    // Go to a split time state.  Show the current
                    // (split) time and keep all our variables as
                    // if we're still running.
                    m_state = 2;
                    m_split_time = SystemClock.elapsedRealtime()
                            - m_start_time
                            + m_elapsed_time;
                }
                break;

            case 2:			// Showing split time, running
                if (button == m_start_button) {
                    // Hit stop button.  Don't change the displayed
                    // time, but stop the counting by setting the
                    // elapsed time.
                    m_state = 3;
                    m_elapsed_time = (SystemClock.elapsedRealtime()
                            - m_start_time
                            + m_elapsed_time);
                }
                else {
                    // They hit the split button.  Make a new
                    // split time.
                    m_split_time = SystemClock.elapsedRealtime()
                            - m_start_time
                            + m_elapsed_time;
                }
                break;

            case 3:			// Showing split time, stopped
                if (button == m_start_button) {
                    // Start button hit, start counting again.
                    // Gotta reset m_start_time, but m_elapsed_time
                    // will take care of the pause.
                    m_state = 2;
                    m_start_time = SystemClock.elapsedRealtime();
                }
                else {
                    // Hit clear.  Go back to zero.
                    m_state = 0;
                    m_elapsed_time = 0;
                }
                break;

            case 4:			// Stopped showing stopped time
                if (button == m_start_button) {
                    // Hit start button.  Keep on truckin'!
                    m_state = 1;
                    m_start_time = SystemClock.elapsedRealtime();
                }
                else {
                    // Hit clear button.  Zero out everything.
                    m_state = 0;
                    m_elapsed_time = 0;
                }
                break;

            default:
                Toast.makeText(this, "Whoa! Illegal state!  What should I do?", Toast.LENGTH_LONG).show();
                finish();
                break;
        } // switch

        // Gotta do this!!!
        set_widgets();
    } // next_state (button)


    //-----------------------------
    //	Sets the widgets' text and appearance based the current
    //	state.
    //
    //	preconditions:
    //		m_state		Holds the correct state.
    //
    //	side effects:
    //		display widgets may appear/disappear, text can
    //		change, and buttons may be en/disabled.
    //
    void set_widgets() {
        switch (m_state) {
            case 0:			// Clear Display
                m_start_button.setText(R.string.start);
                m_split_button.setText(null);
                m_split_button.setEnabled(false);
                update_display(0, m_display1);
                m_display2.setVisibility(View.INVISIBLE);
                break;

            case 1:			// Running
                m_start_button.setText(R.string.stop);
                m_split_button.setText(R.string.split);
                m_split_button.setEnabled(true);
                break;

            case 2:			// Showing split time, running
                m_start_button.setText (R.string.stop);
                m_split_button.setText (R.string.split);
                m_split_button.setEnabled (true);
                m_display2.setVisibility (View.VISIBLE);
                update_display (m_split_time, m_display2);
                break;

            case 3:			// Showing split time, stopped
                m_start_button.setText(R.string.start);
                m_split_button.setText(R.string.clear);
                m_split_button.setEnabled(true);
                update_display (m_elapsed_time, m_display1);
                update_display (m_split_time, m_display2);
                m_display2.setVisibility (View.VISIBLE);
                break;

            case 4:			// Stopped showing stopped time
                m_start_button.setText(R.string.start);
                m_split_button.setText(R.string.clear);
                m_split_button.setEnabled(true);
                update_display (m_elapsed_time, m_display1);
                break;
        } // switch

    } // set_widgets()


    //-----------------------------
    //	The user hit a button.
    //
    @Override
    public void onClick(View v) {
        next_state (v);
//		Toast.makeText(this, "State " + m_state, Toast.LENGTH_SHORT).show();

        if (m_sound) {
            m_sound_mgr.playSound(1);
        }

        // NOTE:  Vibration may be done through the Button
        // itself.
        if (m_vibrate) {
            m_vibrator.vibrate (10);	// Vibrate 100 milliseconds
        }
    } // onClick (v)


    //-----------------------------
    //	Updates the main display with the given time.  This
    //	assumes that the time is in milliseconds and is the
    //	correct time to show.  All the processing of dividing
    //	it up into minutes, seconds, etc. will be done here.
    //
    //	input:
    //		milliseconds		Time to display
    //
    //		tv  			    The textview to display this time.
    //
    private void update_display (long milliseconds, TextView tv) {
        int hours = (int) (milliseconds / 3600000);
        long temp = milliseconds % 3600000;
        int minutes = (int) (temp / 60000);
        temp = temp % 60000;
        int seconds = (int) (temp / 1000);
        temp = temp % 1000;
        int hundredths = (int) (temp / 10);
        int tenths = hundredths / 10;

//		Log.i (tag, "hours = " + hours + ", minutes = " + minutes +
//				", seconds = " + seconds + ", hundredths = " + hundredths);

        String str;

        hours = hours % 100;    // Truncate hours

        if (hours < 1) {
            // Just minutes, seconds, and hundredths
            str = String.format("%d:%02d.%02d", minutes, seconds, hundredths);
        }

        else if (hours < 10) {
            str = String.format("%d:%02d:%02d.%1d", hours, minutes, seconds, tenths);
        }

        else {
            str = String.format("%d:%02d:%02d", hours, minutes, seconds);
        }

        tv.setText(str);

    } // update_display (milliseconds)


    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //  Classes
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //	Extending the CountDownTimer class allows me to neatly
    //	make my own
    class MyCountDown extends CountDownTimer {

        //------------------
        //	Constructor
        public MyCountDown (long millisInFuture,
                            long countDownInterval) {
            super (millisInFuture, countDownInterval);
        } // constructor

        //------------------
        //	Not used, but necessary.
        //
        @Override
        public void onFinish() {
        }

        //------------------
        //	Update the display (if we're in the right states)
        //
        @Override
        public void onTick (long millisUntilFinished) {
            if ((1 == m_state) || (2 == m_state)) {
                update_display (SystemClock.elapsedRealtime()
                        - m_start_time
                        + m_elapsed_time, m_display1);
            }
        } // onTick (millisUntilFinished)

    } // class MyCountDown


    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //	A class to manage sounds which are used to make
    //	button clicks audible.
    //
    //	I got this from the web:
    //
    //	http://www.droidnova.com/creating-sound-effects-in-android-part-1,570.html
    //
    public class SoundManager {

        // The Android provided object we use to create and
        // play sounds.
        private  SoundPool mSoundPool;

        // A Hashmap to store the sounds once they are loaded.
        private  HashMap mSoundPoolMap;

        // A handle to the service that plays the sounds we want.
        private  AudioManager  mAudioManager;

        // A handle to the application Context.
        private  Context mContext;


        //------------------------
        @SuppressLint("UseSparseArrays")        // Prevents the warning about using HashMap instead of SparseArray
        public void initSounds(Context context) {
            mContext = context;
            mSoundPool = new SoundPool(1,
                    AudioManager.STREAM_MUSIC,
                    0);
            mSoundPoolMap = new HashMap<Integer, Integer>();
            mAudioManager = (AudioManager)
                    mContext.getSystemService(Context.AUDIO_SERVICE);
        } // initSound (context)

        //------------------------
        //	Adds a sound to the system.
        //
        //	input:
        //		index		A number to associate this particular
        //					sound with.  Remember it!
        //
        //		SoundID		from raw resources file.  See example.
        //
        public void addSound (int index, int SoundID) {
            mSoundPoolMap.put (index,
                    mSoundPool.load(mContext, SoundID, 1));
        } // addSound (index, SoundID)

        //------------------------
        public void playSound(int index) {
            float streamVolume = mAudioManager
                    .getStreamVolume(AudioManager.STREAM_MUSIC);
            streamVolume = streamVolume
                    / mAudioManager
                    .getStreamMaxVolume(AudioManager.STREAM_MUSIC);

            // The example from the web would not compile here.
            mSoundPool.play (index,
                    streamVolume, streamVolume, 1, 0, 1f);
        } // playSound

        //------------------------
        public void playLoopedSound(int index) {
            float streamVolume = mAudioManager
                    .getStreamVolume(AudioManager.STREAM_MUSIC);
            streamVolume = streamVolume
                    / mAudioManager
                    .getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            // The example from the web would not compile here.
            mSoundPool.play (index,
                    streamVolume, streamVolume, 1, -1, 1f);
        } // playLoopedSound (index)


    } // class SoundManager


}
