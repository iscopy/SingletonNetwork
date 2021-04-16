package com.xolo.singletonnetwork;

import com.xolo.singletonnetwork.model.BaseResponse;
import com.xolo.singletonnetwork.model.PostData;

import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Url;
import rx.Observable;

public interface ApiService {

    /**
     * post 请求， BaseResponse 返回数据模型， PostData 请求参数模型
     *
     * @param postData
     * @return
     */
    @POST(ApiConstants.POST_GO)
    Observable<BaseResponse<String>> postApi(@Body PostData postData);

    /**
     * get 请求   BaseResponse 返回数据模型， Url 接口链接，
     *
     * @param url
     * @param getData1
     * @param getData2
     * @return
     */
    @GET
    Observable<BaseResponse<String>> getApi(@Url String url,
                                    @Query("getData1") String getData1,
                                    @Query("getData2") String getData2);
}
