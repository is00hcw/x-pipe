package com.ctrip.xpipe.redis.console.migration.status.migration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.ctrip.xpipe.redis.console.annotation.DalTransaction;
import com.ctrip.xpipe.redis.console.migration.command.result.ShardMigrationResult.ShardMigrationStep;
import com.ctrip.xpipe.redis.console.migration.model.MigrationCluster;
import com.ctrip.xpipe.redis.console.migration.model.MigrationShard;
import com.ctrip.xpipe.redis.console.migration.status.cluster.ClusterStatus;
import com.ctrip.xpipe.redis.console.model.ClusterTbl;
import com.ctrip.xpipe.redis.console.model.MigrationClusterTbl;
import com.ctrip.xpipe.utils.XpipeThreadFactory;

/**
 * @author shyin
 *
 * Dec 8, 2016
 */
public class MigrationPartialSuccessStat extends AbstractMigrationMigratingStat {
	
	private ExecutorService cachedThreadPool;
	
	public MigrationPartialSuccessStat(MigrationCluster holder) {
		super(holder, MigrationStatus.PartialSuccess);
		this.setNextAfterSuccess(new MigrationPublishStat(getHolder()))
			.setNextAfterFail(this);
		
		cachedThreadPool = Executors.newCachedThreadPool(XpipeThreadFactory.create("MigrationPartialSuccess"));
	}

	@Override
	public void action() {
		updateDB();
		
		for(final MigrationShard shard : getHolder().getMigrationShards()) {
			if(!shard.getShardMigrationResult().stepSuccess(ShardMigrationStep.MIGRATE)) {
				cachedThreadPool.submit(new Runnable() {

					@Override
					public void run() {
						logger.info("[doMigrate][start]{},{}",getHolder().getCurrentCluster().getClusterName(), 
								shard.getCurrentShard().getShardName());
						shard.doMigrate();
						logger.info("[doMigrate][done]{},{}",getHolder().getCurrentCluster().getClusterName(), 
								shard.getCurrentShard().getShardName());
					}
				});
			}
		}
	}
	
	@DalTransaction
	private void updateDB() {
		ClusterTbl cluster = getHolder().getCurrentCluster();
		cluster.setStatus(ClusterStatus.Migrating.toString());
		getHolder().getClusterService().update(cluster);
		
		MigrationClusterTbl migrationClusterTbl = getHolder().getMigrationCluster();
		migrationClusterTbl.setStatus(MigrationStatus.PartialSuccess.toString());
		getHolder().getMigrationService().updateMigrationCluster(migrationClusterTbl);
		
		logger.info("[updateDB]Cluster:{}, MigrationCluster:{}", cluster, migrationClusterTbl);
	}

}
