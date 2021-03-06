package edu.pku.sxt.pkuc.client.ui;
import java.io.DataOutputStream; 
import java.io.File; 
import java.io.FileOutputStream; 
import java.io.FileWriter; 
import java.io.FileInputStream; 
import java.io.FileNotFoundException; 
import java.io.IOException; 
import android.content.Context; 
import android.os.Environment; 
public class SDCard {
	private Context context; 
	/** SD卡是否存在**/ 
	private boolean hasSD = false; 
	/** SD卡的路径**/ 
	public String SDPATH; 
	/** 当前程序包的路径**/ 
	public String FILESPATH; 
	public void FileHelper(Context context) { 
		this.context = context; 
		hasSD = Environment.getExternalStorageState().equals( 
				android.os.Environment.MEDIA_MOUNTED); 
		SDPATH = Environment.getExternalStorageDirectory().getPath(); 
		FILESPATH = this.context.getFilesDir().getPath(); 
	} 
	/** 
	* 在SD卡上创建文件 
	* 
	* @throws IOException 
	*/ 
	public File createSDFile(String fileName) throws IOException { 
		File file = new File(SDPATH + "//" + fileName); 
		if (!file.exists()) { 
			file.createNewFile(); 
		} 
		return file; 
	} 
	/** 
	* 删除SD卡上的文件 
	* 
	* @param fileName 
	*/ 
	public boolean deleteSDFile(String fileName) { 
		File file = new File(SDPATH + "//" + fileName); 
		if (file == null || !file.exists() || file.isDirectory()) 
			return false; 
		return file.delete(); 
	} 
	/** 
	* 写入内容到SD卡中的txt文本中 
	* str为内容 
	*/ 
	public void writeSDFile(String str,String fileName) { 
		try { 
			FileWriter fw = new FileWriter(SDPATH + "//" + fileName); 
			File f = new File(SDPATH + "//" + fileName); 
			fw.write(str); 
			FileOutputStream os = new FileOutputStream(f); 
			DataOutputStream out = new DataOutputStream(os); 
			out.writeShort(2); 
			out.writeUTF(""); 
			System.out.println(out); 
			fw.flush(); 
			fw.close(); 
			System.out.println(fw); 
		} catch (Exception e) { 
		
		} 
	}
}
