package top.ox16.yuedu.presenter.contract;

import top.ox16.basemvplib.impl.IPresenter;
import top.ox16.basemvplib.impl.IView;
import top.ox16.yuedu.bean.BookSourceBean;

import io.reactivex.Observable;

public interface SourceEditContract {
    interface Presenter extends IPresenter {

        Observable<Boolean> saveSource(BookSourceBean bookSource, BookSourceBean bookSourceOld);

        void copySource(String bookSource);

        void pasteSource();

        void setText(String bookSourceStr);
    }

    interface View extends IView {

        void setText(BookSourceBean bookSourceBean);

        String getBookSourceStr(boolean hasFind);
    }
}
