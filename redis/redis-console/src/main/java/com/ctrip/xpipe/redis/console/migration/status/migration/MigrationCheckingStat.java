package com.ctrip.xpipe.redis.console.migration.status.migration;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.ctrip.xpipe.redis.console.migration.command.result.ShardMigrationResult.ShardMigrationStep;
import com.ctrip.xpipe.redis.console.migration.model.MigrationCluster;
import com.ctrip.xpipe.redis.console.migration.model.MigrationShard;
import com.ctrip.xpipe.redis.console.model.MigrationClusterTbl;
import com.ctrip.xpipe.utils.XpipeThreadFactory;

/**
 * @author shyin
 *
 *         Dec 8, 2016
 */
public class MigrationCheckingStat extends AbstractMigrationStat {

	private ExecutorService fixedThreadPool;

	public MigrationCheckingStat(MigrationCluster holder) {
		super(holder, MigrationStatus.Checking);
		this.setNextAfterSuccess(new MigrationMigratingStat(getHolder())).setNextAfterFail(this);

		int threadSize = holder.getMigrationShards().size() == 0 ? 1 : holder.getMigrationShards().size();
		fixedThreadPool = Executors.newFixedThreadPool(threadSize, XpipeThreadFactory.create("MigrationChecking"));
	}

	@Override
	public void action() {
		MigrationCluster migrationCluster = getHolder();
		List<MigrationShard> migrationShards = migrationCluster.getMigrationShards();

		// Update db
		MigrationClusterTbl migrationClusterTbl = migrationCluster.getMigrationCluster();
		migrationClusterTbl.setStatus(MigrationStatus.Checking.toString());
		getHolder().getMigrationService().updateMigrationCluster(migrationClusterTbl);

		for (final MigrationShard migrationShard : migrationShards) {
			fixedThreadPool.submit(new Runnable() {

				@Override
				public void run() {
					migrationShard.doCheck();
				}
			});
		}
	}

	@Override
	public void refresh() {
		int successCnt = 0;
		for (MigrationShard migrationShard : getHolder().getMigrationShards()) {
			if (migrationShard.getShardMigrationResult().stepSuccess(ShardMigrationStep.CHECK)) {
				++successCnt;
			}
		}

		if (successCnt == getHolder().getMigrationShards().size()) {
			logger.info("[MigrationChecking][success][continue]{}", getHolder().getCurrentCluster().getClusterName());
			updateAndProcess(nextAfterSuccess(), true);
		}
	}

}
