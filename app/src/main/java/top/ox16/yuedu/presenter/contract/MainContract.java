package top.ox16.yuedu.presenter.contract;

import top.ox16.basemvplib.impl.IPresenter;
import top.ox16.basemvplib.impl.IView;

public interface MainContract {

    interface View extends IView {

        void initImmersionBar();

        /**
         * 取消弹出框
         */
        void dismissHUD();

        /**
         * 恢复数据
         */
        void onRestore(String msg);

        void recreate();

        void toast(String msg);

        void toast(int strId);

        int getGroup();
    }

    interface Presenter extends IPresenter {

        void backupData();

        void restoreData();

        void addBookUrl(String bookUrl);
    }

}
