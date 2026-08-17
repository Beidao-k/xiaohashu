package com.quanxiaoha.xiaohashu.distributed.generator.biz.core.common;


import com.quanxiaoha.xiaohashu.distributed.generator.biz.core.IDGen;

public class ZeroIDGen implements IDGen {
    @Override
    public Result get(String key) {
        return new Result(0, Status.SUCCESS);
    }

    @Override
    public boolean init() {
        return true;
    }
}
