package com.ctrip.xpipe.redis.keeper.store;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;

import com.ctrip.xpipe.api.utils.ControllableFile;
import com.ctrip.xpipe.exception.XpipeRuntimeException;
import com.ctrip.xpipe.utils.DefaultControllableFile;

/**
 * @author wenchao.meng
 *
 *         Dec 7, 2016
 */
public class CommandFileContext {
	
	private final long currentStartOffset;

	private ControllableFile controllableFile;

	public CommandFileContext(long currentStartOffset, File currentFile) throws IOException {
		this.currentStartOffset = currentStartOffset;
		this.controllableFile = new DefaultControllableFile(currentFile, currentFile.length());
	}
	

	public void close() throws IOException {
		controllableFile.close();
	}
	
	
	
	public long fileLength(){
		try {
			return controllableFile.size();
		}catch (IOException e) {
			throw new XpipeRuntimeException(String.format("%s", controllableFile), e);
		}
	}

	public long totalLength() {
		return currentStartOffset + fileLength();
	}

	public FileChannel getChannel() throws IOException {
		return controllableFile.getFileChannel();
	}

}
