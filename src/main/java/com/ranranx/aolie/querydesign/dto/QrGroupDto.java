package com.ranranx.aolie.querydesign.dto;

import javax.persistence.Table;
import com.ranranx.aolie.core.common.BaseDto;
/**
 * @author xxl 
 * @date 2021-06-16 16:15:50
 * @version 1.0
 */
@Table(name = "aolie_qr_group")
public class QrGroupDto extends BaseDto implements java.io.Serializable{

	private static final long serialVersionUID = 1L;
	private Long reportId;
	private String reportName;
	private Long blockId;
	private String fixFilter;
	private String lvlCode;
	public void setReportId(Long reportId){
		this.reportId = reportId;
	}
	public Long getReportId(){
		return this.reportId;
	}
	public void setReportName(String reportName){
		this.reportName = reportName;
	}
	public String getReportName(){
		return this.reportName;
	}
	public void setBlockId(Long blockId){
		this.blockId = blockId;
	}
	public Long getBlockId(){
		return this.blockId;
	}
	public void setFixFilter(String fixFilter){
		this.fixFilter = fixFilter;
	}
	public String getFixFilter(){
		return this.fixFilter;
	}
	public void setLvlCode(String lvlCode){
		this.lvlCode = lvlCode;
	}
	public String getLvlCode(){
		return this.lvlCode;
	}

}