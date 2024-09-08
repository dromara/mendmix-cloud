/**
 * 
 */
package org.dromara.mendmix.common.model;

/**
 * <br>
 * @author vakinge(vakinge)
 * @date 2023年12月12日
 */
public class ModifiedObject implements Comparable<ModifiedObject>{

	private long lastModifiedTime;
	private Object object;
	
	public ModifiedObject() {}
	public ModifiedObject(Object object) {
		this.lastModifiedTime = System.currentTimeMillis();
		this.object = object;
	}
	
	public long getLastModifiedTime() {
		return lastModifiedTime;
	}
	public void setLastModifiedTime(long lastModifiedTime) {
		this.lastModifiedTime = lastModifiedTime;
	}
	public <T> T getObject() {
		return (T) object;
	}
	public void setObject(Object object) {
		this.object = object;
	}
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(ModifiedObject o) {
		return (int) (this.lastModifiedTime - o.lastModifiedTime);
	}
	
	
}
