package top.ox16.yuedu.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.baidu.tts.chainofresponsibility.logger.LoggerProxy;
import com.baidu.tts.client.SpeechError;
import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.SpeechSynthesizerListener;
import com.baidu.tts.client.TtsMode;
import com.hwangjr.rxbus.RxBus;

import top.ox16.yuedu.BuildConfig;
import top.ox16.yuedu.MApplication;
import top.ox16.yuedu.R;
import top.ox16.yuedu.baidutts.control.InitConfig;
import top.ox16.yuedu.baidutts.control.MySyntherizer;
import top.ox16.yuedu.baidutts.util.OfflineResource;
import top.ox16.yuedu.constant.RxBusTag;
import top.ox16.yuedu.help.MediaManager;
import top.ox16.yuedu.help.ReadBookControl;
import top.ox16.yuedu.view.activity.ReadBookActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.text.TextUtils.isEmpty;
import static top.ox16.yuedu.constant.AppConstant.ActionDoneService;

/**
 * Created by GKF on 2018/1/2.
 * 朗读服务
 */
public class ReadAloudService extends Service {
    private static final String TAG = ReadAloudService.class.getSimpleName();
    public static final String ActionMediaButton = "mediaButton";
    public static final String ActionNewReadAloud = "newReadAloud";
    public static final String ActionPauseService = "pauseService";
    public static final String ActionResumeService = "resumeService";
    private static final String ActionReadActivity = "readActivity";
    private static final String ActionSetTimer = "updateTimer";
    private static final String ActionSetProgress = "setProgress";
    private static final int notificationId = 3222;
    private static final long MEDIA_SESSION_ACTIONS = PlaybackStateCompat.ACTION_PLAY
            | PlaybackStateCompat.ACTION_PAUSE
            | PlaybackStateCompat.ACTION_PLAY_PAUSE
            | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            | PlaybackStateCompat.ACTION_STOP
            | PlaybackStateCompat.ACTION_SEEK_TO;
    public static Boolean running = false;
    private Boolean ttsInitSuccess = false;
    private Boolean speak = true;
    private Boolean pause = false;
    private List<String> contentList = new ArrayList<>();
    private int nowSpeak;
    private int timeMinute = 0;
    private boolean timerEnable = false;
    private AudioManager audioManager;
    private MediaSessionCompat mediaSessionCompat;
    private AudioFocusChangeListener audioFocusChangeListener;
    private AudioFocusRequest mFocusRequest;
    private BroadcastReceiver broadcastReceiver;
    private SharedPreferences preference;
    private String title;
    private String text;
    private boolean fadeTts;
    private Handler handler = new Handler();
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable dsRunnable;
    private Runnable mpRunnable;
    private MediaManager mediaManager;
    private int readAloudNumber;
    private boolean isAudio;
    private MediaPlayer mediaPlayer;
    private String audioUrl;
    private int progress;
    private ReadBookControl readBookControl = ReadBookControl.getInstance();
    private MySyntherizer synthesizer;
    private OfflineResource offlineResource;

    /**
     * 朗读
     */
    public static void play(Context context, Boolean aloudButton, String content, String title, String text, boolean isAudio, int progress) {
        Intent readAloudIntent = new Intent(context, ReadAloudService.class);
        readAloudIntent.setAction(ActionNewReadAloud);
        readAloudIntent.putExtra("aloudButton", aloudButton);
        readAloudIntent.putExtra("content", content);
        readAloudIntent.putExtra("title", title);
        readAloudIntent.putExtra("text", text);
        readAloudIntent.putExtra("isAudio", isAudio);
        readAloudIntent.putExtra("progress", progress);
        context.startService(readAloudIntent);
    }

    /**
     * @param context 停止
     */
    public static void stop(Context context) {
        if (running) {
            Intent intent = new Intent(context, ReadAloudService.class);
            intent.setAction(ActionDoneService);
            context.startService(intent);
        }
    }

    /**
     * @param context 暂停
     */
    public static void pause(Context context) {
        if (running) {
            Intent intent = new Intent(context, ReadAloudService.class);
            intent.setAction(ActionPauseService);
            context.startService(intent);
        }
    }

    /**
     * @param context 继续
     */
    public static void resume(Context context) {
        if (running) {
            Intent intent = new Intent(context, ReadAloudService.class);
            intent.setAction(ActionResumeService);
            context.startService(intent);
        }
    }

    public static void setTimer(Context context, int minute) {
        if (running) {
            Intent intent = new Intent(context, ReadAloudService.class);
            intent.setAction(ActionSetTimer);
            intent.putExtra("minute", minute);
            context.startService(intent);
        }
    }

    public static void setProgress(Context context, int progress) {
        if (running) {
            Intent intent = new Intent(context, ReadAloudService.class);
            intent.setAction(ActionSetProgress);
            intent.putExtra("progress", progress);
            context.startService(intent);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        running = true;
        preference = MApplication.getConfigPreferences();
        initTTS();
        audioFocusChangeListener = new AudioFocusChangeListener();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mediaManager = MediaManager.getInstance();
        mediaManager.setStream(TextToSpeech.Engine.DEFAULT_STREAM);
        fadeTts = preference.getBoolean("fadeTTS", false);
        dsRunnable = this::doDs;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initFocusRequest();
        }
        initMediaSession();
        initBroadcastReceiver();
        mediaSessionCompat.setActive(true);
        updateMediaSessionPlaybackState();
        updateNotification();
        mpRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null) {
                    RxBus.get().post(RxBusTag.AUDIO_DUR, mediaPlayer.getCurrentPosition());
                }
                handler.postDelayed(this, 1000);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ActionDoneService:
                        stopSelf();
                        break;
                    case ActionPauseService:
                        pauseReadAloud(true);
                        break;
                    case ActionResumeService:
                        resumeReadAloud();
                        break;
                    case ActionSetTimer:
                        updateTimer(intent.getIntExtra("minute", 10));
                        break;
                    case ActionNewReadAloud:
                        newReadAloud(intent.getStringExtra("content"),
                                intent.getBooleanExtra("aloudButton", false),
                                intent.getStringExtra("title"),
                                intent.getStringExtra("text"),
                                intent.getBooleanExtra("isAudio", false),
                                intent.getIntExtra("progress", 0));
                        break;
                    case ActionSetProgress:
                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                            mediaPlayer.seekTo(intent.getIntExtra("progress", 0));
                        }
                        break;
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }

    public class MyBinder extends Binder {
        public ReadAloudService getService() {
            return ReadAloudService.this;
        }
    }

    private void initTTS() {
        LoggerProxy.printable(false); // 日志打印在logcat中
        // 设置初始化参数
        // 此处可以改为 含有您业务逻辑的SpeechSynthesizerListener的实现类
        SpeechSynthesizerListener listener = new SpeechSyncListener();

        Map<String, String> params = getParams();

        String appId = BuildConfig.APP_ID;
        String appKey = BuildConfig.APP_KEY;
        String secretKey = BuildConfig.SECRET_KEY;

        // TtsMode.MIX; 离在线融合，在线优先； TtsMode.ONLINE 纯在线； 没有纯离线
        TtsMode ttsMode = TtsMode.MIX;
        // appId appKey secretKey 网站上您申请的应用获取。注意使用离线合成功能的话，需要应用中填写您app的包名。包名在build.gradle中获取。
        InitConfig initConfig = new InitConfig(appId, appKey, secretKey, ttsMode, params, listener);
        ttsInitSuccess = true;

        synthesizer = new MySyntherizer(this, initConfig, mainHandler); // 此处可以改为MySyntherizer 了解调用过程
    }

    private void initMediaPlayer() {
        if (mediaPlayer != null) return;
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            mainHandler.post(() ->
                    Toast.makeText(ReadAloudService.this, "播放出错", Toast.LENGTH_LONG).show());
            pauseReadAloud(true);
            mp.reset();
            return false;
        });
        mediaPlayer.setOnPreparedListener(mp -> {
            mp.start();
            mp.seekTo(progress);
            speak = true;
            RxBus.get().post(RxBusTag.ALOUD_STATE, Status.PLAY);
            RxBus.get().post(RxBusTag.AUDIO_SIZE, mp.getDuration());
            RxBus.get().post(RxBusTag.AUDIO_DUR, mp.getCurrentPosition());
            handler.postDelayed(mpRunnable, 1000);
        });
        mediaPlayer.setOnCompletionListener(mp -> {
            handler.removeCallbacks(mpRunnable);
            mp.reset();
            RxBus.get().post(RxBusTag.ALOUD_STATE, Status.NEXT);
        });
    }

    private void newReadAloud(String content, Boolean aloudButton, String title, String text, boolean isAudio, int progress) {
        if (TextUtils.isEmpty(content)) {
            stopSelf();
            return;
        }
        synthesizer.setParams(getParams());
        synthesizer.loadModel(offlineResource.getModelFilename(), offlineResource.getTextFilename());
        this.text = text;
        this.title = title;
        this.progress = progress;
        this.isAudio = isAudio;
        nowSpeak = 0;
        readAloudNumber = 0;
        contentList.clear();
        if (isAudio) {
            initMediaPlayer();
            audioUrl = content;
        } else {
            String[] splitSpeech = content.split("\n");
            for (String aSplitSpeech : splitSpeech) {
                if (!isEmpty(aSplitSpeech)) {
                    contentList.add(aSplitSpeech);
                }
            }
        }
        if (aloudButton || speak) {
            synthesizer.stop();
            speak = false;
            pause = false;
            playTTS();
        }
    }

    public void playTTS() {
        updateNotification();
        if (isAudio) {
            try {
                mediaPlayer.reset();
                mediaPlayer.setDataSource(audioUrl);
                mediaPlayer.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (fadeTts) {
                AsyncTask.execute(() -> mediaManager.fadeInVolume());
                handler.postDelayed(this::playTTSN, 200);
            } else {
                playTTSN();
            }
        }
    }

    public void playTTSN() {
        if (contentList.size() < 1) {
            RxBus.get().post(RxBusTag.ALOUD_STATE, Status.NEXT);
            return;
        }
        if (ttsInitSuccess && !speak && requestFocus()) {
            speak = !speak;
            RxBus.get().post(RxBusTag.ALOUD_STATE, Status.PLAY);
            updateNotification();
            for (int i = nowSpeak; i < contentList.size(); i++) {
                synthesizer.speak(contentList.get(i), String.valueOf(i));
            }
        }
    }

    /**
     * @param pause true 暂停, false 失去焦点
     */
    private void pauseReadAloud(Boolean pause) {
        this.pause = pause;
        speak = false;
        updateNotification();
        updateMediaSessionPlaybackState();
        if (isAudio) {
            if (mediaPlayer != null && mediaPlayer.isPlaying())
                mediaPlayer.pause();
        } else {
            if (fadeTts) {
                AsyncTask.execute(() -> mediaManager.fadeOutVolume());
                handler.postDelayed(() -> synthesizer.stop(), 300);
            } else {
                synthesizer.stop();
            }
        }
        RxBus.get().post(RxBusTag.ALOUD_STATE, Status.PAUSE);
    }

    /**
     * 恢复朗读
     */
    private void resumeReadAloud() {
        synthesizer.setParams(getParams());
        synthesizer.loadModel(offlineResource.getModelFilename(), offlineResource.getTextFilename());
        updateTimer(0);
        pause = false;
        updateNotification();
        if (isAudio) {
            if (mediaPlayer != null && !mediaPlayer.isPlaying())
                mediaPlayer.start();
        } else {
            playTTS();
        }
        RxBus.get().post(RxBusTag.ALOUD_STATE, Status.PLAY);
    }

    private void updateTimer(int minute) {
        timeMinute = timeMinute + minute;
        int maxTimeMinute = 60;
        if (timeMinute > maxTimeMinute) {
            timerEnable = false;
            handler.removeCallbacks(dsRunnable);
            timeMinute = 0;
            updateNotification();
        } else if (timeMinute <= 0) {
            if (timerEnable) {
                handler.removeCallbacks(dsRunnable);
                stopSelf();
            }
        } else {
            timerEnable = true;
            updateNotification();
            handler.removeCallbacks(dsRunnable);
            handler.postDelayed(dsRunnable, 60000);
        }
    }

    private void doDs() {
        if (!pause) {
            setTimer(this, -1);
        }
    }

    private PendingIntent getReadBookActivityPendingIntent() {
        Intent intent = new Intent(this, ReadBookActivity.class);
        intent.setAction(ReadAloudService.ActionReadActivity);
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getThisServicePendingIntent(String actionStr) {
        Intent intent = new Intent(this, this.getClass());
        intent.setAction(actionStr);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * 更新通知
     */
    private void updateNotification() {
        if (text == null)
            text = getString(R.string.read_aloud_s);
        String nTitle;
        if (pause) {
            nTitle = getString(R.string.read_aloud_pause);
        } else if (timeMinute > 0 && timeMinute <= 60) {
            nTitle = getString(R.string.read_aloud_timer, timeMinute);
        } else {
            nTitle = getString(R.string.read_aloud_t);
        }
        nTitle += ": " + title;
        RxBus.get().post(RxBusTag.ALOUD_TIMER, nTitle);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, MApplication.channelIdReadAloud)
                .setSmallIcon(R.drawable.ic_volume_up)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_read_book))
                .setOngoing(true)
                .setContentTitle(nTitle)
                .setContentText(text)
                .setContentIntent(getReadBookActivityPendingIntent());
        if (pause) {
            builder.addAction(R.drawable.ic_play_24dp, getString(R.string.resume), getThisServicePendingIntent(ActionResumeService));
        } else {
            builder.addAction(R.drawable.ic_pause_24dp, getString(R.string.pause), getThisServicePendingIntent(ActionPauseService));
        }
        builder.addAction(R.drawable.ic_stop_black_24dp, getString(R.string.stop), getThisServicePendingIntent(ActionDoneService));
        builder.addAction(R.drawable.ic_time_add_24dp, getString(R.string.set_timer), getThisServicePendingIntent(ActionSetTimer));
        builder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSessionCompat.getSessionToken())
                .setShowActionsInCompactView(0, 1, 2));
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        Notification notification = builder.build();
        startForeground(notificationId, notification);
    }

    @Override
    public void onDestroy() {
        running = false;
        super.onDestroy();
        stopForeground(true);
        handler.removeCallbacks(dsRunnable);
        RxBus.get().post(RxBusTag.ALOUD_STATE, Status.STOP);
        unRegisterMediaButton();
        unregisterReceiver(broadcastReceiver);
        clearTTS();
    }

    private void clearTTS() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (synthesizer != null) {
            if (fadeTts) {
                AsyncTask.execute(() -> mediaManager.fadeOutVolume());
            }
            synthesizer.stop();
            synthesizer.release();
            synthesizer = null;
        }
    }

    private void unRegisterMediaButton() {
        if (mediaSessionCompat != null) {
            mediaSessionCompat.setCallback(null);
            mediaSessionCompat.setActive(false);
            mediaSessionCompat.release();
        }
        audioManager.abandonAudioFocus(audioFocusChangeListener);
    }

    /**
     * @return 音频焦点
     */
    private boolean requestFocus() {
        if (!isAudio) {
            MediaManager.playSilentSound(this);
        }
        int request;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            request = audioManager.requestAudioFocus(mFocusRequest);
        } else {
            request = audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
        return (request == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void initFocusRequest() {
        AudioAttributes mPlaybackAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        mFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(mPlaybackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build();
    }

    /**
     * 初始化MediaSession
     */
    private void initMediaSession() {
        ComponentName mComponent = new ComponentName(getPackageName(), MediaButtonIntentReceiver.class.getName());
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(mComponent);
        PendingIntent mediaButtonReceiverPendingIntent = PendingIntent.getBroadcast(this, 0,
                mediaButtonIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        mediaSessionCompat = new MediaSessionCompat(this, TAG, mComponent, mediaButtonReceiverPendingIntent);
        mediaSessionCompat.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
                return MediaButtonIntentReceiver.handleIntent(ReadAloudService.this, mediaButtonEvent);
            }
        });
        mediaSessionCompat.setMediaButtonReceiver(mediaButtonReceiverPendingIntent);
    }

    private void initBroadcastReceiver() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action)) {
                    pauseReadAloud(true);
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(broadcastReceiver, intentFilter);
    }

    private void updateMediaSessionPlaybackState() {
        mediaSessionCompat.setPlaybackState(
                new PlaybackStateCompat.Builder()
                        .setActions(MEDIA_SESSION_ACTIONS)
                        .setState(speak ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED,
                                nowSpeak, 1)
                        .build());
    }

    /**
     * 合成的参数，可以初始化时填写，也可以在合成前设置。
     */
    protected Map<String, String> getParams() {
        Map<String, String> params = new HashMap<>();
        // 以下参数均为选填
        String speaker = mapSpeakerByRadioIndex(readBookControl.getSpeechSpeaker());
        // 设置在线发声音人： 0 普通女声（默认） 1 普通男声 2 特别男声 3 情感男声<度逍遥> 4 情感儿童声<度丫丫>
        params.put(SpeechSynthesizer.PARAM_SPEAKER, speaker);
        // 设置合成的音量，0-15 ，默认 5
        params.put(SpeechSynthesizer.PARAM_VOLUME, "15");
        // 设置合成的语速，0-15 ，默认 5
        params.put(SpeechSynthesizer.PARAM_SPEED, String.valueOf(readBookControl.getSpeechRate()));
        // 设置合成的语调，0-15 ，默认 5
        params.put(SpeechSynthesizer.PARAM_PITCH, String.valueOf(readBookControl.getSpeechPitch()));

        params.put(SpeechSynthesizer.PARAM_MIX_MODE, SpeechSynthesizer.MIX_MODE_DEFAULT);
        // 该参数设置为TtsMode.MIX生效。即纯在线模式不生效。
        // MIX_MODE_DEFAULT 默认 ，wifi状态下使用在线，非wifi离线。在线状态下，请求超时6s自动转离线
        // MIX_MODE_HIGH_SPEED_SYNTHESIZE_WIFI wifi状态下使用在线，非wifi离线。在线状态下， 请求超时1.2s自动转离线
        // MIX_MODE_HIGH_SPEED_NETWORK ， 3G 4G wifi状态下使用在线，其它状态离线。在线状态下，请求超时1.2s自动转离线
        // MIX_MODE_HIGH_SPEED_SYNTHESIZE, 2G 3G 4G wifi状态下使用在线，其它状态离线。在线状态下，请求超时1.2s自动转离线

        // 离线发音选择，VOICE_FEMALE即为离线女声发音。
        String offlineVoice;
        switch (speaker) {
            case "0":
                offlineVoice = OfflineResource.VOICE_FEMALE;
                break;
            case "1":
                offlineVoice = OfflineResource.VOICE_MALE;
                break;
            case "3":
            case "106":
                offlineVoice = OfflineResource.VOICE_DUXY;
                break;
            case "4":
            case "110":
            case "103":
                offlineVoice = OfflineResource.VOICE_DUYY;
                break;
            default:
                offlineVoice = OfflineResource.VOICE_FEMALE;
                break;
        }

        // 离线资源文件， 从assets目录中复制到临时目录，需要在initTTs方法前完成
        offlineResource = createOfflineResource(offlineVoice);
        // 声学模型文件路径 (离线引擎使用), 请确认下面两个文件存在
        params.put(SpeechSynthesizer.PARAM_TTS_TEXT_MODEL_FILE, offlineResource.getTextFilename());
        params.put(SpeechSynthesizer.PARAM_TTS_SPEECH_MODEL_FILE,
                offlineResource.getModelFilename());
        return params;
    }

    private String mapSpeakerByRadioIndex(int index) {
        String speaker;
        switch (index) {
            case 0:
                speaker = "0";
                break;
            case 1:
                speaker = "1";
                break;
            case 2:
                speaker = "3";
                break;
            case 3:
                speaker = "4";
                break;
            case 4:
                speaker = "106";
                break;
            case 5:
                speaker = "110";
                break;
            case 6:
                speaker = "111";
                break;
            case 7:
                speaker = "103";
                break;
            case 8:
                speaker = "5";
                break;
            default:
                speaker = "0";
                break;
        }
        return speaker;
    }

    protected OfflineResource createOfflineResource(String voiceType) {
        OfflineResource offlineResource = null;
        try {
            offlineResource = new OfflineResource(this, voiceType);
        } catch (IOException e) {
            // IO 错误自行处理
            e.printStackTrace();
//            toPrint("【error】:copy files from assets failed." + e.getMessage());
        }
        return offlineResource;
    }

    /**
     * 朗读监听
     */
    private class SpeechSyncListener implements SpeechSynthesizerListener {
        private static final String TAG = "SpeechSyncListener";

        /**
         * 播放开始，每句播放开始都会回调
         *
         * @param utteranceId utteranceId
         */
        @Override
        public void onSynthesizeStart(String utteranceId) {
//            sendMessage("准备开始合成,序列号:" + utteranceId);
        }

        /**
         * 语音流 16K采样率 16bits编码 单声道 。
         *
         * @param utteranceId utteranceId
         * @param bytes       二进制语音 ，注意可能有空data的情况，可以忽略
         * @param progress    如合成“百度语音问题”这6个字， progress肯定是从0开始，到6结束。 但progress无法和合成到第几个字对应。
         */
        @Override
        public void onSynthesizeDataArrived(String utteranceId, byte[] bytes, int progress) {
            //  Log.i(TAG, "合成进度回调, progress：" + progress + ";序列号:" + utteranceId );
        }

        /**
         * 合成正常结束，每句合成正常结束都会回调，如果过程中出错，则回调onError，不再回调此接口
         *
         * @param utteranceId utteranceId
         */
        @Override
        public void onSynthesizeFinish(String utteranceId) {
//            sendMessage("合成结束回调, 序列号:" + utteranceId);
        }

        @Override
        public void onSpeechStart(String utteranceId) {
            sendMessage("播放开始回调, 序列号:" + utteranceId);
            updateMediaSessionPlaybackState();
            RxBus.get().post(RxBusTag.READ_ALOUD_START, readAloudNumber + 1);
            RxBus.get().post(RxBusTag.READ_ALOUD_NUMBER, readAloudNumber + 1);
        }

        /**
         * 播放进度回调接口，分多次回调
         *
         * @param utteranceId utteranceId
         * @param progress    如合成“百度语音问题”这6个字， progress肯定是从0开始，到6结束。 但progress无法保证和合成到第几个字对应。
         */
        @Override
        public void onSpeechProgressChanged(String utteranceId, int progress) {
            //  Log.i(TAG, "播放进度回调, progress：" + progress + ";序列号:" + utteranceId );
        }

        /**
         * 播放正常结束，每句播放正常结束都会回调，如果过程中出错，则回调onError,不再回调此接口
         *
         * @param utteranceId utteranceId
         */
        @Override
        public void onSpeechFinish(String utteranceId) {
            sendMessage("播放结束回调, 序列号:" + utteranceId);
            readAloudNumber = readAloudNumber + contentList.get(nowSpeak).length() + 1;
            nowSpeak = nowSpeak + 1;
            if (nowSpeak >= contentList.size()) {
                RxBus.get().post(RxBusTag.ALOUD_STATE, Status.NEXT);
            }
        }

        /**
         * 当合成或者播放过程中出错时回调此接口
         *
         * @param utteranceId utteranceId
         * @param speechError 包含错误码和错误信息
         */
        @Override
        public void onError(String utteranceId, SpeechError speechError) {
            sendErrorMessage("错误发生：" + speechError.description + "，错误编码："
                    + speechError.code + "，序列号:" + utteranceId);
            pauseReadAloud(true);
            RxBus.get().post(RxBusTag.ALOUD_STATE, Status.PAUSE);
        }

        private void sendErrorMessage(String message) {
            sendMessage(message, true);
        }

        private void sendMessage(String message) {
            sendMessage(message, false);
        }

        private void sendMessage(String message, boolean isError) {
            if (BuildConfig.DEBUG) {
                if (isError) {
                    Log.e(TAG, message);
                } else {
                    Log.i(TAG, message);
                }
            }
        }
    }

    class AudioFocusChangeListener implements AudioManager.OnAudioFocusChangeListener {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    // 重新获得焦点,  可做恢复播放，恢复后台音量的操作
                    if (!pause) {
                        resumeReadAloud();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    // 永久丢失焦点除非重新主动获取，这种情况是被其他播放器抢去了焦点，  为避免与其他播放器混音，可将音乐暂停
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    // 暂时丢失焦点，这种情况是被其他应用申请了短暂的焦点，可压低后台音量
                    if (!pause) {
                        pauseReadAloud(false);
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    // 短暂丢失焦点，这种情况是被其他应用申请了短暂的焦点希望其他声音能压低音量（或者关闭声音）凸显这个声音（比如短信提示音），
                    break;
            }
        }
    }

    public enum Status {
        PLAY, STOP, PAUSE, NEXT
    }

}
