package sleepfuriously.com.biggsstopwatch2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import java.util.HashMap;

/**
 * Class to play sounds.
 *
 * Retrived from the Web:
 *      http://www.droidnova.com/creating-sound-effects-in-android-part-1,570.html
 */
public class SoundManager {

    /** The Android provided object we use to create and play sounds. */
    private SoundPool mSoundPool;

    /** A Hashmap to store the sounds once they are loaded. */
    private HashMap mSoundPoolMap;

    /** A handle to the service that plays the sounds we want. */
    private AudioManager mAudioManager;

    /** A handle to the application Context. */
    private Context mContext;


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

}
