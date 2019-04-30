package com.handwerkcloud.client;

import com.owncloud.android.lib.resources.status.OCCapability;

public class HCCapability extends OCCapability {

    private String shopUrl = "";

    HCCapability() {
        super();
        shopUrl = "";
    }

    String getShopUrl() {
        return shopUrl;
    }

    void setShopUrl(String shopUrl) {
        this.shopUrl = shopUrl;
    }
}
