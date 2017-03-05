/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license.
 * //
 * Project Oxford: http://ProjectOxford.ai
 * //
 * ProjectOxford SDK GitHub:
 * https://github.com/Microsoft/ProjectOxford-ClientSDK
 * //
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 * //
 * MIT License:
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * //
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * //
 * THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.microsoft.CognitiveServicesExample;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements SpeechRecoCallbacks {
    public static final String TAG = "CGK";

    TextView _logText;
    Button _startButton;
    Spinner filenameSelector;
    List<HandlerThread> handlerThreads = new ArrayList<>();
    List<Handler> handlers = new ArrayList<>();
    String filename;

    /**
     * Gets the default locale.
     *
     * @return The default locale.
     */
    private String getDefaultLocale() {
        return "en-us";
    }

    /**
     * Gets the Cognitive Service Authentication Uri.
     *
     * @return The Cognitive Service Authentication Uri.  Empty if the global default is to be used.
     */
    private String getAuthenticationUri() {
        return this.getString(R.string.authenticationUri);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate() called with: savedInstanceState = [" + savedInstanceState + "]");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this._logText = (TextView) findViewById(R.id.editText1);
        this._startButton = (Button) findViewById(R.id.button1);
        this.filenameSelector = (Spinner) findViewById(R.id.filename);
        this.filenameSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filename = parent.getItemAtPosition(position).toString();
                Log.d(TAG, "onItemSelected: filename set to " + filename);
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {
                filename = null;
            }
        });

        if (getString(R.string.primaryKey).startsWith("Please")) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.add_subscription_key_tip_title))
                    .setMessage(getString(R.string.add_subscription_key_tip))
                    .setCancelable(false)
                    .show();
        }

        // setup the buttons
        final MainActivity This = this;
        this._startButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                This.StartButton_Click(arg0);
            }
        });

    }

    @Override protected void onStop() {
        super.onStop();
        for(Handler handler : handlers) {
            handler.removeCallbacksAndMessages(null);
        }

        for(HandlerThread handlerThread : handlerThreads) {
            handlerThread.quitSafely();
        }
    }

    /**
     * Handles the Click event of the _startButton control.
     */
    private void StartButton_Click(@SuppressWarnings("UnusedParameters") View arg0) {
        if(filename == null) {
            Toast.makeText(this, "pick a file name first", Toast.LENGTH_LONG).show();
            return;
        }

        this.LogRecognitionStart();
        _startButton.setEnabled(false);

        HandlerThread reco = new HandlerThread("reco");
        reco.start();
        SpeechRecoHandler handler = new SpeechRecoHandler(getString(R.string.primaryKey),
                getAuthenticationUri(),
                reco,
                getApplicationContext(), this);

        handler.SendAudioHelper(filename);
        handlerThreads.add(reco);
        handlers.add(handler);
    }

    /**
     * Logs the recognition start.
     */
    private void LogRecognitionStart() {

        this.WriteLine("\n--- Start speech recognition using long wav file with " +
                "Long dictation" + " mode in " + this.getDefaultLocale() + " language ----\n\n");
    }


    /**
     * Writes the line.
     *
     * @param text The line to write.
     */
    private void WriteLine(String text) {
        //_logText.setText(text);
        Log.d("CGK", text);
    }

    @Override public void onFinish(final String output) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                _logText.setText(output);
                _startButton.setEnabled(true);
            }
        });
    }
}
