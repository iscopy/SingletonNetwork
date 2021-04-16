package com.xolo.singletonnetwork;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.xolo.singletonnetwork.model.BaseResponse;
import com.xolo.singletonnetwork.model.PostData;

public class MainActivity extends AppCompatActivity {

    private Button btnPosts;
    private Button btnGets;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnPosts = findViewById(R.id.btnPost);
        btnPosts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //进行模拟post请求
                postGo();
            }
        });

        btnGets = findViewById(R.id.btnGet);
        btnGets.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //进行模拟get请求
                getGo();
            }
        });
    }

    private void postGo() {
        Api.getApiService()
                .postApi(new PostData("参数1", "参数2"))
                .compose(RxSchedulers.<BaseResponse<String>>io_main())
                .subscribe(new RxSubscriber<BaseResponse>() {
                    @Override
                    protected void _onNext(BaseResponse baseResponse) {
                        Toast.makeText(MainActivity.this, baseResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    protected void _onError(String message) {
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void getGo() {
        Api.getApiService()
                .getApi(ApiConstants.GET_GO, "参数1", "参数2")
                .compose(RxSchedulers.<BaseResponse<String>>io_main())
                .subscribe(new RxSubscriber<BaseResponse>() {
                    @Override
                    protected void _onNext(BaseResponse baseResponse) {
                        Toast.makeText(MainActivity.this, baseResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    protected void _onError(String message) {
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}