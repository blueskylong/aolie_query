package com.ranranx.aolie.query.modal.querycol;

import com.ranranx.aolie.core.common.CommonUtils;
import com.ranranx.aolie.core.tree.Node;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 纵向扩展列的数据结构
 *
 * @author xxl
 * @version V0.0.1
 * @date 2021/6/28 0028 13:34
 **/
public class VerticalSeparatorInfo {

    /**
     * 序号虚拟列
     */
    public static final String FIELD_XH = "xh";

    public static final String FIELD_CODE_AFFIX = "_code_final";
    /**
     * 选择的引用生成的树，树的末级节点上存着对应的ID，如果树不是以编码来组织的，则生成各节点的编码，
     */
    private Node nodeRef;
    private VerticalCol leafCol;
    /**
     * 引用数据中，编码对应名称，用于翻译
     */
    private Map<String, String> mapRefCodeToName = new HashMap<>();

    /**
     * 主列字段名
     */
    private String fieldName;
    private String showField;

    /**
     * 生成的虚拟列信息 [编码字段，名称字段]
     */
    private List<VerticalCol> lstField;

    public VerticalSeparatorInfo(Node nodeRef, String realField) {
        this.nodeRef = nodeRef;
        this.fieldName = realField;
        initStruct();
    }

    /**
     * 取得本列最后的层次字段
     *
     * @return
     */
    public String getLastGroupField() {
        return leafCol.getNewField();
    }

    public String getGroupField(String tableAlias, int level) {
        for (VerticalCol col : lstField) {
            if (col.getIndex() == level) {
                return col.getNewField();
            }
        }
        return null;
    }

    public String getShowField() {
        return showField;
    }

    public void setShowField(String showField) {
        this.showField = showField;
    }

    public List<VerticalCol> getCols() {
        return this.lstField;
    }

    /**
     * 根据节点信息，生成数据结构
     */
    private void initStruct() {
        Node[][] nodes = nodeRef.toArray();
        int deep = nodes.length;
        lstField = new ArrayList<>(deep);
        //从第二级开始，第一级是虚拟节点
        for (int i = 1; i < deep; i++) {
            Node[] levelNode = nodes[i];
            //生成级次扩展的虚拟列
            lstField.add(new VerticalCol(fieldName, makeNodeCodeMap(levelNode), i));
        }
        //生成最末级节点
        leafCol = new VerticalCol(fieldName, makeNodeCodeToIds(nodeRef.getLeafNodes()), VerticalCol.LEAF_LEVEL);
        lstField.add(leafCol);
    }

    /**
     * 生成前后节点编码的对照表　　key：thisCode value:List subCodes;
     *
     * @param nodes
     * @return
     */
    private Map<Object, List<Object>> makeNodeCodeMap(Node[] nodes) {
        Map<Object, List<Object>> map = new HashMap<>();
        for (Node node : nodes) {
            //收集对应信息
            this.mapRefCodeToName.put(node.getCustomObject().toString(), node.getText());
            Node[] children = node.getChildren();
            if (children == null || children.length == 0) {
                continue;
            }
            List<Object> lstCodes = new ArrayList<>();
            for (Node subNode : children) {
                lstCodes.add(subNode.getCustomObject());
            }
            map.put(node.getCustomObject(), lstCodes);

        }
        return map;
    }

    /**
     * 取得按级次的查询语句，有多少个级次，就生成多少个查询语句
     *
     * @param preFields
     * @return
     */
    public String[] getLevelQuerySql(List<String> preFields, String tableAlias, String tableName, String otherGroupedValueField) {

        //分级数量即是要查询的次数，且最后一个叶子级不可以算
        String[] sqls = new String[lstField.size() - 1];
        String groupFields = "";
        if (preFields != null && !preFields.isEmpty()) {
            for (String field : preFields) {
                groupFields += tableAlias + "." + field + ",";
            }
        }
        for (int i = 0; i < lstField.size() - 1; i++) {
            StringBuilder sbSql = new StringBuilder();
            VerticalCol verticalCol = lstField.get(i);
            //生成排序列
            if (preFields == null || preFields.isEmpty()) {
                sbSql.append(tableAlias).append(".")
                        .append(verticalCol.getNewField()).append(" as ").append(FIELD_XH).append(",");
            } else {
                sbSql.append("concat(");
                for (String preField : preFields) {
                    sbSql.append(tableAlias).append(".").append(preField).append(" ,").append("'_',");
                }
                //还要加上本级编码
                sbSql.append(tableAlias).append(".").append(verticalCol.getNewField())
                        .append(") as ").append(FIELD_XH).append(",");
            }
            //生成编码字段
            sbSql.append(tableAlias).append(".").append(verticalCol.getNewField())
                    .append(" as ").append(fieldName).append(FIELD_CODE_AFFIX).append(",");
            //增加其它字段
            sbSql.append(otherGroupedValueField);
            sqls[i] = "select " + sbSql.toString() + " from " + tableName + " " + tableAlias
                    + " where " + tableAlias + "." + verticalCol.getNewField() + "!='"
                    + VerticalCol.EMPTY_VALUE + "'" + " group by " + groupFields
                    + tableAlias + "." + verticalCol.getNewField();


        }
        return sqls;
    }

    /**
     * 执行翻译，数组级次，代码层次
     *
     * @param lstArr
     */
    public void transRefName(List<Map<String, Object>>[] lstArr) {
        if (lstArr == null || lstArr.length == 0) {
            return;
        }
        String codeFieldName = fieldName + FIELD_CODE_AFFIX;
        for (int i = 0; i < lstArr.length; i++) {
            //层次空格
            String preFix = "";
            //三个空格一个层级
            preFix = CommonUtils.stuff(preFix, ' ', i * 3);
            List<Map<String, Object>> lstData = lstArr[i];
            if (lstData == null || lstData.isEmpty()) {
                continue;
            }
            for (Map<String, Object> row : lstData) {
                String code = CommonUtils.getStringField(row, codeFieldName);
                row.put(showField, preFix + mapRefCodeToName.get(code));
            }
        }
    }


    /**
     * 取得更新名称列的语句
     *
     * @param tableAlias
     * @return
     */
    public String getUpdateSql(String tableAlias) {
        StringBuilder sbSql = new StringBuilder();

        for (VerticalCol col : lstField) {
            String sSql = col.getUpdateSql(tableAlias, leafCol.getNewField());
            if (CommonUtils.isNotEmpty(sSql)) {
                sbSql.append(sSql).append(",");
            }
        }
        return sbSql.substring(0, sbSql.length() - 1);
    }

    /**
     * 生成最后一级的编码对应ＩＤ的数据，id数据，在生成表头时生成的
     *
     * @param leafNodes
     * @return
     */
    private Map<Object, List<Object>> makeNodeCodeToIds(List<Node> leafNodes) {
        Map<Object, List<Object>> map = new HashMap<>();
        for (Node node : leafNodes) {
            SeparateNodeInfo separateNodeInfo = ((QueryComponent) node.getValue()).getQueryField();
            map.put(node.getCustomObject(), separateNodeInfo.getLstLeafIds());
        }
        return map;
    }

    /**
     * 取得插入的第二层语句
     * 如果是普通列，在第一次插入时，没有值
     *
     * @return
     */
    public String getInsertSuperCodeGroupSql(String tableAlias) {
        StringBuilder sbSql = new StringBuilder();
        for (VerticalCol col : lstField) {

            String colField = col.getInsertSuperCodeGroupSql(tableAlias);
            if (CommonUtils.isNotEmpty(colField)) {
                sbSql.append(colField).append(",");
            }
        }
        if (sbSql.length() > 0) {
            return sbSql.substring(0, sbSql.length() - 1);
        }
        return null;
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
        StringBuilder sbSql = new StringBuilder();
        for (VerticalCol col : lstField) {

            String colField = col.getInsertDetailSql(tableAlias, paramIndex, mapParam);
            if (CommonUtils.isNotEmpty(colField)) {
                sbSql.append(colField).append(",");
            }
        }
        if (sbSql.length() > 0) {
            return sbSql.substring(0, sbSql.length() - 1);
        }
        return sbSql.toString();
    }

}
