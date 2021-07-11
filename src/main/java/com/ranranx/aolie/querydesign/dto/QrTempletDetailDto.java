package com.ranranx.aolie.querydesign.dto;

import javax.persistence.Table;

import com.ranranx.aolie.core.common.BaseDto;
import com.ranranx.aolie.querydesign.QueryConstants;

import java.beans.Transient;

/**
 * @author xxl
 * @version 1.0
 * @date 2021-06-16 16:16:00
 */
@Table(name = "aolie_qr_templet_detail")
public class QrTempletDetailDto extends BaseDto implements java.io.Serializable {

    private static final long serialVersionUID = 1L;
    private Long templetDetailId;
    private Long templetId;
    private Long columnId;
    private Integer extendType;
    private Integer xh;
    private Long refId;
    private Integer levelShow;
    private String selectIds;
    private String lvlCode;
    private String title;
    private Integer orderType;
    private String dispType;
    private Integer width;
    private String format;
    private Integer groupType;
    private Short rollUp;


    public void setTempletDetailId(Long templetDetailId) {
        this.templetDetailId = templetDetailId;
    }

    public Long getTempletDetailId() {
        return this.templetDetailId;
    }

    public void setTempletId(Long templetId) {
        this.templetId = templetId;
    }

    public Long getTempletId() {
        return this.templetId;
    }

    public void setColumnId(Long columnId) {
        this.columnId = columnId;
    }

    public Long getColumnId() {
        return this.columnId;
    }

    public void setExtendType(Integer extendType) {
        this.extendType = extendType;
    }

    public Integer getExtendType() {
        return this.extendType;
    }

    public void setXh(Integer xh) {
        this.xh = xh;
    }

    public Integer getXh() {
        return this.xh;
    }

    public void setRefId(Long refId) {
        this.refId = refId;
    }

    public Long getRefId() {
        return this.refId;
    }

    public void setLevelShow(Integer levelShow) {
        this.levelShow = levelShow;
    }

    public Integer getLevelShow() {
        return this.levelShow;
    }

    public void setSelectIds(String selectIds) {
        this.selectIds = selectIds;
    }

    public String getSelectIds() {
        return this.selectIds;
    }

    public void setLvlCode(String lvlCode) {
        this.lvlCode = lvlCode;
    }

    public String getLvlCode() {
        return this.lvlCode;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return this.title;
    }

    public void setOrderType(Integer orderType) {
        this.orderType = orderType;
    }

    public Integer getOrderType() {
        return this.orderType;
    }

    public String getDispType() {
        return dispType;
    }

    public void setDispType(String dispType) {
        this.dispType = dispType;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Integer getGroupType() {
        return groupType;
    }

    public void setGroupType(Integer groupType) {
        this.groupType = groupType;
    }

    public Short getRollUp() {
        return rollUp;
    }

    public void setRollUp(Short rollUp) {
        this.rollUp = rollUp;
    }


}