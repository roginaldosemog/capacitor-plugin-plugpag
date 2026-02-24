package com.galago.plugins.plugpag;

import com.getcapacitor.Logger;

public class PlugPagPlugin {

    public String echo(String value) {
        Logger.info("Echo", value);
        return value;
    }
}
