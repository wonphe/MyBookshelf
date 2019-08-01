package top.ox16.yuedu.model.content;

import top.ox16.yuedu.MApplication;
import top.ox16.yuedu.R;

public class VipThrowable extends Throwable {

    private final static String tag = "VIP_THROWABLE";

    VipThrowable() {
        super(MApplication.getInstance().getString(R.string.donate_s));
    }
}
