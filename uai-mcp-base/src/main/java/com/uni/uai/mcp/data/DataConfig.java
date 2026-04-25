package com.uni.uai.mcp.data;

import java.beans.PropertyVetoException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import com.uni.uai.mcp.utils.YmlConfigUtil;
import com.uni.ubag.common.constant.UbagConfigEnum;
import com.uni.ubag.common.log.Logger;
import com.uni.ubag.common.log.LoggerFactory;
import com.uni.ubag.common.util.ExceptionUtil;
import com.uni.ubag.common.util.MapUtil;
import com.uni.ubag.data.model.DataSourceConfig;
import com.uni.ubag.log.util.UbagLogUtil;
import com.mchange.v2.c3p0.ComboPooledDataSource;

/**
 * 管理所有datasource的config配置，对应ubag中的6100、6200等配置
 */
public class DataConfig {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private static DataConfig instance = new DataConfig();
	public static DataConfig getInstance() {
		return instance;
	}
	private String defaultDbConfigName = "toolsDbConfigName";
	
	/**
	 * 保存所有6100的配置
	 */
	private Map<String, DataSourceConfig.DB> db = new HashMap<String, DataSourceConfig.DB>();
	
	{
		DataSourceConfig.DB dataSourceConfig = new DataSourceConfig.DB();
		
		dataSourceConfig.setConfigName(defaultDbConfigName);
		dataSourceConfig.setJdbcUrl(YmlConfigUtil.getInstance().getYmlConfigValue("mysql.datasource.url"));
		dataSourceConfig.setUser(YmlConfigUtil.getInstance().getYmlConfigValue("mysql.datasource.username"));
		dataSourceConfig.setPassword(YmlConfigUtil.getInstance().getYmlConfigValue("mysql.datasource.password"));
		
		db.put(defaultDbConfigName, dataSourceConfig);
		
	}
	
	public DataSourceConfig.DB getDefaultDbConfig(){
		return this.getDbConfig(defaultDbConfigName);
	}

	public DataSourceConfig.DB getDbConfig(String configName) {
		return db.get(configName);
	}
	
	ComboPooledDataSource dataSource = null;
	public DataSource getDefaultDataSource() {
		if(dataSource == null) {
			synchronized (this) {
				if(dataSource == null) {
					try {
						dataSource = new ComboPooledDataSource();
						dataSource.setDriverClass("com.mysql.jdbc.Driver");
						String url = YmlConfigUtil.getInstance().getYmlConfigValue("mysql.datasource.url");
						url = "jdbc:mysql://" + url;
		            	//如果mysql连接后没有附加其他连接信息，则添加一些autoReconnect=true等配置，如果已经附加了，则不再添加
						//指向mcp库
						if(url.indexOf("?")>0) {
		                	url = url.substring(0, url.indexOf("?")) + "/mcp" + url.substring(url.indexOf("?"), url.length()) ;
		                }else {
		                	url += "/mcp?allowMultiQueries=true&autoReconnect=true&failOverReadOnly=false";
		                }
						dataSource.setJdbcUrl(url);
						dataSource.setUser(YmlConfigUtil.getInstance().getYmlConfigValue("mysql.datasource.username"));
						dataSource.setPassword(YmlConfigUtil.getInstance().getYmlConfigValue("mysql.datasource.password"));
					} catch (Exception e) {
						e.printStackTrace();
						UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.EXCEPTION.getCode(), "getDefaultDataSource异常", e.getClass() + ":" + e.getMessage(), e, false, 0L, ExceptionUtil.toStackTrace(e));
					}
				}
			}
		}
		return dataSource;
	}
	
	public static void main(String[] args) {
		String url = "m10827.mars.test.mysql.ljnode.com:10827";
		url = "jdbc:mysql://" + url;
    	//如果mysql连接后没有附加其他连接信息，则添加一些autoReconnect=true等配置，如果已经附加了，则不再添加
		//指向mcp库
		if(url.indexOf("?")>0) {
        	url = url.substring(0, url.indexOf("?")) + "/mcp" + url.substring(url.indexOf("?"), url.length()) ;
        }else {
        	url += "/mcp?allowMultiQueries=true&autoReconnect=true&failOverReadOnly=false";
        }
		System.out.println(url);
	}
	

}
