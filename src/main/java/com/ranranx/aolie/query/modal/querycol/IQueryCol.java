package com.ranranx.aolie.query.modal.querycol;

import com.ranranx.aolie.core.tree.Node;
import com.ranranx.aolie.querydesign.dto.QrTempletDetailDto;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 查询分析列接口
 */
public interface IQueryCol {

    /**
     * 取得列的节点信息，包含扩展的
     *
     * @param fieldIndex 传入并需要传出的字段序号
     * @param colLvlCode 传入的当前开始的级次编码，如果有扩展的列，则需要修改此值返回，
     * @return
     */
    Node getColumnHeader(int[] fieldIndex, String[] colLvlCode);

    /**
     * 取得基础插入
     *
     * @param tableAlias
     * @return
     */
    String getDetailSelectSql(List<QueryField> parentQueryFields, String tableAlias, QueryField field);

    /**
     * 取得由明细插入到临时表时的语句，需要带有分组函数
     *
     * @param tableAlias
     * @return
     */
    String getDetailGroupSql(List<QueryField> parentQueryFields, String tableAlias, QueryField field);

    /**
     * 取得分组查询的语句
     *
     * @param tableAlias
     * @return
     */
    String getGroupSelectSql(String tableAlias, QueryField field);

    /**
     * 取得需要放到分组（group by )后面的字段
     *
     * @param tableAlias
     * @return
     */
    String getGroupByFields(String tableAlias);

    /**
     *
     * @return
     */
    VerticalSeparatorInfo getVerticalSeparateColInfo();

    /**
     * 是否存在明细的列，
     * 明细列需要单独查询
     *
     * @return
     */
    boolean hasDetailCol();


    /**
     * 取得下级列
     *
     * @return
     */
    List<IQueryCol> getSubCol();

    /**
     * 检查当前设置是否合理
     *
     * @return
     */
    String check();

    /**
     * 取得
     *
     * @return
     */
    QrTempletDetailDto getDetailDto();
}
