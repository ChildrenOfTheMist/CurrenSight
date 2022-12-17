package com.example.cameratest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

public class SplashActivity extends AppCompatActivity {
    TextToSpeech textToSpeech;
    MediaPlayer filipino;

    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String LANG = "lang";
    public static final String ONCE = "open";
    public String language = "en";
    public String once = "true";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        filipino = MediaPlayer.create(this, R.raw.splash);
        getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        loadLang();
        loadAsk();
        int delay = 5000;
        switch(language){
            case "en":
                String greetings = "Welcome to curren sight! ";
                speakResult(greetings);
                delay = 2500;
                break;

            case "ph":
                filipino.start();
                delay = 3000;
                break;
        }

        if (once == "true"){
            String instruction = "Greetings, Since its your first time, here is a quick training on how to use the application." +
                                "After opening the application, hold the banknote in your non-dominant hand while your mobile phone is in your dominant hand. Make sure that the banknote is not folded." +
                                "Next step is to place your phone on your heart or on your chest level. After that, place the hand holding the banknote in front of your phone. Count 1 to 3, while counting, slowly move the hand with the banknote forward. Once you reach 3, stop moving. " +
                                "Then, tap any part of the screen to capture and wait for the result which is delivered through a voiceover. Make sure to capture the banknote multiple times. " +
                                "To switch language, tap and hold any part of the screen. You can only switch between English and Filipino. That is all, don't forget to allow the permission, the application needs the camera to perform. Thank you and ";
            speakResult(instruction);
            saveState("false");
            delay = 52500;
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish();

            }
        }, delay);


    }

    private void speakResult(String word){
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech.speak(word, TextToSpeech.QUEUE_ADD, null);
                }
            }
        });
    }

    public void loadLang(){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        language = sharedPreferences.getString(LANG, "en");

    }

    public void loadAsk(){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        once = sharedPreferences.getString(ONCE, "true");

    }

    public void saveState(String bool){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(ONCE, bool);
        editor.apply();

    }

}