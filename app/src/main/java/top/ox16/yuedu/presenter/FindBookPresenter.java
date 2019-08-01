//Copyright (c) 2017. 章钦豪. All rights reserved.
package top.ox16.yuedu.presenter;

import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;

import top.ox16.basemvplib.BasePresenterImpl;
import top.ox16.basemvplib.impl.IView;
import top.ox16.yuedu.MApplication;
import top.ox16.yuedu.bean.BookSourceBean;
import top.ox16.yuedu.bean.FindKindBean;
import top.ox16.yuedu.bean.FindKindGroupBean;
import top.ox16.yuedu.model.BookSourceManager;
import top.ox16.yuedu.model.analyzeRule.AnalyzeRule;
import top.ox16.yuedu.presenter.contract.FindBookContract;
import top.ox16.yuedu.utils.ACache;
import top.ox16.yuedu.utils.RxUtils;
import top.ox16.yuedu.widget.recycler.expandable.bean.RecyclerViewData;

import java.util.ArrayList;
import java.util.List;

import javax.script.SimpleBindings;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.disposables.Disposable;

import static top.ox16.yuedu.constant.AppConstant.SCRIPT_ENGINE;

public class FindBookPresenter extends BasePresenterImpl<FindBookContract.View> implements FindBookContract.Presenter {
    private Disposable disposable;
    private AnalyzeRule analyzeRule;

    @SuppressWarnings("unchecked")
    @Override
    public void initData() {
        if (disposable != null) return;
        ACache aCache = ACache.get(mView.getContext(), "findCache");
        Single.create((SingleOnSubscribe<List<RecyclerViewData>>) e -> {
            List<RecyclerViewData> group = new ArrayList<>();
            boolean showAllFind = MApplication.getConfigPreferences().getBoolean("showAllFind", true);
            List<BookSourceBean> sourceBeans = new ArrayList<>(showAllFind ? BookSourceManager.getAllBookSourceBySerialNumber() : BookSourceManager.getSelectedBookSourceBySerialNumber());
            for (BookSourceBean sourceBean : sourceBeans) {
                try {
                    String[] kindA;
                    String findRule;
                    if (!TextUtils.isEmpty(sourceBean.getRuleFindUrl())) {
                        boolean isJsAndCache = sourceBean.getRuleFindUrl().startsWith("<js>");
                        if (isJsAndCache) {
                            findRule = aCache.getAsString(sourceBean.getBookSourceUrl());
                            if (TextUtils.isEmpty(findRule)) {
                                String jsStr = sourceBean.getRuleFindUrl().substring(4, sourceBean.getRuleFindUrl().lastIndexOf("<"));
                                findRule = evalJS(jsStr, sourceBean.getBookSourceUrl()).toString();
                            } else {
                                isJsAndCache = false;
                            }
                        } else {
                            findRule = sourceBean.getRuleFindUrl();
                        }
                        kindA = findRule.split("(&&|\n)+");
                        List<FindKindBean> children = new ArrayList<>();
                        for (String kindB : kindA) {
                            if (kindB.trim().isEmpty()) continue;
                            String[] kind = kindB.split("::");
                            FindKindBean findKindBean = new FindKindBean();
                            findKindBean.setGroup(sourceBean.getBookSourceName());
                            findKindBean.setTag(sourceBean.getBookSourceUrl());
                            findKindBean.setKindName(kind[0]);
                            findKindBean.setKindUrl(kind[1]);
                            children.add(findKindBean);
                        }
                        FindKindGroupBean groupBean = new FindKindGroupBean();
                        groupBean.setGroupName(sourceBean.getBookSourceName());
                        groupBean.setGroupTag(sourceBean.getBookSourceUrl());
                        group.add(new RecyclerViewData(groupBean, children, false));
                        if (isJsAndCache) {
                            aCache.put(sourceBean.getBookSourceUrl(), findRule);
                        }
                    }
                } catch (Exception exception) {
                    sourceBean.addGroup("发现规则语法错误");
                    BookSourceManager.addBookSource(sourceBean);
                }
            }
            e.onSuccess(group);
        })
                .compose(RxUtils::toSimpleSingle)
                .subscribe(new SingleObserver<List<RecyclerViewData>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable = d;
                    }

                    @Override
                    public void onSuccess(List<RecyclerViewData> recyclerViewData) {
                        mView.upData(recyclerViewData);
                        disposable.dispose();
                        disposable = null;
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(mView.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        disposable.dispose();
                        disposable = null;
                    }
                });
    }

    /**
     * 执行JS
     */
    private Object evalJS(String jsStr, String baseUrl) throws Exception {
        SimpleBindings bindings = new SimpleBindings();
        bindings.put("java", getAnalyzeRule());
        bindings.put("baseUrl", baseUrl);
        return SCRIPT_ENGINE.eval(jsStr, bindings);
    }

    private AnalyzeRule getAnalyzeRule() {
        if (analyzeRule == null) {
            analyzeRule = new AnalyzeRule(null);
        }
        return analyzeRule;
    }

    @Override
    public void attachView(@NonNull IView iView) {
        super.attachView(iView);
    }

    @Override
    public void detachView() {

    }

}