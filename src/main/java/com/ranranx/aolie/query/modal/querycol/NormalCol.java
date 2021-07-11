package com.ranranx.aolie.query.modal.querycol;

import com.ranranx.aolie.core.datameta.dto.ColumnDto;
import com.ranranx.aolie.core.tree.LevelProvider;
import com.ranranx.aolie.core.tree.Node;
import com.ranranx.aolie.core.tree.TreeFactory;
import com.ranranx.aolie.querydesign.dto.QrTempletDetailDto;

/**
 * 普通查询分析列
 * 普通列可以直接是明细列分组列等
 * 也可以有下级,下级类型不限制
 *
 * @author xxl
 * @version V0.0.1
 * @date 2021/6/21 0021 21:33
 **/
public class NormalCol extends BaseQueryCol {

    public NormalCol(QrTempletDetailDto detailDto) {
        super(detailDto);

    }

    @Override
    public Node getColumnHeader(int[] fieldIndex, String[] colLvlCode) {
        //这个实现,只适合普通节点
        //使用上级提供的级次
        Node rootNode = TreeFactory.getInstance().createTreeNode(null);
        QueryComponent thisCom = toComponent(detailDto, colLvlCode[0],
                detailDto.getVersionCode());
        Node thisNode = TreeFactory.createTreeNode(detailDto.getTempletDetailId(),
                detailDto.getTitle(), colLvlCode[0], thisCom,
                this.detailDto);
        rootNode.append(thisNode);
        LevelProvider lvlProvider = new LevelProvider(colLvlCode[0]);
        String thisCode = colLvlCode[0];
        colLvlCode[0] = lvlProvider.getNextCode();
        //如果有下级,则本级不占用字段
        if (!hasSubCol()) {
            //分配字段
            ColumnDto columnDto = thisCom.getColumn().getColumnDto();
            String fieldOra = columnDto.getFieldName();
            columnDto.setFieldName(
                    genFieldName(columnDto.getFieldName(), fieldIndex[0]));
            //记录本级字段
            int index = fieldIndex[0];
            fieldIndexes.add(index);
            fieldIndex[0]++;

            QueryField field = new QueryField(fieldOra,
                    null, index, this);
            thisCom.setQueryField(field);
            return rootNode;
        }
        lvlProvider.setSCurrentCode(thisCode);
        String[] lvl = new String[]{lvlProvider.getFirstSubCode()};
        if (this.getSubCol() != null && !this.getSubCol().isEmpty()) {
            for (IQueryCol col : getSubCol()) {
                thisNode.append(col.getColumnHeader(fieldIndex, lvl));
                lvl[0] = lvlProvider.getNextCode();
            }
        }
        return rootNode;
    }

//    @Override
//    public String getDetailSelectSql(String tableAlias) {
//        //如果没有下级，则直接返回来列进行选择
//        if (!hasSubCol()) {
//            String fieldAlias = genFieldName(refCol.getColumnDto().getFieldName(), fieldIndexes.get(0));
//            return tableAlias + "." + refCol.getColumnDto().getFieldName() + " as " + fieldAlias;
//        }
//        //如果有下级，则由下级处理
//        StringBuffer stringBuffer = new StringBuffer();
//        for (IQueryCol col : subCol) {
//            String sql = col.getDetailSelectSql(tableAlias);
//            if (CommonUtils.isNotEmpty(sql)) {
//                stringBuffer.append(sql).append(",");
//            }
//        }
//        return stringBuffer.substring(0, stringBuffer.length() - 1);
//    }
}
