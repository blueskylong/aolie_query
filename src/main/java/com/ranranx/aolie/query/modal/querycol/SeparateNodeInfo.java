package com.ranranx.aolie.query.modal.querycol;

import java.util.List;

/**
 * 扩展列的扩展信息
 *
 * @author xxl
 * @version V0.0.1
 * @date 2021/6/25 0025 14:17
 **/
public class SeparateNodeInfo {
    protected String oraField;
    protected List<Object> lstLeafIds;

    public String getOraField() {
        return oraField;
    }

    public void setOraField(String oraField) {
        this.oraField = oraField;
    }

    public List<Object> getLstLeafIds() {
        return lstLeafIds;
    }

    public void setLstLeafIds(List<Object> lstLeafIds) {
        this.lstLeafIds = lstLeafIds;
    }
}
