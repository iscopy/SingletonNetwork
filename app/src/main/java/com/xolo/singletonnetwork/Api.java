package com.xolo.singletonnetwork;

import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class Api {

    //读超时长，单位：毫秒
    private static final int READ_TIME_OUT = 30 * 1000;
    //连接时长，单位：毫秒
    private static final int CONNECT_TIME_OUT = 30 * 1000;
    private static final String TAG = "Api";
    //Retrofit 仅负责 网络请求接口的封装
    private Retrofit retrofit;
    private static ApiService apiService;
    private static String mHost;

    private static Api api;

    public static void setApiService(ApiService apiService) {
        Api.apiService = apiService;
    }

    public static void setApi(Api api) {
        Api.api = api;
    }

    /**
     * 单例模式
     */
    private static Api getApiInstance() {
        if (api == null) {
            synchronized (Api.class) {
                if (api == null) {
                    api = new Api();
                }
            }
        }
        return api;
    }

    //构造方法私有
    private Api() {
        //开启Log打印网络请求信息-自定义的
        HttpLogging httpLoggingInterceptor = new HttpLogging(new HttpLogging.Logger() {
            @Override
            public void log(String message) {
                if (TextUtils.isEmpty(message)) return;
                Log.i(TAG, message);
            }
        });
        httpLoggingInterceptor.setLevel(HttpLogging.Level.BODY);
        //缓存
        File cacheFile = new File(MyApplication.getMyApplication().getCacheDir(), "cache");
        Cache cache = new Cache(cacheFile, 1024 * 1024 * 100); //100Mb
        //增加头部信息
        Interceptor headerInterceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request build = chain.request().newBuilder()
                        .addHeader("Content-Type", "application/json")
                        .build();
                return chain.proceed(build);
            }
        };

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(READ_TIME_OUT, TimeUnit.MILLISECONDS)
                .connectTimeout(CONNECT_TIME_OUT, TimeUnit.MILLISECONDS)
                .addInterceptor(headerInterceptor)
                .addInterceptor(httpLoggingInterceptor)
                .addInterceptor(commoninterceptor)
                .cache(cache)
                .build();

        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").serializeNulls().create();
        //获取baseUrl
        mHost = ApiConstants.getHost();
        retrofit = new Retrofit.Builder()
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .baseUrl(mHost)
                .build();
        apiService = retrofit.create(ApiService.class);
    }

    public static ApiService getApiService() {
        if (apiService == null) {
            synchronized (ApiService.class) {
                if (apiService == null) {
                    Api.getApiInstance();
                }
            }
        }
        return apiService;
    }

    private final Interceptor commoninterceptor = new Interceptor() {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request oldRequest = chain.request();
            // 添加新的参数
            HttpUrl.Builder authorizedUrlBuilder = oldRequest.url()
                    .newBuilder()
                    .scheme(oldRequest.url().scheme())
                    .host(oldRequest.url().host());
            // 新的请求
            Request newRequest = oldRequest.newBuilder()
                    .url(authorizedUrlBuilder.build())
                    .build();
            return chain.proceed(newRequest);
        }
    };

}
