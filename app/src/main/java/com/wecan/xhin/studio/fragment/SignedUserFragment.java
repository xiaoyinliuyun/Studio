package com.wecan.xhin.studio.fragment;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.View;

import com.jakewharton.rxbinding.view.ViewClickEvent;
import com.wecan.xhin.studio.R;
import com.wecan.xhin.studio.api.Api;
import com.wecan.xhin.studio.bean.common.User;
import com.wecan.xhin.studio.bean.down.BaseData;
import com.wecan.xhin.studio.bean.down.UsersData;
import com.wecan.xhin.studio.bean.up.SignBody;
import com.wecan.xhin.studio.rx.RxNetworking;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by xhinliang on 15-11-20.
 * xhinliang@gmail.com
 */
public class SignedUserFragment extends UsersFragment {

    public static final String KEY_USER = "user";

    private User user;
    private Observable<BaseData> observableSign;
    private Observable.Transformer<BaseData, BaseData> networkingIndicator;

    public static SignedUserFragment newInstance(User user) {
        SignedUserFragment fragment = new SignedUserFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_USER, user);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ProgressDialog pd = new ProgressDialog(getActivity());
        networkingIndicator = RxNetworking.bindConnecting(pd);
        setEvent((FloatingActionButton) view.findViewById(R.id.fab_sign));
    }


    protected void setEvent(final FloatingActionButton fab) {
        fab.setVisibility(View.VISIBLE);
        user = getArguments().getParcelable(KEY_USER);
        if (user == null)
            return;
        fab.setImageResource(user.status == User.VALUE_STATUS_SIGN ? R.drawable.ic_logout : R.drawable.ic_login);
        observableSign = Observable
                .defer(new Func0<Observable<BaseData>>() {
                    @Override
                    public Observable<BaseData> call() {
                        if (user.status == User.VALUE_STATUS_SIGN)
                            return api.unSign(user.name);
                        return api.sign(new SignBody(user.name));
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(networkingIndicator);

        setRxView(fab)
                .flatMap(new Func1<ViewClickEvent, Observable<BaseData>>() {
                    @Override
                    public Observable<BaseData> call(ViewClickEvent viewClickEvent) {
                        return observableSign;
                    }
                })
                .retry()
                .flatMap(new Func1<BaseData, Observable<UsersData>>() {
                    @Override
                    public Observable<UsersData> call(BaseData baseData) {
                        user.status = user.status == User.VALUE_STATUS_UNSIGN ?
                                User.VALUE_STATUS_SIGN : User.VALUE_STATUS_UNSIGN;
                        fab.setImageResource(user.status == User.VALUE_STATUS_SIGN
                                ? R.drawable.ic_logout : R.drawable.ic_login);
                        return observableConnect;
                    }
                })
                .subscribe(new Action1<UsersData>() {
                    @Override
                    public void call(UsersData baseData) {
                        users.clear();
                        users.addAll(baseData.data);
                    }
                }, errorAction);
    }

    @Override
    protected Observable<UsersData> getUserApi(Api api) {
        return api.getSignedUser();
    }
}