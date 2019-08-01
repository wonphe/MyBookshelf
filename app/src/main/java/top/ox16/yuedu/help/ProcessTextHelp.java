package top.ox16.yuedu.help;

import android.content.ComponentName;
import android.content.pm.PackageManager;

import top.ox16.yuedu.MApplication;

public class ProcessTextHelp {

    private static PackageManager packageManager = MApplication.getInstance().getPackageManager();
    private static ComponentName componentName = new ComponentName(MApplication.getInstance(), "top.ox16.yuedu.view.activity.ReceivingSharedActivity");

    public static boolean isProcessTextEnabled() {
        return packageManager.getComponentEnabledSetting(componentName) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
    }

    public static void setProcessTextEnable(boolean enable) {
        if (enable) {
            packageManager.setComponentEnabledSetting(componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        } else {
            packageManager.setComponentEnabledSetting(componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
    }

}
