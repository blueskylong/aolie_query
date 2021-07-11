package com.ranranx.aolie.querydesign;

import java.util.Map;

/**
 * @author xxl
 * @version V0.0.1
 * @date 2021/6/16 0016 19:39
 **/
public class QueryConstants {
    /**
     * 默认查询的模型ID
     */
    public static final Long QUERY_DEFAULT_SCHEMA_ID = 5L;

    public static class ExtendType {
        //无扩展
        public static final int NO_SEPARATE = 0;
        //横向扩展
        public static final int HORIZON_SEPARATE = 1;
        //纵向扩展
        public static final int VERTICAL_SEPARATE = 2;
    }

    /**
     * 分组类型
     */
    public static class GroupType {
        public static final int NONE = 0;
        public static final int SUM = 1;
        public static final int AVG = 2;
        public static final int COUNT = 3;
        public static final int MAX = 4;
        public static final int MIN = 5;

    }

    public static String getGroupSql(int type) {
        switch (type) {
            case GroupType.NONE:
                return null;
            case GroupType.SUM:
                return "sum(";
            case GroupType.AVG:
                return "avg(";
            case GroupType.COUNT:
                return "count(";
            case GroupType.MAX:
                return "max(";
            case GroupType.MIN:
                return "min(";
            default:
                return null;
        }

    }
}
