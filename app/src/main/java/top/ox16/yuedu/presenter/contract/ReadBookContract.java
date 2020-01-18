package top.ox16.yuedu.presenter.contract;

import android.app.Activity;
import android.content.Intent;

import top.ox16.basemvplib.impl.IPresenter;
import top.ox16.basemvplib.impl.IView;
import top.ox16.yuedu.bean.BookChapterBean;
import top.ox16.yuedu.bean.BookShelfBean;
import top.ox16.yuedu.bean.BookSourceBean;
import top.ox16.yuedu.bean.BookmarkBean;
import top.ox16.yuedu.bean.SearchBookBean;
import top.ox16.yuedu.presenter.ReadBookPresenter;
import top.ox16.yuedu.service.ReadAloudService;

import java.util.List;

public interface ReadBookContract {
    interface View extends IView {

        String getNoteUrl();

        Boolean getAdd();

        void setAdd(Boolean isAdd);

        void changeSourceFinish(BookShelfBean book);

        void startLoadingBook();

        void upMenu();

        void openBookFromOther();

        void showBookmark(BookmarkBean bookmarkBean);

        void skipToChapter(int chapterIndex, int pageIndex);

        void onMediaButton(String cmd);

        void upAloudState(ReadAloudService.Status state);

        void upAloudTimer(String timer);

        void readAloudStart(int start);

        void readAloudLength(int readAloudLength);

        void refresh(boolean recreate);

        void finish();

        void recreate();

        void upAudioSize(int audioSize);

        void upAudioDur(int audioDur);
    }

    interface Presenter extends IPresenter {

        void loadBook(Intent intent);

        BookShelfBean getBookShelf();

        List<BookChapterBean> getChapterList();

        BookChapterBean getDurChapter();

        void setChapterList(List<BookChapterBean> chapterList);

        void saveBook();

        void saveProgress();

        void addToShelf(final ReadBookPresenter.OnAddListener Listener);

        void removeFromShelf();

        void initData(Activity activity);

        void openBookFromOther(Activity activity);

        void addDownload(int start, int end);

        void changeBookSource(SearchBookBean searchBookBean);

        void autoChangeSource();

        void saveBookmark(BookmarkBean bookmarkBean);

        void delBookmark(BookmarkBean bookmarkBean);

        void disableDurBookSource();

        BookSourceBean getBookSource();
    }
}
