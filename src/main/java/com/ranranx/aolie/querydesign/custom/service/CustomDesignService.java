package com.ranranx.aolie.querydesign.custom.service;

import com.ranranx.aolie.core.handler.HandleResult;
import com.ranranx.aolie.querydesign.dto.QrTempletDetailDto;
import com.ranranx.aolie.querydesign.dto.QrTempletQuery;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 自定义查询设计服务
 */
public interface CustomDesignService {

    /**
     * 查询模板主数据和明细数据
     *
     * @param tempId
     * @param version
     * @return
     */
    HandleResult findTempletQuery(Long tempId, String version);


    /**
     * 删除自定义查询模板
     *
     * @param tempid
     * @return
     */
    HandleResult deleteTemplet(Long tempid);

    /**
     * 模板另存为
     *
     * @param templetQuery
     * @return
     */
    HandleResult saveAsTemplet(QrTempletQuery templetQuery);

    /**
     * 保存查询模板
     *
     * @param templetQuery
     * @return
     */
    HandleResult saveTemplet(QrTempletQuery templetQuery);

    /**
     * 取得自定义报表模板列表
     *
     * @return
     */
    HandleResult getCustomReportTemplets(String version);

    /**
     * 保存明细信息
     *
     * @param lstDetail
     * @param templetId
     * @param version
     * @return
     */

    HandleResult saveTempletDetail(List<QrTempletDetailDto> lstDetail, Long templetId,
                                   String version);


}
