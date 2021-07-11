package com.ranranx.aolie.query.modal.querycol;

import com.ranranx.aolie.core.common.CommonUtils;
import com.ranranx.aolie.core.datameta.datamodel.Column;
import com.ranranx.aolie.core.datameta.datamodel.SchemaHolder;
import com.ranranx.aolie.core.datameta.dto.ComponentDto;
import com.ranranx.aolie.core.tree.Node;
import com.ranranx.aolie.querydesign.QueryConstants;
import com.ranranx.aolie.querydesign.dto.QrTempletDetailDto;
import org.springframework.beans.BeanUtils;

import java.beans.Transient;
import java.util.ArrayList;
import java.util.List;

/**
 * @author xxl
 * @version V0.0.1
 * @date 2021/6/21 0021 22:14
 **/
public abstract class BaseQueryCol implements IQueryCol {
    /**
     * 默认的顶级查询字段根本的ID
     */
    protected static Integer ROOT_ID = 999999;
    /**
     * 第一级子节点
     */
    protected List<IQueryCol> subCol;
    /**
     * 引用到的列信息
     */
    protected Column refCol;

//    protected List<QueryField> getFields(List<QueryField> parentFields) {
//        Integer parentIndex = ROOT_ID;
//        if (parentFields != null && !parentFields.isEmpty()) {
//            parentIndex = parentFields.get(parentFields.size() - 1).getFieldIndex();
//        }
//        return getFields(parentIndex);
//    }

    /**
     * 原始设计信息
     */
    protected QrTempletDetailDto detailDto;
    /**
     * 本表头占用的序列号
     */
    protected List<Integer> fieldIndexes = new ArrayList<>();


    public BaseQueryCol(QrTempletDetailDto detailDto) {
        this.detailDto = detailDto;
        this.refCol = SchemaHolder.getColumn(detailDto.getColumnId(), detailDto.getVersionCode());
    }

    public BaseQueryCol() {

    }

    @Override
    public List<IQueryCol> getSubCol() {
        return subCol;
    }

    public void setSubCol(List<IQueryCol> subCol) {
        this.subCol = subCol;
    }


    @Override
    public Node getColumnHeader(int[] fieldIndex, String[] colLvlCode) {
        return null;
    }


    /**
     * 将Detail转换成显示控件,以显示表头
     *
     * @param detailDto
     * @param lvlCode
     * @param version
     * @return
     */
    protected QueryComponent toComponent(QrTempletDetailDto detailDto, String lvlCode, String version) {
        QueryComponent com = new QueryComponent();
        ComponentDto comDto = new ComponentDto();
        BeanUtils.copyProperties(detailDto, comDto);
        comDto.setLvlCode(lvlCode);
        com.setComponentDto(comDto);
        com.setColumn(CommonUtils.deepClone(SchemaHolder.getColumn(detailDto.getColumnId(), version)));
        return com;
    }

    @Override
    public String getDetailSelectSql(List<QueryField> parentQueryFields, String tableAlias, QueryField queryField) {
        return makeCaseFieldFromDetailTable(parentQueryFields, tableAlias, queryField, false);
    }

    /**
     * 具体分部特性的列，需要考虑上级的分布条件
     *
     * @param parentQueryFields
     * @return
     */
    private String makeCaseFieldFromDetailTable(List<QueryField> parentQueryFields, String tableAlias, QueryField curField, boolean needGroup) {
        //先收集上级的条件
        if (needGroup) {
            //如果是普通不分组的列，则不返回此列的查询
            if (detailDto.getGroupType() == null || detailDto.getGroupType().equals(QueryConstants.GroupType.NONE)) {
                return "";
            }
        }

        //如果存在父级引用条件，则加上条件
        if (parentQueryFields != null && !parentQueryFields.isEmpty()) {
            StringBuilder sbCase = new StringBuilder("case when ");
            for (QueryField field : parentQueryFields) {
                //如果上级没有指定ID,则不处理(但这种现在不应该出现)
                if (field.getLstLeafIds() == null || field.getLstLeafIds().isEmpty()) {
                    continue;
                }
                sbCase.append(tableAlias).append(".");
                sbCase.append(field.getOraField()).append(" in(")
                        .append(genInSql(field.getLstLeafIds())).append(") and ");
            }
            if (needGroup) {
                return QueryConstants.getGroupSql(curField.getGroupType())
                        + sbCase.substring(0, sbCase.length() - 4) + " then " +
                        tableAlias + "." + curField.getOraField() + " else "
                        + (curField.isNumberField() ? "0" : "null") + " end"
                        + ") as " +
                        genFieldName(curField.getOraField(), curField.getFieldIndex());
            } else {
                return sbCase.substring(0, sbCase.length() - 4) + " then " +
                        tableAlias + "." + curField.getOraField() + " else "
                        + (curField.isNumberField() ? "0" : "null") + " end" + " as " +
                        genFieldName(curField.getOraField(), curField.getFieldIndex());
            }
        } else {
            //如果没有上级条件，则直接返回自身查询列
            if (needGroup) {
                return QueryConstants.getGroupSql(curField.getGroupType())
                        + "." + curField.getOraField()
                        + ") as " + genFieldName(curField.getOraField(), curField.getFieldIndex());
            } else {
                return curField.getOraField()
                        + " as " + genFieldName(curField.getOraField(), curField.getFieldIndex());
            }

        }

    }


    private String genInSql(List<Object> lstIds) {
        StringBuilder sbSql = new StringBuilder();
        if (lstIds.get(0) instanceof Number) {
            lstIds.forEach(el -> sbSql.append(el).append(","));
        } else {
            lstIds.forEach(el -> sbSql.append("'").append(el).append("'").append(","));
        }
        return sbSql.substring(0, sbSql.length() - 1);
    }


    @Override
    public String getDetailGroupSql(List<QueryField> parentQueryFields, String tableAlias, QueryField field) {
        return makeCaseFieldFromDetailTable(parentQueryFields, tableAlias, field, true);
    }

    @Override
    public String getGroupSelectSql(String tableAlias, QueryField field) {
        String newField = genFieldName(field.getOraField(), field.getFieldIndex());
        if (field.isDetailField()) {
            return null;
        }
        return QueryConstants.getGroupSql(field.getGroupType()) + tableAlias + "." + newField + ") as " + newField;
    }

    @Override
    public String getGroupByFields(String tableAlias) {
        return null;
    }

    @Override
    public boolean hasDetailCol() {
        return false;
    }

    @Override
    public QrTempletDetailDto getDetailDto() {
        return detailDto;
    }

    public void setDetailDto(QrTempletDetailDto detailDto) {
        this.detailDto = detailDto;
        this.refCol = SchemaHolder.getColumn(detailDto.getColumnId(), detailDto.getVersionCode());
    }

    /**
     * 是否存在下级节点
     *
     * @return
     */
    protected boolean hasSubCol() {
        return subCol != null && !subCol.isEmpty();
    }

    /**
     * 通过节点的方式,初始化成树状结构
     * 本功能初始化自身,并调用孩子节点,进行嵌套
     *
     * @param node
     */
    public void initSubByNode(Node node) {
        if (node.getChildrenCount() <= 0) {
            return;
        }
        subCol = new ArrayList<>();
        for (int i = 0; i < node.getChildrenCount(); i++) {
            Node subNode = node.getChildAt(i);
            BaseQueryCol queryCol = QueryColHelper.createQueryCol((QrTempletDetailDto) subNode.getValue());
            subCol.add(queryCol);
            //递归调用下级生成节点
            queryCol.initSubByNode(subNode);
        }
    }

    /**
     * 生成虚拟列信息
     *
     * @param oraFieldName
     * @param fieldIndex
     * @return
     */
    protected String genFieldName(String oraFieldName, int fieldIndex) {
        return oraFieldName + "__" + fieldIndex;
    }

    @Override
    public String check() {
        return null;
    }

    /**
     * 是不是非扩展列
     *
     * @return
     */
    @Transient
    public boolean isNormalCol() {
        return isNormalCol(this.detailDto);
    }

    public boolean isNormalCol(QrTempletDetailDto dto) {
        return dto.getExtendType() == null || dto.equals(QueryConstants.ExtendType.NO_SEPARATE);
    }

    /**
     * 是不是横向扩展列
     *
     * @return
     */
    @Transient
    public boolean isHorizonSeparate() {
        return detailDto != null && detailDto.equals(QueryConstants.ExtendType.HORIZON_SEPARATE);
    }

    /**
     * 是不是纵向扩展列
     *
     * @return
     */
    @Transient
    public boolean isVerticalSeparate(QrTempletDetailDto dto) {
        return dto.getExtendType() != null && dto.getExtendType()
                .equals(QueryConstants.ExtendType.VERTICAL_SEPARATE);
    }

    /**
     * 是不是纵向扩展列
     *
     * @return
     */
    @Transient
    public boolean isVerticalSeparate() {
        return isVerticalSeparate(this.detailDto);
    }

    /**
     * 是不是分组字段
     *
     * @return
     */
    @Transient
    public boolean isGroupField(QrTempletDetailDto dto) {
        return dto.getGroupType() != null && !dto.getGroupType().equals(QueryConstants.GroupType.NONE);
    }

    public boolean isRollup(QrTempletDetailDto dto) {
        return dto.getRollUp() != null && dto.getRollUp() == 1;
    }

    /**
     * @return
     */
    @Override
    public VerticalSeparatorInfo getVerticalSeparateColInfo() {
        return null;
    }
}
