package com.xolo.singletonnetwork;

import android.content.Context;
import android.view.View;
import rx.Subscriber;

public abstract class RxSubscriber<T> extends Subscriber<T> {

    private Context mContext;
    //popwin 定位
    private View view;
    //弹出 popwin 或者 Toast
    private boolean orPrompt = false;
    //Loading 提示
    private String message = "请求中";
    public RxSubscriber(Context context, View view, boolean orPrompt, String message) {
        this.mContext = context;
        this.view = view;
        this.orPrompt = orPrompt;
        this.message = message;
    }

    public RxSubscriber() {

    }


    @Override
    public void onCompleted() {
    }

    @Override
    public void onStart() {
        //开启转圈图标，表示正在进行网络请求
        /*if(orPrompt)
            new PopLoading(mContext, view, message, true);*/
        super.onStart();
    }


    @Override
    public void onNext(T t) {
        //停止转圈的图标
        //PopLoading.stopLoading();
        _onNext(t);
    }

    @Override
    public void onError(Throwable e) {
        //停止转圈的图标
        //PopLoading.stopLoading();
        String tips = e.toString();
        e.printStackTrace();
        //错误情况监测
        /*if (!NetCheckUtil.checkNet(V6AgentApplication.getV6Application())) {
            tips = "网络不可用,请检查你的网络";
        } else if (e.toString().contains("404")) {
            tips = "请求服务器404";
        } else if (e.toString().contains("500")) {
            tips = "请求服务器500";
        } else if (e.toString().contains("SocketTimeoutException")) {
            tips = "连接服务器超时";
        } else if (e.toString().contains("UnknownServiceException")) {
            tips = "高版本需要请求https";
        } else {
            tips = e.toString();
        }*/
        _onError(tips);
        //弹框提示错误或Toast提示错误
        /*if (orPrompt) {
            new PopWinOneTips(mContext, view, "*温馨提示*", tips, popupWindow -> popupWindow.dismiss());
        } else {
            Toast.makeText(V6AgentApplication.getV6Application(), tips, Toast.LENGTH_SHORT).show();
        }*/
    }

    protected abstract void _onNext(T t);

    protected abstract void _onError(String message);


}
