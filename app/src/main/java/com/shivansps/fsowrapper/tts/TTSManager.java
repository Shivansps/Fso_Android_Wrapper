package com.shivansps.fsowrapper.tts;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TTSManager implements TextToSpeech.OnInitListener {
    private static volatile TextToSpeech tts;
    private static volatile boolean ready;
    private static volatile String desiredLangTag = null;
    private static volatile float desiredRate  = 1.0f;
    private static volatile float desiredPitch = 1.0f;
    private static final List<String> pending = new ArrayList<>();

    private TTSManager(){}

    public static synchronized void init(Context ctx) {
        if (tts != null) return;
        tts = new TextToSpeech(ctx.getApplicationContext(), new TTSManager());
    }

    public static boolean isReady() { return ready; }

    public static synchronized void setLanguageTag(String bcp47) {
        desiredLangTag = bcp47;
        if (tts != null && ready) {
            if (Build.VERSION.SDK_INT >= 21) {
                tts.setLanguage(Locale.forLanguageTag(bcp47));
            } else {
                String[] p = bcp47.split("[-_]");
                tts.setLanguage((p.length>=2) ? new Locale(p[0], p[1]) : new Locale(p[0]));
            }
        }
    }
    public static synchronized void setRate(float rate) {
        desiredRate = rate;
        if (tts != null && ready) tts.setSpeechRate(rate);
    }
    public static synchronized void setPitch(float pitch) {
        desiredPitch = pitch;
        if (tts != null && ready) tts.setPitch(pitch);
    }

    public static synchronized boolean speak(String text) {
        if (tts == null) return false;
        if (!ready) { pending.add(text); return true; }
        String id = String.valueOf(System.nanoTime());
        int res;
        if (Build.VERSION.SDK_INT >= 21) {
            Bundle params = new Bundle();
            res = tts.speak(text != null ? text : "", TextToSpeech.QUEUE_FLUSH, params, id);
        } else {
            @SuppressWarnings("deprecation")
            java.util.HashMap<String,String> params = new java.util.HashMap<>();
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, id);
            res = tts.speak(text != null ? text : "", TextToSpeech.QUEUE_FLUSH, params);
        }
        return res == TextToSpeech.SUCCESS;
    }

    public static boolean stop()   { return tts != null && tts.stop()   == TextToSpeech.SUCCESS; }
    public static boolean pause()  { return stop(); }
    public static boolean resume() { return false; }
    public static boolean isSpeaking() {
        return speaking;
    }

    private static volatile boolean speaking = false;

    @Override public void onInit(int status) {
        ready = (status == TextToSpeech.SUCCESS);
        if (!ready) return;

        if (Build.VERSION.SDK_INT >= 21) {
            tts.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build());
        }

        if (desiredLangTag != null) setLanguageTag(desiredLangTag);
        else                        tts.setLanguage(Locale.getDefault());
        tts.setSpeechRate(desiredRate);
        tts.setPitch(desiredPitch);

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String id) { speaking = true; }
            @Override public void onDone(String id)  { speaking = false; }
            @Override public void onError(String id) { speaking = false; }
        });

        List<String> toSpeak;
        synchronized (TTSManager.class) { toSpeak = new ArrayList<>(pending); pending.clear(); }
        for (String s : toSpeak) speak(s);
    }

    public static synchronized void shutdown() {
        if (tts != null) { tts.shutdown(); tts = null; }
        ready = false; speaking = false; pending.clear();
    }
}