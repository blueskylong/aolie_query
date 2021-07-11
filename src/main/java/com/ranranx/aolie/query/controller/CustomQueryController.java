package com.ranranx.aolie.query.controller;

import com.ranranx.aolie.core.common.SessionUtils;
import com.ranranx.aolie.core.handler.HandleResult;
import com.ranranx.aolie.query.service.TempletQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author xxl
 * @version V0.0.1
 * @date 2021/6/21 0021 17:13
 **/
@RestController
@RequestMapping("/qr")
public class CustomQueryController {

    @Autowired
    private TempletQueryService queryService;

    /**
     * 按自定义模板查询
     *
     * @param tempId 模板ID
     * @param filter 过滤条件
     * @return
     */
    @PostMapping("/findCustomQueryResult/{tempId}")
    public HandleResult findCustomQueryResult(@PathVariable Long tempId, @RequestBody Map<String, Object> filter) {
        return queryService.findCustomQueryResult(tempId, SessionUtils.getLoginVersion(), filter);
    }
}
