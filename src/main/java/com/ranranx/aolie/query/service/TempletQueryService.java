package com.ranranx.aolie.query.service;

import com.ranranx.aolie.core.handler.HandleResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * 自定义综合查询的查询服务
 */
public interface TempletQueryService {

    /**
     * 按自定义模板查询
     *
     * @param tempId
     * @param filter
     * @return
     */
    HandleResult findCustomQueryResult(Long tempId, String version, Map<String, Object> filter);
}
