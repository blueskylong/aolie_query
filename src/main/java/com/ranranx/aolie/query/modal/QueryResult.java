package com.ranranx.aolie.query.modal;

import com.ranranx.aolie.core.datameta.datamodel.BlockViewer;
import com.ranranx.aolie.core.handler.param.Page;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author xxl
 * @version V0.0.1
 * @date 2021/6/22 0022 14:48
 **/
public class QueryResult implements Serializable {
    /**
     * 表头信息
     */
    private BlockViewer viewer;
    /**
     * 表体信息
     */
    private List<Map<String, Object>> lstData;

    /**
     * 分面信息
     */
    private Page page;

    public BlockViewer getViewer() {
        return viewer;
    }

    public void setViewer(BlockViewer viewer) {
        this.viewer = viewer;
    }

    public List<Map<String, Object>> getLstData() {
        return lstData;
    }

    public void setLstData(List<Map<String, Object>> lstData) {
        this.lstData = lstData;
    }

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }
}
