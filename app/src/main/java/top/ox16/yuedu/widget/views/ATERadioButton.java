package top.ox16.yuedu.widget.views;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatRadioButton;

import top.ox16.yuedu.utils.theme.ATH;
import top.ox16.yuedu.utils.theme.ThemeStore;

/**
 * @author Aidan Follestad (afollestad)
 */
public class ATERadioButton extends AppCompatRadioButton {

    public ATERadioButton(Context context) {
        super(context);
        init(context, null);
    }

    public ATERadioButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ATERadioButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        ATH.setTint(this, ThemeStore.accentColor(context));
    }
}
