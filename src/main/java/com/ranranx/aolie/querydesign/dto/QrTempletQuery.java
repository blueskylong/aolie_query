package com.ranranx.aolie.querydesign.dto;

import java.util.List;

/**
 * @author xxl
 * @version V0.0.1
 * @date 2021/6/16 0016 16:43
 **/
public class QrTempletQuery {
    private QrCustomTempletDto mainDto;
    private List<QrTempletDetailDto> lstDetail;

    public QrCustomTempletDto getMainDto() {
        return mainDto;
    }

    public void setMainDto(QrCustomTempletDto mainDto) {
        this.mainDto = mainDto;
    }

    public List<QrTempletDetailDto> getLstDetail() {
        return lstDetail;
    }

    public void setLstDetail(List<QrTempletDetailDto> lstDetail) {
        this.lstDetail = lstDetail;
    }
}
