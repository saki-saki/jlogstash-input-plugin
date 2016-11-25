package com.dtstack.logstash.inputs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

/**
 * 读取tar文件信息
 * Reason: TODO ADD REASON(可选)
 * Date: 2016年11月24日
 * Company: www.dtstack.com
 * @author xuchao
 *
 */
public class ReadTarFile implements IReader {
	
	private static final Logger logger = LoggerFactory.getLogger(ReadTarFile.class);
		
	private BufferedReader currBuff;
	
	private TarArchiveInputStream tarAchive;
	
	private FileInputStream fins;
	
	private String currFileName;
	
	private int currFileSize = 0;
	
	private String tarFileName = "";
	
	private String encoding = "UTF-8"; 
	
	private boolean readEnd = false;
	
	private Map<String, Integer> fileCurrPos;
	
	private ReadTarFile(String fileName, String encoding, Map fileCurrPos){
		this.tarFileName = fileName;
		this.fileCurrPos = fileCurrPos;
		this.encoding = encoding;
	}
	
	public static ReadTarFile createInstance(String fileName, String encoding, ConcurrentHashMap<String, Integer> fileCurrPos){
		ReadTarFile readTar = new ReadTarFile(fileName, encoding, fileCurrPos);
		if(readTar.init()){
			return readTar;
		}
		
		return null;
	}
	
	public boolean init(){
		if (!tarFileName.toLowerCase().endsWith(".tar")) {
			logger.error("file:{} is not a tar file.", tarFileName);
			return false;
		}
		
		try{
			fins = new FileInputStream(tarFileName);
			tarAchive = new TarArchiveInputStream(fins);
			getNextBuffer();
		}catch(Exception e){
			logger.error("", e);
			return false;
		}
		
		return true;
	}
		
	public BufferedReader getNextBuffer(){
		
		if(currBuff != null){
			//不能关闭currBuffer否则得重新创建inputstream
			currBuff = null;
		}
		
		TarArchiveEntry entry = null;
		
		try{
			while((entry = tarAchive.getNextTarEntry()) != null){
				if(entry.isDirectory()){
					continue;
				}
				
				currFileName = entry.getName();
				currFileSize = (int) entry.getSize();
				String identify = getIdentify(currFileName);
				int skipNum = getSkipNum(identify);
				if(skipNum >= entry.getSize()){
					continue;
				}
				
				tarAchive.skip(skipNum);
				currBuff = new BufferedReader(new InputStreamReader(tarAchive, encoding));
				break;
			}
			
		}catch(Exception e){
			logger.error("", e);
		}
		
		if(currBuff == null){
			try {
				tarAchive.close();
				fins.close();
				doAfterReaderOver();
			} catch (IOException e) {
				logger.error("", e);
			}
		}
		
		return currBuff;
	}
	
	/**
	 * 清理之前记录的zip文件里的文件信息eg: e:\\d\xx.tar|mydata/aa.log
	 */
	private void doAfterReaderOver(){
		Iterator<Entry<String, Integer>> it = fileCurrPos.entrySet().iterator();
		String preFix = tarFileName + "|";
		while(it.hasNext()){
			Entry<String, Integer> entry = it.next();
			if(entry.getKey().startsWith(preFix)){
				it.remove();
			}
		}
		
		//重新插入一条表示zip包读取完成的信息
		fileCurrPos.put(tarFileName, -1);
		readEnd = true;
	}

	@Override
	public String readLine() throws IOException {
		
		if(currBuff == null){
			return null;
		}
		
		String str = currBuff.readLine();
		if(str == null){
			BufferedReader buff = getNextBuffer();
			if(buff != null){
				str = currBuff.readLine();
			}
		}
		return str;
	}

	@Override
	public String getFileName() {
		
		if(currFileName == null){
			return tarFileName;
		}
		
		return getIdentify(currFileName);
	}

	@Override
	public int getCurrBufPos() {

		if(readEnd){
			return -1;
		}
		
		int available = 0;
		try {
			available = tarAchive.available();
		} catch (IOException e) {
			logger.error("error:", e);
			return 0;
		}
		
		return currFileSize - available;
	
	}
	
	private String getIdentify(String fileName){
		return tarFileName + "|" + fileName; 
	}
	
	private int getSkipNum(String identify){
		Integer skipNum = fileCurrPos.get(identify);
		skipNum = skipNum == null ? 0 : skipNum;
		return skipNum;
	}
	    
    public static void main(String[] args) throws IOException {
    	Map<String, Integer> map = Maps.newConcurrentMap();
		ReadTarFile readTar = new ReadTarFile("E:\\data\\xcdir.tar", "utf-8",map);
		readTar.init();
		String line = null;
		while( (line = readTar.readLine()) != null){
			System.out.println(line);
		}
	}

}
