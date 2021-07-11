package com.ranranx.aolie.query.service.impl;

import com.ranranx.aolie.core.handler.HandleResult;
import com.ranranx.aolie.core.handler.HandlerFactory;
import com.ranranx.aolie.query.modal.QueryResult;
import com.ranranx.aolie.query.modal.TempletQueryEngine;
import com.ranranx.aolie.query.service.TempletQueryService;
import com.ranranx.aolie.querydesign.custom.service.CustomDesignService;
import com.ranranx.aolie.querydesign.dto.QrTempletQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * @author xxl
 * @version V0.0.1
 * @date 2021/6/21 0021 17:07
 **/
@Service
@Transactional(readOnly = true)
public class TempletQueryServiceImpl implements TempletQueryService {

    @Autowired
    private CustomDesignService designService;

    @Autowired
    private HandlerFactory handlerFactory;

    @Override
    @Transactional(readOnly = false)
    public HandleResult findCustomQueryResult(Long tempId, String version, Map<String, Object> filter) {
        HandleResult result = designService.findTempletQuery(tempId, version);
        QrTempletQuery templetQuery = (QrTempletQuery) result.getData();
        TempletQueryEngine engine = new TempletQueryEngine(templetQuery, handlerFactory);

        HandleResult success = HandleResult.success(1);
        QueryResult queryResult = engine.doQuery(filter);
        success.setData(queryResult);
        return success;
    }


}
