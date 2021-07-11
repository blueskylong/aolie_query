package com.ranranx.aolie.query.modal.querycol;

import com.ranranx.aolie.core.common.CommonUtils;
import com.ranranx.aolie.core.datameta.datamodel.*;
import com.ranranx.aolie.core.datameta.dto.ColumnDto;
import com.ranranx.aolie.core.datameta.dto.ComponentDto;
import com.ranranx.aolie.core.exceptions.IllegalOperatorException;
import com.ranranx.aolie.core.exceptions.InvalidConfigException;
import com.ranranx.aolie.core.exceptions.InvalidParamException;
import com.ranranx.aolie.core.exceptions.NotExistException;
import com.ranranx.aolie.core.tree.*;
import com.ranranx.aolie.querydesign.dto.QrTempletDetailDto;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author xxl
 * @version V0.0.1
 * @date 2021/6/21 0021 21:35
 **/
public class HorizonSeparateCol extends BaseQueryCol {
    public HorizonSeparateCol(QrTempletDetailDto detailDto) {
        super(detailDto);
    }

    /**
     * 记录完整的引用树，用于收集得下级子节点的ID
     */
    protected Node fullRefNodes;


    @Override
    public Node getColumnHeader(int[] fieldIndex, String[] colLvlCode) {
        String err = check();
        if (CommonUtils.isNotEmpty(err)) {
            throw new InvalidConfigException(err);
        }

        //取得设计的表头结构
        //取得本级扩展的表头(不包含末级节点) 节点上的VALUE是 Component
        Node refHeader = getDistributeHeader(colLvlCode);
        //扩展至下级
        List<Node> lstLeafNode = findAllLeafNode(refHeader);
        for (Node leafNode : lstLeafNode) {
            //为每一个叶子节点，增加查询字段信息
            QueryField field = new QueryField(refCol.getColumnDto().getFieldName(), findLeafIds(fullRefNodes,
                    leafNode.getIdentifier()), this);
            ((QueryComponent) leafNode.getValue()).setQueryField(field);
            insertSubCol(leafNode, fieldIndex);
        }
        return refHeader;
        //找到每一个叶子节点,再和下级拼接
    }

    /**
     * 是不是只有一个儿子，没有孙子
     *
     * @return
     */
    private boolean isOnlyHaveSingleSon() {
        return this.subCol.size() == 1 &&
                (this.subCol.get(0).getSubCol() == null || this.subCol.get(0).getSubCol().isEmpty());
    }

    /**
     * 根据指定的ID 取得所有叶子节点的ID
     *
     * @param nodeFull
     * @param id
     * @return
     */
    protected List<Object> findLeafIds(Node nodeFull, Object id) {
        Node node = nodeFull.findNode(id);
        List<Node> allLeafNode = findAllLeafNode(node);
        if (!allLeafNode.isEmpty()) {
            List<Object> lstId = new ArrayList<>();
            allLeafNode.forEach(el -> lstId.add(el.getIdentifier()));
            return lstId;
        }
        return null;

    }


    /**
     * 查找所有的叶子节点
     *
     * @param node
     * @return
     */
    protected List<Node> findAllLeafNode(Node node) {
        List<Node> lstNode = new ArrayList<>();
        if (node.getChildrenCount() == 0) {
            lstNode.add(node);
            return lstNode;
        }
        Node[] children = node.getChildren();
        for (Node subNode : children) {
            findSubLeafNode(subNode, lstNode);
        }
        return lstNode;
    }

    private void findSubLeafNode(Node node, List<Node> lstLeaf) {
        if (node.getChildrenCount() == 0) {
            lstLeaf.add(node);
        } else {
            Node[] children = node.getChildren();
            for (Node subNode : children) {
                findSubLeafNode(subNode, lstLeaf);
            }
        }
    }

    /**
     * 一个一个在扩展引用表头的基础上,增加设计的下级列
     *
     * @param curRefNode
     */
    private void insertSubCol(Node curRefNode, int[] fieldIndex) {
        //不是叶子节点不处理
        if (curRefNode.getChildrenCount() > 0) {
            return;
        }

        Component component = (Component) curRefNode.getValue();
        LevelProvider provider = new LevelProvider(component.getComponentDto().getLvlCode());
        String[] lvl = new String[]{provider.getFirstSubCode()};
        for (IQueryCol col : subCol) {
            Node columnHeader = col.getColumnHeader(fieldIndex, lvl);
            curRefNode.append(columnHeader.getChildren());
        }
    }

    /**
     * 生成本级扩展表头
     *
     * @param lvlCode
     * @return
     */
    protected Node getDistributeHeader(String[] lvlCode) {
        Node refHeader = findRefHeader();
        distributeLvlCodeAndCreateHeadCol(refHeader, lvlCode);
        return refHeader;
    }

    /**
     * 根据扩展出来的表头节点,生成表头信息
     * 由于表头都是上级,所以不带字段值
     * 只生成级次及表头信息,关注下级节点
     *
     * @param headerNode
     * @return 返回最后一个一级级次(没有被使用)
     */
    private void distributeLvlCodeAndCreateHeadCol(Node headerNode, String[] lvlCode) {
        LevelProvider levelProvider = new LevelProvider(lvlCode[0]);
        int count = headerNode.getChildrenCount();
        String lvl = lvlCode[0];
        for (int i = 0; i < count; i++) {
            Node subNode = headerNode.getChildAt(i);
            distributeSubLvl(subNode, lvl);
            lvl = levelProvider.getNextCode();
        }
        lvlCode[0] = lvl;
    }


    /**
     * 生成下级子节点的表头信息
     *
     * @param subNode
     * @param lvlCode
     */
    private void distributeSubLvl(Node subNode, String lvlCode) {
        LevelProvider levelProvider = new LevelProvider(lvlCode);
        ReferenceData referenceData = (ReferenceData) subNode.getValue();
        Component component = makeComponent(this.detailDto, this.refCol, referenceData, lvlCode, refCol.getColumnDto().getFieldName());
        subNode.setValue(component);
        subNode.setCustomObject(lvlCode);
        if (subNode.getChildrenCount() > 0) {
            lvlCode = levelProvider.getFirstSubCode();
            for (int i = 0; i < subNode.getChildrenCount(); i++) {
                distributeSubLvl(subNode.getChildAt(i), lvlCode);
                lvlCode = levelProvider.getNextCode();
            }
        }

    }

    private QueryComponent makeComponent(QrTempletDetailDto detailDto, Column column,
                                         ReferenceData referenceData,
                                         String lvlCode, String fieldName) {
        ComponentDto comDto = new ComponentDto();
        BeanUtils.copyProperties(detailDto, comDto);
        comDto.setLvlCode(lvlCode);
        comDto.setTitle(referenceData.getName());
        column = CommonUtils.deepClone(column);
        column.getColumnDto().setFieldName(fieldName);
        QueryComponent component = new QueryComponent();
        component.setColumn(column);
        component.setComponentDto(comDto);
        return component;


    }


    /**
     * 生成表头节点,不含表头信息
     *
     * @return
     */
    private Node findRefHeader() {
        try {
            Long refId = this.detailDto.getRefId();
            if (refId == null) {
                refId = SchemaHolder.getColumn(this.detailDto.getColumnId(),
                        detailDto.getVersionCode()).getColumnDto().getRefId();
                if (refId == null) {
                    throw new InvalidParamException("横向扩展列,引用不可以为空");
                }
                detailDto.setRefId(refId);

            }
            List<ReferenceData> referenceData = QueryColHelper.findReferenceData(refId, this.detailDto.getVersionCode());
            Reference reference = SchemaHolder.getReference(refId, detailDto.getVersionCode());

            if (referenceData == null || referenceData.isEmpty() || reference == null) {
                throw new NotExistException("引用数据为空");
            }

            //生成完全表头
            Node node;
            if (reference.isHasParentIdField()) {
                node = TreeNodeHelper.getInstance().generateById(referenceData, "id", "name", "parentId",
                        "code");
            } else {
                node = TreeNodeHelper.getInstance().generateByCode(referenceData, "id", "code", "name",
                        SysCodeRule.createDefault());
            }
            //复制节点，备用
            this.fullRefNodes = CommonUtils.deepClone(node);
            //修整表头
            if (isRollup(detailDto)) {
                node = getRollUpNodes(detailDto.getLevelShow(), node);
            } else {
                node = getLvlNodes(detailDto.getLevelShow(), node);
            }
            if (CommonUtils.isNotEmpty(detailDto.getSelectIds())) {
                String[] split = detailDto.getSelectIds().split(";");
                if (split.length > 0) {
                    List<Object> lstIds = Arrays.asList(split);
                    deleteUnSelectNodes(node, lstIds);
                }
            }
            return node;

        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalOperatorException("操作失败：" + e.getMessage());

        }

    }

    public void deleteUnSelectNodes(Node nodeValid, List<Object> lstSelectIds) {
        List<Node> leafNodes = nodeValid.getLeafNodes();
        leafNodes.forEach(node -> {
            if (lstSelectIds.indexOf(node.getIdentifier()) == -1) {
                removeNode(node);
            }
        });
    }

    /**
     * 删除节点，并级联删除只有此一个节点的父节点
     *
     * @param nodeToDelete
     */
    private void removeNode(Node nodeToDelete) {
        if (nodeToDelete.getParent() != null) {
            Node nodeParent = nodeToDelete.getParent();
            nodeParent.deleteSubNode(nodeToDelete);
            if (nodeParent.getChildrenCount() == 0) {
                removeNode(nodeParent);
            }
        }
    }

    /**
     * 取得指定级次的节点
     *
     * @param lvl  级次,如果小于等于0则表示最后一级  级次从1 开始计数
     * @param node
     * @return
     */
    private Node getLvlNodes(Integer lvl, Node node) {
        Node rootNode = TreeFactory.getInstance().createTreeNode(null);
        Node[][] nodes = node.toArray();
        if (lvl == null || lvl <= 0) {
            Node subNode;
            for (int i = 0; i < nodes.length; i++) {
                for (int j = 0; j < nodes[i].length; j++) {
                    subNode = nodes[i][j];
                    if (subNode.getChildrenCount() == 0) {
                        rootNode.append(subNode);
                    }
                }
            }
            return rootNode;
        } else {
            if (lvl > nodes.length) {
                return null;
            }
            Node[] lvlNodes = nodes[lvl - 1];
            if (lvlNodes == null || lvlNodes.length == 0) {
                return null;
            }
            for (Node subNode : lvlNodes) {
                rootNode.append(subNode);
            }
            return rootNode;
        }
    }

    /**
     * 取得向上汇总的所有节点,采用的删除多余节点策略
     *
     * @param lvl
     * @param node
     * @return
     */
    private Node getRollUpNodes(Integer lvl, Node node) {
        //如果小于1则全返回
        if (lvl == null || lvl < 1) {
            return node;
        }
        Node[][] nodes = node.toArray();
        //超过范围的也全返回
        if (lvl > nodes.length) {
            return node;
        }
        Node[] subNodes = nodes[lvl - 1];
        //这些节点删除下级节点就可以了
        for (Node subNode : subNodes) {
            clearNodeChildren(subNode);
        }
        return node;
    }

    private void clearNodeChildren(Node node) {
        int iCount = node.getChildrenCount();
        for (int i = iCount - 1; i >= 0; i--) {
            node.deleteSubNode(node.getChildAt(0));
        }
    }

    @Override
    public String check() {
        //横向扩展，必须有下级的数据列，否则无显示的内容
        if (!this.hasSubCol()) {
            return "横向扩展列需要指定具有数值的下级列[" + detailDto.getTitle() + "]";
        }
        QrTempletDetailDto subDetail;
        String err = null;
        //如下下级为普通列则还必须是分组属性
        for (IQueryCol col : this.subCol) {
            subDetail = col.getDetailDto();
            if (isNormalCol(subDetail) && !isGroupField(subDetail)) {
                return "横向扩展下的字段需要指定分组类型[" + subDetail.getTitle() + "]";
            }
            if (isVerticalSeparate(subDetail)) {
                //这里不允许存在横向扩展下有纵向
                return "横向扩展下有不允许存在纵向扩展列[" + subDetail.getTitle() + "]";
            }
            err = col.check();
            if (!CommonUtils.isNotEmpty(err)) {
                return err;
            }
        }
        return null;
    }
}
