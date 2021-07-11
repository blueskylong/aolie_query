package com.ranranx.aolie.querydesign.dto;

import javax.persistence.Table;

import com.ranranx.aolie.core.common.BaseDto;

/**
 * @author xxl
 * @version 1.0
 * @date 2021-06-16 16:15:28
 */
@Table(name = "aolie_qr_custom_templet")
public class QrCustomTempletDto extends BaseDto implements java.io.Serializable {

    private static final long serialVersionUID = 1L;
    private Long templetId;
    private String templetName;
    //暂时使用,如果改造成可以跨表查询,则此字段就这期了,其实,这里也可以使用明细表中的信息,来查询引用到的表信息
    private Long dsId;
    //这个不用了,使用明细中的列来决定显示明细的类型
    private Integer detailType;
    private String lvlCode;
    private Long filterViewId;

    public void setTempletId(Long templetId) {
        this.templetId = templetId;
    }

    public Long getTempletId() {
        return this.templetId;
    }

    public void setTempletName(String templetName) {
        this.templetName = templetName;
    }

    public String getTempletName() {
        return this.templetName;
    }

    public void setDsId(Long dsId) {
        this.dsId = dsId;
    }

    public Long getDsId() {
        return this.dsId;
    }

    public void setDetailType(Integer detailType) {
        this.detailType = detailType;
    }

    public Integer getDetailType() {
        return this.detailType;
    }

    public String getLvlCode() {
        return lvlCode;
    }

    public void setLvlCode(String lvlCode) {
        this.lvlCode = lvlCode;
    }

    public Long getFilterViewId() {
        return filterViewId;
    }

    public void setFilterViewId(Long filterViewId) {
        this.filterViewId = filterViewId;
    }
}