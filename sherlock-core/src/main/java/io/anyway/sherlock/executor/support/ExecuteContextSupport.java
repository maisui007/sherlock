package io.anyway.sherlock.executor.support;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.anyway.sherlock.router.table.GlobalTableRepository;
import org.springframework.util.CollectionUtils;

import io.anyway.sherlock.datasource.DatabaseType;
import io.anyway.sherlock.datasource.ShardingDataSourceRepository;
import io.anyway.sherlock.executor.ExecuteContext;
import io.anyway.sherlock.jdbc.ShardingConnection;
import io.anyway.sherlock.router.table.LogicTableRepository;
import io.anyway.sherlock.sqlparser.SQLParsedResult;
import io.anyway.sherlock.sqlparser.bean.SQLStatementType;

public class ExecuteContextSupport implements ExecuteContext{

	private Map<String,Connection> connectionMapping;
	
	private SQLStatementType statementType;
	
	private SQLParsedResult sqlParsedResult;
	
	private String logicSql;
	
	private List<Object> parameters;
	
	private ShardingConnection shardingConnection;
	
	private ShardingDataSourceRepository shardingDataSourceRepository;
	
	private GlobalTableRepository globalTableRepository;
	
	private LogicTableRepository logicTableRepository;
	
	public ExecuteContextSupport(
			ShardingConnection shardingConnection,
			ShardingDataSourceRepository shardingDataSourceRepository,
			GlobalTableRepository globalTableRepository,
			LogicTableRepository logicTableRepository){
		this.shardingConnection= shardingConnection;
		this.shardingDataSourceRepository= shardingDataSourceRepository;
		this.globalTableRepository= globalTableRepository;
		this.logicTableRepository= logicTableRepository;
	}
	
	@Override
	public boolean isSimplyDQLOperation() {
		try {
			return SQLStatementType.SELECT == statementType &&
					(shardingConnection.getAutoCommit() ||
							(!shardingConnection.getAutoCommit() && shardingConnection.isReadOnly()));
		} catch (SQLException e) {
			return false;
		}
	}
	
	@Override
	public boolean isDMLOperation(){
		return SQLStatementType.SELECT != statementType;
	}

	public void setSQLParsedResult(SQLParsedResult sqlParsedResult){
		this.sqlParsedResult= sqlParsedResult;
	}

	@Override
	public SQLParsedResult getSQLParsedResult() {
		return sqlParsedResult;
	}

	public void setTranOrUpdateConnection(String dataSourceName,Connection conn){
		if(connectionMapping== null){
			connectionMapping= new HashMap<String,Connection>();
		}
		connectionMapping.put(dataSourceName, conn);
	}
	
	public Connection getTranOrUpdateConnection(String dataSourceName) {
		if(CollectionUtils.isEmpty(connectionMapping)){
			return null;
		}
		return connectionMapping.get(dataSourceName);
	}
	
	public void setStatementType(SQLStatementType statementType){
		this.statementType= statementType;
	}
	
	public SQLStatementType getStatementType(){
		return statementType;
	}
	
	public void setLogicSql(String logicSql){
		this.logicSql= logicSql;
	}
	
	@Override
	public String getLogicSql(){
		return logicSql;
	}

	@Override
	public ShardingConnection getShardingConnection() {
		return shardingConnection;
	}
	
	public void setParameters(List<Object> parameters){
		this.parameters= parameters;
	}

	@Override
	public List<Object> getParameters() {
		return parameters;
	}

	@Override
	public ShardingDataSourceRepository getShardingDataSourceRepository() {
		return shardingDataSourceRepository;
	}

	@Override
	public GlobalTableRepository getGlobalTableRepository() {
		return globalTableRepository;
	}

	@Override
	public LogicTableRepository getLogicTableRepository() {
		return logicTableRepository;
	}

	@Override
	public DatabaseType getDatabaseType() {
		return shardingDataSourceRepository.getDatabaseType();
	}
}
