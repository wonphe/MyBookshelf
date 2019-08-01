package top.ox16.yuedu.view.fragment;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.hwangjr.rxbus.RxBus;
import top.ox16.basemvplib.impl.IPresenter;
import top.ox16.yuedu.DbHelper;
import top.ox16.yuedu.R;
import top.ox16.yuedu.base.MBaseFragment;
import top.ox16.yuedu.base.observer.MySingleObserver;
import top.ox16.yuedu.bean.BookShelfBean;
import top.ox16.yuedu.bean.BookmarkBean;
import top.ox16.yuedu.bean.OpenChapterBean;
import top.ox16.yuedu.constant.RxBusTag;
import top.ox16.yuedu.help.BookshelfHelp;
import top.ox16.yuedu.utils.RxUtils;
import top.ox16.yuedu.view.activity.ChapterListActivity;
import top.ox16.yuedu.view.adapter.BookmarkAdapter;
import top.ox16.yuedu.widget.modialog.BookmarkDialog;
import top.ox16.yuedu.widget.recycler.scroller.FastScrollRecyclerView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;

public class BookmarkFragment extends MBaseFragment {
    @BindView(R.id.rv_list)
    FastScrollRecyclerView rvList;

    private Unbinder unbinder;
    private BookShelfBean bookShelf;
    private List<BookmarkBean> bookmarkBeanList;
    private BookmarkAdapter adapter;

    @Override
    public int createLayoutId() {
        return R.layout.fragment_bookmark_list;
    }

    /**
     * P层绑定   若无则返回null;
     */
    @Override
    protected IPresenter initInjector() {
        return null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RxBus.get().register(this);
    }

    /**
     * 数据初始化
     */
    @Override
    protected void initData() {
        super.initData();
        if (getFatherActivity() != null) {
            bookShelf = getFatherActivity().getBookShelf();
        }
    }

    /**
     * 控件绑定
     */
    @Override
    protected void bindView() {
        super.bindView();
        unbinder = ButterKnife.bind(this, view);
        adapter = new BookmarkAdapter(bookShelf, new BookmarkAdapter.OnItemClickListener() {
            @Override
            public void itemClick(int index, int page) {
                if (index != bookShelf.getDurChapter()) {
                    RxBus.get().post(RxBusTag.SKIP_TO_CHAPTER, new OpenChapterBean(index, page));
                }
                if (getFatherActivity() != null) {
                    getFatherActivity().searchViewCollapsed();
                    getFatherActivity().finish();
                }
            }

            @Override
            public void itemLongClick(BookmarkBean bookmarkBean) {
                if (getFatherActivity() != null) {
                    getFatherActivity().searchViewCollapsed();
                }
                showBookmark(bookmarkBean);
            }
        });
        rvList.setLayoutManager(new LinearLayoutManager(getActivity()));
        rvList.setAdapter(adapter);
    }

    @Override
    protected void firstRequest() {
        super.firstRequest();
        Single.create((SingleOnSubscribe<Boolean>) emitter -> {
            if (bookShelf != null) {
                bookmarkBeanList = BookshelfHelp.getBookmarkList(bookShelf.getBookInfoBean().getName());
                emitter.onSuccess(true);
            } else {
                emitter.onSuccess(false);
            }
        }).compose(RxUtils::toSimpleSingle)
                .subscribe(new MySingleObserver<Boolean>() {
                    @Override
                    public void onSuccess(Boolean aBoolean) {
                        if (aBoolean) {
                            adapter.setAllBookmark(bookmarkBeanList);
                        }
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
        RxBus.get().unregister(this);
    }

    public void startSearch(String key) {
        adapter.search(key);
    }

    private void showBookmark(BookmarkBean bookmarkBean) {
        BookmarkDialog.builder(getContext(), bookmarkBean, false)
                .setPositiveButton(new BookmarkDialog.Callback() {
                    @Override
                    public void saveBookmark(BookmarkBean bookmarkBean) {
                        DbHelper.getDaoSession().getBookmarkBeanDao().insertOrReplace(bookmarkBean);
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void delBookmark(BookmarkBean bookmarkBean) {
                        DbHelper.getDaoSession().getBookmarkBeanDao().delete(bookmarkBean);
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void openChapter(int chapterIndex, int pageIndex) {
                        RxBus.get().post(RxBusTag.OPEN_BOOK_MARK, bookmarkBean);
                        if (getFatherActivity() != null) {
                            getFatherActivity().finish();
                        }
                    }
                }).show();
    }

    private ChapterListActivity getFatherActivity() {
        return (ChapterListActivity) getActivity();
    }

}
