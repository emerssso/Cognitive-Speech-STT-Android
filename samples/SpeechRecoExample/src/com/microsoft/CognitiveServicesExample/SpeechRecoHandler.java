package com.microsoft.CognitiveServicesExample;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.microsoft.bing.speech.SpeechClientStatus;
import com.microsoft.cognitiveservices.speechrecognition.DataRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.ISpeechRecognitionServerEvents;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionResult;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionStatus;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionMode;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionServiceFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

public class SpeechRecoHandler extends Handler implements ISpeechRecognitionServerEvents {

    private static final String TAG = "SpeechRecoHandler";

    private final DataRecognitionClient dataClient;
    private final SpeechRecoCallbacks callbacks;
    private Context context;
    private String filename;

    SpeechRecoHandler(String pkey, String uri, HandlerThread thread, Context context,
                      SpeechRecoCallbacks callbacks) {
        super(thread.getLooper());
        this.dataClient = SpeechRecognitionServiceFactory.createDataClient(
                SpeechRecognitionMode.LongDictation,
                "en-us",
                this,
                pkey);
        dataClient.setAuthenticationUri(uri);
        this.context = context;
        this.callbacks = callbacks;
        this.filename = null;
    }

    /**
     * Gets the current speech recognition mode.
     *
     * @return The speech recognition mode.
     */
    private SpeechRecognitionMode getMode() {
        return SpeechRecognitionMode.LongDictation;
    }

    @SuppressWarnings("FinalizeCalledExplicitly")
    public void onFinalResponseReceived(final RecognitionResult response) {
        post(new Runnable() {
            @Override public void run() {
                boolean isFinalDicationMessage = SpeechRecoHandler.this.getMode() == SpeechRecognitionMode.LongDictation &&
                        (response.RecognitionStatus == RecognitionStatus.EndOfDictation ||
                                response.RecognitionStatus == RecognitionStatus.DictationEndSilenceTimeout);

                if (!isFinalDicationMessage) {
                    WriteLine("********* Final n-BEST Results *********");
                    Log.d(TAG, "onFinalResponseReceived: response length: " + response.Results.length);
                    for (int i = 0; i < response.Results.length; i++) {
                        Log.d("FINAL_RESULT", ("[" + i + "]" +
                                " Confidence=" + response.Results[i].Confidence +
                                " Text=\"" + response.Results[i].DisplayText + "\""));

                        File dir = Environment.getExternalStorageDirectory();
                        boolean result = dir.mkdirs();
                        Log.d(TAG, "run: result = " + result);

                        File file = new File(dir, filename + ".txt");

                        FileWriter fw = null;
                        BufferedWriter bw = null;
                        try {
                            fw = new FileWriter(file.getAbsoluteFile(), true);
                            bw = new BufferedWriter(fw);

                            final String value = filename + "\n" + response.Results[i].DisplayText;
                            Log.d(TAG, "run: about to write file value: " + value);
                            Log.d(TAG, "run: to file: " + file.getAbsolutePath());
                            bw.write(value);
                        } catch (Exception e) {
                            Log.w(TAG, "run: failed to write file", e);
                        } finally {
                            if(bw != null) {
                                try {
                                    bw.close();
                                } catch (IOException e) {
                                    Log.w(TAG, "run: failed to close file", e);
                                }
                            }

                            if(fw != null) {
                                try {
                                    fw.close();
                                } catch (IOException e) {
                                    Log.w(TAG, "run: failed to close file", e);
                                }
                            }
                        }
                    }
                    callbacks.onFinish(response.Results[0].DisplayText);

                    WriteLine();
                }

                // Reset everything
                if (dataClient != null) {
                    try {
                        dataClient.finalize();
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                }
            }
        });
    }

    void SendAudioHelper(final String filename) {
        this.filename = filename;

        post(new Runnable() {
            @Override public void run() {
                try {
                    final InputStream fileStream = context.getAssets().open(filename + ".wav");
                    try {
                        int bytesRead;
                        byte[] buffer = new byte[4096];
                        long totalBytes = 0;

                        do {
                            // Get  Audio data to send into byte buffer.
                            bytesRead = fileStream.read(buffer);
                            totalBytes += bytesRead;

                            if (bytesRead > -1) {
                                // Send of audio data to service.
                                dataClient.sendAudio(buffer, bytesRead);
                            }
                            Log.d(TAG, "doInBackground: bytesRead: " + bytesRead + " total bytes: " + totalBytes);
                        } while (bytesRead > 0);

                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    } finally {
                        dataClient.endAudio();
                    }
                } catch (Exception e) {
                    Log.w(TAG, "run: ", e);
                }
            }
        });
    }

    /**
     * Called when a final response is received and its intent is parsed
     */
    public void onIntentReceived(final String payload) {
        Log.d(TAG, "onIntentReceived: " + payload);
    }

    public void onPartialResponseReceived(final String response) {
        Log.d(TAG, "onPartialResponseReceived: " + response);
    }

    public void onError(final int errorCode, final String response) {
        this.WriteLine("--- Error received by onError() ---");
        this.WriteLine("Error code: " + SpeechClientStatus.fromInt(errorCode) + " " + errorCode);
        this.WriteLine("Error text: " + response);
        this.WriteLine();
    }

    /**
     * Called when the microphone status has changed.
     *
     * @param recording The current recording state
     */
    public void onAudioEvent(boolean recording) {
        this.WriteLine("--- Microphone status change received by onAudioEvent() ---");
        this.WriteLine("********* Microphone status: " + recording + " *********");
        if (recording) {
            this.WriteLine("Please start speaking.");
        }

        WriteLine();
    }

    /**
     * Writes the line.
     */
    private void WriteLine() {
        this.WriteLine("");
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
}
