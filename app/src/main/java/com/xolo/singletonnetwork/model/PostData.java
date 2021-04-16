package com.xolo.singletonnetwork.model;

/**
 * Created by isCopy
 * on 2021/4/2
 */
public class PostData {
    private String post1;
    private String post2;

    public PostData() {
    }

    public PostData(String post1, String post2) {
        this.post1 = post1;
        this.post2 = post2;
    }

    public String getPost1() {
        return post1;
    }

    public void setPost1(String post1) {
        this.post1 = post1;
    }

    public String getPost2() {
        return post2;
    }

    public void setPost2(String post2) {
        this.post2 = post2;
    }
}
