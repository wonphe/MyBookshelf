package top.ox16.yuedu.presenter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import top.ox16.basemvplib.BasePresenterImpl;
import top.ox16.basemvplib.impl.IView;
import top.ox16.yuedu.DbHelper;
import top.ox16.yuedu.bean.BookSourceBean;
import top.ox16.yuedu.model.BookSourceManager;
import top.ox16.yuedu.presenter.contract.SourceEditContract;
import top.ox16.yuedu.utils.RxUtils;

import java.util.Objects;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;

/**
 * Created by GKF on 2018/1/28.
 * 编辑书源
 */
public class SourceEditPresenter extends BasePresenterImpl<SourceEditContract.View> implements SourceEditContract.Presenter {

    @Override
    public Observable<Boolean> saveSource(BookSourceBean bookSource, BookSourceBean bookSourceOld) {
        return Observable.create((ObservableOnSubscribe<Boolean>) e -> {
            if (!TextUtils.isEmpty(bookSourceOld.getBookSourceUrl()) && !Objects.equals(bookSource.getBookSourceUrl(), bookSourceOld.getBookSourceUrl())) {
                DbHelper.getDaoSession().getBookSourceBeanDao().delete(bookSourceOld);
            }
            BookSourceManager.addBookSource(bookSource);
            e.onNext(true);
        }).compose(RxUtils::toSimpleSingle);
    }

    @Override
    public void copySource(BookSourceBean bookSourceBean) {
        ClipboardManager clipboard = (ClipboardManager) mView.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText(null, mView.getBookSourceStr());
        if (clipboard != null) {
            clipboard.setPrimaryClip(clipData);
        }
    }

    @Override
    public void pasteSource() {
        ClipboardManager clipboard = (ClipboardManager) mView.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = clipboard != null ? clipboard.getPrimaryClip() : null;
        if (clipData != null && clipData.getItemCount() > 0) {
            setText(String.valueOf(clipData.getItemAt(0).getText()));
        }
    }

    @Override
    public void setText(String bookSourceStr) {
        try {
            Gson gson = new Gson();
            BookSourceBean bookSourceBean = gson.fromJson(bookSourceStr, BookSourceBean.class);
            mView.setText(bookSourceBean);
        } catch (Exception e) {
            mView.toast("数据格式不对");
        }
    }

    @Override
    public void attachView(@NonNull IView iView) {
        super.attachView(iView);
    }

    @Override
    public void detachView() {

    }
}
