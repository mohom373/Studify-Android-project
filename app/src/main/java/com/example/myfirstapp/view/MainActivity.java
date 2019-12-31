package com.example.myfirstapp.view;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myfirstapp.R;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInApi;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

import org.w3c.dom.Text;

import java.io.Serializable;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout mDrawer;

    private EditText mEditTimerInput;
    private TextView mTextViewCountDown;

    private ProgressBar mProgressBar;

    private Button mButtonEdit;
    private Button mButtonStart;
    private Button mButtonReset;

    private CountDownTimer mCountDownTimer;

    private boolean mTimerRunning;

    private long mStartTimeInMillis;
    private long mTimeLeftInMillis = mStartTimeInMillis;
    private long mEndTime;

    private String mPersonName;
    private String mPersonEmail;
    private TextView mNameId;

    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mFirebaseAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Creation of navigation Drawer
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDrawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        mDrawer.addDrawerListener(toggle);
        toggle.syncState();

        //##########################################################################################
        mFirebaseAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);

        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
        if (acct != null) {
            mPersonName = acct.getDisplayName();
            mPersonEmail = acct.getEmail();
        }

        View navHeaderView = navigationView.getHeaderView(0);

        TextView userName = navHeaderView.findViewById(R.id.userId);
        userName.setText(mPersonName);

        TextView userEmail = navHeaderView.findViewById(R.id.userEmail);
        userEmail.setText(mPersonEmail);

        //##########################################################################################

        // Creation of Timer views
        mTextViewCountDown = (TextView)findViewById(R.id.textViewCountdown);
        mEditTimerInput = (EditText)findViewById(R.id.editTextInput);
        mProgressBar = (ProgressBar)findViewById(R.id.progressBar);

        mButtonEdit = (Button)(findViewById(R.id.buttonEdit));
        mButtonStart = (Button)findViewById(R.id.buttonStart);
        mButtonReset = (Button)findViewById(R.id.buttonReset);

        mButtonEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String input = mEditTimerInput.getText().toString();
                if (input.length() == 0) {
                    Toast.makeText(MainActivity.this,
                            "Input cannot be empty, please enter a number",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                int turnToMinutes = 60000;
                long millisecondInput = Long.parseLong(input) * turnToMinutes;

                if (millisecondInput == 0) {
                    Toast.makeText(MainActivity.this,
                            "Please enter a number higher than 0", Toast.LENGTH_SHORT).show();
                    return;
                }

                setTimer(millisecondInput);
                mEditTimerInput.setText("");
            }
        });

        mButtonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!mTimerRunning) {
                    startTimer();
                }
            }
        });

        mButtonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetTimer();
            }
        });
        updateCountDownText();

    }
    //############################## ON CREATE ENDS #########################################

    public void changeActivity(){

        mFirebaseAuth.signOut();

        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, (task) -> {
                    //Toast.makeText(this, "SignOut Successful", Toast.LENGTH_SHORT).show();

                    startActivity(new Intent(this,
                                LoginPageActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));

                    finish();
                });
    }

    @Override
    public void onBackPressed() {
        if(mDrawer.isDrawerOpen(GravityCompat.START)) {
            mDrawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    private void setTimer(long milliseconds) {
        mStartTimeInMillis = milliseconds;
        resetTimer();
        closeKeyboard();
    }
    private void startTimer() {
        mEndTime = System.currentTimeMillis() + mTimeLeftInMillis;

        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mTimeLeftInMillis = millisUntilFinished;

                mProgressBar.setMax((int) mStartTimeInMillis / 1000);
                int progress = (int)(millisUntilFinished / 1000);
                mProgressBar.setProgress(mProgressBar.getMax() - progress);
                updateCountDownText();
            }

            @Override
            public void onFinish() {
                mTimerRunning = false;
            }
        }.start();

        mTimerRunning = true;
    }

    private void resetTimer() {
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
            mTimerRunning = false;
        }
        mTimeLeftInMillis = mStartTimeInMillis;
        mProgressBar.setProgress(0);
        updateCountDownText();
    }
    private void updateCountDownText() {

        int hours = (int)(mTimeLeftInMillis / 1000) / 3600;
        int minutes = (int)((mTimeLeftInMillis / 1000) % 3600) / 60;
        int seconds = (int)(mTimeLeftInMillis / 1000) % 60;

        String timeLeftFormatted = String.format(Locale.getDefault(),
                "%02d:%02d:%02d", hours, minutes, seconds);

        mTextViewCountDown.setText(timeLeftFormatted);
    }

    private void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("countDown", mTimeLeftInMillis);
        outState.putLong("startTimeInMillis", mStartTimeInMillis);
        outState.putLong("endTimer", mEndTime);
        outState.putBoolean("timerRunning", mTimerRunning);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mTimeLeftInMillis = savedInstanceState.getLong("countDown");
        mTimerRunning = savedInstanceState.getBoolean("timerRunning");
        mStartTimeInMillis = savedInstanceState.getLong("startTimeInMillis");
        updateCountDownText();

        if (mTimerRunning) {
            mEndTime = savedInstanceState.getLong("endTimer");
            mTimeLeftInMillis = mEndTime - System.currentTimeMillis();
            startTimer();
        }
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_sign_out:
                changeActivity();
                Toast.makeText(this, "I just got touched too im the message box!!!", Toast.LENGTH_SHORT).show();
                break;
        }

        mDrawer.closeDrawer(GravityCompat.START);
        return true;
    }
}

