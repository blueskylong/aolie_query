package com.ranranx.aolie.query.modal;

import com.ranranx.aolie.core.common.CommonUtils;
import com.ranranx.aolie.core.common.IdGenerator;
import com.ranranx.aolie.core.datameta.datamodel.*;
import com.ranranx.aolie.core.datameta.dto.BlockViewDto;
import com.ranranx.aolie.core.ds.definition.SqlExp;
import com.ranranx.aolie.core.exceptions.IllegalOperatorException;
import com.ranranx.aolie.core.exceptions.InvalidConfigException;
import com.ranranx.aolie.core.handler.HandleResult;
import com.ranranx.aolie.core.handler.HandlerFactory;
import com.ranranx.aolie.core.handler.param.QueryParam;
import com.ranranx.aolie.core.handler.param.UpdateParam;
import com.ranranx.aolie.core.tree.Node;
import com.ranranx.aolie.core.tree.SysCodeRule;
import com.ranranx.aolie.core.tree.TreeFactory;
import com.ranranx.aolie.core.tree.TreeNodeHelper;
import com.ranranx.aolie.query.modal.querycol.*;
import com.ranranx.aolie.querydesign.QueryConstants;
import com.ranranx.aolie.querydesign.dto.QrTempletDetailDto;
import com.ranranx.aolie.querydesign.dto.QrTempletQuery;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 自定义查询的模板查询引擎
 * 执行步骤：
 * 一、有纵向分组情况
 * 1. 根据最明细分组将明细数据生成到临时表中
 * 第一次的插入，按照最末级的节点组合，做二次外包查询，即里层SQL CASE分列，外层分组合并
 * 2. 根据纵向分组从后向前，分次查询各级数据。
 * 3. 如果有明细数据，则在原始表中查询明细数据
 * 4. 合并数据并排序并拼写名称列
 * 二、无纵向分组情况（只体现在查询明细数据）
 * 仅在原始数据表中查询即可
 *
 * @author xxl
 * @version V0.0.1
 * @date 2021/6/21 0021 17:39
 **/
public class TempletQueryEngine {


    private static AtomicInteger TEMP_TABLE_SER = new AtomicInteger(0);
    private HandlerFactory handlerFactory;
    /**
     * 查询定义信息
     */
    private QrTempletQuery queryMain;
    /**
     * 生成的查询分析列
     */
    private List<IQueryCol> lstCols;
    /**
     * 查询表信息
     */
    private TableInfo table;
    //纵向扩展列详细信息
    private List<VerticalSeparatorInfo> lstVerSpInfo;
    /**
     * 是否是设置完备的表
     */
    private boolean isValidSet = true;

    public TempletQueryEngine(QrTempletQuery queryMain, HandlerFactory handlerFactory) {
        this.queryMain = queryMain;
        try {
            initCol();
            if (queryMain.getLstDetail() == null || queryMain.getLstDetail().isEmpty()) {
                isValidSet = false;
                return;
            }
            this.table = getQueryTable();
            this.setHandlerFactory(handlerFactory);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalOperatorException("执行异常:" + e.getMessage());
        }
    }

    public void setHandlerFactory(HandlerFactory handlerFactory) {
        this.handlerFactory = handlerFactory;
    }

    /**
     * 查询
     *
     * @return
     */
    public QueryResult doQuery(Map<String, Object> filter) {
        if (!isValidSet) {
            return null;
        }
        Node queryStruct = initQueryStruct();
        BlockViewer blockViewer = getHeader(queryStruct);
        QueryResult result = new QueryResult();
        result.setViewer(blockViewer);
        result.setLstData(queryData(queryStruct, filter));

        return result;
    }

    private String genFilter(Map<String, Object> filter, String tableAlias, int[] paramIndex,
                             Map<String, Object> mapParam) {
        if (filter == null || filter.isEmpty()) {
            return null;
        }
        StringBuilder sbSql = new StringBuilder();
        filter.forEach((key, value) -> {
            if (CommonUtils.isEmpty(value)) {
                return;
            }
            int index = paramIndex[0]++;
            sbSql.append(tableAlias).append(".").append(key).append(" like concat('%',")
                    .append("#{P").append(index).append("},'%') and ");
            mapParam.put("P" + index, value);
        });
        return sbSql.substring(0, sbSql.length() - 4);
    }

    /**
     * 执行查询
     *
     * @param queryStruct 查询表头结构
     * @return
     */
    private List<Map<String, Object>> queryData(Node queryStruct, Map<String, Object> mapFilter) {
        boolean hasDetailCol = false;
        //收集纵向扩展的信息
        lstVerSpInfo = new ArrayList<>();
        for (IQueryCol col : lstCols) {
            VerticalSeparatorInfo verticalSeparateColInfo = col.getVerticalSeparateColInfo();
            if (verticalSeparateColInfo != null) {
                lstVerSpInfo.add(verticalSeparateColInfo);
            }
            //如果是明细列
            if (col.getDetailDto().getGroupType() == null
                    || col.getDetailDto().getGroupType().equals(QueryConstants.GroupType.NONE)) {
                hasDetailCol = true;
            }
        }
        //如果没有纵向分组的列，则直接在表中查询数据即可
        if (lstVerSpInfo.isEmpty()) {
            return findDataFromDetail(queryStruct, mapFilter);
        }
        //插入临时表
        String tempTable = createAndInsertTemptable(queryStruct, lstVerSpInfo, table.getTableDto().getTableName()
                , mapFilter);
        //更新临时表上级编码
        updateCode(tempTable, lstVerSpInfo);
        String groupTableAlias = "b";
        String[] detailAndGroupFieldExp = getDetailAndGroupFieldExp(queryStruct, "a", groupTableAlias);
        //按批次查询数据
        List<Map<String, Object>> lstData = queryLevelData(lstVerSpInfo, groupTableAlias,
                tempTable, detailAndGroupFieldExp[1]);
        if (hasDetailCol) {
            List<Map<String, Object>> lstDetail = queryDetailData(queryStruct, lstVerSpInfo, mapFilter);
            lstData.addAll(lstDetail);
        }
        sortData(lstData);

        return lstData;
    }

    /**
     * 取得明细数据
     *
     * @param queryStruct
     * @return
     */
    private List<Map<String, Object>> findDataFromDetail(Node queryStruct, Map<String, Object> mapFilter) {
        String tableAlias = "a";
        String sDetailSql = getDetailFieldExp(queryStruct, tableAlias);
        StringBuilder sbSql = new StringBuilder();
        sbSql.append("select ").append(sDetailSql).append(" from ").append(table.getTableDto().getTableName())
                .append(" ").append(tableAlias);
        Map<String, Object> mapParam = new HashMap<>();
        int[] indexs = new int[]{0};
        String filter = genFilter(mapFilter, tableAlias, indexs, mapParam);
        SqlExp sqlExp;
        if (CommonUtils.isNotEmpty(filter)) {
            sqlExp = new SqlExp(sbSql.toString() + " where " + filter, mapParam);
        } else {
            sqlExp = new SqlExp(sbSql.toString());
        }


        QueryParam queryParam = new QueryParam();
        queryParam.setSqlExp(sqlExp);
        HandleResult result = handlerFactory.handleQuery(queryParam);
        return result.getLstData();
    }

    /**
     * 生成最后一层次的数据并汇总生成临时表
     *
     * @return 返回临时表名
     */
    private String createAndInsertTemptable(
            Node queryStruct,
            List<VerticalSeparatorInfo> lstColInfo, String dataTableName,
            Map<String, Object> mapFilter) {
        StringBuilder sbSql = new StringBuilder();

        String tableAliasIn = "a";
        String tableAliasOut = "b";
        String[] detailAndGroupFieldExp = getDetailAndGroupFieldExp(queryStruct, tableAliasIn, tableAliasOut);
        String sDetailSql = detailAndGroupFieldExp[0];
        String sGroupSql = detailAndGroupFieldExp[1];
        String tempTableName = genTempTableName();
        sbSql.append("create TEMPORARY table  ").append(tempTableName).append(" IGNORE as ");
        //增加上层查询
        Map<String, Object> mapParam = new HashMap<>();
        String innerSql = getDetailSql(lstColInfo, mapParam, dataTableName,
                sDetailSql, tableAliasIn, mapFilter);
        sbSql.append(getDetailGroupSql(lstColInfo, sGroupSql, innerSql, tableAliasOut));


//        String delet = "drop table " + tempTableName;
//        SqlExp exp = new SqlExp(delet);
//        try {
//            UpdateParam param1 = new UpdateParam();
//            param1.setSqlExp(exp);
//            handlerFactory.handleUpdate(param1);
//        } catch (Exception e) {
//
//        }


        SqlExp exp = new SqlExp();
        exp.setSql(sbSql.toString());
        exp.setParamValues(mapParam);
        UpdateParam param = new UpdateParam();
        param.setSqlExp(exp);
        handlerFactory.handleUpdate(param);
        return tempTableName;
    }

    private String[] getDetailAndGroupFieldExp(Node queryStruct, String tableAliasDetail, String tableAliasGroup) {
        List<Node> leafNodes = queryStruct.getLeafNodes();
        StringBuilder sbDetailSql = new StringBuilder();
        StringBuilder sbGroupSql = new StringBuilder();
        for (Node node : leafNodes) {
            QueryField field = ((QueryComponent) node.getValue()).getQueryField();
            String colSql = field.getQueryCol()
                    .getDetailSelectSql(findParentFields(node), tableAliasDetail, field);
            if (CommonUtils.isNotEmpty(colSql)) {
                sbDetailSql.append(colSql).append(",");
            }
            colSql = field.getQueryCol()
                    .getGroupSelectSql(tableAliasGroup, field);
            if (CommonUtils.isNotEmpty(colSql)) {
                sbGroupSql.append(colSql).append(",");
            }
        }
        return new String[]{sbDetailSql.substring(0, sbDetailSql.length() - 1),
                sbGroupSql.substring(0, sbGroupSql.length() - 1)};
    }

    /**
     * 从物理表中直接查询明细数据的语句
     *
     * @param queryStruct
     * @param tableAliasDetail
     * @return
     */
    private String getDetailFieldExp(Node queryStruct, String tableAliasDetail) {
        List<Node> leafNodes = queryStruct.getLeafNodes();
        StringBuilder sbDetailSql = new StringBuilder();
        for (Node node : leafNodes) {
            QueryField field = ((QueryComponent) node.getValue()).getQueryField();
            String colSql = field.getQueryCol()
                    .getDetailSelectSql(findParentFields(node), tableAliasDetail, field);
            if (CommonUtils.isNotEmpty(colSql)) {
                sbDetailSql.append(colSql).append(",");
            }

        }
        return sbDetailSql.substring(0, sbDetailSql.length() - 1);

    }


    /**
     * 查询 明细数据,从原始表中取数
     *
     * @param lstCols
     * @param param
     * @param dataTableName
     * @param otherDetailColSql
     * @param tableAlias
     * @return
     */
    private String getDetailSql(List<VerticalSeparatorInfo> lstCols, Map<String, Object> param,
                                String dataTableName, String otherDetailColSql, String tableAlias
            , Map<String, Object> mapFilter) {
        StringBuilder sbSql = new StringBuilder();
        int[] indexes = new int[]{0};

        for (VerticalSeparatorInfo col : lstCols) {
            String sql = col.getInsertDetailSql(tableAlias, indexes, param);
            if (CommonUtils.isNotEmpty(sql)) {
                sbSql.append(sql).append(",");
            }
        }
        sbSql.append(otherDetailColSql);

        String sSql = "select " + sbSql.toString() + " from " + dataTableName + " " + tableAlias;
        String filter = genFilter(mapFilter, tableAlias, indexes, param);
        if (CommonUtils.isNotEmpty(filter)) {
            sSql += " where " + filter;
        }
        return sSql;
    }

    /**
     * 生成最后一级的查询语句
     *
     * @param lstVerCols
     * @param otherGroupField
     * @param detailSql
     * @return
     */
    private String getDetailGroupSql(List<VerticalSeparatorInfo> lstVerCols, String otherGroupField,
                                     String detailSql, String tableAlias) {
        StringBuilder sbSql = new StringBuilder();

        StringBuilder sbGroup = new StringBuilder();
        for (VerticalSeparatorInfo col : lstVerCols) {
            String sql = col.getInsertSuperCodeGroupSql(tableAlias);
            if (CommonUtils.isNotEmpty(sql)) {
                sbSql.append(sql).append(",");
            }
            String group = col.getGroupField(tableAlias, VerticalCol.LEAF_LEVEL);
            if (CommonUtils.isNotEmpty(group)) {
                sbGroup.append(group).append(",");
            }

        }
        sbSql.append(otherGroupField);
        return "select " + sbSql.toString() + " from (" + detailSql + ")   " + tableAlias +
                " group by " + sbGroup.substring(0, sbGroup.length() - 1);
    }

    private String genTempTableName() {
        return "aolie_q_t_" + IdGenerator.getNextId(TempletQueryEngine.class.getName());
    }

    public void updateCode(String tableName, List<VerticalSeparatorInfo> lstVerCols) {
        StringBuilder sbUpdate = new StringBuilder();
        String tableAlias = "a";
        for (VerticalSeparatorInfo col : lstVerCols) {
            String sql = col.getUpdateSql(tableAlias);
            if (CommonUtils.isNotEmpty(sql)) {
                sbUpdate.append(sql).append(",");
            }
        }
        if (sbUpdate.length() == 0) {
            return;
        }
        String sSql = "update " + tableName + " " + tableAlias + " set  " + sbUpdate.substring(0, sbUpdate.length() - 1);
        SqlExp sqlExp = new SqlExp(sSql);
        UpdateParam param = new UpdateParam();
        param.setSqlExp(sqlExp);
        handlerFactory.handleUpdate(param);
    }

    /**
     * 分层次查询数据
     *
     * @return
     */
    private List<Map<String, Object>> queryLevelData(List<VerticalSeparatorInfo> lstVerSpInfo,
                                                     String tableAlias, String tableName, String otherFieldGroupExp) {
        //一个一个纵向扩展列的查询，
        //从最后一列向前查询
        //每列需要加上前面最后一层级作为分组条件
        List<Map<String, Object>> lstResult = new ArrayList<>();
        //这里收集前面列分组字段
        Stack<String> lstPreField = getPreField(lstVerSpInfo);
        //这里从最里层向外层查询
        for (int i = lstVerSpInfo.size() - 1; i >= 0; i--) {
            lstPreField.pop();
            VerticalSeparatorInfo separatorInfo = lstVerSpInfo.get(i);
            String[] sSqls = separatorInfo.getLevelQuerySql(lstPreField, tableAlias, tableName, otherFieldGroupExp);
            List<Map<String, Object>>[] lstResults = queryData(sSqls);
            separatorInfo.transRefName(lstResults);
            for (List<Map<String, Object>> lstData : lstResults) {
                if (lstData != null) {
                    lstResult.addAll(lstData);
                }
            }
        }
        return lstResult;
    }

    //取得各纵向分组最后一级的字段信息
    private Stack<String> getPreField(List<VerticalSeparatorInfo> lstVerSpInfo) {
        Stack<String> lstPreField = new Stack<>();
        for (VerticalSeparatorInfo separatorInfo : lstVerSpInfo) {
            lstPreField.push(separatorInfo.getLastGroupField());
        }
        return lstPreField;
    }

    private List<Map<String, Object>>[] queryData(String[] querySql) {
        List<Map<String, Object>>[] lstResult = new List[querySql.length];
        SqlExp sqlExp = new SqlExp("");
        for (int i = 0; i < querySql.length; i++) {
            sqlExp.setSql(querySql[i]);
            QueryParam param = new QueryParam();
            param.setSqlExp(sqlExp);
            lstResult[i] = handlerFactory.handleQuery(param).getLstData();
        }
        return lstResult;

    }

    /**
     * 查询明细数据，如果存在明细列时，才会查询,需要拼入纵向分组字段，并翻译成编码，以生成序号，
     * 与前面的分组进行整合
     *
     * @return
     */
    private List<Map<String, Object>> queryDetailData(Node queryStruct, List<VerticalSeparatorInfo> lstColInfo
            , Map<String, Object> mapFilter) {
        Map<String, Object> mapParam = new HashMap<>();
        String tableAlias = "a";
        String innerSql = getDetailSql(lstColInfo, mapParam, table.getTableDto().getTableName(),
                getDetailFieldExp(queryStruct, tableAlias), tableAlias, mapFilter);
        //需要再包装一层，增加序号
        String tableAliasB = "b";
        Stack<String> preFields = getPreField(lstColInfo);
        StringBuilder sbSql = new StringBuilder();
        for (int i = 0; i < preFields.size(); i++) {
            sbSql.append("concat(");
            for (String preField : preFields) {
                sbSql.append(tableAliasB).append(".").append(preField).append(" ,").append("'_',");
            }
            sbSql.delete(sbSql.length() - 1, sbSql.length()).append(") as ").append(VerticalSeparatorInfo.FIELD_XH).append(",");
        }
        String fullSql = "select " + sbSql.substring(0, sbSql.length() - 1) + ","
                + tableAliasB + ".* from (" + innerSql + ")" + tableAliasB;

        return findBySql(fullSql, mapParam);

    }

    private List<Map<String, Object>> findBySql(String sSql) {
        SqlExp sqlExp = new SqlExp(sSql);
        QueryParam param = new QueryParam();
        param.setSqlExp(sqlExp);
        return handlerFactory.handleQuery(param).getLstData();
    }

    private List<Map<String, Object>> findBySql(String sSql, Map<String, Object> mapParam) {
        SqlExp sqlExp = new SqlExp(sSql, mapParam);
        QueryParam param = new QueryParam();
        param.setSqlExp(sqlExp);
        return handlerFactory.handleQuery(param).getLstData();
    }


    /**
     * 更新引用数据名称字段并排序
     */
    private void sortData(List<Map<String, Object>> lstData) {
        lstData.sort(new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> row1, Map<String, Object> row2) {
                return CommonUtils.getStringField(row1, VerticalSeparatorInfo.FIELD_XH, "")
                        .compareTo(CommonUtils.getStringField(row2, VerticalSeparatorInfo.FIELD_XH, ""));
            }
        });
        for (Map<String, Object> row : lstData) {
            System.out.println("------>" + row.get(VerticalSeparatorInfo.FIELD_XH) + "       " + row.get("detail_item1__3"));
        }
    }

    private TableInfo getQueryTable() {
        Column column = SchemaHolder.getColumn(this.lstCols.get(0).getDetailDto().getColumnId(),
                this.lstCols.get(0).getDetailDto().getVersionCode());
        return SchemaHolder.getTable(column.getColumnDto().getTableId(), column.getColumnDto().getVersionCode());
    }

    /**
     * 查找上级字段设置情况
     *
     * @param thisNode
     * @return
     */
    private List<QueryField> findParentFields(Node thisNode) {
        if (thisNode.getParent() == null) {
            return null;
        }
        Node node = thisNode;
        List<QueryField> lstResult = new ArrayList<>();
        while (node.getParent() != null) {
            node = node.getParent();
            if (node.getValue() == null) {
                //表明是根节点
                break;
            }
            QueryField field = ((QueryComponent) node.getValue()).getQueryField();
            if (field != null) {
                lstResult.add(field);
            }
        }
        Collections.reverse(lstResult);
        return lstResult;
    }

    private Node initQueryStruct() {
        //初始化化列序号
        int[] colIndex = new int[]{1};
        //从一开始
        String[] lvl = new String[]{"001"};
        if (lstCols == null) {
            throw new InvalidConfigException("没有子列信息");
        }
        Node root = TreeFactory.getInstance().createTreeNode(null);
        for (IQueryCol tempCol : lstCols) {
            Node nodeCol = tempCol.getColumnHeader(colIndex, lvl);
            if (nodeCol.getChildrenCount() > 0) {
                for (int i = 0; i < nodeCol.getChildrenCount(); i++) {
                    root.append((nodeCol.getChildAt(i)));
                }
            }
        }
        return root;
    }

    /**
     * 先生成表头
     *
     * @return
     */
    private BlockViewer getHeader(Node root) {
        List<Component> lstComp = new ArrayList<>();

        for (int i = 0; i < root.getChildrenCount(); i++) {
            Node nodeCol = root.getChildAt(i);
            lstComp.addAll(collectComp(nodeCol));
        }
        BlockViewDto viewDto = new BlockViewDto();
        viewDto.setBlockViewName("查询列表");
        viewDto.setFieldToCamel((short) 0);
        viewDto.setBlockViewId(-1L);
        viewDto.setTitle("查询列表");
        return new BlockViewer(viewDto, lstComp);
    }

    /**
     * 收集节点上的控件信息
     * 这里做一个特殊处理，如果一个分组节点下，只有一个普通列，则
     *
     * @param nodeCol
     */
    private List<QueryComponent> collectComp(Node nodeCol) {
        List<QueryComponent> lstCompOut = new ArrayList<>();
        QueryComponent thisComp = (QueryComponent) nodeCol.getValue();
        lstCompOut.add(thisComp);
        //如果只有一个直接孩子，自己是扩展的列，则去掉一个层次
        if (hasOnlyDirectSon(nodeCol)) {
            QueryComponent subComponent = (QueryComponent) nodeCol.getChildAt(0).getValue();
            thisComp.setColumn(subComponent.getColumn());
            thisComp.getComponentDto().setDispType(subComponent.getComponentDto().getDispType());
            thisComp.getComponentDto().setFormat(subComponent.getComponentDto().getFormat());
            thisComp.getComponentDto().setWidth(subComponent.getComponentDto().getWidth());
            return lstCompOut;
        }
        if (nodeCol.getChildrenCount() > 0) {

            for (int i = 0; i < nodeCol.getChildrenCount(); i++) {
                lstCompOut.addAll(collectComp(nodeCol.getChildAt(i)));
            }
        }
        return lstCompOut;
    }

    private boolean hasOnlyDirectSon(Node curNode) {
        if (curNode.getChildrenCount() != 1) {
            return false;
        }
        //本级的定义是分组，只有一个下级普通列
        QueryField field = ((QueryComponent) curNode.getValue()).getQueryField();
        return field.getQueryCol().getDetailDto().getExtendType().equals(QueryConstants.ExtendType.HORIZON_SEPARATE);
    }

    /**
     * 初始化列信息
     */
    private void initCol() throws Exception {
        //这里只生成第一级次的列信息,因为创建是递归的
        List<QrTempletDetailDto> lstDetail = queryMain.getLstDetail();
        if (lstDetail == null || lstDetail.isEmpty()) {
            return;
        }
        lstCols = new ArrayList<>();
        //将明细生成树状结构
        Node node = TreeNodeHelper.getInstance().generateByCode(lstDetail, "templetDetailId",
                "lvlCode", "title",
                SysCodeRule.createDefault());
        int childCount = node.getChildrenCount();
        for (int i = 0; i < childCount; i++) {
            Node childNode = node.getChildAt(i);
            BaseQueryCol queryCol = QueryColHelper.createQueryCol((QrTempletDetailDto) childNode.getValue());
            lstCols.add(queryCol);
            queryCol.initSubByNode(childNode);
        }
    }
}
