package com.ranranx.aolie.query.modal.querycol;

import com.ranranx.aolie.core.tree.LevelProvider;
import com.ranranx.aolie.core.tree.Node;
import com.ranranx.aolie.core.tree.TreeFactory;
import com.ranranx.aolie.querydesign.dto.QrTempletDetailDto;

import java.util.List;

/**
 * 纵向扩展列
 *
 * @author xxl
 * @version V0.0.1
 * @date 2021/6/21 0021 21:38
 **/
public class VerticalRollupCol extends HorizonSeparateCol {
    private VerticalSeparatorInfo separatorInfo;
    private String showField;

    public VerticalRollupCol(QrTempletDetailDto detailDto) {
        super(detailDto);
        initColInfo();
    }

    @Override
    public String check() {
        //纵向扩展下面不允许有其它列
        if (this.subCol != null || !this.subCol.isEmpty()) {
            return "纵向扩展列下不可以有下级列";
        }
        return null;
    }

    private void initColInfo() {
        //取得设计的表头结构
        //取得本级扩展的表头(不包含末级节点) 节点上的VALUE是 Component
        Node refHeader = initLeafIds(getDistributeHeader(new String[]{"001"}));
        separatorInfo = new VerticalSeparatorInfo(refHeader, refCol.getColumnDto().getFieldName());
    }

    /**
     * 生成末级编码对应ID 的关系
     *
     * @param refHeader
     */
    private Node initLeafIds(Node refHeader) {
        List<Node> lstLeafNode = refHeader.getLeafNodes();
        for (Node leafNode : lstLeafNode) {
            //为每一个叶子节点，增加查询字段信息
            QueryField field = new QueryField(refCol.getColumnDto().getFieldName(), findLeafIds(fullRefNodes,
                    leafNode.getIdentifier()), this);
            ((QueryComponent) leafNode.getValue()).setQueryField(field);
        }
        return refHeader;
    }

    @Override
    public Node getColumnHeader(int[] fieldIndex, String[] colLvlCode) {
        //这个实现,只适合普通节点
        //使用上级提供的级次
        Node rootNode = TreeFactory.getInstance().createTreeNode(null);
        QueryComponent thisCom = toComponent(detailDto, colLvlCode[0],
                detailDto.getVersionCode());
        //这里显示转换成文本
        detailDto.setDispType(null);
        thisCom.getComponentDto().setDispType(null);
        Node thisNode = TreeFactory.createTreeNode(detailDto.getTempletDetailId(),
                detailDto.getTitle(), colLvlCode[0], thisCom,
                this.detailDto);
        rootNode.append(thisNode);
        LevelProvider lvlProvider = new LevelProvider(colLvlCode[0]);
        colLvlCode[0] = lvlProvider.getNextCode();
        //分配字段
        String fieldOra = thisCom.getColumn().getColumnDto().getFieldName();
        showField = genFieldName(fieldOra, fieldIndex[0]);
        thisCom.getColumn().getColumnDto().setFieldName(
                showField);
        //记录本级字段
        fieldIndexes.add(++fieldIndex[0]);
        thisCom.setQueryField(new QueryField(fieldOra,
                null, fieldIndexes.get(0), this));
        //显示字段确定后，提交给列信息
        separatorInfo.setShowField(showField);
        return rootNode;
    }

    /**
     * @return
     */
    @Override
    public VerticalSeparatorInfo getVerticalSeparateColInfo() {
        return this.separatorInfo;
    }
}
