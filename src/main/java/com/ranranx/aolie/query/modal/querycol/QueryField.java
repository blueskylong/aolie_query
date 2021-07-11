package com.ranranx.aolie.query.modal.querycol;

import com.ranranx.aolie.core.datameta.datamodel.Column;
import com.ranranx.aolie.core.datameta.datamodel.DmConstants;
import com.ranranx.aolie.core.datameta.datamodel.SchemaHolder;
import com.ranranx.aolie.querydesign.QueryConstants;

import java.util.List;

/**
 * 记录生成的查询字段的状态
 *
 * @author xxl
 * @version V0.0.1
 * @date 2021/6/25 0025 14:00
 **/
public class QueryField extends SeparateNodeInfo {
    /**
     * 字段分配的序号
     */
    private Integer fieldIndex;

    private IQueryCol queryCol;

    public int getFieldIndex() {
        return fieldIndex;
    }

    private Column column;

    public void setFieldIndex(int fieldIndex) {
        this.fieldIndex = fieldIndex;
    }

    /**
     * 是不是
     *
     * @return
     */
    public boolean isDetailField() {
        return queryCol.getDetailDto().getGroupType() == null
                || queryCol.getDetailDto().getGroupType().equals(QueryConstants.GroupType.NONE);
    }

    public void setFieldIndex(Integer fieldIndex) {
        this.fieldIndex = fieldIndex;
    }

    public IQueryCol getQueryCol() {
        return queryCol;
    }

    public void setQueryCol(IQueryCol queryCol) {
        this.queryCol = queryCol;
    }

    public boolean isNumberField() {

        String fieldType = column.getColumnDto().getFieldType();
        return DmConstants.FieldType.DECIMAL.equals(fieldType)
                || DmConstants.FieldType.INT.equals(fieldType)
                || DmConstants.FieldType.BINARY.equals(fieldType);
    }

    /**
     * 生成一个末级字段
     *
     * @param oraField
     * @param leafIds
     * @param fieldIndex
     */
    public QueryField(String oraField, List<Object> leafIds,
                      Integer fieldIndex, IQueryCol queryCol) {
        this.oraField = oraField;
        this.lstLeafIds = leafIds;
        this.fieldIndex = fieldIndex;
        this.queryCol = queryCol;
        this.column = SchemaHolder.getColumn(queryCol.getDetailDto().getColumnId()
                , queryCol.getDetailDto().getVersionCode());


    }

    /**
     * 生成一非末级查询字段
     *
     * @param oraField
     * @param leafIds
     */
    public QueryField(String oraField, List<Object> leafIds, IQueryCol queryCol) {
        this(oraField, leafIds, -1, queryCol);
    }

    public Integer getGroupType() {
        return this.queryCol.getDetailDto().getGroupType();
    }
}
