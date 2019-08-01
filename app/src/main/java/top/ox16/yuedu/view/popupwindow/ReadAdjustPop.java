//Copyright (c) 2017. 章钦豪. All rights reserved.
package top.ox16.yuedu.view.popupwindow;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import top.ox16.yuedu.R;
import top.ox16.yuedu.help.ReadBookControl;
import top.ox16.yuedu.widget.check_box.SmoothCheckBox;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ReadAdjustPop extends FrameLayout {
    @BindView(R.id.hpb_light)
    SeekBar hpbLight;
    @BindView(R.id.scb_follow_sys)
    SmoothCheckBox scbFollowSys;
    @BindView(R.id.ll_follow_sys)
    LinearLayout llFollowSys;
    @BindView(R.id.ll_click)
    LinearLayout llClick;
    @BindView(R.id.hpb_click)
    SeekBar hpbClick;
    @BindView(R.id.hpb_tts_SpeechRate)
    SeekBar hpbTtsSpeechRate;
    @BindView(R.id.tv_tts_SpeechRate)
    TextView tvTtsSpeechRate;
    @BindView(R.id.hpb_tts_SpeechPitch)
    SeekBar hpbTtsSpeechPitch;
    @BindView(R.id.tv_tts_SpeechPitch)
    TextView tvTtsSpeechPitch;
    @BindView(R.id.tv_auto_page)
    TextView tvAutoPage;
    @BindView(R.id.rgSpeakerGroup)
    RadioGroup rgSpeakerGroup;

    private Activity context;
    private ReadBookControl readBookControl = ReadBookControl.getInstance();
    private Callback callback;

    public ReadAdjustPop(Context context) {
        super(context);
        init(context);
    }

    public ReadAdjustPop(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ReadAdjustPop(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.pop_read_adjust, this);
        ButterKnife.bind(this, view);
    }

    public void setListener(Activity activity, Callback callback) {
        this.context = activity;
        this.callback = callback;
        initData();
        bindEvent();
        initLight();
    }

    public void show() {
        initLight();
    }

    private void initData() {
        hpbClick.setMax(180);
        hpbClick.setProgress(readBookControl.getClickSensitivity());
        tvAutoPage.setText(String.format("%sS", readBookControl.getClickSensitivity()));
        hpbTtsSpeechRate.setProgress(readBookControl.getSpeechRate());
        tvTtsSpeechRate.setText(String.format("%s", readBookControl.getSpeechRate()));
        hpbTtsSpeechPitch.setProgress(readBookControl.getSpeechPitch());
        tvTtsSpeechPitch.setText(String.format("%s", readBookControl.getSpeechPitch()));
        ((RadioButton) rgSpeakerGroup.getChildAt(readBookControl.getSpeechSpeaker())).setChecked(true);
    }

    private void bindEvent() {
        //亮度调节
        llFollowSys.setOnClickListener(v -> {
            if (scbFollowSys.isChecked()) {
                scbFollowSys.setChecked(false, true);
            } else {
                scbFollowSys.setChecked(true, true);
            }
        });
        scbFollowSys.setOnCheckedChangeListener((checkBox, isChecked) -> {
            readBookControl.setLightFollowSys(isChecked);
            if (isChecked) {
                //跟随系统
                hpbLight.setEnabled(false);
                setScreenBrightness();
            } else {
                //不跟随系统
                hpbLight.setEnabled(true);
                setScreenBrightness(readBookControl.getLight());
            }
        });
        hpbLight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (!readBookControl.getLightFollowSys()) {
                    readBookControl.setLight(i);
                    setScreenBrightness(i);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //自动翻页间隔
        hpbClick.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                tvAutoPage.setText(String.format("%sS", i));
                readBookControl.setClickSensitivity(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //朗读语速调节
        hpbTtsSpeechRate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                tvTtsSpeechRate.setText(String.format("%s", i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                readBookControl.setSpeechRate(seekBar.getProgress());
                if (callback != null) {
                    callback.changeSpeechRate(readBookControl.getSpeechRate());
                }
            }
        });

        //朗读语速调节
        hpbTtsSpeechPitch.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                tvTtsSpeechPitch.setText(String.format("%s", i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                readBookControl.setSpeechPitch(seekBar.getProgress());
                if (callback != null) {
                    callback.changeSpeechPitch(readBookControl.getSpeechPitch());
                }
            }
        });

        //朗读音色选择
        rgSpeakerGroup.setOnCheckedChangeListener((radioGroup, i) -> {
            View checkView = radioGroup.findViewById(i);
            if (!checkView.isPressed()) {
                return;
            }
            int idx = radioGroup.indexOfChild(checkView);
            readBookControl.setSpeechSpeaker(idx);
            if (callback != null) {
                callback.changeSpeechSpeaker(readBookControl.getSpeechSpeaker());
            }
        });
    }

    public void setScreenBrightness() {
        WindowManager.LayoutParams params = (context).getWindow().getAttributes();
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        (context).getWindow().setAttributes(params);
    }

    public void setScreenBrightness(int value) {
        if (value < 1) value = 1;
        WindowManager.LayoutParams params = (context).getWindow().getAttributes();
        params.screenBrightness = value * 1.0f / 255f;
        (context).getWindow().setAttributes(params);
    }

    public void initLight() {
        hpbLight.setProgress(readBookControl.getLight());
        scbFollowSys.setChecked(readBookControl.getLightFollowSys());
        if (!readBookControl.getLightFollowSys()) {
            setScreenBrightness(readBookControl.getLight());
        }
    }

    public interface Callback {
        void changeSpeechRate(int speechRate);

        void changeSpeechPitch(int speechPitch);

        void changeSpeechSpeaker(int speechSpeaker);

        void speechRateFollowSys();
    }
}
