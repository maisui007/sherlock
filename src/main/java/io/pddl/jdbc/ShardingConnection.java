package io.pddl.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;

import io.pddl.datasource.PartitionDataSource;
import io.pddl.datasource.ShardingDataSourceRepository;
import io.pddl.executor.ExecuteContext;
import io.pddl.executor.ExecuteProcessor;
import io.pddl.executor.support.ExecuteContextSupport;
import io.pddl.jdbc.adapter.AbstractConnectionAdapter;
import io.pddl.router.SQLRouter;

public final class ShardingConnection extends AbstractConnectionAdapter {
	
	private ShardingDataSourceRepository shardingDataSourceRepository;
	
	private SQLRouter sqlRouter;
	
	private ExecuteContext executeContext;
	
	private ExecuteProcessor processor;
	
	private Set<Connection> connections= new HashSet<Connection>();
	
    public ShardingConnection(ShardingDataSourceRepository shardingDataSourceRepository,SQLRouter sqlRouter,ExecuteProcessor processor){
    	this.shardingDataSourceRepository= shardingDataSourceRepository;
    	this.sqlRouter= sqlRouter;
    	this.processor= processor;
    	this.executeContext= new ExecuteContextSupport(this);
    }
    
    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
    	DataSource ds= shardingDataSourceRepository.getDefaultPartitionDataSource().getMasterDataSource();
    	Connection conn= ds.getConnection();
    	try{
    		return conn.getMetaData();
    	}finally{
    		conn.close();
    	}
    }
    
    @Override
    public PreparedStatement prepareStatement(final String sql) throws SQLException {
        return new ShardingPreparedStatement(this, sql);
    }
    
    @Override
    public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency) throws SQLException {
        return new ShardingPreparedStatement(this, sql, resultSetType, resultSetConcurrency);
    }
    
    @Override
    public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability) throws SQLException {
        return new ShardingPreparedStatement(this, sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }
    
    @Override
    public PreparedStatement prepareStatement(final String sql, final int autoGeneratedKeys) throws SQLException {
        return new ShardingPreparedStatement(this, sql, autoGeneratedKeys);
    }
    
    @Override
    public PreparedStatement prepareStatement(final String sql, final int[] columnIndexes) throws SQLException {
        return new ShardingPreparedStatement(this, sql, columnIndexes);
    }
    
    @Override
    public PreparedStatement prepareStatement(final String sql, final String[] columnNames) throws SQLException {
        return new ShardingPreparedStatement(this, sql, columnNames);
    }
    
    @Override
    public Statement createStatement() throws SQLException {
        return new ShardingStatement(this);
    }
    
    @Override
    public Statement createStatement(final int resultSetType, final int resultSetConcurrency) throws SQLException {
        return new ShardingStatement(this, resultSetType, resultSetConcurrency);
    }
    
    @Override
    public Statement createStatement(final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability) throws SQLException {
        return new ShardingStatement(this, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

	@Override
	protected Collection<Connection> getConnections() {
		return connections;
	}
	@Override
	public Connection getConnection(String dataSourceName) throws SQLException{
		PartitionDataSource dsp= shardingDataSourceRepository.getPartitionDataSource(dataSourceName);
		Connection connection = null;
		ExecuteContextSupport ctx= (ExecuteContextSupport)getExecuteContext();
		if(!ctx.isDQLWithoutTransaction()){
			if(null!= (connection= ctx.getTranOrUpdateConnection(dataSourceName))){
				return connection;
			}
			connection= dsp.getMasterDataSource().getConnection();
			connection.setAutoCommit(getAutoCommit());
			ctx.setTranOrUpdateConnection(dataSourceName,connection);
		}
		else{
			connection= dsp.getSlaveDataSource().getConnection();
		}
		connections.add(connection);
		return connection;
	}
	
	public SQLRouter getSqlRouter(){
		return sqlRouter;
	}
	
	public ExecuteProcessor getProcessor(){
		return processor;
	}
	
	public ExecuteContext getExecuteContext(){
	    return executeContext;
	}
	
	public ShardingDataSourceRepository getShardingDataSourceRepository(){
		return shardingDataSourceRepository;
	}
    
}