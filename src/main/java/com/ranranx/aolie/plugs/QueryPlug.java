package com.ranranx.aolie.plugs;

import com.ranranx.aolie.core.annotation.Plug;
import com.ranranx.aolie.core.interfaces.IBaseDbService;
import com.ranranx.aolie.core.plugs.model.BasePlug;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * @author xxl
 * @version V0.0.1
 * @date 2021/7/6 0006 18:25
 **/
@Plug
public class QueryPlug extends BasePlug {
    private static final String PLUG_CODE = "AOLIE_CUSTOM_QUERY_PLUG";
    private static final String VERSION = "0.0.1";


    public QueryPlug(@Qualifier("CommonBaseService") IBaseDbService plugsService) {
        super((plugsService));
    }

    @Override
    protected String getPlugCode() {
        return PLUG_CODE;
    }

    @Override
    protected boolean doInstall() {
        insertPlugInfo();
        //创建相关表和数据
        return true;
    }

    @Override
    protected String getPlugName() {
        return "综合查询";
    }

    @Override
    protected boolean doUninstall() {
        //删除相关的表和数据
        return true;
    }

    @Override
    protected boolean doUpdate() {
        return true;
    }

    @Override
    protected String doRepair() {
        return null;
    }

    @Override
    public String getNewVersion() {
        return VERSION;
    }

    /**
     * 取得说明
     *
     * @return
     */
    @Override
    protected String getMemo() {
        return "提供分组分级查询表的制作和查询";
    }

    /**
     * 更新缓存中的插件信息
     */
    @Override
    public void updatePlug() {

    }

    @Override
    protected void stop() {

    }

    @Override
    protected void start() {
    }
}
