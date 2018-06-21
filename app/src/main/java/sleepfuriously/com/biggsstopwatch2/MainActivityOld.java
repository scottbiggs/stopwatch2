package sleepfuriously.com.biggsstopwatch2;

import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;

/**
 * This was my work-area for trying new ways to do the stopwatch.
 * I subsequently went back and copied my original working program
 * into what is now the MainActivity.
 */
public class MainActivityOld extends AppCompatActivity {

    //------------------------
    //  Constants
    //------------------------


    //------------------------
    //  Widgets
    //------------------------

    private TextView m_main_tv, m_split_tv;

    private Button m_start_stop_butt, m_split_reset_butt;

    //------------------------
    //  Data
    //------------------------

    /** When TRUE, the timer is currently running */
    private boolean m_running = false;

    /** The time (in millis since 1970) when the start was first pressed */
    private long m_startTime = 0L;

    /** Used to periodically update a running stopwatch's TextView */
    private Handler m_handler = new Handler();


    //------------------------
    //  Data-Methods
    //------------------------

    /**
     * This runs when the timer is counting, updating the counter
     * Textview periodically.
     */
    private Runnable mTimer = new Runnable() {
        @Override
        public void run() {

            long now = System.currentTimeMillis();
            long elapsed = now - m_startTime;
            long hours = elapsed / (1000 * 60 * 60);
            elapsed = elapsed - (hours * 1000 * 60 * 60);
            long minutes = elapsed / (1000 * 60);
            elapsed = elapsed - (minutes * 1000 * 60);
            long seconds = elapsed / 1000;
            elapsed = elapsed - (seconds * 1000);
            long hundredths = elapsed / 10;

            // Only use 2 digits for display of hours
            if (hours > 99) {
                hours %= 100;
            }

            String str = String.format("%02d:%02d:%02d.%02d", hours, minutes, seconds, hundredths);
            m_main_tv.setText(str);
        }
    };


    //------------------------
    //  Methods
    //------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // The buttons
        m_start_stop_butt = findViewById(R.id.butt_1);
        m_split_reset_butt = findViewById(R.id.butt_2);

        m_start_stop_butt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                do_start_stop_click();
            }
        });

        m_split_reset_butt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // todo
            }
        });



    } // onCreate(.)


    private void do_start_stop_click() {
        // First, figure out if this is a start or a stop.
        if (m_running) {
            // Stop the timer
            m_running = false;
        }

        else {
            // Start the timer
            m_running = true;
            m_startTime = System.currentTimeMillis();

            // todo
//            m_handler.po
        }


        Chronometer chronometer = new Chronometer(this);
        chronometer.start();

    } // do_start_stop_click()

}
