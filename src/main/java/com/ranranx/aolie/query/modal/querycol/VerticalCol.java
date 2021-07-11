package com.ranranx.aolie.query.modal.querycol;

import java.util.List;
import java.util.Map;

/**
 * 纵向分组中，分解的每一层次的信息类 对就于树结构的层次
 *
 * @author xxl
 * @version V0.0.1
 * @date 2021/6/30 0030 10:17
 **/
public class VerticalCol {
    /**
     * 用于设置编码列为空时的值，因为在生成 临时表时，把临时列设置成非空，所以不
     * 可以更新成空，就更新成这个值，以表示字段为空
     */
    public static final String EMPTY_VALUE = "x";
    public static final int LEAF_LEVEL = 99;
    private Integer index;
    //记录本列的编码，对子列一群编码的关系
    //如果是末级节点，则键是CODE，值是id
    //如果是不是，则是本级CODE对应下级CODE
    private Map<Object, List<Object>> mapToSubIds;
    private String fieldName;
    //编码的长度
    private int codeLength = 0;

    public VerticalCol(String oraFieldName, Map<Object, List<Object>> mapToSubIds,
                       int index) {
        this.mapToSubIds = mapToSubIds;
        this.fieldName = oraFieldName;
        this.index = index;
        this.codeLength = index * 3;
    }

    /**
     * 取得更新名称列的语句
     *
     * @param tableAlias
     * @return 生成语句形如： set field_code_1 =substring(field_code_99,0,3)
     */
    public String getUpdateSql(String tableAlias, String detailFieldName) {
        //末级不需要更新
        if (this.isLeafCol()) {
            return null;
        }
        String detailFullName = tableAlias + "." + detailFieldName;
        String nameField = this.getField();
        //这里x表示空值，临时字段设置的需要
        StringBuilder sbSql = new StringBuilder(nameField + "=( case when length(")
                .append(detailFullName).append(")>=").append(this.codeLength)
                .append(" then subStr(").append(detailFullName).append(",1,").append(codeLength)
                .append(") else '").append(EMPTY_VALUE).append("' end)");
        return sbSql.toString();
    }

    public int getIndex() {
        return this.index;
    }

    /**
     * 取得插入的第一层语句
     *
     * @param tableAlias
     * @param paramIndex
     * @param mapParam
     * @return
     */
    public String getInsertDetailSql(String tableAlias, int[] paramIndex, Map<String, Object> mapParam) {
        //如果是内嵌插入语句中的末级节点，则要交ＩＤ转换成编码
        if (this.isLeafCol()) {
            return genLeafInsertDetailSql(tableAlias, paramIndex, mapParam);
        }
        //如果是上级节点,在最先查询中不需要提供，在插入时，才需要提供
        else {
            return null;
        }
    }

    /**
     * 取得插入的第二层语句
     * 如果是普通列，在第一次插入时，没有值
     *
     * @return
     */
    public String getInsertSuperCodeGroupSql(String tableAlias) {
        if (this.isLeafCol()) {
            //第二次外包层查询，明细节点只查询字段
            return tableAlias + "." + getNewField();
        } else {
            //只返回空作为占位列
            return "'xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx' as " + getNewField();
        }
    }


    /**
     * 生成类似于　　　case  when ref1 in(2,3)then '001' when ref1 in(4,5) then '002' else null end as  ref1_code_99
     *
     * @param tableAlias
     * @param mapParam
     * @return
     */
    public String genLeafInsertDetailSql(String tableAlias, int[] paramIndex, Map<String, Object> mapParam) {
        StringBuilder sbSql = new StringBuilder();
        sbSql.append("case ");
        this.mapToSubIds.forEach((key, lstId) -> {
            sbSql.append(" when ").append(tableAlias)
                    .append(".").append(fieldName).append(" in(").append(genParams(lstId, paramIndex, mapParam))
                    .append(") then '").append(key).append("' ");
        });
        sbSql.append(" else null end as ").append(getNewField());
        return sbSql.toString();
    }

    public String getNewField() {
        return fieldName + "_code_" + index;
    }


    public String genParams(List<Object> values, int[] paramIndex, Map<String, Object> paramOut) {
        StringBuilder sb = new StringBuilder();
        String paramName;
        for (Object value : values) {
            paramName = "P" + paramIndex[0];
            paramIndex[0]++;
            sb.append("#{").append(paramName).append("},");
            paramOut.put(paramName, value);
        }
        return sb.substring(0, sb.length() - 1);
    }

    public boolean isLeafCol() {
        return this.index == LEAF_LEVEL;
    }

    /**
     * 取得当前编码和名称列
     *
     * @return
     */
    public String getField() {
        return fieldName + "_code_" + index;
    }
}

