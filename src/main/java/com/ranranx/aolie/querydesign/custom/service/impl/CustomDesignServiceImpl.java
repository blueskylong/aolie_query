package com.ranranx.aolie.querydesign.custom.service.impl;

import com.ranranx.aolie.core.api.interfaces.ModelApi;
import com.ranranx.aolie.core.common.CommonUtils;
import com.ranranx.aolie.core.common.Constants;
import com.ranranx.aolie.core.common.SessionUtils;
import com.ranranx.aolie.core.datameta.datamodel.SchemaHolder;
import com.ranranx.aolie.core.datameta.datamodel.TableInfo;
import com.ranranx.aolie.core.ds.definition.FieldOrder;
import com.ranranx.aolie.core.exceptions.IllegalOperatorException;
import com.ranranx.aolie.core.exceptions.NotExistException;
import com.ranranx.aolie.core.handler.HandleResult;
import com.ranranx.aolie.core.handler.HandlerFactory;
import com.ranranx.aolie.core.handler.param.DeleteParam;
import com.ranranx.aolie.core.handler.param.InsertParam;
import com.ranranx.aolie.core.handler.param.QueryParam;
import com.ranranx.aolie.core.handler.param.UpdateParam;
import com.ranranx.aolie.querydesign.QueryConstants;
import com.ranranx.aolie.querydesign.custom.service.CustomDesignService;
import com.ranranx.aolie.querydesign.dto.QrCustomTempletDto;
import com.ranranx.aolie.querydesign.dto.QrTempletDetailDto;
import com.ranranx.aolie.querydesign.dto.QrTempletQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * @author xxl
 * @version V0.0.1
 * @date 2021/6/16 0016 16:11
 **/
@Service
@Transactional(readOnly = true)
public class CustomDesignServiceImpl implements CustomDesignService {

    @Autowired
    private ModelApi modelApi;

    @Autowired
    private HandlerFactory factory;

    /**
     * 保存查询模板
     *
     * @param templetQuery
     * @return
     */
    @Override
    @Transactional(readOnly = false)
    public HandleResult saveTemplet(QrTempletQuery templetQuery) {
        if (templetQuery.getMainDto().getTempletId() == null || templetQuery.getMainDto().getTempletId() < 0) {
            //新增
            return addTemplet(templetQuery);
        } else {
            //执行更新
            return updateTemplet(templetQuery, SessionUtils.getLoginVersion());
        }
    }

    private HandleResult updateTemplet(QrTempletQuery templetQuery, String version) {

        //更新主表
        UpdateParam param = UpdateParam.genUpdateByObject(QueryConstants.QUERY_DEFAULT_SCHEMA_ID,
                version, templetQuery, false);
        HandleResult result = factory.handleUpdate(param);
        if (!result.isSuccess()) {
            return result;
        }
        //执行更新,删除插入
        TableInfo mainTable = SchemaHolder.findTableByDto(QrCustomTempletDto.class,
                QueryConstants.QUERY_DEFAULT_SCHEMA_ID, version);
        if (mainTable == null) {
            throw new NotExistException("没找到数据表定义:" + QrCustomTempletDto.class.getName());
        }
        return saveTempletDetail(templetQuery.getLstDetail(), templetQuery.getMainDto().getTempletId(), version);
    }

    /**
     * 保存明细信息
     *
     * @param lstDetail
     * @param templetId
     * @param version
     * @return
     */

    @Override
    @Transactional(readOnly = false)
    public HandleResult saveTempletDetail(List<QrTempletDetailDto> lstDetail, Long templetId,
                                          String version) {
        TableInfo detailTable = SchemaHolder.findTableByDto(QrTempletDetailDto.class,
                QueryConstants.QUERY_DEFAULT_SCHEMA_ID, version);
        TableInfo mainTable = SchemaHolder.findTableByDto(QrCustomTempletDto.class,
                QueryConstants.QUERY_DEFAULT_SCHEMA_ID, version);
        if (detailTable == null) {
            throw new NotExistException("没找到数据表定义:" + QrTempletDetailDto.class.getName());
        }
        return modelApi.saveSlaveRows(CommonUtils.toMapAndConvertToUnderLine(lstDetail),
                detailTable.getTableDto().getTableId(), mainTable.getTableDto().getTableId(),
                templetId);
    }

    /**
     * 增加模板数据
     *
     * @param templetQuery
     * @return
     */
    private HandleResult addTemplet(QrTempletQuery templetQuery) {
        Long mainId = -1L;
        InsertParam insertParam = new InsertParam();
        templetQuery.getMainDto().setTempletId(mainId);
        insertParam.setObject(templetQuery.getMainDto(), QueryConstants.QUERY_DEFAULT_SCHEMA_ID);
        HandleResult result = factory.handleInsert(insertParam);
        if (!result.isSuccess()) {
            return result;
        }
        result = new HandleResult();

        result.setSuccess(true);
        //取得插入后ID值
        mainId = (Long) ((Map<String, Object>) result.getLstData()
                .get(0).get(Constants.ConstFieldName.CHANGE_KEYS_FEILD)).get(mainId);
        result.setData(mainId);
        List<QrTempletDetailDto> lstDetail = templetQuery.getLstDetail();
        if (lstDetail == null || lstDetail.isEmpty()) {
            return result;
        }

        //更新子数据集的ID
        long detailId = -1;
        for (QrTempletDetailDto detailDto : lstDetail) {
            detailDto.setTempletId(mainId);
            detailDto.setTempletDetailId(detailId--);
        }
        insertParam = new InsertParam();
        insertParam.setObjects(lstDetail, QueryConstants.QUERY_DEFAULT_SCHEMA_ID);
        return factory.handleInsert(insertParam);
    }

    /**
     * 模板另存为
     *
     * @param templetQuery
     * @return
     */
    @Override
    @Transactional(readOnly = false)
    public HandleResult saveAsTemplet(QrTempletQuery templetQuery) {
        return saveTemplet(templetQuery);
    }

    /**
     * 删除自定义查询模板
     *
     * @param tempid
     * @return
     */
    @Override
    @Transactional(readOnly = false)
    public HandleResult deleteTemplet(Long tempid) {
        DeleteParam param = DeleteParam.deleteById(QueryConstants.QUERY_DEFAULT_SCHEMA_ID, QrTempletDetailDto.class,
                tempid, SessionUtils.getLoginVersion());
        HandleResult result = factory.handleDelete(param);
        if (!result.isSuccess()) {
            return result;
        }
        QrTempletDetailDto dto = new QrTempletDetailDto();
        dto.setTempletId(tempid);
        param = new DeleteParam();
        param.setOperDto(QueryConstants.QUERY_DEFAULT_SCHEMA_ID, dto, SessionUtils.getLoginVersion());
        result = factory.handleDelete(param);
        if (!result.isSuccess()) {
            throw new IllegalOperatorException("删除失败:" + result.getErr());
        }
        return result;
    }

    /**
     * 查询模板主数据和明细数据
     *
     * @param tempId
     * @param version
     * @return
     */
    @Override
    public HandleResult findTempletQuery(Long tempId, String version) {
        QueryParam param = new QueryParam();
        QrCustomTempletDto mainDto = new QrCustomTempletDto();
        mainDto.setTempletId(tempId);
        param.setFilterObjectAndTableAndResultType(QueryConstants.QUERY_DEFAULT_SCHEMA_ID, version, mainDto);
        HandleResult mainResult = factory.handleQuery(param);
        mainDto = (QrCustomTempletDto) mainResult.singleValue();
        if (mainDto == null) {
            return null;
        }
        QrTempletDetailDto detailDto = new QrTempletDetailDto();
        detailDto.setTempletId(tempId);
        param = new QueryParam();
        param.setFilterObjectAndTableAndResultType(QueryConstants.QUERY_DEFAULT_SCHEMA_ID, version, detailDto);
        param.addOrder(new FieldOrder(CommonUtils.getTableName(QrTempletDetailDto.class),
                "lvl_code", true, 1));
        HandleResult detailResult = factory.handleQuery(param);
        QrTempletQuery templetQuery = new QrTempletQuery();
        templetQuery.setMainDto(mainDto);
        templetQuery.setLstDetail((List<QrTempletDetailDto>) detailResult.getData());
        HandleResult result = HandleResult.success(1);
        result.setData(templetQuery);
        return result;
    }

    /**
     * 取得自定义报表模板列表
     *
     * @return
     */
    @Override
    public HandleResult getCustomReportTemplets(String version) {
        QueryParam param = new QueryParam();
        QrCustomTempletDto dto = new QrCustomTempletDto();
        dto.setVersionCode(version);
        param.addOrder(new FieldOrder(CommonUtils.getTableName(QrCustomTempletDto.class),
                "lvl_code", true, 1));
        return factory.handleQuery(param);
    }

}
