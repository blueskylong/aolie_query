package com.ranranx.aolie.querydesign.custom.controller;

import com.ranranx.aolie.core.common.SessionUtils;
import com.ranranx.aolie.core.handler.HandleResult;
import com.ranranx.aolie.querydesign.custom.service.CustomDesignService;
import com.ranranx.aolie.querydesign.dto.QrTempletDetailDto;
import com.ranranx.aolie.querydesign.dto.QrTempletQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author xxl
 * @version V0.0.1
 * @date 2021/6/16 0016 23:00
 **/
@RestController
@RequestMapping("/qr")
public class CustomDesignController {

    @Autowired
    private CustomDesignService designService;

    /**
     * 查询模板主数据和明细数据
     *
     * @param tempId
     * @return
     */
    @GetMapping("/findTempletQuery/{tempId}")
    public HandleResult findTempletQuery(@PathVariable Long tempId) {
        return designService.findTempletQuery(tempId, SessionUtils.getLoginVersion());
    }


    /**
     * 删除自定义查询模板
     *
     * @param tempId
     * @return
     */
    @PutMapping("/deleteTemplet/{tempId}")
    public HandleResult deleteTemplet(@PathVariable Long tempId) {
        return designService.deleteTemplet(tempId);
    }

    /**
     * 模板另存为
     *
     * @param templetQuery
     * @return
     */
    @PostMapping("/saveAsTemplet")
    public HandleResult saveAsTemplet(@RequestBody QrTempletQuery templetQuery) {
        return designService.saveAsTemplet(templetQuery);
    }

    /**
     * 模板另存为
     *
     * @param lstDetail
     * @return
     */
    @PostMapping("/saveTempletDetail/{templetId}")
    public HandleResult saveAsTempletDetail(@RequestBody List<QrTempletDetailDto> lstDetail,
                                            @PathVariable Long templetId) {
        return designService.saveTempletDetail(lstDetail, templetId, SessionUtils.getLoginVersion());
    }

    /**
     * 保存查询模板
     *
     * @param templetQuery
     * @return
     */
    @PostMapping("/saveTemplet")
    public HandleResult saveTemplet(@RequestBody QrTempletQuery templetQuery) {
        return designService.saveTemplet(templetQuery);
    }

    /**
     * 取得自定义报表模板列表
     *
     * @return
     */
    @GetMapping("/getCustomReportTemplets")
    public HandleResult getCustomReportTemplets() {
        return designService.getCustomReportTemplets(SessionUtils.getLoginVersion());
    }
}
