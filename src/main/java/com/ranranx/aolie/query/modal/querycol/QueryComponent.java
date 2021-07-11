package com.ranranx.aolie.query.modal.querycol;


import com.ranranx.aolie.core.datameta.datamodel.Component;

import java.util.List;

/**
 * @author xxl
 * @version V0.0.1
 * @date 2021/6/27 0027 16:08
 **/
public class QueryComponent extends Component {
    private QueryField queryField;

    public QueryField getQueryField() {
        return queryField;
    }

    public void setQueryField(QueryField queryField) {
        this.queryField = queryField;
    }
}
